/**
 * Mi Día — Vista diaria de imputaciones de horas Jira
 * Lista de worklogs del día + capacidad + indicador jornada completa
 */

import { JiraTabs } from "@/components/jira/JiraTabs";
import { ConfirmDialog } from "@/components/ui";
import { toast } from "@/lib/toast";
import { useDocumentTitle } from "@/lib/useDocumentTitle";
import { jiraWorklogService } from "@/services/jiraService";
import { personaService } from "@/services/personaService";
import type { WorklogRequest } from "@/types/jira";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import {
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  PlusCircle,
  Trash2,
  XCircle,
} from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/jira/mi-dia")({
  component: MiDiaPage,
});

// ─────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("es-ES", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

function todayIso(): string {
  return new Date().toISOString().split("T")[0];
}

function addDays(iso: string, days: number): string {
  const d = new Date(iso);
  d.setDate(d.getDate() + days);
  return d.toISOString().split("T")[0];
}

// ─────────────────────────────────────────────────────────────────
// WorklogForm — formulario inline para registrar imputación
// ─────────────────────────────────────────────────────────────────

interface WorklogFormProps {
  personaId: number;
  fecha: string;
  onSuccess: () => void;
  onCancel: () => void;
}

function WorklogForm({
  personaId,
  fecha,
  onSuccess,
  onCancel,
}: WorklogFormProps) {
  const [jiraKey, setJiraKey] = useState("");
  const [horas, setHoras] = useState("1");
  const [comentario, setComentario] = useState("");

  const mutación = useMutation({
    mutationFn: (req: WorklogRequest) => jiraWorklogService.registrar(req),
    onSuccess: () => {
      onSuccess();
      toast.success("Imputación registrada");
    },
    onError: () => toast.error("Error al registrar la imputación"),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!jiraKey.trim() || !horas) return;
    mutación.mutate({
      jiraKey: jiraKey.trim().toUpperCase(),
      personaId,
      fecha,
      horas: parseFloat(horas),
      comentario: comentario || undefined,
    });
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="border border-primary/30 bg-primary/5 rounded-lg p-4 space-y-3"
    >
      <h4 className="text-sm font-semibold text-foreground">
        Nueva imputación
      </h4>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label
            htmlFor="wl-jira-key"
            className="block text-xs text-muted-foreground mb-1"
          >
            Clave Jira <span className="text-destructive">*</span>
          </label>
          <input
            id="wl-jira-key"
            value={jiraKey}
            onChange={(e) => setJiraKey(e.target.value)}
            placeholder="PROJ-123"
            className="w-full px-2 py-1.5 rounded border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            required
            autoFocus
          />
        </div>
        <div>
          <label
            htmlFor="wl-horas"
            className="block text-xs text-muted-foreground mb-1"
          >
            Horas <span className="text-destructive">*</span>
          </label>
          <input
            id="wl-horas"
            type="number"
            value={horas}
            onChange={(e) => setHoras(e.target.value)}
            min="0.25"
            step="0.25"
            className="w-full px-2 py-1.5 rounded border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            required
          />
        </div>
      </div>
      <div>
        <label
          htmlFor="wl-comentario"
          className="block text-xs text-muted-foreground mb-1"
        >
          Comentario
        </label>
        <input
          id="wl-comentario"
          value={comentario}
          onChange={(e) => setComentario(e.target.value)}
          placeholder="Descripción de la tarea..."
          className="w-full px-2 py-1.5 rounded border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
        />
      </div>
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={mutación.isPending}
          className="px-3 py-1.5 bg-primary text-primary-foreground rounded text-xs font-medium hover:bg-primary/90 transition-colors disabled:opacity-50"
        >
          {mutación.isPending ? "Guardando..." : "Guardar"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="px-3 py-1.5 bg-secondary text-secondary-foreground rounded text-xs font-medium hover:bg-secondary/80 transition-colors"
        >
          Cancelar
        </button>
        {mutación.isError && (
          <span className="text-xs text-destructive self-center">
            Error al guardar
          </span>
        )}
      </div>
    </form>
  );
}

// ─────────────────────────────────────────────────────────────────
// MiDiaPage
// ─────────────────────────────────────────────────────────────────

function MiDiaPage() {
  const queryClient = useQueryClient();
  useDocumentTitle("Mi Día");
  const [personaId, setPersonaId] = useState<number | null>(null);
  const [fecha, setFecha] = useState<string>(todayIso());
  const [showForm, setShowForm] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);

  const { data: personasPage, isLoading: loadingPersonas } = useQuery({
    queryKey: ["personas-list"],
    queryFn: () => personaService.listar(0, 200),
  });

  const personas = personasPage?.content ?? [];

  const {
    data: miDia,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["worklog-mi-dia", personaId, fecha],
    queryFn: () => jiraWorklogService.getMiDia(personaId!, fecha),
    enabled: personaId !== null,
  });

  const eliminarMutation = useMutation({
    mutationFn: (id: number) => jiraWorklogService.eliminar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["worklog-mi-dia", personaId, fecha],
      });
      toast.success("Imputación eliminada");
    },
    onError: () => toast.error("Error al eliminar la imputación"),
  });

  const handleWorklogSaved = () => {
    setShowForm(false);
    queryClient.invalidateQueries({
      queryKey: ["worklog-mi-dia", personaId, fecha],
    });
  };

  const capacidadPct = miDia
    ? Math.min(
        Math.round((miDia.horasImputadas / miDia.horasCapacidad) * 100),
        100,
      )
    : 0;

  return (
    <div className="space-y-4 max-w-3xl">
      <JiraTabs active="mi-dia" />

      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Mi Día</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Imputaciones de horas de la jornada.
          </p>
        </div>
      </div>

      {/* Controles: persona + fecha */}
      <div className="flex items-center gap-3 flex-wrap">
        <div className="flex items-center gap-2">
          <label
            htmlFor="midia-persona"
            className="text-sm font-medium sr-only"
          >
            Persona
          </label>
          <select
            id="midia-persona"
            value={personaId ?? ""}
            onChange={(e) =>
              setPersonaId(e.target.value ? Number(e.target.value) : null)
            }
            className="px-3 py-2 rounded-md border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary min-w-[200px]"
            disabled={loadingPersonas}
          >
            <option value="">
              {loadingPersonas ? "Cargando..." : "Seleccionar persona"}
            </option>
            {personas.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nombre}
              </option>
            ))}
          </select>
        </div>

        {/* Navegación de fecha */}
        <div className="flex items-center gap-1">
          <button
            onClick={() => setFecha(addDays(fecha, -1))}
            className="p-1.5 rounded hover:bg-accent transition-colors text-muted-foreground hover:text-foreground"
            aria-label="Día anterior"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <input
            type="date"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            className="px-2 py-1.5 rounded-md border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          />
          <button
            onClick={() => setFecha(addDays(fecha, 1))}
            className="p-1.5 rounded hover:bg-accent transition-colors text-muted-foreground hover:text-foreground"
            aria-label="Día siguiente"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
          <button
            onClick={() => setFecha(todayIso())}
            className="px-2 py-1.5 text-xs text-muted-foreground hover:text-foreground hover:bg-accent rounded transition-colors"
          >
            Hoy
          </button>
        </div>
      </div>

      {/* Contenido */}
      {!personaId ? (
        <div className="border border-dashed border-border rounded-lg p-10 text-center text-muted-foreground">
          <Clock className="h-10 w-10 mx-auto mb-3 opacity-40" />
          <p className="text-sm">
            Selecciona una persona para ver sus imputaciones.
          </p>
        </div>
      ) : isLoading ? (
        <div className="space-y-2">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-14 rounded-lg bg-muted animate-pulse" />
          ))}
        </div>
      ) : error || !miDia ? (
        <div className="border border-border rounded-lg p-4 text-sm text-muted-foreground">
          Error al cargar los datos del día.
        </div>
      ) : (
        <div className="space-y-4">
          {/* Fecha + indicador jornada */}
          <div className="flex items-center justify-between">
            <p className="text-sm font-medium text-foreground capitalize">
              {formatDate(miDia.fecha)}
            </p>
            <span
              className={`flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full ${
                miDia.jornadaCompleta
                  ? "bg-green-500/10 text-green-600 border border-green-500/30"
                  : "bg-amber-500/10 text-amber-600 border border-amber-500/30"
              }`}
            >
              {miDia.jornadaCompleta ? (
                <>
                  <CheckCircle2 className="h-3 w-3" /> Jornada completa
                </>
              ) : (
                <>
                  <XCircle className="h-3 w-3" /> Sin imputar completo
                </>
              )}
            </span>
          </div>

          {/* Barra de capacidad */}
          <div>
            <div className="flex justify-between text-xs text-muted-foreground mb-1">
              <span>
                {miDia.horasImputadas}h imputadas / {miDia.horasCapacidad}h
                capacidad
              </span>
              <span>{capacidadPct}%</span>
            </div>
            <div className="h-2 bg-muted rounded-full overflow-hidden">
              <div
                className={`h-2 rounded-full transition-all ${
                  capacidadPct >= 100
                    ? "bg-green-500"
                    : capacidadPct >= 50
                      ? "bg-amber-500"
                      : "bg-muted-foreground/40"
                }`}
                style={{ width: `${capacidadPct}%` }}
              />
            </div>
          </div>

          {/* Lista de worklogs */}
          {miDia.worklogs.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">
              Sin imputaciones registradas para este día.
            </p>
          ) : (
            <div className="space-y-2">
              {miDia.worklogs.map((wl) => (
                <div
                  key={wl.worklogId}
                  className="flex items-center gap-3 border border-border rounded-lg px-4 py-3 bg-card hover:bg-accent/30 transition-colors"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-mono font-semibold text-primary">
                        {wl.jiraKey}
                      </span>
                      {wl.sincronizado ? (
                        <span className="text-xs text-green-600 border border-green-500/30 bg-green-500/10 rounded px-1">
                          sincronizado
                        </span>
                      ) : (
                        <span className="text-xs text-amber-600 border border-amber-500/30 bg-amber-500/10 rounded px-1">
                          pendiente
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-foreground truncate">
                      {wl.issueSummary}
                    </p>
                    {wl.comentario && (
                      <p className="text-xs text-muted-foreground truncate">
                        {wl.comentario}
                      </p>
                    )}
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-sm font-semibold text-foreground">
                      {wl.horas}h
                    </p>
                  </div>
                  {!wl.sincronizado && (
                    <button
                      onClick={() => setDeleteConfirm(wl.worklogId)}
                      className="text-muted-foreground hover:text-destructive transition-colors"
                      aria-label={`Eliminar imputación ${wl.jiraKey}`}
                    >
                      <Trash2 className="h-4 w-4" aria-hidden="true" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}

          {/* Formulario para nueva imputación */}
          {showForm ? (
            <WorklogForm
              personaId={personaId}
              fecha={fecha}
              onSuccess={handleWorklogSaved}
              onCancel={() => setShowForm(false)}
            />
          ) : (
            <button
              onClick={() => setShowForm(true)}
              className="flex items-center gap-2 text-sm text-primary hover:text-primary/80 transition-colors"
            >
              <PlusCircle className="h-4 w-4" />
              Añadir imputación
            </button>
          )}
        </div>
      )}

      {/* ConfirmDialog for delete */}
      <ConfirmDialog
        isOpen={deleteConfirm !== null}
        onCancel={() => setDeleteConfirm(null)}
        onConfirm={() => {
          if (deleteConfirm !== null) {
            eliminarMutation.mutate(deleteConfirm);
            setDeleteConfirm(null);
          }
        }}
        title="Eliminar imputación"
        description="¿Estás seguro de eliminar esta imputación? Esta acción no se puede deshacer."
        confirmText="Eliminar"
        variant="danger"
      />
    </div>
  );
}

// Named export for testing
export { MiDiaPage };
