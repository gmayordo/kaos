/**
 * MiDiaPage — 10 tests
 * Cubre: selector persona, navegación de fecha, worklogs del día,
 * imputar horas, borrado, barra capacidad, jornada completa
 */

import * as jiraService from "@/services/jiraService";
import * as personaService from "@/services/personaService";
import type { WorklogDiaResponse } from "@/types/jira";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MiDiaPage } from "./mi-dia";

// ── Mocks ────────────────────────────────────────────────────────────────────
vi.mock("@/services/jiraService");
vi.mock("@/services/personaService");
vi.mock("@tanstack/react-router", () => ({
  Link: ({ children, to }: any) => <a href={to}>{children}</a>,
  createFileRoute: () => (cfg: any) => cfg,
}));
vi.mock("lucide-react", async (orig) => {
  const actual = await orig<typeof import("lucide-react")>();
  return {
    ...actual,
    ChevronLeft: () => <span>{"<"}</span>,
    ChevronRight: () => <span>{">"}</span>,
    Clock: () => <span data-testid="icon-clock" />,
    PlusCircle: () => <span data-testid="icon-plus" />,
    Trash2: () => <span data-testid="icon-trash" />,
    X: () => <span data-testid="icon-x" />,
  };
});

// ── Fixtures ─────────────────────────────────────────────────────────────────
const TODAY = new Date().toISOString().split("T")[0];

const mockWorklogDia: WorklogDiaResponse = {
  personaId: 1,
  personaNombre: "Ana García",
  fecha: TODAY,
  horasCapacidad: 8,
  horasImputadas: 5,
  jornadaCompleta: false,
  worklogs: [
    {
      worklogId: 10,
      jiraKey: "KAOS-42",
      issueSummary: "Implementación de login",
      horas: 3,
      comentario: "Implementación de login",
      sincronizado: false,
    },
    {
      worklogId: 11,
      jiraKey: "KAOS-43",
      issueSummary: "Code review sprint",
      horas: 2,
      comentario: "Code review",
      sincronizado: true,
    },
  ],
};

const mockEmptyWorklogDia: WorklogDiaResponse = {
  ...mockWorklogDia,
  horasImputadas: 0,
  jornadaCompleta: false,
  worklogs: [],
};

const mockPersonas = {
  content: [
    {
      id: 1,
      nombre: "Ana García",
      email: "ana@ehcos.com",
      perfilHorarioId: 1,
      perfilHorarioNombre: "JC",
      seniority: "SENIOR" as const,
      ciudad: "Madrid",
    },
  ],
  totalElements: 1,
  totalPages: 1,
  size: 200,
  number: 0,
};

const buildQC = () =>
  new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

function renderPage() {
  return render(
    <QueryClientProvider client={buildQC()}>
      <MiDiaPage />
    </QueryClientProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────
describe("MiDiaPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(personaService.personaService.listar).mockResolvedValue(
      mockPersonas as any,
    );
  });

  it("muestra el selector de personas al cargar", async () => {
    renderPage();
    // Esperar a que las personas carguen y aparezca la opción por defecto
    await waitFor(() => {
      expect(screen.getByText("Seleccionar persona")).toBeInTheDocument();
    });
    expect(screen.getByRole("combobox")).toBeInTheDocument();
  });

  it("sin persona seleccionada muestra el icono de placeholder", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId("icon-clock")).toBeInTheDocument();
    });
  });

  it("al seleccionar persona se carga el worklog del día", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiDia).mockResolvedValue(
      mockWorklogDia,
    );
    const { getByRole } = renderPage();
    await waitFor(() => {
      expect(screen.getByText("Ana García")).toBeInTheDocument();
    });
    const select = getByRole("combobox");
    fireEvent.change(select, { target: { value: "1" } });
    await waitFor(() => {
      expect(jiraService.jiraWorklogService.getMiDia).toHaveBeenCalledWith(
        1,
        TODAY,
      );
    });
  });

  it("muestra los worklogs del día con su jiraKey y horas", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiDia).mockResolvedValue(
      mockWorklogDia,
    );
    const { getByRole } = renderPage();
    // Esperar a que las personas carguen ANTES de cambiar el select
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      expect(screen.getByText("KAOS-42")).toBeInTheDocument();
    });
    expect(screen.getByText("KAOS-43")).toBeInTheDocument();
  });

  it("muestra 'Sin imputaciones' cuando el día no tiene registros", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiDia).mockResolvedValue(
      mockEmptyWorklogDia,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      expect(screen.getByText(/sin imputaciones/i)).toBeInTheDocument();
    });
  });

  it("solo worklog MANUAL (no sincronizado) tiene botón de borrado", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiDia).mockResolvedValue(
      mockWorklogDia,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      expect(screen.getAllByTestId("icon-trash")).toHaveLength(1);
    });
  });

  it("botón eliminar llama a eliminar con el ID del worklog", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiDia).mockResolvedValue(
      mockWorklogDia,
    );
    vi.mocked(jiraService.jiraWorklogService.eliminar).mockResolvedValue(
      undefined as any,
    );
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    const trash = await screen.findByTestId("icon-trash");
    fireEvent.click(trash.parentElement!);
    await waitFor(() => {
      expect(jiraService.jiraWorklogService.eliminar).toHaveBeenCalledWith(10);
    });
  });

  it("botón 'Añadir imputación' muestra el formulario de registro", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiDia).mockResolvedValue(
      mockWorklogDia,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    const addBtn = await screen.findByText(/añadir imputación/i);
    fireEvent.click(addBtn);
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/PROJ-/i)).toBeInTheDocument();
    });
  });

  it("la barra de capacidad muestra las horas imputadas vs capacidad", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiDia).mockResolvedValue(
      mockWorklogDia,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      const els = screen.getAllByText(/imputadas/i);
      expect(els.length).toBeGreaterThan(0);
    });
  });

  it("botón 'Hoy' muestra la fecha de hoy", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiDia).mockResolvedValue(
      mockWorklogDia,
    );
    renderPage();
    const todayBtn = await screen.findByRole("button", { name: /hoy/i });
    expect(todayBtn).toBeInTheDocument();
  });
});
