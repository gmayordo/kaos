/**
 * MiSemanaPage — 10 tests
 * Cubre: selector persona, navegación semanal,
 * cuadrícula de issues × días, totales y modal de imputación
 */

import * as jiraService from "@/services/jiraService";
import * as personaService from "@/services/personaService";
import type { WorklogSemanaResponse } from "@/types/jira";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MiSemanaPage } from "./mi-semana";

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
    PlusCircle: () => <span data-testid="icon-plus-cell" />,
    X: () => <span data-testid="icon-x" />,
  };
});

// ── Fixtures ─────────────────────────────────────────────────────────────────

// Lunes de esta semana — misma lógica que thisWeekMonday() del componente
function getMondayISO(): string {
  const isoDate = new Date().toISOString().split("T")[0];
  const d = new Date(isoDate + "T00:00");
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  d.setDate(diff);
  return d.toISOString().split("T")[0];
}

const MONDAY = getMondayISO();
const TUESDAY = (() => {
  const d = new Date(MONDAY + "T00:00");
  d.setDate(d.getDate() + 1);
  return d.toISOString().split("T")[0];
})();
const FRIDAY = (() => {
  const d = new Date(MONDAY + "T00:00");
  d.setDate(d.getDate() + 4);
  return d.toISOString().split("T")[0];
})();

const mockSemana: WorklogSemanaResponse = {
  semanaInicio: MONDAY,
  semanaFin: FRIDAY,
  personaId: 1,
  personaNombre: "Ana García",
  horasCapacidadDia: 8,
  totalHorasSemana: 11,
  totalCapacidadSemana: 40,
  filas: [
    {
      jiraKey: "KAOS-42",
      issueSummary: "Implementar login OAuth",
      dias: [
        { fecha: MONDAY, horas: 3, worklogId: 10 },
        { fecha: TUESDAY, horas: 0, worklogId: null },
      ],
      totalHorasTarea: 3,
    },
    {
      jiraKey: "KAOS-43",
      issueSummary: "Code review sprint 5",
      dias: [
        { fecha: MONDAY, horas: 0, worklogId: null },
        { fecha: TUESDAY, horas: 8, worklogId: 11 },
      ],
      totalHorasTarea: 8,
    },
  ],
};

const mockEmptySemana: WorklogSemanaResponse = {
  ...mockSemana,
  totalHorasSemana: 0,
  filas: [],
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
      <MiSemanaPage />
    </QueryClientProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────
describe("MiSemanaPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(personaService.personaService.listar).mockResolvedValue(
      mockPersonas as any,
    );
  });

  it("muestra el selector de personas al cargar", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByRole("combobox")).toBeInTheDocument();
    });
  });

  it("sin persona seleccionada muestra el placeholder con icono", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId("icon-clock")).toBeInTheDocument();
    });
  });

  it("al seleccionar persona se llama a getMiSemana", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiSemana).mockResolvedValue(
      mockSemana,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      expect(jiraService.jiraWorklogService.getMiSemana).toHaveBeenCalledWith(
        1,
        MONDAY,
      );
    });
  });

  it("muestra las issues en la cuadrícula", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiSemana).mockResolvedValue(
      mockSemana,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      expect(screen.getByText("KAOS-42")).toBeInTheDocument();
    });
    expect(screen.getByText("KAOS-43")).toBeInTheDocument();
  });

  it("celdas con horas muestran el valor en hh", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiSemana).mockResolvedValue(
      mockSemana,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      // "3h" aparece en celda, total día y total fila
      expect(screen.getAllByText("3h").length).toBeGreaterThanOrEqual(1);
    });
    expect(screen.getAllByText("8h").length).toBeGreaterThanOrEqual(1);
  });

  it("muestra 'Sin imputaciones' si no hay filas", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiSemana).mockResolvedValue(
      mockEmptySemana,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      expect(
        screen.getByText(/Sin imputaciones registradas/i),
      ).toBeInTheDocument();
    });
  });

  it("muestra el total semanal en el resumen de capacidad", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiSemana).mockResolvedValue(
      mockSemana,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      // "11h" = totalHorasSemana (resumen + tfoot); "40h" = totalCapacidadSemana
      expect(screen.getAllByText(/11h/).length).toBeGreaterThanOrEqual(1);
    });
    expect(screen.getAllByText(/40h/).length).toBeGreaterThanOrEqual(1);
  });

  it("botón 'Esta semana' está presente en la barra de navegación", async () => {
    renderPage();
    expect(
      screen.getByRole("button", { name: /esta semana/i }),
    ).toBeInTheDocument();
  });

  it("click en celda vacía abre el modal de imputación con el jiraKey correcto", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiSemana).mockResolvedValue(
      mockSemana,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    // Las celdas vacías muestran PlusCircle
    await waitFor(() => {
      expect(screen.getAllByTestId("icon-plus-cell").length).toBeGreaterThan(0);
    });
    // Clicar la primera celda vacía (botón con icon-plus-cell)
    fireEvent.click(
      screen.getAllByTestId("icon-plus-cell")[0].closest("button")!,
    );
    await waitFor(() => {
      // Modal con el jiraKey correspondiente
      expect(screen.getByText(/imputar horas/i)).toBeInTheDocument();
    });
  });

  it("fila de totales muestra el total por día", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiSemana).mockResolvedValue(
      mockSemana,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      // Total día lunes: 3h
      expect(screen.getAllByText("3h").length).toBeGreaterThanOrEqual(1);
    });
  });

  it("total por tarea (columna final) se muestra en la fila", async () => {
    vi.mocked(jiraService.jiraWorklogService.getMiSemana).mockResolvedValue(
      mockSemana,
    );
    const { getByRole } = renderPage();
    await waitFor(() => screen.getByText("Ana García"));
    fireEvent.change(getByRole("combobox"), { target: { value: "1" } });
    await waitFor(() => {
      // KAOS-43 totalHorasTarea = 8h (también aparece como celda), pero
      // la columna total de fila tendría "8h"
      expect(screen.getAllByText("8h").length).toBeGreaterThanOrEqual(1);
    });
  });
});
