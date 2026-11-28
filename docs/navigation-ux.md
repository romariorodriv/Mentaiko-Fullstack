# HU-36 — Navegación de Mentaiko

Objetivo: hacer más fácil encontrar las herramientas sin eliminar funcionalidades.

## Grupos
- **Inicio:** Resumen
- **Mi bienestar:** Mis check-ins, Asistente de bienestar, Microactividades, Mi historial de actividades
- **Mi seguimiento:** Calendario emocional, Mi meta semanal, Mi sueño, Mi energía, Factores emocionales, Recordatorios
- **Preferencias:** Preferencias de actividades, Mi perfil
- **Administración:** Dashboard, Emociones, Actividades, Reglas de recomendación, Auditoría de cambios, Indicadores (solo rol ADMIN)

## Alcance
Cambios exclusivamente en `frontend/src/app/layouts/app-shell/`. No se modifica ningún endpoint, URL, guard, servicio, esquema, componente de contenido o registro de usuario. La navegación conserva los 13 destinos del estudiante y los 6 destinos administrativos. Se ajusta la altura y el desplazamiento del menú para pantallas pequeñas, se mantienen estilos de foco accesible y se cierran los menús móviles tras seleccionar una ruta.

## Validación
`cd frontend && npm ci && npm test -- --watch=false && npm run build -- --configuration production`.

Comprobar manualmente en escritorio y móvil: navegar a cada destino, mantener la sección activa visible, acceder a todas las secciones desplazándose por el menú, validar el rol administrador, confirmar disponibilidad de ayuda y cerrar sesión. Actualizar únicamente los archivos estáticos del frontend en EC2 después de aprobar el pipeline. No se requiere reiniciar Spring Boot.
