/**
 * AusenciaForm — Tests básicos del formulario de ausencia
 */

import type { PersonaResponse } from "@/types/api";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AusenciaForm } from "./AusenciaForm";

// Mock data
const mockPersonas: PersonaResponse[] = [
  {
    id: 1,
    nombre: "Juan Pérez",
    email: "juan@example.com",
    perfilHorarioId: 1,
    perfilHorarioNombre: "Estándar",
    seniority: "SENIOR",
    activo: true,
    sendNotifications: true,
    createdAt: "2024-01-01T00:00:00Z",
    updatedAt: "2024-01-01T00:00:00Z",
  },
];

describe("AusenciaForm", () => {
  const mockOnSubmit = vi.fn();
  const mockOnCancel = vi.fn();

  it("debe renderizar el formulario básico", () => {
    render(
      <AusenciaForm
        personas={mockPersonas}
        onSubmit={mockOnSubmit}
        onCancel={mockOnCancel}
      />,
    );

    expect(screen.getByText("Registrar ausencia")).toBeInTheDocument();
    expect(screen.getByLabelText("Persona *")).toBeInTheDocument();
    expect(screen.getByLabelText("Fecha inicio *")).toBeInTheDocument();
    expect(screen.getByText("Guardar")).toBeInTheDocument();
    expect(screen.getByText("Cancelar")).toBeInTheDocument();
  });
});
