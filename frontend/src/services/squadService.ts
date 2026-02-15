/**
 * Servicio API para Squads
 * Operaciones CRUD para gestión de equipos de desarrollo
 */

import { api } from "./api";
import type { SquadResponse, SquadRequest, PageResponse } from "@/types/api";

const toPageResponse = (
  content: SquadResponse[],
  page: number,
  size: number,
): PageResponse<SquadResponse> => {
  const totalElements = content.length;
  const totalPages =
    size > 0 ? Math.max(1, Math.ceil(totalElements / size)) : 1;

  return {
    content,
    pageable: {
      pageNumber: page,
      pageSize: size,
      sort: {
        sorted: false,
        unsorted: true,
        empty: true,
      },
      offset: page * size,
      paged: true,
      unpaged: false,
    },
    totalElements,
    totalPages,
    last: page >= totalPages - 1,
    first: page === 0,
    size,
    number: page,
    sort: {
      sorted: false,
      unsorted: true,
      empty: true,
    },
    numberOfElements: totalElements,
    empty: totalElements === 0,
  };
};

export const squadService = {
  /**
   * Lista todos los squads con paginación
   * @param page Número de página (0-indexed)
   * @param size Tamaño de página
   * @param estado Filtro por estado (opcional)
   */
  listar: async (page = 0, size = 20, estado?: "ACTIVO" | "INACTIVO") => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    if (estado) {
      params.append("estado", estado);
    }

    const { data } = await api.get<
      PageResponse<SquadResponse> | SquadResponse[]
    >(`/squads?${params.toString()}`);
    if (Array.isArray(data)) {
      return toPageResponse(data, page, size);
    }
    return data;
  },

  /**
   * Obtiene un squad por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<SquadResponse>(`/squads/${id}`);
    return data;
  },

  /**
   * Crea un nuevo squad
   */
  crear: async (squad: SquadRequest) => {
    const { data } = await api.post<SquadResponse>("/squads", squad);
    return data;
  },

  /**
   * Actualiza un squad existente
   */
  actualizar: async (id: number, squad: SquadRequest) => {
    const { data } = await api.put<SquadResponse>(`/squads/${id}`, squad);
    return data;
  },

  /**
   * Desactiva un squad (soft delete)
   */
  desactivar: async (id: number) => {
    await api.post(`/squads/${id}/desactivar`);
  },
};
