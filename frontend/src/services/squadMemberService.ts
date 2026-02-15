/**
 * Servicio API para Squad Members (Dedicación)
 * Operaciones CRUD para gestión de asignaciones persona-squad
 */

import { api } from "./api";
import type { SquadMemberResponse, SquadMemberRequest } from "@/types/api";

export const squadMemberService = {
  /**
   * Lista todos los miembros de un squad
   */
  listarPorSquad: async (squadId: number) => {
    const { data } = await api.get<SquadMemberResponse[]>(
      `/squads/${squadId}/miembros`,
    );
    return data;
  },

  /**
   * Lista todos los squads de una persona
   */
  listarPorPersona: async (personaId: number) => {
    const { data } = await api.get<SquadMemberResponse[]>(
      `/personas/${personaId}/squads`,
    );
    return data;
  },

  /**
   * Obtiene un squad member por ID
   * Nota: Backend no tiene endpoint individual, usar listarPorSquad/listarPorPersona
   */
  obtener: async (
    squadId: number,
    miembroId: number,
  ): Promise<SquadMemberResponse | undefined> => {
    const miembros = await squadMemberService.listarPorSquad(squadId);
    return miembros.find((m) => m.id === miembroId);
  },

  /**
   * Crea una nueva asignación persona-squad
   */
  crear: async (squadMember: SquadMemberRequest) => {
    const { data } = await api.post<SquadMemberResponse>(
      "/squad-members",
      squadMember,
    );
    return data;
  },

  /**
   * Actualiza una asignación existente
   */
  actualizar: async (id: number, squadMember: SquadMemberRequest) => {
    const { data } = await api.put<SquadMemberResponse>(
      `/squad-members/${id}`,
      squadMember,
    );
    return data;
  },

  /**
   * Elimina una asignación
   */
  eliminar: async (id: number) => {
    await api.delete(`/squad-members/${id}`);
  },
};
