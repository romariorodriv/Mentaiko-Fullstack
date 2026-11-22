# Arquitectura del frontend Mentaiko

La aplicación utiliza Angular 22 con componentes standalone y organización por responsabilidad.

## Directorios principales

- `src/app/components`: pantallas y componentes por módulo funcional (autenticación, bienestar y administración).
- `src/app/layouts`: estructuras visuales compartidas, incluido `app-shell`.
- `src/app/core/auth`: sesión y autenticación.
- `src/app/core/guards`: restricciones de navegación.
- `src/app/core/interceptors`: cabeceras HTTP y token JWT.
- `src/app/core/services`: integración con la API REST de Spring Boot.
- `src/app/shared/models`: tipos y contratos de datos reutilizables.
- `src/app/app.routes.ts`: rutas públicas, autenticadas y administrativas.

## Convención de cuatro archivos

Cada componente visual cuenta con:

- `nombre.component.ts`: clase, estado y comportamiento.
- `nombre.component.html`: plantilla Angular.
- `nombre.component.css`: estilos exclusivos del componente cuando los haya.
- `nombre.component.spec.ts`: prueba inicial del componente.

Los estilos compartidos siguen en `src/styles.css`. Los archivos CSS individuales que contienen solo un comentario son puntos de extensión y no duplican reglas globales.

## Organización

Las pantallas se agrupan por función dentro de `components` para mantener juntos sus cuatro archivos. `auth` agrupa login, registro y recuperación de contraseña; `admin` agrupa las pantallas administrativas. `landing` es una pantalla pública independiente de autenticación.

## Verificación

```bash
cd frontend
npm ci
npm test -- --watch=false
npm run build -- --configuration production
```

Las pruebas de componentes agregadas por esta reorganización verifican inicialmente que puedan instanciarse; los flujos de negocio y sus interacciones requieren pruebas adicionales. La reorganización no modifica endpoints, modelos de datos ni lógica del servidor.
