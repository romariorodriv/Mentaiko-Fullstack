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

## Equipo

- Romario
- Oscar
- Rodrigo
- Pedro
- Martin
