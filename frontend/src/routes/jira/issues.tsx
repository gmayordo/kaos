/**
 * Issues Jira — Lista de issues planificables por sprint.
 * Bloque 5: planificación avanzada desde Jira → KAOS.
 */

import { JiraTabs } from "@/components/jira/JiraTabs";
import { ModalPlanificarIssue } from "@/features/planificacion/ModalPlanificarIssue";
import { useDocumentTitle } from "@/lib/useDocumentTitle";
import { jiraIssueService } from "@/services/jiraIssueService";
import { personaService } from "@/services/personaService";
import { sprintService } from "@/services/sprintService";
import { squadService } from "@/services/squadService";
import type { JiraIssueResponse } from "@/types/jira";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import {
  ChevronDown,
  ChevronRight,
  Filter,
  GitBranch,
  ListChecks,
  Search,
} from "lucide-react";
import { useMemo, useState } from "react";

export const Route = createFileRoute("/jira/issues")({
  component: IssuesPage,
});

// ─────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────

function badgePlanificado(tareaId: number | null) {
  if (tareaId != null) {
    return (
      <span className="inline-flex items-center text-xs px-1.5 py-0.5 rounded-full bg-muted text-muted-foreground border border-border">
        Ya planificada
      </span>
    );
  }
  return (
    <span className="inline-flex items-center text-xs px-1.5 py-0.5 rounded-full bg-blue-500/10 text-blue-600 border border-blue-400/30">
      Pendiente
    </span>
  );
}

function badgeTipo(tipo: string) {
  const colors: Record<string, string> = {
    Story: "bg-emerald-500/10 text-emerald-700 border-emerald-400/30",
    Bug: "bg-red-500/10 text-red-700 border-red-400/30",
    Task: "bg-purple-500/10 text-purple-700 border-purple-400/30",
    Spike: "bg-amber-500/10 text-amber-700 border-amber-400/30",
    "Sub-task": "bg-slate-500/10 text-slate-700 border-slate-400/30",
  };
  const cls = colors[tipo] ?? "bg-muted text-muted-foreground border-border";
  return (
    <span
      className={`inline-flex items-center text-xs px-1.5 py-0.5 rounded border ${cls}`}
    >
      {tipo}
    </span>
  );
}

// ─────────────────────────────────────────────────────────────────
// Fila de issue (expandible si tiene subtareas)
// ─────────────────────────────────────────────────────────────────

interface IssueRowProps {
  issue: JiraIssueResponse;
  level: number;
  expanded: boolean;
  onToggleExpand: (key: string) => void;
  selected: boolean;
  onToggleSelect: (key: string) => void;
  onPlanificar: (issue: JiraIssueResponse) => void;
}

function IssueRow({
  issue,
  level,
  expanded,
  onToggleExpand,
  selected,
  onToggleSelect,
  onPlanificar,
}: IssueRowProps) {
  const hasSubtareas = issue.subtareas.length > 0;
  const isPlanificado = issue.tareaId != null;

  return (
    <tr
      className={`border-b border-border/50 hover:bg-muted/30 transition-colors ${
        selected ? "bg-primary/5" : ""
      }`}
    >
      {/* Checkbox selección */}
      <td className="py-2 pl-3 pr-1 w-6">
        {!isPlanificado && (
          <input
            type="checkbox"
            checked={selected}
            onChange={() => onToggleSelect(issue.jiraKey)}
            className="h-4 w-4 rounded border-border"
            aria-label={`Seleccionar ${issue.jiraKey}`}
          />
        )}
      </td>

      {/* Expand toggle */}
      <td className="py-2 pr-1 w-6">
        {hasSubtareas && (
          <button
            type="button"
            onClick={() => onToggleExpand(issue.jiraKey)}
            className="p-0.5 rounded hover:bg-accent transition-colors text-muted-foreground"
            aria-label={expanded ? "Contraer subtareas" : "Expandir subtareas"}
            aria-expanded={expanded}
          >
            {expanded ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )}
          </button>
        )}
      </td>

      {/* Clave + Resumen */}
      <td className="py-2 pr-4">
        <div style={{ paddingLeft: `${level * 20}px` }}>
          <a
            href={`#jira-issue-${issue.jiraKey}`}
            className="text-xs font-mono text-primary hover:underline"
          >
            {issue.jiraKey}
          </a>
          <p className="text-sm text-foreground mt-0.5 leading-tight">
            {issue.summary}
          </p>
        </div>
      </td>

      {/* Tipo */}
      <td className="py-2 pr-3 whitespace-nowrap">
        {badgeTipo(issue.tipoJira)}
      </td>

      {/* Asignado */}
      <td className="py-2 pr-3 text-sm text-muted-foreground whitespace-nowrap">
        {issue.asignadoNombre ?? "—"}
      </td>

      {/* Estimación Jira */}
      <td className="py-2 pr-3 text-sm text-right tabular-nums whitespace-nowrap">
        {issue.estimacionHoras != null ? (
          <span className="text-foreground">{issue.estimacionHoras}h</span>
        ) : (
          <span className="text-muted-foreground">—</span>
        )}
      </td>

      {/* Subtareas count */}
      <td className="py-2 pr-3 text-center text-sm">
        {hasSubtareas ? (
          <span className="inline-flex items-center gap-0.5 text-xs text-muted-foreground">
            <ListChecks className="h-3 w-3" />
            {issue.subtareas.length}
          </span>
        ) : (
          <span className="text-muted-foreground">—</span>
        )}
      </td>

      {/* Estado */}
      <td className="py-2 pr-4 whitespace-nowrap">
        {badgePlanificado(issue.tareaId)}
      </td>

      {/* Acción */}
      <td className="py-2 pr-3 text-right whitespace-nowrap">
        {!isPlanificado && (
          <button
            type="button"
            onClick={() => onPlanificar(issue)}
            className="text-xs px-3 py-1.5 rounded bg-primary text-primary-foreground hover:bg-primary/90 transition-colors font-medium"
          >
            Planificar
          </button>
        )}
        {isPlanificado && (
          <span className="text-xs text-muted-foreground">
            KAOS #{issue.tareaId}
          </span>
        )}
      </td>
    </tr>
  );
}

// ─────────────────────────────────────────────────────────────────
// Página principal
// ─────────────────────────────────────────────────────────────────

export function IssuesPage() {
  useDocumentTitle("Issues Jira");
  const queryClient = useQueryClient();

  // ── Estado de filtros ──────────────────────────────────────────
  const [selectedSquadId, setSelectedSquadId] = useState<number | null>(null);
  const [selectedSprintId, setSelectedSprintId] = useState<number | null>(null);
  const [textFilter, setTextFilter] = useState("");
  const [tipoFilter, setTipoFilter] = useState("");
  const [soloPendientes, setSoloPendientes] = useState(false);

  // ── Estado de UI ───────────────────────────────────────────────
  const [expandedKeys, setExpandedKeys] = useState<Set<string>>(new Set());
  const [selectedKeys, setSelectedKeys] = useState<Set<string>>(new Set());
  const [modalIssue, setModalIssue] = useState<JiraIssueResponse | null>(null);
  const [modalBulkOpen, setModalBulkOpen] = useState(false);

  // ── Queries ────────────────────────────────────────────────────
  const { data: squadsPage, isLoading: loadingSquads } = useQuery({
    queryKey: ["squads-activos"],
    queryFn: () => squadService.listar(0, 100, "ACTIVO"),
  });
  const squads = squadsPage?.content ?? [];

  const { data: sprintsPage } = useQuery({
    queryKey: ["sprints", selectedSquadId],
    queryFn: () =>
      sprintService.listar(0, 50, {
        squadId: selectedSquadId ?? undefined,
      }),
    enabled: selectedSquadId != null,
  });
  const sprints = sprintsPage?.content ?? [];

  const selectedSprint = sprints.find((s) => s.id === selectedSprintId);

  const {
    data: issues = [],
    isLoading: loadingIssues,
    error: issuesError,
  } = useQuery({
    queryKey: [
      "jira-issues",
      soloPendientes,
      tipoFilter,
      selectedSprint?.nombre,
    ],
    queryFn: () =>
      jiraIssueService.listar({
        soloPendientes,
        tipo: tipoFilter || undefined,
        sprintNombre: selectedSprint?.nombre,
      }),
    enabled: true,
  });

  const { data: personasPage } = useQuery({
    queryKey: ["personas-activas"],
    queryFn: () => personaService.listar(0, 200),
  });
  const personas = (personasPage?.content ?? []).map((p) => ({
    id: p.id,
    nombre: p.nombre,
  }));

  // ── Filtros locales ────────────────────────────────────────────
  const filteredIssues = useMemo(() => {
    const q = textFilter.toLowerCase().trim();
    return issues.filter((issue) => {
      if (
        q &&
        !issue.jiraKey.toLowerCase().includes(q) &&
        !issue.summary.toLowerCase().includes(q)
      ) {
        return false;
      }
      return true;
    });
  }, [issues, textFilter]);

  // ── Handlers ──────────────────────────────────────────────────
  function toggleExpand(key: string) {
    setExpandedKeys((prev) => {
      const next = new Set(prev);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  }

  function toggleSelect(key: string) {
    setSelectedKeys((prev) => {
      const next = new Set(prev);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  }

  function toggleSelectAll() {
    const planificables = filteredIssues.filter((i) => i.tareaId == null);
    if (selectedKeys.size === planificables.length) {
      setSelectedKeys(new Set());
    } else {
      setSelectedKeys(new Set(planificables.map((i) => i.jiraKey)));
    }
  }

  function handlePlanificado() {
    setModalIssue(null);
    setModalBulkOpen(false);
    setSelectedKeys(new Set());
    queryClient.invalidateQueries({ queryKey: ["jira-issues"] });
  }

  // Issue sintético para bulk planificar
  const bulkIssue: JiraIssueResponse | null = useMemo(() => {
    if (selectedKeys.size === 0) return null;
    const selectedIssues = filteredIssues.filter((i) =>
      selectedKeys.has(i.jiraKey),
    );
    if (selectedIssues.length === 0) return null;
    // Usamos el primero como "padre" y el resto como sus "subtareas" en el modal
    const [first, ...rest] = selectedIssues;
    return {
      ...first,
      subtareas: [...first.subtareas, ...rest],
    };
  }, [selectedKeys, filteredIssues]);

  const planificables = filteredIssues.filter((i) => i.tareaId == null);
  const allSelected =
    planificables.length > 0 && selectedKeys.size === planificables.length;

  // ── Render ────────────────────────────────────────────────────
  return (
    <div className="space-y-5 max-w-7xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
          <GitBranch className="h-6 w-6 text-primary" aria-hidden="true" />
          Issues Jira
        </h1>
        <p className="text-sm text-muted-foreground mt-1">
          List de issues Jira planificables. Desde aquí puedes convertirlos en
          tareas KAOS.
        </p>
      </div>

      {/* JiraTabs */}
      <JiraTabs active="issues" />

      {/* Filtros */}
      <div className="flex flex-wrap items-end gap-3">
        {/* Squad */}
        <div>
          <label className="block text-xs text-muted-foreground mb-1">
            Squad
          </label>
          <select
            value={selectedSquadId ?? ""}
            onChange={(e) => {
              setSelectedSquadId(
                e.target.value ? Number(e.target.value) : null,
              );
              setSelectedSprintId(null);
            }}
            className="px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary min-w-[160px]"
            disabled={loadingSquads}
          >
            <option value="">Todos los squads</option>
            {squads.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre}
              </option>
            ))}
          </select>
        </div>

        {/* Sprint */}
        <div>
          <label className="block text-xs text-muted-foreground mb-1">
            Sprint
          </label>
          <select
            value={selectedSprintId ?? ""}
            onChange={(e) =>
              setSelectedSprintId(
                e.target.value ? Number(e.target.value) : null,
              )
            }
            className="px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary min-w-[180px]"
            disabled={!selectedSquadId || sprints.length === 0}
          >
            <option value="">Todos los sprints</option>
            {sprints.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre}
              </option>
            ))}
          </select>
        </div>

        {/* Tipo */}
        <div>
          <label className="block text-xs text-muted-foreground mb-1">
            Tipo
          </label>
          <select
            value={tipoFilter}
            onChange={(e) => setTipoFilter(e.target.value)}
            className="px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="">Todos</option>
            {["Story", "Task", "Bug", "Spike", "Sub-task"].map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>

        {/* Solo pendientes */}
        <label className="flex items-center gap-2 cursor-pointer select-none pb-0.5">
          <input
            type="checkbox"
            checked={soloPendientes}
            onChange={(e) => setSoloPendientes(e.target.checked)}
            className="h-4 w-4 rounded border-border"
          />
          <span className="text-sm text-foreground">Solo sin planificar</span>
        </label>

        {/* Búsqueda texto */}
        <div className="ml-auto relative">
          <Search
            className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground"
            aria-hidden
          />
          <input
            type="text"
            value={textFilter}
            onChange={(e) => setTextFilter(e.target.value)}
            placeholder="Buscar clave o resumen..."
            className="pl-8 pr-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary w-56"
          />
        </div>
      </div>

      {/* Barra de acciones bulk */}
      {selectedKeys.size > 0 && (
        <div className="flex items-center gap-3 px-4 py-2.5 rounded-md bg-primary/10 border border-primary/30">
          <Filter className="h-4 w-4 text-primary" aria-hidden />
          <span className="text-sm text-foreground font-medium">
            {selectedKeys.size} issue{selectedKeys.size !== 1 ? "s" : ""}{" "}
            seleccionado{selectedKeys.size !== 1 ? "s" : ""}
          </span>
          <button
            type="button"
            onClick={() => {
              if (bulkIssue && selectedSprintId) {
                setModalBulkOpen(true);
              }
            }}
            disabled={!selectedSprintId}
            className="ml-auto text-sm px-4 py-1.5 rounded bg-primary text-primary-foreground hover:bg-primary/90 transition-colors font-medium disabled:opacity-50"
            title={
              !selectedSprintId ? "Selecciona un sprint primero" : undefined
            }
          >
            Planificar seleccionados
          </button>
          <button
            type="button"
            onClick={() => setSelectedKeys(new Set())}
            className="text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            Cancelar
          </button>
        </div>
      )}

      {/* Tabla */}
      <div className="border border-border rounded-lg overflow-hidden bg-card">
        {loadingIssues ? (
          <div className="p-8 text-center">
            <div className="animate-spin h-6 w-6 border-2 border-primary border-t-transparent rounded-full mx-auto mb-2" />
            <p className="text-sm text-muted-foreground">Cargando issues...</p>
          </div>
        ) : issuesError ? (
          <div
            className="p-8 text-center text-destructive text-sm"
            role="alert"
          >
            Error al cargar los issues Jira. Verifica la configuración de
            conexión.
          </div>
        ) : filteredIssues.length === 0 ? (
          <div className="p-8 text-center text-muted-foreground text-sm">
            No hay issues que coincidan con los filtros aplicados.
          </div>
        ) : (
          <table className="w-full text-sm" aria-label="Tabla de issues Jira">
            <thead className="bg-muted/50">
              <tr className="border-b border-border text-xs text-muted-foreground uppercase tracking-wide">
                <th className="py-3 pl-3 pr-1 w-6">
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={toggleSelectAll}
                    className="h-4 w-4 rounded border-border"
                    aria-label="Seleccionar todos"
                  />
                </th>
                <th className="py-3 pr-1 w-6"></th>
                <th className="py-3 pr-4 text-left">Issue</th>
                <th className="py-3 pr-3 text-left">Tipo</th>
                <th className="py-3 pr-3 text-left">Asignado</th>
                <th className="py-3 pr-3 text-right">Estimación</th>
                <th className="py-3 pr-3 text-center">Subtareas</th>
                <th className="py-3 pr-4 text-left">Estado</th>
                <th className="py-3 pr-3 text-right">Acción</th>
              </tr>
            </thead>
            <tbody>
              {filteredIssues.map((issue) => (
                <>
                  <IssueRow
                    key={issue.jiraKey}
                    issue={issue}
                    level={0}
                    expanded={expandedKeys.has(issue.jiraKey)}
                    onToggleExpand={toggleExpand}
                    selected={selectedKeys.has(issue.jiraKey)}
                    onToggleSelect={toggleSelect}
                    onPlanificar={(i) => {
                      if (!selectedSprintId) {
                        alert("Selecciona un sprint antes de planificar");
                        return;
                      }
                      setModalIssue(i);
                    }}
                  />
                  {/* Subtareas */}
                  {expandedKeys.has(issue.jiraKey) &&
                    issue.subtareas.map((sub) => (
                      <IssueRow
                        key={sub.jiraKey}
                        issue={sub}
                        level={1}
                        expanded={false}
                        onToggleExpand={() => {}}
                        selected={selectedKeys.has(sub.jiraKey)}
                        onToggleSelect={toggleSelect}
                        onPlanificar={(i) => {
                          if (!selectedSprintId) {
                            alert("Selecciona un sprint antes de planificar");
                            return;
                          }
                          setModalIssue(i);
                        }}
                      />
                    ))}
                </>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal planificar individual */}
      {modalIssue && selectedSprintId && (
        <ModalPlanificarIssue
          issue={modalIssue}
          sprintId={selectedSprintId}
          personas={personas}
          onSuccess={handlePlanificado}
          onClose={() => setModalIssue(null)}
        />
      )}

      {/* Modal bulk */}
      {modalBulkOpen && bulkIssue && selectedSprintId && (
        <ModalPlanificarIssue
          issue={bulkIssue}
          sprintId={selectedSprintId}
          personas={personas}
          onSuccess={handlePlanificado}
          onClose={() => setModalBulkOpen(false)}
        />
      )}
    </div>
  );
}
