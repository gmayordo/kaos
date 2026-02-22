/**
 * ModalTarea — Modal de crear / editar tarea
 * Presentacional: recibe datos, emite submit/cancel.
 */

import type {
  CategoriaTarea,
  EstadoTarea,
  PersonaResponse,
  PrioridadTarea,
  TareaRequest,
  TareaResponse,
  TipoTarea,
} from "@/types/api";
import { X } from "lucide-react";
import type { FC } from "react";
import { useEffect, useState } from "react";

interface Props {
  /** Tarea existente para editar, o null para crear */
  tarea?: TareaResponse | null;
  /** Sprint ID al que pertenecerá la tarea */
  sprintId: number;
  /** Personas disponibles para asignar */
  personas: PersonaResponse[];
  /** Día preseleccionado (del click en celda timeline) */
  diaPreseleccionado?: number;
  /** Persona ID preseleccionada */
  personaPreseleccionadaId?: number;
  /** Callback al enviar el formulario */
  onSubmit: (data: TareaRequest) => void;
  /** Callback al cancelar */
  onCancel: () => void;
  /** Callback al eliminar la tarea */
  onDelete?: (tareaId: number) => void;
  /** Estado de envío */
  isSubmitting?: boolean;
}

const TIPOS: { value: TipoTarea; label: string }[] = [
  { value: "HISTORIA", label: "📖 Historia" },
  { value: "TAREA", label: "✓ Tarea" },
  { value: "BUG", label: "🐛 Bug" },
  { value: "SPIKE", label: "⚡ Spike" },
];

const CATEGORIAS: { value: CategoriaTarea; label: string }[] = [
  { value: "CORRECTIVO", label: "🔧 Correctivo" },
  { value: "EVOLUTIVO", label: "✨ Evolutivo" },
];

const PRIORIDADES: { value: PrioridadTarea; label: string }[] = [
  { value: "BAJA", label: "↓ Baja" },
  { value: "NORMAL", label: "➡ Normal" },
  { value: "ALTA", label: "↑ Alta" },
  { value: "BLOQUEANTE", label: "🚫 Bloqueante" },
];

const ESTADOS: { value: EstadoTarea; label: string }[] = [
  { value: "PENDIENTE", label: "◯ Pendiente" },
  { value: "EN_PROGRESO", label: "◆ En progreso" },
  { value: "BLOQUEADO", label: "⊗ Bloqueado" },
  { value: "COMPLETADA", label: "✓ Completada" },
];

type FormState = {
  titulo: string;
  descripcion: string;
  tipo: TipoTarea;
  categoria: CategoriaTarea;
  estimacion: string;
  prioridad: PrioridadTarea;
  personaId: string;
  diaAsignado: string;
  referenciaJira: string;
  estado: EstadoTarea;
};

const emptyForm = (
  _sprintId: number,
  diaPreseleccionado?: number,
  personaPreseleccionadaId?: number,
): FormState => ({
  titulo: "",
  descripcion: "",
  tipo: "TAREA",
  categoria: "EVOLUTIVO",
  estimacion: "",
  prioridad: "NORMAL",
  personaId: personaPreseleccionadaId ? String(personaPreseleccionadaId) : "",
  diaAsignado: diaPreseleccionado ? String(diaPreseleccionado) : "",
  referenciaJira: "",
  estado: "PENDIENTE",
});

function tareaToForm(tarea: TareaResponse): FormState {
  return {
    titulo: tarea.titulo,
    descripcion: "",
    tipo: tarea.tipo as TipoTarea,
    categoria: tarea.categoria as CategoriaTarea,
    estimacion: String(tarea.estimacion),
    prioridad: tarea.prioridad as PrioridadTarea,
    personaId: tarea.personaId ? String(tarea.personaId) : "",
    diaAsignado: tarea.diaAsignado ? String(tarea.diaAsignado) : "",
    referenciaJira: tarea.referenciaJira ?? "",
    estado: tarea.estado as EstadoTarea,
  };
}

export const ModalTarea: FC<Props> = ({
  tarea,
  sprintId,
  personas,
  diaPreseleccionado,
  personaPreseleccionadaId,
  onSubmit,
  onCancel,
  onDelete,
  isSubmitting = false,
}) => {
  const [form, setForm] = useState<FormState>(
    tarea
      ? tareaToForm(tarea)
      : emptyForm(sprintId, diaPreseleccionado, personaPreseleccionadaId),
  );
  const [errors, setErrors] = useState<
    Partial<Record<keyof FormState, string>>
  >({});

  // Sincronizar cuando cambia la tarea (ej: abrir con distinta tarea)
  useEffect(() => {
    setForm(
      tarea
        ? tareaToForm(tarea)
        : emptyForm(sprintId, diaPreseleccionado, personaPreseleccionadaId),
    );
    setErrors({});
  }, [tarea, sprintId, diaPreseleccionado, personaPreseleccionadaId]);

  const set = (field: keyof FormState, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setErrors((prev) => ({ ...prev, [field]: undefined }));
  };

  const validate = (): boolean => {
    const errs: Partial<Record<keyof FormState, string>> = {};
    if (!form.titulo.trim()) errs.titulo = "El título es obligatorio";
    if (
      !form.estimacion ||
      isNaN(Number(form.estimacion)) ||
      Number(form.estimacion) <= 0
    )
      errs.estimacion = "Estimación debe ser mayor a 0";
    if (form.diaAsignado) {
      const d = Number(form.diaAsignado);
      if (isNaN(d) || d < 1 || d > 10)
        errs.diaAsignado = "El día debe estar entre 1 y 10";
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const request: TareaRequest = {
      titulo: form.titulo.trim(),
      sprintId,
      descripcion: form.descripcion.trim() || undefined,
      tipo: form.tipo,
      categoria: form.categoria,
      estimacion: Number(form.estimacion),
      prioridad: form.prioridad,
      personaId: form.personaId ? Number(form.personaId) : undefined,
      diaAsignado: form.diaAsignado ? Number(form.diaAsignado) : undefined,
      referenciaJira: form.referenciaJira.trim() || undefined,
      estado: form.estado,
    };
    onSubmit(request);
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-tarea-titulo"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
    >
      <div className="w-full max-w-lg rounded-xl bg-white shadow-xl dark:bg-gray-800">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4 dark:border-gray-700">
          <h2
            id="modal-tarea-titulo"
            className="text-base font-semibold text-gray-900 dark:text-gray-50"
          >
            {tarea ? "Editar tarea" : "Nueva tarea"}
          </h2>
          <button
            type="button"
            onClick={onCancel}
            aria-label="Cerrar modal"
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700 dark:hover:bg-gray-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} noValidate>
          <div className="max-h-[65vh] space-y-4 overflow-y-auto px-5 py-4">
            {/* Título */}
            <div>
              <label
                htmlFor="tarea-titulo"
                className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
              >
                Título <span className="text-red-500">*</span>
              </label>
              <input
                id="tarea-titulo"
                type="text"
                value={form.titulo}
                onChange={(e) => set("titulo", e.target.value)}
                placeholder="Título de la tarea"
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
                aria-required="true"
                aria-invalid={!!errors.titulo}
              />
              {errors.titulo && (
                <p className="mt-1 text-xs text-red-500">{errors.titulo}</p>
              )}
            </div>

            {/* Tipo y Categoría */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label
                  htmlFor="tarea-tipo"
                  className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
                >
                  Tipo
                </label>
                <select
                  id="tarea-tipo"
                  value={form.tipo}
                  onChange={(e) => set("tipo", e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
                >
                  {TIPOS.map((t) => (
                    <option key={t.value} value={t.value}>
                      {t.label}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label
                  htmlFor="tarea-categoria"
                  className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
                >
                  Categoría
                </label>
                <select
                  id="tarea-categoria"
                  value={form.categoria}
                  onChange={(e) => set("categoria", e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
                >
                  {CATEGORIAS.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Estimación y Prioridad */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label
                  htmlFor="tarea-estimacion"
                  className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
                >
                  Estimación (h) <span className="text-red-500">*</span>
                </label>
                <input
                  id="tarea-estimacion"
                  type="number"
                  min="0.5"
                  step="0.5"
                  value={form.estimacion}
                  onChange={(e) => set("estimacion", e.target.value)}
                  placeholder="0"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
                  aria-required="true"
                  aria-invalid={!!errors.estimacion}
                />
                {errors.estimacion && (
                  <p className="mt-1 text-xs text-red-500">
                    {errors.estimacion}
                  </p>
                )}
              </div>
              <div>
                <label
                  htmlFor="tarea-prioridad"
                  className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
                >
                  Prioridad
                </label>
                <select
                  id="tarea-prioridad"
                  value={form.prioridad}
                  onChange={(e) => set("prioridad", e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
                >
                  {PRIORIDADES.map((p) => (
                    <option key={p.value} value={p.value}>
                      {p.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Persona y Día */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label
                  htmlFor="tarea-persona"
                  className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
                >
                  Persona asignada
                </label>
                <select
                  id="tarea-persona"
                  value={form.personaId}
                  onChange={(e) => set("personaId", e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
                >
                  <option value="">Sin asignar</option>
                  {personas.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.nombre}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label
                  htmlFor="tarea-dia"
                  className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
                >
                  Día asignado (1–10)
                </label>
                <input
                  id="tarea-dia"
                  type="number"
                  min="1"
                  max="10"
                  value={form.diaAsignado}
                  onChange={(e) => set("diaAsignado", e.target.value)}
                  placeholder="—"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
                  aria-invalid={!!errors.diaAsignado}
                />
                {errors.diaAsignado && (
                  <p className="mt-1 text-xs text-red-500">
                    {errors.diaAsignado}
                  </p>
                )}
              </div>
            </div>

            {/* Estado (solo en edición) */}
            {tarea && (
              <div>
                <label
                  htmlFor="tarea-estado"
                  className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
                >
                  Estado
                </label>
                <select
                  id="tarea-estado"
                  value={form.estado}
                  onChange={(e) => set("estado", e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
                >
                  {ESTADOS.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Jira ref */}
            <div>
              <label
                htmlFor="tarea-jira"
                className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
              >
                Referencia Jira
              </label>
              <input
                id="tarea-jira"
                type="text"
                value={form.referenciaJira}
                onChange={(e) => set("referenciaJira", e.target.value)}
                placeholder="KAOS-001"
                className="w-full rounded-lg border border-gray-300 px-3 py-2 font-mono text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
              />
            </div>
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between gap-2 border-t border-gray-200 px-5 py-3 dark:border-gray-700">
            <div>
              {tarea && onDelete && (
                <button
                  type="button"
                  onClick={() => {
                    const confirm = window.confirm(
                      "¿Eliminar esta tarea? Esta accion no se puede deshacer.",
                    );
                    if (confirm) onDelete(tarea.id);
                  }}
                  disabled={isSubmitting}
                  className="rounded-lg border border-red-200 bg-red-50 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100 disabled:opacity-50 dark:border-red-900 dark:bg-red-900/20 dark:text-red-300 dark:hover:bg-red-900/30"
                >
                  Eliminar
                </button>
              )}
            </div>
            <button
              type="button"
              onClick={onCancel}
              disabled={isSubmitting}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-lg bg-blue-500 px-4 py-2 text-sm font-medium text-white hover:bg-blue-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isSubmitting
                ? "Guardando..."
                : tarea
                  ? "Guardar cambios"
                  : "Crear tarea"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
