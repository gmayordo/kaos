/**
 * KanbanBoard — Tablero Kanban de 4 columnas
 * Columnas: PENDIENTE | EN_PROGRESO | BLOQUEADO | COMPLETADA
 * Drag-drop tareas entre columnas con validación de transiciones.
 * Componente presentacional: recibe datos, emite eventos.
 */

import type { EstadoTarea, TareaResponse } from "@/types/api";
import {
  DragDropContext,
  Draggable,
  Droppable,
  DropResult,
} from "@hello-pangea/dnd";
import { clsx } from "clsx";
import type { FC } from "react";
import { TaskCard } from "./TaskCard";

interface Props {
  /** Lista de tareas del sprint */
  tareas: TareaResponse[];
  /** Filtro por persona (null = todas) */
  personaFiltroId?: number | null;
  /** Callback al mover una tarea a otro estado */
  onCambiarEstado: (tareaId: number, nuevoEstado: EstadoTarea) => void;
  /** Callback al hacer click en una tarea */
  onClickTarea: (tarea: TareaResponse) => void;
  /** Estado de carga */
  isLoading?: boolean;
}

// ============= Configuración de columnas =============

interface Columna {
  id: EstadoTarea;
  label: string;
  colorHeader: string;
  colorDrop: string;
}

const COLUMNAS: Columna[] = [
  {
    id: "PENDIENTE",
    label: "Pendiente",
    colorHeader: "border-t-gray-400 bg-gray-50 dark:bg-gray-800/50",
    colorDrop: "bg-gray-50 dark:bg-gray-800/50",
  },
  {
    id: "EN_PROGRESO",
    label: "En progreso",
    colorHeader: "border-t-blue-500 bg-blue-50/50 dark:bg-blue-900/10",
    colorDrop: "bg-blue-50/50 dark:bg-blue-900/10",
  },
  {
    id: "BLOQUEADO",
    label: "Bloqueado",
    colorHeader: "border-t-red-500 bg-red-50/50 dark:bg-red-900/10",
    colorDrop: "bg-red-50/50 dark:bg-red-900/10",
  },
  {
    id: "COMPLETADA",
    label: "Completada",
    colorHeader: "border-t-emerald-500 bg-emerald-50/50 dark:bg-emerald-900/10",
    colorDrop: "bg-emerald-50/50 dark:bg-emerald-900/10",
  },
];

// Transiciones de estado permitidas
const TRANSICIONES: Record<EstadoTarea, EstadoTarea[]> = {
  PENDIENTE: ["EN_PROGRESO"],
  EN_PROGRESO: ["PENDIENTE", "BLOQUEADO", "COMPLETADA"],
  BLOQUEADO: ["EN_PROGRESO"],
  COMPLETADA: ["EN_PROGRESO"],
};

function esTransicionValida(
  estadoActual: EstadoTarea,
  nuevoEstado: EstadoTarea,
): boolean {
  if (estadoActual === nuevoEstado) return false;
  return TRANSICIONES[estadoActual]?.includes(nuevoEstado) ?? false;
}

/**
 * Tablero Kanban de 4 columnas con drag-drop.
 * Valida transiciones antes de emitir el cambio.
 */
export const KanbanBoard: FC<Props> = ({
  tareas,
  personaFiltroId,
  onCambiarEstado,
  onClickTarea,
  isLoading = false,
}) => {
  // Filtrar por persona si hay filtro activo
  const tareasFiltradas = personaFiltroId
    ? tareas.filter((t) => t.personaId === personaFiltroId)
    : tareas;

  const tareasPorEstado = COLUMNAS.reduce<Record<EstadoTarea, TareaResponse[]>>(
    (acc, col) => {
      acc[col.id] = tareasFiltradas.filter((t) => t.estado === col.id);
      return acc;
    },
    { PENDIENTE: [], EN_PROGRESO: [], BLOQUEADO: [], COMPLETADA: [] },
  );

  const handleDragEnd = (result: DropResult) => {
    const { destination, source, draggableId } = result;

    if (!destination) return;
    if (destination.droppableId === source.droppableId) return;

    const estadoOrigen = source.droppableId as EstadoTarea;
    const estadoDestino = destination.droppableId as EstadoTarea;

    if (!esTransicionValida(estadoOrigen, estadoDestino)) {
      // Transición no permitida → no hacer nada
      return;
    }

    const tareaId = parseInt(draggableId, 10);
    onCambiarEstado(tareaId, estadoDestino);
  };

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {COLUMNAS.map((col) => (
          <div
            key={col.id}
            className="rounded-lg border border-gray-200 dark:border-gray-700"
          >
            <div className="h-10 animate-pulse rounded-t-lg bg-gray-200 dark:bg-gray-700" />
            <div className="space-y-3 p-3">
              {[1, 2, 3].map((i) => (
                <div
                  key={i}
                  className="h-20 animate-pulse rounded-lg bg-gray-100 dark:bg-gray-800"
                />
              ))}
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <DragDropContext onDragEnd={handleDragEnd}>
      <div
        className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4"
        role="region"
        aria-label="Tablero Kanban"
      >
        {COLUMNAS.map((columna) => {
          const items = tareasPorEstado[columna.id];
          return (
            <div
              key={columna.id}
              className={clsx(
                "flex flex-col rounded-lg border border-gray-200 border-t-4 dark:border-gray-700",
                columna.colorHeader,
              )}
            >
              {/* Cabecera columna */}
              <div className="flex items-center justify-between px-3 py-2">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">
                  {columna.label}
                </h3>
                <span
                  className="rounded-full bg-gray-200 px-2 py-0.5 text-xs font-medium text-gray-600 dark:bg-gray-700 dark:text-gray-300"
                  aria-label={`${items.length} tareas en ${columna.label}`}
                >
                  {items.length}
                </span>
              </div>

              {/* Drop zone */}
              <Droppable droppableId={columna.id}>
                {(provided, snapshot) => (
                  <div
                    ref={provided.innerRef}
                    {...provided.droppableProps}
                    className={clsx(
                      "min-h-[120px] flex-1 space-y-2 rounded-b-lg p-2 transition-colors",
                      snapshot.isDraggingOver
                        ? "bg-blue-50 ring-2 ring-inset ring-blue-300 dark:bg-blue-900/20"
                        : columna.colorDrop,
                    )}
                    aria-label={`Columna ${columna.label}`}
                  >
                    {items.length === 0 && !snapshot.isDraggingOver && (
                      <p className="py-4 text-center text-xs text-gray-400 dark:text-gray-500">
                        Sin tareas
                      </p>
                    )}
                    {items.map((tarea, index) => (
                      <Draggable
                        key={tarea.id}
                        draggableId={String(tarea.id)}
                        index={index}
                      >
                        {(drag, dragSnapshot) => (
                          <div
                            ref={drag.innerRef}
                            {...drag.draggableProps}
                            {...drag.dragHandleProps}
                            aria-roledescription="Arrastra para mover entre columnas"
                          >
                            <TaskCard
                              tarea={tarea}
                              variant="standard"
                              onClick={onClickTarea}
                              isDragging={dragSnapshot.isDragging}
                            />
                          </div>
                        )}
                      </Draggable>
                    ))}
                    {provided.placeholder}
                  </div>
                )}
              </Droppable>
            </div>
          );
        })}
      </div>
    </DragDropContext>
  );
};
