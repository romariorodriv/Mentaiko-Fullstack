# Recuperación de contraseña: operación y despliegue

Este procedimiento está preparado para ejecución manual posterior. No se ha ejecutado en AWS.

Destino confirmado: Ubuntu 24.04, usuario SSH `ubuntu`, servicio `mentaiko.service`, JAR `/opt/mentaiko/mentaiko.jar`, Angular `/var/www/mentaiko`, backend `127.0.0.1:8080`, PostgreSQL 17 en `5433`, base `mentaiko_db`, dominio `mentaiko-app.duckdns.org`.

## Comportamiento y límites

- Tokens aleatorios de 256 bits, SHA-256 en base de datos, 15 minutos, un solo uso. Un restablecimiento invalida todos los enlaces pendientes del usuario.
- El cambio incrementa `users.auth_version`; los JWT nuevos llevan `av`. Un JWT anterior sin ese claim se interpreta como versión cero, por lo que la primera recuperación lo revoca. Editar el perfil no cierra sesiones.
- El cooldown de dos minutos se guarda en el usuario bajo bloqueo, incluso si SMTP falla. La emisión y el restablecimiento toman primero el bloqueo del usuario para serializar enlaces concurrentes.
- Respuesta genérica inmediata: el trabajo se admite en una cola de 20 elementos y 1–2 hilos. No se mantiene una transacción de base de datos durante SMTP. Tras fallo SMTP se elimina el token emitido, conservando el cooldown.
- Los errores del proveedor se registran por clase, sin cuerpos, direcciones, tokens ni credenciales.
- Límites por 15 minutos: solicitar enlace, 10 por IP y 100 globales; restablecer, 30 por IP y 300 globales. HTTP 429 incluye `Retry-After: 900`. La memoria del limitador está acotada a 10 000 claves. Reiniciar reinicia estos contadores, pero no el cooldown persistente.
- La cola no es durable: reinicios o saturación pueden perder solicitudes; el usuario puede reintentar. Para varias instancias se necesitarían cola y límites compartidos. No hay reintentos SMTP automáticos.
- Se eliminan diariamente tokens cuyo vencimiento ocurrió hace más de siete días.

## Variables de producción

El servicio debe cargar `/etc/mentaiko.env`. No imprimir ni copiar su contenido a tickets o commits.

| Nombre | Requisito |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://127.0.0.1:5433/mentaiko_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuario PostgreSQL ya configurado |
| `DB_PASSWORD` | Contraseña existente, o `SPRING_DATASOURCE_PASSWORD` si se sobrescribe directamente |
| `JWT_SECRET` | Secreto existente de al menos 32 bytes; conservar durante el despliegue normal |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Cuenta SMTP y credencial existentes |
| `APP_PUBLIC_URL` | `https://mentaiko-app.duckdns.org` |
| `JWT_EXPIRATION` | Opcional; predeterminado 86400000 ms |

El código utiliza Gmail, puerto 587 y STARTTLS requerido. Los timeouts SMTP de conexión, lectura y escritura son de 5000 ms. Se pueden sobrescribir con las propiedades estándar de Spring si el proveedor lo requiere. El perfil `prod` desactiva SQL en logs y el bootstrap administrativo por defecto, establece `validate` y escucha en loopback.

## Preparación en Windows / PowerShell

Ejecutar desde la raíz del repositorio solo cuando se decida preparar una entrega. Las primeras órdenes son locales. Usar la configuración SSH existente (agente, `~/.ssh/config` u opciones locales); no colocar claves privadas en el paquete.

```powershell
Set-Location C:\Users\Romario\secondMind\Mentaiko-Fullstack
$ErrorActionPreference = 'Stop'
git branch --show-current
git status --short
git diff --check
if ($LASTEXITCODE -ne 0) { throw 'Diff inválido' }

Push-Location backend
try {
    .\mvnw.cmd clean package
    if ($LASTEXITCODE -ne 0) { throw 'Falló backend o sus pruebas' }
} finally { Pop-Location }
Push-Location frontend
try {
    npm ci --no-audit
    if ($LASTEXITCODE -ne 0) { throw 'Falló instalación reproducible' }
    npm test
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron pruebas Angular' }
    npm run build -- --configuration production
    if ($LASTEXITCODE -ne 0) { throw 'Falló build Angular' }
    npm audit
    if ($LASTEXITCODE -ne 0) { throw 'Revisar alertas antes de entregar' }
} finally { Pop-Location }

$releaseId = 'password-reset-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
$stage = Join-Path $env:TEMP $releaseId
New-Item -ItemType Directory -Path "$stage/frontend" -Force | Out-Null
Copy-Item backend/target/mentaiko-backend-0.0.1-SNAPSHOT.jar "$stage/mentaiko.jar"
Copy-Item frontend/dist/mentaiko/browser/* "$stage/frontend" -Recurse
Copy-Item database/002_password_reset_security.sql $stage
# El archivo 001 no se incluye: password_reset_tokens ya existe.
$sourceCommit = git rev-parse HEAD
$sourceCommit | Set-Content "$stage/source-commit.txt" -Encoding ascii
git status --short | Set-Content "$stage/working-tree.txt" -Encoding utf8
# Antes de entregar oficialmente, hacer commits revisados: source-commit debe identificar el código construido.
Get-ChildItem $stage -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($stage.Length + 1).Replace('\', '/')
    $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $relative"
} | Set-Content "$stage/SHA256SUMS" -Encoding ascii
$bundle = Join-Path $env:TEMP "$releaseId.tar.gz"
tar -czf $bundle -C $stage .
if ($LASTEXITCODE -ne 0) { throw 'Falló empaquetado' }
```

La transferencia y conexión siguientes se ejecutan **solo con autorización posterior de despliegue**. No equivalen a `git push`.

```powershell
$deployTarget = 'ubuntu@mentaiko-app.duckdns.org'
scp $bundle "${deployTarget}:/home/ubuntu/"
if ($LASTEXITCODE -ne 0) { throw 'Falló transferencia' }
Write-Output "Identificador de entrega: $releaseId"
ssh $deployTarget
```

## Preparación y respaldos en Ubuntu

Reemplazar el identificador de ejemplo por el mostrado en PowerShell. Continuar en la misma sesión Bash para conservar las variables. Estos comandos están documentados, no ejecutados.

```bash
set -euo pipefail
RELEASE_ID='password-reset-YYYYMMDD-HHMMSS'
[[ "$RELEASE_ID" =~ ^password-reset-[0-9]{8}-[0-9]{6}$ ]]
RELEASE="/home/ubuntu/releases/$RELEASE_ID"
BACKUP="/opt/mentaiko/backups/$RELEASE_ID"
mkdir -p "$RELEASE"
tar -xzf "/home/ubuntu/$RELEASE_ID.tar.gz" -C "$RELEASE"
(cd "$RELEASE" && sha256sum -c SHA256SUMS)
test -s "$RELEASE/mentaiko.jar"
test -s "$RELEASE/frontend/index.html"
test -f /opt/mentaiko/mentaiko.jar
test -d /var/www/mentaiko
java -version
sudo systemctl is-active --quiet mentaiko.service
sudo systemctl show mentaiko.service -p EnvironmentFiles
sudo nginx -t
curl --fail --silent --show-error http://127.0.0.1:8080/api/health
sudo install -d -m 0700 "$BACKUP"
sudo cp -a /opt/mentaiko/mentaiko.jar "$BACKUP/mentaiko.jar"
sudo tar -czpf "$BACKUP/frontend.tar.gz" -C /var/www/mentaiko .
sudo tar -czpf "$BACKUP/nginx.tar.gz" -C /etc/nginx .
sudo cp -a /etc/mentaiko.env "$BACKUP/mentaiko.env"
sudo chmod 0600 "$BACKUP/mentaiko.env"
sudo -u postgres pg_dump -p 5433 -d mentaiko_db -Fc | sudo tee "$BACKUP/mentaiko_db.dump" >/dev/null
sudo test -s "$BACKUP/mentaiko_db.dump"
```

El dump contiene información personal y el respaldo de variables contiene secretos: mantener ambos en el directorio root 0700; no descargarlos al repositorio. Comprobar las variables de la tabla en el servidor sin pegarlas en chat. No usar `source /etc/mentaiko.env`: la sintaxis de systemd no debe interpretarse como un script.

## Nginx y direcciones de cliente

Revisar el bloque HTTPS existente sin reemplazar certificados ni otras rutas. Debe conservar `/api/` al reenviar:

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $remote_addr;
}
location / {
    try_files $uri $uri/ /index.html;
}
```

El perfil `prod` usa el procesamiento nativo de cabeceras de Tomcat con proxies de confianza limitados a loopback. Nginx debe sobrescribir `X-Forwarded-For`; no exponer 8080 públicamente. Evita que todos los usuarios compartan la cuota IP de `127.0.0.1`, sin confiar en cabeceras enviadas directamente desde Internet.

La ruta de restablecimiento lleva un secreto en la query. En el bloque `server` HTTPS agregar una ubicación exacta, o aplicar el mismo comportamiento a la ubicación ya existente:

```nginx
location = /restablecer-contrasena {
    access_log off;
    try_files $uri /index.html;
}
```

No habilitar logs de depuración HTTP. El documento Angular incluye `Referrer-Policy: no-referrer` mediante meta. Si se añaden analíticas o un proxy externo, excluir también la query del registro allí. Tras revisar cambios: `sudo nginx -t` y luego `sudo systemctl reload nginx`.

## Migración y actualización

Se necesita una pausa breve del backend. La migración **002** es aditiva e idempotente: añade `auth_version BIGINT NOT NULL DEFAULT 0` y `password_reset_requested_at TIMESTAMPTZ` a `users`. No ejecutar nuevamente 001 ni eliminar `password_reset_tokens`.

```bash
sudo systemctl stop mentaiko.service
sudo -u postgres psql -X -p 5433 -d mentaiko_db -v ON_ERROR_STOP=1 < "$RELEASE/002_password_reset_security.sql"
sudo -u postgres psql -X -p 5433 -d mentaiko_db -v ON_ERROR_STOP=1 -c \
  "SELECT column_name,data_type,is_nullable FROM information_schema.columns WHERE table_name='users' AND column_name IN ('auth_version','password_reset_requested_at');"

# Preservar propietario, grupo y permisos del JAR anterior.
sudo cp "$RELEASE/mentaiko.jar" /opt/mentaiko/mentaiko.jar.new
sudo chown --reference=/opt/mentaiko/mentaiko.jar /opt/mentaiko/mentaiko.jar.new
sudo chmod --reference=/opt/mentaiko/mentaiko.jar /opt/mentaiko/mentaiko.jar.new
sudo mv /opt/mentaiko/mentaiko.jar.new /opt/mentaiko/mentaiko.jar
sudo systemctl start mentaiko.service

healthy=0
for attempt in $(seq 1 30); do
    if curl --fail --silent http://127.0.0.1:8080/api/health >/dev/null; then healthy=1; break; fi
    sleep 2
done
test "$healthy" -eq 1
sudo systemctl is-active --quiet mentaiko.service
curl --fail --silent --show-error http://127.0.0.1:8080/api/health

# index.html se copia al final; los assets previos se conservan para pestañas abiertas.
sudo rsync -a --exclude=index.html "$RELEASE/frontend/" /var/www/mentaiko/
sudo cp "$RELEASE/frontend/index.html" /var/www/mentaiko/index.html.new
sudo chown --reference=/var/www/mentaiko/index.html /var/www/mentaiko/index.html.new
sudo chmod --reference=/var/www/mentaiko/index.html /var/www/mentaiko/index.html.new
sudo mv /var/www/mentaiko/index.html.new /var/www/mentaiko/index.html
sudo nginx -t
sudo systemctl reload nginx
curl --fail --silent --show-error https://mentaiko-app.duckdns.org/api/health
curl --fail --silent --show-error -o /dev/null https://mentaiko-app.duckdns.org/olvide-contrasena
curl --fail --silent --show-error -o /dev/null https://mentaiko-app.duckdns.org/restablecer-contrasena
sudo ss -ltnp 'sport = :8080'
```

Si un comando falla, detener el procedimiento y usar el rollback. No continuar subiendo el frontend si el backend no está sano. La salud HTTP no prueba entrega SMTP: realizar luego una prueba manual con una cuenta controlada, abrir el correo, cambiar la contraseña, verificar rechazo de reutilización y del JWT anterior e iniciar sesión con la nueva contraseña. No registrar tokens ni contraseñas en comandos o capturas compartidas.

## Rollback

Restaurar artefactos, no los datos de usuarios. Las dos columnas nuevas son compatibles con el JAR anterior y deben permanecer; no recuperar el dump salvo recuperación de desastre aprobada, porque sobrescribiría datos posteriores.

Si el JAR anterior no comprueba `auth_version`, podría volver a aceptar JWT revocados durante esta entrega. Por eso el rollback hacia ese JAR **rota el secreto JWT sin imprimirlo**, cerrando todas las sesiones. Comprobar previamente que `/etc/mentaiko.env` sea la fuente efectiva de `JWT_SECRET` (sin overrides en otros archivos o argumentos).

```bash
set -euo pipefail
RELEASE_ID='password-reset-YYYYMMDD-HHMMSS'
[[ "$RELEASE_ID" =~ ^password-reset-[0-9]{8}-[0-9]{6}$ ]]
BACKUP="/opt/mentaiko/backups/$RELEASE_ID"
sudo test -s "$BACKUP/mentaiko.jar"
sudo test -s "$BACKUP/frontend.tar.gz"
sudo systemctl stop mentaiko.service
sudo cp -a "$BACKUP/mentaiko.jar" /opt/mentaiko/mentaiko.jar
sudo python3 - <<'PY'
from pathlib import Path
import re, secrets
p = Path('/etc/mentaiko.env')
text = p.read_text()
pattern = r'(?m)^JWT_SECRET=.*$'
if len(re.findall(pattern, text)) != 1:
    raise SystemExit('Revisar la definición única de JWT_SECRET; no se ha cambiado el archivo')
updated = re.sub(pattern, lambda _: 'JWT_SECRET=' + secrets.token_hex(64), text)
p.write_text(updated)
PY
sudo chmod 0600 /etc/mentaiko.env
sudo tar -xzpf "$BACKUP/frontend.tar.gz" -C /var/www/mentaiko
# Los assets nuevos no referenciados se pueden conservar; index.html vuelve al anterior.
sudo systemctl start mentaiko.service
healthy=0
for attempt in $(seq 1 30); do
    if curl --fail --silent http://127.0.0.1:8080/api/health >/dev/null; then healthy=1; break; fi
    sleep 2
done
test "$healthy" -eq 1
sudo nginx -t
sudo systemctl reload nginx
curl --fail --silent --show-error https://mentaiko-app.duckdns.org/api/health
```

Si se modificó Nginx incorrectamente, restaurar únicamente el archivo editado desde `nginx.tar.gz`, ejecutar `nginx -t` y recargar; no sobreescribir configuraciones ajenas cambiadas después del respaldo. No restaurar automáticamente el secreto anterior desde el backup.

## Validaciones automatizadas

La suite normal usa H2 y un `JavaMailSender` simulado, sin configuración manual ni transporte SMTP. La prueba PostgreSQL es opt-in y rechaza URLs distintas de `jdbc:postgresql://127.0.0.1:55439/mentaiko_reset_test`. Solo se permite sobre una base desechable: crea y modifica su esquema.

```powershell
# Con el PostgreSQL desechable local preparado, nunca con producción:
Set-Location backend
.\mvnw.cmd test '-Dtest=PostgresPasswordResetMigrationTest' '-Dpostgres.test.url=jdbc:postgresql://127.0.0.1:55439/mentaiko_reset_test'
.\mvnw.cmd test '-Dtest=PasswordResetServiceTest' '-Dpassword-reset.test-url=jdbc:postgresql://127.0.0.1:55439/mentaiko_reset_test' '-Dpassword-reset.test-driver=org.postgresql.Driver' '-Dpassword-reset.test-user=postgres'
```

Las pruebas Angular usan el builder oficial `@angular/build:unit-test`, Vitest y jsdom: [documentación Angular](https://angular.dev/guide/testing). Las versiones Angular permanecen en la mayor 22; no se utilizó `audit fix --force`. npm 11 se usó temporalmente para resolver un fallo interno de npm 10 al actualizar peers; `npm ci` se verificó con npm 10 y el lockfile final.
