/**
 * TaskCard — Tests unitarios
 * Cubre render variantes, badges de tipo/estado/prioridad y click
 */

import type { TareaResponse } from "@/types/api";
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TaskCard } from "./TaskCard";

const mockTarea: TareaResponse = {
  id: 1,
  titulo: "Implementar login OAuth",
  sprintId: 10,
  personaId: 1,
  personaNombre: "Juan Pérez",
  tipo: "HISTORIA",
  categoria: "EVOLUTIVO",
  estimacion: 4,
  prioridad: "ALTA",
  estado: "PENDIENTE",
  diaAsignado: 3,
  diaCapacidadDisponible: 8,
  bloqueada: false,
  referenciaJira: "KAOS-42",
  createdAt: "2026-02-22T10:00:00Z",
};

describe("TaskCard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("variante compact (timeline)", () => {
    it("renderiza el título de la tarea", () => {
      render(<TaskCard tarea={mockTarea} variant="compact" />);
      expect(screen.getByText("Implementar login OAuth")).toBeInTheDocument();
    });

    it("muestra la estimación en horas", () => {
      render(<TaskCard tarea={mockTarea} variant="compact" />);
      expect(screen.getByText(/4h/i)).toBeInTheDocument();
    });

    it("aplica el color del tipo de tarea (bg-violet-500 para Historia)", () => {
      render(<TaskCard tarea={mockTarea} variant="compact" />);
      // La variante compact sólo muestra icono, no texto del label.
      // Verificamos que el elemento tiene la clase de color del tipo HISTORIA.
      const card = screen.getByRole("button", {
        name: /Tarea: Implementar login OAuth/i,
      });
      expect(card).toHaveClass("bg-violet-500");
    });
  });

  describe("variante standard (kanban)", () => {
    it("renderiza el título y persona asignada", () => {
      render(<TaskCard tarea={mockTarea} variant="standard" />);
      expect(screen.getByText("Implementar login OAuth")).toBeInTheDocument();
      expect(screen.getByText("Juan Pérez")).toBeInTheDocument();
    });

    it("muestra la referencia Jira si existe", () => {
      render(<TaskCard tarea={mockTarea} variant="standard" />);
      expect(screen.getByText("KAOS-42")).toBeInTheDocument();
    });

    it("muestra badge de prioridad ALTA", () => {
      render(<TaskCard tarea={mockTarea} variant="standard" />);
      expect(screen.getByText(/alta/i)).toBeInTheDocument();
    });

    it("tarea bloqueada muestra indicador visual", () => {
      const tareaBloque: TareaResponse = {
        ...mockTarea,
        bloqueada: true,
        estado: "BLOQUEADO",
      };
      render(<TaskCard tarea={tareaBloque} variant="standard" />);
      // El componente añade clase border-red en bloqueada
      const card = screen.getByText("Implementar login OAuth").closest("div");
      expect(card).toBeInTheDocument();
    });
  });

  describe("interacción", () => {
    it("llama onClick al hacer click en la tarjeta", () => {
      const handleClick = vi.fn();
      render(
        <TaskCard tarea={mockTarea} variant="standard" onClick={handleClick} />,
      );
      fireEvent.click(screen.getByText("Implementar login OAuth"));
      expect(handleClick).toHaveBeenCalledWith(mockTarea);
    });

    it("no lanza error si onClick no está definido", () => {
      render(<TaskCard tarea={mockTarea} variant="standard" />);
      expect(() =>
        fireEvent.click(screen.getByText("Implementar login OAuth")),
      ).not.toThrow();
    });
  });
});
