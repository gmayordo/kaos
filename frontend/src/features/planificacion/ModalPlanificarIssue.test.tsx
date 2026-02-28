/**
 * ModalPlanificarIssue — 12 tests
 * Cubre: render título/summary/filas, subtareas, botón plantilla, cancelar,
 * planificar submit, tipos Jira → categorías KAOS, conteo issues.
 */

import * as jiraIssueServiceModule from "@/services/jiraIssueService";
import * as plantillaServiceModule from "@/services/plantillaService";
import type { JiraIssueResponse } from "@/types/jira";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ModalPlanificarIssue } from "./ModalPlanificarIssue";

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock("@/services/jiraIssueService");
vi.mock("@/services/plantillaService");

vi.mock("@/lib/toast", () => ({
  toast: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}));

vi.mock("@/components/ui/AccessibleModal", () => ({
  AccessibleModal: ({ children, title, isOpen }: any) =>
    isOpen ? (
      <div role="dialog" aria-label={title}>
        <h2>{title}</h2>
        {children}
      </div>
    ) : null,
}));

vi.mock("lucide-react", async (orig) => {
  const actual = await orig<typeof import("lucide-react")>();
  return {
    ...actual,
    Wand2: () => <span data-testid="icon-wand" />,
    Lightbulb: () => <span data-testid="icon-lightbulb" />,
  };
});

// ── Helpers ───────────────────────────────────────────────────────────────────

const buildQC = () =>
  new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

function wrap(node: React.ReactNode) {
  return <QueryClientProvider client={buildQC()}>{node}</QueryClientProvider>;
}

const mockPersonas = [
  { id: 1, nombre: "Ana García" },
  { id: 2, nombre: "Carlos López" },
];

function buildIssue(overrides?: Partial<JiraIssueResponse>): JiraIssueResponse {
  return {
    id: 1,
    jiraKey: "RED-42",
    summary: "Implementar login",
    tipoJira: "Story",
    asignadoNombre: "Ana García",
    asignadoJiraId: "ana.garcia",
    estimacionHoras: 8,
    horasConsumidas: 2,
    estadoJira: "To Do",
    estadoKaos: null,
    parentKey: null,
    sprintNombre: "Sprint 1",
    tareaId: null,
    tareaEstado: null,
    subtareas: [],
    ...overrides,
  };
}

const baseProps = {
  sprintId: 10,
  personas: mockPersonas,
  onSuccess: vi.fn(),
  onClose: vi.fn(),
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe("ModalPlanificarIssue", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(
      plantillaServiceModule.plantillaService.aplicar,
    ).mockResolvedValue([]);
  });

  it("muestra el título con la clave del issue", async () => {
    render(wrap(<ModalPlanificarIssue {...baseProps} issue={buildIssue()} />));
    await waitFor(() => {
      expect(screen.getByRole("dialog")).toHaveAttribute(
        "aria-label",
        "Planificar: RED-42",
      );
    });
  });

  it("muestra el summary del issue en el header", async () => {
    render(wrap(<ModalPlanificarIssue {...baseProps} issue={buildIssue()} />));
    await waitFor(() => {
      // "Implementar login" aparece en el panel de cabecera y en la fila de la tabla
      expect(screen.getAllByText("Implementar login").length).toBeGreaterThan(
        0,
      );
    });
  });

  it("sin subtareas → 1 fila (1 checkbox incluir)", async () => {
    render(wrap(<ModalPlanificarIssue {...baseProps} issue={buildIssue()} />));
    await waitFor(() => {
      // Solo el checkbox de incluir del padre
      expect(screen.getAllByRole("checkbox")).toHaveLength(1);
    });
  });

  it("con 2 subtareas → 3 filas en la tabla", async () => {
    const sub1 = buildIssue({
      id: 2,
      jiraKey: "RED-43",
      summary: "Subtarea 1",
      parentKey: "RED-42",
      subtareas: [],
    });
    const sub2 = buildIssue({
      id: 3,
      jiraKey: "RED-44",
      summary: "Subtarea 2",
      parentKey: "RED-42",
      subtareas: [],
    });
    const padre = buildIssue({ subtareas: [sub1, sub2] });

    render(wrap(<ModalPlanificarIssue {...baseProps} issue={padre} />));
    await waitFor(() => {
      expect(screen.getAllByRole("checkbox")).toHaveLength(3);
    });
  });

  it("muestra la estimación Jira en el header", async () => {
    render(
      wrap(
        <ModalPlanificarIssue
          {...baseProps}
          issue={buildIssue({ estimacionHoras: 8 })}
        />,
      ),
    );
    await waitFor(() => {
      expect(screen.getByText("8h Jira")).toBeInTheDocument();
    });
  });

  it("botón Cancelar llama a onClose", async () => {
    render(wrap(<ModalPlanificarIssue {...baseProps} issue={buildIssue()} />));
    await waitFor(() => screen.getByText("Cancelar"));
    fireEvent.click(screen.getByText("Cancelar"));
    expect(baseProps.onClose).toHaveBeenCalledTimes(1);
  });

  it("botón Planificar llama a planificar con sprintId correcto", async () => {
    vi.mocked(
      jiraIssueServiceModule.jiraIssueService.planificar,
    ).mockResolvedValue([]);
    render(wrap(<ModalPlanificarIssue {...baseProps} issue={buildIssue()} />));
    await waitFor(() => screen.getByText("Planificar"));
    fireEvent.click(screen.getByText("Planificar"));
    await waitFor(() => {
      expect(
        jiraIssueServiceModule.jiraIssueService.planificar,
      ).toHaveBeenCalledWith(expect.objectContaining({ sprintId: 10 }));
    });
  });

  it("cuando plantilla tiene ítems → muestra botón 'Plantilla'", async () => {
    vi.mocked(
      plantillaServiceModule.plantillaService.aplicar,
    ).mockResolvedValue([
      {
        jiraKey: "RED-42",
        personaId: undefined,
        estimacion: 5.6,
        diaAsignado: undefined,
        tipo: "HISTORIA",
        categoria: "EVOLUTIVO",
        prioridad: "NORMAL",
      },
    ]);
    render(wrap(<ModalPlanificarIssue {...baseProps} issue={buildIssue()} />));
    await waitFor(() => {
      expect(
        screen.getByTitle("Aplicar plantilla de asignación"),
      ).toBeInTheDocument();
    });
  });

  it("cuando plantilla vacía → no muestra botón 'Plantilla'", async () => {
    vi.mocked(
      plantillaServiceModule.plantillaService.aplicar,
    ).mockResolvedValue([]);
    render(wrap(<ModalPlanificarIssue {...baseProps} issue={buildIssue()} />));
    await waitFor(() => screen.getByText("Planificar"));
    expect(
      screen.queryByTitle("Aplicar plantilla de asignación"),
    ).not.toBeInTheDocument();
  });

  it("muestra '1 issue a planificar' con fila única", async () => {
    render(wrap(<ModalPlanificarIssue {...baseProps} issue={buildIssue()} />));
    await waitFor(() => {
      expect(screen.getByText("1 issue a planificar")).toBeInTheDocument();
    });
  });

  it("tipo Story → tipo KAOS Historia preseleccionado", async () => {
    render(
      wrap(
        <ModalPlanificarIssue
          {...baseProps}
          issue={buildIssue({ tipoJira: "Story" })}
        />,
      ),
    );
    await waitFor(() => {
      expect(screen.getByDisplayValue("📖 Historia")).toBeInTheDocument();
    });
  });

  it("tipo Bug → categoría CORRECTIVO preseleccionada", async () => {
    render(
      wrap(
        <ModalPlanificarIssue
          {...baseProps}
          issue={buildIssue({ tipoJira: "Bug", jiraKey: "BUG-1" })}
        />,
      ),
    );
    await waitFor(() => {
      expect(screen.getByDisplayValue("🔧 Correctivo")).toBeInTheDocument();
    });
  });
});
