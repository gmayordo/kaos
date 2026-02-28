# KAOS v1.4.0 Release Notes

**Release Date**: 28 de febrero de 2026

---

## 📋 Resumen Ejecutivo

**KAOS v1.4.0** integra el **Bloque 4: Integración Jira** y el **Bloque 5: Planificación Avanzada con Issues Jira**.
Incluye sincronización bidireccional con Jira Server/Data Center, motor de alertas configurable,
jerarquía de tareas padre-hijo, dependencias entre tareas y plantillas de asignación automática.

| Aspecto                                  | Estado                                 |
| ---------------------------------------- | -------------------------------------- |
| **Bloque 4: Integración Jira**           | ✅ Completo (HU-016 a HU-020)          |
| **Bloque 5: Planificación Avanzada**     | ✅ Completo (TASK-001 a TASK-025)      |
| **Backend — 6+ controllers + servicios** | ✅ 25+ endpoints REST nuevos           |
| **Frontend — 3 páginas + 3 componentes** | ✅ IssuesPage · PlantillasPage · Modal |
| **Tests Backend**                        | ✅ 35 tests nuevos (4 suites)          |
| **Tests Frontend**                       | ✅ 43 tests nuevos (3 ficheros Vitest) |
| **H2 Test Isolation**                    | ✅ src/test/resources/application.yml  |
| **Liquibase H2 Compatibility**           | ✅ 10 changelogs corregidos            |

---

## 🎯 Bloque 4 — Integración Jira

### 1. Configuración de Conexión Jira (HU-016)

- ✅ Triple metodología: `API_REST` (oficial), `SELENIUM` (headless), `LOCAL` (solo caché)
- ✅ Token cifrado con AES/GCM en BD (`AesEncryptConverter`)
- ✅ Configuración por squad: URL, usuario, token, boardIds, mapeo de estados

**Endpoints:**

```
GET    /api/v1/jira/config           → Obtener configuración activa
PUT    /api/v1/jira/config           → Actualizar configuración
POST   /api/v1/jira/sync             → Disparar sincronización manual
GET    /api/v1/jira/sync/status      → Estado del último ciclo de sync
```

---

### 2. Rate Limiting con Cola de Espera (HU-017)

- ✅ Límite 200 llamadas/2h para API_REST (umbral seguro: 195)
- ✅ `JiraRateLimiter` con ventana deslizante, registro en BD
- ✅ `JiraSyncQueue` para peticiones en cola cuando límite alcanzado
- ✅ Reintento automático con delay configurable

---

### 3. Sincronización Issues + Subtareas + Worklogs (HU-016)

- ✅ Importación completa de issues Jira con subtareas embebidas
- ✅ Worklogs sincronizados con mapeado a persona KAOS por `author.key`
- ✅ Caché local en BD para consultas sin límite de llamadas
- ✅ `JiraImportService` con upsert eficiente (checksum para evitar updates innecesarios)
- ✅ Remote links importados como referencias entre issues

---

### 4. Motor de Alertas Configurable (HU-019)

- ✅ Reglas en BD (tabla `jira_alert_rule`) evaluadas con SpEL
- ✅ 7 reglas predefinidas: issue sin asignar, sprint sobrecargado, bloqueo sin resolver, etc.
- ✅ Severidades: `INFO`, `WARNING`, `CRITICO`
- ✅ Notificaciones en pantalla + resumen HTML por correo

**Endpoints:**

```
GET    /api/v1/jira/alertas          → Listar alertas activas (filtro por tipo/severidad)
PATCH  /api/v1/jira/alertas/{id}     → Marcar como resuelta
GET    /api/v1/jira/alert-rules      → Listar reglas configuradas
PUT    /api/v1/jira/alert-rules/{id} → Activar/desactivar regla
```

---

### 5. Correo Resumen HTML Post-Sync (HU-020)

- ✅ Envío automático tras cada ciclo de sincronización (configurable)
- ✅ HTML con tabla de issues nuevos, alertas CRITICO y métricas de sync
- ✅ Activación por flag: `kaos.email.habilitado: true`

---

## 🎯 Bloque 5 — Planificación Avanzada con Issues Jira

### 6. Jerarquía Padre-Hijo en Tareas

- ✅ Campo `tarea_parent_id` (FK auto-referenciada, nullable)
- ✅ Subtareas Jira vinculadas a su tarea padre KAOS automáticamente
- ✅ `TareaResponse` incluye `subtareas[]` embebido en un solo endpoint
- ✅ Cascade `ON DELETE SET NULL` al eliminar tarea padre

---

### 7. Dependencias entre Tareas (TASK-009 a TASK-013)

- ✅ Entidad `TareaDependencia` con tipo: `BLOQUEANTE`, `NECESARIA`, `RECOMENDADA`
- ✅ Detección de ciclos mediante BFS en `TareaDependenciaService`
- ✅ Validación: no se puede crear dependencia circular
- ✅ `DependenciaCiclicaException` con traza del ciclo detectado

**Endpoints:**

```
POST   /api/v1/tareas/{id}/dependencias            → Añadir dependencia
DELETE /api/v1/tareas/{id}/dependencias/{depId}    → Eliminar dependencia
GET    /api/v1/tareas/{id}/dependencias            → Listar dependencias
```

---

### 8. Plantillas de Asignación Automática (TASK-014 a TASK-018)

- ✅ Plantillas configurables en BD (`plantilla_asignacion` + `plantilla_asignacion_linea`)
- ✅ Asignación automática de personas al planificar issue Jira
- ✅ Operaciones: crear, actualizar, eliminar, aplicar plantilla
- ✅ Filtro por tipo Jira (`tipo_jira`) y estado activo

**Endpoints:**

```
GET    /api/v1/plantillas            → Listar plantillas activas
POST   /api/v1/plantillas            → Crear plantilla
PUT    /api/v1/plantillas/{id}       → Actualizar plantilla
DELETE /api/v1/plantillas/{id}       → Eliminar plantilla
POST   /api/v1/plantillas/{id}/aplicar → Aplicar plantilla a sprint
```

---

### 9. Planificación de Issues Jira (TASK-019 a TASK-025)

- ✅ `GET /api/v1/jira/issues`: lista issues con subtareas embebidas + sugerencia de asignación
- ✅ `POST /api/v1/jira/issues/planificar`: crea N tareas KAOS en una transacción atómica
- ✅ `PlanificarIssueService`: asigna persona, sprint, horas; respeta capacidad disponible
- ✅ Frontend `IssuesPage`: listado de issues con estado de planificación
- ✅ Frontend `ModalPlanificarIssue`: formulario de planificación con subtareas, persona, sprint, horas
- ✅ Frontend `PlantillasPage`: CRUD completo de plantillas con líneas de asignación
- ✅ Integración TanStack Router + Query v5

---

## 🔧 Mejoras de Infraestructura

### H2 Test Isolation

- ✅ `src/test/resources/application.yml` con H2 in-memory (`MODE=PostgreSQL`)
- ✅ Tests completamente aislados del entorno dev PostgreSQL
- ✅ 0 errores de contexto Spring en test suite completo

### Liquibase H2 Compatibility

Se corrigieron **10 changelogs** con syntax incompatible con H2:

| Cambio                         | Archivos afectados                |
| ------------------------------ | --------------------------------- |
| Partial indexes (WHERE clause) | 019, 022, 024, 025, 026, 034, 036 |
| Multi-column ALTER TABLE       | 028, 033, 037                     |

---

## 📊 Métricas de Calidad

| Métrica                     | Valor          |
| --------------------------- | -------------- |
| Tests backend totales       | 544            |
| Tests backend nuevos        | 35             |
| Tests frontend nuevos       | 43             |
| Errores de contexto         | 0              |
| Changelogs Liquibase nuevos | 20             |
| Endpoints REST nuevos       | 25+            |
| Build frontend              | ✅ Sin errores |

---

## 📁 Nuevas Tablas en Base de Datos

| Tabla                        | Bloque | Descripción                              |
| ---------------------------- | ------ | ---------------------------------------- |
| `jira_config`                | B4     | Configuración de conexión Jira por squad |
| `jira_api_call_log`          | B4     | Log de llamadas para rate limiting       |
| `jira_sync_queue`            | B4     | Cola de sincronización pendiente         |
| `jira_sync_status`           | B4     | Estado del último ciclo sync             |
| `jira_issue`                 | B4     | Caché local de issues Jira               |
| `jira_alert_rule`            | B4     | Reglas de alerta configurables           |
| `jira_alerta`                | B4     | Alertas generadas por el motor           |
| `jira_worklog`               | B4     | Worklogs sincronizados desde Jira        |
| `jira_comment`               | B4     | Comentarios importados desde Jira        |
| `jira_remote_link`           | B4     | Remote links entre issues                |
| `tarea_dependencia`          | B5     | Dependencias tipadas entre tareas        |
| `plantilla_asignacion`       | B5     | Plantillas de asignación automática      |
| `plantilla_asignacion_linea` | B5     | Líneas de cada plantilla                 |

---

## 🐛 Bugs Corregidos

| Bug                                         | Solución                                                                                   |
| ------------------------------------------- | ------------------------------------------------------------------------------------------ |
| `CapacidadControllerTest` — 8 fallos        | Campo `detalles` vs `dias`, `doesNotExist()` para null, `EntityNotFoundException` para 404 |
| Liquibase context startup failures en tests | H2 compatibility fixes (partial indexes, multi-column ALTER)                               |
| Checksums Liquibase en PostgreSQL dev       | Aislamiento tests a H2 in-memory                                                           |

---

## ⬆️ Notas de Migración

### Backend

Las siguientes migraciones Liquibase se aplican automáticamente al arrancar:

- Changesets `017` a `038` (nuevas tablas, columnas y datos semilla)
- Los partial indexes han sido convertidos a índices regulares (sin impacto funcional en PostgreSQL)

### Frontend

```bash
npm install  # actualizar dependencias
npm run build
```

---

## Versiones de Componentes

| Componente             | Versión |
| ---------------------- | ------- |
| Backend (kaos-backend) | 1.4.0   |
| Frontend               | 1.4.0   |
| Java                   | 21      |
| Spring Boot            | 3.4.2   |
| React                  | 18.3.1  |
| TanStack Router        | v5      |
| TanStack Query         | v5      |
