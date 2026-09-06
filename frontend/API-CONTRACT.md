# Contrato API esperado por el frontend

Base local: `http://localhost:8080/api`

| HU | Método | Endpoint | Uso |
|---|---|---|---|
| 01 | POST | `/auth/register` | Crear usuario |
| 02 | POST | `/auth/login` | Obtener JWT |
| 03 | GET, PUT | `/users/me` | Consultar/actualizar perfil |
| 04 | GET | `/emotions?activeOnly=true` | Consultar emociones |
| 04 | POST, PUT | `/admin/emotions`, `/admin/emotions/{id}` | Administrar emociones |
| 05 | POST | `/checkins` | Crear check-in |
| 06 | GET | `/checkins` | Listar y filtrar check-ins |
| 07 | PUT | `/checkins/{id}` | Actualizar check-in propio |
| 08 | DELETE | `/checkins/{id}` | Eliminar check-in propio |
| 09 | GET | `/micro-activities?activeOnly=true` | Consultar actividades |
| 09 | POST, PUT | `/admin/micro-activities`, `/admin/micro-activities/{id}` | Administrar actividades |
| 10 | POST | `/recommendations` | Generar recomendación |
| 10 | GET | `/recommendations/me` | Consultar recomendaciones propias |
| 11 | POST | `/recommendations/{id}/complete` | Completar actividad |
| 12 | GET | `/reports/weekly?week=YYYY-Www` | Resumen semanal |
| 13 | GET | `/reports/distribution?from=&to=` | Distribución personal |
| 14 | GET | `/admin/indicators` | Indicadores anonimizados |

## Respuestas importantes

- Login: `{ "token": "...", "user": { ... } }`
- Listado de check-ins: formato paginado `{ content, totalElements, totalPages, number, size }`
- Roles JWT: el frontend reconoce `role`, `roles[0]` o `authorities[0]`, con o sin prefijo `ROLE_`.
- Los errores deberían devolver `{ status, message, errors? }`.
