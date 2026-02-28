/**
 * Tipos de la API generados manualmente desde el backend.
 * TODO: Generar automáticamente desde OpenAPI spec.
 */

// ============= Common =============

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  numberOfElements: number;
  empty: boolean;
}

export interface ErrorResponse {
  code: string;
  message: string;
  timestamp: string;
}

// ============= PerfilHorario =============

export interface PerfilHorarioResponse {
  id: number;
  nombre: string;
  zonaHoraria: string;
  horasLunes: number;
  horasMartes: number;
  horasMiercoles: number;
  horasJueves: number;
  horasViernes: number;
  totalSemanal: number;
  createdAt: string;
  updatedAt: string;
}

export interface PerfilHorarioRequest {
  nombre: string;
  zonaHoraria: string;
  horasLunes: number;
  horasMartes: number;
  horasMiercoles: number;
  horasJueves: number;
  horasViernes: number;
}

// ============= Squad =============

export type EstadoSquad = "ACTIVO" | "INACTIVO";

export interface SquadResponse {
  id: number;
  nombre: string;
  descripcion?: string;
  estado: EstadoSquad;
  idSquadCorrJira?: string;
  idSquadEvolJira?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SquadRequest {
  nombre: string;
  descripcion?: string;
  estado: EstadoSquad;
  idSquadCorrJira?: string;
  idSquadEvolJira?: string;
}

// ============= Persona =============

export type Rol =
  | "LIDER_TECNICO"
  | "LIDER_FUNCIONAL"
  | "FRONTEND"
  | "BACKEND"
  | "QA"
  | "SCRUM_MASTER";
export type Seniority = "JUNIOR" | "MID" | "SENIOR" | "LEAD";

export interface PersonaResponse {
  id: number;
  nombre: string;
  email: string;
  idJira?: string;
  perfilHorarioId: number;
  perfilHorarioNombre: string;
  ciudad: string;
  seniority: Seniority;
  skills?: string;
  costeHora?: number;
  activo: boolean;
  fechaIncorporacion?: string;
  sendNotifications: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PersonaRequest {
  nombre: string;
  email: string;
  idJira?: string;
  ciudad: string;
  perfilHorarioId: number;
  seniority: Seniority;
  skills?: string;
  costeHora?: number;
  fechaIncorporacion?: string;
  sendNotifications?: boolean;
}

// ============= SquadMember =============

export interface SquadMemberResponse {
  id: number;
  personaId: number;
  personaNombre: string;
  squadId: number;
  squadNombre: string;
  rol: Rol;
  porcentaje: number;
  fechaInicio?: string;
  fechaFin?: string;
  capacidadDiariaLunes: number;
  capacidadDiariaMartes: number;
  capacidadDiariaMiercoles: number;
  capacidadDiariaJueves: number;
  capacidadDiariaViernes: number;
  createdAt: string;
  updatedAt: string;
}

export interface SquadMemberRequest {
  personaId: number;
  squadId: number;
  rol: Rol;
  porcentaje: number;
  fechaInicio?: string;
  fechaFin?: string;
}

// ============= Festivo =============

export type TipoFestivo = "NACIONAL" | "REGIONAL" | "LOCAL";

export interface FestivoResponse {
  id: number;
  fecha: string;
  descripcion: string;
  tipo: TipoFestivo;
  ciudad: string;
  createdAt: string;
  updatedAt: string;
}

export interface FestivoRequest {
  fecha: string;
  descripcion: string;
  tipo: TipoFestivo;
  ciudad: string;
}

export interface FestivoCsvUploadResponse {
  procesadas: number;
  exitosas: number;
  errores: FestivoCsvError[];
}

export interface FestivoCsvError {
  fila: number;
  mensaje: string;
}

// ============= Vacacion =============

export type TipoVacacion =
  | "VACACIONES"
  | "ASUNTOS_PROPIOS"
  | "LIBRE_DISPOSICION"
  | "PERMISO";
export type EstadoVacacion = "SOLICITADA" | "REGISTRADA";

export interface VacacionResponse {
  id: number;
  personaId: number;
  personaNombre: string;
  fechaInicio: string;
  fechaFin: string;
  tipo: TipoVacacion;
  estado: EstadoVacacion;
  comentario?: string;
  diasLaborables: number;
  createdAt: string;
  updatedAt: string;
}

export interface VacacionRequest {
  personaId: number;
  fechaInicio: string;
  fechaFin: string;
  tipo: TipoVacacion;
  estado?: EstadoVacacion;
  comentario?: string;
}

// ============= Ausencia =============

export type TipoAusencia = "BAJA_MEDICA" | "EMERGENCIA" | "OTRO";

export interface AusenciaResponse {
  id: number;
  personaId: number;
  personaNombre: string;
  fechaInicio: string;
  fechaFin?: string;
  tipo: TipoAusencia;
  comentario?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AusenciaRequest {
  personaId: number;
  fechaInicio: string;
  fechaFin?: string;
  tipo: TipoAusencia;
  comentario?: string;
}

// ============= Capacidad =============

export interface CapacidadDiaResponse {
  fecha: string;
  horasDisponibles: number;
  horasTeoricas: number;
  motivoReduccion?: string;
}

export interface CapacidadPersonaResponse {
  personaId: number;
  personaNombre: string;
  horasDisponibles: number;
  horasTeoricas: number;
  porcentajeCapacidad: number;
  diasDisponibles: number;
  diasReducidos: number;
  detalles: CapacidadDiaResponse[];
}

export interface CapacidadSquadResponse {
  squadId: number;
  squadNombre: string;
  fechaInicio: string;
  fechaFin: string;
  horasTotales: number;
  diasLaborables: number;
  personas: CapacidadPersonaResponse[];
}
// ============= Planificación — Sprint =============

export type SprintEstado = "PLANIFICACION" | "ACTIVO" | "CERRADO";

export interface SprintResponse {
  id: number;
  nombre: string;
  squadId: number;
  squadNombre: string;
  fechaInicio: string;
  fechaFin: string;
  objetivo?: string;
  estado: SprintEstado;
  capacidadTotal: number;
  tareasPendientes: number;
  tareasEnProgreso: number;
  tareasCompletadas: number;
  createdAt: string;
}

export interface SprintRequest {
  nombre: string;
  squadId: number;
  fechaInicio: string;
  objetivo?: string;
}

// ============= Planificación — Tarea =============

export type TipoTarea = "HISTORIA" | "TAREA" | "BUG" | "SPIKE";
export type CategoriaTarea = "CORRECTIVO" | "EVOLUTIVO";
export type PrioridadTarea = "BAJA" | "NORMAL" | "ALTA" | "BLOQUEANTE";
export type EstadoTarea =
  | "PENDIENTE"
  | "EN_PROGRESO"
  | "BLOQUEADO"
  | "COMPLETADA";

export interface TareaResponse {
  id: number;
  titulo: string;
  sprintId: number;
  personaId?: number;
  personaNombre?: string;
  tipo: TipoTarea;
  categoria: CategoriaTarea;
  estimacion: number;
  prioridad: PrioridadTarea;
  estado: EstadoTarea;
  diaAsignado?: number;
  diaCapacidadDisponible?: number;
  bloqueada: boolean;
  referenciaJira?: string;
  createdAt: string;
  // Bloque 5: jerarquía Jira + dependencias
  tareaParentId?: number;
  jiraIssueSummary?: string;
  jiraEstimacionHoras?: number;
  jiraIssueKey?: string;
  jiraHorasConsumidas?: number;
}

export interface TareaRequest {
  titulo: string;
  sprintId: number;
  descripcion?: string;
  tipo: string;
  categoria: string;
  estimacion: number;
  prioridad: string;
  personaId?: number;
  diaAsignado?: number;
  referenciaJira?: string;
  estado?: string;
}

// ============= Planificación — Bloqueo =============

export type TipoBloqueo =
  | "DEPENDENCIA_EXTERNA"
  | "RECURSO"
  | "TECNICO"
  | "COMUNICACION"
  | "OTRO";
export type EstadoBloqueo = "ACTIVO" | "EN_GESTION" | "RESUELTO";

export interface BloqueoResponse {
  id: number;
  titulo: string;
  descripcion?: string;
  tipo: TipoBloqueo;
  estado: EstadoBloqueo;
  responsableId?: number;
  responsableNombre?: string;
  fechaResolucion?: string;
  notas?: string;
  tareasAfectadas: number;
  createdAt: string;
}

export interface BloqueoRequest {
  titulo: string;
  descripcion?: string;
  tipo: string;
  estado?: string;
  responsableId?: number;
  notas?: string;
}

// ============= Planificación — Dashboard =============

export interface DashboardSprintResponse {
  sprintId: number;
  sprintNombre: string;
  estado: SprintEstado;
  tareasTotal: number;
  tareasPendientes: number;
  tareasEnProgreso: number;
  tareasCompletadas: number;
  tareasBloqueadas: number;
  progresoEsperado: number;
  progresoReal: number;
  capacidadTotalHoras: number;
  capacidadAsignadaHoras: number;
  ocupacionPorcentaje: number;
  bloqueosActivos: number;
  alertas: string[];
  fechaInicio: string;
  fechaFin: string;
}

// ============= Planificación — Timeline =============

export interface TareaEnLinea {
  tareaId: number;
  titulo: string;
  estimacion: number;
  estado: EstadoTarea;
  prioridad: PrioridadTarea;
  bloqueada: boolean;
}

export interface DiaConTareas {
  dia: number;
  horasDisponibles: number;
  tareas: TareaEnLinea[];
}

export interface PersonaEnLinea {
  personaId: number;
  personaNombre: string;
  dias: DiaConTareas[];
}

export interface TimelineSprintResponse {
  sprintId: number;
  sprintNombre: string;
  fechaInicio: string;
  fechaFin: string;
  personas: PersonaEnLinea[];
}

// ============= Planificación — Jira Issues (Bloque 5) =============

export interface PlanificarAsignacionItem {
  jiraKey: string;
  personaId?: number;
  estimacion?: number;
  diaAsignado?: number;
  tipo?: string;
  categoria?: string;
  prioridad?: string;
}

export interface PlanificarIssueRequest {
  sprintId: number;
  asignaciones: PlanificarAsignacionItem[];
}

// ============= Planificación — Dependencias (Bloque 5) =============

export type TipoDependencia = "ESTRICTA" | "SUAVE";

export interface TareaDependenciaResponse {
  id: number;
  tareaOrigenId: number;
  tareaOrigenTitulo: string;
  tareaDestinoId: number;
  tareaDestinoTitulo: string;
  tipo: TipoDependencia;
  createdAt: string;
}

export interface CrearDependenciaRequest {
  tareaDestinoId: number;
  tipo: TipoDependencia;
}

// ============= Planificación — Plantillas (Bloque 5) =============

export type RolPlantilla =
  | "DESARROLLADOR"
  | "QA"
  | "TECH_LEAD"
  | "FUNCIONAL"
  | "OTRO";

export interface PlantillaAsignacionLineaResponse {
  id: number;
  rol: RolPlantilla;
  porcentajeHoras: number;
  orden: number;
  dependeDeOrden?: number;
}

export interface PlantillaAsignacionResponse {
  id: number;
  nombre: string;
  tipoJira: string;
  activo: boolean;
  lineas: PlantillaAsignacionLineaResponse[];
}

export interface PlantillaAsignacionLineaRequest {
  rol: RolPlantilla;
  porcentajeHoras: number;
  orden: number;
  dependeDeOrden?: number;
}

export interface PlantillaAsignacionRequest {
  nombre: string;
  tipoJira: string;
  activo?: boolean;
  lineas: PlantillaAsignacionLineaRequest[];
}

// ============= Excel Import =============

export interface ExcelPersonaMatch {
  nombreExcel: string;
  personaId: number;
  personaNombre: string;
}

export interface ExcelAnalysisResponse {
  totalFilasPersona: number;
  personasResueltas: ExcelPersonaMatch[];
  personasNoResueltas: string[];
}

export interface ExcelImportResponse {
  personasProcesadas: number;
  vacacionesCreadas: number;
  ausenciasCreadas: number;
  personasNoEncontradas: string[];
  errores: string[];
}
