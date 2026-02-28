/**
 * TaskCard — Tarjeta de tarea
 * Componente presentacional. No contiene lógica de negocio.
 * Usado en Timeline (compacto) y Kanban (estándar).
 */

import { ColaboradoresChip } from "@/components/jira/ColaboradoresChip";
import type {
  EstadoTarea,
  PrioridadTarea,
  TareaResponse,
  TipoTarea,
} from "@/types/api";
import { clsx } from "clsx";
import { AlertCircle, BookOpen, Bug, CheckSquare, Zap } from "lucide-react";
import type { FC } from "react";

interface Props {
  /** Datos de la tarea */
  tarea: TareaResponse;
  /** Variante: 'compact' para timeline, 'standard' para kanban */
  variant?: "compact" | "standard";
  /** Callback al hacer click en la tarjeta */
  onClick?: (tarea: TareaResponse) => void;
  /** Si es arrastrable (se controla externamente, aquí sólo estilo) */
  isDragging?: boolean;
  /** Co-desarrolladores adicionales (beyond the primary assignee) */
  colaboradores?: { nombre: string }[];
}

// ============= Mapas de colores y etiquetas =============

const tipoConfig: Record<
  TipoTarea,
  { icon: FC<{ className?: string }>; bg: string; text: string; label: string }
> = {
  HISTORIA: {
    icon: BookOpen,
    bg: "bg-violet-500",
    text: "text-white",
    label: "Historia",
  },
  TAREA: {
    icon: CheckSquare,
    bg: "bg-sky-500",
    text: "text-white",
    label: "Tarea",
  },
  BUG: {
    icon: Bug,
    bg: "bg-red-500",
    text: "text-white",
    label: "Bug",
  },
  SPIKE: {
    icon: Zap,
    bg: "bg-amber-500",
    text: "text-white",
    label: "Spike",
  },
};

const estadoBorderColor: Record<EstadoTarea, string> = {
  PENDIENTE: "border-l-gray-400",
  EN_PROGRESO: "border-l-blue-500",
  BLOQUEADO: "border-l-red-600",
  COMPLETADA: "border-l-emerald-500",
};

const estadoLabel: Record<EstadoTarea, string> = {
  PENDIENTE: "Pendiente",
  EN_PROGRESO: "En progreso",
  BLOQUEADO: "Bloqueado",
  COMPLETADA: "Completada",
};

const prioridadBadge: Record<PrioridadTarea, string> = {
  BAJA: "bg-gray-100 text-gray-600",
  NORMAL: "bg-blue-100 text-blue-700",
  ALTA: "bg-amber-100 text-amber-700",
  BLOQUEANTE: "bg-red-100 text-red-700",
};

const prioridadLabel: Record<PrioridadTarea, string> = {
  BAJA: "↓ Baja",
  NORMAL: "➡ Normal",
  ALTA: "↑ Alta",
  BLOQUEANTE: "🚫 Bloqueante",
};

/**
 * Tarjeta de tarea reutilizable.
 * variant='compact' → uso en timeline (ancho reducido, solo título + horas)
 * variant='standard' → uso en kanban (ficha completa con persona + prioridad)
 */
export const TaskCard: FC<Props> = ({
  tarea,
  variant = "standard",
  onClick,
  isDragging = false,
  colaboradores,
}) => {
  const tipo = tipoConfig[tarea.tipo as TipoTarea] ?? tipoConfig.TAREA;
  const TipoIcon = tipo.icon;

  if (variant === "compact") {
    return (
      <div
        role="button"
        tabIndex={0}
        aria-label={`Tarea: ${tarea.titulo}, ${tarea.estimacion}h`}
        onClick={() => onClick?.(tarea)}
        onKeyDown={(e) => e.key === "Enter" && onClick?.(tarea)}
        className={clsx(
          "flex cursor-pointer select-none items-center gap-1 rounded px-2 py-1 text-xs shadow-sm transition-shadow",
          tipo.bg,
          tipo.text,
          isDragging && "scale-95 shadow-lg opacity-80",
          tarea.bloqueada && "ring-2 ring-red-400",
        )}
      >
        <TipoIcon className="h-3 w-3 shrink-0" aria-hidden="true" />
        <span className="max-w-[80px] truncate font-medium">
          {tarea.titulo}
        </span>
        <span className="shrink-0 ml-auto opacity-90">{tarea.estimacion}h</span>
      </div>
    );
  }

  return (
    <div
      role="button"
      tabIndex={0}
      aria-label={`Tarea: ${tarea.titulo}, ${estadoLabel[tarea.estado as EstadoTarea] ?? tarea.estado}`}
      onClick={() => onClick?.(tarea)}
      onKeyDown={(e) => e.key === "Enter" && onClick?.(tarea)}
      className={clsx(
        "cursor-pointer select-none rounded-lg border border-gray-200 bg-white p-3 shadow-sm transition-shadow hover:shadow-md dark:border-gray-700 dark:bg-gray-800",
        "border-l-4",
        estadoBorderColor[tarea.estado as EstadoTarea] ?? "border-l-gray-300",
        isDragging && "scale-95 shadow-lg opacity-90",
        tarea.bloqueada && "ring-1 ring-red-400",
      )}
    >
      {/* Header: tipo + ID jira */}
      <div className="mb-1.5 flex items-center gap-1.5">
        <span
          className={clsx(
            "flex items-center gap-1 rounded px-1.5 py-0.5 text-xs font-medium",
            tipo.bg,
            tipo.text,
          )}
        >
          <TipoIcon className="h-3 w-3" aria-hidden="true" />
          {tipo.label}
        </span>
        {tarea.referenciaJira && (
          <span className="text-xs font-mono text-gray-400">
            {tarea.referenciaJira}
          </span>
        )}
        {tarea.bloqueada && (
          <AlertCircle
            className="ml-auto h-4 w-4 text-red-500"
            aria-label="Tarea bloqueada"
          />
        )}
      </div>

      {/* Título */}
      <p className="mb-2 text-sm font-medium text-gray-900 leading-snug dark:text-gray-100">
        {tarea.titulo}
      </p>

      {/* Footer: persona, estimación, prioridad */}
      <div className="flex items-center gap-2 flex-wrap">
        <span
          className={clsx(
            "rounded px-1.5 py-0.5 text-xs font-medium",
            prioridadBadge[tarea.prioridad as PrioridadTarea] ??
              "bg-gray-100 text-gray-600",
          )}
        >
          {prioridadLabel[tarea.prioridad as PrioridadTarea] ?? tarea.prioridad}
        </span>
        <span className="text-xs text-gray-500 dark:text-gray-400">
          {tarea.estimacion}h
        </span>
        {tarea.personaNombre && (
          <span
            className="ml-auto max-w-[100px] truncate text-xs text-gray-500 dark:text-gray-400"
            title={tarea.personaNombre}
          >
            {tarea.personaNombre}
          </span>
        )}
      </div>

      {/* Colaboradores adicionales */}
      {colaboradores && colaboradores.length > 0 && (
        <div className="mt-2 pt-2 border-t border-gray-100 dark:border-gray-700">
          <ColaboradoresChip
            personas={colaboradores}
            size={20}
            maxVisible={3}
          />
        </div>
      )}
    </div>
  );
};
