/**
 * ColaboradoresChip — 6 tests
 * Cubre render de avatares, overflow +N y tooltip
 */

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ColaboradoresChip } from "./ColaboradoresChip";

const personas = [
  { nombre: "Ana García" },
  { nombre: "Carlos López" },
  { nombre: "María Fernández" },
  { nombre: "Pedro Sánchez" },
];

describe("ColaboradoresChip", () => {
  it("no renderiza nada si personas está vacío", () => {
    const { container } = render(<ColaboradoresChip personas={[]} />);
    expect(container.firstChild).toBeNull();
  });

  it("muestra las iniciales del primer colaborador", () => {
    render(<ColaboradoresChip personas={[{ nombre: "Ana García" }]} />);
    expect(screen.getByText("AG")).toBeInTheDocument();
  });

  it("muestra hasta maxVisible avatares (default 3)", () => {
    render(<ColaboradoresChip personas={personas} />);
    const avatares = screen.getAllByText(/^[A-Z]{2}$/);
    // Con 4 personas y maxVisible=3, se muestran 3 iniciales + "+1" overflow
    expect(avatares).toHaveLength(3);
  });

  it("muestra badge de overflow si hay más personas de las visibles", () => {
    render(<ColaboradoresChip personas={personas} maxVisible={2} />);
    expect(screen.getByText("+2")).toBeInTheDocument();
  });

  it("no muestra badge de overflow si entran todas las personas", () => {
    render(
      <ColaboradoresChip personas={personas.slice(0, 3)} maxVisible={3} />,
    );
    expect(screen.queryByText(/^\+\d+$/)).not.toBeInTheDocument();
  });

  it("muestra tooltip con todos los nombres al hacer hover", () => {
    render(<ColaboradoresChip personas={personas.slice(0, 2)} />);
    // El div tiene aria-label="Colaboradores: ..."
    const container = screen.getByLabelText(/Colaboradores:/i);
    fireEvent.mouseEnter(container);
    expect(screen.getByRole("tooltip")).toBeInTheDocument();
    expect(screen.getByRole("tooltip")).toHaveTextContent("Ana García");
    expect(screen.getByRole("tooltip")).toHaveTextContent("Carlos López");
  });
});
