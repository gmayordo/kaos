/**
 * Servicio API para Planificación
 * Dashboard y Timeline de sprints
 */

import { api } from "./api";
import type {
  DashboardSprintResponse,
  TimelineSprintResponse,
} from "@/types/api";

export const planificacionService = {
  /**
   * Obtiene el dashboard de un sprint (métricas, alertas, ocupación)
   */
  obtenerDashboard: async (sprintId: number) => {
    const { data } = await api.get<DashboardSprintResponse>(
      `/planificacion/${sprintId}/dashboard`,
    );
    return data;
  },

  /**
   * Obtiene la timeline (grid personas × días) de un sprint
   */
  obtenerTimeline: async (sprintId: number) => {
    const { data } = await api.get<TimelineSprintResponse>(
      `/planificacion/${sprintId}/timeline`,
    );
    return data;
  },
};
