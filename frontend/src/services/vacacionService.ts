/**
 * Servicio API para Vacaciones
 * Gestión de vacaciones y permisos
 */

import { api } from "./api";
import type {
  VacacionResponse,
  VacacionRequest,
  ExcelAnalysisResponse,
  ExcelImportResponse,
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
   * Obtiene vacaciones de un squad en un rango de fechas.
   * Usa el endpoint dedicado /squads/{squadId}/vacaciones que devuelve List<T> directo.
   */
  porSquad: async (squadId: number, fechaInicio: string, fechaFin: string) => {
    const { data } = await api.get<VacacionResponse[]>(
      `/squads/${squadId}/vacaciones?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`,
    );
    return data;
  },

  /**
   * Analiza un fichero Excel sin importar nada (dry-run).
   * Devuelve qué nombres se resolvieron y cuáles necesitan mapeo manual.
   */
  analizarExcel: async (file: File, año?: number) => {
    const formData = new FormData();
    formData.append("file", file);
    const params = año ? `?año=${año}` : "";
    const { data } = await api.post<ExcelAnalysisResponse>(
      `/vacaciones/analizar-excel${params}`,
      formData,
      { headers: { "Content-Type": "multipart/form-data" } },
    );
    return data;
  },

  /**
   * Importa vacaciones y ausencias desde un fichero Excel (.xlsx).
   *
   * @param file     Fichero Excel seleccionado por el usuario
   * @param año      Año fiscal (por defecto el año en curso)
   * @param mappings Mapeos manuales { nombreExcel → personaId } para nombres no auto-resueltos
   */
  importarExcel: async (
    file: File,
    año?: number,
    mappings?: Record<string, number>,
  ) => {
    const formData = new FormData();
    formData.append("file", file);
    const params = new URLSearchParams();
    if (año) params.set("año", String(año));
    if (mappings && Object.keys(mappings).length > 0) {
      params.set("mappingsJson", JSON.stringify(mappings));
    }
    const qs = params.toString() ? `?${params.toString()}` : "";
    const { data } = await api.post<ExcelImportResponse>(
      `/vacaciones/importar-excel${qs}`,
      formData,
      { headers: { "Content-Type": "multipart/form-data" } },
    );
    return data;
  },
};
