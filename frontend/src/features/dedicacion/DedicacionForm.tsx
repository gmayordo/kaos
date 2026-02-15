/**
 * DedicacionForm — Formulario de dedicación persona-squad
 * Componente presentacional para crear/editar asignaciones con % semanal
 */

import type {
  PersonaResponse,
  SquadMemberRequest,
  SquadResponse,
} from "@/types/api";
import type { FC } from "react";
import { useEffect, useState } from "react";

interface Props {
  /** Asignación existente (para editar) o null (para crear) */
  squadMember?: SquadMemberRequest | null;
  /** Lista de squads disponibles */
  squads: SquadResponse[];
  /** Lista de personas disponibles */
  personas: PersonaResponse[];
  /** Capacidad restante disponible de la persona (en %) */
  capacidadDisponible?: number;
  /** Callback al enviar el formulario */
  onSubmit: (data: SquadMemberRequest) => void;
  /** Callback al cancelar */
  onCancel: () => void;
  /** Estado de carga */
  isSubmitting?: boolean;
}

/**
 * Formulario para gestionar dedicación de persona a squad.
 * Valida que el total de % no exceda 100% por persona.
 */
export const DedicacionForm: FC<Props> = ({
  squadMember,
  squads,
  personas,
  capacidadDisponible = 100,
  onSubmit,
  onCancel,
  isSubmitting = false,
}) => {
  const [formData, setFormData] = useState<SquadMemberRequest>({
    personaId: squadMember?.personaId || 0,
    squadId: squadMember?.squadId || 0,
    porcentaje: squadMember?.porcentaje || 0,
    rol: squadMember?.rol || "FRONTEND",
    fechaInicio:
      squadMember?.fechaInicio || new Date().toISOString().split("T")[0],
  });

  // Resetear form cuando cambia squadMember
  useEffect(() => {
    if (squadMember) {
      setFormData(squadMember);
    }
  }, [squadMember]);

  const handleChange = (field: keyof SquadMemberRequest, value: any) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmitForm = (e: React.FormEvent) => {
    e.preventDefault();

    // Validación: % debe ser entre 1 y 100
    if (formData.porcentaje < 1 || formData.porcentaje > 100) {
      alert("El porcentaje de dedicación debe estar entre 1% y 100%");
      return;
    }

    // Validación: no exceder capacidad disponible
    if (formData.porcentaje > capacidadDisponible) {
      alert(
        `Esta persona solo tiene ${capacidadDisponible}% de capacidad disponible. Reduce el porcentaje.`,
      );
      return;
    }

    onSubmit(formData);
  };

  const capacidadRestante = capacidadDisponible - formData.porcentaje;
  const isOverCapacity = capacidadRestante < 0;

  return (
    <form onSubmit={handleSubmitForm} className="space-y-4">
      {/* Squad */}
      <div>
        <label className="block text-sm font-medium mb-1">Squad *</label>
        <select
          value={formData.squadId}
          onChange={(e) => handleChange("squadId", Number(e.target.value))}
          className="w-full px-3 py-2 border rounded-md"
          required
          disabled={!!squadMember}
        >
          <option value={0}>Selecciona un squad</option>
          {squads.map((squad) => (
            <option key={squad.id} value={squad.id}>
              {squad.nombre} ({squad.estado})
            </option>
          ))}
        </select>
        {squadMember && (
          <p className="text-xs text-muted-foreground mt-1">
            No puedes cambiar el squad al editar
          </p>
        )}
      </div>

      {/* Persona */}
      <div>
        <label className="block text-sm font-medium mb-1">Persona *</label>
        <select
          value={formData.personaId}
          onChange={(e) => handleChange("personaId", Number(e.target.value))}
          className="w-full px-3 py-2 border rounded-md"
          required
          disabled={!!squadMember}
        >
          <option value={0}>Selecciona una persona</option>
          {personas
            .filter((p) => p.activo)
            .map((persona) => (
              <option key={persona.id} value={persona.id}>
                {persona.nombre} ({persona.seniority})
              </option>
            ))}
        </select>
        {squadMember && (
          <p className="text-xs text-muted-foreground mt-1">
            No puedes cambiar la persona al editar
          </p>
        )}
      </div>

      {/* Rol en Squad */}
      <div>
        <label className="block text-sm font-medium mb-1">Rol en Squad *</label>
        <select
          value={formData.rol}
          onChange={(e) => handleChange("rol", e.target.value)}
          className="w-full px-3 py-2 border rounded-md"
          required
        >
          <option value="LIDER_TECNICO">Líder Técnico</option>
          <option value="LIDER_FUNCIONAL">Líder Funcional</option>
          <option value="FRONTEND">Frontend</option>
          <option value="BACKEND">Backend</option>
          <option value="QA">QA</option>
          <option value="SCRUM_MASTER">Scrum Master</option>
        </select>
      </div>

      {/* Porcentaje de Dedicación */}
      <div>
        <label className="block text-sm font-medium mb-1">
          Porcentaje de Dedicación *
        </label>
        <div className="flex items-center gap-3">
          <input
            type="range"
            min="0"
            max="100"
            step="5"
            value={formData.porcentaje}
            onChange={(e) => handleChange("porcentaje", Number(e.target.value))}
            className="flex-1"
          />
          <input
            type="number"
            min="0"
            max="100"
            value={formData.porcentaje}
            onChange={(e) => handleChange("porcentaje", Number(e.target.value))}
            className="w-20 px-3 py-2 border rounded-md text-center"
            required
          />
          <span className="text-sm font-medium">%</span>
        </div>
      </div>

      {/* Capacidad Disponible */}
      <div
        className={`p-3 rounded-md border ${
          isOverCapacity
            ? "bg-destructive/10 border-destructive"
            : "bg-muted/50 border-muted"
        }`}
      >
        <div className="text-sm font-medium">Capacidad Disponible</div>
        <div className="flex items-baseline gap-2 mt-1">
          <span
            className={`text-2xl font-bold ${isOverCapacity ? "text-destructive" : ""}`}
          >
            {capacidadRestante}%
          </span>
          <span className="text-sm text-muted-foreground">
            (de {capacidadDisponible}% inicial)
          </span>
        </div>
        {isOverCapacity && (
          <p className="text-xs text-destructive mt-1">
            ⚠️ Esta persona no tiene suficiente capacidad disponible
          </p>
        )}
      </div>

      {/* Botones */}
      <div className="flex justify-end gap-3 pt-4">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 border rounded-md hover:bg-muted transition-colors"
          disabled={isSubmitting}
        >
          Cancelar
        </button>
        <button
          type="submit"
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors disabled:opacity-50"
          disabled={isSubmitting || isOverCapacity}
        >
          {isSubmitting ? "Guardando..." : "Guardar"}
        </button>
      </div>
    </form>
  );
};
