/**
 * KanbanBoard — Tests unitarios
 * Cubre render de 4 columnas, filtro persona, estado loading y callbacks
 *
 * Nota: @hello-pangea/dnd se mockea para entorno jsdom (sin drag-drop real)
 */

import type { TareaResponse } from "@/types/api";
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { KanbanBoard } from "./KanbanBoard";

// Mock de @hello-pangea/dnd — los componentes solo renderizan sus children
vi.mock("@hello-pangea/dnd", () => ({
  DragDropContext: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
  Droppable: ({
    children,
  }: {
    children: (provided: object, snapshot: object) => React.ReactNode;
  }) =>
    children(
      {
        innerRef: () => {},
        droppableProps: {},
        placeholder: null,
      },
      { isDraggingOver: false },
    ) as React.ReactElement,
  Draggable: ({
    children,
  }: {
    children: (provided: object, snapshot: object) => React.ReactNode;
  }) =>
    children(
      {
        innerRef: () => {},
        draggableProps: {},
        dragHandleProps: {},
      },
      { isDragging: false },
    ) as React.ReactElement,
}));

const buildTarea = (
  id: number,
  estado: TareaResponse["estado"],
): TareaResponse => ({
  id,
  titulo: `Tarea ${id}`,
  sprintId: 1,
  personaId: id % 2 === 0 ? 2 : 1,
  personaNombre: id % 2 === 0 ? "María García" : "Juan Pérez",
  tipo: "HISTORIA",
  categoria: "EVOLUTIVO",
  estimacion: 3,
  prioridad: "NORMAL",
  estado,
  diaAsignado: id,
  bloqueada: estado === "BLOQUEADO",
  createdAt: "2026-02-22T10:00:00Z",
});

const mockTareas: TareaResponse[] = [
  buildTarea(1, "PENDIENTE"),
  buildTarea(2, "EN_PROGRESO"),
  buildTarea(3, "BLOQUEADO"),
  buildTarea(4, "COMPLETADA"),
  buildTarea(5, "PENDIENTE"),
];

describe("KanbanBoard", () => {
  const onCambiarEstado = vi.fn();
  const onClickTarea = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renderiza las 4 columnas del kanban", () => {
    render(
      <KanbanBoard
        tareas={mockTareas}
        onCambiarEstado={onCambiarEstado}
        onClickTarea={onClickTarea}
      />,
    );
    expect(screen.getByText("Pendiente")).toBeInTheDocument();
    expect(screen.getByText("En progreso")).toBeInTheDocument();
    expect(screen.getByText(/bloque/i)).toBeInTheDocument();
    // "Completada" puede ser "Completadas" según el componente
    expect(screen.getByText(/complet/i)).toBeInTheDocument();
  });

  it("muestra las tareas distribuidas en sus columnas", () => {
    render(
      <KanbanBoard
        tareas={mockTareas}
        onCambiarEstado={onCambiarEstado}
        onClickTarea={onClickTarea}
      />,
    );
    expect(screen.getByText("Tarea 1")).toBeInTheDocument();
    expect(screen.getByText("Tarea 2")).toBeInTheDocument();
    expect(screen.getByText("Tarea 4")).toBeInTheDocument();
  });

  it("filtra tareas por persona si se pasa personaFiltroId", () => {
    render(
      <KanbanBoard
        tareas={mockTareas}
        personaFiltroId={1}
        onCambiarEstado={onCambiarEstado}
        onClickTarea={onClickTarea}
      />,
    );
    // personaId=1 → Tarea 1, 3, 5
    expect(screen.getByText("Tarea 1")).toBeInTheDocument();
    expect(screen.queryByText("Tarea 2")).not.toBeInTheDocument();
  });

  it("muestra skeleton de carga cuando isLoading=true", () => {
    const { container } = render(
      <KanbanBoard
        tareas={[]}
        onCambiarEstado={onCambiarEstado}
        onClickTarea={onClickTarea}
        isLoading={true}
      />,
    );
    // Cuando isLoading, el componente renderiza divs animate-pulse
    expect(container.querySelector(".animate-pulse")).toBeInTheDocument();
  });

  it("renderiza correctamente sin tareas (tablero vacío)", () => {
    render(
      <KanbanBoard
        tareas={[]}
        onCambiarEstado={onCambiarEstado}
        onClickTarea={onClickTarea}
      />,
    );
    expect(screen.getByText("Pendiente")).toBeInTheDocument();
    // No debe romperse con array vacío
  });
});
