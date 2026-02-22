/**
 * DashboardWidgets — Widgets del dashboard de planificación
 * Métricas, alertas, gráficos de ocupación y estados.
 * Componente presentacional: recibe datos, sin lógica de negocio.
 */

import type { DashboardSprintResponse, TareaResponse } from "@/types/api";
import { clsx } from "clsx";
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  TrendingUp,
  Users,
} from "lucide-react";
import type { FC } from "react";
import {
  Bar,
  BarChart,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

// ============= MetricCard =============

interface MetricCardProps {
  label: string;
  value: string | number;
  sub?: string;
  icon: FC<{ className?: string }>;
  colorClass?: string;
}

const MetricCard: FC<MetricCardProps> = ({
  label,
  value,
  sub,
  icon: Icon,
  colorClass = "text-blue-500",
}) => (
  <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm dark:border-gray-700 dark:bg-gray-800">
    <div className="flex items-start justify-between">
      <div>
        <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
        <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-50">
          {value}
        </p>
        {sub && (
          <p className="mt-0.5 text-xs text-gray-400 dark:text-gray-500">
            {sub}
          </p>
        )}
      </div>
      <Icon className={clsx("h-8 w-8", colorClass)} aria-hidden="true" />
    </div>
  </div>
);

// ============= AlertBox =============

interface AlertBoxProps {
  alertas: string[];
}

const AlertBox: FC<AlertBoxProps> = ({ alertas }) => {
  if (alertas.length === 0) {
    return (
      <div className="flex items-center gap-2 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 dark:border-emerald-800 dark:bg-emerald-900/20">
        <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-500" />
        <p className="text-sm text-emerald-700 dark:text-emerald-300">
          Sin alertas. El sprint está en buen estado.
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 dark:border-amber-800 dark:bg-amber-900/20">
      <div className="mb-2 flex items-center gap-2">
        <AlertTriangle className="h-5 w-5 shrink-0 text-amber-500" />
        <h3 className="text-sm font-semibold text-amber-800 dark:text-amber-300">
          Alertas del sprint
        </h3>
      </div>
      <ul className="space-y-1" role="list" aria-label="Lista de alertas">
        {alertas.map((alerta, i) => (
          <li key={i} className="text-sm text-amber-700 dark:text-amber-300">
            • {alerta}
          </li>
        ))}
      </ul>
    </div>
  );
};

// ============= Colores estados tarea =============

const ESTADO_COLORS: Record<string, string> = {
  PENDIENTE: "#9CA3AF",
  EN_PROGRESO: "#3B82F6",
  BLOQUEADO: "#DC2626",
  COMPLETADA: "#10B981",
};

// ============= DashboardWidgets principal =============

interface Props {
  /** Datos del dashboard del sprint */
  dashboard: DashboardSprintResponse;
  /** Lista de tareas para los gráficos (opcional) */
  tareas?: TareaResponse[];
  /** Estado de carga */
  isLoading?: boolean;
}

/**
 * Widgets del dashboard: métricas principales, alertas y gráficos.
 */
export const DashboardWidgets: FC<Props> = ({
  dashboard,
  tareas = [],
  isLoading = false,
}) => {
  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          {[1, 2, 3, 4].map((i) => (
            <div
              key={i}
              className="h-28 animate-pulse rounded-lg bg-gray-100 dark:bg-gray-800"
            />
          ))}
        </div>
        <div className="h-16 animate-pulse rounded-lg bg-gray-100 dark:bg-gray-800" />
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div className="h-64 animate-pulse rounded-lg bg-gray-100 dark:bg-gray-800" />
          <div className="h-64 animate-pulse rounded-lg bg-gray-100 dark:bg-gray-800" />
        </div>
      </div>
    );
  }

  // Datos para gráfico de donut por estado
  const datosEstados = [
    {
      name: "Pendiente",
      value: dashboard.tareasPendientes,
      color: ESTADO_COLORS.PENDIENTE,
    },
    {
      name: "En progreso",
      value: dashboard.tareasEnProgreso,
      color: ESTADO_COLORS.EN_PROGRESO,
    },
    {
      name: "Bloqueadas",
      value: dashboard.tareasBloqueadas,
      color: ESTADO_COLORS.BLOQUEADO,
    },
    {
      name: "Completadas",
      value: dashboard.tareasCompletadas,
      color: ESTADO_COLORS.COMPLETADA,
    },
  ].filter((d) => d.value > 0);

  // Datos para gráfico de barras de ocupación por persona (desde lista de tareas)
  const ocupacionPersonaMap = new Map<string, number>();
  tareas.forEach((t) => {
    if (!t.personaNombre) return;
    const actual = ocupacionPersonaMap.get(t.personaNombre) ?? 0;
    ocupacionPersonaMap.set(t.personaNombre, actual + t.estimacion);
  });
  const datosOcupacion = Array.from(ocupacionPersonaMap.entries())
    .map(([nombre, horas]) => ({ nombre, horas }))
    .sort((a, b) => b.horas - a.horas)
    .slice(0, 10);

  const pctOcupacion = Math.round(dashboard.ocupacionPorcentaje);
  const pctOcupacionColor =
    pctOcupacion > 100
      ? "text-red-600"
      : pctOcupacion >= 80
        ? "text-amber-600"
        : "text-emerald-600";

  return (
    <div className="space-y-6">
      {/* Métricas principales */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <MetricCard
          label="Tareas totales"
          value={dashboard.tareasTotal}
          sub={`${dashboard.tareasCompletadas} completadas`}
          icon={CheckCircle2}
          colorClass="text-blue-500"
        />
        <MetricCard
          label="Ocupación"
          value={`${pctOcupacion}%`}
          sub={`${dashboard.capacidadAsignadaHoras}h / ${dashboard.capacidadTotalHoras}h`}
          icon={Users}
          colorClass={pctOcupacionColor}
        />
        <MetricCard
          label="Progreso real"
          value={`${Math.round(dashboard.progresoReal)}%`}
          sub={`Esperado: ${Math.round(dashboard.progresoEsperado)}%`}
          icon={TrendingUp}
          colorClass="text-violet-500"
        />
        <MetricCard
          label="Bloqueos activos"
          value={dashboard.bloqueosActivos}
          sub={`${dashboard.tareasBloqueadas} tareas bloqueadas`}
          icon={Clock}
          colorClass={
            dashboard.bloqueosActivos > 0 ? "text-red-500" : "text-gray-400"
          }
        />
      </div>

      {/* Alertas */}
      <AlertBox alertas={dashboard.alertas ?? []} />

      {/* Gráficos */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Donut: tareas por estado */}
        <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm dark:border-gray-700 dark:bg-gray-800">
          <h3 className="mb-3 text-sm font-semibold text-gray-700 dark:text-gray-200">
            Tareas por estado
          </h3>
          {datosEstados.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-400">Sin tareas</p>
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie
                  data={datosEstados}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  innerRadius={55}
                  outerRadius={85}
                  paddingAngle={3}
                  aria-label="Gráfico de tareas por estado"
                >
                  {datosEstados.map((entry, index) => (
                    <Cell key={index} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(
                    val: number | undefined,
                    name: string | undefined,
                  ) => [`${val ?? 0} tareas`, name ?? ""]}
                />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Barras: horas asignadas por persona */}
        <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm dark:border-gray-700 dark:bg-gray-800">
          <h3 className="mb-3 text-sm font-semibold text-gray-700 dark:text-gray-200">
            Horas asignadas por persona
          </h3>
          {datosOcupacion.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-400">
              Sin tareas asignadas
            </p>
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart
                data={datosOcupacion}
                layout="vertical"
                margin={{ left: 8, right: 16 }}
                aria-label="Gráfico de horas asignadas por persona"
              >
                <XAxis type="number" tick={{ fontSize: 11 }} />
                <YAxis
                  type="category"
                  dataKey="nombre"
                  tick={{ fontSize: 11 }}
                  width={80}
                />
                <Tooltip
                  formatter={(val: number | undefined) => [
                    `${val ?? 0}h`,
                    "Horas",
                  ]}
                />
                <Bar dataKey="horas" radius={[0, 4, 4, 0]}>
                  {datosOcupacion.map((entry, index) => {
                    const capacidad =
                      dashboard.capacidadTotalHoras /
                      Math.max(datosOcupacion.length, 1);
                    const pct =
                      capacidad > 0 ? (entry.horas / capacidad) * 100 : 0;
                    const color =
                      pct > 100 ? "#DC2626" : pct >= 80 ? "#F59E0B" : "#3B82F6";
                    return <Cell key={index} fill={color} />;
                  })}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  );
};
