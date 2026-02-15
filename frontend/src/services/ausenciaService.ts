/**
 * Servicio API para Ausencias
 * Gestión de ausencias (bajas médicas, emergencias, etc.)
 */

import { api } from "./api";
import type {
  AusenciaResponse,
  AusenciaRequest,
  PageResponse,
} from "@/types/api";

export interface AusenciaFilters {
  personaId?: number;
  squadId?: number;
  tipo?: string;
  fechaInicio?: string;
  fechaFin?: string;
}

export const ausenciaService = {
  /**
   * Lista ausencias con paginación y filtros
   */
  listar: async (page = 0, size = 20, filters?: AusenciaFilters) => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters?.personaId)
      params.append("personaId", filters.personaId.toString());
    if (filters?.squadId) params.append("squadId", filters.squadId.toString());
    if (filters?.tipo) params.append("tipo", filters.tipo);
    if (filters?.fechaInicio) params.append("fechaInicio", filters.fechaInicio);
    if (filters?.fechaFin) params.append("fechaFin", filters.fechaFin);

    const { data } = await api.get<PageResponse<AusenciaResponse>>(
      `/ausencias?${params.toString()}`,
    );
    return data;
  },

  /**
   * Obtiene una ausencia por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<AusenciaResponse>(`/ausencias/${id}`);
    return data;
  },

  /**
   * Crea una nueva ausencia
   */
  crear: async (request: AusenciaRequest) => {
    const { data } = await api.post<AusenciaResponse>("/ausencias", request);
    return data;
  },

  /**
   * Actualiza una ausencia existente
   */
  actualizar: async (id: number, request: AusenciaRequest) => {
    const { data } = await api.put<AusenciaResponse>(
      `/ausencias/${id}`,
      request,
    );
    return data;
  },

  /**
   * Elimina una ausencia
   */
  eliminar: async (id: number) => {
    await api.delete(`/ausencias/${id}`);
  },

  /**
   * Obtiene ausencias de un squad en un rango de fechas
   */
  porSquad: async (squadId: number, fechaInicio: string, fechaFin: string) => {
    const { data } = await api.get<PageResponse<AusenciaResponse>>(
      `/ausencias?squadId=${squadId}&fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`,
    );
    return data.content;
  },
};
