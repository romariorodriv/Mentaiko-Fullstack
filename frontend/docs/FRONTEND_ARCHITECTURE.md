# Arquitectura del frontend Mentaiko

La aplicación utiliza Angular 22 con componentes standalone y organización por responsabilidad.

## Directorios principales

- `src/app/components`: pantallas y componentes por módulo funcional (autenticación, bienestar y administración).
- `src/app/layouts`: estructuras visuales compartidas, incluido `app-shell`.
- `src/app/core/auth`: sesión y autenticación.
- `src/app/core/guards`: restricciones de navegación.
- `src/app/core/interceptors`: cabeceras HTTP y token JWT.
- `src/app/core/services`: clientes HTTP especializados para la API REST de Spring Boot (emociones, check-ins, actividades, recomendaciones, reportes y administración), junto a los servicios de hábitos y preferencias.
- `src/app/shared/models`: tipos y contratos de datos reutilizables.
- `src/app/app.routes.ts`: rutas públicas, autenticadas y administrativas.

## Convención de cuatro archivos

Cada componente visual cuenta con:

- `nombre.component.ts`: clase, estado y comportamiento.
- `nombre.component.html`: plantilla Angular.
- `nombre.component.css`: estilos exclusivos del componente cuando los haya.
- `nombre.component.spec.ts`: prueba inicial del componente.

Los estilos compartidos siguen en `src/styles.css`. Cuando un componente no necesita reglas propias, su archivo CSS puede permanecer vacío. Los estilos generales se mantienen en `src/styles.css`.

## Organización

Las pantallas se agrupan por función dentro de `components` para mantener juntos sus cuatro archivos. `auth` agrupa login, registro y recuperación de contraseña; `admin` agrupa las pantallas administrativas. `landing` es una pantalla pública independiente de autenticación.

## Verificación

```bash
cd frontend
npm ci
npm test -- --watch=false
npm run build -- --configuration production
```

Las pruebas de componentes cubren tanto la creación de vistas como distintos flujos de interacción. La cobertura de aceptación en navegador y la integración con el servidor desplegado se validan por separado.

## Organización de core

```text
core/
├── auth/
│   ├── auth.service.ts
│   └── auth.service.spec.ts
├── guards/
│   ├── auth.guard.ts
│   └── admin.guard.ts
├── interceptors/
│   └── auth.interceptor.ts
└── services/
    ├── emotions.service.ts
    ├── checkins.service.ts
    ├── activities.service.ts
    ├── recommendations.service.ts
    ├── reports.service.ts
    ├── admin.service.ts
    ├── daily-wellbeing.service.ts
    ├── emotional-factors-api.service.ts
    ├── weekly-goal-api.service.ts
    ├── reminder-api.service.ts
    ├── activity-preferences-api.service.ts
    └── domain-services.spec.ts
```

Los servicios HTTP están separados por dominio. Los componentes importan únicamente los servicios necesarios para su pantalla. `core/auth` conserva el tratamiento de sesión; los guards e interceptores continúan separados.

Los componentes Angular requieren archivos `.ts`, `.html`, `.css` y `.spec.ts`; los servicios, guards e interceptores no tienen plantilla ni estilos porque no representan vistas. Los contratos HTTP de los seis servicios especializados se verifican en `domain-services.spec.ts`.
