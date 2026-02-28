/**
 * TimelineGrid — Grid de planificación Personas × Días
 * Muestra la matriz de asignaciones con drag-drop entre celdas.
 * Soporta tareas puntuales (un día) y barras multi-día (JIRA_PADRE, CONTINUA).
 * Componente presentacional: recibe datos, emite eventos.
 */

import type {
  DiaConTareas,
  PersonaEnLinea,
  TareaEnLinea,
  TareaResponse,
  TimelineSprintResponse,
} from "@/types/api";
import { jiraIssueUrl } from "@/lib/jira";
import {
  DragDropContext,
  Draggable,
  Droppable,
  DropResult,
} from "@hello-pangea/dnd";
import { clsx } from "clsx";
import type { FC, ReactNode } from "react";
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
  // Only count point tasks (not bars) for capacity display
  const pointTareas = dia.tareas.filter(
    (t) => !t.diaInicio || !t.diaFin || t.diaFin <= t.diaInicio,
  );
  const horasAsignadas = pointTareas.reduce((acc, t) => acc + (t.estimacion ?? 0), 0);
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
            if (pointTareas.length === 0 && onCrearEnCelda) {
              onCrearEnCelda(personaId, dia.dia);
            }
          }}
          className={clsx(
            "min-h-[64px] min-w-[80px] rounded border p-1 transition-colors",
            borderColor,
            snapshot.isDraggingOver
              ? "bg-blue-50 ring-2 ring-inset ring-blue-300 dark:bg-blue-900/20"
              : "bg-white dark:bg-gray-800",
            pointTareas.length === 0 && onCrearEnCelda
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

          {/* Tareas compactas (puntuales) */}
          <div className="space-y-0.5">
            {pointTareas.map((tarea, index) => (
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

// ============= BarCard (tarea multi-día) =============

interface BarCardProps {
  bar: TareaEnLinea;
  /** Número de columnas que abarca la barra */
  span: number;
  onClick: (tareaId: number) => void;
}

const BarCard: FC<BarCardProps> = ({ bar, span: _span, onClick }) => {
  const isInformativa = bar.esInformativa === true;
  const isContinua = bar.origen === "CONTINUA";
  const bgColor = isContinua && bar.color ? bar.color : undefined;

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={(e) => {
        e.stopPropagation();
        onClick(bar.tareaId);
      }}
      onKeyDown={(e) => e.key === "Enter" && onClick(bar.tareaId)}
      title={`${bar.titulo}${bar.diaFin ? ` (días ${bar.diaInicio}–${bar.diaFin})` : ""}${bar.horasPorDia ? ` · ${bar.horasPorDia}h/día` : ""}`}
      className={clsx(
        "mb-0.5 flex cursor-pointer select-none items-center gap-1 overflow-hidden rounded px-2 py-1 text-xs font-medium shadow-sm transition-shadow hover:shadow-md",
        isInformativa
          ? "border border-dashed border-current bg-transparent"
          : "text-white",
        !bgColor && !isInformativa && isContinua && "bg-indigo-500",
        !bgColor && !isInformativa && !isContinua && "bg-violet-600",
        isInformativa && isContinua && "text-indigo-600 dark:text-indigo-400",
        isInformativa && !isContinua && "text-violet-600 dark:text-violet-400",
      )}
      style={bgColor && !isInformativa ? { backgroundColor: bgColor } : undefined}
      aria-label={`Barra: ${bar.titulo}, días ${bar.diaInicio}–${bar.diaFin}`}
    >
      {/* Etiqueta de origen */}
      <span className="shrink-0 rounded bg-white/20 px-1 py-0.5 text-[9px] font-semibold uppercase">
        {bar.origen === "CONTINUA" ? "●" : "J"}
      </span>

      {/* Título */}
      <span className="min-w-0 flex-1 truncate">{bar.titulo}</span>

      {/* Jira key con link */}
      {bar.jiraIssueKey && (
        <a
          href={jiraIssueUrl(bar.jiraIssueKey)}
          target="_blank"
          rel="noopener noreferrer"
          onClick={(e) => e.stopPropagation()}
          className="shrink-0 rounded bg-white/20 px-1 py-0.5 text-[9px] hover:bg-white/30"
          aria-label={`Abrir ${bar.jiraIssueKey} en Jira`}
        >
          {bar.jiraIssueKey}
        </a>
      )}

      {/* Horas por día */}
      {bar.horasPorDia != null && (
        <span className="shrink-0 opacity-80">{bar.horasPorDia}h/d</span>
      )}

      {/* Rango de días */}
      <span className="shrink-0 opacity-70 text-[9px]">
        {bar.diaInicio}–{bar.diaFin}
      </span>
    </div>
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
    estimacion: t.estimacion ?? 0,
    prioridad: t.prioridad,
    estado: t.estado,
    bloqueada: t.bloqueada,
    jiraIssueKey: t.jiraIssueKey,
    createdAt: "",
  };
}

// ============= TimelineGrid principal =============

/**
 * Grid Personas × Días con drag-drop.
 * Drag: TareaEnLinea entre celdas (persona + día).
 * Barras: TareaEnLinea con diaInicio/diaFin se renderizan con colspan multi-columna.
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

  // Dividir los días en semanas de 5 días cada una
  const totalDias = personasFiltradas[0]?.dias.length ?? 10;
  const DIAS_POR_SEMANA = 5;
  const numSemanas = Math.ceil(totalDias / DIAS_POR_SEMANA);
  const semanas = Array.from({ length: numSemanas }, (_, s) => ({
    label: numSemanas > 1 ? `Semana ${s + 1}` : "Días del sprint",
    offset: s * DIAS_POR_SEMANA,
    count: Math.min(DIAS_POR_SEMANA, totalDias - s * DIAS_POR_SEMANA),
  }));

  return (
    <DragDropContext onDragEnd={handleDragEnd}>
      <div
        className="space-y-4"
        role="region"
        aria-label="Timeline de planificación"
      >
        {semanas.map((semana) => (
          <div
            key={semana.label}
            className="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700"
          >
            {/* Etiqueta de semana */}
            <div className="border-b border-gray-200 bg-gray-50 px-3 py-1.5 text-xs font-semibold text-gray-500 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400">
              {semana.label}
            </div>

            <table className="w-full min-w-max border-collapse text-sm">
              {/* Cabecera días */}
              <thead>
                <tr>
                  <th className="w-32 border-b border-r border-gray-200 bg-gray-50 px-3 py-2 text-left text-xs font-semibold text-gray-600 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400">
                    Persona
                  </th>
                  {Array.from({ length: semana.count }, (_, i) => {
                    const diaNum = semana.offset + i + 1;
                    return (
                      <th
                        key={diaNum}
                        className="border-b border-r border-gray-200 bg-gray-50 px-1 py-2 text-center text-xs font-semibold text-gray-600 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
                      >
                        <span className="block font-medium">
                          {DIAS_LABEL[semana.offset + i] ?? diaNum}
                        </span>
                        <span className="block text-[9px] text-gray-400">
                          {diaNum}
                        </span>
                      </th>
                    );
                  })}
                </tr>
              </thead>

              {/* Filas personas */}
              <tbody>
                {personasFiltradas.map((persona: PersonaEnLinea) => {
                  const diasSemana = persona.dias.slice(
                    semana.offset,
                    semana.offset + semana.count,
                  );
                  return (
                    <PersonaRow
                      key={persona.personaId}
                      persona={persona}
                      diasSemana={diasSemana}
                      weekOffset={semana.offset}
                      weekCount={semana.count}
                      onClickTarea={onClickTarea}
                      onCrearEnCelda={onCrearEnCelda}
                    />
                  );
                })}
              </tbody>
            </table>
          </div>
        ))}
      </div>
    </DragDropContext>
  );
};

// ============= PersonaRow — renderiza fila con soporte de colspan para barras =============

interface PersonaRowProps {
  persona: PersonaEnLinea;
  diasSemana: DiaConTareas[];
  weekOffset: number;
  weekCount: number;
  onClickTarea: (tareaId: number) => void;
  onCrearEnCelda?: (personaId: number, dia: number) => void;
}

const PersonaRow: FC<PersonaRowProps> = ({
  persona,
  diasSemana,
  weekOffset,
  weekCount,
  onClickTarea,
  onCrearEnCelda,
}) => {
  const weekEnd = weekOffset + weekCount; // último día de la semana (inclusive)

  // Calcular qué días están cubiertos por barras (para omitir su <td>)
  const skipDays = new Set<number>();
  // Map: diaInicio -> barras que comienzan en ese día
  const barsAtDay = new Map<number, TareaEnLinea[]>();

  for (let i = 0; i < weekCount; i++) {
    const diaNum = weekOffset + i + 1;
    const dia = diasSemana[i];
    if (!dia) continue;

    const barsHere = dia.tareas.filter(
      (t) =>
        t.diaInicio != null &&
        t.diaFin != null &&
        t.diaFin > t.diaInicio &&
        t.diaInicio === diaNum,
    );

    if (barsHere.length > 0) {
      barsAtDay.set(diaNum, barsHere);
      // Marcar días cubiertos (limitado al fin de la semana)
      barsHere.forEach((bar) => {
        const endDay = Math.min(bar.diaFin!, weekEnd);
        for (let d = diaNum + 1; d <= endDay; d++) {
          skipDays.add(d);
        }
      });
    }
  }

  const cells: ReactNode[] = [];

  for (let i = 0; i < weekCount; i++) {
    const diaNum = weekOffset + i + 1;

    if (skipDays.has(diaNum)) continue; // cubierto por colspan, omitir

    const dia = diasSemana[i];
    const barsStartingHere = barsAtDay.get(diaNum) ?? [];

    if (barsStartingHere.length > 0) {
      // Calcular colspan para el maior diaFin de las barras de este día
      const maxEndDay = barsStartingHere.reduce(
        (max, b) => Math.max(max, Math.min(b.diaFin!, weekEnd)),
        diaNum,
      );
      const colspan = maxEndDay - diaNum + 1;

      cells.push(
        <td
          key={diaNum}
          colSpan={colspan}
          className="border-r border-gray-100 p-1 align-top dark:border-gray-800"
        >
          {/* Barras multi-día */}
          {barsStartingHere.map((bar) => (
            <BarCard
              key={bar.tareaId}
              bar={bar}
              span={Math.min(bar.diaFin!, weekEnd) - diaNum + 1}
              onClick={onClickTarea}
            />
          ))}
          {/* Tareas puntuales en el mismo día */}
          {dia && (
            <DayCell
              personaId={persona.personaId}
              dia={dia}
              onClickTarea={onClickTarea}
              onCrearEnCelda={onCrearEnCelda}
            />
          )}
        </td>,
      );
    } else {
      cells.push(
        <td
          key={diaNum}
          className="border-r border-gray-100 p-1 align-top dark:border-gray-800"
        >
          {dia && (
            <DayCell
              personaId={persona.personaId}
              dia={dia}
              onClickTarea={onClickTarea}
              onCrearEnCelda={onCrearEnCelda}
            />
          )}
        </td>,
      );
    }
  }

  return (
    <tr className="border-b border-gray-100 dark:border-gray-800">
      {/* Nombre persona */}
      <td className="border-r border-gray-200 bg-gray-50 px-3 py-2 dark:border-gray-700 dark:bg-gray-900">
        <p
          className="max-w-[112px] truncate text-xs font-medium text-gray-800 dark:text-gray-200"
          title={persona.personaNombre}
        >
          {persona.personaNombre}
        </p>
      </td>
      {cells}
    </tr>
  );
};
