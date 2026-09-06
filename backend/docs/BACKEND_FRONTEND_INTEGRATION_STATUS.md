# Diagnostico de integracion Backend-Frontend Mentaiko

Fecha de auditoria: 2026-09-06

Alcance: backend Spring Boot en `back/mentaiko-backend` y frontend Angular localizado en `mentaiko-angular-frontend`. No se modifico codigo funcional.

## A. Resumen ejecutivo

| Pregunta | Estado | Evidencia |
| --- | --- | --- |
| El backend compila? | Si con Maven global; `mvnw.cmd` falla | `mvn test` termino en `BUILD SUCCESS`; `.\mvnw.cmd test` falla con `Cannot start maven from wrapper` |
| Los tests pasan? | Si, pero cobertura minima | 1 test `contextLoads`, 0 fallos |
| El backend inicia? | No en configuracion real actual | `mvn spring-boot:run` falla por datasource: `'url' must start with "jdbc"` |
| PostgreSQL conecta? | No comprobado | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` no configurados en el entorno |
| Frontend y backend pueden comunicarse? | No de punta a punta | Backend no inicia y faltan la mayoria de endpoints esperados por Angular |
| Registro funciona E2E? | No | Angular no envia `birthDate`; backend lo exige con `@NotNull @Past` |
| Login funciona E2E? | No | Backend devuelve `message` y `user`; Angular espera `token` |
| JWT funciona? | Falta | No hay dependencia JWT, filtro JWT, `SecurityFilterChain` ni servicio JWT |
| Porcentaje aproximado completo | 15-20% | Existen health, registro parcial y login parcial; faltan seguridad, perfil, check-ins, catalogos, recomendaciones, reportes y admin |
| Bloqueante principal | Autenticacion/contratos base | Login no entrega JWT y no existe seguridad; ademas registro no coincide con el formulario |

## B. Stack real

| Elemento | Estado real |
| --- | --- |
| Java runtime | OpenJDK Temurin 25.0.4.1 |
| `pom.xml` Java | `<java.version>25</java.version>` |
| Spring Boot | 4.1.1 |
| Maven Wrapper | Existe, version 3.3.4, pero `mvnw.cmd` falla en PowerShell/cmd |
| Maven global | Apache Maven 3.9.9 |
| Web | `spring-boot-starter-webmvc` |
| JPA | `spring-boot-starter-data-jpa` |
| PostgreSQL | Driver runtime `org.postgresql:postgresql` |
| Validation | `spring-boot-starter-validation` |
| Security | Solo `spring-security-crypto`; no `spring-boot-starter-security` |
| JWT | No existe dependencia ni codigo JWT |
| Tests | `spring-boot-starter-webmvc-test` y H2 test |
| DB runtime | `spring.datasource.url=${DB_URL}` |
| DB tests | H2 en memoria `jdbc:h2:mem:mentaiko-test` |
| Esquema runtime | `spring.jpa.hibernate.ddl-auto=update` |
| Migraciones | No hay Flyway, Liquibase ni scripts SQL |
| Secretos | `DB_URL`: No configurado; `DB_USERNAME`: No configurado; `DB_PASSWORD`: No configurado; `JWT_SECRET`: No configurado |

## C. Arbol real del backend

```text
src/main/java/com/mentaiko/backend
|-- MentaikoBackendApplication.java
|-- config
|   `-- PasswordConfig.java
|-- controller
|   |-- HealthController.java
|   `-- UserController.java
|-- dto
|   `-- auth
|       |-- LoginRequest.java
|       |-- LoginResponse.java
|       |-- RegisterRequest.java
|       `-- UserResponse.java
|-- entity
|   |-- EmotionalEntry.java
|   |-- MicroActivity.java
|   `-- User.java
|-- enums
|   `-- Role.java
|-- repository
|   `-- UserRepository.java
`-- service
    `-- UserService.java

src/test/java/com/mentaiko/backend
`-- MentaikoBackendApplication.java

src/test/resources
`-- application.properties
```

| Clase | Capa | Responsabilidad | Dependencias | Estado |
| --- | --- | --- | --- | --- |
| `MentaikoBackendApplication` | Bootstrap/config | Arranca Spring Boot y escanea entidades | Spring Boot, `@EntityScan` | Implementado |
| `HealthController` | Controller | Expone `GET /api/health` como texto plano | Spring WebMVC | Implementado simple |
| `UserController` | Controller | Expone registro y login bajo `/api/auth` | `UserService`, DTO auth | Parcial |
| `UserService` | Service | Registra usuarios, valida email duplicado, cifra password, login por password | `UserRepository`, `PasswordEncoder` | Parcial |
| `UserRepository` | Repository | Acceso JPA a `User`; busqueda por email ignore-case | `JpaRepository` | Implementado para usuarios |
| `User` | Entity | Tabla `users` con datos de registro | JPA | Parcial |
| `EmotionalEntry` | Entity nominal | Clase vacia sin anotaciones | Ninguna | No implementado |
| `MicroActivity` | Entity nominal | Clase vacia sin anotaciones | Ninguna | No implementado |
| `Role` | Enum | Declara `USER`, `ADMIN` | Ninguna | No integrado |
| `PasswordConfig` | Config | Bean BCrypt `PasswordEncoder` | Spring Security Crypto | Implementado |

No hay paquetes reales `mapper`, `security`, `exception` ni `validation` separados.

## D. Endpoints reales

| Metodo | URL completa | Controller | Request DTO | Response DTO | Acceso | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/api/health` | `HealthController` | Ninguno | `String` | Publico de facto | Implementado, texto plano |
| POST | `/api/auth/register` | `UserController` | `RegisterRequest` | `UserResponse` | Publico de facto | Implementado parcialmente; no coincide con frontend por `birthDate` |
| POST | `/api/auth/login` | `UserController` | `LoginRequest` | `LoginResponse` | Publico de facto | Implementado parcialmente; no genera JWT |

No existen endpoints backend para:

- `/api/users/me`
- `/api/emotions`
- `/api/checkins`
- `/api/micro-activities`
- `/api/recommendations`
- `/api/reports/weekly`
- `/api/reports/distribution`
- `/api/admin/emotions`
- `/api/admin/micro-activities`
- `/api/admin/indicators`

## E. Estado de arquitectura por capas

| Capa | Estado |
| --- | --- |
| Controller | Usa DTO y delega a servicio para auth. Solo cubre health, register, login. |
| Service | `UserService` contiene reglas basicas: normaliza email, valida duplicado, cifra password, compara password. No tiene transacciones explicitas. |
| Repository | Solo `UserRepository extends JpaRepository<User, Long>`. No hay repositorios para emociones, check-ins ni actividades. |
| JPA/Hibernate | Solo `User` es entidad persistible. `EmotionalEntry` y `MicroActivity` no tienen `@Entity`. |
| PostgreSQL | Configurado por variables `DB_*`, pero en este entorno no conecta porque no estan configuradas. |
| Mappers | No existen; hay metodo privado `toResponse` en `UserService`. |
| Excepciones | Se usa `ResponseStatusException`; no hay `@RestControllerAdvice`. |
| Validacion | Existe Jakarta Validation en DTO auth, pero no para dominios faltantes. |

## F. Estado de seguridad

| Control de seguridad | Estado | Evidencia | Riesgo |
| --- | --- | --- | --- |
| Registro cifra password | Si | `passwordEncoder.encode(request.password())` en `UserService` | Correcto para registro |
| Login compara password cifrada | Si | `passwordEncoder.matches(request.password(), user.getPasswordHash())` | Correcto para credenciales |
| Login genera JWT real | No | `LoginResponse` solo contiene `message`, `user` | P0: Angular no queda autenticado realmente |
| JWT contiene usuario y rol | No aplica/falta | No existe JWT | P0 |
| JWT tiene expiracion | No aplica/falta | No existe JWT | P1 |
| Filtro valida firma y expiracion | No aplica/falta | No hay filtro JWT | P1 |
| Usuario en `SecurityContext` | No | No hay Spring Security Web ni filtro | P1 |
| Endpoints privados exigen token | No | No hay `SecurityFilterChain`; todos son publicos de facto | P1 |
| Admin exige rol `ADMIN` | No | No hay reglas `hasRole`/`hasAuthority` | P1 |
| Diferencia 401/403 | Parcial | Login usa 401; no hay autorizacion para 403 | P1 |
| Password aparece en response | No | `UserResponse` no incluye `passwordHash` | Correcto |
| CORS permite `http://localhost:4200` | No comprobado/no configurado | No hay configuracion CORS | P0/P1 para navegador |
| Rutas accidentalmente publicas | Si | Sin `SecurityFilterChain` | P1 |
| Secreto JWT fuera del repo | No configurado | No existe `JWT_SECRET` usado por codigo | P1 |

Adicional: `Role enum` define `USER` y `ADMIN`, pero `UserService` y `User.prePersist` asignan `"STUDENT"`. Angular solo acepta `USER | ADMIN`.

## G. Entidades y base de datos

| Entidad | Tabla | PK | Campos principales | Relaciones | Restricciones |
| --- | --- | --- | --- | --- | --- |
| `User` | `users` | `id Long`, identity | `name`, `email`, `passwordHash`, `birthDate`, `university`, `career`, `role`, `createdAt` | Ninguna | `email unique`, varios `nullable=false`, longitudes 100/150/30 |
| `EmotionalEntry` | Ninguna | Ninguna | Ninguno | Ninguna | No es entidad JPA |
| `MicroActivity` | Ninguna | Ninguna | Ninguno | Ninguna | No es entidad JPA |

Suficiencia del modelo:

| Necesidad | Estado |
| --- | --- |
| Usuarios | Parcial |
| Emociones | Falta |
| Check-ins emocionales | Falta |
| Contextos emocionales separados | Falta/no aplica aun |
| Microactividades | Falta |
| Actividades completadas | Falta |
| Roles persistidos separadamente | Falta/no aplica; rol es `String` en `users` |
| Fechas actualizacion | Falta `updatedAt` |
| Borrado logico | Falta |
| Integridad referencial | Falta para dominios no modelados |

El esquema se administra con Hibernate `ddl-auto=update` en runtime y `create-drop` en tests. No hay migraciones versionadas.

## H. Contratos utilizados por Angular

Base URL real del frontend: `http://localhost:8080/api`.

| Pantalla/servicio | Metodo | URL frontend | Request esperado | Response esperado | Token |
| --- | --- | --- | --- | --- | --- |
| Login | POST | `/auth/login` | `{ email, password }` | `{ token, user? }` | No para request; guarda `token` |
| Registro | POST | `/auth/register` | `{ name, email, password, university?, career? }` | `UserProfile` | No |
| Shell/perfil | GET | `/users/me` | Ninguno | `UserProfile` | Si |
| Perfil | PUT | `/users/me` | `{ name, university, career }` | `UserProfile` | Si |
| Check-ins | GET | `/checkins?from&to&emotionId&context&page&size` | Filtros query | `PageResponse<Checkin>` | Si |
| Check-ins | POST | `/checkins` | `{ emotionId, intensity, context, note? }` | `Checkin` | Si |
| Check-ins | PUT | `/checkins/{id}` | `{ emotionId, intensity, context, note? }` | `Checkin` | Si |
| Check-ins | DELETE | `/checkins/{id}` | Ninguno | `void` | Si |
| Emociones | GET | `/emotions?activeOnly=true|false` | Query | `Emotion[]` | Si |
| Admin emociones | POST | `/admin/emotions` | `{ name }` | `Emotion` | Si, rol admin |
| Admin emociones | PUT | `/admin/emotions/{id}` | `Partial<Emotion>` | `Emotion` | Si, rol admin |
| Actividades | GET | `/micro-activities?activeOnly=true|false` | Query | `MicroActivity[]` | Si |
| Admin actividades | POST | `/admin/micro-activities` | `{ title, description, durationMinutes, active }` | `MicroActivity` | Si, rol admin |
| Admin actividades | PUT | `/admin/micro-activities/{id}` | `Partial<MicroActivity>` | `MicroActivity` | Si, rol admin |
| Recomendacion | POST | `/recommendations` | `{ checkinId }` | `Recommendation` | Si |
| Recomendaciones | GET | `/recommendations/me` | Ninguno | `Recommendation[]` | Si |
| Completar actividad | POST | `/recommendations/{id}/complete` | `{}` | `Recommendation` | Si |
| Dashboard | GET | `/reports/weekly?week=YYYY-Www` | Query | `WeeklyPoint[]` | Si |
| Dashboard | GET | `/reports/distribution?from&to` | Query | `{ emotions, contexts }` | Si |
| Admin indicadores | GET | `/admin/indicators` | Ninguno | `AdminIndicators` | Si, rol admin |

Modelos Angular relevantes:

- `Role = 'USER' | 'ADMIN'`
- `UserProfile`: `id`, `name`, `email`, `role`, `active`, `university?`, `career?`, `createdAt?`
- `AuthResponse`: `token`, `user?`
- `RegisterRequest`: `name`, `email`, `password`, `university?`, `career?`
- `Checkin`: `id`, `emotion`, `intensity`, `context`, `note?`, `createdAt`
- `PageResponse<T>`: `content`, `totalElements`, `totalPages`, `number`, `size`

## I. Matriz frontend-backend

| Funcionalidad | Frontend espera | Backend ofrece | Coincide? | Diferencia | Accion requerida |
| --- | --- | --- | --- | --- | --- |
| Health | `GET /api/health` | `GET /api/health` texto | Parcial | Respuesta no JSON | Decidir si mantener texto o estandarizar JSON |
| Registro URL | `POST /api/auth/register` | Igual | Si | - | Mantener |
| Registro request | `name,email,password,university?,career?` | `name,email,password,birthDate,university,career` | No | Falta `birthDate`; backend hace universidad/carrera obligatorias | Alinear formulario o DTO |
| Registro response | `UserProfile` con `active` y rol `USER|ADMIN` | `UserResponse` sin `active`, rol `"STUDENT"` | No | Campos/rol incompatibles | Unificar `UserProfile` |
| Login URL | `POST /api/auth/login` | Igual | Si | - | Mantener |
| Login response | `{ token, user? }` | `{ message, user }` | No | No hay `token` | Implementar JWT real o cambiar frontend; recomendado backend JWT |
| Header auth | `Authorization: Bearer <token>` | No se valida | No | Backend ignora token | Spring Security + JWT filter |
| Rol | `USER`/`ADMIN`, limpia `ROLE_` si viene | Backend persiste `"STUDENT"` | No | `STUDENT` no existe en Angular | Usar enum `USER`/`ADMIN` |
| Perfil | `/api/users/me` GET/PUT | No existe | No | Endpoint faltante | Implementar vertical perfil autenticado |
| Emociones | `/api/emotions`, `/api/admin/emotions` | No existe | No | Entidad/repositorio/controlador faltan | Implementar catalogo |
| Check-ins | `/api/checkins` CRUD + filtros/page | No existe | No | Modelo y endpoints faltan | Implementar check-ins por usuario autenticado |
| Actividades | `/api/micro-activities`, admin CRUD | No existe | No | Modelo y endpoints faltan | Implementar microactividades |
| Recomendaciones | `/api/recommendations*` | No existe | No | Dominio faltante | Implementar recomendacion y completado |
| Reportes | `/api/reports/*` | No existe | No | Consultas agregadas faltan | Implementar reportes de usuario |
| Admin indicadores | `/api/admin/indicators` | No existe | No | Endpoint faltante | Implementar agregados admin |
| Errores | `ApiError {status,message,errors?}` | Error default Spring `ResponseStatusException`/validation | Parcial | Sin formato global consistente | Crear `@RestControllerAdvice` |
| CORS | Navegador desde `localhost:4200` | No configurado | No comprobado | Sin CORS explicito | Configurar CORS para frontend |

## J. Matriz funcional

| Funcionalidad | Frontend | Backend | Base de datos | Seguridad | E2E |
| --- | --- | --- | --- | --- | --- |
| Health | Completo | Parcial | No aplica | Publico | Bloqueado por arranque |
| Registro | Parcial | Parcial | Parcial | Publico | Bloqueado |
| Login | Parcial | Parcial | Parcial | Falta JWT | Bloqueado |
| Sesion actual | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Perfil | Completo en UI/servicio | Falta | Parcial usuario | Falta | Falta |
| Actualizar perfil | Completo en UI/servicio | Falta | Parcial usuario | Falta | Falta |
| Catalogo de emociones | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Crear check-in | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Listar check-ins propios | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Filtrar check-ins | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Editar check-in | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Eliminar check-in | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Listar microactividades | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Obtener recomendacion | Servicio existe | Falta | Falta | Falta | Falta |
| Marcar actividad completada | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Reporte semanal | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Distribucion por emocion | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Distribucion por contexto | Servicio espera data | Falta | Falta | Falta | Falta |
| Admin: emociones CRUD | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Admin: actividades CRUD | Completo en UI/servicio | Falta | Falta | Falta | Falta |
| Admin: indicadores | Completo en UI/servicio | Falta | Falta | Falta | Falta |

## K. Resultado de tests

Comandos ejecutados:

```powershell
Get-Location
git status --short
git branch --show-current
java -version
.\mvnw.cmd -version
.\mvnw.cmd test
mvn test
Get-NetTCPConnection -LocalPort 8080
mvn spring-boot:run
```

Resultados clave:

| Test o suite | Capa | Resultado | Que comprueba |
| --- | --- | --- | --- |
| `MentaikoBackendApplicationTests.contextLoads` | Integracion minima Spring | Pasa con `mvn test` | Solo carga contexto con H2 |

Observaciones:

- `.\mvnw.cmd -version` y `.\mvnw.cmd test` fallan con `No se puede indizar en una matriz nula` y `Cannot start maven from wrapper`.
- `mvn test` inicialmente fallo dentro del sandbox por red bloqueada; al repetir con permiso externo paso.
- Tests ejecutados: 1; fallos: 0; errores: 0; omitidos: 0.
- No hay tests de controller, service, repository, seguridad, validacion ni contratos frontend-backend.
- Los tests usan H2 en memoria, no PostgreSQL.

Arranque:

- Puerto 8080: sin proceso detectado.
- `mvn spring-boot:run` con permiso externo compila, inicia Tomcat en 8080 y falla creando `dataSource`.
- Causa raiz: `DB_URL` no configurado; Spring usa el placeholder literal `${DB_URL}` y Hikari falla con `'url' must start with "jdbc"`.
- No se realizaron pruebas HTTP porque la aplicacion no quedo iniciada.

## L. Problemas priorizados

| Prioridad | Problema | Evidencia | Impacto | Solucion recomendada |
| --- | --- | --- | --- | --- |
| P0 | Backend no inicia sin `DB_URL` real | `spring-boot:run` falla: `'url' must start with "jdbc"` | No hay API disponible para Angular | Definir configuracion local segura o perfil dev/test documentado |
| P0 | Login no entrega JWT | `LoginResponse(message,user)` | Angular guarda `r.token`, queda `undefined` | Implementar JWT y response `{ token, user }` |
| P0 | Registro frontend-backend incompatible | Angular no envia `birthDate`; backend lo exige | Registro devuelve 400 antes de crear usuario | Alinear DTO/formulario y obligatoriedad de campos |
| P1 | No hay Spring Security Web | Solo `spring-security-crypto` | Todos los endpoints serian publicos | Agregar `spring-boot-starter-security`, `SecurityFilterChain`, JWT filter |
| P1 | Roles inconsistentes | Backend usa `"STUDENT"`; enum y Angular usan `USER/ADMIN` | Admin guard no funciona y usuarios no encajan | Persistir `Role.USER`/`Role.ADMIN` |
| P1 | No hay CORS explicito | No existe config CORS | Angular desde 4200 puede fallar por navegador | Configurar origen `http://localhost:4200` |
| P2 | Perfil autenticado falta | Angular llama `/users/me`; backend no lo expone | Shell/perfil fallan tras login | Implementar GET/PUT `/api/users/me` |
| P2 | Dominio emocional falta | `EmotionalEntry` vacio, no hay `Emotion` | Check-ins/reportes imposibles | Modelar emociones y check-ins |
| P2 | Microactividades/recomendaciones faltan | `MicroActivity` vacio; no endpoints | Actividades no funcionan | Implementar catalogo, recomendacion y completado |
| P2 | Reportes e indicadores faltan | No hay endpoints `/reports` ni `/admin/indicators` | Dashboard/admin no funcionan | Implementar consultas agregadas |
| P3 | Sin excepciones globales | No hay `@RestControllerAdvice` | Errores inconsistentes para Angular | Crear formato `ApiError` |
| P3 | Sin migraciones | Solo `ddl-auto=update` | Riesgo de drift de esquema | Introducir Flyway o Liquibase |
| P3 | Test class con nombre de archivo confuso | Archivo `MentaikoBackendApplication.java` contiene `MentaikoBackendApplicationTests` | Mantenibilidad baja | Renombrar en etapa posterior |

## M. Plan ordenado de integracion

### Vertical 1: Arranque local, health y CORS

- Backend: `application.properties`, nuevo config CORS, health.
- Frontend: `environment.ts`.
- Endpoint: `GET /api/health`.
- Request: ninguno.
- Response: ideal `{ status: "ok", service: "mentaiko-backend" }` o mantener texto si se acuerda.
- Seguridad: publico.
- Tests: context load, health controller, CORS preflight.
- Terminado cuando backend inicia en 8080 con PostgreSQL local y Angular puede llamar health.

### Vertical 2: Registro completo

- Backend: `User`, `RegisterRequest`, `UserResponse`, `UserService`, `UserController`, exception handler.
- Frontend: `register.component.ts`, `models.ts`, `AuthService`.
- Endpoint: `POST /api/auth/register`.
- Request: acordar si `birthDate` existe en frontend o deja de ser obligatorio.
- Response: `UserProfile` con `id,name,email,role,active,university,career,createdAt`.
- Seguridad: publico.
- Tests: validacion, email duplicado, password cifrada, no exponer hash.
- Terminado cuando un usuario de prueba se registra desde Angular y queda persistido.

### Vertical 3: Login + JWT

- Backend: dependencias JWT/security, `SecurityFilterChain`, `JwtService`, filter, `LoginResponse`.
- Frontend: `AuthService`, interceptor ya existe.
- Endpoint: `POST /api/auth/login`.
- Request: `{ email, password }`.
- Response: `{ token, user }`.
- Seguridad: publico para login/register; privados con Bearer.
- Tests: 401 credenciales invalidas, token valido, expiracion, claims rol.
- Terminado cuando Angular guarda token y puede llamar un endpoint protegido.

### Vertical 4: Perfil autenticado

- Backend: `UserController` o `ProfileController`, DTO update, obtencion desde JWT.
- Frontend: `profile.component.ts`, `app-shell.component.ts`, `AuthService`.
- Endpoints: `GET /api/users/me`, `PUT /api/users/me`.
- Request: update `{ name, university, career }`.
- Response: `UserProfile`.
- Seguridad: `USER` o `ADMIN` autenticado.
- Tests: usuario solo accede a su propio perfil.
- Terminado cuando shell y pantalla perfil cargan/actualizan datos.

### Vertical 5: Catalogo de emociones

- Backend: `Emotion` entity, repository, service, public/auth list, admin create/update.
- Frontend: `ApiService`, `admin-emotions`, `checkins`.
- Endpoints: `GET /api/emotions`, `POST/PUT /api/admin/emotions`.
- Seguridad: listar autenticado; admin para escritura.
- Tests: duplicados, `activeOnly`, permisos.
- Terminado cuando check-ins carga emociones y admin gestiona catalogo.

### Vertical 6: Check-ins

- Backend: `Checkin`/`EmotionalEntry` entity real, repository con filtros, service por usuario.
- Frontend: `checkins.component.ts`, `ApiService`.
- Endpoints: `GET/POST/PUT/DELETE /api/checkins`.
- Request: `{ emotionId, intensity, context, note? }`.
- Response: `Checkin` y `PageResponse<Checkin>`.
- Seguridad: usuario autenticado, propiedad del registro.
- Tests: rango intensidad, emocion activa, filtros, 403 si no pertenece.
- Terminado cuando CRUD y filtros funcionan desde Angular.

### Vertical 7: Microactividades y recomendaciones

- Backend: `MicroActivity`, `Recommendation`, completion tracking.
- Frontend: `activities.component.ts`, `admin-activities`.
- Endpoints: `/api/micro-activities`, `/api/admin/micro-activities`, `/api/recommendations`.
- Seguridad: usuario para recomendaciones, admin para catalogo.
- Tests: recomendacion por check-in propio, completar una vez, catalogo activo.
- Terminado cuando un check-in puede generar una recomendacion y completarse.

### Vertical 8: Reportes

- Backend: consultas agregadas por usuario.
- Frontend: `dashboard.component.ts`.
- Endpoints: `/api/reports/weekly`, `/api/reports/distribution`.
- Seguridad: usuario autenticado.
- Tests: agregados por rango, no mezclar usuarios.
- Terminado cuando dashboard carga resumen real.

### Vertical 9: Administracion e indicadores

- Backend: indicadores agregados y privacidad minima.
- Frontend: `admin-indicators.component.ts`, guards.
- Endpoint: `GET /api/admin/indicators`.
- Seguridad: solo `ADMIN`.
- Tests: 401 sin token, 403 rol `USER`, 200 admin.
- Terminado cuando admin ve indicadores y usuario comun queda bloqueado.

### Vertical 10: pruebas E2E y documentacion

- Backend: suite controller/service/repository/security.
- Frontend: build/test y smoke E2E manual o automatizado.
- Endpoints: todos.
- Seguridad: matriz completa 401/403.
- Terminado cuando registro, login, perfil, check-ins, recomendaciones, reportes y admin tienen tests y smoke documentado.

## N. Proximo paso exacto

Primera tarea recomendada: corregir la vertical de autenticacion base antes de avanzar a dominios emocionales.

Orden concreto:

1. Acordar contrato `UserProfile`, `RegisterRequest` y `AuthResponse`.
2. Corregir rol inicial a `USER`.
3. Resolver `birthDate`: agregarlo al frontend o hacerlo opcional en backend segun decision de producto.
4. Implementar JWT real y `SecurityFilterChain`.
5. Agregar `/api/users/me`.
6. Probar flujo: registro -> login -> Bearer token -> perfil.

Motivo: todas las pantallas privadas dependen de token, usuario actual y rol. Sin esta base, emociones, check-ins, actividades y admin no pueden integrarse de forma segura ni comprobable.
