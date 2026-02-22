/**
 * Página Resumen de Sprint (RF-014)
 * Documento de cierre disponible solo para sprints CERRADO.
 * Muestra: identificación, tareas completadas/pendientes, bloqueos, métricas.
 * Permite al LF editar notas finales e imprimir/exportar como PDF.
 */

import { bloqueoService } from "@/services/bloqueoService";
import { planificacionService } from "@/services/planificacionService";
import { sprintService } from "@/services/sprintService";
import { tareaService } from "@/services/tareaService";
import type { BloqueoResponse, TareaResponse } from "@/types/api";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { clsx } from "clsx";
import {
  AlertTriangle,
  ArrowLeft,
  CalendarRange,
  CheckCircle2,
  Circle,
  Clock,
  Kanban,
  LayoutDashboard,
  Lock,
  PrinterIcon,
  Target,
  Users,
} from "lucide-react";
import { useRef, useState } from "react";

export const Route = createFileRoute("/planificacion/resumen/$sprintId")({
  component: ResumenPage,
});

// ============= Helpers =============

const estadoTareaLabel: Record<string, string> = {
  PENDIENTE: "Pendiente",
  EN_PROGRESO: "En progreso",
  BLOQUEADO: "Bloqueado",
  COMPLETADA: "Completada",
};

const estadoBloqueoLabel: Record<string, string> = {
  ACTIVO: "Activo",
  EN_GESTION: "En gestión",
  RESUELTO: "Resuelto",
};

const estadoBloqueoBadge: Record<string, string> = {
  ACTIVO: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300",
  EN_GESTION:
    "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300",
  RESUELTO:
    "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300",
};

const tipoBloqueoLabel: Record<string, string> = {
  DEPENDENCIA_EXTERNA: "Dependencia externa",
  RECURSO: "Recurso",
  TECNICO: "Técnico",
  COMUNICACION: "Comunicación",
  OTRO: "Otro",
};

// ============= Skeleton =============

function ResumenSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      <div className="h-8 w-64 rounded bg-gray-200 dark:bg-gray-700" />
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className="h-24 rounded-lg bg-gray-100 dark:bg-gray-800"
          />
        ))}
      </div>
      <div className="h-48 rounded-lg bg-gray-100 dark:bg-gray-800" />
      <div className="h-48 rounded-lg bg-gray-100 dark:bg-gray-800" />
    </div>
  );
}

// ============= Fila de tarea =============

function TareaRow({ tarea }: { tarea: TareaResponse }) {
  const completada = tarea.estado === "COMPLETADA";
  return (
    <tr className="border-b border-border last:border-0">
      <td className="py-2 pr-4">
        <div className="flex items-center gap-2">
          {completada ? (
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-500" />
          ) : (
            <Circle className="h-4 w-4 shrink-0 text-gray-400" />
          )}
          <span
            className={clsx(
              "text-sm",
              completada ? "text-foreground" : "text-muted-foreground italic",
            )}
          >
            {tarea.titulo}
          </span>
          {tarea.referenciaJira && (
            <span className="text-xs text-muted-foreground">
              [{tarea.referenciaJira}]
            </span>
          )}
        </div>
      </td>
      <td className="py-2 pr-4 text-sm text-muted-foreground whitespace-nowrap">
        {tarea.personaNombre ?? "—"}
      </td>
      <td className="py-2 pr-4 text-sm text-right text-muted-foreground whitespace-nowrap">
        {tarea.estimacion}h
      </td>
      <td className="py-2 text-sm whitespace-nowrap">
        <span
          className={clsx(
            "rounded-full px-2 py-0.5 text-xs font-medium",
            completada
              ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300"
              : tarea.estado === "BLOQUEADO"
                ? "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300"
                : "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400",
          )}
        >
          {estadoTareaLabel[tarea.estado] ?? tarea.estado}
        </span>
      </td>
    </tr>
  );
}

// ============= Fila de bloqueo =============

function BloqueoRow({ bloqueo }: { bloqueo: BloqueoResponse }) {
  return (
    <tr className="border-b border-border last:border-0">
      <td className="py-2 pr-4 text-sm text-foreground">{bloqueo.titulo}</td>
      <td className="py-2 pr-4 text-sm text-muted-foreground">
        {tipoBloqueoLabel[bloqueo.tipo] ?? bloqueo.tipo}
      </td>
      <td className="py-2 pr-4 text-sm text-muted-foreground whitespace-nowrap">
        {bloqueo.responsableNombre ?? "—"}
      </td>
      <td className="py-2 pr-4 text-sm text-muted-foreground whitespace-nowrap">
        {bloqueo.createdAt ? bloqueo.createdAt.split("T")[0] : "—"}
      </td>
      <td className="py-2 text-sm whitespace-nowrap">
        <span
          className={clsx(
            "rounded-full px-2 py-0.5 text-xs font-medium",
            estadoBloqueoBadge[bloqueo.estado] ?? "bg-gray-100 text-gray-600",
          )}
        >
          {estadoBloqueoLabel[bloqueo.estado] ?? bloqueo.estado}
        </span>
      </td>
    </tr>
  );
}

// ============= Página principal =============

function ResumenPage() {
  const { sprintId } = Route.useParams();
  const id = Number(sprintId);
  const printRef = useRef<HTMLDivElement>(null);
  const [notasLF, setNotasLF] = useState("");

  // ── Queries ───────────────────────────────────────────────────────────────
  const { data: sprint, isLoading: sprintLoading } = useQuery({
    queryKey: ["sprint", id],
    queryFn: () => sprintService.obtener(id),
  });

  const { data: dashboard, isLoading: dashboardLoading } = useQuery({
    queryKey: ["planificacion", "dashboard", id],
    queryFn: () => planificacionService.obtenerDashboard(id),
  });

  const { data: tareasData, isLoading: tareasLoading } = useQuery({
    queryKey: ["tareas", id],
    queryFn: () => tareaService.listar(0, 500, { sprintId: id }),
  });

  const { data: bloqueosData, isLoading: bloqueosLoading } = useQuery({
    queryKey: ["bloqueos", id],
    queryFn: () => bloqueoService.listar(0, 100),
  });

  const isLoading =
    sprintLoading || dashboardLoading || tareasLoading || bloqueosLoading;

  const tareas = tareasData?.content ?? [];
  const bloqueos = bloqueosData?.content ?? [];

  const tareasCompletadas = tareas.filter((t) => t.estado === "COMPLETADA");
  const tareasPendientes = tareas.filter((t) => t.estado !== "COMPLETADA");
  const totalEstimado = tareas.reduce((acc, t) => acc + t.estimacion, 0);
  const horasCompletadas = tareasCompletadas.reduce(
    (acc, t) => acc + t.estimacion,
    0,
  );
  const pctCompletado =
    totalEstimado > 0
      ? Math.round((horasCompletadas / totalEstimado) * 100)
      : 0;

  // ── Imprimir PDF ──────────────────────────────────────────────────────────
  const handleImprimir = () => {
    window.print();
  };

  // ── Guard: sprint no cerrado ──────────────────────────────────────────────
  const sprintCerrado = sprint?.estado === "CERRADO";

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3 print:hidden">
        <div className="flex items-center gap-3">
          <Link
            to="/planificacion"
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            Planificación
          </Link>
          <span className="text-muted-foreground">/</span>
          <h1 className="text-xl font-bold text-foreground">
            Resumen{sprint ? ` — ${sprint.nombre}` : ""}
          </h1>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleImprimir}
            disabled={!sprintCerrado || isLoading}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md bg-primary text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            title={
              !sprintCerrado ? "Solo disponible para sprints cerrados" : ""
            }
          >
            <PrinterIcon className="h-4 w-4" />
            Imprimir / PDF
          </button>
          <Link
            to="/planificacion/timeline/$sprintId"
            params={{ sprintId }}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
          >
            <CalendarRange className="h-4 w-4" />
            Timeline
          </Link>
          <Link
            to="/planificacion/kanban/$sprintId"
            params={{ sprintId }}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
          >
            <Kanban className="h-4 w-4" />
            Kanban
          </Link>
          <Link
            to="/planificacion/dashboard/$sprintId"
            params={{ sprintId }}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
          >
            <LayoutDashboard className="h-4 w-4" />
            Dashboard
          </Link>
        </div>
      </div>

      {/* Aviso si no está cerrado */}
      {!isLoading && !sprintCerrado && (
        <div className="flex items-center gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 dark:border-amber-800 dark:bg-amber-900/20">
          <AlertTriangle className="h-5 w-5 shrink-0 text-amber-500" />
          <div>
            <p className="text-sm font-medium text-amber-800 dark:text-amber-300">
              Sprint no cerrado
            </p>
            <p className="text-sm text-amber-700 dark:text-amber-400">
              El resumen de cierre solo está disponible cuando el sprint tiene
              estado <strong>CERRADO</strong>. Estado actual:{" "}
              <strong>{sprint?.estado ?? "—"}</strong>.
            </p>
          </div>
        </div>
      )}

      {/* Contenido del resumen */}
      {isLoading ? (
        <ResumenSkeleton />
      ) : (
        <div ref={printRef} className="space-y-6">
          {/* ── Sección 1: Identificación ── */}
          <div className="rounded-lg border border-border bg-card p-6 print:border-0 print:p-0">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-foreground">
              <Lock className="h-5 w-5 text-muted-foreground" />
              Identificación del Sprint
            </h2>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  Sprint
                </p>
                <p className="mt-1 font-semibold text-foreground">
                  {sprint?.nombre ?? "—"}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  Squad
                </p>
                <p className="mt-1 font-semibold text-foreground">
                  {sprint?.squadNombre ?? "—"}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  Fechas
                </p>
                <p className="mt-1 text-sm text-foreground">
                  {sprint?.fechaInicio ?? "—"} → {sprint?.fechaFin ?? "—"}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  Estado
                </p>
                <span
                  className={clsx(
                    "mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-medium",
                    sprint?.estado === "CERRADO"
                      ? "bg-gray-200 text-gray-600 dark:bg-gray-700 dark:text-gray-400"
                      : sprint?.estado === "ACTIVO"
                        ? "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300"
                        : "bg-gray-100 text-gray-600",
                  )}
                >
                  {sprint?.estado ?? "—"}
                </span>
              </div>
              {sprint?.objetivo && (
                <div className="col-span-2 sm:col-span-4">
                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    Objetivo
                  </p>
                  <p className="mt-1 text-sm italic text-muted-foreground">
                    {sprint.objetivo}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* ── Sección 2: Métricas resumen ── */}
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            {[
              {
                label: "Tareas completadas",
                value: `${tareasCompletadas.length} / ${tareas.length}`,
                sub: `${pctCompletado}% del total`,
                icon: CheckCircle2,
                color: "text-emerald-500",
              },
              {
                label: "Horas completadas",
                value: `${horasCompletadas}h`,
                sub: `de ${totalEstimado}h estimadas`,
                icon: Clock,
                color: "text-blue-500",
              },
              {
                label: "Capacidad utilizada",
                value: dashboard
                  ? `${Math.round(dashboard.ocupacionPorcentaje)}%`
                  : "—",
                sub: `${dashboard?.capacidadAsignadaHoras ?? 0}h / ${dashboard?.capacidadTotalHoras ?? 0}h`,
                icon: Users,
                color:
                  (dashboard?.ocupacionPorcentaje ?? 0) > 100
                    ? "text-red-500"
                    : "text-blue-500",
              },
              {
                label: "Bloqueos registrados",
                value: bloqueos.length,
                sub: `${bloqueos.filter((b) => b.estado === "RESUELTO").length} resueltos`,
                icon: AlertTriangle,
                color: bloqueos.some((b) => b.estado === "ACTIVO")
                  ? "text-amber-500"
                  : "text-emerald-500",
              },
            ].map((m) => (
              <div
                key={m.label}
                className="rounded-lg border border-border bg-card p-4 shadow-sm"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">{m.label}</p>
                    <p className="mt-1 text-2xl font-bold text-foreground">
                      {m.value}
                    </p>
                    {m.sub && (
                      <p className="mt-0.5 text-xs text-muted-foreground">
                        {m.sub}
                      </p>
                    )}
                  </div>
                  <m.icon
                    className={clsx("h-8 w-8", m.color)}
                    aria-hidden="true"
                  />
                </div>
              </div>
            ))}
          </div>

          {/* ── Sección 3: Tareas completadas ── */}
          <div className="rounded-lg border border-border bg-card p-6">
            <h2 className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground">
              <CheckCircle2 className="h-5 w-5 text-emerald-500" />
              Tareas completadas
              <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300">
                {tareasCompletadas.length}
              </span>
            </h2>
            {tareasCompletadas.length === 0 ? (
              <p className="text-sm text-muted-foreground italic">
                Ninguna tarea completada.
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="pb-2 pr-4 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Tarea
                      </th>
                      <th className="pb-2 pr-4 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Responsable
                      </th>
                      <th className="pb-2 pr-4 text-right text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Estimación
                      </th>
                      <th className="pb-2 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Estado
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {tareasCompletadas.map((t) => (
                      <TareaRow key={t.id} tarea={t} />
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* ── Sección 4: Tareas pendientes / no completadas ── */}
          {tareasPendientes.length > 0 && (
            <div className="rounded-lg border border-border bg-card p-6">
              <h2 className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground">
                <Circle className="h-5 w-5 text-gray-400" />
                Tareas no completadas
                <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600 dark:bg-gray-800 dark:text-gray-400">
                  {tareasPendientes.length}
                </span>
              </h2>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="pb-2 pr-4 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Tarea
                      </th>
                      <th className="pb-2 pr-4 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Responsable
                      </th>
                      <th className="pb-2 pr-4 text-right text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Estimación
                      </th>
                      <th className="pb-2 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Estado
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {tareasPendientes.map((t) => (
                      <TareaRow key={t.id} tarea={t} />
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ── Sección 5: Bloqueos ── */}
          <div className="rounded-lg border border-border bg-card p-6">
            <h2 className="mb-4 flex items-center gap-2 text-base font-semibold text-foreground">
              <AlertTriangle className="h-5 w-5 text-amber-500" />
              Bloqueos registrados
              <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700 dark:bg-amber-900/30 dark:text-amber-300">
                {bloqueos.length}
              </span>
            </h2>
            {bloqueos.length === 0 ? (
              <p className="text-sm text-muted-foreground italic">
                No se registraron bloqueos en este sprint.
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="pb-2 pr-4 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Bloqueo
                      </th>
                      <th className="pb-2 pr-4 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Tipo
                      </th>
                      <th className="pb-2 pr-4 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Responsable
                      </th>
                      <th className="pb-2 pr-4 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Desde
                      </th>
                      <th className="pb-2 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        Estado
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {bloqueos.map((b) => (
                      <BloqueoRow key={b.id} bloqueo={b} />
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* ── Sección 6: Notas del LF ── */}
          <div className="rounded-lg border border-border bg-card p-6">
            <h2 className="mb-3 flex items-center gap-2 text-base font-semibold text-foreground">
              <Target className="h-5 w-5 text-blue-500" />
              Notas del Líder Funcional
            </h2>
            <p className="mb-2 text-xs text-muted-foreground">
              Observaciones, métricas adicionales, lecciones aprendidas y
              comentarios de cierre del sprint.
            </p>
            <textarea
              value={notasLF}
              onChange={(e) => setNotasLF(e.target.value)}
              rows={6}
              placeholder="Escribe aquí las observaciones del sprint: logros, dificultades, lecciones aprendidas, puntos de mejora..."
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary resize-y print:border-0 print:p-0 print:resize-none"
              aria-label="Notas del Líder Funcional"
            />
          </div>

          {/* Pie de página para impresión */}
          <div className="hidden print:block border-t border-gray-300 pt-4 text-center text-xs text-gray-400">
            Resumen generado el {new Date().toLocaleDateString("es-ES")} —
            Sprint: {sprint?.nombre ?? ""} — Squad: {sprint?.squadNombre ?? ""}
          </div>
        </div>
      )}
    </div>
  );
}
