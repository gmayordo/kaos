/**
 * Servicio API para Tareas
 * Gestión de tareas dentro de sprints
 */

import { api } from "./api";
import type { TareaRequest, TareaResponse, PageResponse } from "@/types/api";

export interface TareaFilters {
  sprintId?: number;
  personaId?: number;
  estado?: string;
  tipo?: string;
}

export const tareaService = {
  /**
   * Lista tareas con paginación y filtros
   */
  listar: async (page = 0, size = 100, filters?: TareaFilters) => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters?.sprintId)
      params.append("sprintId", filters.sprintId.toString());
    if (filters?.personaId)
      params.append("personaId", filters.personaId.toString());
    if (filters?.estado) params.append("estado", filters.estado);
    if (filters?.tipo) params.append("tipo", filters.tipo);

    const { data } = await api.get<PageResponse<TareaResponse>>(
      `/tareas?${params.toString()}`,
    );
    return data;
  },

  /**
   * Obtiene una tarea por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<TareaResponse>(`/tareas/${id}`);
    return data;
  },

  /**
   * Crea una nueva tarea
   */
  crear: async (request: TareaRequest) => {
    const { data } = await api.post<TareaResponse>("/tareas", request);
    return data;
  },

  /**
   * Actualiza una tarea existente
   */
  actualizar: async (id: number, request: TareaRequest) => {
    const { data } = await api.patch<TareaResponse>(`/tareas/${id}`, request);
    return data;
  },

  /**
   * Cambia el estado de una tarea
   */
  cambiarEstado: async (id: number, estado: string) => {
    const { data } = await api.patch<TareaResponse>(
      `/tareas/${id}/estado?estado=${estado}`,
    );
    return data;
  },

  /**
   * Elimina una tarea
   */
  eliminar: async (id: number) => {
    await api.delete(`/tareas/${id}`);
  },
};
