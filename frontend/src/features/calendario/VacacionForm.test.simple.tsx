/**
 * Test simple para VacacionForm
 * Verifica que el componente se renderiza correctamente
 */

import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { VacacionForm } from "./VacacionForm";

describe("VacacionForm", () => {
  it("should render the form", () => {
    const mockPersonas = [
      { id: 1, nombre: "Juan Pérez", email: "juan@example.com" },
    ];

    render(
      <VacacionForm
        personas={mockPersonas}
        onSubmit={() => {}}
        onCancel={() => {}}
        isSubmitting={false}
        squadId={1}
      />,
    );

    expect(screen.getByText("Nueva Vacación")).toBeInTheDocument();
  });
});
