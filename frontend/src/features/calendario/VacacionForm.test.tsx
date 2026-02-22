/**
 * VacacionForm — Tests del formulario de vacación
 * Tests unitarios para el componente VacacionForm
 */

import type { PersonaResponse, VacacionRequest } from "@/types/api";
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { VacacionForm } from "./VacacionForm";

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

const mockVacacion: VacacionRequest & { id?: number } = {
  id: 1,
  personaId: 1,
  fechaInicio: "2024-08-01",
  fechaFin: "2024-08-15",
  tipo: "VACACIONES",
  estado: "REGISTRADA",
  comentario: "Vacaciones de verano",
};

describe("VacacionForm", () => {
  const mockOnSubmit = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("Renderizado inicial (crear)", () => {
    it("debe renderizar el formulario con campos vacíos por defecto", () => {
      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Verificar título
      expect(screen.getByText("Registrar vacación")).toBeInTheDocument();

      // Verificar campos
      expect(screen.getByLabelText("Persona *")).toBeInTheDocument();
      expect(screen.getByLabelText("Fecha inicio *")).toBeInTheDocument();
      expect(screen.getByLabelText("Fecha fin *")).toBeInTheDocument();
      expect(screen.getByLabelText("Tipo *")).toBeInTheDocument();
      expect(screen.getByLabelText("Estado")).toBeInTheDocument();
      expect(screen.getByLabelText("Comentario")).toBeInTheDocument();

      // Verificar indicador de duración
      expect(screen.getByText(/Duración:/)).toBeInTheDocument();

      // Verificar botones
      expect(screen.getByText("Guardar")).toBeInTheDocument();
      expect(screen.getByText("Cancelar")).toBeInTheDocument();
    });

    it("debe mostrar solo personas activas en el select", () => {
      render(
        <VacacionForm
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

    it("debe mostrar tipos de vacación correctos", () => {
      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByText("Vacaciones")).toBeInTheDocument();
      expect(screen.getByText("Asuntos propios")).toBeInTheDocument();
      expect(screen.getByText("Libre disposición")).toBeInTheDocument();
      expect(screen.getByText("Permiso")).toBeInTheDocument();
    });

    it("debe mostrar estados de vacación correctos", () => {
      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByText("Solicitada")).toBeInTheDocument();
      expect(screen.getByText("Registrada")).toBeInTheDocument();
    });

    it("debe tener fecha de hoy como valor por defecto en fechaInicio y fechaFin", () => {
      const today = new Date().toISOString().split("T")[0];

      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      expect(fechaInicioInput).toHaveValue(today);

      const fechaFinInput = screen.getByLabelText("Fecha fin *");
      expect(fechaFinInput).toHaveValue(today);
    });

    it("debe mostrar duración de 1 día cuando fechaInicio = fechaFin", () => {
      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByText("1 días")).toBeInTheDocument();
    });
  });

  describe("Modo edición", () => {
    it("debe renderizar con datos pre-cargados cuando se pasa vacacion", () => {
      render(
        <VacacionForm
          vacacion={mockVacacion}
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Verificar título
      expect(screen.getByText("Editar vacación")).toBeInTheDocument();

      // Verificar valores pre-cargados
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      expect(personaSelect).toHaveValue("1");

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      expect(fechaInicioInput).toHaveValue("2024-08-01");

      const fechaFinInput = screen.getByLabelText("Fecha fin *");
      expect(fechaFinInput).toHaveValue("2024-08-15");

      const tipoSelect = screen.getByRole("combobox", { name: "Tipo *" });
      expect(tipoSelect).toHaveValue("VACACIONES");

      const estadoSelect = screen.getByRole("combobox", { name: "Estado" });
      expect(estadoSelect).toHaveValue("REGISTRADA");

      const comentarioTextarea = screen.getByLabelText("Comentario");
      expect(comentarioTextarea).toHaveValue("Vacaciones de verano");
    });

    it("debe calcular la duración correcta para el rango de fechas", () => {
      render(
        <VacacionForm
          vacacion={mockVacacion}
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Del 1 al 15 de agosto = 15 días
      expect(screen.getByText("15 días")).toBeInTheDocument();
    });
  });

  describe("Cálculo de duración (CA-10)", () => {
    it("debe calcular correctamente 1 día cuando fechas son iguales", () => {
      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      const fechaFinInput = screen.getByLabelText("Fecha fin *");

      fireEvent.change(fechaInicioInput, { target: { value: "2024-03-10" } });
      fireEvent.change(fechaFinInput, { target: { value: "2024-03-10" } });

      expect(screen.getByText("1 días")).toBeInTheDocument();
    });

    it("debe calcular correctamente múltiples días", () => {
      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      const fechaFinInput = screen.getByLabelText("Fecha fin *");

      // Del 1 al 10 = 10 días
      fireEvent.change(fechaInicioInput, { target: { value: "2024-03-01" } });
      fireEvent.change(fechaFinInput, { target: { value: "2024-03-10" } });

      expect(screen.getByText("10 días")).toBeInTheDocument();
    });

    it("debe recalcular duración al cambiar fechas", () => {
      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      const fechaFinInput = screen.getByLabelText("Fecha fin *");

      // Inicialmente 1 día (hoy)
      expect(screen.getByText("1 días")).toBeInTheDocument();

      // Cambiar a 5 días
      fireEvent.change(fechaInicioInput, { target: { value: "2024-03-01" } });
      fireEvent.change(fechaFinInput, { target: { value: "2024-03-05" } });

      expect(screen.getByText("5 días")).toBeInTheDocument();
    });
  });

  describe("Validación de formulario", () => {
    it("debe mostrar alerta cuando no se selecciona persona", () => {
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <VacacionForm
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

    it("debe mostrar alerta cuando fechaInicio está vacía", () => {
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <VacacionForm
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

    it("debe mostrar alerta cuando fechaFin está vacía", () => {
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Seleccionar persona
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      fireEvent.change(personaSelect, { target: { value: "1" } });

      // Vaciar fechaFin
      const fechaFinInput = screen.getByLabelText("Fecha fin *");
      fireEvent.change(fechaFinInput, { target: { value: "" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(alertMock).toHaveBeenCalledWith("La fecha de fin es obligatoria");

      alertMock.mockRestore();
    });

    it("debe mostrar alerta cuando fechaFin es anterior a fechaInicio", () => {
      const alertMock = vi.spyOn(window, "alert").mockImplementation(() => {});

      render(
        <VacacionForm
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

      const fechaFinInput = screen.getByLabelText("Fecha fin *");
      fireEvent.change(fechaFinInput, { target: { value: "2024-03-05" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(alertMock).toHaveBeenCalledWith(
        "La fecha de fin debe ser posterior o igual a la fecha de inicio",
      );

      alertMock.mockRestore();
    });

    it("debe permitir fechaFin igual a fechaInicio", () => {
      render(
        <VacacionForm
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

      const fechaFinInput = screen.getByLabelText("Fecha fin *");
      fireEvent.change(fechaFinInput, { target: { value: "2024-03-10" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(mockOnSubmit).toHaveBeenCalledWith({
        personaId: 1,
        fechaInicio: "2024-03-10",
        fechaFin: "2024-03-10",
        tipo: "VACACIONES",
        estado: "REGISTRADA",
        comentario: "",
      });
    });
  });

  describe("Envío de formulario", () => {
    it("debe llamar onSubmit con datos correctos en modo creación", () => {
      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Llenar formulario
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      fireEvent.change(personaSelect, { target: { value: "1" } });

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      fireEvent.change(fechaInicioInput, { target: { value: "2024-08-01" } });

      const fechaFinInput = screen.getByLabelText("Fecha fin *");
      fireEvent.change(fechaFinInput, { target: { value: "2024-08-15" } });

      const tipoSelect = screen.getByRole("combobox", { name: "Tipo *" });
      fireEvent.change(tipoSelect, { target: { value: "ASUNTOS_PROPIOS" } });

      const estadoSelect = screen.getByRole("combobox", { name: "Estado" });
      fireEvent.change(estadoSelect, { target: { value: "SOLICITADA" } });

      const comentarioTextarea = screen.getByLabelText("Comentario");
      fireEvent.change(comentarioTextarea, {
        target: { value: "Asuntos familiares" },
      });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(mockOnSubmit).toHaveBeenCalledWith({
        personaId: 1,
        fechaInicio: "2024-08-01",
        fechaFin: "2024-08-15",
        tipo: "ASUNTOS_PROPIOS",
        estado: "SOLICITADA",
        comentario: "Asuntos familiares",
      });
    });

    it("debe usar valores por defecto para tipo y estado", () => {
      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // Solo llenar campos obligatorios
      const personaSelect = screen.getByRole("combobox", { name: "Persona *" });
      fireEvent.change(personaSelect, { target: { value: "1" } });

      const fechaInicioInput = screen.getByLabelText("Fecha inicio *");
      fireEvent.change(fechaInicioInput, { target: { value: "2024-03-01" } });

      const fechaFinInput = screen.getByLabelText("Fecha fin *");
      fireEvent.change(fechaFinInput, { target: { value: "2024-03-01" } });

      const form = screen.getByRole("form");
      fireEvent.submit(form);

      expect(mockOnSubmit).toHaveBeenCalledWith({
        personaId: 1,
        fechaInicio: "2024-03-01",
        fechaFin: "2024-03-01",
        tipo: "VACACIONES", // Valor por defecto
        estado: "REGISTRADA", // Valor por defecto
        comentario: "",
      });
    });
  });

  describe("Interacciones de usuario", () => {
    it("debe llamar onCancel cuando se hace click en Cancelar", () => {
      render(
        <VacacionForm
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
        <VacacionForm
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
        <VacacionForm
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
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      expect(screen.getByLabelText("Persona *")).toBeInTheDocument();
      expect(screen.getByLabelText("Fecha inicio *")).toBeInTheDocument();
      expect(screen.getByLabelText("Fecha fin *")).toBeInTheDocument();
      expect(screen.getByLabelText("Tipo *")).toBeInTheDocument();
      expect(screen.getByLabelText("Estado")).toBeInTheDocument();
      expect(screen.getByLabelText("Comentario")).toBeInTheDocument();
    });

    it("debe tener roles ARIA correctos", () => {
      render(
        <VacacionForm
          personas={mockPersonas}
          onSubmit={mockOnSubmit}
          onCancel={mockOnCancel}
        />,
      );

      // El form no tiene role="form" explícito, pero podemos verificar que existe
      const form = document.querySelector("form");
      expect(form).toBeInTheDocument();

      expect(screen.getAllByRole("combobox")).toHaveLength(3); // persona, tipo, estado
      expect(screen.getByRole("textbox")).toBeInTheDocument(); // comentario
    });
  });
});
