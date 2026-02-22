/**
 * ModalTarea — Tests unitarios
 * Cubre render crear/editar, validaciones, callbacks y estado de envío
 */

import type { PersonaResponse, TareaResponse } from "@/types/api";
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ModalTarea } from "./ModalTarea";

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock("lucide-react", () => ({
  X: () => <span data-testid="icon-x">X</span>,
}));

// ── Fixtures ──────────────────────────────────────────────────────────────────

const mockPersonas: PersonaResponse[] = [
  {
    id: 1,
    nombre: "Ana García",
    email: "ana.garcia@ehcos.com",
    perfilHorarioId: 1,
    perfilHorarioNombre: "Jornada Completa",
    seniority: "SENIOR",
    activo: true,
    sendNotifications: false,
    createdAt: "2025-01-01T00:00:00Z",
    updatedAt: "2025-01-01T00:00:00Z",
  },
  {
    id: 2,
    nombre: "Carlos López",
    email: "carlos.lopez@ehcos.com",
    perfilHorarioId: 1,
    perfilHorarioNombre: "Jornada Completa",
    seniority: "MID",
    activo: true,
    sendNotifications: false,
    createdAt: "2025-01-01T00:00:00Z",
    updatedAt: "2025-01-01T00:00:00Z",
  },
];

const mockTarea: TareaResponse = {
  id: 42,
  titulo: "Implementar dashboard",
  sprintId: 10,
  personaId: 1,
  personaNombre: "Ana García",
  tipo: "HISTORIA",
  categoria: "EVOLUTIVO",
  estimacion: 5,
  prioridad: "ALTA",
  estado: "EN_PROGRESO",
  diaAsignado: 3,
  diaCapacidadDisponible: 8,
  bloqueada: false,
  referenciaJira: "KAOS-100",
  createdAt: "2026-03-01T10:00:00Z",
};

const baseProps = {
  sprintId: 10,
  personas: mockPersonas,
  onSubmit: vi.fn(),
  onCancel: vi.fn(),
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe("ModalTarea", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ── Modo Crear ──────────────────────────────────────────────────────────────

  describe("modo crear (sin tarea prop)", () => {
    it('muestra el título "Nueva tarea"', () => {
      render(<ModalTarea {...baseProps} />);
      expect(screen.getByText("Nueva tarea")).toBeInTheDocument();
    });

    it('muestra el botón submit con texto "Crear tarea"', () => {
      render(<ModalTarea {...baseProps} />);
      expect(
        screen.getByRole("button", { name: "Crear tarea" }),
      ).toBeInTheDocument();
    });

    it("no muestra el botón Eliminar", () => {
      render(<ModalTarea {...baseProps} onDelete={vi.fn()} />);
      // Sin tarea prop, el botón Eliminar no debe aparecer
      expect(
        screen.queryByRole("button", { name: "Eliminar" }),
      ).not.toBeInTheDocument();
    });

    it("preselecciona diaAsignado cuando se pasa diaPreseleccionado", () => {
      render(<ModalTarea {...baseProps} diaPreseleccionado={5} />);
      const input = screen.getByLabelText(/día/i) as HTMLInputElement;
      expect(input.value).toBe("5");
    });
  });

  // ── Modo Editar ─────────────────────────────────────────────────────────────

  describe("modo editar (con tarea prop)", () => {
    it('muestra el título "Editar tarea"', () => {
      render(<ModalTarea {...baseProps} tarea={mockTarea} />);
      expect(screen.getByText("Editar tarea")).toBeInTheDocument();
    });

    it('muestra el botón submit con texto "Guardar cambios"', () => {
      render(<ModalTarea {...baseProps} tarea={mockTarea} />);
      expect(
        screen.getByRole("button", { name: "Guardar cambios" }),
      ).toBeInTheDocument();
    });

    it("muestra el botón Eliminar cuando se provee onDelete", () => {
      render(
        <ModalTarea {...baseProps} tarea={mockTarea} onDelete={vi.fn()} />,
      );
      expect(
        screen.getByRole("button", { name: "Eliminar" }),
      ).toBeInTheDocument();
    });

    it("rellena el formulario con los datos de la tarea", () => {
      render(<ModalTarea {...baseProps} tarea={mockTarea} />);
      const input = screen.getByLabelText(/título/i) as HTMLInputElement;
      expect(input.value).toBe("Implementar dashboard");
    });
  });

  // ── Validaciones ─────────────────────────────────────────────────────────────

  describe("validaciones del formulario", () => {
    it("muestra error si se envía sin título", () => {
      render(<ModalTarea {...baseProps} />);
      // Rellenar estimación para no tener ese error
      const estimacion = screen.getByLabelText(/estimaci/i) as HTMLInputElement;
      fireEvent.change(estimacion, { target: { value: "3" } });

      fireEvent.submit(screen.getByRole("dialog").querySelector("form")!);

      expect(screen.getByText("El título es obligatorio")).toBeInTheDocument();
      expect(baseProps.onSubmit).not.toHaveBeenCalled();
    });

    it("muestra error si la estimación es 0 o vacía", () => {
      render(<ModalTarea {...baseProps} />);
      // Rellenar título para no tener ese error
      fireEvent.change(screen.getByLabelText(/título/i), {
        target: { value: "Mi tarea" },
      });

      fireEvent.submit(screen.getByRole("dialog").querySelector("form")!);

      expect(
        screen.getByText("Estimación debe ser mayor a 0"),
      ).toBeInTheDocument();
      expect(baseProps.onSubmit).not.toHaveBeenCalled();
    });
  });

  // ── Callbacks ────────────────────────────────────────────────────────────────

  describe("callbacks", () => {
    it("llama a onSubmit con los datos correctos al enviar formulario válido", () => {
      render(<ModalTarea {...baseProps} />);

      fireEvent.change(screen.getByLabelText(/título/i), {
        target: { value: "Nueva tarea test" },
      });
      fireEvent.change(screen.getByLabelText(/estimaci/i), {
        target: { value: "3" },
      });

      fireEvent.submit(screen.getByRole("dialog").querySelector("form")!);

      expect(baseProps.onSubmit).toHaveBeenCalledOnce();
      expect(baseProps.onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          titulo: "Nueva tarea test",
          estimacion: 3,
          sprintId: 10,
        }),
      );
    });

    it("llama a onCancel al pulsar el botón Cancelar", () => {
      render(<ModalTarea {...baseProps} />);
      fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));
      expect(baseProps.onCancel).toHaveBeenCalledOnce();
    });

    it("llama a onDelete al confirmar la eliminación", () => {
      const onDelete = vi.fn();
      vi.spyOn(window, "confirm").mockReturnValue(true);

      render(
        <ModalTarea {...baseProps} tarea={mockTarea} onDelete={onDelete} />,
      );
      fireEvent.click(screen.getByRole("button", { name: "Eliminar" }));

      expect(window.confirm).toHaveBeenCalled();
      expect(onDelete).toHaveBeenCalledWith(42);
    });

    it("NO llama a onDelete si el usuario cancela la confirmación", () => {
      const onDelete = vi.fn();
      vi.spyOn(window, "confirm").mockReturnValue(false);

      render(
        <ModalTarea {...baseProps} tarea={mockTarea} onDelete={onDelete} />,
      );
      fireEvent.click(screen.getByRole("button", { name: "Eliminar" }));

      expect(onDelete).not.toHaveBeenCalled();
    });
  });

  // ── Estado de envío ──────────────────────────────────────────────────────────

  describe("estado isSubmitting", () => {
    it('muestra "Guardando..." en el botón submit', () => {
      render(<ModalTarea {...baseProps} isSubmitting={true} />);
      expect(screen.getByText("Guardando...")).toBeInTheDocument();
    });

    it("deshabilita los botones cuando isSubmitting=true", () => {
      render(
        <ModalTarea
          {...baseProps}
          tarea={mockTarea}
          onDelete={vi.fn()}
          isSubmitting={true}
        />,
      );
      expect(screen.getByRole("button", { name: "Cancelar" })).toBeDisabled();
      expect(screen.getByRole("button", { name: "Eliminar" })).toBeDisabled();
      expect(screen.getByText("Guardando...").closest("button")).toBeDisabled();
    });
  });
});
