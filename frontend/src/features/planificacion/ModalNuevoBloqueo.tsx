/**
 * ModalNuevoBloqueo — Modal para crear un impedimento (bloqueo)
 * Presentacional: recibe datos, emite submit/cancel.
 */

import type {
  BloqueoRequest,
  PersonaResponse,
  TareaResponse,
  TipoBloqueo,
} from "@/types/api";
import { X } from "lucide-react";
import type { FC } from "react";
import { useEffect, useState } from "react";

interface Props {
  /** Personas disponibles como responsable */
  personas: PersonaResponse[];
  /** Tareas del sprint para marcar como afectadas (informativo) */
  tareas: TareaResponse[];
  /** Callback al enviar el formulario */
  onSubmit: (data: BloqueoRequest, tareasAfectadasIds: number[]) => void;
  /** Callback al cancelar */
  onCancel: () => void;
  /** Estado de envío */
  isSubmitting?: boolean;
}

const TIPOS: { value: TipoBloqueo; label: string; desc: string }[] = [
  {
    value: "DEPENDENCIA_EXTERNA",
    label: "Dependencia externa",
    desc: "Espera cliente, proveedor",
  },
  {
    value: "RECURSO",
    label: "Recurso",
    desc: "Falta máquina, tiempo, espacio",
  },
  { value: "TECNICO", label: "Técnico", desc: "Error BD, bug framework" },
  {
    value: "COMUNICACION",
    label: "Comunicación",
    desc: "Falta decisión, feedback",
  },
  { value: "OTRO", label: "Otro", desc: "Genérico, no clasificado" },
];

type FormState = {
  titulo: string;
  descripcion: string;
  tipo: TipoBloqueo;
  responsableId: string;
  notas: string;
};

const emptyForm: FormState = {
  titulo: "",
  descripcion: "",
  tipo: "TECNICO",
  responsableId: "",
  notas: "",
};

export const ModalNuevoBloqueo: FC<Props> = ({
  personas,
  tareas,
  onSubmit,
  onCancel,
  isSubmitting = false,
}) => {
  const [form, setForm] = useState<FormState>(emptyForm);
  const [tareasSeleccionadas, setTareasSeleccionadas] = useState<Set<number>>(
    new Set(),
  );
  const [errors, setErrors] = useState<
    Partial<Record<keyof FormState, string>>
  >({});

  // Resetear al abrir
  useEffect(() => {
    setForm(emptyForm);
    setTareasSeleccionadas(new Set());
    setErrors({});
  }, []);

  const set = (field: keyof FormState, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setErrors((prev) => ({ ...prev, [field]: undefined }));
  };

  const toggleTarea = (id: number) => {
    setTareasSeleccionadas((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const validate = (): boolean => {
    const errs: Partial<Record<keyof FormState, string>> = {};
    if (!form.titulo.trim()) errs.titulo = "El título es obligatorio";
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const request: BloqueoRequest = {
      titulo: form.titulo.trim(),
      descripcion: form.descripcion.trim() || undefined,
      tipo: form.tipo,
      estado: "ACTIVO",
      responsableId: form.responsableId
        ? Number(form.responsableId)
        : undefined,
      notas: form.notas.trim() || undefined,
    };

    onSubmit(request, Array.from(tareasSeleccionadas));
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-bloqueo-titulo"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
    >
      <div className="w-full max-w-md rounded-xl bg-white shadow-xl dark:bg-gray-800">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4 dark:border-gray-700">
          <h2
            id="modal-bloqueo-titulo"
            className="text-base font-semibold text-gray-900 dark:text-gray-50"
          >
            Nuevo bloqueo
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
          <div className="max-h-[60vh] space-y-4 overflow-y-auto px-5 py-4">
            {/* Título */}
            <div>
              <label
                htmlFor="bloqueo-titulo"
                className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
              >
                Título <span className="text-red-500">*</span>
              </label>
              <input
                id="bloqueo-titulo"
                type="text"
                value={form.titulo}
                onChange={(e) => set("titulo", e.target.value)}
                placeholder="Describe el impedimento"
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
                aria-required="true"
                aria-invalid={!!errors.titulo}
              />
              {errors.titulo && (
                <p className="mt-1 text-xs text-red-500">{errors.titulo}</p>
              )}
            </div>

            {/* Descripción */}
            <div>
              <label
                htmlFor="bloqueo-desc"
                className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
              >
                Descripción
              </label>
              <textarea
                id="bloqueo-desc"
                rows={2}
                value={form.descripcion}
                onChange={(e) => set("descripcion", e.target.value)}
                placeholder="Detalles del bloqueo..."
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
              />
            </div>

            {/* Tipo */}
            <div>
              <label
                htmlFor="bloqueo-tipo"
                className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
              >
                Tipo
              </label>
              <select
                id="bloqueo-tipo"
                value={form.tipo}
                onChange={(e) => set("tipo", e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
              >
                {TIPOS.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label} — {t.desc}
                  </option>
                ))}
              </select>
            </div>

            {/* Responsable */}
            <div>
              <label
                htmlFor="bloqueo-responsable"
                className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
              >
                Responsable
              </label>
              <select
                id="bloqueo-responsable"
                value={form.responsableId}
                onChange={(e) => set("responsableId", e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
              >
                <option value="">Sin responsable asignado</option>
                {personas.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nombre}
                  </option>
                ))}
              </select>
            </div>

            {/* Tareas afectadas (multiselect) */}
            {tareas.length > 0 && (
              <div>
                <p className="mb-1.5 text-sm font-medium text-gray-700 dark:text-gray-300">
                  Tareas afectadas
                </p>
                <div className="max-h-32 overflow-y-auto rounded-lg border border-gray-200 dark:border-gray-700">
                  {tareas.map((t) => (
                    <label
                      key={t.id}
                      className="flex cursor-pointer items-center gap-2 px-3 py-1.5 text-sm hover:bg-gray-50 dark:hover:bg-gray-700/50"
                    >
                      <input
                        type="checkbox"
                        checked={tareasSeleccionadas.has(t.id)}
                        onChange={() => toggleTarea(t.id)}
                        className="rounded border-gray-300 text-blue-500 focus:ring-blue-500"
                        aria-label={`Marcar tarea ${t.titulo} como afectada`}
                      />
                      <span className="truncate text-gray-700 dark:text-gray-300">
                        {t.titulo}
                      </span>
                      {t.referenciaJira && (
                        <span className="ml-auto shrink-0 text-xs font-mono text-gray-400">
                          {t.referenciaJira}
                        </span>
                      )}
                    </label>
                  ))}
                </div>
                {tareasSeleccionadas.size > 0 && (
                  <p className="mt-1 text-xs text-gray-400">
                    {tareasSeleccionadas.size} tarea(s) seleccionada(s)
                  </p>
                )}
              </div>
            )}

            {/* Notas */}
            <div>
              <label
                htmlFor="bloqueo-notas"
                className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
              >
                Notas adicionales
              </label>
              <textarea
                id="bloqueo-notas"
                rows={2}
                value={form.notas}
                onChange={(e) => set("notas", e.target.value)}
                placeholder="Pasos de gestión, solución propuesta..."
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100"
              />
            </div>
          </div>

          {/* Footer */}
          <div className="flex justify-end gap-2 border-t border-gray-200 px-5 py-3 dark:border-gray-700">
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
              className="rounded-lg bg-red-500 px-4 py-2 text-sm font-medium text-white hover:bg-red-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isSubmitting ? "Registrando..." : "Registrar bloqueo"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
