# KAOS — Plataforma de Gestión de Equipos

Aplicación full-stack para gestión de capacidad de squads de desarrollo.

| Capa       | Stack                                         |
| ---------- | --------------------------------------------- |
| Frontend   | React 19 + Vite 6 + TypeScript + Tailwind CSS |
| Backend    | Java 21 + Spring Boot 3.4 + Liquibase         |
| Base datos | PostgreSQL 16                                 |

## Arranque rápido (Docker)

> **Requisito**: Docker y Docker Compose instalados.

```bash
cd kaos/

# Arrancar todo (frontend + backend + PostgreSQL)
docker-compose up -d --build

# Ver logs en tiempo real
docker-compose logs -f
```

| Servicio | URL                                   |
| -------- | ------------------------------------- |
| Frontend | http://localhost:2000                 |
| API REST | http://localhost:6060/api/v1          |
| Swagger  | http://localhost:6060/swagger-ui.html |
| H2 (dev) | http://localhost:8080/h2-console      |

La primera vez, Liquibase ejecuta las migraciones y carga los datos reales del equipo (3 squads, 17 personas, 19 asignaciones).

### Variables de entorno opcionales

```bash
# Si el puerto 5432 está ocupado (PostgreSQL local)
DB_PORT=5433 docker-compose up -d --build

# Personalizar puertos
FRONTEND_PORT=4000 BACKEND_PORT=9090 docker-compose up -d --build
```

### Parar y limpiar

```bash
# Parar servicios
docker-compose down

# Parar y borrar datos (BD limpia en siguiente arranque)
docker-compose down -v
```

---

## Desarrollo local

### Backend (profile `dev` — H2 en memoria)

```bash
cd backend/
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

API disponible en http://localhost:8080/api/v1  
Consola H2 en http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:kaosdb`, user: `sa`, sin password)

### Frontend

```bash
cd frontend/
npm install
npm run dev
```

Frontend disponible en http://localhost:5173  
Proxy automático: las peticiones a `/api/` se redirigen al backend en `:8080`.

---

## Datos iniciales

Los scripts de Liquibase cargan datos reales desde `equipos.yaml`:

| Entidad       | Cantidad | Detalle                                                      |
| ------------- | -------- | ------------------------------------------------------------ |
| PerfilHorario | 2        | España (Europe/Madrid), Chile (America/Santiago)             |
| Squad         | 3        | red (22517), green (22516), blue (22515)                     |
| Persona       | 17       | Nombres, emails y Jira IDs reales                            |
| SquadMember   | 19       | Luis Galván como SM en 3 squads (33%+33%+34%), resto al 100% |

## Endpoints principales

### Squads

```
GET    /api/v1/squads                → Lista squads (filtro opcional por estado)
GET    /api/v1/squads/{id}           → Detalle squad
POST   /api/v1/squads               → Crear squad
PUT    /api/v1/squads/{id}          → Actualizar squad
```

### Personas

```
GET    /api/v1/personas              → Lista personas (paginada + filtros avanzados)
GET    /api/v1/personas/{id}         → Detalle persona
POST   /api/v1/personas             → Crear persona
PUT    /api/v1/personas/{id}        → Actualizar persona
```

**Filtros disponibles en personas:**

- `squadId`: Filtrar por squad asignado
- `rol`: Filtrar por rol (SM, DEV, QA, etc.)
- `seniority`: Filtrar por seniority (JUNIOR, SENIOR, etc.)
- `ubicacion`: Filtrar por ubicación
- `activo`: Filtrar por estado activo/inactivo
- `page`, `size`, `sort`: Paginación y ordenamiento

### Asignaciones (Squad Members)

```
GET    /api/v1/squads/{squadId}/miembros    → Miembros de un squad
GET    /api/v1/personas/{personaId}/squads → Squads de una persona
POST   /api/v1/squad-members               → Asignar persona a squad
PUT    /api/v1/squad-members/{id}          → Modificar asignación
DELETE /api/v1/squad-members/{id}          → Eliminar asignación
```

### Perfiles de Horario

```
GET    /api/v1/perfiles-horario      → Lista perfiles horario
GET    /api/v1/perfiles-horario/{id} → Detalle perfil horario
POST   /api/v1/perfiles-horario     → Crear perfil horario
PUT    /api/v1/perfiles-horario/{id} → Actualizar perfil horario
DELETE /api/v1/perfiles-horario/{id} → Eliminar perfil horario
```

---

## ✨ Características del Bloque 1

### 🎨 Iconos KAOS Aleatorios

- Dashboard con logos KAOS dinámicos en cada tarjeta
- 6 estilos disponibles: classic, modern, neon, geometric, vintage, icon
- Selección aleatoria en cada carga para variedad visual
- Favicon personalizado con icono KAOS
- Sistema de logo manager para gestión centralizada

### 🧪 Testing y Calidad

## Backend (Java + Spring Boot) - JaCoCo Coverage

| Métrica       | Cobertura | Estado |
| ------------- | --------- | ------ |
| **Instrucciones** | 99% | 🟢 Excelente |
| **Ramas** | 94% | 🟢 Muy Bueno |
| **Líneas** | 99% | 🟢 Excelente |
| **Métodos** | 100% | 🟢 Perfecto |
| **Clases** | 100% | 🟢 Perfecto |

**Cobertura por paquete:**

| Paquete | Instrucciones | Ramas | Estado |
| ------- | ------------- | ----- | ------ |
| `squad.service` | 97% | 87% | 🟡 Bueno |
| `dedicacion.service` | 100% | 100% | 🟢 Perfecto |
| `persona.service` | 100% | 100% | 🟢 Perfecto |
| `horario.service` | 100% | 100% | 🟢 Perfecto |
| `persona.controller` | 100% | 75% | 🟡 Bueno |
| `dedicacion.controller` | 100% | N/A | 🟢 Perfecto |
| `squad.controller` | 100% | N/A | 🟢 Perfecto |
| `horario.controller` | 100% | N/A | 🟢 Perfecto |
| `common.exception` | 100% | N/A | 🟢 Perfecto |

**Tests implementados:**
- ✅ Unit tests para servicios (SquadService, PersonaService, SquadMemberService, PerfilHorarioService)
- ✅ Integration tests para controladores con mocking
- ✅ Tests de repositorios con datos de prueba
- ✅ Tests de mappers (MapStruct)
- ✅ Tests de validación y error handling

## Frontend (React + TypeScript) - Vitest Coverage

| Archivo | Statements | Branches | Functions | Lines | Estado |
| ------- | ---------- | -------- | --------- | ----- | ------ |
| `logo-manager.ts` | 92.45% | 100% | 80% | 92.45% | 🟢 Excelente |
| `LogoDisplay.tsx` | 100% | 100% | 100% | 100% | 🟢 Perfecto |
| **Total** | **96.2%** | **100%** | **90%** | **96.2%** | 🟢 Excelente |

**Resultados de testing:**
```
✅ TODOS LOS TESTS PASARON
 ✓ src/routes/index.test.tsx  (67 tests) ✅
 ✓ src/lib/logo-manager.test.ts  (16 tests) ✅
 ✓ src/components/LogoDisplay.test.tsx  (25 tests) ✅

 Test Files  3 passed (3) ✅
      Tests  108 passed (108) ✅
   Duration  1.10s
```

**Tests implementados:**
- ✅ **67 tests** en rutas (navegación, estados, interacciones)
- ✅ **16 tests** en logo-manager (lógica de selección aleatoria, tipos, URLs)
- ✅ **25 tests** en LogoDisplay (renderizado, props, estados)
- ✅ Tests de integración con React Testing Library
- ✅ Tests de componentes con diferentes props y estados
- ✅ Cobertura de edge cases y validaciones

### Comandos de Testing

```bash
# Backend - Ejecutar tests con cobertura
cd backend/
./mvnw test

# Ver reporte JaCoCo en navegador
open target/site/jacoco/index.html

# Frontend - Ejecutar tests
cd frontend/
npm test

# Tests con watch mode
npm run test:watch

# Coverage report
npm run test:coverage
```

### Estrategia de Testing

- **Backend**: Tests unitarios + integración con JUnit 5, Mockito, Testcontainers
- **Frontend**: Tests unitarios + integración con Vitest, React Testing Library
- **Cobertura objetivo**: >80% en todas las métricas
- **CI/CD**: Tests automáticos en cada push/PR
- **Exclusiones**: Solo clases de configuración e infraestructura

### 🐳 Infraestructura (React 19 + Vite 6 + Nginx)

- Backend: puerto 6060 (Spring Boot 3.4 + Java 21)
- PostgreSQL: integración con contenedor externo
- Multi-stage builds optimizados
- Health checks configur
- Backend: puerto 6060
- Integración con PostgreSQL externo
- Multi-stage builds optimizados

### 📊 Dashboard Funcional

- Contadores reales conectados a datos (Squads, Personas, Configuración)
- Navegación a Squads, Personas y Configuración
- Estados de carga y error manejados
- Diseño responsive con Tailwind CSS
- **Iconos KAOS aleatorios** en cada tarjeta del dashboard
