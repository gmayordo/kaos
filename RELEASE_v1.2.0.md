# KAOS v1.2.0 Release Notes

**Release Date**: 22 de febrero de 2026  
**Hotfix**: v1.2.1 (22/02/2026) — Dashboard sprint activo + botón exportar timeline

---

## 📋 Resumen Ejecutivo

**KAOS v1.2.0** completa el **Bloque 3: Planificación de Sprints**, el módulo central de la plataforma.
Incluye gestión integral de sprints, asignación diaria de tareas (Timeline), tablero Kanban,
dashboard de métricas, gestión de bloqueos y resumen exportable del sprint.

| Aspecto                                  | Estado                                     |
| ---------------------------------------- | ------------------------------------------ |
| **Bloque 3: Planificación**              | ✅ Completo (RF-009 a RF-015)              |
| **Backend — 4 controllers + servicios**  | ✅ 18 endpoints REST                       |
| **Frontend — 7 componentes + 4 páginas** | ✅ Timeline · Kanban · Dashboard · Resumen |
| **Tests Backend**                        | ✅ 91 tests (service + controller)         |
| **Tests Frontend**                       | ✅ 46 tests Vitest (5 ficheros)            |
| **Excel Export**                         | ✅ Timeline exportable como XLSX           |
| **QA — Bugs corregidos**                 | ✅ 2 (v1.2.1 hotfix)                       |

---

## 🎯 Features Principales

### 1. Gestión de Sprints (RF-009)

CRUD completo de sprints por squad con máquina de estados validada.

- ✅ Crear sprint con nombre, squad, fechas y objetivo
- ✅ Fecha fin calculada automáticamente en domingo (inicio lunes + 13 días)
- ✅ Estados: `PLANIFICACION → ACTIVO → CERRADO` (transiciones validadas)
- ✅ No se permiten dos sprints ACTIVOS del mismo squad simultáneamente
- ✅ Sprint cerrado es inmutable
- ✅ Selector global de sprint por squad con badges de estado visual

**Endpoints:**

```
GET    /api/v1/sprints                → Listar (filtro por squadId, estado, paginado)
POST   /api/v1/sprints                → Crear sprint
GET    /api/v1/sprints/{id}           → Obtener
PATCH  /api/v1/sprints/{id}/estado    → Cambiar estado
DELETE /api/v1/sprints/{id}           → Eliminar (solo en PLANIFICACION)
```

---

### 2. Gestión de Tareas del Sprint (RF-010)

- ✅ CRUD de tareas con: tipo (HISTORIA/TAREA/BUG/SPIKE), categoría (CORRECTIVO/EVOLUTIVO), estimación, prioridad, referencia Jira
- ✅ Asignación a persona por día del sprint (1-10)
- ✅ Validación de capacidad disponible antes de asignar (lanza `CapacidadInsuficienteException` si se excede)
- ✅ Solo se pueden mover tareas en estado PENDIENTE (reasignación libre)
- ✅ Estado: `PENDIENTE → EN_PROGRESO → BLOQUEADO → COMPLETADA`

**Endpoints:**

```
GET    /api/v1/tareas                 → Listar (filtro por sprintId, personaId, estado)
POST   /api/v1/tareas                 → Crear tarea
GET    /api/v1/tareas/{id}            → Obtener
PATCH  /api/v1/tareas/{id}/estado     → Cambiar estado
DELETE /api/v1/tareas/{id}            → Eliminar (solo PENDIENTE)
```

---

### 3. Vista Timeline (RF-011)

Grid tipo Gantt donde filas = personas y columnas = días del sprint.

- ✅ 10 días laborables visualizados (L-V semana 1, L-V semana 2)
- ✅ Drag & drop de tareas entre días (`@hello-pangea/dnd`)
- ✅ Indicador visual de sobreasignación (rojo > 100%, naranja 80-100%, verde ≤ 80%)
- ✅ Días festivos / vacaciones bloqueados con fondo diferenciado
- ✅ Click en tarea → abre ModalTarea para editar
- ✅ Click en celda vacía → abre ModalTarea prerellenada (persona + día)
- ✅ **Exportación a Excel (XLSX)** — botón "Descargar" en cabecera

---

### 4. Vista Kanban (RF-012)

Tablero con 4 columnas: Pendiente · En Progreso · Bloqueado · Completada.

- ✅ Drag & drop entre columnas para cambiar estado
- ✅ Filtro por persona
- ✅ Colores por tipo (violeta=Historia, azul=Tarea, rojo=Bug, ámbar=Spike)
- ✅ Badges de prioridad y referencia Jira en tarjetas

---

### 5. Dashboard del Sprint (RF-013)

Panel de métricas para monitoreo del LT.

- ✅ 4 métricas: capacidad total, % ocupación, tareas totales, bloqueos activos
- ✅ Donut chart — tareas por estado (recharts)
- ✅ Bar chart — distribución por persona
- ✅ AlertBox — alertas de sobreasignación y bloqueos
- ✅ Color semáforo en % ocupación (verde / naranja / rojo)

---

### 6. Resumen del Sprint (RF-014)

Página de resumen al cierre del sprint, generada desde el LF.

- ✅ Tareas completadas, en progreso, pendientes y bloqueadas
- ✅ Tabla de bloqueos encontrados con estado y resolución
- ✅ Exportación a Excel (XLSX)
- ✅ Solo accesible para sprints en estado ACTIVO o CERRADO
- ✅ Link desde DashboardPage

---

### 7. Gestión de Bloqueos (RF-015)

Registro y seguimiento de impedimentos del equipo.

- ✅ CRUD de bloqueos con título, descripción, tipo y estado (`ABIERTO → EN_GESTION → RESUELTO`)
- ✅ Relacionados con tareas afectadas
- ✅ Contador de bloqueos activos en dashboard
- ✅ Historial por sprint

**Endpoints:**

```
GET    /api/v1/bloqueos                      → Listar (filtro por estado)
POST   /api/v1/bloqueos                      → Crear bloqueo
GET    /api/v1/bloqueos/{id}                 → Obtener
PATCH  /api/v1/bloqueos/{id}/estado          → Cambiar estado
DELETE /api/v1/bloqueos/{id}                 → Eliminar
GET    /api/v1/bloqueos/activos/count        → Contador de bloqueos activos
GET    /api/v1/planificacion/{id}/dashboard  → Dashboard métricas sprint
GET    /api/v1/planificacion/{id}/timeline   → Grid timeline (personas × días)
GET    /api/v1/planificacion/{id}/export     → Export XLSX timeline
```

---

## 🐛 Hotfix v1.2.1 (22/02/2026)

### FIX-001 — Dashboard Home: sprint planificado eliminado

**Problema**: La página de inicio mostraba simultáneamente el sprint ACTIVO y el de PLANIFICACION del squad seleccionado.  
**Solución**: La query ahora filtra exclusivamente `estado=ACTIVO`.

### FEATURE-006 — Timeline: botón exportar a Excel

**Descripción**: Nuevo botón "Descargar" en la cabecera de la página de Timeline que exporta la planificación del sprint en formato XLSX con nombre dinámico `{sprint}_{fecha}.xlsx`.

---

## 📦 Cambios Técnicos

### Backend — Nuevas entidades

```
com.kaos.planificacion.entity.Sprint
  id, nombre, squadId, fechaInicio, fechaFin, objetivo, estado,
  capacidadTotal, createdAt, updatedAt

com.kaos.planificacion.entity.Tarea
  id, sprintId, titulo, descripcion, tipo, categoria, estimacion,
  prioridad, estado, personaId, diaAsignado, referenciaJira, createdAt

com.kaos.planificacion.entity.Bloqueo
  id, sprintId, titulo, descripcion, tipo, estado,
  tareasAfectadas (N:N), fechaResolucion, createdAt

com.kaos.planificacion.entity.SprintEstado (enum)
  PLANIFICACION, ACTIVO, CERRADO

com.kaos.planificacion.entity.EstadoBloqueo (enum)
  ABIERTO, EN_GESTION, RESUELTO
```

### Backend — Servicios nuevos

```
SprintService      → CRUD + cambiarEstado() con validación de transiciones
TareaService       → CRUD + cambiarEstado() + validación capacidad
BloqueoService     → CRUD + cambiarEstado() + contarActivos()
PlanificacionService → obtenerDashboard() + obtenerTimeline() + exportarTimeline()
```

### Backend — Excepciones nuevas

```
SolapamientoSprintException       → 409 CONFLICT
SprintNoEnPlanificacionException  → 422 UNPROCESSABLE_ENTITY
CapacidadInsuficienteException     → 409 CONFLICT
TareaNoEnPendienteException       → 422 UNPROCESSABLE_ENTITY
```

### Backend — Migraciones Liquibase

```
005-create-sprint.sql
006-create-tarea.sql
007-create-bloqueo.sql
008-create-bloqueo-tarea.sql
```

### Frontend — Nuevas rutas

```
/planificacion/{sprintId}            → PlanificacionPage (Timeline · Kanban · Dashboard)
/planificacion/{sprintId}/resumen    → ResumenPage
```

### Frontend — Nuevos componentes

```
SprintSelector       → Dropdown de sprints con badges estado y acciones
TaskCard             → Tarjeta de tarea (variantes compact/standard)
KanbanBoard          → Tablero con 4 columnas DnD
DashboardWidgets     → Panel métricas + recharts
ModalTarea           → Formulario crear/editar tarea
TimelineGrid         → Grid personas × días (drag & drop)
```

### Frontend — Nuevas dependencias

```json
"@hello-pangea/dnd": "^16.6.0"   (drag & drop)
"recharts": "^2.13.3"             (gráficos)
"xlsx": "^0.18.5"                 (exportación Excel)
```

---

## 🧪 Testing

### Backend

| Suite                       | Tests  | Resultado   |
| --------------------------- | ------ | ----------- |
| SprintServiceTest           | 15     | ✅ 0 fallos |
| TareaServiceTest            | 15     | ✅ 0 fallos |
| BloqueoServiceTest          | 14     | ✅ 0 fallos |
| PlanificacionServiceTest    | 9      | ✅ 0 fallos |
| SprintControllerTest        | 11     | ✅ 0 fallos |
| TareaControllerTest         | 11     | ✅ 0 fallos |
| BloqueoControllerTest       | 10     | ✅ 0 fallos |
| PlanificacionControllerTest | 6      | ✅ 0 fallos |
| **Total**                   | **91** | **✅ PASS** |

### Frontend (Vitest)

| Fichero                   | Tests  | Resultado   |
| ------------------------- | ------ | ----------- |
| TaskCard.test.tsx         | 9      | ✅ 0 fallos |
| KanbanBoard.test.tsx      | 5      | ✅ 0 fallos |
| DashboardWidgets.test.tsx | 6      | ✅ 0 fallos |
| SprintSelector.test.tsx   | 10     | ✅ 0 fallos |
| ModalTarea.test.tsx       | 16     | ✅ 0 fallos |
| **Total**                 | **46** | **✅ PASS** |

### Correcciones durante tests

- **SprintService**: lógica de transición de estados era permisiva (`// todas las transiciones permitidas`). Corregido a validación explícita `PLANIFICACION→ACTIVO` y `ACTIVO→CERRADO`.
- **GlobalExceptionHandler**: `TareaNoEnPendienteException` no tenía handler → devolvía 500. Añadido con HTTP 422.
- **BloqueoControllerTest**: `EstadoBloqueo.ACTIVO` no existe (el enum usa `ABIERTO`). Corregido.

---

## 📊 Database Changes

### Nuevas tablas

```sql
sprint        (id, nombre, squad_id, fecha_inicio, fecha_fin, objetivo, estado, capacidad_total)
tarea         (id, sprint_id, titulo, descripcion, tipo, categoria, estimacion, prioridad, estado,
               persona_id, dia_asignado, referencia_jira)
bloqueo       (id, sprint_id, titulo, descripcion, tipo, estado, fecha_resolucion)
bloqueo_tarea (bloqueo_id, tarea_id)                   ← tabla intermedia N:N
```

---

## 🚀 Deployment

```bash
cd kaos
docker-compose down
docker-compose up -d --build

# Verificar
curl http://localhost:6060/actuator/health
open http://localhost:6060/swagger-ui.html
```

| Servicio | URL                                   |
| -------- | ------------------------------------- |
| Frontend | http://localhost:2000                 |
| API REST | http://localhost:6060/api/v1          |
| Swagger  | http://localhost:6060/swagger-ui.html |

---

## 📝 Issues Conocidos (v1.2.1)

### GitHub Issue #1 — Excel Import: idempotencia y deduplicación

Reportado en https://github.com/gmayordo/kaos/issues/1

| #   | Problema                                              | Prioridad |
| --- | ----------------------------------------------------- | --------- |
| 1   | Ausencias duplicadas al cargar mismo Excel dos veces  | 🔴 Alta   |
| 2   | Mapeo columna→persona no persiste entre sesiones      | 🟡 Media  |
| 3   | Persona en dos Excel diferentes se carga dos veces    | 🟡 Media  |
| 4   | UI de configuración del comportamiento de importación | 🟢 Baja   |

---

## 🗺️ Roadmap — Próximos Bloques

### Bloque 4: Integración Jira — v1.3.0 (estimado marzo 2026)

- **HU-016**: Importar tareas desde Jira (RF-016) — sincronizar issues de sprint
- **HU-017**: Imputación centralizada de horas (RF-017) — Mi día / Mi semana
- **HU-018**: Sincronización KAOS ↔ Jira (RF-018) — fuente única de verdad
- **HU-019**: Configuración de conexión Jira (RF-019) — URL, token, mapeo de boards

### Bloque 5: Centro de Control LT — v1.4.0 (estimado abril 2026)

- **HU-020**: Panel diario del LT (RF-020) — imputaciones, desviaciones, alertas
- **HU-021**: Supervisión de imputaciones (RF-021) — quién imputó, cuánto, cuándo
- **HU-022**: Gestión de riesgos (RF-022) — registro y seguimiento
- **HU-023**: Informe para reunión con SM (RF-023) — auto-generado con IA asistida
- **HU-024**: Sistema de alertas inteligentes (RF-024) — motor configurable

---

**Release Manager**: Agente 13 🕵️‍♂️  
**QA**: 137 tests (91 backend + 46 frontend) — 0 fallos ✅  
**Status**: Production Ready 🚀
