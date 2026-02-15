/**
 * FestivoForm — Formulario de festivo
 * Componente para crear/editar festivos con asignación a personas
 */

import type { FestivoRequest, PersonaResponse, TipoFestivo } from "@/types/api";
import type { FC } from "react";
import { useEffect, useState } from "react";

interface Props {
  /** Festivo existente (para editar) o null (para crear) */
  festivo?: (FestivoRequest & { id?: number }) | null;
  /** Lista de personas disponibles */
  personas: PersonaResponse[];
  /** Callback al enviar el formulario */
  onSubmit: (data: FestivoRequest) => void;
  /** Callback al cancelar */
  onCancel: () => void;
  /** Estado de carga */
  isSubmitting?: boolean;
}

const tiposFestivo: { value: TipoFestivo; label: string; emoji: string }[] = [
  { value: "NACIONAL", label: "Nacional", emoji: "🇪🇸" },
  { value: "REGIONAL", label: "Regional", emoji: "📍" },
  { value: "LOCAL", label: "Local", emoji: "🏘️" },
];

/**
 * Formulario para gestionar festivos.
 * Permite seleccionar múltiples personas afectadas.
 */
export const FestivoForm: FC<Props> = ({
  festivo,
  personas,
  onSubmit,
  onCancel,
  isSubmitting = false,
}) => {
  const [formData, setFormData] = useState<FestivoRequest>({
    fecha: festivo?.fecha || new Date().toISOString().split("T")[0],
    descripcion: festivo?.descripcion || "",
    tipo: festivo?.tipo || "NACIONAL",
    personaIds: festivo?.personaIds || [],
  });

  const [searchTerm, setSearchTerm] = useState("");

  // Resetear form cuando cambia festivo
  useEffect(() => {
    if (festivo) {
      setFormData(festivo);
    }
  }, [festivo]);

  const handleChange = (field: keyof FestivoRequest, value: any) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const togglePersona = (personaId: number) => {
    setFormData((prev) => ({
      ...prev,
      personaIds: prev.personaIds.includes(personaId)
        ? prev.personaIds.filter((id) => id !== personaId)
        : [...prev.personaIds, personaId],
    }));
  };

  const handleSubmitForm = (e: React.FormEvent) => {
    e.preventDefault();

    // Validaciones
    if (!formData.fecha) {
      alert("La fecha es obligatoria");
      return;
    }
    if (!formData.descripcion.trim()) {
      alert("La descripción es obligatoria");
      return;
    }
    if (formData.personaIds.length === 0) {
      alert("Debes seleccionar al menos una persona");
      return;
    }

    onSubmit(formData);
  };

  const personasSeleccionadas = personas.filter((p) =>
    formData.personaIds.includes(p.id),
  );

  const personasFiltradas = personas.filter(
    (p) =>
      !formData.personaIds.includes(p.id) &&
      (p.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.email.toLowerCase().includes(searchTerm.toLowerCase())),
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-xl font-semibold">
            {festivo?.id ? "Editar festivo" : "Nuevo festivo"}
          </h2>
          <button
            onClick={onCancel}
            className="text-zinc-400 hover:text-zinc-600"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmitForm} className="space-y-4">
          {/* Fecha */}
          <div>
            <label className="mb-1 block text-sm font-medium text-zinc-700">
              Fecha *
            </label>
            <input
              type="date"
              value={formData.fecha}
              onChange={(e) => handleChange("fecha", e.target.value)}
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            />
          </div>

          {/* Descripción */}
          <div>
            <label className="mb-1 block text-sm font-medium text-zinc-700">
              Descripción *
            </label>
            <input
              type="text"
              value={formData.descripcion}
              onChange={(e) => handleChange("descripcion", e.target.value)}
              placeholder="Día de la Hispanidad"
              maxLength={200}
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            />
          </div>

          {/* Tipo */}
          <div>
            <label className="mb-1 block text-sm font-medium text-zinc-700">
              Tipo *
            </label>
            <select
              value={formData.tipo}
              onChange={(e) =>
                handleChange("tipo", e.target.value as TipoFestivo)
              }
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            >
              {tiposFestivo.map((tipo) => (
                <option key={tipo.value} value={tipo.value}>
                  {tipo.emoji} {tipo.label}
                </option>
              ))}
            </select>
          </div>

          {/* Personas afectadas */}
          <div>
            <label className="mb-1 block text-sm font-medium text-zinc-700">
              Personas afectadas *
            </label>

            {/* Personas seleccionadas (chips) */}
            {personasSeleccionadas.length > 0 && (
              <div className="mb-2 flex flex-wrap gap-2">
                {personasSeleccionadas.map((persona) => (
                  <span
                    key={persona.id}
                    className="inline-flex items-center gap-1 rounded-md bg-blue-100 px-2 py-1 text-sm text-blue-700"
                  >
                    {persona.nombre}
                    <button
                      type="button"
                      onClick={() => togglePersona(persona.id)}
                      className="ml-1 text-blue-500 hover:text-blue-700"
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            )}

            {/* Búsqueda y selección */}
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Buscar persona..."
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />

            {/* Lista de personas */}
            {searchTerm && personasFiltradas.length > 0 && (
              <div className="mt-2 max-h-40 overflow-y-auto rounded-md border border-zinc-300">
                {personasFiltradas.map((persona) => (
                  <button
                    key={persona.id}
                    type="button"
                    onClick={() => {
                      togglePersona(persona.id);
                      setSearchTerm("");
                    }}
                    className="w-full px-3 py-2 text-left hover:bg-zinc-50"
                  >
                    <div className="font-medium">{persona.nombre}</div>
                    <div className="text-xs text-zinc-500">{persona.email}</div>
                  </button>
                ))}
              </div>
            )}

            <div className="mt-1 text-sm text-zinc-600">
              {formData.personaIds.length} personas seleccionadas
            </div>
          </div>

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
