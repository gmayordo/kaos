/**
 * EventoBadge — Tests del badge de eventos
 * Tests unitarios para el componente EventoBadge
 */

import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EventoBadge } from "./EventoBadge";

describe("EventoBadge", () => {
  describe("Renderizado básico", () => {
    it("debe renderizar el contenido children", () => {
      render(<EventoBadge tipo="festivo">Día festivo</EventoBadge>);

      expect(screen.getByText("Día festivo")).toBeInTheDocument();
    });

    it("debe renderizar como un span", () => {
      render(<EventoBadge tipo="vacacion">Vacaciones</EventoBadge>);

      const badge = screen.getByText("Vacaciones");
      expect(badge.tagName).toBe("SPAN");
    });
  });

  describe("Tipos de evento", () => {
    it("debe aplicar clases correctas para tipo festivo", () => {
      render(<EventoBadge tipo="festivo">Festivo</EventoBadge>);

      const badge = screen.getByText("Festivo");
      expect(badge).toHaveClass(
        "bg-zinc-100",
        "text-zinc-700",
        "border-zinc-300",
      );
    });

    it("debe aplicar clases correctas para tipo vacacion", () => {
      render(<EventoBadge tipo="vacacion">Vacaciones</EventoBadge>);

      const badge = screen.getByText("Vacaciones");
      expect(badge).toHaveClass(
        "bg-blue-100",
        "text-blue-700",
        "border-blue-300",
      );
    });

    it("debe aplicar clases correctas para tipo ausencia", () => {
      render(<EventoBadge tipo="ausencia">Ausencia</EventoBadge>);

      const badge = screen.getByText("Ausencia");
      expect(badge).toHaveClass(
        "bg-amber-100",
        "text-amber-700",
        "border-amber-300",
      );
    });

    it("debe aplicar clases correctas para tipo libre", () => {
      render(<EventoBadge tipo="libre">Día libre</EventoBadge>);

      const badge = screen.getByText("Día libre");
      expect(badge).toHaveClass(
        "bg-emerald-100",
        "text-emerald-700",
        "border-emerald-300",
      );
    });
  });

  describe("Variantes", () => {
    it("debe usar variante default por defecto", () => {
      render(<EventoBadge tipo="festivo">Festivo</EventoBadge>);

      const badge = screen.getByText("Festivo");
      expect(badge).toHaveClass(
        "bg-zinc-100",
        "text-zinc-700",
        "border-zinc-300",
      );
      expect(badge).not.toHaveClass("bg-white");
    });

    it("debe aplicar variante outline correctamente", () => {
      render(
        <EventoBadge tipo="vacacion" variant="outline">
          Vacaciones
        </EventoBadge>,
      );

      const badge = screen.getByText("Vacaciones");
      expect(badge).toHaveClass("bg-white", "text-blue-700", "border-blue-500");
      expect(badge).not.toHaveClass("bg-blue-100");
    });
  });

  describe("Tamaños", () => {
    it("debe usar tamaño sm por defecto", () => {
      render(<EventoBadge tipo="ausencia">Ausencia</EventoBadge>);

      const badge = screen.getByText("Ausencia");
      expect(badge).toHaveClass("text-xs", "px-2", "py-0.5");
      expect(badge).not.toHaveClass("text-sm", "px-3", "py-1");
    });

    it("debe aplicar tamaño md correctamente", () => {
      render(
        <EventoBadge tipo="libre" size="md">
          Día libre
        </EventoBadge>,
      );

      const badge = screen.getByText("Día libre");
      expect(badge).toHaveClass("text-sm", "px-3", "py-1");
      expect(badge).not.toHaveClass("text-xs", "px-2", "py-0.5");
    });
  });

  describe("Clases comunes", () => {
    it("debe tener clases base correctas", () => {
      render(<EventoBadge tipo="festivo">Festivo</EventoBadge>);

      const badge = screen.getByText("Festivo");
      expect(badge).toHaveClass(
        "inline-flex",
        "items-center",
        "rounded-md",
        "border",
        "font-medium",
      );
    });

    it("debe combinar clases de tipo, variante y tamaño", () => {
      render(
        <EventoBadge tipo="vacacion" variant="outline" size="md">
          Vacaciones
        </EventoBadge>,
      );

      const badge = screen.getByText("Vacaciones");
      expect(badge).toHaveClass(
        "inline-flex",
        "items-center",
        "rounded-md",
        "border",
        "font-medium",
        "bg-white",
        "text-blue-700",
        "border-blue-500",
        "text-sm",
        "px-3",
        "py-1",
      );
    });
  });

  describe("Accesibilidad", () => {
    it("debe ser un elemento span accesible", () => {
      render(<EventoBadge tipo="ausencia">Ausencia</EventoBadge>);

      const badge = screen.getByText("Ausencia");
      expect(badge.tagName).toBe("SPAN");
      expect(badge).toBeInTheDocument();
    });
  });
});
