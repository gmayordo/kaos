/**
 * SprintSelector — Tests unitarios
 * Cubre render dropdown, selección, botones contextuales por estado y callbacks
 */

import type { SprintResponse } from "@/types/api";
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SprintSelector } from "./SprintSelector";

const buildSprint = (
  id: number,
  estado: SprintResponse["estado"],
): SprintResponse => ({
  id,
  nombre: `Sprint ${id}`,
  squadId: 1,
  squadNombre: "Squad Backend",
  fechaInicio: "2026-03-02",
  fechaFin: "2026-03-13",
  estado,
  capacidadTotal: 80,
  tareasPendientes: 5,
  tareasEnProgreso: 2,
  tareasCompletadas: 3,
  createdAt: "2026-02-22T10:00:00Z",
});

const sprints: SprintResponse[] = [
  buildSprint(1, "PLANIFICACION"),
  buildSprint(2, "ACTIVO"),
  buildSprint(3, "CERRADO"),
];

const defaultProps = {
  sprints,
  sprintSeleccionado: sprints[0],
  onSprintChange: vi.fn(),
  onCrearSprint: vi.fn(),
  onActivarSprint: vi.fn(),
  onCerrarSprint: vi.fn(),
  onReplanificarSprint: vi.fn(),
  onEliminarSprint: vi.fn(),
};

describe("SprintSelector", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("muestra el sprint seleccionado en el botón principal", () => {
    render(<SprintSelector {...defaultProps} />);
    expect(screen.getByText(/Sprint 1/i)).toBeInTheDocument();
    expect(screen.getByText(/Squad Backend/i)).toBeInTheDocument();
  });

  it("muestra texto 'Seleccionar sprint' cuando no hay sprint seleccionado", () => {
    render(<SprintSelector {...defaultProps} sprintSeleccionado={null} />);
    expect(screen.getByText("Seleccionar sprint")).toBeInTheDocument();
  });

  it("abre el dropdown al hacer click en el botón", () => {
    render(<SprintSelector {...defaultProps} />);
    const trigger = screen.getByRole("button", { name: /sprint 1/i });
    fireEvent.click(trigger);
    expect(screen.getByRole("listbox")).toBeInTheDocument();
    expect(screen.getAllByRole("option").length).toBe(3);
  });

  it("llama onSprintChange al seleccionar un sprint en el dropdown", () => {
    render(<SprintSelector {...defaultProps} />);
    fireEvent.click(screen.getByRole("button", { name: /sprint 1/i }));
    fireEvent.click(screen.getByRole("option", { name: /sprint 2/i }));
    expect(defaultProps.onSprintChange).toHaveBeenCalledWith(sprints[1]);
  });

  it("muestra botón Activar para sprint en PLANIFICACION", () => {
    render(
      <SprintSelector {...defaultProps} sprintSeleccionado={sprints[0]} />,
    );
    expect(
      screen.getByRole("button", { name: /activar/i }),
    ).toBeInTheDocument();
  });

  it("llama onActivarSprint al hacer click en Activar", () => {
    render(
      <SprintSelector {...defaultProps} sprintSeleccionado={sprints[0]} />,
    );
    fireEvent.click(screen.getByRole("button", { name: /activar/i }));
    expect(defaultProps.onActivarSprint).toHaveBeenCalledTimes(1);
  });

  it("muestra botón Inactivar sprint para sprint ACTIVO", () => {
    render(
      <SprintSelector {...defaultProps} sprintSeleccionado={sprints[1]} />,
    );
    expect(
      screen.getByRole("button", { name: /inactivar/i }),
    ).toBeInTheDocument();
  });

  it("muestra el estado como badge visible", () => {
    render(
      <SprintSelector {...defaultProps} sprintSeleccionado={sprints[1]} />,
    );
    expect(screen.getByText("Activo")).toBeInTheDocument();
  });

  it("muestra skeleton de carga cuando isLoading=true", () => {
    const { container } = render(
      <SprintSelector {...defaultProps} isLoading={true} />,
    );
    expect(
      container.querySelector('[aria-label="Cargando sprints..."]'),
    ).toBeInTheDocument();
  });

  it("llama onCrearSprint al hacer click en el botón Nuevo Sprint", () => {
    render(<SprintSelector {...defaultProps} />);
    const btnNuevo = screen.getByRole("button", { name: /nuevo|crear/i });
    fireEvent.click(btnNuevo);
    expect(defaultProps.onCrearSprint).toHaveBeenCalledTimes(1);
  });
});
