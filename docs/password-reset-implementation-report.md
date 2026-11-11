# Recuperación de contraseña: entrega local aislada

Estado: código organizado en `fix/password-reset-hardening`, con commits locales y autoría `romariorodriv <moises.rrr199@gmail.com>`. No se realizó push, PR, merge ni despliegue. `main` permanece en `fd4d2a8`.

Los documentos `hu-16-22-initial-audit.md` y `hu-16-22-plan.md` permanecen sin seguimiento y fuera de esta rama. No se ha iniciado ninguna de esas siete historias. Estos commits de recuperación no cuentan para su objetivo de 100 commits.

## Funcionalidades incluidas

- Login enlaza a /olvide-contrasena; /restablecer-contrasena lee el token del correo.
- Formularios con correo válido, contraseña de mínimo 8 caracteres y máximo 72 bytes UTF-8, confirmación, carga, errores, prevención de solicitudes simultáneas y regreso al login.
- Ausencia/formato inválido del token se detectan localmente; el backend valida uso y vencimiento.
- Contratos conservados: email; token y newPassword; respuestas message.
- Tokens aleatorios de 256 bits, hashes SHA-256, 15 minutos y consumo único. El cambio invalida todos los enlaces pendientes y los JWT anteriores.
- SMTP fuera de la respuesta HTTP y la transacción; cola acotada, timeouts, eliminación del token fallido y cooldown persistente aunque falle el proveedor.
- Logs del proveedor sin direcciones, tokens, cuerpos ni credenciales; JSON malformado responde 400 sin registrar fragmentos de contraseñas.
- Límites por IP/globales, confianza restringida al proxy local y limpieza periódica de tokens antiguos.
- Validación UTF-8 también en registro y login.
- Correo simulado en las pruebas. Se restauró la ejecución de diez pruebas al corregir la extensión de EmotionIntegrationTest,java.
- Se conservan CORS localhost/dominio AWS y apiUrl '/api'.

## Respaldo e aislamiento

Antes de crear la rama o preparar archivos se creó el respaldo externo:

`C:\Users\Romario\secondMind\Mentaiko-backups\password-reset-20261111T134223Z`

Contiene tracked.patch, status.txt, untracked.zip, copia de los archivos modificados/nuevos y manifest.json con SHA-256. Se verificaron las copias de 56 archivos y se registró el origen eliminado del renombrado. No se copiaron archivos ignorados como .env ni se modificaron credenciales.

El staging fue explícito. El hunk CORS de SecurityConfig.java se separó del filtro funcional. Se revisaron los archivos de cada commit y git diff --cached --check. No se utilizó git add . ni se descartaron modificaciones.

Durante la revisión se corrigió únicamente un carácter de codificación del mensaje de validación en register.component.ts. El respaldo conserva la versión anterior. Se actualizó este informe para reflejar el aislamiento; el código funcional restante se conserva.

## Migración

Aplicar database/002_password_reset_security.sql antes del JAR: añade users.auth_version y users.password_reset_requested_at. No modifica ni recrea password_reset_tokens. Es aditiva e idempotente.

La aplicación en producción requiere perfil prod y ddl-auto=validate. No se aplicó esta migración en AWS. En un rollback de artefactos, conservar las columnas nuevas.

## Dependencias

Angular framework/compiler-cli 22.2.5 y CLI/build 22.2.6: misma versión mayor. Vitest 4.1.12 y jsdom 27.4.0 se utilizan solo para pruebas. El lockfile coincide con package.json y resuelve paquetes desde registry.npmjs.org. No se utilizó force ni legacy-peer-deps.

La revisión anterior de npm audit reportó cero vulnerabilidades; no se repitió la consulta durante este aislamiento. Es un resultado fechado de la auditoría anterior, no una garantía permanente.

## Verificaciones ejecutadas durante el aislamiento

- Backend: mvnw.cmd clean package, sin propiedades SMTP manuales. BUILD SUCCESS; 100 pruebas descubiertas, 99 satisfactorias, una omitida por ser opt-in de PostgreSQL. Incluye compilación, pruebas y JAR.
- Angular: npm test, 19 pruebas satisfactorias.
- Angular: npm run build -- --configuration production, correcto.
- git diff --check y revisión del staging: sin errores.
- Las validaciones se ejecutan sobre el código del árbol local; no se afirma que se haya probado cada commit intermedio de forma aislada.
- La prueba opt-in PostgreSQL no se repitió en esta sesión. En el trabajo anterior pasó la migración repetida, arranque prod con validate y HTTP/proxy; también pasaron once pruebas de servicio en PostgreSQL 17 desechable. No se contactó producción.

Código funcional de referencia: `9655259b31a77812664215912c96517ea2dfeac4`. El commit de documentación posterior no altera código. Para consultar los commits y sus archivos: `git log --reverse --stat main..fix/password-reset-hardening`.

## Riesgos y validación pendiente

No se enviaron correos reales ni se probó el navegador con SMTP/servicio de producción. Angular utiliza DOM y HTTP simulados; el backend utiliza H2 en su suite normal. Falta una prueba de aceptación controlada tras un despliegue autorizado.

La cola y el limitador global viven en memoria; reinicios pueden perder solicitudes en cola y reinician la cuota IP, pero conservan el cooldown persistido. Varias instancias requerirían mecanismos compartidos.

El despliegue debe comprobar que Nginx sobrescribe X-Forwarded-For y excluye el token de logs. Un rollback hacia un JAR que ignore auth_version exige rotar JWT_SECRET para no volver a aceptar sesiones revocadas.

## Integración recomendada

Tras revisión del diff y los commits, solicitar autorización independiente para publicar la rama y crear un PR hacia main. Integrar mediante merge commit, conservando los commits individuales y sin squash ni reescritura de autorías. Volver a comprobar la base remota y los checks antes de integrar. No incluir los documentos de planificación sin seguimiento.

Variables y comandos PowerShell/Ubuntu, respaldos, migración, salud y rollback: [procedimiento](password-reset-deployment.md). Son instrucciones futuras; no se ejecutaron en AWS.

## Inventario del cambio funcional frente a main

El siguiente inventario usa detección de renombrados. Este informe y password-reset-deployment.md se incluyen adicionalmente en el commit de documentación.

```text
A	backend/src/main/java/com/mentaiko/backend/config/PasswordResetConfig.java
M	backend/src/main/java/com/mentaiko/backend/config/SecurityConfig.java
M	backend/src/main/java/com/mentaiko/backend/dto/auth/LoginRequest.java
M	backend/src/main/java/com/mentaiko/backend/dto/auth/RegisterRequest.java
M	backend/src/main/java/com/mentaiko/backend/dto/auth/ResetPasswordRequest.java
M	backend/src/main/java/com/mentaiko/backend/entity/User.java
M	backend/src/main/java/com/mentaiko/backend/exception/GlobalExceptionHandler.java
M	backend/src/main/java/com/mentaiko/backend/repository/PasswordResetTokenRepository.java
M	backend/src/main/java/com/mentaiko/backend/repository/UserRepository.java
M	backend/src/main/java/com/mentaiko/backend/security/JwtService.java
A	backend/src/main/java/com/mentaiko/backend/security/PasswordResetRateLimitFilter.java
A	backend/src/main/java/com/mentaiko/backend/security/PasswordResetRateLimiter.java
A	backend/src/main/java/com/mentaiko/backend/service/PasswordResetCleanup.java
M	backend/src/main/java/com/mentaiko/backend/service/PasswordResetService.java
A	backend/src/main/java/com/mentaiko/backend/validation/Utf8Password.java
A	backend/src/main/java/com/mentaiko/backend/validation/Utf8PasswordValidator.java
M	backend/src/main/resources/application-prod.properties
M	backend/src/main/resources/application.properties
M	backend/src/test/java/com/mentaiko/backend/MentaikoBackendApplication.java
M	backend/src/test/java/com/mentaiko/backend/config/AdminAccountInitializerLocalProfileTest.java
M	backend/src/test/java/com/mentaiko/backend/config/AdminAccountInitializerProdProfileTest.java
A	backend/src/test/java/com/mentaiko/backend/config/PostgresPasswordResetMigrationTest.java
A	backend/src/test/java/com/mentaiko/backend/config/TestMailConfiguration.java
M	backend/src/test/java/com/mentaiko/backend/controller/AdminDashboardIntegrationTest.java
M	backend/src/test/java/com/mentaiko/backend/controller/AdminIndicatorsIntegrationTest.java
M	backend/src/test/java/com/mentaiko/backend/controller/AuthIntegrationTest.java
M	backend/src/test/java/com/mentaiko/backend/controller/CheckinIntegrationTest.java
R099	backend/src/test/java/com/mentaiko/backend/controller/EmotionIntegrationTest,java	backend/src/test/java/com/mentaiko/backend/controller/EmotionIntegrationTest.java
M	backend/src/test/java/com/mentaiko/backend/controller/MicroActivityIntegrationTest.java
A	backend/src/test/java/com/mentaiko/backend/controller/PasswordResetIntegrationTest.java
M	backend/src/test/java/com/mentaiko/backend/controller/RecommendationIntegrationTest.java
M	backend/src/test/java/com/mentaiko/backend/controller/ReportIntegrationTest.java
M	backend/src/test/java/com/mentaiko/backend/security/JwtServiceTest.java
A	backend/src/test/java/com/mentaiko/backend/security/PasswordResetRateLimiterTest.java
A	backend/src/test/java/com/mentaiko/backend/service/PasswordResetServiceTest.java
M	backend/src/test/resources/application.properties
A	database/002_password_reset_security.sql
M	frontend/angular.json
M	frontend/package-lock.json
M	frontend/package.json
M	frontend/src/app/app.routes.ts
A	frontend/src/app/core/auth/auth.service.spec.ts
M	frontend/src/app/core/auth/auth.service.ts
A	frontend/src/app/features/auth/forgot-password/forgot-password.component.ts
M	frontend/src/app/features/auth/login/login.component.ts
A	frontend/src/app/features/auth/password-recovery.spec.ts
A	frontend/src/app/features/auth/password.validators.ts
M	frontend/src/app/features/auth/register/register.component.ts
A	frontend/src/app/features/auth/reset-password/reset-password.component.ts
M	frontend/src/environments/environment.ts
M	frontend/src/index.html
A	frontend/tsconfig.spec.json
```
