/**
 * AusenciaForm — Formulario de ausencia
 * Componente para crear/editar ausencias (bajas médicas, emergencias)
 */

import type {
  AusenciaRequest,
  PersonaResponse,
  TipoAusencia,
} from "@/types/api";
import type { FC } from "react";
import { useEffect, useState } from "react";

interface Props {
  /** Ausencia existente (para editar) o null (para crear) */
  ausencia?: (AusenciaRequest & { id?: number }) | null;
  /** Lista de personas disponibles */
  personas: PersonaResponse[];
  /** Callback al enviar el formulario */
  onSubmit: (data: AusenciaRequest) => void;
  /** Callback al cancelar */
  onCancel: () => void;
  /** Estado de carga */
  isSubmitting?: boolean;
  /** ID del squad (opcional, para filtrar personas) */
  squadId?: number;
}

const tiposAusencia: { value: TipoAusencia; label: string }[] = [
  { value: "BAJA_MEDICA", label: "Baja médica" },
  { value: "EMERGENCIA", label: "Emergencia" },
  { value: "OTRO", label: "Otro" },
];

/**
 * Formulario para gestionar ausencias.
 * Permite ausencias indefinidas (sin fecha fin).
 */
export const AusenciaForm: FC<Props> = ({
  ausencia,
  personas,
  onSubmit,
  onCancel,
  isSubmitting = false,
}) => {
  const [formData, setFormData] = useState<AusenciaRequest>({
    personaId: ausencia?.personaId || 0,
    fechaInicio:
      ausencia?.fechaInicio || new Date().toISOString().split("T")[0],
    fechaFin: ausencia?.fechaFin || undefined,
    tipo: ausencia?.tipo || "BAJA_MEDICA",
    comentario: ausencia?.comentario || "",
  });

  // Resetear form cuando cambia ausencia
  useEffect(() => {
    if (ausencia) {
      setFormData(ausencia);
    }
  }, [ausencia]);

  const handleChange = (field: keyof AusenciaRequest, value: any) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmitForm = (e: React.FormEvent) => {
    e.preventDefault();

    // Validaciones
    if (!formData.personaId) {
      alert("Debes seleccionar una persona");
      return;
    }
    if (!formData.fechaInicio) {
      alert("La fecha de inicio es obligatoria");
      return;
    }
    if (formData.fechaFin && formData.fechaFin < formData.fechaInicio) {
      alert("La fecha de fin debe ser posterior o igual a la fecha de inicio");
      return;
    }

    onSubmit(formData);
  };

  const personasFiltradas = personas.filter((p) => p.activo);

  const esIndefinida = !formData.fechaFin;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-xl font-semibold">
            {ausencia?.id ? "Editar ausencia" : "Registrar ausencia"}
          </h2>
          <button
            onClick={onCancel}
            className="text-zinc-400 hover:text-zinc-600"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmitForm} role="form" className="space-y-4">
          {/* Persona */}
          <div>
            <label
              htmlFor="personaId"
              className="mb-1 block text-sm font-medium text-zinc-700"
            >
              Persona *
            </label>
            <select
              id="personaId"
              value={formData.personaId}
              onChange={(e) =>
                handleChange("personaId", Number.parseInt(e.target.value))
              }
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            >
              <option value={0}>Seleccionar persona...</option>
              {personasFiltradas.map((persona) => (
                <option key={persona.id} value={persona.id}>
                  {persona.nombre}
                </option>
              ))}
            </select>
          </div>

          {/* Fecha inicio */}
          <div>
            <label
              htmlFor="fechaInicio"
              className="mb-1 block text-sm font-medium text-zinc-700"
            >
              Fecha inicio *
            </label>
            <input
              id="fechaInicio"
              type="date"
              value={formData.fechaInicio}
              onChange={(e) => handleChange("fechaInicio", e.target.value)}
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            />
          </div>

          {/* Fecha fin */}
          <div>
            <label
              htmlFor="fechaFin"
              className="mb-1 block text-sm font-medium text-zinc-700"
            >
              Fecha fin
            </label>
            <input
              id="fechaFin"
              type="date"
              value={formData.fechaFin || ""}
              onChange={(e) =>
                handleChange("fechaFin", e.target.value || undefined)
              }
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
            <p className="mt-1 text-xs text-zinc-500">
              Dejar vacío si la fecha de fin es desconocida
            </p>
          </div>

          {/* Tipo */}
          <div>
            <label
              htmlFor="tipo"
              className="mb-1 block text-sm font-medium text-zinc-700"
            >
              Tipo *
            </label>
            <select
              id="tipo"
              value={formData.tipo}
              onChange={(e) =>
                handleChange("tipo", e.target.value as TipoAusencia)
              }
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            >
              {tiposAusencia.map((tipo) => (
                <option key={tipo.value} value={tipo.value}>
                  {tipo.label}
                </option>
              ))}
            </select>
          </div>

          {/* Comentario */}
          <div>
            <label
              htmlFor="comentario"
              className="mb-1 block text-sm font-medium text-zinc-700"
            >
              Comentario
            </label>
            <textarea
              id="comentario"
              value={formData.comentario}
              onChange={(e) => handleChange("comentario", e.target.value)}
              placeholder="Opcional..."
              maxLength={500}
              rows={3}
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          {/* Alerta ausencia indefinida */}
          {esIndefinida && (
            <div className="rounded-md bg-amber-50 border border-amber-200 px-3 py-2 text-sm text-amber-700">
              ⚠️ Ausencia indefinida (sin fecha fin)
            </div>
          )}

          {/* Botones */}
          <div className="flex justify-end gap-2 pt-4">
            <button
              type="button"
              onClick={onCancel}
              disabled={isSubmitting}
              className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-50 disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {isSubmitting ? "Guardando..." : "Guardar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
