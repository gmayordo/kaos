/**
 * Servicio API para Festivos
 * Gestión de días no laborables por ubicación
 */

import { api } from "./api";
import type {
  FestivoResponse,
  FestivoRequest,
  FestivoCsvUploadResponse,
  PageResponse,
} from "@/types/api";

export interface FestivoFilters {
  anio?: number;
  tipo?: string;
}

export const festivoService = {
  /**
   * Lista festivos con paginación y filtros
   */
  listar: async (page = 0, size = 20, filters?: FestivoFilters) => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters?.anio) params.append("anio", filters.anio.toString());
    if (filters?.tipo) params.append("tipo", filters.tipo);

    const { data } = await api.get<PageResponse<FestivoResponse>>(
      `/festivos?${params.toString()}`,
    );
    return data;
  },

  /**
   * Obtiene un festivo por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<FestivoResponse>(`/festivos/${id}`);
    return data;
  },

  /**
   * Crea un nuevo festivo
   */
  crear: async (request: FestivoRequest) => {
    const { data } = await api.post<FestivoResponse>("/festivos", request);
    return data;
  },

  /**
   * Actualiza un festivo existente
   */
  actualizar: async (id: number, request: FestivoRequest) => {
    const { data } = await api.put<FestivoResponse>(`/festivos/${id}`, request);
    return data;
  },

  /**
   * Elimina un festivo
   */
  eliminar: async (id: number) => {
    await api.delete(`/festivos/${id}`);
  },

  /**
   * Carga masiva de festivos desde CSV
   */
  cargarCsv: async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);

    const { data } = await api.post<FestivoCsvUploadResponse>(
      "/festivos/csv",
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      },
    );
    return data;
  },

  /**
   * Obtiene festivos de una persona específica en un rango de fechas
   */
  porPersona: async (
    personaId: number,
    fechaInicio: string,
    fechaFin: string,
  ) => {
    const { data } = await api.get<FestivoResponse[]>(
      `/festivos/persona/${personaId}?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`,
    );
    return data;
  },
};
