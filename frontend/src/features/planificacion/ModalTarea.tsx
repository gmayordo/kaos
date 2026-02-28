/**
 * ModalTarea — Modal de crear / editar tarea
 * Presentacional: recibe datos, emite submit/cancel.
 */

import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { toast } from "@/lib/toast";
import { tareaDependenciaService } from "@/services/tareaDependenciaService";
import { tareaService } from "@/services/tareaService";
import type {
  CategoriaTarea,
  CrearDependenciaRequest,
  EstadoTarea,
  PersonaResponse,
  PrioridadTarea,
  TareaRequest,
  TareaResponse,
  TipoDependencia,
  TipoTarea,
} from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ExternalLink, GitMerge, Plus, Trash2, X } from "lucide-react";
import type { FC } from "react";
import { useCallback, useEffect, useRef, useState } from "react";

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
  const queryClient = useQueryClient();
  const [form, setForm] = useState<FormState>(
    tarea
      ? tareaToForm(tarea)
      : emptyForm(sprintId, diaPreseleccionado, personaPreseleccionadaId),
  );
  const [errors, setErrors] = useState<
    Partial<Record<keyof FormState, string>>
  >({});
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [addingDep, setAddingDep] = useState(false);
  const [newDepTargetId, setNewDepTargetId] = useState("");
  const [newDepTipo, setNewDepTipo] = useState<TipoDependencia>("ESTRICTA");
  const modalRef = useRef<HTMLDivElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  // ── Dependencias (solo en edición) ─────────────────────────────
  const { data: dependencias = [], refetch: refetchDeps } = useQuery({
    queryKey: ["tarea-dependencias", tarea?.id],
    queryFn: () => tareaDependenciaService.listar(tarea!.id),
    enabled: !!tarea?.id,
  });

  // Tareas del sprint para el selector de dependencia
  const { data: tareasSprintPage } = useQuery({
    queryKey: ["tareas-sprint-selector", sprintId],
    queryFn: () => tareaService.listar(0, 200, { sprintId }),
    enabled: addingDep && !!sprintId,
  });
  const tareasParaSelector = (tareasSprintPage?.content ?? []).filter(
    (t) => t.id !== tarea?.id,
  );

  const crearDepMutation = useMutation({
    mutationFn: (req: CrearDependenciaRequest) =>
      tareaDependenciaService.crear(tarea!.id, req),
    onSuccess: () => {
      toast.success("Dependencia añadida");
      setAddingDep(false);
      setNewDepTargetId("");
      refetchDeps();
      queryClient.invalidateQueries({ queryKey: ["tarea-dependencias"] });
    },
    onError: (err: any) =>
      toast.error(err?.response?.data?.message ?? "Error al crear dependencia"),
  });

  const eliminarDepMutation = useMutation({
    mutationFn: (depId: number) =>
      tareaDependenciaService.eliminar(tarea!.id, depId),
    onSuccess: () => {
      toast.success("Dependencia eliminada");
      refetchDeps();
    },
    onError: () => toast.error("Error al eliminar dependencia"),
  });

  // Sincronizar cuando cambia la tarea (ej: abrir con distinta tarea)
  useEffect(() => {
    setForm(
      tarea
        ? tareaToForm(tarea)
        : emptyForm(sprintId, diaPreseleccionado, personaPreseleccionadaId),
    );
    setErrors({});
  }, [tarea, sprintId, diaPreseleccionado, personaPreseleccionadaId]);

  // Focus trap: Tab/Shift+Tab cycles within modal
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onCancel();
        return;
      }
      if (e.key !== "Tab") return;
      const focusable = modalRef.current?.querySelectorAll<HTMLElement>(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
      );
      if (!focusable || focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (e.shiftKey) {
        if (document.activeElement === first) {
          e.preventDefault();
          last.focus();
        }
      } else {
        if (document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    },
    [onCancel],
  );

  // Body scroll lock + focus management
  useEffect(() => {
    previousFocusRef.current = document.activeElement as HTMLElement;
    document.body.style.overflow = "hidden";
    requestAnimationFrame(() => {
      const firstInput = modalRef.current?.querySelector<HTMLElement>("input");
      firstInput?.focus();
    });
    return () => {
      document.body.style.overflow = "";
      previousFocusRef.current?.focus();
    };
  }, []);

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
      onKeyDown={handleKeyDown}
    >
      <div
        ref={modalRef}
        className="w-full max-w-lg rounded-xl bg-card shadow-xl border border-border"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border px-5 py-4">
          <h2
            id="modal-tarea-titulo"
            className="text-base font-semibold text-foreground"
          >
            {tarea ? "Editar tarea" : "Nueva tarea"}
          </h2>
          <button
            type="button"
            onClick={onCancel}
            aria-label="Cerrar modal"
            className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} noValidate>
          <div className="max-h-[65vh] space-y-4 overflow-y-auto px-5 py-4">
            {/* Título */}
            <div>
              <label
                htmlFor="tarea-titulo"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Título <span className="text-destructive">*</span>
              </label>
              <input
                id="tarea-titulo"
                type="text"
                value={form.titulo}
                onChange={(e) => set("titulo", e.target.value)}
                placeholder="Título de la tarea"
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                aria-required="true"
                aria-invalid={!!errors.titulo}
                aria-describedby={
                  errors.titulo ? "tarea-titulo-error" : undefined
                }
              />
              {errors.titulo && (
                <p
                  id="tarea-titulo-error"
                  className="mt-1 text-xs text-destructive"
                  role="alert"
                >
                  {errors.titulo}
                </p>
              )}
            </div>

            {/* Tipo y Categoría */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label
                  htmlFor="tarea-tipo"
                  className="mb-1 block text-sm font-medium text-foreground"
                >
                  Tipo
                </label>
                <select
                  id="tarea-tipo"
                  value={form.tipo}
                  onChange={(e) => set("tipo", e.target.value)}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
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
                  className="mb-1 block text-sm font-medium text-foreground"
                >
                  Categoría
                </label>
                <select
                  id="tarea-categoria"
                  value={form.categoria}
                  onChange={(e) => set("categoria", e.target.value)}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
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
                  className="mb-1 block text-sm font-medium text-foreground"
                >
                  Estimación (h) <span className="text-destructive">*</span>
                </label>
                <input
                  id="tarea-estimacion"
                  type="number"
                  min="0.5"
                  step="0.5"
                  value={form.estimacion}
                  onChange={(e) => set("estimacion", e.target.value)}
                  placeholder="0"
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                  aria-required="true"
                  aria-invalid={!!errors.estimacion}
                  aria-describedby={
                    errors.estimacion ? "tarea-estimacion-error" : undefined
                  }
                />
                {errors.estimacion && (
                  <p
                    id="tarea-estimacion-error"
                    className="mt-1 text-xs text-destructive"
                    role="alert"
                  >
                    {errors.estimacion}
                  </p>
                )}
              </div>
              <div>
                <label
                  htmlFor="tarea-prioridad"
                  className="mb-1 block text-sm font-medium text-foreground"
                >
                  Prioridad
                </label>
                <select
                  id="tarea-prioridad"
                  value={form.prioridad}
                  onChange={(e) => set("prioridad", e.target.value)}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
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
                  className="mb-1 block text-sm font-medium text-foreground"
                >
                  Persona asignada
                </label>
                <select
                  id="tarea-persona"
                  value={form.personaId}
                  onChange={(e) => set("personaId", e.target.value)}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
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
                  className="mb-1 block text-sm font-medium text-foreground"
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
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                  aria-invalid={!!errors.diaAsignado}
                  aria-describedby={
                    errors.diaAsignado ? "tarea-dia-error" : undefined
                  }
                />
                {errors.diaAsignado && (
                  <p
                    id="tarea-dia-error"
                    className="mt-1 text-xs text-destructive"
                    role="alert"
                  >
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
                  className="mb-1 block text-sm font-medium text-foreground"
                >
                  Estado
                </label>
                <select
                  id="tarea-estado"
                  value={form.estado}
                  onChange={(e) => set("estado", e.target.value)}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
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
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Referencia Jira
              </label>
              <input
                id="tarea-jira"
                type="text"
                value={form.referenciaJira}
                onChange={(e) => set("referenciaJira", e.target.value)}
                placeholder="KAOS-001"
                className="w-full rounded-lg border border-border bg-background px-3 py-2 font-mono text-sm shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>

            {/* ─── Sección Jira (Bloque 5): info de issue vinculado ─── */}
            {tarea?.jiraIssueKey && (
              <div className="rounded-lg border border-blue-400/30 bg-blue-500/5 p-3 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-blue-700 flex items-center gap-1">
                    <GitMerge className="h-3.5 w-3.5" aria-hidden />
                    Issue Jira
                  </span>
                  <a
                    href={`#jira-${tarea.jiraIssueKey}`}
                    className="text-xs text-blue-600 hover:underline flex items-center gap-0.5"
                    aria-label={`Abrir issue ${tarea.jiraIssueKey} en Jira`}
                  >
                    {tarea.jiraIssueKey}
                    <ExternalLink className="h-3 w-3" aria-hidden />
                  </a>
                </div>
                {tarea.jiraIssueSummary && (
                  <p className="text-xs text-foreground">
                    {tarea.jiraIssueSummary}
                  </p>
                )}
                {/* Barra de progreso: estimado KAOS → horas consumidas Jira */}
                {tarea.jiraEstimacionHoras != null && (
                  <div>
                    <div className="flex justify-between text-xs text-muted-foreground mb-1">
                      <span>Progreso Jira</span>
                      <span>
                        {tarea.jiraHorasConsumidas ?? 0}h / {tarea.estimacion}h
                        estimadas
                      </span>
                    </div>
                    {(() => {
                      const pct = Math.min(
                        100,
                        Math.round(
                          ((tarea.jiraHorasConsumidas ?? 0) /
                            tarea.estimacion) *
                            100,
                        ),
                      );
                      const colorBar =
                        pct >= 90
                          ? "bg-destructive"
                          : pct >= 70
                            ? "bg-amber-500"
                            : "bg-green-500";
                      return (
                        <div
                          className="h-2 bg-muted rounded-full overflow-hidden"
                          role="progressbar"
                          aria-valuenow={pct}
                          aria-valuemin={0}
                          aria-valuemax={100}
                          aria-label={`Progreso: ${pct}%`}
                        >
                          <div
                            className={`h-2 rounded-full transition-all ${colorBar}`}
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                      );
                    })()}
                  </div>
                )}
                {/* Tarea padre */}
                {tarea.tareaParentId && (
                  <p className="text-xs text-muted-foreground">
                    Subtarea de KAOS #{tarea.tareaParentId}
                  </p>
                )}
              </div>
            )}

            {/* ─── Sección Dependencias (Bloque 5) ─── */}
            {tarea && (
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-foreground flex items-center gap-1">
                    <GitMerge
                      className="h-4 w-4 text-muted-foreground"
                      aria-hidden
                    />
                    Dependencias
                    {dependencias.length > 0 && (
                      <span className="ml-1 text-xs bg-muted text-muted-foreground rounded-full px-1.5 py-0.5">
                        {dependencias.length}
                      </span>
                    )}
                  </span>
                  {!addingDep && (
                    <button
                      type="button"
                      onClick={() => setAddingDep(true)}
                      className="flex items-center gap-1 text-xs text-primary hover:underline"
                    >
                      <Plus className="h-3.5 w-3.5" />
                      Añadir
                    </button>
                  )}
                </div>

                {/* Lista de dependencias */}
                {dependencias.length > 0 && (
                  <ul className="space-y-1.5">
                    {dependencias.map((dep) => (
                      <li
                        key={dep.id}
                        className="flex items-center justify-between rounded border border-border bg-muted/30 px-2.5 py-1.5 text-xs"
                      >
                        <div>
                          <span className="font-medium text-foreground">
                            KAOS #{dep.tareaDestinoId}
                          </span>
                          <span className="ml-1 text-muted-foreground truncate">
                            — {dep.tareaDestinoTitulo}
                          </span>
                        </div>
                        <div className="flex items-center gap-2 shrink-0">
                          <span
                            className={`text-xs px-1.5 py-0.5 rounded border ${
                              dep.tipo === "ESTRICTA"
                                ? "bg-red-500/10 text-red-600 border-red-400/30"
                                : "bg-amber-500/10 text-amber-600 border-amber-400/30"
                            }`}
                          >
                            {dep.tipo}
                          </span>
                          <button
                            type="button"
                            onClick={() => eliminarDepMutation.mutate(dep.id)}
                            disabled={eliminarDepMutation.isPending}
                            className="text-muted-foreground hover:text-destructive transition-colors disabled:opacity-50"
                            aria-label={`Eliminar dependencia con ${dep.tareaDestinoTitulo}`}
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}

                {/* Formulario añadir dependencia */}
                {addingDep && (
                  <div className="flex items-end gap-2 p-2.5 rounded border border-border bg-muted/20">
                    <div className="flex-1">
                      <label className="block text-xs text-muted-foreground mb-1">
                        Tarea destino
                      </label>
                      <select
                        value={newDepTargetId}
                        onChange={(e) => setNewDepTargetId(e.target.value)}
                        className="w-full px-2 py-1.5 rounded border border-border bg-background text-foreground text-xs focus:outline-none focus:ring-1 focus:ring-primary"
                        autoFocus
                      >
                        <option value="">Seleccionar...</option>
                        {tareasParaSelector.map((t) => (
                          <option key={t.id} value={t.id}>
                            #{t.id} — {t.titulo}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs text-muted-foreground mb-1">
                        Tipo
                      </label>
                      <select
                        value={newDepTipo}
                        onChange={(e) =>
                          setNewDepTipo(e.target.value as TipoDependencia)
                        }
                        className="px-2 py-1.5 rounded border border-border bg-background text-foreground text-xs focus:outline-none focus:ring-1 focus:ring-primary"
                      >
                        <option value="ESTRICTA">Estricta</option>
                        <option value="SUAVE">Suave</option>
                      </select>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        if (!newDepTargetId) return;
                        crearDepMutation.mutate({
                          tareaDestinoId: Number(newDepTargetId),
                          tipo: newDepTipo,
                        });
                      }}
                      disabled={!newDepTargetId || crearDepMutation.isPending}
                      className="px-3 py-1.5 bg-primary text-primary-foreground rounded text-xs font-medium hover:bg-primary/90 transition-colors disabled:opacity-50"
                    >
                      {crearDepMutation.isPending ? "..." : "Añadir"}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setAddingDep(false);
                        setNewDepTargetId("");
                      }}
                      className="px-2 py-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors"
                    >
                      Cancelar
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between gap-2 border-t border-border px-5 py-3">
            <div>
              {tarea && onDelete && (
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(true)}
                  disabled={isSubmitting}
                  className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/20 disabled:opacity-50 transition-colors"
                >
                  Eliminar
                </button>
              )}
            </div>
            <button
              type="button"
              onClick={onCancel}
              disabled={isSubmitting}
              className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted disabled:opacity-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50 transition-colors"
            >
              {isSubmitting
                ? "Guardando..."
                : tarea
                  ? "Guardar cambios"
                  : "Crear tarea"}
            </button>
          </div>
        </form>

        {/* Confirm delete dialog */}
        {tarea && onDelete && (
          <ConfirmDialog
            isOpen={showDeleteConfirm}
            onConfirm={() => {
              setShowDeleteConfirm(false);
              onDelete(tarea.id);
            }}
            onCancel={() => setShowDeleteConfirm(false)}
            title="Eliminar tarea"
            description={`¿Eliminar la tarea "${tarea.titulo}"? Esta acción no se puede deshacer.`}
            variant="danger"
            confirmText="Eliminar"
          />
        )}
      </div>
    </div>
  );
};
