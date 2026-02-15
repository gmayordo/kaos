/**
 * VacacionForm — Formulario de vacación
 * Componente para crear/editar vacaciones con cálculo de duración
 */

import type {
  EstadoVacacion,
  PersonaResponse,
  TipoVacacion,
  VacacionRequest,
} from "@/types/api";
import type { FC } from "react";
import { useEffect, useState } from "react";

interface Props {
  /** Vacación existente (para editar) o null (para crear) */
  vacacion?: (VacacionRequest & { id?: number }) | null;
  /** Lista de personas disponibles */
  personas: PersonaResponse[];
  /**Callback al enviar el formulario */
  onSubmit: (data: VacacionRequest) => void;
  /** Callback al cancelar */
  onCancel: () => void;
  /** Estado de carga */
  isSubmitting?: boolean;
  /** Filtrar personas por squad (opcional) */
  squadId?: number;
}

const tiposVacacion: { value: TipoVacacion; label: string }[] = [
  { value: "VACACIONES", label: "Vacaciones" },
  { value: "ASUNTOS_PROPIOS", label: "Asuntos propios" },
  { value: "LIBRE_DISPOSICION", label: "Libre disposición" },
  { value: "PERMISO", label: "Permiso" },
];

const estadosVacacion: { value: EstadoVacacion; label: string }[] = [
  { value: "SOLICITADA", label: "Solicitada" },
  { value: "REGISTRADA", label: "Registrada" },
];

/**
 * Formulario para gestionar vacaciones.
 * Calcula duración en días laborables automáticamente.
 */
export const VacacionForm: FC<Props> = ({
  vacacion,
  personas,
  onSubmit,
  onCancel,
  isSubmitting = false,
  squadId,
}) => {
  const [formData, setFormData] = useState<VacacionRequest>({
    personaId: vacacion?.personaId || 0,
    fechaInicio:
      vacacion?.fechaInicio || new Date().toISOString().split("T")[0],
    fechaFin: vacacion?.fechaFin || new Date().toISOString().split("T")[0],
    tipo: vacacion?.tipo || "VACACIONES",
    estado: vacacion?.estado || "REGISTRADA",
    comentario: vacacion?.comentario || "",
  });

  // Resetear form cuando cambia vacacion
  useEffect(() => {
    if (vacacion) {
      setFormData(vacacion);
    }
  }, [vacacion]);

  const handleChange = (field: keyof VacacionRequest, value: any) => {
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
    if (!formData.fechaFin) {
      alert("La fecha de fin es obligatoria");
      return;
    }
    if (formData.fechaFin < formData.fechaInicio) {
      alert("La fecha de fin debe ser posterior o igual a la fecha de inicio");
      return;
    }

    onSubmit(formData);
  };

  // Cálculo simple de días (sin considerar festivos ni fines de semana por ahora)
  const calcularDias = () => {
    if (!formData.fechaInicio || !formData.fechaFin) return 0;
    const inicio = new Date(formData.fechaInicio);
    const fin = new Date(formData.fechaFin);
    const diff = Math.ceil(
      (fin.getTime() - inicio.getTime()) / (1000 * 60 * 60 * 24),
    );
    return diff + 1;
  };

  const personasFiltradas = personas.filter((p) => p.activo);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-xl font-semibold">
            {vacacion?.id ? "Editar vacación" : "Registrar vacación"}
          </h2>
          <button
            onClick={onCancel}
            className="text-zinc-400 hover:text-zinc-600"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmitForm} className="space-y-4">
          {/* Persona */}
          <div>
            <label className="mb-1 block text-sm font-medium text-zinc-700">
              Persona *
            </label>
            <select
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
            <label className="mb-1 block text-sm font-medium text-zinc-700">
              Fecha inicio *
            </label>
            <input
              type="date"
              value={formData.fechaInicio}
              onChange={(e) => handleChange("fechaInicio", e.target.value)}
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            />
          </div>

          {/* Fecha fin */}
          <div>
            <label className="mb-1 block text-sm font-medium text-zinc-700">
              Fecha fin *
            </label>
            <input
              type="date"
              value={formData.fechaFin}
              onChange={(e) => handleChange("fechaFin", e.target.value)}
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
                handleChange("tipo", e.target.value as TipoVacacion)
              }
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              required
            >
              {tiposVacacion.map((tipo) => (
                <option key={tipo.value} value={tipo.value}>
                  {tipo.label}
                </option>
              ))}
            </select>
          </div>

          {/* Estado */}
          <div>
            <label className="mb-1 block text-sm font-medium text-zinc-700">
              Estado
            </label>
            <select
              value={formData.estado}
              onChange={(e) =>
                handleChange("estado", e.target.value as EstadoVacacion)
              }
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              {estadosVacacion.map((estado) => (
                <option key={estado.value} value={estado.value}>
                  {estado.label}
                </option>
              ))}
            </select>
          </div>

          {/* Comentario */}
          <div>
            <label className="mb-1 block text-sm font-medium text-zinc-700">
              Comentario
            </label>
            <textarea
              value={formData.comentario}
              onChange={(e) => handleChange("comentario", e.target.value)}
              placeholder="Opcional..."
              maxLength={500}
              rows={3}
              className="w-full rounded-md border border-zinc-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          {/* Duración calculada */}
          <div className="rounded-md bg-blue-50 px-3 py-2 text-sm text-blue-700">
            Duración:{" "}
            <span className="font-semibold">{calcularDias()} días</span>
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
