/**
 * ModalPlanificarIssue — Planifica un issue Jira (y sus subtareas) como tarea KAOS.
 * Presentacional: recibe datos, emite submit/cancel.
 * Soporta pre-carga desde plantilla y sugerencia de persona.
 */

import { AccessibleModal } from "@/components/ui/AccessibleModal";
import { toast } from "@/lib/toast";
import { jiraIssueService } from "@/services/jiraIssueService";
import { plantillaService } from "@/services/plantillaService";
import type { PlanificarAsignacionItem } from "@/types/api";
import type { JiraIssueResponse } from "@/types/jira";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Lightbulb, Wand2 } from "lucide-react";
import { useState } from "react";

// ─────────────────────────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────────────────────────

interface IssueFormRow {
  jiraKey: string;
  summary: string;
  tipoJira: string;
  estimacionJira: number | null;
  /** Incluir en la planificación */
  incluir: boolean;
  personaId: string;
  estimacion: string;
  diaAsignado: string;
  tipo: string;
  categoria: string;
  prioridad: string;
}

interface Props {
  issue: JiraIssueResponse;
  sprintId: number;
  personas: { id: number; nombre: string }[];
  onSuccess: () => void;
  onClose: () => void;
}

const TIPOS_KAOS = [
  { value: "HISTORIA", label: "📖 Historia" },
  { value: "TAREA", label: "✓ Tarea" },
  { value: "BUG", label: "🐛 Bug" },
  { value: "SPIKE", label: "⚡ Spike" },
];

const CATEGORIAS = [
  { value: "CORRECTIVO", label: "🔧 Correctivo" },
  { value: "EVOLUTIVO", label: "✨ Evolutivo" },
];

const PRIORIDADES = [
  { value: "BAJA", label: "↓ Baja" },
  { value: "NORMAL", label: "➡ Normal" },
  { value: "ALTA", label: "↑ Alta" },
  { value: "BLOQUEANTE", label: "🚫 Bloqueante" },
];

// ─────────────────────────────────────────────────────────────────
// Normalizar tipoJira → TipoTarea KAOS
// ─────────────────────────────────────────────────────────────────

function tipoJiraAKaos(tipoJira: string): string {
  const map: Record<string, string> = {
    Story: "HISTORIA",
    Bug: "BUG",
    Spike: "SPIKE",
    "Sub-task": "TAREA",
    Task: "TAREA",
  };
  return map[tipoJira] ?? "TAREA";
}

function categoriaDeJira(tipoJira: string): string {
  return tipoJira === "Bug" ? "CORRECTIVO" : "EVOLUTIVO";
}

// ─────────────────────────────────────────────────────────────────
// Construir fila inicial
// ─────────────────────────────────────────────────────────────────

function buildRow(issue: JiraIssueResponse, incluir = true): IssueFormRow {
  return {
    jiraKey: issue.jiraKey,
    summary: issue.summary,
    tipoJira: issue.tipoJira,
    estimacionJira: issue.estimacionHoras,
    incluir,
    personaId: "",
    estimacion:
      issue.estimacionHoras != null ? String(issue.estimacionHoras) : "",
    diaAsignado: "",
    tipo: tipoJiraAKaos(issue.tipoJira),
    categoria: categoriaDeJira(issue.tipoJira),
    prioridad: "NORMAL",
  };
}

// ─────────────────────────────────────────────────────────────────
// Componente
// ─────────────────────────────────────────────────────────────────

export function ModalPlanificarIssue({
  issue,
  sprintId,
  personas,
  onSuccess,
  onClose,
}: Props) {
  // Construir filas: padre + subtareas
  const [rows, setRows] = useState<IssueFormRow[]>(() => [
    buildRow(issue),
    ...issue.subtareas.map((s) => buildRow(s, true)),
  ]);
  const [sugiriendoPara, setSugiriendoPara] = useState<string | null>(null);

  // ── Plantilla del tipo principal ─────────────────────────────
  const { data: itemsPlantilla } = useQuery({
    queryKey: ["plantilla-aplicar", issue.tipoJira, issue.estimacionHoras],
    queryFn: () =>
      plantillaService.aplicar(issue.tipoJira, issue.estimacionHoras ?? 1),
    enabled: !!issue.tipoJira,
  });

  // ── Sugerencia de persona ─────────────────────────────────────
  const sugerenciaMutation = useMutation({
    mutationFn: ({ jiraKey }: { jiraKey: string }) =>
      jiraIssueService.sugerencia(jiraKey, sprintId),
    onSuccess: (sugerencias, { jiraKey }) => {
      if (sugerencias.length === 0) {
        toast.info("No hay sugerencias disponibles para este issue");
        return;
      }
      const personaId = String(sugerencias[0].personaId);
      setRows((prev) =>
        prev.map((r) => (r.jiraKey === jiraKey ? { ...r, personaId } : r)),
      );
      toast.success(
        `Persona sugerida: ${sugerencias[0].personaNombre} (${sugerencias[0].horasDisponibles}h libres)`,
      );
    },
    onError: () => toast.error("Error al obtener sugerencia"),
  });

  // ── Aplicar plantilla ─────────────────────────────────────────
  function aplicarPlantilla() {
    if (!itemsPlantilla || itemsPlantilla.length === 0) {
      toast.info("No hay plantilla activa para el tipo " + issue.tipoJira);
      return;
    }
    // Pre-fill padre con primer ítem de la plantilla
    const primerItem = itemsPlantilla[0];
    setRows((prev) =>
      prev.map((r, idx) => {
        if (idx === 0) {
          return {
            ...r,
            estimacion:
              primerItem.estimacion != null
                ? String(primerItem.estimacion)
                : r.estimacion,
          };
        }
        return r;
      }),
    );
    toast.success("Plantilla aplicada");
  }

  // ── Planificar mutation ───────────────────────────────────────
  const planificarMutation = useMutation({
    mutationFn: () => {
      const asignaciones: PlanificarAsignacionItem[] = rows
        .filter((r) => r.incluir)
        .map((r) => ({
          jiraKey: r.jiraKey,
          personaId: r.personaId ? Number(r.personaId) : undefined,
          estimacion: r.estimacion ? parseFloat(r.estimacion) : undefined,
          diaAsignado: r.diaAsignado ? Number(r.diaAsignado) : undefined,
          tipo: r.tipo,
          categoria: r.categoria,
          prioridad: r.prioridad,
        }));
      return jiraIssueService.planificar({ sprintId, asignaciones });
    },
    onSuccess: () => {
      toast.success("Issues planificados correctamente");
      onSuccess();
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message ?? "Error al planificar";
      toast.error(msg);
    },
  });

  // ── Helpers ───────────────────────────────────────────────────
  function updateRow(jiraKey: string, patch: Partial<IssueFormRow>) {
    setRows((prev) =>
      prev.map((r) => (r.jiraKey === jiraKey ? { ...r, ...patch } : r)),
    );
  }

  const rowsIncluidas = rows.filter((r) => r.incluir);
  const puedeEnviar =
    rowsIncluidas.length > 0 &&
    rowsIncluidas.every((r) => !r.estimacion || parseFloat(r.estimacion) > 0);

  return (
    <AccessibleModal
      isOpen
      onClose={onClose}
      title={`Planificar: ${issue.jiraKey}`}
      size="xl"
    >
      {/* Header del issue */}
      <div className="mb-4 p-3 rounded-md bg-muted/50 border border-border">
        <div className="flex items-start justify-between gap-2">
          <div>
            <p className="text-xs text-muted-foreground font-mono mb-0.5">
              {issue.jiraKey} · {issue.tipoJira}
            </p>
            <p className="text-sm font-medium text-foreground">
              {issue.summary}
            </p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            {issue.estimacionHoras != null && (
              <span className="text-xs px-2 py-0.5 rounded bg-blue-500/10 text-blue-600 border border-blue-400/30">
                {issue.estimacionHoras}h Jira
              </span>
            )}
            {itemsPlantilla && itemsPlantilla.length > 0 && (
              <button
                type="button"
                onClick={aplicarPlantilla}
                className="flex items-center gap-1 text-xs px-2 py-1 rounded border border-border hover:bg-accent transition-colors"
                title="Aplicar plantilla de asignación"
              >
                <Wand2 className="h-3 w-3" />
                Plantilla
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Tabla de issues a planificar */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm border-collapse">
          <thead>
            <tr className="border-b border-border text-xs text-muted-foreground">
              <th className="text-left pb-2 w-6"></th>
              <th className="text-left pb-2 pr-2">Issue</th>
              <th className="text-left pb-2 pr-2 w-40">Persona</th>
              <th className="text-left pb-2 pr-2 w-24">Estimación (h)</th>
              <th className="text-left pb-2 pr-2 w-20">Día</th>
              <th className="text-left pb-2 pr-2 w-28">Tipo KAOS</th>
              <th className="text-left pb-2 pr-2 w-28">Categoría</th>
              <th className="text-left pb-2 w-8"></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, idx) => (
              <tr
                key={row.jiraKey}
                className={`border-b border-border/50 ${!row.incluir ? "opacity-40" : ""}`}
              >
                {/* Checkbox incluir */}
                <td className="py-2 pr-2">
                  <input
                    type="checkbox"
                    checked={row.incluir}
                    onChange={(e) =>
                      updateRow(row.jiraKey, { incluir: e.target.checked })
                    }
                    className="h-4 w-4 rounded border-border"
                    aria-label={`Incluir ${row.jiraKey}`}
                  />
                </td>

                {/* Issue info */}
                <td className="py-2 pr-2">
                  <div className={idx > 0 ? "pl-4" : ""}>
                    <p className="text-xs font-mono text-muted-foreground">
                      {idx > 0 && "↳ "}
                      {row.jiraKey}
                    </p>
                    <p
                      className="text-xs text-foreground truncate max-w-[200px]"
                      title={row.summary}
                    >
                      {row.summary}
                    </p>
                  </div>
                </td>

                {/* Persona */}
                <td className="py-2 pr-2">
                  <select
                    value={row.personaId}
                    onChange={(e) =>
                      updateRow(row.jiraKey, { personaId: e.target.value })
                    }
                    disabled={!row.incluir}
                    className="w-full px-2 py-1 rounded border border-border bg-background text-foreground text-xs focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50"
                    aria-label={`Persona para ${row.jiraKey}`}
                  >
                    <option value="">Sin asignar</option>
                    {personas.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.nombre}
                      </option>
                    ))}
                  </select>
                </td>

                {/* Estimación */}
                <td className="py-2 pr-2">
                  <input
                    type="number"
                    value={row.estimacion}
                    onChange={(e) =>
                      updateRow(row.jiraKey, { estimacion: e.target.value })
                    }
                    disabled={!row.incluir}
                    min="0.25"
                    step="0.25"
                    placeholder={
                      row.estimacionJira != null
                        ? String(row.estimacionJira)
                        : "0.0"
                    }
                    className="w-full px-2 py-1 rounded border border-border bg-background text-foreground text-xs focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50"
                    aria-label={`Estimación para ${row.jiraKey}`}
                  />
                </td>

                {/* Día */}
                <td className="py-2 pr-2">
                  <input
                    type="number"
                    value={row.diaAsignado}
                    onChange={(e) =>
                      updateRow(row.jiraKey, { diaAsignado: e.target.value })
                    }
                    disabled={!row.incluir}
                    min="1"
                    placeholder="—"
                    className="w-full px-2 py-1 rounded border border-border bg-background text-foreground text-xs focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50"
                    aria-label={`Día para ${row.jiraKey}`}
                  />
                </td>

                {/* Tipo KAOS */}
                <td className="py-2 pr-2">
                  <select
                    value={row.tipo}
                    onChange={(e) =>
                      updateRow(row.jiraKey, { tipo: e.target.value })
                    }
                    disabled={!row.incluir}
                    className="w-full px-2 py-1 rounded border border-border bg-background text-foreground text-xs focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50"
                  >
                    {TIPOS_KAOS.map((t) => (
                      <option key={t.value} value={t.value}>
                        {t.label}
                      </option>
                    ))}
                  </select>
                </td>

                {/* Categoría */}
                <td className="py-2 pr-2">
                  <select
                    value={row.categoria}
                    onChange={(e) =>
                      updateRow(row.jiraKey, { categoria: e.target.value })
                    }
                    disabled={!row.incluir}
                    className="w-full px-2 py-1 rounded border border-border bg-background text-foreground text-xs focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50"
                  >
                    {CATEGORIAS.map((c) => (
                      <option key={c.value} value={c.value}>
                        {c.label}
                      </option>
                    ))}
                  </select>
                </td>

                {/* Sugerir persona */}
                <td className="py-2">
                  <button
                    type="button"
                    onClick={() => {
                      setSugiriendoPara(row.jiraKey);
                      sugerenciaMutation.mutate({ jiraKey: row.jiraKey });
                    }}
                    disabled={
                      !row.incluir ||
                      (sugerenciaMutation.isPending &&
                        sugiriendoPara === row.jiraKey)
                    }
                    title="Sugerir persona según capacidad"
                    className="p-1 text-muted-foreground hover:text-primary transition-colors disabled:opacity-50"
                    aria-label={`Sugerir persona para ${row.jiraKey}`}
                  >
                    <Lightbulb className="h-4 w-4" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Prioridad global (se aplica a todos los incluidos) */}
      <div className="mt-4 flex items-center gap-4 text-sm">
        <span className="text-muted-foreground text-xs">Prioridad global:</span>
        <div className="flex gap-2">
          {PRIORIDADES.map((p) => (
            <button
              key={p.value}
              type="button"
              onClick={() =>
                setRows((prev) =>
                  prev.map((r) =>
                    r.incluir ? { ...r, prioridad: p.value } : r,
                  ),
                )
              }
              className={`px-2 py-0.5 rounded text-xs border transition-colors ${
                rows.every((r) => !r.incluir || r.prioridad === p.value)
                  ? "bg-primary text-primary-foreground border-primary"
                  : "border-border hover:bg-accent"
              }`}
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>

      {/* Error */}
      {planificarMutation.isError && (
        <div
          role="alert"
          className="mt-3 text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded p-2"
        >
          {(planificarMutation.error as any)?.response?.data?.message ??
            "Error al planificar. Revisa los datos e inténtalo de nuevo."}
        </div>
      )}

      {/* Botones */}
      <div className="mt-5 flex justify-between items-center">
        <p className="text-xs text-muted-foreground">
          {rowsIncluidas.length} issue
          {rowsIncluidas.length !== 1 ? "s" : ""} a planificar
        </p>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={onClose}
            disabled={planificarMutation.isPending}
            className="px-4 py-2 bg-secondary text-secondary-foreground rounded-md text-sm font-medium hover:bg-secondary/80 transition-colors disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={() => planificarMutation.mutate()}
            disabled={!puedeEnviar || planificarMutation.isPending}
            className="px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {planificarMutation.isPending ? "Planificando..." : "Planificar"}
          </button>
        </div>
      </div>
    </AccessibleModal>
  );
}
