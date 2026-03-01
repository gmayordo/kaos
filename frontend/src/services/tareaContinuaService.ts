/**
 * Servicio API para tareas continuas multi-sprint (tipo Gantt).
 * Gestiona tareas de larga duración como seguimiento, formación, reuniones recurrentes, etc.
 */

import { api } from "./api";
import type {
  TareaContinuaRequest,
  TareaContinuaResponse,
} from "@/types/api";

export const tareaContinuaService = {
  /**
   * Lista tareas continuas activas de un squad
   */
  listarPorSquad: async (squadId: number) => {
    const { data } = await api.get<TareaContinuaResponse[]>(
      "/tareas-continuas",
      { params: { squadId } },
    );
    return data;
  },

  /**
   * Obtiene una tarea continua por ID
   */
  obtener: async (id: number) => {
    const { data } = await api.get<TareaContinuaResponse>(
      `/tareas-continuas/${id}`,
    );
    return data;
  },

  /**
   * Crea una nueva tarea continua
   */
  crear: async (request: TareaContinuaRequest) => {
    const { data } = await api.post<TareaContinuaResponse>(
      "/tareas-continuas",
      request,
    );
    return data;
  },

  /**
   * Actualiza una tarea continua existente
   */
  actualizar: async (id: number, request: TareaContinuaRequest) => {
    const { data } = await api.put<TareaContinuaResponse>(
      `/tareas-continuas/${id}`,
      request,
    );
    return data;
  },

  /**
   * Elimina (soft delete) una tarea continua
   */
  eliminar: async (id: number) => {
    await api.delete(`/tareas-continuas/${id}`);
  },
};
