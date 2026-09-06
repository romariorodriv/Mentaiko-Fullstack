# Mentaiko Frontend

Frontend Angular para el proyecto académico Mentaiko. Cubre las 14 historias de usuario del Product Backlog y consume exclusivamente la API Spring Boot.

## Stack

- Angular 22 con componentes standalone
- Reactive Forms
- Angular Router
- HttpClient e interceptor JWT
- Guards de autenticación y rol ADMIN
- CSS responsive sin librerías visuales externas

## Requisitos

- Node.js 22.22.3+, 24.15+ o 26
- npm 11+
- Backend Spring Boot disponible en `http://localhost:8080`

## Ejecución

```bash
npm install
npm start
```

Abrir `http://localhost:4200`.

## Configuración de API

Editar `src/environments/environment.ts`:

```ts
apiUrl: 'http://localhost:8080/api'
```

## Contrato esperado

El archivo `API-CONTRACT.md` detalla los endpoints que el backend debe implementar. No se incluyen datos simulados: las pantallas muestran carga, vacío o error según la respuesta real.

## Seguridad

El token JWT se guarda en `localStorage` para este MVP académico y se adjunta como `Authorization: Bearer TOKEN`. Para producción se recomienda revisar el modelo de sesión, CSP, expiración y estrategia frente a XSS.
