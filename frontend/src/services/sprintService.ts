/**
 * Servicio API para Sprints
 * Gestión de sprints de planificación
 */

import { api } from "./api";
import type { SprintRequest, SprintResponse, PageResponse } from "@/types/api";

export interface SprintFilters {
  squadId?: number;
  estado?: string;
}

export const sprintService = {
  /**
   * Lista sprints con paginación y filtros
   */
  listar: async (page = 0, size = 20, filters?: SprintFilters) => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters?.squadId) params.append("squadId", filters.squadId.toString());
    if (filters?.estado) params.append("estado", filters.estado);

    const { data } = await api.get<PageResponse<SprintResponse>>(
      `/sprints?${params.toString()}`,
    );
    return data;
  },

  /**
   * Obtiene un sprint por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<SprintResponse>(`/sprints/${id}`);
    return data;
  },

  /**
   * Crea un nuevo sprint
   */
  crear: async (request: SprintRequest) => {
    const { data } = await api.post<SprintResponse>("/sprints", request);
    return data;
  },

  /**
   * Actualiza un sprint existente
   */
  actualizar: async (id: number, request: SprintRequest) => {
    const { data } = await api.put<SprintResponse>(`/sprints/${id}`, request);
    return data;
  },

  /**
   * Cambia el estado de un sprint (PLANNING → ACTIVO → CERRADO)
   */
  cambiarEstado: async (id: number, estado: string) => {
    const { data } = await api.patch<SprintResponse[]>(
      `/sprints/${id}/estado?estado=${estado}`,
    );
    return data;
  },

  /**
   * Elimina un sprint
   */
  eliminar: async (id: number) => {
    await api.delete(`/sprints/${id}`);
  },
};
