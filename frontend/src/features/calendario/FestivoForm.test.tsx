/**
 * FestivoForm — Tests del formulario de festivo
 * Tests unitarios para el componente FestivoForm
 */

import type { FestivoRequest, PersonaResponse } from "@/types/api";
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { FestivoForm } from "./FestivoForm";

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
  {
    id: 2,
    nombre: "María García",
    email: "maria@example.com",
    perfilHorarioId: 1,
    perfilHorarioNombre: "Estándar",
    seniority: "JUNIOR",
    activo: true,
    sendNotifications: false,
    createdAt: "2024-01-01T00:00:00Z",
    updatedAt: "2024-01-01T00:00:00Z",
  },
  {
    id: 3,
    nombre: "Pedro López",
    email: "pedro@example.com",
    perfilHorarioId: 1,
    perfilHorarioNombre: "Estándar",
    seniority: "SENIOR",
    activo: true,
    sendNotifications: true,
    createdAt: "2024-01-01T00:00:00Z",
    updatedAt: "2024-01-01T00:00:00Z",
  },
  {
    id: 4,
    nombre: "Ana Rodríguez",
    email: "ana@example.com",
    perfilHorarioId: 1,
    perfilHorarioNombre: "Estándar",
    seniority: "MID",
    activo: true,
    sendNotifications: true,
    createdAt: "2024-01-01T00:00:00Z",
    updatedAt: "2024-01-01T00:00:00Z",
  },
];

const mockFestivo: FestivoRequest & { id?: number } = {
  id: 1,
  fecha: "2024-10-12",
  descripcion: "Día de la Hispanidad",
  tipo: "NACIONAL",
  personaIds: [1, 2],
};

describe("FestivoForm", () => {
  const mockOnSubmit = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("Renderizado inicial (crear)", () => {
    it("debe renderizar el formulario con campos vacíos por defecto", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Verificar título
      expect(screen.getByText("Nuevo festivo")).toBeInTheDocument();

      // Verificar campos
      expect(screen.getByLabelText("Fecha *")).toBeInTheDocument();
      expect(screen.getByLabelText("Descripción *")).toBeInTheDocument();
      expect(screen.getByLabelText("Tipo *")).toBeInTheDocument();
      expect(screen.getByLabelText("Personas afectadas *")).toBeInTheDocument();

      // Verificar campo de búsqueda
      expect(
        screen.getByPlaceholderText("Buscar persona..."),
      ).toBeInTheDocument();

      // Verificar botones
      expect(screen.getByText("Guardar")).toBeInTheDocument();
      expect(screen.getByText("Cancelar")).toBeInTheDocument();
    });

    it("debe mostrar tipos de festivo con emojis", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByText("🇪🇸 Nacional")).toBeInTheDocument();
      expect(screen.getByText("📍 Regional")).toBeInTheDocument();
      expect(screen.getByText("🏘️ Local")).toBeInTheDocument();
    });

    it("debe tener fecha de hoy como valor por defecto", () => {
      const today = new Date().toISOString().split("T")[0];

      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const fechaInput = screen.getByLabelText("Fecha *");
      expect(fechaInput).toHaveValue(today);
    });

    it("debe tener tipo NACIONAL como valor por defecto", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const tipoSelect = screen.getByRole("combobox", { name: "Tipo *" });
      expect(tipoSelect).toHaveValue("NACIONAL");
    });

    it("debe mostrar contador de 0 personas seleccionadas inicialmente", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByText("0 personas seleccionadas")).toBeInTheDocument();
    });
  });

  describe("Modo edición", () => {
    it("debe renderizar con datos pre-cargados cuando se pasa festivo", () => {
      render(
        <FestivoForm
          festivo={mockFestivo}
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Verificar título
      expect(screen.getByText("Editar festivo")).toBeInTheDocument();

      // Verificar valores pre-cargados
      const fechaInput = screen.getByLabelText("Fecha *");
      expect(fechaInput).toHaveValue("2024-10-12");

      const descripcionInput = screen.getByLabelText("Descripción *");
      expect(descripcionInput).toHaveValue("Día de la Hispanidad");

      const tipoSelect = screen.getByRole("combobox", { name: "Tipo *" });
      expect(tipoSelect).toHaveValue("NACIONAL");
    });

    it("debe mostrar personas seleccionadas como chips", () => {
      render(
        <FestivoForm
          festivo={mockFestivo}
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByText("Juan Pérez")).toBeInTheDocument();
      expect(screen.getByText("María García")).toBeInTheDocument();
      expect(screen.getByText("2 personas seleccionadas")).toBeInTheDocument();
    });

    it("debe permitir quitar personas con el botón ×", () => {
      render(
        <FestivoForm
          festivo={mockFestivo}
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const removeButton = screen.getAllByText("×")[0]; // Primer botón ×
      fireEvent.click(removeButton);

      expect(screen.getByText("1 personas seleccionadas")).toBeInTheDocument();
      expect(screen.queryByText("Juan Pérez")).not.toBeInTheDocument();
    });
  });

  describe("Selección de personas", () => {
    it("debe mostrar lista de personas al escribir en búsqueda", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const searchInput = screen.getByPlaceholderText("Buscar persona...");
      fireEvent.change(searchInput, { target: { value: "Juan" } });

      expect(screen.getByText("Juan Pérez")).toBeInTheDocument();
      expect(screen.getByText("juan@example.com")).toBeInTheDocument();
    });

    it("debe filtrar personas por nombre", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const searchInput = screen.getByPlaceholderText("Buscar persona...");
      fireEvent.change(searchInput, { target: { value: "María" } });

      expect(screen.getByText("María García")).toBeInTheDocument();
      expect(screen.queryByText("Juan Pérez")).not.toBeInTheDocument();
    });

    it("debe filtrar personas por email", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const searchInput = screen.getByPlaceholderText("Buscar persona...");
      fireEvent.change(searchInput, { target: { value: "ana@example.com" } });

      expect(screen.getByText("Ana Rodríguez")).toBeInTheDocument();
      expect(screen.queryByText("Juan Pérez")).not.toBeInTheDocument();
    });

    it("debe agregar persona al hacer click en ella", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const searchInput = screen.getByPlaceholderText("Buscar persona...");
      fireEvent.change(searchInput, { target: { value: "Juan" } });

      const personaButton = screen.getByText("Juan Pérez");
      fireEvent.click(personaButton);

      expect(screen.getByText("Juan Pérez")).toBeInTheDocument();
      expect(screen.getByText("1 personas seleccionadas")).toBeInTheDocument();
      expect(searchInput).toHaveValue(""); // Se limpia la búsqueda
    });

    it("debe quitar persona al hacer click en ×", () => {
      render(
        <FestivoForm
          festivo={mockFestivo}
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByText("2 personas seleccionadas")).toBeInTheDocument();

      const removeButton = screen.getAllByText("×")[0];
      fireEvent.click(removeButton);

      expect(screen.getByText("1 personas seleccionadas")).toBeInTheDocument();
    });

    it("no debe mostrar personas ya seleccionadas en la búsqueda", () => {
      render(
        <FestivoForm
          festivo={mockFestivo}
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const searchInput = screen.getByPlaceholderText("Buscar persona...");
      fireEvent.change(searchInput, { target: { value: "Juan" } });

      // Juan ya está seleccionado, no debe aparecer en la lista
      expect(screen.queryByText("Juan Pérez")).not.toBeInTheDocument();
    });
  });

  describe("Validación de formulario", () => {
    it("debe mostrar alerta cuando fecha está vacía", () => {
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Vaciar fecha
      const fechaInput = screen.getByLabelText("Fecha *");
      fireEvent.change(fechaInput, { target: { value: "" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(alertMock).toHaveBeenCalledWith("La fecha es obligatoria");

      alertMock.mockRestore();
    });

    it("debe mostrar alerta cuando descripción está vacía", () => {
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Vaciar descripción
      const descripcionInput = screen.getByLabelText("Descripción *");
      fireEvent.change(descripcionInput, { target: { value: "" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(alertMock).toHaveBeenCalledWith("La descripción es obligatoria");

      alertMock.mockRestore();
    });

    it("debe mostrar alerta cuando descripción solo tiene espacios", () => {
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Descripción con solo espacios
      const descripcionInput = screen.getByLabelText("Descripción *");
      fireEvent.change(descripcionInput, { target: { value: "   " } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(alertMock).toHaveBeenCalledWith("La descripción es obligatoria");

      alertMock.mockRestore();
    });

    it("debe mostrar alerta cuando no se selecciona ninguna persona", () => {
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Llenar campos obligatorios
      const fechaInput = screen.getByLabelText("Fecha *");
      fireEvent.change(fechaInput, { target: { value: "2024-10-12" } });

      const descripcionInput = screen.getByLabelText("Descripción *");
      fireEvent.change(descripcionInput, { target: { value: "Día festivo" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(alertMock).toHaveBeenCalledWith(
        "Debes seleccionar al menos una persona",
      );

      alertMock.mockRestore();
    });
  });

  describe("Envío de formulario", () => {
    it("debe llamar onSubmit con datos correctos en modo creación", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Llenar formulario
      const fechaInput = screen.getByLabelText("Fecha *");
      fireEvent.change(fechaInput, { target: { value: "2024-10-12" } });

      const descripcionInput = screen.getByLabelText("Descripción *");
      fireEvent.change(descripcionInput, {
        target: { value: "Día de la Hispanidad" },
      });

      const tipoSelect = screen.getByRole("combobox", { name: "Tipo *" });
      fireEvent.change(tipoSelect, { target: { value: "REGIONAL" } });

      // Seleccionar personas
      const searchInput = screen.getByPlaceholderText("Buscar persona...");
      fireEvent.change(searchInput, { target: { value: "Juan" } });
      const personaButton = screen.getByText("Juan Pérez");
      fireEvent.click(personaButton);

      fireEvent.change(searchInput, { target: { value: "María" } });
      const personaButton2 = screen.getByText("María García");
      fireEvent.click(personaButton2);

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(mockOnSubmit).toHaveBeenCalledWith({
        fecha: "2024-10-12",
        descripcion: "Día de la Hispanidad",
        tipo: "REGIONAL",
        personaIds: [1, 2],
      });
    });

    it("debe usar valores por defecto para tipo cuando no se cambia", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Llenar campos obligatorios mínimos
      const fechaInput = screen.getByLabelText("Fecha *");
      fireEvent.change(fechaInput, { target: { value: "2024-10-12" } });

      const descripcionInput = screen.getByLabelText("Descripción *");
      fireEvent.change(descripcionInput, { target: { value: "Festivo" } });

      // Seleccionar una persona
      const searchInput = screen.getByPlaceholderText("Buscar persona...");
      fireEvent.change(searchInput, { target: { value: "Juan" } });
      const personaButton = screen.getByText("Juan Pérez");
      fireEvent.click(personaButton);

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(mockOnSubmit).toHaveBeenCalledWith({
        fecha: "2024-10-12",
        descripcion: "Festivo",
        tipo: "NACIONAL", // Valor por defecto
        personaIds: [1],
      });
    });
  });

  describe("Interacciones de usuario", () => {
    it("debe llamar onCancel cuando se hace click en Cancelar", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const cancelButton = screen.getByText("Cancelar");
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    it("debe llamar onCancel cuando se hace click en la X", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const closeButton = screen.getByText("✕");
      fireEvent.click(closeButton);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    it("debe deshabilitar botones durante envío", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
          isSubmitting={true}
        />,
      );

      const saveButton = screen.getByText("Guardando...");
      const cancelButton = screen.getByText("Cancelar");

      expect(saveButton).toBeDisabled();
      expect(cancelButton).toBeDisabled();
    });
  });

  describe("Accesibilidad", () => {
    it("debe tener labels correctos para todos los campos", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByLabelText("Fecha *")).toBeInTheDocument();
      expect(screen.getByLabelText("Descripción *")).toBeInTheDocument();
      expect(screen.getByLabelText("Tipo *")).toBeInTheDocument();
      expect(screen.getByLabelText("Personas afectadas *")).toBeInTheDocument();
    });

    it("debe tener roles ARIA correctos", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByRole("form")).toBeInTheDocument();
      expect(
        screen.getByRole("combobox", { name: "Tipo *" }),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("textbox", { name: "Descripción *" }),
      ).toBeInTheDocument();
    });

    it("debe tener maxLength en descripción", () => {
      render(
        <FestivoForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const descripcionInput = screen.getByLabelText("Descripción *");
      expect(descripcionInput).toHaveAttribute("maxLength", "200");
    });
  });
});
