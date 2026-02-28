/**
 * Tests para PlantillaFormPanel y PlantillasPage (Bloque 5)
 * Cubre: TASK-024, CA-01 → CA-08 (plantillas de asignación)
 */

import {
  PlantillaFormPanel,
  PlantillasPage,
} from "@/routes/configuracion/plantillas";
import type { PlantillaAsignacionResponse } from "@/types/api";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

// ─────────────────────────────────────────────────────────────────
// Mocks
// ─────────────────────────────────────────────────────────────────

vi.mock("@/services/plantillaService", () => ({
  plantillaService: {
    listar: vi.fn(),
    crear: vi.fn(),
    actualizar: vi.fn(),
    eliminar: vi.fn(),
    aplicar: vi.fn(),
  },
}));

vi.mock("@/lib/toast", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}));

vi.mock("@/lib/useDocumentTitle", () => ({
  useDocumentTitle: vi.fn(),
}));

vi.mock("@/components/ui/ConfirmDialog", () => ({
  ConfirmDialog: ({ isOpen, onConfirm, onCancel }: any) =>
    isOpen ? (
      <div data-testid="confirm-dialog">
        <button onClick={onConfirm}>Confirmar</button>
        <button onClick={onCancel}>Cancelar confirmación</button>
      </div>
    ) : null,
}));

vi.mock("@tanstack/react-router", () => ({
  Link: ({ children, to }: any) => <a href={to}>{children}</a>,
  createFileRoute: () => (cfg: any) => cfg,
}));

vi.mock("lucide-react", async (importOriginal) => {
  const actual = await importOriginal<typeof import("lucide-react")>();
  return {
    ...actual,
    Plus: () => <span data-testid="icon-plus">+</span>,
    Save: () => <span data-testid="icon-save">💾</span>,
    Trash2: () => <span data-testid="icon-trash">🗑</span>,
    X: () => <span data-testid="icon-x">✕</span>,
    Settings: () => <span data-testid="icon-settings">⚙</span>,
    ChevronDown: () => <span data-testid="icon-chevron-down">▼</span>,
    ChevronRight: () => <span data-testid="icon-chevron-right">▶</span>,
  };
});

// ─────────────────────────────────────────────────────────────────
// Fixtures
// ─────────────────────────────────────────────────────────────────

const mockPlantilla: PlantillaAsignacionResponse = {
  id: 1,
  nombre: "Story estándar",
  tipoJira: "Story",
  activo: true,
  lineas: [
    {
      id: 10,
      rol: "DESARROLLADOR",
      porcentajeHoras: 70,
      orden: 1,
      dependeDeOrden: null,
    },
    { id: 11, rol: "QA", porcentajeHoras: 30, orden: 2, dependeDeOrden: null },
  ],
};

// ─────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────

function makeQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

function renderWithQueryClient(ui: React.ReactElement) {
  const qc = makeQueryClient();
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

// ─────────────────────────────────────────────────────────────────
// PlantillaFormPanel
// ─────────────────────────────────────────────────────────────────

describe("PlantillaFormPanel", () => {
  const onSubmit = vi.fn();
  const onCancel = vi.fn();

  beforeEach(() => {
    onSubmit.mockClear();
    onCancel.mockClear();
  });

  it("muestra 'Nueva plantilla' cuando no hay initial", () => {
    render(
      <PlantillaFormPanel
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={false}
      />,
    );
    expect(screen.getByText("Nueva plantilla")).toBeInTheDocument();
  });

  it("muestra 'Editar plantilla' cuando se pasa initial", () => {
    render(
      <PlantillaFormPanel
        initial={mockPlantilla}
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={false}
      />,
    );
    expect(screen.getByText("Editar plantilla")).toBeInTheDocument();
  });

  it("botón de submit muestra 'Crear plantilla' cuando no hay initial", () => {
    render(
      <PlantillaFormPanel
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={false}
      />,
    );
    expect(screen.getByText("Crear plantilla")).toBeInTheDocument();
  });

  it("botón de submit muestra 'Actualizar' cuando hay initial", () => {
    render(
      <PlantillaFormPanel
        initial={mockPlantilla}
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={false}
      />,
    );
    expect(screen.getByText("Actualizar")).toBeInTheDocument();
  });

  it("botón de submit desactivado cuando totalPct ≠ 100 (estado inicial vacío)", () => {
    render(
      <PlantillaFormPanel
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={false}
      />,
    );
    const submitBtn = screen.getByText("Crear plantilla").closest("button");
    expect(submitBtn).toBeDisabled();
  });

  it("botón de submit habilitado cuando totalPct === 100 (modal con initial válido)", () => {
    render(
      <PlantillaFormPanel
        initial={mockPlantilla}
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={false}
      />,
    );
    const submitBtn = screen.getByText("Actualizar").closest("button");
    expect(submitBtn).not.toBeDisabled();
  });

  it("'Añadir línea' añade una nueva fila", () => {
    render(
      <PlantillaFormPanel
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={false}
      />,
    );
    // Initially 1 delete line button
    const deleteButtons = screen.getAllByRole("button", {
      name: /Eliminar línea/,
    });
    expect(deleteButtons).toHaveLength(1);

    fireEvent.click(screen.getByText("Añadir línea"));

    const deleteButtonsAfter = screen.getAllByRole("button", {
      name: /Eliminar línea/,
    });
    expect(deleteButtonsAfter).toHaveLength(2);
  });

  it("botón 'Eliminar línea 1' está desactivado cuando solo hay 1 línea", () => {
    render(
      <PlantillaFormPanel
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={false}
      />,
    );
    const deleteBtn = screen.getByRole("button", { name: "Eliminar línea 1" });
    expect(deleteBtn).toBeDisabled();
  });

  it("botón Cancelar (pie del formulario) llama a onCancel", () => {
    render(
      <PlantillaFormPanel
        onSubmit={onSubmit}
        onCancel={onCancel}
        isSubmitting={false}
      />,
    );
    // The text "Cancelar" button at the bottom (second Cancelar, first is the X icon with aria-label)
    const cancelBtns = screen.getAllByRole("button", { name: "Cancelar" });
    const cancelBtn = cancelBtns[cancelBtns.length - 1];
    fireEvent.click(cancelBtn);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});

// ─────────────────────────────────────────────────────────────────
// PlantillasPage
// ─────────────────────────────────────────────────────────────────

describe("PlantillasPage", () => {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  let plantillaService: any;

  beforeEach(async () => {
    const mod = await import("@/services/plantillaService");
    plantillaService = (mod as any).plantillaService;
    vi.clearAllMocks();
  });

  it("muestra el título 'Plantillas de asignación'", async () => {
    plantillaService.listar.mockResolvedValue([]);
    renderWithQueryClient(<PlantillasPage />);
    await waitFor(() => {
      expect(screen.getByText("Plantillas de asignación")).toBeInTheDocument();
    });
  });

  it("muestra el botón 'Nueva plantilla'", async () => {
    plantillaService.listar.mockResolvedValue([]);
    renderWithQueryClient(<PlantillasPage />);
    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: /Nueva plantilla/ }),
      ).toBeInTheDocument();
    });
  });

  it("muestra el nombre de una plantilla cuando está cargada", async () => {
    plantillaService.listar.mockResolvedValue([mockPlantilla]);
    renderWithQueryClient(<PlantillasPage />);
    await waitFor(() => {
      expect(screen.getByText("Story estándar")).toBeInTheDocument();
    });
  });

  it("muestra el tipo Jira de la plantilla", async () => {
    plantillaService.listar.mockResolvedValue([mockPlantilla]);
    renderWithQueryClient(<PlantillasPage />);
    await waitFor(() => {
      expect(screen.getByText("Story")).toBeInTheDocument();
    });
  });

  it("click en 'Nueva plantilla' muestra el formulario de creación", async () => {
    plantillaService.listar.mockResolvedValue([]);
    renderWithQueryClient(<PlantillasPage />);
    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: /Nueva plantilla/ }),
      ).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Nueva plantilla/ }));
    expect(
      screen.getByText("Nueva plantilla", { selector: "h3" }),
    ).toBeInTheDocument();
  });

  it("click en 'Editar' abre el formulario con 'Editar plantilla'", async () => {
    plantillaService.listar.mockResolvedValue([mockPlantilla]);
    renderWithQueryClient(<PlantillasPage />);
    await waitFor(() => {
      expect(screen.getByText("Story estándar")).toBeInTheDocument();
    });

    const editBtn = screen.getByRole("button", {
      name: `Editar ${mockPlantilla.nombre}`,
    });
    fireEvent.click(editBtn);

    expect(screen.getByText("Editar plantilla")).toBeInTheDocument();
  });

  it("click en 'Eliminar' abre el confirm dialog", async () => {
    plantillaService.listar.mockResolvedValue([mockPlantilla]);
    renderWithQueryClient(<PlantillasPage />);
    await waitFor(() => {
      expect(screen.getByText("Story estándar")).toBeInTheDocument();
    });

    const deleteBtn = screen.getByRole("button", {
      name: `Eliminar ${mockPlantilla.nombre}`,
    });
    fireEvent.click(deleteBtn);

    expect(screen.getByTestId("confirm-dialog")).toBeInTheDocument();
  });

  it("confirmar eliminación llama a plantillaService.eliminar", async () => {
    plantillaService.listar.mockResolvedValue([mockPlantilla]);
    plantillaService.eliminar.mockResolvedValue(undefined);
    renderWithQueryClient(<PlantillasPage />);
    await waitFor(() => {
      expect(screen.getByText("Story estándar")).toBeInTheDocument();
    });

    fireEvent.click(
      screen.getByRole("button", { name: `Eliminar ${mockPlantilla.nombre}` }),
    );
    await waitFor(() =>
      expect(screen.getByTestId("confirm-dialog")).toBeInTheDocument(),
    );

    fireEvent.click(screen.getByRole("button", { name: "Confirmar" }));
    await waitFor(() => {
      expect(plantillaService.eliminar).toHaveBeenCalledWith(
        mockPlantilla.id,
        expect.anything(),
      );
    });
  });

  it("cancelar eliminación oculta el confirm dialog", async () => {
    plantillaService.listar.mockResolvedValue([mockPlantilla]);
    renderWithQueryClient(<PlantillasPage />);
    await waitFor(() => {
      expect(screen.getByText("Story estándar")).toBeInTheDocument();
    });

    fireEvent.click(
      screen.getByRole("button", { name: `Eliminar ${mockPlantilla.nombre}` }),
    );
    expect(screen.getByTestId("confirm-dialog")).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: "Cancelar confirmación" }),
    );
    expect(screen.queryByTestId("confirm-dialog")).not.toBeInTheDocument();
  });

  it("sin plantillas muestra mensaje 'No hay plantillas creadas todavía'", async () => {
    plantillaService.listar.mockResolvedValue([]);
    renderWithQueryClient(<PlantillasPage />);
    await waitFor(() => {
      expect(
        screen.getByText("No hay plantillas creadas todavía."),
      ).toBeInTheDocument();
    });
  });
});
