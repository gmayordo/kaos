/**
 * Servicio API para Personas
 * Operaciones CRUD para gestión de personas/integrantes
 */

import { api } from "./api";
import type {
  PersonaResponse,
  PersonaRequest,
  PageResponse,
} from "@/types/api";

export interface PersonaFilters {
  squadId?: number;
  rol?: string;
  seniority?: string;
  ubicacion?: string;
  activo?: boolean;
}

export const personaService = {
  /**
   * Lista todas las personas con paginación y filtros
   */
  listar: async (page = 0, size = 20, filters?: PersonaFilters) => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters?.squadId) params.append("squadId", filters.squadId.toString());
    if (filters?.rol) params.append("rol", filters.rol);
    if (filters?.seniority) params.append("seniority", filters.seniority);
    if (filters?.ubicacion) params.append("ubicacion", filters.ubicacion);
    if (filters?.activo !== undefined)
      params.append("activo", filters.activo.toString());

    const { data } = await api.get<PageResponse<PersonaResponse>>(
      `/personas?${params.toString()}`,
    );
    return data;
  },

  /**
   * Obtiene una persona por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<PersonaResponse>(`/personas/${id}`);
    return data;
  },

  /**
   * Crea una nueva persona
   */
  crear: async (persona: PersonaRequest) => {
    const { data } = await api.post<PersonaResponse>("/personas", persona);
    return data;
  },

  /**
   * Actualiza una persona existente
   */
  actualizar: async (id: number, persona: PersonaRequest) => {
    const { data } = await api.put<PersonaResponse>(`/personas/${id}`, persona);
    return data;
  },

  /**
   * Elimina una persona (soft delete por activo=false)
   */
  eliminar: async (id: number) => {
    await api.delete(`/personas/${id}`);
  },
};
