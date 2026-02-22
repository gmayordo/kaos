/**
 * Servicio API para Capacidad
 * Cálculo de capacidad disponible considerando festivos, vacaciones y ausencias
 */

import { api } from "./api";
import type { CapacidadSquadResponse } from "@/types/api";

export const capacidadService = {
  /**
   * Calcula la capacidad de un squad para un rango de fechas
   */
  calcularSquad: async (
    squadId: number,
    fechaInicio: string,
    fechaFin: string,
  ) => {
    const { data } = await api.get<CapacidadSquadResponse>(
      `/capacidad/squad/${squadId}?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`,
    );
    return data;
  },
};
