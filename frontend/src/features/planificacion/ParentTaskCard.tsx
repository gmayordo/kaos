/**
 * ParentTaskCard — Card expandible para issues padre Jira con sus subtareas.
 * Muestra progreso (completadas/total) y lista las subtareas anidadas como Draggables.
 * Componente presentacional. No contiene lógica de negocio.
 */

import { jiraIssueUrl } from "@/lib/jira";
import type { EstadoTarea, TareaResponse } from "@/types/api";
import { Draggable } from "@hello-pangea/dnd";
import { clsx } from "clsx";
import { BookOpen, CheckSquare, ChevronDown, ChevronRight } from "lucide-react";
import { type FC, useState } from "react";

interface Props {
  /** Issue padre */
  tarea: TareaResponse;
  /** Subtareas hijas */
  subtareas: TareaResponse[];
  /** Callback al hacer click en cualquier tarea */
  onClickTarea: (tarea: TareaResponse) => void;
  /** Índice del padre en el droppable (para posicionamiento) */
  draggableIndex: number;
  /** Estado calculado a partir de las subtareas (sobreescribe tarea.estado al abrir modal) */
  estadoCalculado: EstadoTarea;
}

const estadoSubtareaStyle: Record<
  EstadoTarea,
  { dot: string; label: string; border: string; bg: string; badge: string }
> = {
  PENDIENTE: {
    dot: "bg-gray-400",
    label: "Pendiente",
    border: "border-l-gray-300 dark:border-l-gray-500",
    bg: "bg-gray-50 dark:bg-gray-700/40",
    badge: "bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300",
  },
  EN_PROGRESO: {
    dot: "bg-blue-500",
    label: "En progreso",
    border: "border-l-blue-400",
    bg: "bg-blue-50/60 dark:bg-blue-900/20",
    badge: "bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300",
  },
  BLOQUEADO: {
    dot: "bg-red-500",
    label: "Bloqueado",
    border: "border-l-red-400",
    bg: "bg-red-50/60 dark:bg-red-900/20",
    badge: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300",
  },
  COMPLETADA: {
    dot: "bg-emerald-500",
    label: "Completada",
    border: "border-l-emerald-400",
    bg: "bg-emerald-50/60 dark:bg-emerald-900/20",
    badge:
      "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300",
  },
};

/**
 * Card de issue padre con barra de progreso y subtareas expandibles.
 * El padre NO es Draggable — solo sus subtareas lo son.
 */
export const ParentTaskCard: FC<Props> = ({
  tarea,
  subtareas,
  onClickTarea,
  draggableIndex,
  estadoCalculado,
}) => {
  const completadas = subtareas.filter((s) => s.estado === "COMPLETADA").length;
  const total = subtareas.length;
  const porcentaje = total > 0 ? Math.round((completadas / total) * 100) : 0;

  // Siempre minimizado por defecto
  const [expanded, setExpanded] = useState(false);

  return (
    <div
      className="rounded-lg border border-gray-200 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-800"
      aria-label={`Issue padre: ${tarea.jiraIssueKey ?? tarea.titulo}`}
    >
      {/* ── Cabecera del padre ─────────────────────────────────────── */}
      <div
        role="button"
        tabIndex={0}
        onClick={() => onClickTarea({ ...tarea, estado: estadoCalculado })}
        onKeyDown={(e) => e.key === "Enter" && onClickTarea({ ...tarea, estado: estadoCalculado })}
        className="cursor-pointer select-none p-3 hover:bg-gray-50 dark:hover:bg-gray-700/50 rounded-t-lg transition-colors"
      >
        <div className="flex items-start gap-1.5 mb-2">
          <span className="flex items-center gap-1 rounded px-1.5 py-0.5 text-xs font-medium bg-violet-500 text-white shrink-0">
            <BookOpen className="h-3 w-3" aria-hidden="true" />
            Historia
          </span>
          {tarea.jiraIssueKey && (
            <a
              href={jiraIssueUrl(tarea.jiraIssueKey!)}
              target="_blank"
              rel="noopener noreferrer"
              onClick={(e) => e.stopPropagation()}
              className="text-xs font-mono text-blue-500 hover:text-blue-700 hover:underline transition-colors truncate"
            >
              {tarea.jiraIssueKey}
            </a>
          )}
        </div>

        <p className="text-sm font-medium text-gray-900 dark:text-gray-100 leading-snug mb-2 line-clamp-2">
          {tarea.titulo}
        </p>

        {/* Barra de progreso */}
        <div className="flex items-center gap-2">
          <div className="flex-1 h-1.5 bg-gray-200 dark:bg-gray-600 rounded-full overflow-hidden">
            <div
              className={clsx(
                "h-full rounded-full transition-all",
                porcentaje === 100 ? "bg-emerald-500" : "bg-blue-500",
              )}
              style={{ width: `${porcentaje}%` }}
              aria-label={`${completadas} de ${total} subtareas completadas`}
            />
          </div>
          <span className="text-xs text-gray-500 dark:text-gray-400 shrink-0">
            {completadas}/{total}
          </span>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              setExpanded((p) => !p);
            }}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 transition-colors"
            aria-label={expanded ? "Colapsar subtareas" : "Expandir subtareas"}
          >
            {expanded ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )}
          </button>
        </div>
      </div>

      {/* ── Lista de subtareas (Draggables) ───────────────────────── */}
      {expanded && subtareas.length > 0 && (
        <div className="border-t border-gray-100 dark:border-gray-700 px-2 pb-2 pt-1 space-y-1">
          {subtareas.map((sub, idx) => {
            const estilo =
              estadoSubtareaStyle[sub.estado] ?? estadoSubtareaStyle.PENDIENTE;
            return (
              <Draggable
                key={sub.id}
                draggableId={String(sub.id)}
                index={draggableIndex * 1000 + idx}
              >
                {(drag, dragSnap) => (
                  <div
                    ref={drag.innerRef}
                    {...drag.draggableProps}
                    {...drag.dragHandleProps}
                    aria-roledescription="Arrastra para mover entre columnas"
                    role="button"
                    tabIndex={0}
                    onClick={() => onClickTarea(sub)}
                    onKeyDown={(e) => e.key === "Enter" && onClickTarea(sub)}
                    className={clsx(
                      "flex flex-col gap-1 rounded border-l-[3px] px-2 py-2 cursor-pointer select-none transition-colors",
                      estilo.border,
                      estilo.bg,
                      "hover:brightness-95 dark:hover:brightness-110",
                      dragSnap.isDragging && "scale-95 shadow-md opacity-90",
                      sub.bloqueada && "ring-1 ring-red-400",
                    )}
                  >
                    {/* Fila superior: clave + badge de estado + horas */}
                    <div className="flex items-center gap-1.5">
                      <CheckSquare
                        className="h-3 w-3 shrink-0 text-gray-400"
                        aria-hidden="true"
                      />
                      {sub.jiraIssueKey && (
                        <a
                          href={jiraIssueUrl(sub.jiraIssueKey!)}
                          target="_blank"
                          rel="noopener noreferrer"
                          onClick={(e) => e.stopPropagation()}
                          className="font-mono text-[10px] text-blue-500 hover:text-blue-700 hover:underline transition-colors shrink-0"
                        >
                          {sub.jiraIssueKey}
                        </a>
                      )}
                      <span
                        className={clsx(
                          "ml-auto shrink-0 rounded px-1.5 py-0.5 text-[10px] font-medium",
                          estilo.badge,
                        )}
                      >
                        {estilo.label}
                      </span>
                      <span className="shrink-0 text-[10px] text-gray-400">
                        {sub.estimacion}h
                      </span>
                    </div>
                    {/* Título de la subtarea */}
                    <span
                      className="text-xs leading-snug text-gray-700 dark:text-gray-200 line-clamp-2"
                      title={sub.titulo}
                    >
                      {sub.titulo}
                    </span>
                  </div>
                )}
              </Draggable>
            );
          })}
        </div>
      )}
    </div>
  );
};
