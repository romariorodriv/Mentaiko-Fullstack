# HU-35 — Asistente de bienestar

## Alcance
La página `/app/bienestar-chat` permite conversar con un asistente orientativo. Utiliza el proveedor Groq configurado en el backend de la HU-25, sin persistencia de conversaciones, migraciones ni servicios adicionales.

- Ruta autenticada: `POST /api/wellness/chat`.
- JSON de entrada: `{"message":"Estoy estresado y tengo 5 minutos","history":[]}`.
- Respuesta: `{"reply":"...","activity":{"id":1,"title":"...","description":"...","durationMinutes":5}}`; `activity` puede ser `null`.
- El usuario debe iniciar sesión. El historial de cliente se limita a seis turnos, mensajes de hasta 500 caracteres.
- El servidor solo permite actividades activas verificadas contra PostgreSQL; la IA no puede inventar un identificador de actividad válido.
- No se envían correo, notas personales de check-in ni identidad del usuario al proveedor. **El texto que el usuario escribe sí es enviado a Groq:** se advierte que no incluya información sensible.
- No se guardan conversaciones en la base de datos y se eliminan del componente al navegar o iniciar una conversación nueva.
- La respuesta es orientativa, no clínica, y nunca sustituye atención profesional o de emergencia.

## Activación
Reutiliza `GROQ_API_KEY`, `GROQ_RECOMMENDATION_MODEL` y `GROQ_BASE_URL` del backend, documentados en [la guía de recomendaciones](recommendation-provider.md).

## Pruebas
Backend: `cd backend && ./mvnw verify`.
Frontend: `cd frontend && npm ci && npm test -- --watch=false && npm run build -- --configuration production`.

Prueba funcional manual:
1. Iniciar sesión y abrir **Asistente de bienestar** en el menú.
2. Escribir: «Estoy en la biblioteca, tengo cinco minutos y me siento estresado».
3. Comprobar una respuesta breve y, si se selecciona una actividad, verificar que existe en el catálogo.
4. Enviar una segunda pregunta para comprobar el contexto.
5. Confirmar el tratamiento de error con una configuración de prueba sin Groq.
6. Confirmar en el panel Groq Usage una petición adicional para acreditar la conexión externa.

El catálogo de producción debe contener varias microactividades activas para obtener sugerencias variadas. Esta funcionalidad se añadió después de la planificación original de cuatro sprints; no afecta sus métricas históricas.
