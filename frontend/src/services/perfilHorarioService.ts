/**
 * Servicio API para Perfiles Horario
 * Operaciones CRUD para gestión de perfiles de horario laboral
 */

import { api } from "./api";
import type {
  PerfilHorarioResponse,
  PerfilHorarioRequest,
  PageResponse,
} from "@/types/api";

export const perfilHorarioService = {
  /**
   * Lista todos los perfiles horario con paginación
   */
  listar: async (page = 0, size = 20) => {
    const { data } = await api.get<
      PageResponse<PerfilHorarioResponse> | PerfilHorarioResponse[]
    >(`/perfiles-horario?page=${page}&size=${size}`);
    if (Array.isArray(data)) {
      return {
        content: data,
        pageable: {
          pageNumber: page,
          pageSize: size,
          sort: { sorted: false, unsorted: true, empty: true },
          offset: page * size,
          paged: true,
          unpaged: false,
        },
        totalElements: data.length,
        totalPages: 1,
        last: true,
        first: true,
        size,
        number: page,
        sort: { sorted: false, unsorted: true, empty: true },
        numberOfElements: data.length,
        empty: data.length === 0,
      } satisfies PageResponse<PerfilHorarioResponse>;
    }
    return data;
  },

  /**
   * Obtiene un perfil horario por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<PerfilHorarioResponse>(
      `/perfiles-horario/${id}`,
    );
    return data;
  },

  /**
   * Crea un nuevo perfil horario
   */
  crear: async (perfil: PerfilHorarioRequest) => {
    const { data } = await api.post<PerfilHorarioResponse>(
      "/perfiles-horario",
      perfil,
    );
    return data;
  },

  /**
   * Actualiza un perfil horario existente
   */
  actualizar: async (id: number, perfil: PerfilHorarioRequest) => {
    const { data } = await api.put<PerfilHorarioResponse>(
      `/perfiles-horario/${id}`,
      perfil,
    );
    return data;
  },

  /**
   * Elimina un perfil horario
   */
  eliminar: async (id: number) => {
    await api.delete(`/perfiles-horario/${id}`);
  },
};
