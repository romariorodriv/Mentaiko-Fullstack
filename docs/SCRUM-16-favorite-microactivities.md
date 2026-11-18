# SCRUM-16 — Guardar microactividades favoritas

## Alcance

La selección de tipos preferidos `/api/me/activity-preferences` es una función diferente de los favoritos individuales. Esta historia permite al usuario autenticado guardar microactividades concretas de su catálogo y retirarlas posteriormente.

## Contrato API

| Método | Endpoint | Resultado |
| --- | --- | --- |
| GET | `/api/me/favorite-micro-activities` | Lista de favoritas del usuario autenticado |
| PUT | `/api/me/favorite-micro-activities/{activityId}` | Guarda o devuelve una favorita existente sin duplicarla |
| DELETE | `/api/me/favorite-micro-activities/{activityId}` | Elimina de favoritas para el usuario actual; responde 204 |

Los tres endpoints requieren JWT válido, no reciben identificadores de usuario, y obtienen la identidad de `Authentication`. Guardar una microactividad inexistente o inactiva responde 404. La vista puede mostrar favoritas que un administrador desactivó después de haberlas guardado.

## Base de datos

`favorite_micro_activities` guarda `user_id`, `micro_activity_id` y la fecha. La restricción única compuesta impide duplicaciones por usuario. En PostgreSQL, si no se usa `ddl-auto=update`, ejecutar `database/010_favorite_micro_activities.sql` una vez, después de crear `users` y `micro_activities`.

## Verificación

- Backend: `cd backend && ./mvnw test` (PowerShell: `.\\mvnw.cmd test`).
- Frontend: `cd frontend && npm test -- --watch=false && npm run build`.
- Manual: iniciar sesión como dos usuarios, guardar la misma actividad, verificar aislamiento, retirar y volver a guardar.
- Comprobar que una actividad desactivada no puede agregarse como favorita nueva.

## Trazabilidad

Historia de Jira: SCRUM-16 / HU-24.
Código: `FavoriteMicroActivityController` → `FavoriteMicroActivityService` → `FavoriteMicroActivityRepository` → PostgreSQL.
Interfaz: catálogo de microactividades y `/app/favoritas`.

## Pendiente de validación académica

Los criterios de aceptación completos de Jira no estaban accesibles al elaborar esta implementación. Confirmar el alcance exacto con el equipo antes de fusionar. La evidencia definitiva depende de un flujo probado en ejecución real y del CI satisfactorio.
