# Arquitectura del frontend Mentaiko

Este frontend Angular usa componentes standalone y una organizacion por responsabilidades. La idea principal es que cada archivo este cerca de la parte del producto a la que pertenece, sin mezclar pantallas, infraestructura y modelos compartidos en una sola carpeta.

## Estructura general

```text
src/app
|-- core
|   |-- auth
|   |-- guards
|   |-- interceptors
|   `-- services
|-- features
|   |-- activities
|   |-- admin
|   |-- auth
|   |-- checkins
|   |-- dashboard
|   |-- landing
|   `-- profile
|-- layouts
|   `-- app-shell
|-- shared
|   `-- models
|-- app.component.ts
|-- app.config.ts
`-- app.routes.ts
```

## Responsabilidad de `core`

`core` contiene la infraestructura global de la aplicacion. No contiene pantallas.

- `core/auth`: servicio de autenticacion y manejo de sesion.
- `core/guards`: reglas de acceso para rutas autenticadas y rutas de administrador.
- `core/interceptors`: interceptor HTTP que adjunta el token JWT.
- `core/services`: servicios generales que comunican el frontend con la API Spring Boot.

## Responsabilidad de `shared`

`shared` contiene elementos reutilizados por varias funcionalidades. En este proyecto, los modelos se usan desde servicios y pantallas diferentes, por eso viven en `shared/models`.

No todo debe ir en `shared`. Si un componente o servicio solo pertenece a una funcionalidad, debe quedarse dentro de esa funcionalidad.

## Responsabilidad de `layouts`

`layouts` contiene estructuras de pagina que organizan otras pantallas. El `app-shell` es el layout de la zona autenticada: incluye sidebar, topbar y el `router-outlet` donde cargan dashboard, check-ins, actividades, perfil y administracion.

## Responsabilidad de `features`

`features` agrupa las pantallas por funcionalidad del producto:

- `landing`: pagina publica inicial.
- `auth/login`: inicio de sesion.
- `auth/register`: registro.
- `dashboard`: resumen del usuario.
- `checkins`: registro e historial de check-ins emocionales.
- `activities`: microactividades y recomendaciones.
- `profile`: perfil del usuario.
- `admin`: pantallas administrativas agrupadas por tema.

## Donde crear una nueva pantalla

Una nueva pantalla debe crearse dentro de `features`, en la carpeta de su funcionalidad. Por ejemplo, una pantalla de historial separada podria vivir en:

```text
src/app/features/checkins/history/history.component.ts
```

Luego se conecta desde `app.routes.ts` con `loadComponent`.

## Donde crear un servicio

Si el servicio es global o se comparte entre varias funcionalidades, debe ir en `core/services`.

Si el servicio pertenece solo a una funcionalidad, puede ir dentro de esa feature. Por ejemplo:

```text
src/app/features/checkins/checkins.service.ts
```

En el estado actual, `ApiService` sigue siendo global porque concentra la comunicacion con varios endpoints usados por varias pantallas.

## Donde crear un modelo

Si el modelo se usa en varias partes, debe ir en `shared/models`.

Si el modelo solo pertenece a una funcionalidad, debe vivir en esa funcionalidad. Por ejemplo:

```text
src/app/features/activities/activity.models.ts
```

Actualmente los modelos se conservan juntos en `shared/models/models.ts` porque son compartidos por servicios, dashboard, check-ins, actividades, perfil y administracion.

## Relacion entre rutas y funcionalidades

`app.routes.ts` es el mapa principal de navegacion. Cada ruta carga un componente desde su carpeta de feature.

```text
/                         -> features/landing
/login                    -> features/auth/login
/registro                 -> features/auth/register
/app/dashboard            -> features/dashboard
/app/checkins             -> features/checkins
/app/actividades          -> features/activities
/app/perfil               -> features/profile
/app/admin/emociones      -> features/admin/emotions
/app/admin/actividades    -> features/admin/activities
/app/admin/indicadores    -> features/admin/indicators
```

Las rutas bajo `/app` usan `AppShellComponent` como layout y estan protegidas con `authGuard`. Las rutas administrativas tambien usan `adminGuard`.

## Arbol final real

```text
src/app
|-- app.component.ts
|-- app.config.ts
|-- app.routes.ts
|-- core
|   |-- auth
|   |   `-- auth.service.ts
|   |-- guards
|   |   |-- admin.guard.ts
|   |   `-- auth.guard.ts
|   |-- interceptors
|   |   `-- auth.interceptor.ts
|   `-- services
|       `-- api.service.ts
|-- features
|   |-- activities
|   |   `-- activities.component.ts
|   |-- admin
|   |   |-- activities
|   |   |   `-- admin-activities.component.ts
|   |   |-- emotions
|   |   |   `-- admin-emotions.component.ts
|   |   `-- indicators
|   |       `-- admin-indicators.component.ts
|   |-- auth
|   |   |-- login
|   |   |   `-- login.component.ts
|   |   `-- register
|   |       `-- register.component.ts
|   |-- checkins
|   |   `-- checkins.component.ts
|   |-- dashboard
|   |   `-- dashboard.component.ts
|   |-- landing
|   |   |-- landing.component.css
|   |   |-- landing.component.html
|   |   `-- landing.component.ts
|   `-- profile
|       `-- profile.component.ts
|-- layouts
|   `-- app-shell
|       `-- app-shell.component.ts
`-- shared
    `-- models
        `-- models.ts
```

## Como defender esta arquitectura

Esta organizacion separa lo que la aplicacion hace de como se sostiene tecnicamente.

Las pantallas estan en `features`, la infraestructura esta en `core`, los elementos que varias partes comparten estan en `shared` y las estructuras visuales generales estan en `layouts`. Esto hace que el proyecto sea mas facil de explicar, probar y escalar.
