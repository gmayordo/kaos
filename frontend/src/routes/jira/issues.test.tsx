/**
 * IssuesPage / IssueRow — 12 tests
 * Cubre: título, loading, lista con badges, planificar, ya planificada,
 * filtro de texto, expand/collapse subtareas, selección múltiple, modal.
 */

import * as jiraIssueServiceModule from "@/services/jiraIssueService";
import * as personaServiceModule from "@/services/personaService";
import * as sprintServiceModule from "@/services/sprintService";
import * as squadServiceModule from "@/services/squadService";
import type { JiraIssueResponse } from "@/types/jira";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { IssuesPage } from "./issues";

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock("@/services/jiraIssueService");
vi.mock("@/services/personaService");
vi.mock("@/services/sprintService");
vi.mock("@/services/squadService");
vi.mock("@/lib/useDocumentTitle", () => ({ useDocumentTitle: vi.fn() }));

vi.mock("@/components/jira/JiraTabs", () => ({
  JiraTabs: ({ active }: any) => (
    <div data-testid="jira-tabs" data-active={active} />
  ),
}));

vi.mock("@/features/planificacion/ModalPlanificarIssue", () => ({
  ModalPlanificarIssue: ({ issue, onClose }: any) => (
    <div data-testid="modal-planificar" data-key={issue?.jiraKey}>
      <button type="button" onClick={onClose}>
        Cerrar modal
      </button>
    </div>
  ),
}));

vi.mock("@tanstack/react-router", () => ({
  Link: ({ children, to }: any) => <a href={to}>{children}</a>,
  createFileRoute: () => (cfg: any) => cfg,
  useNavigate: () => vi.fn(),
}));

vi.mock("lucide-react", async (orig) => {
  const actual = await orig<typeof import("lucide-react")>();
  return {
    ...actual,
    GitBranch: () => <span data-testid="icon-git-branch" />,
    ChevronDown: () => <span data-testid="chevron-down" />,
    ChevronRight: () => <span data-testid="chevron-right" />,
    Filter: () => <span data-testid="icon-filter" />,
    Search: () => <span data-testid="icon-search" />,
    ListChecks: () => <span data-testid="icon-list-checks" />,
  };
});

// ── Fixtures ──────────────────────────────────────────────────────────────────

function buildIssue(overrides?: Partial<JiraIssueResponse>): JiraIssueResponse {
  return {
    id: 1,
    jiraKey: "RED-42",
    summary: "Implementar login",
    tipoJira: "Story",
    asignadoNombre: "Ana García",
    asignadoJiraId: "ana",
    estimacionHoras: 8,
    horasConsumidas: 0,
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

const mockSquadsPage = {
  content: [{ id: 1, nombre: "KAOS", estado: "ACTIVO" }],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 100,
};

const mockSprintsPage = {
  content: [
    {
      id: 10,
      nombre: "Sprint 1",
      squadId: 1,
      squads: [],
      estado: "ACTIVO",
      activo: true,
    },
  ],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 50,
};

const mockPersonasPage = {
  content: [
    {
      id: 1,
      nombre: "Ana García",
      email: "ana@kaos.com",
      perfilHorarioId: 1,
      perfilHorarioNombre: "JC",
      seniority: "SENIOR" as const,
      activo: true,
      sendNotifications: false,
      createdAt: "",
      updatedAt: "",
    },
  ],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 200,
};

const buildQC = () =>
  new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

function renderPage() {
  return render(
    <QueryClientProvider client={buildQC()}>
      <IssuesPage />
    </QueryClientProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe("IssuesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(window, "alert").mockImplementation(() => {});
    vi.mocked(squadServiceModule.squadService.listar).mockResolvedValue(
      mockSquadsPage as any,
    );
    vi.mocked(personaServiceModule.personaService.listar).mockResolvedValue(
      mockPersonasPage as any,
    );
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [],
    );
    vi.mocked(sprintServiceModule.sprintService.listar).mockResolvedValue(
      mockSprintsPage as any,
    );
  });

  it("renderiza el título 'Issues Jira'", () => {
    renderPage();
    expect(screen.getByText("Issues Jira")).toBeInTheDocument();
  });

  it("renderiza el componente JiraTabs con active='issues'", () => {
    renderPage();
    const tabs = screen.getByTestId("jira-tabs");
    expect(tabs).toHaveAttribute("data-active", "issues");
  });

  it("muestra lista vacía sin errores cuando no hay issues", async () => {
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [],
    );
    renderPage();
    await waitFor(() => {
      expect(
        screen.queryByRole("button", { name: /Planificar/i }),
      ).not.toBeInTheDocument();
    });
  });

  it("muestra botón 'Planificar' para issue sin tareaId", async () => {
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [buildIssue({ jiraKey: "RED-42", tareaId: null })],
    );
    renderPage();
    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: "Planificar" }),
      ).toBeInTheDocument();
    });
  });

  it("muestra badge 'Ya planificada' y oculta botón para issue con tareaId", async () => {
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [buildIssue({ tareaId: 99 })],
    );
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("Ya planificada")).toBeInTheDocument();
      expect(
        screen.queryByRole("button", { name: "Planificar" }),
      ).not.toBeInTheDocument();
    });
  });

  it("muestra el badge del tipo de issue (Story, Bug…)", async () => {
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [buildIssue({ tipoJira: "Bug", jiraKey: "RED-99" })],
    );
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("Bug")).toBeInTheDocument();
    });
  });

  it("click en 'Planificar' abre el modal con el issue correcto", async () => {
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [buildIssue({ jiraKey: "RED-42", tareaId: null })],
    );
    renderPage();
    // Select squad (index 0) → triggers sprint load
    await waitFor(() => screen.getByText("KAOS"));
    const allSelects = screen.getAllByRole("combobox");
    fireEvent.change(allSelects[0], { target: { value: "1" } });
    // Wait for sprint options (Sprint 1 with id=10)
    await waitFor(() => {
      const opts = screen
        .getAllByRole("combobox")[1]
        .querySelectorAll("option");
      expect(Array.from(opts).some((o: any) => o.value === "10")).toBe(true);
    });
    // Select sprint (index 1)
    fireEvent.change(screen.getAllByRole("combobox")[1], {
      target: { value: "10" },
    });
    // Now Planificar is allowed
    await waitFor(() => screen.getByRole("button", { name: "Planificar" }));
    fireEvent.click(screen.getByRole("button", { name: "Planificar" }));
    await waitFor(() => {
      const modal = screen.getByTestId("modal-planificar");
      expect(modal).toHaveAttribute("data-key", "RED-42");
    });
  });

  it("cerrar modal oculta el modal", async () => {
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [buildIssue({ tareaId: null })],
    );
    renderPage();
    // Select squad + sprint first
    await waitFor(() => screen.getByText("KAOS"));
    const [squadSelect] = screen.getAllByRole("combobox");
    fireEvent.change(squadSelect, { target: { value: "1" } });
    await waitFor(() => {
      const sprintSel = screen.getAllByRole("combobox")[1];
      const options = Array.from(sprintSel.querySelectorAll("option"));
      expect(options.some((o: any) => o.value === "10")).toBe(true);
    });
    const sprintSelect = screen.getAllByRole("combobox")[1];
    fireEvent.change(sprintSelect, { target: { value: "10" } });
    await waitFor(() => screen.getByRole("button", { name: "Planificar" }));
    fireEvent.click(screen.getByRole("button", { name: "Planificar" }));
    await waitFor(() => screen.getByTestId("modal-planificar"));
    fireEvent.click(screen.getByText("Cerrar modal"));
    await waitFor(() => {
      expect(screen.queryByTestId("modal-planificar")).not.toBeInTheDocument();
    });
  });

  it("filtro de texto oculta issues que no coinciden", async () => {
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [
        buildIssue({ jiraKey: "RED-42", summary: "Implementar login" }),
        buildIssue({
          id: 2,
          jiraKey: "RED-43",
          summary: "Dashboard de métricas",
        }),
      ],
    );
    renderPage();
    await waitFor(() => screen.getByText("Implementar login"));

    const searchInput = screen.getByPlaceholderText(/buscar/i);
    fireEvent.change(searchInput, { target: { value: "login" } });

    await waitFor(() => {
      expect(screen.getByText("Implementar login")).toBeInTheDocument();
      expect(
        screen.queryByText("Dashboard de métricas"),
      ).not.toBeInTheDocument();
    });
  });

  it("issue con subtareas muestra el botón expand", async () => {
    const sub = buildIssue({
      id: 2,
      jiraKey: "RED-43",
      parentKey: "RED-42",
      subtareas: [],
    });
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [buildIssue({ subtareas: [sub] })],
    );
    renderPage();
    await waitFor(() => {
      expect(
        screen.getByRole("button", {
          name: /Expandir subtareas|Contraer subtareas/i,
        }),
      ).toBeInTheDocument();
    });
  });

  it("click en expand muestra subtareas", async () => {
    const sub = buildIssue({
      id: 2,
      jiraKey: "RED-43",
      summary: "Subtarea auth",
      parentKey: "RED-42",
      subtareas: [],
    });
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [buildIssue({ subtareas: [sub] })],
    );
    renderPage();
    await waitFor(() =>
      screen.getByRole("button", { name: /Expandir subtareas/i }),
    );

    // Subtarea aún no visible
    expect(screen.queryByText("Subtarea auth")).not.toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: /Expandir subtareas/i }),
    );

    await waitFor(() => {
      expect(screen.getByText("Subtarea auth")).toBeInTheDocument();
    });
  });

  it("checkbox de selección no aparece en issues ya planificados", async () => {
    vi.mocked(jiraIssueServiceModule.jiraIssueService.listar).mockResolvedValue(
      [buildIssue({ tareaId: 99 })],
    );
    renderPage();
    await waitFor(() => screen.getByText("Ya planificada"));
    // Un issue planificado no tiene checkbox de selección individual
    // (hay otros checkboxes: "Solo sin planificar" en filtros y "Seleccionar todos")
    expect(
      screen.queryByRole("checkbox", { name: /Seleccionar RED/ }),
    ).not.toBeInTheDocument();
  });
});
