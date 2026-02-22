# Changelog — Bloque 2 Frontend: Calendario

**Fecha**: 2026-02-15
**Agente**: Hymie (Frontend Developer)
**Estado**: ✅ Completado

---

## Resumen

Implementación completa del frontend para el Bloque 2 (Calendario), incluyendo gestión de festivos, vacaciones, ausencias y dashboard de capacidad.

---

## Archivos Creados

### Tipos TypeScript (`/src/types/api.ts`)

- ✅ `FestivoResponse`, `FestivoRequest`, `TipoFestivo`
- ✅ `FestivoCsvUploadResponse`, `FestivoCsvError`
- ✅ `VacacionResponse`, `VacacionRequest`, `TipoVacacion`, `EstadoVacacion`
- ✅ `AusenciaResponse`, `AusenciaRequest`, `TipoAusencia`
- ✅ `CapacidadDiaResponse`, `CapacidadPersonaResponse`, `CapacidadSquadResponse`

### Servicios API (`/src/services/`)

- ✅ `festivoService.ts` — CRUD + carga CSV + filtros por año/tipo
- ✅ `vacacionService.ts` — CRUD + consultas por squad/persona
- ✅ `ausenciaService.ts` — CRUD + consultas por squad/persona
- ✅ `capacidadService.ts` — Cálculo de capacidad por squad

### Componentes (`/src/features/calendario/`)

- ✅ `EventoBadge.tsx` — Badge reutilizable para eventos (festivo/vacación/ausencia/libre)
- ✅ `FestivoForm.tsx` — Formulario modal para crear/editar festivos con multi-select de personas
- ✅ `VacacionForm.tsx` — Formulario modal para registrar vacaciones con cálculo de días
- ✅ `AusenciaForm.tsx` — Formulario modal para registrar ausencias (con fecha fin opcional)
- ✅ `index.ts` — Barrel exports

### Páginas (`/src/routes/`)

- ✅ `configuracion/festivos.tsx` — Gestión de festivos con tabla, filtros, CSV upload
- ✅ `calendario.tsx` — Calendario del squad con lista de vacaciones y ausencias por mes
- ✅ `capacidad.tsx` — Dashboard de capacidad con barras visuales y detalle día a día

### Layout

- ✅ Actualizado `__root.tsx` — Añadido enlace "Calendario" en sidebar con icono Calendar

---

## Funcionalidades Implementadas

### Pantalla Festivos (`/configuracion/festivos`)

- [x] Tabla de festivos con columnas: Fecha, Descripción, Tipo, Personas, Acciones
- [x] Filtro por año (2024-2028)
- [x] Botón "Cargar CSV" con upload de archivo
- [x] Botón "Nuevo" para crear festivo
- [x] Acciones: Editar (✏️), Eliminar (🗑)
- [x] Formulario modal con multi-select de personas (chips)
- [x] Resultado de carga CSV con errores detallados
- [x] Estados: Loading (skeleton), Empty (icono + mensaje), Error (banner)
- [x] Badge de tipo festivo con emoji (🇪🇸 Nacional / 📍 Regional / 🏘️ Local)

### Pantalla Calendario (`/calendario`)

- [x] Selector de squad
- [x] Navegación mensual con botones < >
- [x] Botón "Registrar" que abre selector de tipo (Vacación/Ausencia)
- [x] Lista de vacaciones del mes con badges azules
- [x] Lista de ausencias del mes con badges naranjas
- [x] Botón eliminar (×) en cada evento
- [x] Formularios modales para vacación y ausencia
- [x] Detalle: persona, fechas, duración, comentario
- [x] Leyenda de colores (🔵 Vacaciones / 🟠 Ausencias / ⚪ Festivos / 🟢 Libre disp.)
- [x] Estados: Sin squad (placeholder), Loading (skeleton), Sin datos (mensaje)

### Dashboard Capacidad (`/capacidad`)

- [x] Selector de squad + rango de fechas (inicio/fin)
- [x] Botón "Calcular" para ejecutar cálculo
- [x] Card resumen: horas totales + días laborables + rango
- [x] Cards por persona con:
  - Nombre
  - Días disponibles y reducidos
  - Barra visual de capacidad (verde/amarillo/rojo)
  - Horas disponibles / horas teóricas
  - Porcentaje de capacidad
- [x] Detalle día a día expandible (tabla con fecha, día, horas, motivo reducción)
- [x] Colores según capacidad: >80% verde, 50-79% amarillo, <50% rojo
- [x] Filas con reducción: rojo (0h) o amarillo (parcial)
- [x] Estados: Sin calcular (placeholder), Loading (skeleton), Error (banner)

---

## Patrones Aplicados

### Consistencia con Bloque 1

- ✅ Estructura de archivos: `/features/{dominio}/` para componentes, `/services/` para API, `/types/api.ts` para tipos
- ✅ Query con TanStack Query: `useQuery` para lectura, `useMutation` para escritura
- ✅ Invalidación de queries tras mutaciones exitosas
- ✅ Formularios modales con overlay negro semi-transparente
- ✅ Botones: Cancelar (border zinc) a la izquierda, Guardar (primary blue) a la derecha
- ✅ Estados de carga: skeleton con `animate-pulse bg-zinc-100`
- ✅ Estados vacíos: icono + mensaje + CTA
- ✅ Validaciones inline en formularios (alerts por ahora, TODO: mensajes bajo campos)

### Mejoras Futuras (Notas para siguientes sprints)

- [ ] Calendario mensual tipo grid (7x6) con celdas clickeables (actualmente es lista)
- [ ] Popover con detalle al hover evento en calendario
- [ ] Validación solapamiento vacaciones (actualmente en backend, falta feedback UI)
- [ ] Drag & drop visual para CSV upload (actualmente input file oculto)
- [ ] Tooltips en columna "Personas" de festivos (mostrar nombres)
- [ ] Loading states más granulares (spinner inline vs full page)
- [ ] Toast notifications en lugar de `alert()`
- [ ] Breadcrumbs en pantallas secundarias
- [ ] Responsive completo (actualmente funcional en desktop)
- [ ] Tests unitarios para componentes

---

## Verificado en Base al Handoff UX

| Requisito UX                                     | Estado |
| ------------------------------------------------ | ------ |
| Festivos: Tabla con 5 columnas                   | ✅     |
| Festivos: Filtro por año                         | ✅     |
| Festivos: CSV upload con reporte                 | ✅     |
| Festivos: Multi-select personas con chips        | ✅     |
| Calendario: Selector squad + navegación mes      | ✅     |
| Calendario: Lista vacaciones/ausencias           | ✅     |
| Calendario: Botón registrar con selector tipo    | ✅     |
| Calendario: Leyenda de colores                   | ✅     |
| Capacidad: Selector squad + rango fechas         | ✅     |
| Capacidad: Card resumen total                    | ✅     |
| Capacidad: Barra visual por persona              | ✅     |
| Capacidad: Detalle día a día expandible          | ✅     |
| Capacidad: Colores según % (verde/amarillo/rojo) | ✅     |

---

## Comandos para Probar

```bash
# Frontend
cd frontend
npm run dev

#访问
# http://localhost:5173/configuracion/festivos
# http://localhost:5173/calendario
# http://localhost:5173/capacidad
```

**Nota**: Backend debe estar corriendo en `http://localhost:8080` para que las llamadas API funcionen.

---

## Siguiente Paso

**Agente sugerido**: Maxwell Smart (Backend) para implementar:

- Endpoint POST `/festivos/csv` con parsing de CSV
- Endpoint GET `/vacaciones/squad/{id}` con filtro rango fechas
- Endpoint GET `/ausencias/squad/{id}` con filtro rango fechas
- Endpoint GET `/capacidad/squad/{id}` con lógica de cálculo

Referencia: [handoff-desarrollo.yaml](file:///Users/gmayordo/Documents/git/Gerardo/ehcos-ai-prompts/projects/kaos/bloque-2/handoff-desarrollo.yaml) tareas TASK-015 a TASK-021.
