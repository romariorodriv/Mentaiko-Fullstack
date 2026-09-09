# Contrato API esperado por el frontend

Base local: `http://localhost:8080/api`

| HU | Metodo | Endpoint | Uso |
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
| 10 | POST | `/recommendations` | Generar recomendacion |
| 10 | GET | `/recommendations/me` | Consultar recomendaciones propias |
| 11 | POST | `/recommendations/{id}/complete` | Completar actividad |
| 12 | GET | `/reports/weekly?week=YYYY-Www` | Resumen semanal |
| 13 | GET | `/reports/distribution?from=&to=` | Distribucion personal |
| 14 | GET | `/admin/indicators` | Indicadores anonimizados |

## Respuestas importantes

- Login: `{ "token": "...", "user": { ... } }`
- Listado de check-ins: formato paginado `{ content, totalElements, totalPages, number, size }`
- Roles JWT: el frontend reconoce `role`, `roles[0]` o `authorities[0]`, con o sin prefijo `ROLE_`.
- Los errores deberian devolver `{ status, message, errors? }`.

## HU05 - Crear check-in emocional

### Crear check-in

```http
POST /api/checkins
Authorization: Bearer <token>
Content-Type: application/json
```

Request:

```json
{
  "emotionId": 1,
  "intensity": 4,
  "context": "Estudios",
  "note": "Hoy tuve una presentacion"
}
```

Reglas:

- `emotionId` es obligatorio y debe existir.
- La emocion seleccionada debe estar activa.
- `intensity` es obligatoria y usa rango `1` a `5`.
- `context` es obligatorio, maximo 100 caracteres.
- `note` es opcional, maximo 500 caracteres.
- El usuario se toma del JWT; el frontend no debe enviar `userId`.

Respuesta `201 Created`:

```json
{
  "id": 1,
  "emotion": {
    "id": 1,
    "name": "Ansiedad"
  },
  "intensity": 4,
  "context": "Estudios",
  "note": "Hoy tuve una presentacion",
  "createdAt": "2026-09-08T10:30:00"
}
```

Errores:

- `400`: validacion de campos o emocion inactiva.
- `401`: token ausente, invalido o usuario inactivo.
- `404`: usuario o emocion no encontrada.

Prueba manual PowerShell:

```powershell
$token = "pega-aqui-un-jwt-valido"
$body = @{
  emotionId = 1
  intensity = 4
  context = "Estudios"
  note = "Hoy tuve una presentacion"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/checkins" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body $body
```

Archivos principales:

- Backend: `EmotionalEntry`, `EmotionalEntryRepository`, `CreateCheckinRequest`, `CheckinResponse`, `CheckinService`, `CheckinController`.
- Frontend: `CheckinsComponent`, `ApiService.createCheckin`, `CheckinRequest`, `Checkin`.

## HU06 - Consultar historial de check-ins

### Listar check-ins propios

```http
GET /api/checkins?from=2026-09-01&to=2026-09-08&context=Estudios&page=0&size=10
Authorization: Bearer <token>
```

Query parameters:

- `from`: opcional, fecha inicial `YYYY-MM-DD`. Incluye el comienzo del dia.
- `to`: opcional, fecha final `YYYY-MM-DD`. Incluye el dia completo.
- `context`: opcional, busqueda parcial sin distinguir mayusculas.
- `page`: opcional, empieza en `0`. No puede ser negativo.
- `size`: opcional, por defecto `10`, maximo `50`.

Orden:

- `createdAt DESC`, del mas reciente al mas antiguo.
- Las fechas se interpretan como dias del negocio en zona `America/Lima`.

Respuesta `200 OK`:

```json
{
  "content": [
    {
      "id": 1,
      "emotion": {
        "id": 2,
        "name": "Ansiedad"
      },
      "intensity": 4,
      "context": "Estudios",
      "note": "Tuve una presentacion",
      "createdAt": "2026-09-08T18:30:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 10
}
```

Errores:

- `400`: rango de fechas invalido o paginacion invalida.
- `401`: token ausente, invalido o usuario inactivo.
- `404`: usuario autenticado no encontrado.

Prueba manual PowerShell:

```powershell
$token = "pega-aqui-un-jwt-valido"

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/checkins?from=2026-09-01&to=2026-09-08&context=Estudios&page=0&size=10" `
  -Headers @{ Authorization = "Bearer $token" }
```
