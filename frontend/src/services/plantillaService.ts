/**
 * Servicio API para Plantillas de Asignación (Bloque 5)
 * CRUD de plantillas y aplicación automática por tipo de issue.
 */

import { api } from "./api";
import type {
  PlanificarAsignacionItem,
  PlantillaAsignacionRequest,
  PlantillaAsignacionResponse,
} from "@/types/api";

export const plantillaService = {
  /**
   * Lista todas las plantillas activas.
   * GET /api/v1/plantillas
   */
  listar: async (): Promise<PlantillaAsignacionResponse[]> => {
    const { data } =
      await api.get<PlantillaAsignacionResponse[]>("/plantillas");
    return data;
  },

  /**
   * Obtiene una plantilla por ID.
   * GET /api/v1/plantillas/:id
   */
  obtener: async (id: number): Promise<PlantillaAsignacionResponse> => {
    const { data } = await api.get<PlantillaAsignacionResponse>(
      `/plantillas/${id}`,
    );
    return data;
  },

  /**
   * Crea una nueva plantilla.
   * POST /api/v1/plantillas
   */
  crear: async (
    request: PlantillaAsignacionRequest,
  ): Promise<PlantillaAsignacionResponse> => {
    const { data } = await api.post<PlantillaAsignacionResponse>(
      "/plantillas",
      request,
    );
    return data;
  },

  /**
   * Actualiza una plantilla existente.
   * PUT /api/v1/plantillas/:id
   */
  actualizar: async (
    id: number,
    request: PlantillaAsignacionRequest,
  ): Promise<PlantillaAsignacionResponse> => {
    const { data } = await api.put<PlantillaAsignacionResponse>(
      `/plantillas/${id}`,
      request,
    );
    return data;
  },

  /**
   * Elimina una plantilla.
   * DELETE /api/v1/plantillas/:id
   */
  eliminar: async (id: number): Promise<void> => {
    await api.delete(`/plantillas/${id}`);
  },

  /**
   * Aplica la plantilla activa para un tipoJira y estimación.
   * GET /api/v1/plantillas/aplicar?tipoJira=Story&estimacion=10
   */
  aplicar: async (
    tipoJira: string,
    estimacion: number,
  ): Promise<PlanificarAsignacionItem[]> => {
    const { data } = await api.get<PlanificarAsignacionItem[]>(
      `/plantillas/aplicar?tipoJira=${encodeURIComponent(tipoJira)}&estimacion=${estimacion}`,
    );
    return data;
  },
};
