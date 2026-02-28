/**
 * Servicio API para Dependencias entre Tareas (Bloque 5)
 * Gestión de dependencias ESTRICTA/SUAVE entre tareas KAOS.
 */

import { api } from "./api";
import type {
  CrearDependenciaRequest,
  TareaDependenciaResponse,
} from "@/types/api";

export const tareaDependenciaService = {
  /**
   * Lista las dependencias de una tarea (como origen).
   * GET /api/v1/tareas/:id/dependencias
   */
  listar: async (tareaId: number): Promise<TareaDependenciaResponse[]> => {
    const { data } = await api.get<TareaDependenciaResponse[]>(
      `/tareas/${tareaId}/dependencias`,
    );
    return data;
  },

  /**
   * Crea una nueva dependencia desde la tarea origen.
   * POST /api/v1/tareas/:id/dependencias
   */
  crear: async (
    tareaId: number,
    request: CrearDependenciaRequest,
  ): Promise<TareaDependenciaResponse> => {
    const { data } = await api.post<TareaDependenciaResponse>(
      `/tareas/${tareaId}/dependencias`,
      request,
    );
    return data;
  },

  /**
   * Elimina una dependencia por ID.
   * DELETE /api/v1/tareas/:id/dependencias/:dependenciaId
   */
  eliminar: async (tareaId: number, dependenciaId: number): Promise<void> => {
    await api.delete(`/tareas/${tareaId}/dependencias/${dependenciaId}`);
  },
};
