/**
 * Servicio API para asignaciones de tareas padre en el timeline.
 * Permite vincular issues Jira padre (HISTORIA) a personas con rango de días.
 */

import { api } from "./api";
import type {
  TareaAsignacionTimelineRequest,
  TareaAsignacionTimelineResponse,
} from "@/types/api";

export const tareaAsignacionTimelineService = {
  /**
   * Lista asignaciones de tareas padre para un sprint
   */
  listarPorSprint: async (sprintId: number) => {
    const { data } = await api.get<TareaAsignacionTimelineResponse[]>(
      "/timeline-asignaciones",
      { params: { sprintId } },
    );
    return data;
  },

  /**
   * Obtiene una asignación por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<TareaAsignacionTimelineResponse>(
      `/timeline-asignaciones/${id}`,
    );
    return data;
  },

  /**
   * Crea una nueva asignación de tarea padre en el timeline
   */
  crear: async (request: TareaAsignacionTimelineRequest) => {
    const { data } = await api.post<TareaAsignacionTimelineResponse>(
      "/timeline-asignaciones",
      request,
    );
    return data;
  },

  /**
   * Actualiza una asignación existente
   */
  actualizar: async (id: number, request: TareaAsignacionTimelineRequest) => {
    const { data } = await api.put<TareaAsignacionTimelineResponse>(
      `/timeline-asignaciones/${id}`,
      request,
    );
    return data;
  },

  /**
   * Elimina una asignación
   */
  eliminar: async (id: number) => {
    await api.delete(`/timeline-asignaciones/${id}`);
  },
};
