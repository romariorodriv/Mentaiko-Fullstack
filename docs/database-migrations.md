# Migraciones de PostgreSQL

Los archivos SQL de `database/` documentan cambios incrementales del esquema. En el proyecto actual se aplican de forma controlada; no se utiliza un mecanismo automático de migraciones versionadas como Flyway o Liquibase.

## Antes de aplicar cambios

1. Identifica el commit y la versión del backend que vas a desplegar.
2. Haz una copia de seguridad verificable de PostgreSQL.
3. Comprueba tablas, columnas, restricciones y migraciones ya aplicadas.
4. Lee el script completo y sus condiciones de ejecución.
5. Ejecuta únicamente los scripts necesarios, en un entorno controlado.
6. Comprueba el esquema y después inicia Spring Boot con perfil `prod` (`ddl-auto=validate`).

No ejecutar todas las migraciones de esta carpeta de forma indiscriminada. Algunas están diseñadas para instalaciones previas o incluyen operaciones de migración de datos.

## Scripts presentes

| Archivo | Propósito |
| --- | --- |
| `001_password_reset_tokens.sql` y `002_password_reset_security.sql` | Recuperación segura de contraseñas |
| `003_weekly_goals.sql` a `009_activity_preferences.sql` | Objetivos, recordatorios, calendario, factores y hábitos |
| `010_activity_feedback.sql` | Valoración de recomendaciones |
| `010_favorite_micro_activities.sql` | Estructura anterior de favoritos |
| `011_activity_post_intensity.sql` | Intensidad posterior a la actividad |
| `012_user_favorite_activities.sql` | Estructura actual de favoritos y migración de datos anteriores |
| `012_activity_type_descanso_ocular.sql` | Categoría de descanso ocular |
| `012_recommendation_alternatives.sql` | Recomendaciones descartadas y alternativas |
| `013_focus_sessions.sql` | Sesiones de actividades guiadas |
| `014_admin_audit_events.sql` | Auditoría de cambios administrativos HU-34 (aplicar antes del nuevo backend) |

## Numeraciones duplicadas

Los prefijos `010` y `012` aparecen en varios archivos porque proceden de desarrollos independientes. **El número del archivo no determina su ejecución.** No renombres ni reejecutes scripts sin comprobar su historial y dependencias en PostgreSQL.

Para favoritos, sigue [el procedimiento específico](SCRUM-16-favorites-migration.md); distingue entre instalaciones nuevas y bases con datos existentes. Para recuperación de contraseñas, sigue [su procedimiento de despliegue](password-reset-deployment.md).

## Despliegue

El merge de un PR en GitHub **no actualiza** la base de datos de AWS. Valida las migraciones pendientes contra el esquema de producción antes de desplegar el JAR. Conserva un registro de fecha, responsable, script y resultado de cada ejecución.

## HU-32 y HU-34: precauciones especiales

- HU-32 elimina registros relacionados dentro de una única transacción. Antes de desplegar, contrastar en PostgreSQL las FK efectivas que referencian `users` y `checkins`; no se debe asumir cascada sin verificar `pg_constraint`.
- HU-34 requiere aplicar `014_admin_audit_events.sql` manualmente antes de iniciar el backend con `ddl-auto=validate`. No altera los eventos históricos anteriores a la instalación.
- El identificador numérico de administrador permanece para la trazabilidad incluso si su usuario se elimina; no se almacenan contraseñas ni correos en la tabla de auditoría.
- Preparar backup y ventana de mantenimiento; este PR no debe ejecutar migraciones ni despliegues automáticamente.
