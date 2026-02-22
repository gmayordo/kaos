/**
 * Servicio API para Bloqueos (impedimentos)
 * Gestión de impedimentos que afectan tareas en sprints
 */

import { api } from "./api";
import type {
  BloqueoRequest,
  BloqueoResponse,
  PageResponse,
} from "@/types/api";

export interface BloqueoFilters {
  estado?: string;
}

export const bloqueoService = {
  /**
   * Lista bloqueos con filtro opcional de estado
   */
  listar: async (page = 0, size = 50, filters?: BloqueoFilters) => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters?.estado) params.append("estado", filters.estado);

    const { data } = await api.get<PageResponse<BloqueoResponse>>(
      `/bloqueos?${params.toString()}`,
    );
    return data;
  },

  /**
   * Obtiene un bloqueo por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<BloqueoResponse>(`/bloqueos/${id}`);
    return data;
  },

  /**
   * Crea un nuevo bloqueo
   */
  crear: async (request: BloqueoRequest) => {
    const { data } = await api.post<BloqueoResponse>("/bloqueos", request);
    return data;
  },

  /**
   * Actualiza un bloqueo existente
   */
  actualizar: async (id: number, request: BloqueoRequest) => {
    const { data } = await api.put<BloqueoResponse>(`/bloqueos/${id}`, request);
    return data;
  },

  /**
   * Cambia el estado de un bloqueo
   */
  cambiarEstado: async (id: number, estado: string) => {
    const { data } = await api.patch<BloqueoResponse>(
      `/bloqueos/${id}/estado?estado=${estado}`,
    );
    return data;
  },

  /**
   * Elimina un bloqueo
   */
  eliminar: async (id: number) => {
    await api.delete(`/bloqueos/${id}`);
  },
};
