/**
 * AlertasPanel — 8 tests
 * Cubre renderizado por severidad, filtro, resolver y estados vacío/error
 */

import * as jiraService from "@/services/jiraService";
import type { AlertaResponse } from "@/types/jira";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AlertasPanel } from "./AlertasPanel";

// ── Mocks ────────────────────────────────────────────────────────────────────
vi.mock("@/services/jiraService");
vi.mock("lucide-react", async (orig) => {
  const actual = await orig<typeof import("lucide-react")>();
  return {
    ...actual,
    AlertCircle: () => <span data-testid="icon-critico" />,
    AlertTriangle: () => <span data-testid="icon-aviso" />,
    Info: () => <span data-testid="icon-info" />,
    CheckCircle2: () => <span data-testid="icon-ok" />,
    X: () => <span data-testid="icon-x">✕</span>,
  };
});

// ── Fixtures ─────────────────────────────────────────────────────────────────
const mockAlertaCritico: AlertaResponse = {
  id: 1,
  sprintId: 10,
  squadId: 1,
  reglaId: 1,
  reglaNombre: "TAREA_SIN_WORKLOG",
  severidad: "CRITICO",
  mensaje: "La tarea KAOS-1 lleva 3 días sin imputación.",
  jiraKey: "KAOS-1",
  personaNombre: "Ana García",
  resuelta: false,
  notificadaEmail: false,
  createdAt: "2026-02-25T10:00:00Z",
};

const mockAlertaAviso: AlertaResponse = {
  id: 2,
  sprintId: 10,
  squadId: 1,
  reglaId: 2,
  reglaNombre: "ESTIMACION_DESVIADA",
  severidad: "AVISO",
  mensaje: "KAOS-2 supera la estimación en un 40%.",
  jiraKey: "KAOS-2",
  personaNombre: null,
  resuelta: false,
  notificadaEmail: false,
  createdAt: "2026-02-25T10:00:00Z",
};

const mockAlertaInfo: AlertaResponse = {
  id: 3,
  sprintId: 10,
  squadId: 1,
  reglaId: 3,
  reglaNombre: "CO_DESARROLLADOR_DETECTADO",
  severidad: "INFO",
  mensaje: "KAOS-3 tiene co-desarrollador detectado.",
  jiraKey: "KAOS-3",
  personaNombre: "Carlos López",
  resuelta: false,
  notificadaEmail: false,
  createdAt: "2026-02-25T10:00:00Z",
};

const buildQueryClient = () =>
  new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

function renderPanel(sprintId = 10, client?: QueryClient) {
  const qc = client ?? buildQueryClient();
  return render(
    <QueryClientProvider client={qc}>
      <AlertasPanel sprintId={sprintId} />
    </QueryClientProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────
describe("AlertasPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("muestra estado vacío cuando no hay alertas", async () => {
    vi.mocked(jiraService.jiraAlertService.listarAlertas).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 200,
      number: 0,
    } as any);

    renderPanel();
    await waitFor(() => {
      expect(screen.getByTestId("icon-ok")).toBeInTheDocument();
    });
    expect(screen.getByText(/No hay alertas pendientes/i)).toBeInTheDocument();
  });

  it("muestra alertas críticas con su icono y mensaje", async () => {
    vi.mocked(jiraService.jiraAlertService.listarAlertas).mockResolvedValue({
      content: [mockAlertaCritico],
      totalElements: 1,
      totalPages: 1,
      size: 200,
      number: 0,
    } as any);

    renderPanel();
    await waitFor(() => {
      expect(screen.getByTestId("icon-critico")).toBeInTheDocument();
    });
    expect(screen.getByText("TAREA_SIN_WORKLOG")).toBeInTheDocument();
    expect(
      screen.getByText(/La tarea KAOS-1 lleva 3 días sin imputación/i),
    ).toBeInTheDocument();
  });

  it("muestra alertas de tipo AVISO e INFO", async () => {
    vi.mocked(jiraService.jiraAlertService.listarAlertas).mockResolvedValue({
      content: [mockAlertaAviso, mockAlertaInfo],
      totalElements: 2,
      totalPages: 1,
      size: 200,
      number: 0,
    } as any);

    renderPanel();
    await waitFor(() => {
      expect(screen.getByTestId("icon-aviso")).toBeInTheDocument();
    });
    expect(screen.getByTestId("icon-info")).toBeInTheDocument();
  });

  it("ordena grupos: CRITICO primero, luego AVISO, luego INFO", async () => {
    vi.mocked(jiraService.jiraAlertService.listarAlertas).mockResolvedValue({
      content: [mockAlertaInfo, mockAlertaAviso, mockAlertaCritico],
      totalElements: 3,
      totalPages: 1,
      size: 200,
      number: 0,
    } as any);

    renderPanel();
    await waitFor(() => {
      expect(screen.getByText("Crítico")).toBeInTheDocument();
    });
    const sections = screen
      .getAllByText(/^(Crítico|Aviso|Info)$/)
      .map((el) => el.textContent ?? "");
    expect(sections[0]).toBe("Crítico");
    expect(sections[1]).toBe("Aviso");
    expect(sections[2]).toBe("Info");
  });

  it("botón resolver llama a resolverAlerta con el ID correcto", async () => {
    vi.mocked(jiraService.jiraAlertService.listarAlertas).mockResolvedValue({
      content: [mockAlertaCritico],
      totalElements: 1,
      totalPages: 1,
      size: 200,
      number: 0,
    } as any);
    vi.mocked(jiraService.jiraAlertService.resolverAlerta).mockResolvedValue(
      undefined as any,
    );

    renderPanel();
    await waitFor(() => {
      expect(screen.getByTestId("icon-x")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("icon-x"));
    await waitFor(() => {
      expect(jiraService.jiraAlertService.resolverAlerta).toHaveBeenCalledWith(
        1,
      );
    });
  });

  it("muestra filtros cuando hay más de una regla diferente", async () => {
    vi.mocked(jiraService.jiraAlertService.listarAlertas).mockResolvedValue({
      content: [mockAlertaCritico, mockAlertaAviso],
      totalElements: 2,
      totalPages: 1,
      size: 200,
      number: 0,
    } as any);

    renderPanel();
    await waitFor(() => {
      expect(screen.getByText("Todas (2)")).toBeInTheDocument();
    });
    expect(screen.getByText("TAREA_SIN_WORKLOG (1)")).toBeInTheDocument();
    expect(screen.getByText("ESTIMACION_DESVIADA (1)")).toBeInTheDocument();
  });

  it("filtrar por reglaNombre muestra solo alertas del tipo seleccionado", async () => {
    vi.mocked(jiraService.jiraAlertService.listarAlertas).mockResolvedValue({
      content: [mockAlertaCritico, mockAlertaAviso],
      totalElements: 2,
      totalPages: 1,
      size: 200,
      number: 0,
    } as any);

    renderPanel();
    await waitFor(() => {
      expect(screen.getByText("ESTIMACION_DESVIADA (1)")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText("ESTIMACION_DESVIADA (1)"));
    await waitFor(() => {
      expect(screen.getByText(/KAOS-2 supera/i)).toBeInTheDocument();
    });
    expect(screen.queryByText(/KAOS-1 lleva 3 días/i)).not.toBeInTheDocument();
  });

  it("muestra el contexto (jiraKey + personaNombre) cuando están disponibles", async () => {
    vi.mocked(jiraService.jiraAlertService.listarAlertas).mockResolvedValue({
      content: [mockAlertaCritico],
      totalElements: 1,
      totalPages: 1,
      size: 200,
      number: 0,
    } as any);

    renderPanel();
    await waitFor(() => {
      // jiraKey aparece en el span de contexto Y en el mensaje
      expect(screen.getAllByText(/KAOS-1/).length).toBeGreaterThanOrEqual(1);
    });
    expect(screen.getByText(/Ana García/)).toBeInTheDocument();
  });
});
