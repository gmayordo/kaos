import { useDocumentTitle } from "@/lib/useDocumentTitle";
import { jiraSyncService } from "@/services/jiraService";
import { perfilHorarioService } from "@/services/perfilHorarioService";
import { personaService } from "@/services/personaService";
import { planificacionService } from "@/services/planificacionService";
import { sprintService } from "@/services/sprintService";
import { squadService } from "@/services/squadService";
import type { SprintResponse } from "@/types/api";
import type { EstadoSync, JiraSyncStatusResponse } from "@/types/jira";
import { useQueries, useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  ArrowRight,
  Briefcase,
  Calendar,
  CheckCircle2,
  Clock,
  GitBranch,
  LayoutDashboard,
  RefreshCw,
  Settings,
  TrendingUp,
  Users,
  XCircle,
  Zap,
} from "lucide-react";
import { useEffect, useState } from "react";

export const Route = createFileRoute("/")({
  component: IndexPage,
});

// ─── Helpers ──────────────────────────────────────────────────────────────────

function estadoBadge(estado: EstadoSync) {
  switch (estado) {
    case "IDLE":
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-green-500/15 text-green-600 border border-green-500/30">
          <CheckCircle2 className="h-3 w-3" />
          Ok
        </span>
      );
    case "SINCRONIZANDO":
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-blue-500/15 text-blue-600 border border-blue-500/30">
          <RefreshCw className="h-3 w-3 animate-spin" />
          Syncing
        </span>
      );
    case "CUOTA_AGOTADA":
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-amber-500/15 text-amber-600 border border-amber-500/30">
          <AlertTriangle className="h-3 w-3" />
          Cuota
        </span>
      );
    case "ERROR":
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-red-500/15 text-red-600 border border-red-500/30">
          <XCircle className="h-3 w-3" />
          Error
        </span>
      );
  }
}

function formatRelative(iso: string | null): string {
  if (!iso) return "Nunca";
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 1) return "Ahora";
  if (mins < 60) return `Hace ${mins}m`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `Hace ${hours}h`;
  return `Hace ${Math.floor(hours / 24)}d`;
}

// ─── Main component ────────────────────────────────────────────────────────────

function IndexPage() {
  useDocumentTitle("Dashboard");

  // Query para squads
  const { data: squadsData, isLoading: isLoadingSquads } = useQuery({
    queryKey: ["squads-dashboard"],
    queryFn: () => squadService.listar(0, 100),
  });

  const squads = Array.isArray(squadsData)
    ? squadsData
    : (squadsData?.content ?? []);

  const [selectedSquadId, setSelectedSquadId] = useState<number | "">("");

  useEffect(() => {
    if (selectedSquadId === "" && squads.length > 0) {
      setSelectedSquadId(squads[0].id);
    }
  }, [selectedSquadId, squads]);

  // Query para personas
  const { data: personasData, isLoading: isLoadingPersonas } = useQuery({
    queryKey: ["personas-dashboard"],
    queryFn: () => personaService.listar(0, 100),
  });

  // Query para perfiles horario
  const { data: perfilesData, isLoading: isLoadingPerfiles } = useQuery({
    queryKey: ["perfiles-horario-dashboard"],
    queryFn: () => perfilHorarioService.listar(0, 100),
  });

  const squadsCount = squads.length;
  const personasCount = personasData?.content?.length ?? 0;
  const perfilesCount = perfilesData?.content?.length ?? 0;

  // Query sprint activo
  const { data: sprintActivoData, isLoading: isLoadingSprintActivo } = useQuery(
    {
      queryKey: ["sprint-activo", selectedSquadId],
      queryFn: () =>
        sprintService.listar(0, 1, {
          squadId: Number(selectedSquadId),
          estado: "ACTIVO",
        }),
      enabled: selectedSquadId !== "",
    },
  );

  const sprintActivo = sprintActivoData?.content?.[0] ?? null;

  const { data: dashboardActivo, isLoading: isLoadingDashboardActivo } =
    useQuery({
      queryKey: ["dashboard-sprint-activo", sprintActivo?.id],
      queryFn: () => planificacionService.obtenerDashboard(sprintActivo!.id),
      enabled: !!sprintActivo,
    });

  // Jira sync status — uno por squad
  const jiraQueries = useQueries({
    queries: squads.map((squad) => ({
      queryKey: ["jira-sync-status", squad.id],
      queryFn: () => jiraSyncService.obtenerEstado(squad.id),
      staleTime: 30_000,
      retry: false,
    })),
  });

  const jiraStatuses: (JiraSyncStatusResponse | undefined)[] = jiraQueries.map(
    (q) => q.data,
  );
  const jiraLoading = jiraQueries.some((q) => q.isLoading);

  return (
    <div className="space-y-8">
      {/* ── Hero ───────────────────────────────────────────────── */}
      <div className="rounded-2xl bg-gradient-to-br from-primary/10 via-primary/5 to-transparent border border-primary/20 px-8 py-7">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-semibold uppercase tracking-widest text-primary/70">
                Panel de control
              </span>
            </div>
            <h1 className="text-3xl font-bold tracking-tight">
              Bienvenido a <span className="text-primary">KAOS</span>
            </h1>
            <p className="text-muted-foreground mt-1 max-w-lg">
              Gestión de equipos, sprints y capacidad. Planifica, sincroniza con
              Jira y monitoriza el avance de tus squads en tiempo real.
            </p>
          </div>

          {/* KPI pills */}
          <div className="flex flex-wrap gap-3 md:flex-col md:items-end">
            <KpiPill
              icon={Briefcase}
              label="Squads"
              value={squadsCount}
              loading={isLoadingSquads}
              color="blue"
            />
            <KpiPill
              icon={Users}
              label="Personas"
              value={personasCount}
              loading={isLoadingPersonas}
              color="violet"
            />
            <KpiPill
              icon={Settings}
              label="Perfiles"
              value={perfilesCount}
              loading={isLoadingPerfiles}
              color="slate"
            />
          </div>
        </div>
      </div>

      {/* ── Sprint activo ─────────────────────────────────────── */}
      <section>
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold flex items-center gap-2">
            <LayoutDashboard
              className="h-4 w-4 text-primary"
              aria-hidden="true"
            />
            Sprint activo
          </h2>
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <label htmlFor="dashboard-squad" className="sr-only">
              Seleccionar squad
            </label>
            <span aria-hidden="true">Squad:</span>
            <select
              id="dashboard-squad"
              value={selectedSquadId}
              onChange={(e) =>
                setSelectedSquadId(e.target.value ? Number(e.target.value) : "")
              }
              className="px-3 py-1 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              {squads.map((squad) => (
                <option key={squad.id} value={squad.id}>
                  {squad.nombre}
                </option>
              ))}
            </select>
          </div>
        </div>

        {isLoadingSprintActivo ? (
          <div
            className="rounded-xl border border-border bg-card p-6 animate-pulse"
            role="status"
          >
            <span className="sr-only">Cargando sprint activo...</span>
            <div className="h-4 bg-muted rounded w-1/4 mb-3" />
            <div className="h-3 bg-muted rounded w-1/2 mb-2" />
            <div className="h-2 bg-muted rounded w-full" />
          </div>
        ) : sprintActivo ? (
          <SprintActivoCard
            sprint={sprintActivo}
            progresoReal={dashboardActivo?.progresoReal ?? null}
            progresoEsperado={dashboardActivo?.progresoEsperado ?? null}
            ocupacion={dashboardActivo?.ocupacionPorcentaje ?? null}
            isLoadingDashboard={isLoadingDashboardActivo}
          />
        ) : (
          <div className="rounded-xl border border-dashed border-border bg-card/50 p-6 text-center text-sm text-muted-foreground">
            No hay sprint activo para este squad.
          </div>
        )}
      </section>

      {/* ── Jira overview ─────────────────────────────────────── */}
      <section>
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold flex items-center gap-2">
            <GitBranch className="h-4 w-4 text-primary" aria-hidden="true" />
            Estado Jira
          </h2>
          <Link
            to="/jira"
            className="text-xs text-primary hover:underline flex items-center gap-1"
          >
            Gestionar sincronización{" "}
            <ArrowRight className="h-3 w-3" aria-hidden="true" />
          </Link>
        </div>

        {jiraLoading && squads.length === 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4" role="status">
            <span className="sr-only">Cargando estado Jira...</span>
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                className="rounded-xl border border-border bg-card p-4 animate-pulse h-28"
              />
            ))}
          </div>
        ) : squads.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {squads.map((squad, i) => {
              const status = jiraStatuses[i];
              const loadingStatus = jiraQueries[i]?.isLoading;
              return (
                <JiraSquadCard
                  key={squad.id}
                  squadId={squad.id}
                  squadNombre={squad.nombre}
                  status={status}
                  loading={loadingStatus}
                />
              );
            })}
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">
            No hay squads configurados.
          </p>
        )}
      </section>

      {/* ── Accesos rápidos ───────────────────────────────────── */}
      <section>
        <h2 className="text-lg font-semibold mb-3 flex items-center gap-2">
          <Zap className="h-4 w-4 text-primary" aria-hidden="true" />
          Accesos rápidos
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <QuickCard
            icon={Briefcase}
            title="Squads"
            description="Gestiona equipos y miembros"
            href="/squads"
            count={squadsCount}
            color="blue"
          />
          <QuickCard
            icon={Users}
            title="Personas"
            description="Administra roles y dedicación"
            href="/personas"
            count={personasCount}
            color="violet"
          />
          <QuickCard
            icon={Calendar}
            title="Calendario"
            description="Vacaciones y ausencias"
            href="/calendario"
            color="emerald"
          />
          <QuickCard
            icon={TrendingUp}
            title="Planificación"
            description="Sprints, tareas y timeline"
            href="/planificacion"
            color="orange"
          />
        </div>
      </section>
    </div>
  );
}

// ─── Sub-components ──────────────────────────────────────────────────────────

interface KpiPillProps {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: number;
  loading: boolean;
  color: "blue" | "violet" | "slate";
}

const colorMap: Record<
  KpiPillProps["color"],
  { bg: string; text: string; border: string }
> = {
  blue: {
    bg: "bg-blue-500/10",
    text: "text-blue-600",
    border: "border-blue-500/20",
  },
  violet: {
    bg: "bg-violet-500/10",
    text: "text-violet-600",
    border: "border-violet-500/20",
  },
  slate: {
    bg: "bg-slate-500/10",
    text: "text-slate-600",
    border: "border-slate-500/20",
  },
};

function KpiPill({ icon: Icon, label, value, loading, color }: KpiPillProps) {
  const c = colorMap[color];
  return (
    <div
      className={`flex items-center gap-2 px-3 py-1.5 rounded-full border ${c.bg} ${c.border}`}
      role="status"
      aria-label={`${label}: ${loading ? "cargando" : value}`}
    >
      <Icon className={`h-4 w-4 ${c.text}`} aria-hidden="true" />
      <span className="text-sm font-medium text-foreground">
        {loading ? "–" : value}
      </span>
      <span className="text-xs text-muted-foreground">{label}</span>
    </div>
  );
}

// ─── SprintActivoCard ──────────────────────────────────────────────────────────

interface SprintActivoCardProps {
  sprint: SprintResponse;
  progresoReal: number | null;
  progresoEsperado: number | null;
  ocupacion: number | null;
  isLoadingDashboard: boolean;
}

function SprintActivoCard({
  sprint,
  progresoReal,
  progresoEsperado,
  ocupacion,
  isLoadingDashboard,
}: SprintActivoCardProps) {
  const total =
    (sprint.tareasPendientes ?? 0) +
    (sprint.tareasEnProgreso ?? 0) +
    (sprint.tareasCompletadas ?? 0);

  const realVsEsperado =
    progresoReal !== null && progresoEsperado !== null
      ? progresoReal >= progresoEsperado
        ? "ahead"
        : "behind"
      : null;

  return (
    <div className="rounded-xl border border-border bg-card p-6 space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2 mb-0.5">
            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-500/15 text-green-600 border border-green-500/30">
              ACTIVO
            </span>
            {sprint.capacidadTotal !== null && (
              <span className="text-xs text-muted-foreground flex items-center gap-1">
                <Clock className="h-3 w-3" aria-hidden="true" />{" "}
                {sprint.capacidadTotal}h cap.
              </span>
            )}
          </div>
          <h3 className="text-base font-semibold text-foreground mt-1">
            {sprint.nombre}
          </h3>
          <p className="text-xs text-muted-foreground">
            {sprint.fechaInicio} → {sprint.fechaFin}
          </p>
        </div>
        <Link
          to="/planificacion"
          className="flex-shrink-0 text-xs text-primary hover:underline flex items-center gap-1"
        >
          Ver sprint <ArrowRight className="h-3 w-3" aria-hidden="true" />
        </Link>
      </div>

      {/* Tareas segmentadas */}
      <div>
        <div className="flex justify-between text-xs text-muted-foreground mb-1.5">
          <span>Tareas completadas</span>
          <span className="font-medium text-foreground">
            {sprint.tareasCompletadas ?? 0}/{total}
          </span>
        </div>
        <div
          className="h-2.5 bg-muted rounded-full overflow-hidden flex"
          role="progressbar"
          aria-valuenow={sprint.tareasCompletadas ?? 0}
          aria-valuemin={0}
          aria-valuemax={total}
          aria-label={`Tareas completadas: ${sprint.tareasCompletadas ?? 0} de ${total}`}
        >
          {total > 0 && (
            <>
              <div
                className="h-full bg-green-500 transition-all"
                style={{
                  width: `${Math.round(((sprint.tareasCompletadas ?? 0) / total) * 100)}%`,
                }}
              />
              <div
                className="h-full bg-blue-500 transition-all"
                style={{
                  width: `${Math.round(((sprint.tareasEnProgreso ?? 0) / total) * 100)}%`,
                }}
              />
            </>
          )}
        </div>
        <div className="flex gap-3 mt-1.5 text-xs">
          <span className="flex items-center gap-1 text-muted-foreground">
            <span className="h-2 w-2 rounded-full bg-green-500 inline-block" />
            Completadas: {sprint.tareasCompletadas ?? 0}
          </span>
          <span className="flex items-center gap-1 text-muted-foreground">
            <span className="h-2 w-2 rounded-full bg-blue-500 inline-block" />
            En progreso: {sprint.tareasEnProgreso ?? 0}
          </span>
          <span className="flex items-center gap-1 text-muted-foreground">
            <span className="h-2 w-2 rounded-full bg-muted-foreground/30 inline-block" />
            Pendientes: {sprint.tareasPendientes ?? 0}
          </span>
        </div>
      </div>

      {/* Dashboard metrics */}
      {isLoadingDashboard ? (
        <div className="grid grid-cols-3 gap-3 animate-pulse">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-16 bg-muted rounded-lg" />
          ))}
        </div>
      ) : progresoReal !== null ? (
        <div className="grid grid-cols-3 gap-3">
          <ProgressMetric
            label="Avance real"
            value={progresoReal}
            color={
              realVsEsperado === "ahead"
                ? "green"
                : realVsEsperado === "behind"
                  ? "red"
                  : "blue"
            }
          />
          <ProgressMetric
            label="Avance esperado"
            value={progresoEsperado ?? 0}
            color="blue"
          />
          <ProgressMetric
            label="Ocupación"
            value={ocupacion ?? 0}
            color={
              (ocupacion ?? 0) > 90
                ? "red"
                : (ocupacion ?? 0) > 70
                  ? "amber"
                  : "green"
            }
          />
        </div>
      ) : null}
    </div>
  );
}

type MetricColor = "green" | "blue" | "red" | "amber";
const metricColorMap: Record<MetricColor, { bar: string; text: string }> = {
  green: { bar: "bg-green-500", text: "text-green-600" },
  blue: { bar: "bg-blue-500", text: "text-blue-600" },
  red: { bar: "bg-red-500", text: "text-red-600" },
  amber: { bar: "bg-amber-500", text: "text-amber-600" },
};

function ProgressMetric({
  label,
  value,
  color,
}: {
  label: string;
  value: number;
  color: MetricColor;
}) {
  const mc = metricColorMap[color];
  return (
    <div className="bg-muted/40 rounded-lg p-3 space-y-1.5">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={`text-xl font-bold ${mc.text}`}>{value}%</p>
      <div className="h-1.5 bg-muted rounded-full overflow-hidden">
        <div
          className={`h-full ${mc.bar} rounded-full transition-all`}
          role="progressbar"
          aria-valuenow={value}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={`${label}: ${value}%`}
          style={{ width: `${Math.min(value, 100)}%` }}
        />
      </div>
    </div>
  );
}

// ─── JiraSquadCard ─────────────────────────────────────────────────────────────

interface JiraSquadCardProps {
  squadId: number;
  squadNombre: string;
  status: JiraSyncStatusResponse | undefined;
  loading: boolean | undefined;
}

function JiraSquadCard({ squadNombre, status, loading }: JiraSquadCardProps) {
  if (loading) {
    return (
      <div className="rounded-xl border border-border bg-card p-4 animate-pulse h-28" />
    );
  }

  if (!status) {
    return (
      <div className="rounded-xl border border-dashed border-border bg-card/50 p-4 flex items-center justify-center text-xs text-muted-foreground">
        Sin config Jira — {squadNombre}
      </div>
    );
  }

  const cuotaPct = Math.round((status.callsConsumidas2h / 200) * 100);

  return (
    <div className="rounded-xl border border-border bg-card p-4 space-y-3 hover:shadow-sm transition-shadow">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-sm font-semibold text-foreground">
            {status.squadNombre}
          </p>
          <p className="text-xs text-muted-foreground">
            {formatRelative(status.ultimaSync)}
          </p>
        </div>
        {estadoBadge(status.estado)}
      </div>

      <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
        <span>
          <span className="text-foreground font-medium">
            {status.issuesImportadas.toLocaleString()}
          </span>{" "}
          issues
        </span>
        <span>
          <span className="text-foreground font-medium">
            {status.worklogsImportados.toLocaleString()}
          </span>{" "}
          worklogs
        </span>
      </div>

      {/* Cuota mini bar */}
      <div>
        <div
          className="h-1.5 bg-muted rounded-full overflow-hidden"
          role="progressbar"
          aria-valuenow={status.callsConsumidas2h}
          aria-valuemin={0}
          aria-valuemax={200}
          aria-label={`Cuota API: ${status.callsConsumidas2h} de 200`}
        >
          <div
            className={`h-full rounded-full transition-all ${
              cuotaPct >= 90
                ? "bg-red-500"
                : cuotaPct >= 70
                  ? "bg-amber-500"
                  : "bg-green-500"
            }`}
            style={{ width: `${Math.min(cuotaPct, 100)}%` }}
          />
        </div>
        <p className="text-xs text-muted-foreground mt-0.5">
          Cuota API: {status.callsConsumidas2h}/200
        </p>
      </div>

      {status.ultimoError && (
        <p
          className="text-xs text-destructive truncate"
          title={status.ultimoError}
        >
          {status.ultimoError}
        </p>
      )}
    </div>
  );
}

// ─── QuickCard ─────────────────────────────────────────────────────────────────

type QuickColor = "blue" | "violet" | "emerald" | "orange";

const quickColorMap: Record<
  QuickColor,
  { iconBg: string; iconText: string; hover: string }
> = {
  blue: {
    iconBg: "bg-blue-500/10",
    iconText: "text-blue-600",
    hover: "hover:border-blue-500/40",
  },
  violet: {
    iconBg: "bg-violet-500/10",
    iconText: "text-violet-600",
    hover: "hover:border-violet-500/40",
  },
  emerald: {
    iconBg: "bg-emerald-500/10",
    iconText: "text-emerald-600",
    hover: "hover:border-emerald-500/40",
  },
  orange: {
    iconBg: "bg-orange-500/10",
    iconText: "text-orange-600",
    hover: "hover:border-orange-500/40",
  },
};

interface QuickCardProps {
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  description: string;
  href: string;
  count?: number;
  color: QuickColor;
}

function QuickCard({
  icon: Icon,
  title,
  description,
  href,
  count,
  color,
}: QuickCardProps) {
  const qc = quickColorMap[color];
  return (
    <Link
      to={href}
      className={`group block p-5 rounded-xl border border-border bg-card hover:bg-accent/30 hover:shadow-md transition-all ${qc.hover}`}
    >
      <div className="flex items-start justify-between mb-3">
        <div className={`p-2 rounded-lg ${qc.iconBg}`}>
          <Icon className={`h-5 w-5 ${qc.iconText}`} aria-hidden="true" />
        </div>
        {count !== undefined && (
          <span className="text-2xl font-bold text-foreground">{count}</span>
        )}
      </div>
      <h3 className="font-semibold text-sm text-foreground group-hover:text-primary transition-colors">
        {title}
      </h3>
      <p className="text-xs text-muted-foreground mt-0.5">{description}</p>
      <div className="flex items-center gap-1 mt-3 text-xs text-muted-foreground group-hover:text-primary transition-colors">
        <span>Ir a {title.toLowerCase()}</span>
        <ArrowRight
          className="h-3 w-3 group-hover:translate-x-0.5 transition-transform"
          aria-hidden="true"
        />
      </div>
    </Link>
  );
}
