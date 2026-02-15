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
  dias: number;
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
