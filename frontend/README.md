# Frontend de Mentaiko

Aplicación Angular 22 basada en componentes standalone. Consume la API REST de Spring Boot; no utiliza datos simulados en el funcionamiento normal.

## Requisitos

- Una versión de Node.js compatible con Angular 22.
- npm.
- Backend disponible para atender las solicitudes a `/api`.

## Desarrollo

```bash
cd frontend
npm ci
npm start
```

Abrir `http://localhost:4200`.

La URL de la API se define en `src/environments/environment.ts` y actualmente es relativa: `/api`. En producción, Nginx puede enrutar ese prefijo al backend. Para trabajar con servidores distintos, configura el proxy o el punto de entrada HTTP correspondiente; no cambies la URL a `localhost` sin verificar el entorno.

## Comprobaciones

```bash
npm test -- --watch=false
npm run build -- --configuration production
```

Las pruebas utilizan Vitest. La compilación de producción se ejecuta también en CI.

## Organización

| Ruta | Contenido |
| --- | --- |
| `src/app/components/` | Pantallas y componentes funcionales |
| `src/app/core/auth/` | Estado de sesión |
| `src/app/core/guards/` | Acceso a rutas |
| `src/app/core/interceptors/` | Cabeceras HTTP y autenticación |
| `src/app/core/services/` | Clientes de la API por dominio |
| `src/app/layouts/` | Estructura visual compartida |
| `src/app/shared/models/` | Modelos y contratos TypeScript |
| `src/environments/` | Configuración de entorno |

Cada componente visual mantiene su plantilla, lógica, estilos y pruebas en archivos separados cuando corresponda. Más detalles en [Arquitectura del frontend](docs/FRONTEND_ARCHITECTURE.md).

## Seguridad

El cliente utiliza JWT y protección de rutas en Angular. La autorización efectiva depende del backend. El uso de `localStorage` para el token requiere medidas frente a XSS y revisión del modelo de sesión antes de ampliar el uso productivo.

## API

Consulta [API-CONTRACT.md](API-CONTRACT.md) para los endpoints y formatos de intercambio. Mantén este documento sincronizado con los controladores y DTO del backend.
