/**
 * Servicio API para Vacaciones
 * Gestión de vacaciones y permisos
 */

import { api } from "./api";
import type {
  VacacionResponse,
  VacacionRequest,
  PageResponse,
} from "@/types/api";

export interface VacacionFilters {
  personaId?: number;
  squadId?: number;
  tipo?: string;
  estado?: string;
  fechaInicio?: string;
  fechaFin?: string;
}

export const vacacionService = {
  /**
   * Lista vacaciones con paginación y filtros
   */
  listar: async (page = 0, size = 20, filters?: VacacionFilters) => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters?.personaId)
      params.append("personaId", filters.personaId.toString());
    if (filters?.squadId) params.append("squadId", filters.squadId.toString());
    if (filters?.tipo) params.append("tipo", filters.tipo);
    if (filters?.estado) params.append("estado", filters.estado);
    if (filters?.fechaInicio) params.append("fechaInicio", filters.fechaInicio);
    if (filters?.fechaFin) params.append("fechaFin", filters.fechaFin);

    const { data } = await api.get<PageResponse<VacacionResponse>>(
      `/vacaciones?${params.toString()}`,
    );
    return data;
  },

  /**
   * Obtiene una vacación por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<VacacionResponse>(`/vacaciones/${id}`);
    return data;
  },

  /**
   * Crea una nueva vacación
   */
  crear: async (request: VacacionRequest) => {
    const { data } = await api.post<VacacionResponse>("/vacaciones", request);
    return data;
  },

  /**
   * Actualiza una vacación existente
   */
  actualizar: async (id: number, request: VacacionRequest) => {
    const { data } = await api.put<VacacionResponse>(
      `/vacaciones/${id}`,
      request,
    );
    return data;
  },

  /**
   * Elimina una vacación
   */
  eliminar: async (id: number) => {
    await api.delete(`/vacaciones/${id}`);
  },

  /**
   * Obtiene vacaciones de un squad en un rango de fechas
   */
  porSquad: async (squadId: number, fechaInicio: string, fechaFin: string) => {
    const { data } = await api.get<VacacionResponse[]>(
      `/vacaciones/squad/${squadId}?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`,
    );
    return data;
  },
};
