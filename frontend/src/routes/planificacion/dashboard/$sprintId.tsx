/**
 * Página Dashboard del Sprint
 * Métricas, alertas y gráficos de capacidad del sprint.
 */

import { DashboardWidgets } from "@/features/planificacion";
import { planificacionService } from "@/services/planificacionService";
import { sprintService } from "@/services/sprintService";
import { tareaService } from "@/services/tareaService";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { clsx } from "clsx";
import {
  ArrowLeft,
  CalendarRange,
  Clock,
  FileText,
  Kanban,
  RefreshCw,
  Target,
} from "lucide-react";

export const Route = createFileRoute("/planificacion/dashboard/$sprintId")({
  component: DashboardPage,
});

function DashboardPage() {
  const { sprintId } = Route.useParams();
  const id = Number(sprintId);

  // ── Queries ───────────────────────────────────────────────────────────────
  const { data: sprint, isLoading: sprintLoading } = useQuery({
    queryKey: ["sprint", id],
    queryFn: () => sprintService.obtener(id),
  });

  const {
    data: dashboard,
    isLoading: dashboardLoading,
    refetch,
  } = useQuery({
    queryKey: ["planificacion", "dashboard", id],
    queryFn: () => planificacionService.obtenerDashboard(id),
  });

  const { data: tareasData } = useQuery({
    queryKey: ["tareas", id],
    queryFn: () => tareaService.listar(0, 200, { sprintId: id }),
  });
  const tareas = tareasData?.content ?? [];

  const isLoading = sprintLoading || dashboardLoading;

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
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
            Dashboard{sprint ? ` — ${sprint.nombre}` : ""}
          </h1>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => refetch()}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
          >
            <RefreshCw className="h-4 w-4" />
            Actualizar
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
          {sprint?.estado === "CERRADO" && (
            <Link
              to="/planificacion/resumen/$sprintId"
              params={{ sprintId }}
              className={clsx(
                "flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border transition-colors",
                "border-emerald-300 bg-emerald-50 text-emerald-700 hover:bg-emerald-100",
                "dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-300 dark:hover:bg-emerald-900/40",
              )}
            >
              <FileText className="h-4 w-4" />
              Resumen
            </Link>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Panel izquierdo — Info del sprint */}
        <div className="lg:col-span-1 space-y-4">
          <div className="bg-card border border-border rounded-lg p-4 space-y-3">
            <h2 className="font-semibold text-foreground text-sm uppercase tracking-wide text-muted-foreground">
              Detalles del Sprint
            </h2>

            {sprint ? (
              <>
                <div>
                  <p className="text-lg font-bold text-foreground">
                    {sprint.nombre}
                  </p>
                  <span
                    className={`inline-block mt-1 text-xs px-2 py-0.5 rounded-full font-medium ${
                      sprint.estado === "ACTIVO"
                        ? "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300"
                        : sprint.estado === "CERRADO"
                          ? "bg-gray-200 text-gray-600 dark:bg-gray-700 dark:text-gray-400"
                          : "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400"
                    }`}
                  >
                    {sprint.estado}
                  </span>
                </div>

                {sprint.fechaInicio && sprint.fechaFin && (
                  <div className="flex items-start gap-2 text-sm text-muted-foreground">
                    <Clock className="h-4 w-4 mt-0.5 shrink-0" />
                    <div>
                      <p>{sprint.fechaInicio}</p>
                      <p>→ {sprint.fechaFin}</p>
                    </div>
                  </div>
                )}

                {sprint.objetivo && (
                  <div className="flex items-start gap-2 text-sm text-muted-foreground">
                    <Target className="h-4 w-4 mt-0.5 shrink-0" />
                    <p className="italic">{sprint.objetivo}</p>
                  </div>
                )}
              </>
            ) : (
              <div className="h-20 animate-pulse bg-muted rounded-md" />
            )}
          </div>

          {/* Resumen rápido tareas */}
          <div className="bg-card border border-border rounded-lg p-4 space-y-2">
            <h2 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Tareas
            </h2>
            {[
              {
                label: "Pendiente",
                count: tareas.filter((t) => t.estado === "PENDIENTE").length,
                color: "bg-gray-400",
              },
              {
                label: "En progreso",
                count: tareas.filter((t) => t.estado === "EN_PROGRESO").length,
                color: "bg-blue-500",
              },
              {
                label: "Bloqueado",
                count: tareas.filter((t) => t.estado === "BLOQUEADO").length,
                color: "bg-red-500",
              },
              {
                label: "Completada",
                count: tareas.filter((t) => t.estado === "COMPLETADA").length,
                color: "bg-green-500",
              },
            ].map((row) => (
              <div
                key={row.label}
                className="flex items-center justify-between text-sm"
              >
                <div className="flex items-center gap-2">
                  <span className={`h-2 w-2 rounded-full ${row.color}`} />
                  <span className="text-muted-foreground">{row.label}</span>
                </div>
                <span className="font-medium text-foreground">{row.count}</span>
              </div>
            ))}
            <div className="pt-1 border-t border-border flex items-center justify-between text-sm font-semibold">
              <span className="text-foreground">Total</span>
              <span className="text-foreground">{tareas.length}</span>
            </div>
          </div>
        </div>

        {/* Panel derecho — Widgets */}
        <div className="lg:col-span-3">
          {dashboard ? (
            <DashboardWidgets
              dashboard={dashboard}
              tareas={tareas}
              isLoading={isLoading}
            />
          ) : isLoading ? (
            <DashboardWidgets
              dashboard={{
                sprintId: id,
                sprintNombre: "",
                estado: "PLANIFICACION",
                tareasTotal: 0,
                tareasPendientes: 0,
                tareasEnProgreso: 0,
                tareasCompletadas: 0,
                tareasBloqueadas: 0,
                progresoEsperado: 0,
                progresoReal: 0,
                capacidadTotalHoras: 0,
                capacidadAsignadaHoras: 0,
                ocupacionPorcentaje: 0,
                bloqueosActivos: 0,
                alertas: [],
                fechaInicio: "",
                fechaFin: "",
              }}
              isLoading={true}
            />
          ) : (
            <div className="h-64 flex items-center justify-center text-muted-foreground bg-card border border-border rounded-lg">
              No hay datos del dashboard disponibles.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
