# Base de integracion de autenticacion Mentaiko

Fecha: 2026-09-06

## Alcance implementado

Esta etapa cierra la base entre Spring Boot y Angular para:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`
- `PUT /api/users/me`
- `GET /api/health`

No se implementaron emociones, check-ins, microactividades, recomendaciones, reportes ni indicadores.

## Arquitectura backend

- `UserController` expone registro, login y perfil.
- `UserService` contiene normalizacion de email, validacion de duplicados, BCrypt, login, generacion JWT y actualizacion de perfil.
- `UserRepository` persiste usuarios con JPA.
- `JwtService` genera y valida JWT firmados.
- `JwtAuthenticationFilter` extrae `Authorization: Bearer <token>`, valida firma/expiracion, carga usuario activo y llena `SecurityContext`.
- `SecurityConfig` define API stateless, CORS, rutas publicas, rutas autenticadas y `/api/admin/**` solo para `ADMIN`.
- `GlobalExceptionHandler` estandariza errores JSON con `message` y `errors`.

La API usa JWT stateless, por eso CSRF esta deshabilitado: no hay sesion ni cookies de autenticacion que el navegador envie automaticamente.

## Contratos finales

### Health

```http
GET /api/health
```

```json
{
  "status": "ok",
  "service": "mentaiko-backend"
}
```

### Registro

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "name": "Usuario de prueba",
  "email": "usuario@example.com",
  "password": "Clave123",
  "birthDate": "2000-05-20",
  "university": "UPC",
  "career": "Ingenieria de Software"
}
```

Respuesta `201`:

```json
{
  "id": 1,
  "name": "Usuario de prueba",
  "email": "usuario@example.com",
  "birthDate": "2000-05-20",
  "university": "UPC",
  "career": "Ingenieria de Software",
  "role": "USER",
  "active": true,
  "createdAt": "2026-09-06T12:00:00"
}
```

El backend siempre asigna `USER`; el frontend no puede elegir rol. La contrasena se guarda cifrada con BCrypt y nunca se responde `password` ni `passwordHash`.

### Login

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "usuario@example.com",
  "password": "Clave123"
}
```

Respuesta `200`:

```json
{
  "token": "jwt-generado",
  "user": {
    "id": 1,
    "name": "Usuario de prueba",
    "email": "usuario@example.com",
    "birthDate": "2000-05-20",
    "university": "UPC",
    "career": "Ingenieria de Software",
    "role": "USER",
    "active": true,
    "createdAt": "2026-09-06T12:00:00"
  }
}
```

El JWT incluye:

- `sub`: email normalizado.
- `role`: `USER` o `ADMIN`.
- `iat`: fecha de emision.
- `exp`: expiracion.

`JWT_EXPIRATION` se interpreta en milisegundos. Valor sugerido para desarrollo: `86400000`.

### Consultar perfil

```http
GET /api/users/me
Authorization: Bearer <token>
```

Respuesta `200`: `UserProfile`.

### Actualizar perfil

```http
PUT /api/users/me
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "name": "Nombre actualizado",
  "university": "UPC",
  "career": "Ingenieria de Software"
}
```

El usuario se obtiene del JWT. Este endpoint no permite cambiar `id`, `email`, `passwordHash`, `role`, `active` ni `createdAt`.

## Errores

Formato:

```json
{
  "timestamp": "2026-09-06T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Los datos enviados no son validos",
  "path": "/api/auth/register",
  "errors": {
    "email": "debe tener formato de direccion de correo electronico"
  }
}
```

Estados relevantes:

- `400`: validaciones.
- `401`: token ausente, invalido o credenciales incorrectas.
- `403`: usuario desactivado o acceso prohibido.
- `404`: recurso no encontrado.
- `409`: email duplicado.
- `500`: error inesperado sin stack trace.

## Variables necesarias

La aplicacion real usa PostgreSQL y requiere variables de entorno:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
```

Ejemplo de URL:

```text
jdbc:postgresql://localhost:5432/mentaiko_db
```

`JWT_SECRET` debe tener al menos 32 bytes. No se debe guardar un secreto real en Git.

PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/mentaiko_db"
$env:DB_USERNAME="tu_usuario"
$env:DB_PASSWORD="tu_password"
$env:JWT_SECRET="un-secreto-local-de-al-menos-32-bytes"
$env:JWT_EXPIRATION="86400000"
```

Spring Boot no carga automaticamente `.env`; ese archivo es solo una plantilla de referencia.

## PostgreSQL local

1. Iniciar PostgreSQL local.
2. Confirmar que existe la base `mentaiko_db`.
3. Definir las variables de entorno en la misma terminal.
4. Ejecutar el backend.

No se agregaron migraciones Flyway/Liquibase en esta etapa. El proyecto sigue usando `spring.jpa.hibernate.ddl-auto=update`.

Si ya existen usuarios con rol `STUDENT`, el codigo puede leerlos como `USER` mediante `RoleConverter`. Para limpiar esos datos se recomienda una migracion explicita posterior, aprobada antes de ejecutarse.

## Comandos

Backend:

```powershell
cd "C:\Users\Romario\secondMind\metaiko arquitectura web\back\mentaiko-backend"
.\mvnw.cmd test
mvn clean test
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd "C:\Users\Romario\secondMind\metaiko arquitectura web\mentaiko-angular-frontend"
npm run build
npm start
```

Tests frontend:

```powershell
npm test -- --help
npm test
```

El proyecto Angular actual no tiene target `test` en `angular.json`, por eso `npm test` devuelve `Cannot determine project or target for command.`

## Frontend

- `RegisterComponent` envia `name`, `email`, `password`, `birthDate`, `university`, `career`.
- `AuthService` espera `{ token, user }`, guarda solo `mentaiko_token` y mantiene el perfil en memoria.
- `authInterceptor` agrega `Authorization: Bearer <token>`.
- `authGuard` exige token decodificable y no expirado.
- `adminGuard` exige rol `ADMIN`; el backend vuelve a validar `/api/admin/**`.
- `ProfileComponent` carga `GET /api/users/me` y guarda `PUT /api/users/me`.

## Resultados de verificacion

Backend:

- `mvn test`: 14 tests, 0 fallos.
- `.\mvnw.cmd test`: 14 tests, 0 fallos.
- `mvn clean test`: 14 tests, 0 fallos.

Cobertura funcional backend:

- Registro correcto.
- Email invalido.
- Email duplicado.
- Password corta.
- Fecha futura.
- Campos obligatorios.
- Password cifrada.
- Responses sin hash.
- Rol inicial `USER`.
- Login correcto.
- Email inexistente.
- Password incorrecta.
- Usuario desactivado.
- JWT generado, validado, firma invalida y expiracion.
- Health publico.
- `/api/users/me` 401 sin token o con token invalido.
- `/api/users/me` 200 con token valido.
- PUT perfil solo campos permitidos.
- `USER` bloqueado en `/api/admin/**`.
- `ADMIN` supera autorizacion y recibe 404 si endpoint futuro no existe.
- CORS preflight desde `http://localhost:4200`.

Frontend:

- `npm run build`: exitoso.
- `npm test -- --help`: exitoso.
- `npm test`: bloqueado por configuracion del proyecto, no existe target `test`.

## Bloqueos pendientes

- Prueba PostgreSQL E2E real bloqueada porque en este entorno no estan configuradas `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` ni `JWT_EXPIRATION`.
- No se creo usuario de prueba real en PostgreSQL.
- No se ejecuto recorrido visual en Angular contra backend real por el mismo bloqueo de configuracion.

## Proxima vertical

Con esta base, la siguiente vertical recomendada es catalogo de emociones y check-ins:

1. Entidad y endpoint de emociones.
2. CRUD admin de emociones.
3. Entidad de check-ins asociada al usuario autenticado.
4. Crear/listar/filtrar/editar/eliminar check-ins propios.
5. Tests de propiedad por usuario y validacion de intensidad.
