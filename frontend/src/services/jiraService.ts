/**
 * Servicio API para el dominio Jira (Bloque 4).
 * Capa centralizada — nunca hacer fetch directo en componentes.
 */

import { api } from "./api";
import type { PageResponse } from "@/types/api";
import type {
  AlertaResponse,
  AlertRuleRequest,
  AlertRuleResponse,
  JiraConfigRequest,
  JiraConfigResponse,
  JiraLoadMethod,
  JiraSyncQueueResponse,
  JiraSyncStatusResponse,
  JiraWorklogResponse,
  WorklogDiaResponse,
  WorklogRequest,
  WorklogSemanaResponse,
} from "@/types/jira";

// ─────────────────────────────────────────────────────────────────
// Jira Config — GET /jira/config / PATCH /method / POST /{id}/test
// ─────────────────────────────────────────────────────────────────

export const jiraConfigService = {
  /**
   * Obtiene la configuración Jira activa de un squad.
   * El token se devuelve ofuscado ("****").
   */
  obtenerConfig: async (squadId: number): Promise<JiraConfigResponse> => {
    const { data } = await api.get<JiraConfigResponse>(
      `/jira/config/${squadId}`,
    );
    return data;
  },

  /**
   * Crea o actualiza la configuración Jira de un squad.
   * Si token viene vacío, el backend mantiene el existente.
   */
  guardarConfig: async (
    squadId: number,
    request: JiraConfigRequest,
  ): Promise<JiraConfigResponse> => {
    const { data } = await api.put<JiraConfigResponse>(
      `/jira/config/${squadId}`,
      request,
    );
    return data;
  },

  /**
   * Cambia el método de carga de Jira en caliente (sin reiniciar).
   */
  cambiarMetodo: async (
    method: JiraLoadMethod,
  ): Promise<{ method: string; description: string }> => {
    const { data } = await api.patch<{ method: string; description: string }>(
      "/jira/config/method",
      { method },
    );
    return data;
  },

  /**
   * Prueba la conectividad con el servidor Jira de un squad.
   * Solo funciona con método API_REST.
   */
  probarConexion: async (squadId: number): Promise<boolean> => {
    const { data } = await api.post<{ ok: boolean }>(
      `/jira/config/${squadId}/test`,
    );
    return data.ok;
  },
};

// ─────────────────────────────────────────────────────────────────
// Jira Sync — POST /sync/{squadId} / GET /sync/{squadId}/status ...
// ─────────────────────────────────────────────────────────────────

export const jiraSyncService = {
  /**
   * Dispara una sincronización completa (issues + worklogs) para el squad.
   */
  syncCompleta: async (squadId: number): Promise<JiraSyncStatusResponse> => {
    const { data } = await api.post<JiraSyncStatusResponse>(
      `/jira/sync/${squadId}`,
    );
    return data;
  },

  /**
   * Sincroniza solo las issues del squad (sin worklogs).
   */
  syncIssues: async (squadId: number): Promise<JiraSyncStatusResponse> => {
    const { data } = await api.post<JiraSyncStatusResponse>(
      `/jira/sync/${squadId}/issues`,
    );
    return data;
  },

  /**
   * Importa solo worklogs de issues ya cacheadas.
   */
  syncWorklogs: async (squadId: number): Promise<JiraSyncStatusResponse> => {
    const { data } = await api.post<JiraSyncStatusResponse>(
      `/jira/sync/${squadId}/worklogs`,
    );
    return data;
  },

  /**
   * Devuelve el estado actual de sincronización del squad.
   */
  obtenerEstado: async (squadId: number): Promise<JiraSyncStatusResponse> => {
    const { data } = await api.get<JiraSyncStatusResponse>(
      `/jira/sync/${squadId}/status`,
    );
    return data;
  },

  /**
   * Devuelve todas las operaciones encoladas (global, todos los squads).
   */
  obtenerCola: async (): Promise<JiraSyncQueueResponse[]> => {
    const { data } = await api.get<JiraSyncQueueResponse[]>("/jira/sync/queue");
    return data;
  },

  /**
   * Fuerza el reintento de una operación en estado ERROR.
   */
  reintentarOperacion: async (id: number): Promise<JiraSyncQueueResponse> => {
    const { data } = await api.post<JiraSyncQueueResponse>(
      `/jira/sync/queue/${id}/retry`,
    );
    return data;
  },
};

// ─────────────────────────────────────────────────────────────────
// Jira Worklogs — /jira/worklogs
// ─────────────────────────────────────────────────────────────────

export const jiraWorklogService = {
  /**
   * Vista "Mi Día": imputaciones de una persona en una fecha concreta.
   * @param fecha formato yyyy-MM-dd (si se omite, usa hoy)
   */
  getMiDia: async (
    personaId: number,
    fecha?: string,
  ): Promise<WorklogDiaResponse> => {
    const params = new URLSearchParams({ personaId: personaId.toString() });
    if (fecha) params.append("fecha", fecha);
    const { data } = await api.get<WorklogDiaResponse>(
      `/jira/worklogs/mia?${params.toString()}`,
    );
    return data;
  },

  /**
   * Vista "Mi Semana": cuadrícula 5d × tareas.
   * @param semanaInicio lunes en formato yyyy-MM-dd (si se omite, usa semana actual)
   */
  getMiSemana: async (
    personaId: number,
    semanaInicio?: string,
  ): Promise<WorklogSemanaResponse> => {
    const params = new URLSearchParams({ personaId: personaId.toString() });
    if (semanaInicio) params.append("semanaInicio", semanaInicio);
    const { data } = await api.get<WorklogSemanaResponse>(
      `/jira/worklogs/semana?${params.toString()}`,
    );
    return data;
  },

  /**
   * Registra una nueva imputación de horas en KAOS.
   */
  registrar: async (request: WorklogRequest): Promise<JiraWorklogResponse> => {
    const { data } = await api.post<JiraWorklogResponse>(
      "/jira/worklogs",
      request,
    );
    return data;
  },

  /**
   * Edita una imputación no sincronizada.
   */
  editar: async (
    id: number,
    request: WorklogRequest,
  ): Promise<JiraWorklogResponse> => {
    const { data } = await api.put<JiraWorklogResponse>(
      `/jira/worklogs/${id}`,
      request,
    );
    return data;
  },

  /**
   * Elimina una imputación no sincronizada.
   */
  eliminar: async (id: number): Promise<void> => {
    await api.delete(`/jira/worklogs/${id}`);
  },

  /**
   * Todas las imputaciones de una issue Jira.
   */
  getByIssue: async (jiraKey: string): Promise<JiraWorklogResponse[]> => {
    const { data } = await api.get<JiraWorklogResponse[]>(
      `/jira/worklogs/issue/${jiraKey}`,
    );
    return data;
  },
};

// ─────────────────────────────────────────────────────────────────
// Jira Alertas — /jira/alertas
// ─────────────────────────────────────────────────────────────────

export const jiraAlertService = {
  /**
   * Lista alertas de un sprint (filtrable por resuelta y severidad).
   */
  listarAlertas: async (
    sprintId: number,
    page = 0,
    size = 50,
    resuelta?: boolean,
  ): Promise<PageResponse<AlertaResponse>> => {
    const params = new URLSearchParams({
      sprintId: sprintId.toString(),
      page: page.toString(),
      size: size.toString(),
    });
    if (resuelta !== undefined) params.append("resuelta", resuelta.toString());
    const { data } = await api.get<PageResponse<AlertaResponse>>(
      `/jira/alertas?${params.toString()}`,
    );
    return data;
  },

  /**
   * Marca una alerta como resuelta.
   */
  resolverAlerta: async (id: number): Promise<AlertaResponse> => {
    const { data } = await api.patch<AlertaResponse>(
      `/jira/alertas/${id}/resolver`,
    );
    return data;
  },

  /**
   * Lista todas las reglas de alerta.
   */
  listarReglas: async (
    page = 0,
    size = 50,
  ): Promise<PageResponse<AlertRuleResponse>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const { data } = await api.get<PageResponse<AlertRuleResponse>>(
      `/jira/alertas/reglas?${params.toString()}`,
    );
    return data;
  },

  /**
   * Crea una nueva regla de alerta.
   */
  crearRegla: async (request: AlertRuleRequest): Promise<AlertRuleResponse> => {
    const { data } = await api.post<AlertRuleResponse>(
      "/jira/alertas/reglas",
      request,
    );
    return data;
  },

  /**
   * Actualiza una regla de alerta existente.
   */
  actualizarRegla: async (
    id: number,
    request: AlertRuleRequest,
  ): Promise<AlertRuleResponse> => {
    const { data } = await api.put<AlertRuleResponse>(
      `/jira/alertas/reglas/${id}`,
      request,
    );
    return data;
  },

  /**
   * Elimina una regla de alerta.
   */
  eliminarRegla: async (id: number): Promise<void> => {
    await api.delete(`/jira/alertas/reglas/${id}`);
  },

  /**
   * Trigger manual del motor SpEL para un sprint.
   */
  evaluarSprint: async (sprintId: number): Promise<AlertaResponse[]> => {
    const { data } = await api.post<AlertaResponse[]>(
      `/jira/alertas/evaluar/${sprintId}`,
    );
    return data;
  },
};
