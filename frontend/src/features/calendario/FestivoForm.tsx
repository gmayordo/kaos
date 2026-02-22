/**
 * FestivoForm — Formulario de festivo
 * Componente para crear/editar festivos por ciudad
 */

import type { FestivoRequest, TipoFestivo } from "@/types/api";
import type { FC } from "react";
import { useEffect, useState } from "react";

interface Props {
  /** Festivo existente (para editar) o null (para crear) */
  festivo?: (FestivoRequest & { id?: number }) | null;
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

const ciudades = [
  { value: "Zaragoza", label: "Zaragoza 🇪🇸", flag: "🇪🇸" },
  { value: "Valencia", label: "Valencia 🇪🇸", flag: "🇪🇸" },
  { value: "Temuco", label: "Temuco 🇨🇱", flag: "🇨🇱" },
];

/**
 * Formulario para gestionar festivos por ciudad.
 */
export const FestivoForm: FC<Props> = ({
  festivo,
  onSubmit,
  onCancel,
  isSubmitting = false,
}) => {
  const [formData, setFormData] = useState<FestivoRequest>({
    fecha: festivo?.fecha || new Date().toISOString().split("T")[0],
    descripcion: festivo?.descripcion || "",
    tipo: festivo?.tipo || "NACIONAL",
    ciudad: festivo?.ciudad || "Zaragoza",
  });

  // Resetear form cuando cambia festivo
  useEffect(() => {
    if (festivo) {
      setFormData(festivo);
    }
  }, [festivo]);

  const handleChange = (field: keyof FestivoRequest, value: any) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
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
    if (!formData.ciudad) {
      alert("La ciudad es obligatoria");
      return;
    }

    onSubmit(formData);
  };

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

        <form onSubmit={handleSubmitForm} role="form" className="space-y-4">
          {/* Fecha */}
          <div>
            <label
              htmlFor="fecha"
              className="mb-1 block text-sm font-medium text-zinc-700"
            >
              Fecha *
            </label>
            <input
              id="fecha"
              type="date"
              value={formData.fecha}
              onChange={(e) => handleChange("fecha", e.target.value)}
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            />
          </div>

          {/* Descripción */}
          <div>
            <label
              htmlFor="descripcion"
              className="mb-1 block text-sm font-medium text-zinc-700"
            >
              Descripción *
            </label>
            <input
              id="descripcion"
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

          {/* Ciudad */}
          <div>
            <label
              htmlFor="ciudad"
              className="mb-1 block text-sm font-medium text-zinc-700"
            >
              Ciudad *
            </label>
            <select
              id="ciudad"
              value={formData.ciudad}
              onChange={(e) => handleChange("ciudad", e.target.value)}
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            >
              {ciudades.map((ciudad) => (
                <option key={ciudad.value} value={ciudad.value}>
                  {ciudad.label}
                </option>
              ))}
            </select>
            <div className="mt-1 text-sm text-zinc-600">
              Calendario laboral de {formData.ciudad}
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
