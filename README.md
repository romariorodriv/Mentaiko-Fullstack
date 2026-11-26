# Mentaiko

Aplicación web para registrar el bienestar emocional y consultar microactividades. Está desarrollada con Angular, Spring Boot y PostgreSQL.

## Tecnologías

| Capa | Tecnología |
| --- | --- |
| Frontend | Angular 22, TypeScript y CSS |
| Backend | Java 25, Spring Boot 4.1.1 y Spring Security |
| Datos | PostgreSQL; H2 para pruebas automatizadas |
| Autenticación | JWT y BCrypt |
| Pruebas | Vitest, JUnit y GitHub Actions |

## Estructura del repositorio

```text
mentaiko/
├── backend/       API REST, reglas de negocio, persistencia y pruebas
├── frontend/      Aplicación Angular y pruebas de componentes
├── database/      Scripts SQL de evolución del esquema
├── docs/          Procedimientos de despliegue y migraciones
└── .github/       Integración continua
```

En Angular, las pantallas están en `frontend/src/app/components`, los clientes HTTP y la autenticación en `core`, el diseño compartido en `layouts` y los contratos de datos en `shared`. Consulta [la arquitectura del frontend](frontend/docs/FRONTEND_ARCHITECTURE.md) para más detalles.

## Funcionalidades

- Registro, inicio de sesión, perfil y recuperación de contraseña.
- Catálogo de emociones y check-ins con historial, filtros y edición.
- Microactividades, preferencias, favoritos y recomendaciones según el tiempo disponible.
- Actividades guiadas, registro de finalización, valoración y nuevas sugerencias; historial de actividades realizadas con su valoración y opción de repetirlas en modo guiado.
- Seguimiento de hábitos, objetivos, calendario emocional y reportes.
- Administración de emociones, actividades, reglas de recomendación e indicadores.

El avance por historia de usuario se gestiona en Jira. Una funcionalidad integrada en `main` puede requerir todavía migraciones o validación en el entorno desplegado.

## Ejecución local

### Requisitos

- Java 25.
- Node.js compatible con Angular 22 y npm.
- PostgreSQL con una base de datos `mentaiko_db`.

Toma [backend/.env.example](backend/.env.example) como referencia para las variables de entorno; los secretos reales no se versionan. Por defecto se utiliza el perfil `local`. El archivo `backend/src/main/resources/application.properties` contiene los valores iniciales de conexión a PostgreSQL, así que verifica allí la URL y el usuario si tu instalación utiliza otros valores. `DB_PASSWORD` y `JWT_SECRET` se suministran por configuración externa.

Backend (Windows PowerShell):

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend (desde otra terminal):

```powershell
cd frontend
npm ci
npm start
```

Frontend: `http://localhost:4200`. La API local escucha en el puerto `8080`; consulta la configuración de proxy y `src/environments/environment.ts` antes de asumir una URL distinta.

## Pruebas

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
npm ci
npm test -- --watch=false
npm run build -- --configuration production
```

El pipeline está definido en [GitHub Actions](.github/workflows/wellbeing-ci.yml). Las pruebas automatizadas no reemplazan las pruebas de aceptación en un despliegue real.

## Base de datos y despliegue

Los scripts de `database/` **no deben ejecutarse todos automáticamente por orden alfabético**. Existen versiones de distintas ramas y algunas incluyen migraciones de datos. Antes de desplegar, revisa [la guía de migraciones](docs/database-migrations.md), realiza una copia de seguridad y confirma el esquema actual.

La configuración productiva usa el perfil `prod`, validación del esquema y variables de entorno para las credenciales. No publicar claves, contraseñas ni tokens en este repositorio.

## Documentación

- [Frontend: puesta en marcha](frontend/README.md)
- [Frontend: arquitectura](frontend/docs/FRONTEND_ARCHITECTURE.md)
- [Contratos HTTP](frontend/API-CONTRACT.md)
- [Migraciones de base de datos](docs/database-migrations.md)
- [Despliegue de recuperación de contraseña](docs/password-reset-deployment.md)

Los informes de implementación antiguos se conservan como antecedentes, pero no describen necesariamente el estado actual de `main`.

## Configuración segura del administrador local

El perfil `local` no crea administradores automáticamente. Para crear el primer administrador en una base de pruebas, establece `ADMIN_BOOTSTRAP_ENABLED=true` y proporciona `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_BIRTH_DATE`, `ADMIN_UNIVERSITY` y `ADMIN_CAREER` por variables de entorno. No guardes credenciales en archivos versionados. Si se reutilizó una clave incluida en una versión anterior del repositorio, cámbiala en todos los entornos afectados; eliminarla del archivo actual no la borra del historial.

## Exportación de información personal (HU-31)

Desde Perfil, el usuario puede descargar un archivo JSON con sus check-ins y recomendaciones personales. Puede filtrar por fechas y decidir si incluye notas privadas; el valor predeterminado es excluirlas. No se requiere una nueva migración. El endpoint es `GET /api/users/me/export`. Consulta el contrato HTTP para los detalles.
