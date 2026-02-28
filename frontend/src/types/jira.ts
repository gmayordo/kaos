/**
 * Tipos TypeScript para el dominio Jira (Bloque 4).
 * Mapeados desde los Java records del backend.
 */

// ============= Enums =============

export type JiraLoadMethod = "API_REST" | "SELENIUM" | "LOCAL";

export type EstadoSync = "IDLE" | "SINCRONIZANDO" | "CUOTA_AGOTADA" | "ERROR";

export type TipoOperacion = "SYNC_COMPLETA" | "SYNC_ISSUES" | "SYNC_WORKLOGS";

export type EstadoOperacion =
  | "PENDIENTE"
  | "PROCESANDO"
  | "COMPLETADA"
  | "ERROR";

export type WorklogOrigen = "JIRA" | "KAOS";

export type Severidad = "CRITICO" | "AVISO" | "INFO";

export type TipoAlerta =
  | "TAREA_SIN_WORKLOG"
  | "ESTIMACION_DESVIADA"
  | "WORKLOG_SIN_TAREA"
  | "CO_DESARROLLADOR_DETECTADO"
  | "CUSTOM";

// ============= Jira Config =============

export interface JiraConfigResponse {
  squadId: number;
  squadNombre: string;
  url: string;
  usuario: string;
  /** "****" si existe, "no configurado" si no */
  tokenOculto: string;
  loadMethod: JiraLoadMethod;
  activa: boolean;
  /** JSON string con el mapeo de estados Jira → KAOS (puede ser null) */
  mapeoEstados: string | null;
}

export interface JiraMethodRequest {
  method: JiraLoadMethod;
}

/**
 * Request para crear o actualizar la configuración Jira de un squad.
 * Si `token` es vacío al hacer PUT en un update, el token existente se mantiene.
 */
export interface JiraConfigRequest {
  url: string;
  usuario: string;
  /** Vacío = no cambiar el token existente (solo en update) */
  token: string;
  boardCorrectivoId: number | null;
  boardEvolutivoId: number | null;
  /** API_REST | SELENIUM | LOCAL */
  loadMethod: JiraLoadMethod;
  activa: boolean;
  /** JSON: {"Done":"COMPLETADA","In Progress":"EN_PROGRESO","To Do":"PENDIENTE"} */
  mapeoEstados: string | null;
}

// ============= Jira Sync =============

export interface JiraSyncStatusResponse {
  squadId: number;
  squadNombre: string;
  estado: EstadoSync;
  ultimaSync: string | null; // ISO LocalDateTime
  issuesImportadas: number;
  worklogsImportados: number;
  commentsImportados: number;
  remoteLinksImportados: number;
  callsConsumidas2h: number;
  callsRestantes2h: number;
  ultimoError: string | null;
  operacionesPendientes: number;
  updatedAt: string | null;
}

export interface JiraSyncQueueResponse {
  id: number;
  squadId: number;
  squadNombre: string;
  tipoOperacion: TipoOperacion;
  estado: EstadoOperacion;
  intentos: number;
  maxIntentos: number;
  programadaPara: string | null;
  ejecutadaAt: string | null;
  errorMensaje: string | null;
  createdAt: string;
}

// ============= Jira Worklogs =============

export interface JiraWorklogResponse {
  id: number;
  jiraKey: string;
  issueSummary: string;
  personaId: number;
  personaNombre: string;
  fecha: string; // yyyy-MM-dd
  horas: number;
  comentario: string | null;
  origen: WorklogOrigen;
  sincronizado: boolean;
}

export interface WorklogRequest {
  jiraKey: string;
  personaId: number;
  fecha: string; // yyyy-MM-dd
  horas: number;
  comentario?: string;
}

export interface WorklogLineaResponse {
  worklogId: number;
  jiraKey: string;
  issueSummary: string;
  horas: number;
  comentario: string | null;
  sincronizado: boolean;
}

export interface WorklogDiaResponse {
  fecha: string;
  personaId: number;
  personaNombre: string;
  horasCapacidad: number;
  horasImputadas: number;
  jornadaCompleta: boolean;
  worklogs: WorklogLineaResponse[];
}

export interface CeldaDiaResponse {
  fecha: string;
  horas: number;
  worklogId: number | null;
}

export interface FilaTareaResponse {
  jiraKey: string;
  issueSummary: string;
  dias: CeldaDiaResponse[];
  totalHorasTarea: number;
}

export interface WorklogSemanaResponse {
  semanaInicio: string; // yyyy-MM-dd (lunes)
  semanaFin: string; // yyyy-MM-dd (viernes)
  personaId: number;
  personaNombre: string;
  horasCapacidadDia: number;
  totalHorasSemana: number;
  totalCapacidadSemana: number;
  filas: FilaTareaResponse[];
}

// ============= Jira Alertas =============

export interface AlertaResponse {
  id: number;
  sprintId: number;
  squadId: number;
  reglaId: number;
  reglaNombre: string;
  severidad: Severidad;
  mensaje: string;
  jiraKey: string | null;
  personaNombre: string | null;
  resuelta: boolean;
  notificadaEmail: boolean;
  createdAt: string;
}

export interface AlertRuleResponse {
  id: number;
  squadId: number | null;
  nombre: string;
  descripcion: string | null;
  tipo: TipoAlerta;
  condicionSpel: string;
  mensajeTemplate: string;
  severidad: Severidad;
  umbralValor: number | null;
  activa: boolean;
}

export interface AlertRuleRequest {
  squadId?: number;
  nombre: string;
  descripcion?: string;
  tipo: TipoAlerta;
  condicionSpel: string;
  mensajeTemplate: string;
  severidad: Severidad;
  umbralValor?: number;
  activa: boolean;
}

// ============= Jira Issues — Planificaci\u00f3n (Bloque 5) =============

export type TipoIssueJira =
  | "Story"
  | "Task"
  | "Bug"
  | "Spike"
  | "Sub-task"
  | string;

export interface JiraIssueResponse {
  id: number;
  jiraKey: string;
  summary: string;
  tipoJira: TipoIssueJira;
  asignadoNombre: string | null;
  asignadoJiraId: string | null;
  estimacionHoras: number | null;
  horasConsumidas: number | null;
  estadoJira: string | null;
  estadoKaos: string | null;
  parentKey: string | null;
  sprintNombre: string | null;
  tareaId: number | null; // null = no planificado
  tareaEstado: string | null;
  subtareas: JiraIssueResponse[];
}

export interface SugerenciaAsignacionResponse {
  personaId: number;
  personaNombre: string;
  horasDisponibles: number;
  puntuacion: number;
  motivo: string;
}
