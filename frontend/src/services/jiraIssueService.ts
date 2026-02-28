/**
 * Servicio API para Issues Jira — Planificación (Bloque 5)
 * Gestión de issues Jira y su conversión a tareas KAOS.
 */

import { api } from "./api";
import type {
  PlanificarAsignacionItem,
  PlanificarIssueRequest,
  TareaResponse,
} from "@/types/api";
import type {
  JiraIssueResponse,
  SugerenciaAsignacionResponse,
} from "@/types/jira";

export interface JiraIssueFilters {
  /** Si true, solo devuelve los que no tienen tareaId */
  soloPendientes?: boolean;
  /** Filtrar por tipo de issue */
  tipo?: string;
  /** Búsqueda de texto libre en jiraKey o summary */
  q?: string;
  /** Sprint al que pertenece el issue */
  sprintNombre?: string;
}

export const jiraIssueService = {
  /**
   * Lista issues Jira con subtareas embebidas.
   * GET /api/v1/jira/issues
   */
  listar: async (filters?: JiraIssueFilters): Promise<JiraIssueResponse[]> => {
    const params = new URLSearchParams();
    if (filters?.soloPendientes) params.append("soloPendientes", "true");
    if (filters?.tipo) params.append("tipo", filters.tipo);
    if (filters?.q) params.append("q", filters.q);
    if (filters?.sprintNombre)
      params.append("sprintNombre", filters.sprintNombre);

    const { data } = await api.get<JiraIssueResponse[]>(
      `/jira/issues?${params.toString()}`,
    );
    return data;
  },

  /**
   * Planifica uno o varios issues Jira creando tareas KAOS en un sprint.
   * POST /api/v1/jira/issues/planificar
   */
  planificar: async (
    request: PlanificarIssueRequest,
  ): Promise<TareaResponse[]> => {
    const { data } = await api.post<TareaResponse[]>(
      "/jira/issues/planificar",
      request,
    );
    return data;
  },

  /**
   * Sugerencia de persona para un issue concreto.
   * GET /api/v1/jira/issues/:jiraKey/sugerencia?sprintId=X
   */
  sugerencia: async (
    jiraKey: string,
    sprintId: number,
  ): Promise<SugerenciaAsignacionResponse[]> => {
    const { data } = await api.get<SugerenciaAsignacionResponse[]>(
      `/jira/issues/${jiraKey}/sugerencia?sprintId=${sprintId}`,
    );
    return data;
  },

  /**
   * Devuelve las asignaciones sugeridas usando la plantilla del tipoJira.
   * GET /api/v1/plantillas/aplicar?tipoJira=Story&estimacion=10
   */
  aplicarPlantilla: async (
    tipoJira: string,
    estimacion: number,
  ): Promise<PlanificarAsignacionItem[]> => {
    const { data } = await api.get<PlanificarAsignacionItem[]>(
      `/plantillas/aplicar?tipoJira=${encodeURIComponent(tipoJira)}&estimacion=${estimacion}`,
    );
    return data;
  },
};
