/**
 * Mi Semana — Cuadrícula semanal de imputaciones Jira
 * Grid 5d × tareas con horas imputadas + totales
 */

import { JiraTabs } from "@/components/jira/JiraTabs";
import { AccessibleModal } from "@/components/ui/AccessibleModal";
import { toast } from "@/lib/toast";
import { useDocumentTitle } from "@/lib/useDocumentTitle";
import { jiraWorklogService } from "@/services/jiraService";
import { personaService } from "@/services/personaService";
import type { WorklogRequest } from "@/types/jira";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { ChevronLeft, ChevronRight, Clock, PlusCircle } from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/jira/mi-semana")({
  component: MiSemanaPage,
});

// ─────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────

function formatHeaderDate(iso: string): string {
  return new Date(iso + "T00:00").toLocaleDateString("es-ES", {
    weekday: "short",
    day: "numeric",
    month: "short",
  });
}

function formatRangeDate(iso: string): string {
  return new Date(iso + "T00:00").toLocaleDateString("es-ES", {
    day: "numeric",
    month: "short",
  });
}

function getMondayOfWeek(isoDate: string): string {
  const d = new Date(isoDate + "T00:00");
  const day = d.getDay(); // 0=sun, 1=mon...
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  d.setDate(diff);
  return d.toISOString().split("T")[0];
}

function addWeeks(iso: string, weeks: number): string {
  const d = new Date(iso + "T00:00");
  d.setDate(d.getDate() + weeks * 7);
  return d.toISOString().split("T")[0];
}

function thisWeekMonday(): string {
  return getMondayOfWeek(new Date().toISOString().split("T")[0]);
}

function getDaysFromMonday(semanaInicio: string): string[] {
  return Array.from({ length: 5 }, (_, i) => {
    const d = new Date(semanaInicio + "T00:00");
    d.setDate(d.getDate() + i);
    return d.toISOString().split("T")[0];
  });
}

// ─────────────────────────────────────────────────────────────────
// Modal para registrar worklog desde celda
// ─────────────────────────────────────────────────────────────────

interface CellModalProps {
  jiraKey: string;
  personaId: number;
  fecha: string;
  onSuccess: () => void;
  onClose: () => void;
}

function CellWorklogModal({
  jiraKey,
  personaId,
  fecha,
  onSuccess,
  onClose,
}: CellModalProps) {
  const [horas, setHoras] = useState("1");
  const [comentario, setComentario] = useState("");

  const mutación = useMutation({
    mutationFn: (req: WorklogRequest) => jiraWorklogService.registrar(req),
    onSuccess: () => {
      toast.success("Worklog registrado correctamente");
      onSuccess();
    },
    onError: () => toast.error("Error al registrar worklog"),
  });

  return (
    <AccessibleModal
      isOpen
      onClose={onClose}
      title={`Imputar horas — ${jiraKey}`}
      size="sm"
    >
      <p className="text-xs text-muted-foreground mb-3">
        Fecha: {formatHeaderDate(fecha)}
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          mutación.mutate({
            jiraKey,
            personaId,
            fecha,
            horas: parseFloat(horas),
            comentario: comentario || undefined,
          });
        }}
        className="space-y-3"
      >
        <div>
          <label
            htmlFor="cell-wl-horas"
            className="block text-xs text-muted-foreground mb-1"
          >
            Horas <span className="text-destructive">*</span>
          </label>
          <input
            id="cell-wl-horas"
            type="number"
            value={horas}
            onChange={(e) => setHoras(e.target.value)}
            min="0.25"
            step="0.25"
            autoFocus
            className="w-full px-2 py-1.5 rounded border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            required
          />
        </div>
        <div>
          <label
            htmlFor="cell-wl-comentario"
            className="block text-xs text-muted-foreground mb-1"
          >
            Comentario
          </label>
          <input
            id="cell-wl-comentario"
            value={comentario}
            onChange={(e) => setComentario(e.target.value)}
            placeholder="Descripción..."
            className="w-full px-2 py-1.5 rounded border border-border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>
        <div className="flex gap-2 pt-1">
          <button
            type="submit"
            disabled={mutación.isPending}
            className="flex-1 py-2 bg-primary text-primary-foreground rounded text-sm font-medium hover:bg-primary/90 transition-colors disabled:opacity-50"
          >
            {mutación.isPending ? "Guardando..." : "Guardar"}
          </button>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 bg-secondary text-secondary-foreground rounded text-sm font-medium hover:bg-secondary/80 transition-colors"
          >
            Cancelar
          </button>
        </div>
        {mutación.isError && (
          <p className="text-xs text-destructive" role="alert">
            Error al guardar
          </p>
        )}
      </form>
    </AccessibleModal>
  );
}

// ─────────────────────────────────────────────────────────────────
// MiSemanaPage
// ─────────────────────────────────────────────────────────────────

function MiSemanaPage() {
  useDocumentTitle("Mi Semana");
  const queryClient = useQueryClient();
  const [personaId, setPersonaId] = useState<number | null>(null);
  const [semanaInicio, setSemanaInicio] = useState<string>(thisWeekMonday());
  const [cellModal, setCellModal] = useState<{
    jiraKey: string;
    fecha: string;
  } | null>(null);

  const { data: personasPage, isLoading: loadingPersonas } = useQuery({
    queryKey: ["personas-list"],
    queryFn: () => personaService.listar(0, 200),
  });

  const personas = personasPage?.content ?? [];

  const {
    data: semana,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["worklog-mi-semana", personaId, semanaInicio],
    queryFn: () => jiraWorklogService.getMiSemana(personaId!, semanaInicio),
    enabled: personaId !== null,
  });

  const handleCellSaved = () => {
    setCellModal(null);
    queryClient.invalidateQueries({
      queryKey: ["worklog-mi-semana", personaId, semanaInicio],
    });
  };

  const days = semana
    ? getDaysFromMonday(semana.semanaInicio)
    : getDaysFromMonday(semanaInicio);

  // Total horas por día (suma de todas las filas para ese día)
  const totalPorDia = (fecha: string) => {
    if (!semana) return 0;
    return semana.filas.reduce((sum, fila) => {
      const celda = fila.dias.find((d) => d.fecha === fecha);
      return sum + (celda?.horas ?? 0);
    }, 0);
  };

  return (
    <div className="space-y-4 max-w-5xl">
      <JiraTabs active="mi-semana" />

      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-foreground">Mi Semana</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Vista semanal de imputaciones de horas.
        </p>
      </div>

      {/* Controles */}
      <div
        className="flex items-center gap-3 flex-wrap"
        role="group"
        aria-label="Filtros de semana"
      >
        <label htmlFor="misemana-persona" className="sr-only">
          Persona
        </label>
        <select
          id="misemana-persona"
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

        {/* Navegación semanal */}
        <div className="flex items-center gap-1">
          <button
            onClick={() => setSemanaInicio(addWeeks(semanaInicio, -1))}
            className="p-1.5 rounded hover:bg-accent transition-colors text-muted-foreground hover:text-foreground"
            aria-label="Semana anterior"
          >
            <ChevronLeft className="h-4 w-4" aria-hidden="true" />
          </button>
          <span
            className="px-3 py-1.5 text-sm text-foreground min-w-[140px] text-center"
            aria-live="polite"
          >
            {semana
              ? `${formatRangeDate(semana.semanaInicio)} – ${formatRangeDate(semana.semanaFin)}`
              : `${formatRangeDate(semanaInicio)} – …`}
          </span>
          <button
            onClick={() => setSemanaInicio(addWeeks(semanaInicio, 1))}
            className="p-1.5 rounded hover:bg-accent transition-colors text-muted-foreground hover:text-foreground"
            aria-label="Semana siguiente"
          >
            <ChevronRight className="h-4 w-4" aria-hidden="true" />
          </button>
          <button
            onClick={() => setSemanaInicio(thisWeekMonday())}
            className="px-2 py-1.5 text-xs text-muted-foreground hover:text-foreground hover:bg-accent rounded transition-colors"
          >
            Esta semana
          </button>
        </div>
      </div>

      {/* Contenido */}
      {!personaId ? (
        <div className="border border-dashed border-border rounded-lg p-10 text-center text-muted-foreground">
          <Clock
            className="h-10 w-10 mx-auto mb-3 opacity-40"
            aria-hidden="true"
          />
          <p className="text-sm">Selecciona una persona para ver su semana.</p>
        </div>
      ) : isLoading ? (
        <div className="space-y-2" role="status">
          <span className="sr-only">Cargando datos de la semana...</span>
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-12 rounded bg-muted animate-pulse" />
          ))}
        </div>
      ) : error || !semana ? (
        <div
          className="border border-border rounded-lg p-4 text-sm text-muted-foreground"
          role="alert"
        >
          Error al cargar los datos de la semana.
        </div>
      ) : (
        <div className="overflow-x-auto">
          {/* Resumen semanal */}
          <div className="flex items-center gap-4 mb-3 text-sm">
            <span className="text-muted-foreground">
              Total imputado:{" "}
              <strong className="text-foreground">
                {semana.totalHorasSemana}h
              </strong>{" "}
              / {semana.totalCapacidadSemana}h capacidad
            </span>
            <div
              className="flex-1 max-w-xs h-1.5 bg-muted rounded-full overflow-hidden"
              role="progressbar"
              aria-valuenow={semana.totalHorasSemana}
              aria-valuemin={0}
              aria-valuemax={semana.totalCapacidadSemana}
              aria-label="Progreso de imputación semanal"
            >
              <div
                className="h-1.5 bg-primary rounded-full"
                style={{
                  width: `${Math.min(
                    (semana.totalHorasSemana / semana.totalCapacidadSemana) *
                      100,
                    100,
                  )}%`,
                }}
              />
            </div>
          </div>

          {/* Grid */}
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="border-b border-border">
                <th
                  scope="col"
                  className="text-left py-2 px-3 text-xs font-semibold text-muted-foreground w-52"
                >
                  Issue
                </th>
                {days.map((d) => (
                  <th
                    key={d}
                    scope="col"
                    className="py-2 px-2 text-center text-xs font-semibold text-muted-foreground min-w-[80px]"
                  >
                    <div className="capitalize">{formatHeaderDate(d)}</div>
                    <div className="text-[10px] text-muted-foreground/70 font-normal">
                      {semana.horasCapacidadDia}h cap.
                    </div>
                  </th>
                ))}
                <th
                  scope="col"
                  className="py-2 px-2 text-center text-xs font-semibold text-muted-foreground min-w-[60px]"
                >
                  Total
                </th>
              </tr>
            </thead>
            <tbody>
              {semana.filas.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="py-8 text-center text-muted-foreground text-sm"
                  >
                    Sin imputaciones registradas esta semana.
                  </td>
                </tr>
              ) : (
                semana.filas.map((fila) => (
                  <tr
                    key={fila.jiraKey}
                    className="border-b border-border/50 hover:bg-accent/20 transition-colors"
                  >
                    <td className="py-2.5 px-3">
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-mono font-semibold text-primary">
                          {fila.jiraKey}
                        </span>
                      </div>
                      <p className="text-xs text-muted-foreground truncate max-w-[180px]">
                        {fila.issueSummary}
                      </p>
                    </td>
                    {days.map((d) => {
                      const celda = fila.dias.find((c) => c.fecha === d);
                      const tieneHoras = celda && celda.horas > 0;
                      return (
                        <td key={d} className="py-2 px-2 text-center">
                          <button
                            onClick={() =>
                              setCellModal({
                                jiraKey: fila.jiraKey,
                                fecha: d,
                              })
                            }
                            className={`w-full rounded py-1 text-xs transition-colors ${
                              tieneHoras
                                ? "bg-primary/10 text-primary font-semibold hover:bg-primary/20"
                                : "text-muted-foreground/40 hover:bg-accent/40"
                            }`}
                            aria-label={
                              tieneHoras
                                ? `${celda.horas}h — pulsa para añadir`
                                : "Sin horas — pulsa para añadir"
                            }
                          >
                            {tieneHoras ? (
                              `${celda.horas}h`
                            ) : (
                              <PlusCircle
                                className="h-3 w-3 mx-auto opacity-30"
                                aria-hidden="true"
                              />
                            )}
                          </button>
                        </td>
                      );
                    })}
                    <td className="py-2 px-2 text-center">
                      <span className="text-xs font-semibold text-foreground">
                        {fila.totalHorasTarea}h
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
            {/* Fila de totales */}
            {semana.filas.length > 0 && (
              <tfoot>
                <tr className="border-t-2 border-border bg-muted/30">
                  <td className="py-2.5 px-3 text-xs font-semibold text-muted-foreground">
                    Total día
                  </td>
                  {days.map((d) => {
                    const total = totalPorDia(d);
                    const pct =
                      semana.horasCapacidadDia > 0
                        ? total / semana.horasCapacidadDia
                        : 0;
                    return (
                      <td key={d} className="py-2 px-2 text-center">
                        <span
                          className={`text-xs font-bold ${
                            pct >= 1
                              ? "text-green-600"
                              : pct >= 0.5
                                ? "text-amber-600"
                                : "text-muted-foreground"
                          }`}
                        >
                          {total > 0 ? `${total}h` : "—"}
                        </span>
                      </td>
                    );
                  })}
                  <td className="py-2 px-2 text-center">
                    <span className="text-xs font-bold text-foreground">
                      {semana.totalHorasSemana}h
                    </span>
                  </td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      )}

      {/* Modal para celda */}
      {cellModal && personaId && (
        <CellWorklogModal
          jiraKey={cellModal.jiraKey}
          personaId={personaId}
          fecha={cellModal.fecha}
          onSuccess={handleCellSaved}
          onClose={() => setCellModal(null)}
        />
      )}
    </div>
  );
}

// Named export for testing
export { MiSemanaPage };
