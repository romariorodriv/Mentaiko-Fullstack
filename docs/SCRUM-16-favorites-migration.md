# SCRUM-16 / T53 — Migración de favoritas

La versión anterior utilizaba `favorite_micro_activities(micro_activity_id)`; el contrato oficial exige
`user_favorite_activities(activity_id)`, con UNIQUE(user_id, activity_id).

1. Hacer una copia de seguridad y consultar si existe alguna de las dos tablas.
2. Para una instalación nueva, ejecutar `database/012_user_favorite_activities.sql` después de crear
   `users` y `micro_activities`; no es necesario el script legado 010.
3. Para una instalación anterior, ejecutar 012 después del script 010. Renombra tabla y columna
   conservando ID, timestamps y asociaciones.
4. Si ambas tablas ya existen, la migración se detiene deliberadamente; no fusiona ni elimina datos.
5. Verificar `SELECT count(*) FROM user_favorite_activities`, asociaciones y restricciones.
6. Ejecutar el backend con perfil `prod` solamente después de migrar (Hibernate usa `ddl-auto=validate`).

No se ha aplicado esta migración en la base de datos Cloud del equipo.
