# Mentaiko

Plataforma web universitaria de bienestar emocional.

## Tecnologias

- Frontend: Angular standalone
- Backend: Spring Boot 4.1.1, Java 25
- Base de datos: PostgreSQL en ejecucion real, H2 en pruebas
- Seguridad: Spring Security, JWT y BCrypt

## Estructura

- `frontend`: aplicacion Angular
- `backend`: API REST Spring Boot
- `docs`: documentacion del proyecto

## Estado funcional

| HU | Estado |
| --- | --- |
| HU01 Registro de usuario | Implementada |
| HU02 Inicio de sesion | Implementada |
| HU03 Consultar y actualizar perfil | Implementada |
| HU04 Catalogo y administracion de emociones | Implementada |
| HU05 Crear check-in emocional | Implementada |
| HU06 Listar y filtrar check-ins | Implementada |
| HU07 Editar check-in propio | Implementada |
| HU08 Eliminar check-in propio | Implementada |
| HU09 Catalogo y administracion de microactividades | Implementada |
| HU10 Recomendaciones de bienestar | Implementada |
| HU11 Completar recomendaciones | Implementada |
| HU12 Reporte emocional semanal | Implementada |
| HU13 Distribucion emocional | Implementada |
| HU14 Indicadores administrativos anonimizados | Implementada |

## Verificacion

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm.cmd run build
```

El proyecto Angular no define target de pruebas en `angular.json`; la verificacion automatizada del frontend disponible es el build.

## Ejecucion local academica

La aplicacion usa el perfil `local` por defecto mediante `spring.profiles.default=local`.
Despues de hacer `git pull`, basta con levantar PostgreSQL y ejecutar el backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

No es necesario ejecutar `setx` ni declarar variables manuales en PowerShell para crear la cuenta administradora local. El perfil local carga `backend/src/main/resources/application-local.properties` y crea automaticamente una cuenta `ADMIN` si todavia no existe.

La cuenta administradora local es exclusivamente para demostracion academica. La credencial de demostracion se encuentra en `application-local.properties`, no debe reutilizarse en produccion y no debe copiarse a frontend, pantallas, logs ni documentacion publica adicional. Si el administrador ya existe, el inicializador no lo duplica.

## Variables backend

La aplicacion real requiere variables de entorno para PostgreSQL y JWT:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
```

`JWT_SECRET` debe tener al menos 32 bytes. No guardar secretos reales en Git.

## Produccion

Produccion debe usar el perfil `prod`, que no incluye credenciales administrativas reales por defecto. Se inicia con:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=prod"
```

Tambien puede activarse con:

```text
SPRING_PROFILES_ACTIVE=prod
```

En `prod`, el bootstrap administrativo se configura exclusivamente mediante variables de entorno:

```text
ADMIN_BOOTSTRAP_ENABLED
ADMIN_NAME
ADMIN_EMAIL
ADMIN_PASSWORD
ADMIN_BIRTH_DATE
ADMIN_UNIVERSITY
ADMIN_CAREER
```

No colocar valores administrativos reales como valores predeterminados del perfil productivo.

## Equipo

- Romario
- Oscar
- Rodrigo
- Pedro
- Martin
