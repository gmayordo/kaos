/**
 * AusenciaForm — Tests del formulario de ausencia
 * Tests unitarios para el componente AusenciaForm
 */

import type { AusenciaRequest, PersonaResponse } from "@/types/api";
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
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
    activo: false, // Inactive
    sendNotifications: true,
    createdAt: "2024-01-01T00:00:00Z",
    updatedAt: "2024-01-01T00:00:00Z",
  },
];

const mockAusencia: AusenciaRequest & { id?: number } = {
  id: 1,
  personaId: 1,
  fechaInicio: "2024-03-01",
  fechaFin: "2024-03-10",
  tipo: "BAJA_MEDICA",
  comentario: "Baja por gripe",
};

describe("AusenciaForm", () => {
  const mockOnSubmit = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("Renderizado inicial (crear)", () => {
    it("debe renderizar el formulario con campos vacíos por defecto", () => {
      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Verificar título
      expect(screen.getByText("Registrar ausencia")).toBeInTheDocument();

      // Verificar campos
      expect(screen.getByLabelText("Persona *")).toBeInTheDocument();
      expect(screen.getByLabelText("Fecha inicio *")).toBeInTheDocument();
      expect(screen.getByLabelText("Fecha fin")).toBeInTheDocument();
      expect(screen.getByLabelText("Tipo *")).toBeInTheDocument();
      expect(screen.getByLabelText("Comentario")).toBeInTheDocument();

      // Verificar botones
      expect(screen.getByText("Guardar")).toBeInTheDocument();
      expect(screen.getByText("Cancelar")).toBeInTheDocument();
    });

    it("debe mostrar solo personas activas en el select", () => {
      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const select = screen.getByRole("combobox", { name: "Persona *" });
      const options = screen.getAllByRole("option");

      // Debe haber 3 opciones: placeholder + 2 personas activas
      expect(options).toHaveLength(3);
      expect(options[0]).toHaveTextContent("Seleccionar persona...");
      expect(options[1]).toHaveTextContent("Juan Pérez");
      expect(options[2]).toHaveTextContent("María García");
      // Pedro López (inactivo) no debe aparecer
    });

    it("debe mostrar tipos de ausencia correctos", () => {
      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const tipoSelect = screen.getByRole("combobox", { name: "Tipo *" });
      fireEvent.change(tipoSelect, { target: { value: "" } }); // Abrir opciones

      expect(screen.getByText("Baja médica")).toBeInTheDocument();
      expect(screen.getByText("Emergencia")).toBeInTheDocument();
      expect(screen.getByText("Otro")).toBeInTheDocument();
    });

    it("debe tener fecha de hoy como valor por defecto en fechaInicio", () => {
      const today = new Date().toISOString().split("T")[0];

      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      expect(fechaInicioInput).toHaveValue(today);
    });
  });

  describe("Modo edición", () => {
    it("debe renderizar con datos pre-cargados cuando se pasa ausencia", () => {
      render(
        <AusenciaForm
          ausencia={mockAusencia}
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Verificar título
      expect(screen.getByText("Editar ausencia")).toBeInTheDocument();

      // Verificar valores pre-cargados
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      expect(personaSelect).toHaveValue("1");

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      expect(fechaInicioInput).toHaveValue("2024-03-01");

      const fechaFinInput = screen.getByLabelText("Fecha fin");
      expect(fechaFinInput).toHaveValue("2024-03-10");

      const tipoSelect = screen.getByRole("combobox", { name: "Tipo *" });
      expect(tipoSelect).toHaveValue("BAJA_MEDICA");

      const comentarioTextarea = screen.getByLabelText("Comentario");
      expect(comentarioTextarea).toHaveValue("Baja por gripe");
    });

    it("debe mostrar alerta de ausencia indefinida cuando fechaFin es null", () => {
      const ausenciaIndefinida = { ...mockAusencia, fechaFin: undefined };

      render(
        <AusenciaForm
          ausencia={ausenciaIndefinida}
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(
        screen.getByText("⚠️ Ausencia indefinida (sin fecha fin)"),
      ).toBeInTheDocument();
    });
  });

  describe("Validación de formulario", () => {
    it("debe mostrar alerta cuando no se selecciona persona", async () => {
      // Mock window.alert
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(alertMock).toHaveBeenCalledWith("Debes seleccionar una persona");

      alertMock.mockRestore();
    });

    it("debe mostrar alerta cuando fechaInicio está vacía", async () => {
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Seleccionar persona
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      fireEvent.change(personaSelect, { target: { value: "1" } });

      // Vaciar fechaInicio
      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      fireEvent.change(fechaInicioInput, { target: { value: "" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(alertMock).toHaveBeenCalledWith(
        "La fecha de inicio es obligatoria",
      );

      alertMock.mockRestore();
    });

    it("debe mostrar alerta cuando fechaFin es anterior a fechaInicio", async () => {
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Seleccionar persona
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      fireEvent.change(personaSelect, { target: { value: "1" } });

      // Fechas inválidas
      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      fireEvent.change(fechaInicioInput, { target: { value: "2024-03-10" } });

      const fechaFinInput = screen.getByLabelText("Fecha fin");
      fireEvent.change(fechaFinInput, { target: { value: "2024-03-05" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(alertMock).toHaveBeenCalledWith(
        "La fecha de fin debe ser posterior o igual a la fecha de inicio",
      );

      alertMock.mockRestore();
    });

    it("debe permitir fechaFin igual a fechaInicio", async () => {
      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Seleccionar persona
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      fireEvent.change(personaSelect, { target: { value: "1" } });

      // Fechas iguales (válido)
      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      fireEvent.change(fechaInicioInput, { target: { value: "2024-03-10" } });

      const fechaFinInput = screen.getByLabelText("Fecha fin");
      fireEvent.change(fechaFinInput, { target: { value: "2024-03-10" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(mockOnSubmit).toHaveBeenCalledWith({
        personaId: 1,
        fechaInicio: "2024-03-10",
        fechaFin: "2024-03-10",
        tipo: "BAJA_MEDICA",
        comentario: "",
      });
    });
  });

  describe("Envío de formulario", () => {
    it("debe llamar onSubmit con datos correctos en modo creación", () => {
      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Llenar formulario
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      fireEvent.change(personaSelect, { target: { value: "1" } });

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      fireEvent.change(fechaInicioInput, { target: { value: "2024-03-01" } });

      const fechaFinInput = screen.getByLabelText("Fecha fin");
      fireEvent.change(fechaFinInput, { target: { value: "2024-03-10" } });

      const tipoSelect = screen.getByRole("combobox", { name: "Tipo *" });
      fireEvent.change(tipoSelect, { target: { value: "EMERGENCIA" } });

      const comentarioTextarea = screen.getByLabelText("Comentario");
      fireEvent.change(comentarioTextarea, {
        target: { value: "Emergencia familiar" },
      });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(mockOnSubmit).toHaveBeenCalledWith({
        personaId: 1,
        fechaInicio: "2024-03-01",
        fechaFin: "2024-03-10",
        tipo: "EMERGENCIA",
        comentario: "Emergencia familiar",
      });
    });

    it("debe manejar fechaFin undefined correctamente", () => {
      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Llenar formulario sin fechaFin
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      fireEvent.change(personaSelect, { target: { value: "1" } });

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      fireEvent.change(fechaInicioInput, { target: { value: "2024-03-01" } });

      // Dejar fechaFin vacío
      const fechaFinInput = screen.getByLabelText("Fecha fin");
      fireEvent.change(fechaFinInput, { target: { value: "" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(mockOnSubmit).toHaveBeenCalledWith({
        personaId: 1,
        fechaInicio: "2024-03-01",
        fechaFin: undefined,
        tipo: "BAJA_MEDICA",
        comentario: "",
      });
    });

    it("debe mostrar alerta de ausencia indefinida cuando fechaFin está vacío", () => {
      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Llenar formulario sin fechaFin
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      fireEvent.change(personaSelect, { target: { value: "1" } });

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      fireEvent.change(fechaInicioInput, { target: { value: "2024-03-01" } });

      // Dejar fechaFin vacío
      const fechaFinInput = screen.getByLabelText("Fecha fin");
      fireEvent.change(fechaFinInput, { target: { value: "" } });

      expect(
        screen.getByText("⚠️ Ausencia indefinida (sin fecha fin)"),
      ).toBeInTheDocument();
    });
  });

  describe("Interacciones de usuario", () => {
    it("debe llamar onCancel cuando se hace click en Cancelar", () => {
      render(
        <AusenciaForm
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
        <AusenciaForm
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
        <AusenciaForm
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

  describe("Filtrado por squad", () => {
    it("debe filtrar personas por squadId cuando se proporciona", () => {
      const personasConSquad = [
        { ...mockPersonas[0], squadId: 1 },
        { ...mockPersonas[1], squadId: 2 },
      ];

      render(
        <AusenciaForm
          personas={personasConSquad}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
          squadId={1}
        />,
      );

      const select = screen.getByRole("combobox", { name: "Persona *" });
      const options = screen.getAllByRole("option");

      // Solo debe mostrar personas del squad 1
      expect(options).toHaveLength(2); // placeholder + 1 persona
      expect(options[1]).toHaveTextContent("Juan Pérez");
    });
  });

  describe("Accesibilidad", () => {
    it("debe tener labels correctos para todos los campos", () => {
      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByLabelText("Persona *")).toBeInTheDocument();
      expect(screen.getByLabelText("Fecha inicio *")).toBeInTheDocument();
      expect(screen.getByLabelText("Fecha fin")).toBeInTheDocument();
      expect(screen.getByLabelText("Tipo *")).toBeInTheDocument();
      expect(screen.getByLabelText("Comentario")).toBeInTheDocument();
    });

    it("debe tener roles ARIA correctos", () => {
      render(
        <AusenciaForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // El form no tiene role="form" explícito, pero podemos verificar que existe
      const form = document.querySelector("form");
      expect(form).toBeInTheDocument();

      expect(screen.getAllByRole("combobox")).toHaveLength(2); // persona y tipo
      expect(screen.getByRole("textbox")).toBeInTheDocument(); // comentario
    });
  });
});
