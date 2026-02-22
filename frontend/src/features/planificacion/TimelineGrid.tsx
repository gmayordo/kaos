/**
 * TimelineGrid — Grid de planificación Personas × Días
 * Muestra la matriz de asignaciones con drag-drop entre celdas.
 * Componente presentacional: recibe datos, emite eventos.
 */

import type {
  DiaConTareas,
  PersonaEnLinea,
  TareaEnLinea,
  TareaResponse,
  TimelineSprintResponse,
} from "@/types/api";
import {
  DragDropContext,
  Draggable,
  Droppable,
  DropResult,
} from "@hello-pangea/dnd";
import { clsx } from "clsx";
import type { FC } from "react";
import { TaskCard } from "./TaskCard";

// ============= Tipos de drop destination =============

interface DropInfo {
  personaId: number;
  dia: number;
}

function parseDropId(id: string): DropInfo | null {
  const [personaStr, diaStr] = id.split("-");
  const personaId = parseInt(personaStr, 10);
  const dia = parseInt(diaStr, 10);
  if (isNaN(personaId) || isNaN(dia)) return null;
  return { personaId, dia };
}

// ============= Props =============

interface Props {
  /** Datos de la timeline (personas + días + tareas) */
  timeline: TimelineSprintResponse;
  /** Personas excluidas del filtro (IDs ocultados) */
  personasOcultas?: Set<number>;
  /** Callback al mover una tarea a otro día/persona */
  onMoverTarea: (
    tareaId: number,
    nuevaPersonaId: number,
    nuevoDia: number,
  ) => void;
  /** Callback al hacer click en una tarea (abrir modal) */
  onClickTarea: (tareaId: number) => void;
  /** Callback al hacer click en celda vacía (crear tarea) */
  onCrearEnCelda?: (personaId: number, dia: number) => void;
  /** Estado de carga */
  isLoading?: boolean;
}

// ============= DayCell =============

const DIAS_LABEL = ["L", "M", "X", "J", "V", "L", "M", "X", "J", "V"];

interface DayCellProps {
  personaId: number;
  dia: DiaConTareas;
  onClickTarea: (tareaId: number) => void;
  onCrearEnCelda?: (personaId: number, diaNum: number) => void;
}

const DayCell: FC<DayCellProps> = ({
  personaId,
  dia,
  onClickTarea,
  onCrearEnCelda,
}) => {
  const horasAsignadas = dia.tareas.reduce((acc, t) => acc + t.estimacion, 0);
  const disponibles = dia.horasDisponibles;
  const pctOcupacion =
    disponibles > 0 ? (horasAsignadas / disponibles) * 100 : 0;

  const borderColor =
    pctOcupacion > 100
      ? "border-red-400"
      : pctOcupacion >= 80
        ? "border-amber-400"
        : "border-gray-200 dark:border-gray-700";

  const capacidadColor =
    pctOcupacion > 100
      ? "text-red-600 font-bold"
      : pctOcupacion >= 80
        ? "text-amber-600"
        : "text-gray-400";

  const dropId = `${personaId}-${dia.dia}`;

  return (
    <Droppable droppableId={dropId}>
      {(provided, snapshot) => (
        <div
          ref={provided.innerRef}
          {...provided.droppableProps}
          onClick={() => {
            if (dia.tareas.length === 0 && onCrearEnCelda) {
              onCrearEnCelda(personaId, dia.dia);
            }
          }}
          className={clsx(
            "min-h-[64px] min-w-[80px] rounded border p-1 transition-colors",
            borderColor,
            snapshot.isDraggingOver
              ? "bg-blue-50 ring-2 ring-inset ring-blue-300 dark:bg-blue-900/20"
              : "bg-white dark:bg-gray-800",
            dia.tareas.length === 0 && onCrearEnCelda
              ? "cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50"
              : "",
          )}
          aria-label={`Día ${dia.dia}, ${horasAsignadas}h / ${disponibles}h disponibles`}
        >
          {/* Indicador de capacidad */}
          <p
            className={clsx("mb-0.5 text-right text-[10px]", capacidadColor)}
            title={`${horasAsignadas}h asignadas / ${disponibles}h disponibles`}
          >
            {disponibles > 0 ? `${horasAsignadas}/${disponibles}h` : "—"}
          </p>

          {/* Tareas compactas */}
          <div className="space-y-0.5">
            {dia.tareas.map((tarea, index) => (
              <Draggable
                key={tarea.tareaId}
                draggableId={String(tarea.tareaId)}
                index={index}
              >
                {(drag, dragSnapshot) => (
                  <div
                    ref={drag.innerRef}
                    {...drag.draggableProps}
                    {...drag.dragHandleProps}
                    aria-roledescription="Arrastra para reasignar a otro día o persona"
                    onClick={(e) => {
                      e.stopPropagation();
                      onClickTarea(tarea.tareaId);
                    }}
                  >
                    <TaskCard
                      tarea={tareaEnLineaToTareaResponse(tarea)}
                      variant="compact"
                      isDragging={dragSnapshot.isDragging}
                    />
                  </div>
                )}
              </Draggable>
            ))}
          </div>
          {provided.placeholder}
        </div>
      )}
    </Droppable>
  );
};

/**
 * Adapta TareaEnLinea (del timeline) al shape de TareaResponse para TaskCard
 */
function tareaEnLineaToTareaResponse(t: TareaEnLinea): TareaResponse {
  return {
    id: t.tareaId,
    titulo: t.titulo,
    sprintId: 0,
    tipo: "TAREA",
    categoria: "CORRECTIVO",
    estimacion: t.estimacion,
    prioridad: t.prioridad,
    estado: t.estado,
    bloqueada: t.bloqueada,
    createdAt: "",
  };
}

// ============= TimelineGrid principal =============

/**
 * Grid Personas × Días con drag-drop.
 * Drag: TareaEnLinea entre celdas (persona + día).
 */
export const TimelineGrid: FC<Props> = ({
  timeline,
  personasOcultas = new Set(),
  onMoverTarea,
  onClickTarea,
  onCrearEnCelda,
  isLoading = false,
}) => {
  const handleDragEnd = (result: DropResult) => {
    const { destination, source, draggableId } = result;
    if (!destination) return;
    if (destination.droppableId === source.droppableId) return;

    const destInfo = parseDropId(destination.droppableId);
    if (!destInfo) return;

    const tareaId = parseInt(draggableId, 10);
    onMoverTarea(tareaId, destInfo.personaId, destInfo.dia);
  };

  if (isLoading) {
    return (
      <div className="space-y-2" aria-label="Cargando timeline...">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="h-20 animate-pulse rounded-lg bg-gray-100 dark:bg-gray-800"
          />
        ))}
      </div>
    );
  }

  const personasFiltradas = timeline.personas.filter(
    (p) => !personasOcultas.has(p.personaId),
  );

  if (personasFiltradas.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-gray-300 py-12 text-center dark:border-gray-600">
        <p className="text-sm text-gray-400">Sin personas en el sprint.</p>
      </div>
    );
  }

  // Número de días desde la lista de la primera persona
  const diasCount = personasFiltradas[0]?.dias.length ?? 10;

  return (
    <DragDropContext onDragEnd={handleDragEnd}>
      <div
        className="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700"
        role="region"
        aria-label="Timeline de planificación"
      >
        <table className="w-full min-w-max border-collapse text-sm">
          {/* Cabecera días */}
          <thead>
            <tr>
              <th className="w-32 border-b border-r border-gray-200 bg-gray-50 px-3 py-2 text-left text-xs font-semibold text-gray-600 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400">
                Persona
              </th>
              {Array.from({ length: diasCount }, (_, i) => i + 1).map((dia) => (
                <th
                  key={dia}
                  className="border-b border-r border-gray-200 bg-gray-50 px-1 py-2 text-center text-xs font-semibold text-gray-600 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
                >
                  <span className="block font-medium">
                    {DIAS_LABEL[dia - 1] ?? dia}
                  </span>
                  <span className="block text-[9px] text-gray-400">{dia}</span>
                </th>
              ))}
            </tr>
          </thead>

          {/* Filas personas */}
          <tbody>
            {personasFiltradas.map((persona: PersonaEnLinea) => (
              <tr
                key={persona.personaId}
                className="border-b border-gray-100 dark:border-gray-800"
              >
                {/* Nombre persona */}
                <td className="border-r border-gray-200 bg-gray-50 px-3 py-2 dark:border-gray-700 dark:bg-gray-900">
                  <p
                    className="max-w-[112px] truncate text-xs font-medium text-gray-800 dark:text-gray-200"
                    title={persona.personaNombre}
                  >
                    {persona.personaNombre}
                  </p>
                </td>

                {/* Celdas días */}
                {persona.dias.map((dia) => (
                  <td
                    key={dia.dia}
                    className="border-r border-gray-100 p-1 align-top dark:border-gray-800"
                  >
                    <DayCell
                      personaId={persona.personaId}
                      dia={dia}
                      onClickTarea={onClickTarea}
                      onCrearEnCelda={onCrearEnCelda}
                    />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </DragDropContext>
  );
};
