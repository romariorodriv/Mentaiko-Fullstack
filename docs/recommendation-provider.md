# HU-25 / SCRUM-82 — Proveedor externo de recomendaciones

La historia HU-25 permite pedir otra microactividad desde la pantalla de actividades o check-ins. El endpoint existente es `POST /api/recommendations/{id}/alternative`; no se crean nuevas rutas ni tablas.

## Funcionamiento

1. Spring Boot comprueba que la recomendación pertenece al usuario y sigue pendiente.
2. El servicio filtra las microactividades válidas del catálogo y omite las ya sugeridas.
3. Si se indicó un motivo y existe una clave configurada, el proveedor selecciona el **número** de una opción de la lista (máximo 12).
4. El backend comprueba el número, descarta la recomendación anterior y guarda la nueva.
5. Si falta la clave, el servicio falla, vence el tiempo de espera o la respuesta no es válida, se elige una actividad mediante las reglas locales.

**Privacidad:** solo se envían emoción normalizada, intensidad, entorno, duración y categorías de actividades. No se envían correo, nombre, notas del estudiante ni identificadores de base de datos.

## Configuración gratuita para la demostración

Crear una clave en https://console.groq.com/keys y colocarla en variables de entorno **solo en el backend**, nunca en Angular, GitHub ni Postman compartido.

```properties
GROQ_API_KEY=colocar_clave_solo_en_el_entorno
GROQ_RECOMMENDATION_MODEL=openai/gpt-oss-20b
GROQ_BASE_URL=https://api.groq.com/openai/v1
```

El plan gratuito de Groq tiene cuotas y modelos disponibles sujetos a cambios. Comprobar el modelo y límites de la cuenta antes de exponer. Sin `GROQ_API_KEY`, la aplicación sigue funcionando con reglas locales.

La implementación reutiliza la dependencia Spring AI existente y el cliente compatible con OpenAI; no añade bibliotecas ni infraestructura.

## Pruebas

Ejecutar en la carpeta `backend`:

```powershell
.\mvnw.cmd test
```

En la aplicación de pruebas:

1. Iniciar sesión con una cuenta de prueba.
2. Crear un check-in y generar una recomendación.
3. Elegir **Pedir otra sugerencia**, seleccionar un motivo (por ejemplo `BIBLIOTECA`) y tiempo disponible.
4. Confirmar que se devuelve una actividad diferente, que la recomendación original queda descartada y que la nueva se registra.
5. Repetir con una clave ausente o inválida y verificar que el flujo sigue funcionando por reglas locales.

**Evidencia imprescindible para cerrar SCRUM-82:** una ejecución con clave real en la que se confirme la respuesta del servicio externo. Una respuesta exitosa al endpoint de Mentaiko por sí sola **no prueba** que se utilizó Groq, porque existe fallback; documentar la prueba del proveedor por separado y sin mostrar secretos. No marcar `Finalizado` basándose únicamente en pruebas simuladas.

En despliegue, añadir `GROQ_API_KEY` al entorno del servicio Spring Boot, reconstruir el JAR y reiniciar el servicio después de pruebas locales. El frontend no necesita cambios.
