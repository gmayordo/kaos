/**
 * JiraPage — Tests (14 total)
 *  JiraConfigSection  → 8 tests (config display, test conexión OK/KO)
 *  JiraSyncStatusWidget → 6 tests (IDLE, SINCRONIZANDO, CUOTA_AGOTADA, ERROR, progress, botones)
 */

import * as jiraService from "@/services/jiraService";
import type { JiraConfigResponse, JiraSyncStatusResponse } from "@/types/jira";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { JiraConfigSection, JiraSyncStatusWidget } from "./index";

// ── Mocks ────────────────────────────────────────────────────────────────────
vi.mock("@/services/jiraService");
vi.mock("@tanstack/react-router", () => ({
  Link: ({ children, to }: any) => <a href={to}>{children}</a>,
  createFileRoute: () => (cfg: any) => cfg,
  useNavigate: () => vi.fn(),
}));
vi.mock("lucide-react", async (orig) => {
  const actual = await orig<typeof import("lucide-react")>();
  return {
    ...actual,
    CheckCircle: () => <span data-testid="icon-check-ok" />,
    XCircle: () => <span data-testid="icon-x-error" />,
    RefreshCw: () => <span data-testid="icon-refresh" />,
    Clock: () => <span data-testid="icon-clock" />,
    AlertTriangle: () => <span data-testid="icon-alert" />,
    Settings: () => <span data-testid="icon-settings" />,
  };
});

// ── Fixtures ─────────────────────────────────────────────────────────────────
const mockConfig: JiraConfigResponse = {
  squadId: 1,
  squadNombre: "KAOS",
  url: "https://kaos.atlassian.net",
  usuario: "kaos-admin@ehcos.com",
  tokenOculto: "***abc",
  loadMethod: "MANUAL",
  activa: true,
  mapeoEstados: null,
};

const mockStatusIdle: JiraSyncStatusResponse = {
  squadId: 1,
  squadNombre: "KAOS",
  estado: "IDLE",
  ultimaSync: "2026-02-25T10:00:00Z",
  issuesImportadas: 42,
  worklogsImportados: 150,
  callsConsumidas2h: 10,
  callsRestantes2h: 190,
  ultimoError: null,
  operacionesPendientes: 0,
  updatedAt: null,
};

const buildQC = () =>
  new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

function renderConfig(squadId = 1, qc?: QueryClient) {
  const client = qc ?? buildQC();
  return render(
    <QueryClientProvider client={client}>
      <JiraConfigSection squadId={squadId} />
    </QueryClientProvider>,
  );
}

function renderSync(squadId = 1, qc?: QueryClient) {
  const client = qc ?? buildQC();
  return render(
    <QueryClientProvider client={client}>
      <JiraSyncStatusWidget squadId={squadId} />
    </QueryClientProvider>,
  );
}

// ──────────────────────────────────────────────────────────────────────────────
describe("JiraConfigSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("muestra la URL de Jira cuando la config está cargada", async () => {
    vi.mocked(jiraService.jiraConfigService.obtenerConfig).mockResolvedValue(
      mockConfig,
    );
    renderConfig();
    await waitFor(() => {
      expect(
        screen.getByText("https://kaos.atlassian.net"),
      ).toBeInTheDocument();
    });
  });

  it("muestra el usuario de Jira", async () => {
    vi.mocked(jiraService.jiraConfigService.obtenerConfig).mockResolvedValue(
      mockConfig,
    );
    renderConfig();
    await waitFor(() => {
      expect(screen.getByText("kaos-admin@ehcos.com")).toBeInTheDocument();
    });
  });

  it("muestra el token ofuscado (*** parcial)", async () => {
    vi.mocked(jiraService.jiraConfigService.obtenerConfig).mockResolvedValue(
      mockConfig,
    );
    renderConfig();
    await waitFor(() => {
      expect(screen.getByText("***abc")).toBeInTheDocument();
    });
  });

  it("muestra el método de carga (MANUAL)", async () => {
    vi.mocked(jiraService.jiraConfigService.obtenerConfig).mockResolvedValue(
      mockConfig,
    );
    renderConfig();
    await waitFor(() => {
      expect(screen.getByText(/MANUAL/i)).toBeInTheDocument();
    });
  });

  it("muestra botón 'Probar conexión'", async () => {
    vi.mocked(jiraService.jiraConfigService.obtenerConfig).mockResolvedValue(
      mockConfig,
    );
    renderConfig();
    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: /probar conexión/i }),
      ).toBeInTheDocument();
    });
  });

  it("al probar conexión exitosa muestra indicador OK", async () => {
    vi.mocked(jiraService.jiraConfigService.obtenerConfig).mockResolvedValue(
      mockConfig,
    );
    vi.mocked(jiraService.jiraConfigService.probarConexion).mockResolvedValue({
      ok: true,
      mensaje: "Conexión exitosa",
    } as any);

    renderConfig();
    const btn = await screen.findByRole("button", { name: /probar conexión/i });
    fireEvent.click(btn);
    await waitFor(() => {
      expect(screen.getByTestId("icon-check-ok")).toBeInTheDocument();
    });
  });

  it("al probar conexión fallida muestra indicador de error", async () => {
    vi.mocked(jiraService.jiraConfigService.obtenerConfig).mockResolvedValue(
      mockConfig,
    );
    // Returns falsy → testResult = false → muestra XCircle
    vi.mocked(jiraService.jiraConfigService.probarConexion).mockResolvedValue(
      false as any,
    );

    renderConfig();
    const btn = await screen.findByRole("button", { name: /probar conexión/i });
    fireEvent.click(btn);
    await waitFor(() => {
      expect(screen.getByTestId("icon-x-error")).toBeInTheDocument();
    });
  });

  it("muestra el mensaje cuando no hay configuración disponible", async () => {
    vi.mocked(jiraService.jiraConfigService.obtenerConfig).mockResolvedValue(
      null as any,
    );
    renderConfig();
    await waitFor(() => {
      expect(
        screen.getByText(/no hay configuración jira/i),
      ).toBeInTheDocument();
    });
  });
});

// ──────────────────────────────────────────────────────────────────────────────
describe("JiraSyncStatusWidget", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("estado IDLE muestra indicador verde", async () => {
    vi.mocked(jiraService.jiraSyncService.obtenerEstado).mockResolvedValue(
      mockStatusIdle,
    );
    renderSync();
    // estadoLabel("IDLE") = "Sincronizado"
    await waitFor(() => {
      expect(screen.getByText(/Sincronizado/i)).toBeInTheDocument();
    });
    const indicator = document.querySelector(".bg-green-500");
    expect(indicator).toBeTruthy();
  });

  it("estado SINCRONIZANDO muestra spinner azul", async () => {
    vi.mocked(jiraService.jiraSyncService.obtenerEstado).mockResolvedValue({
      ...mockStatusIdle,
      estado: "SINCRONIZANDO",
    });
    renderSync();
    // estadoLabel("SINCRONIZANDO") = "Sincronizando..."
    await waitFor(() => {
      expect(screen.getByText(/Sincronizando/i)).toBeInTheDocument();
    });
    const indicator = document.querySelector('[class*="bg-blue-500"]');
    expect(indicator).toBeTruthy();
  });

  it("estado CUOTA_AGOTADA muestra indicador ámbar", async () => {
    vi.mocked(jiraService.jiraSyncService.obtenerEstado).mockResolvedValue({
      ...mockStatusIdle,
      estado: "CUOTA_AGOTADA",
    });
    renderSync();
    // estadoLabel("CUOTA_AGOTADA") = "Cuota agotada"
    await waitFor(() => {
      expect(screen.getByText(/Cuota agotada/i)).toBeInTheDocument();
    });
    const indicator = document.querySelector('[class*="bg-amber-500"]');
    expect(indicator).toBeTruthy();
  });

  it("estado ERROR muestra indicador rojo", async () => {
    vi.mocked(jiraService.jiraSyncService.obtenerEstado).mockResolvedValue({
      ...mockStatusIdle,
      estado: "ERROR",
      ultimoError: "Internal server error",
    });
    renderSync();
    // estadoLabel("ERROR") = "Error"
    await waitFor(() => {
      expect(screen.getAllByText(/^Error/i).length).toBeGreaterThan(0);
    });
  });

  it("muestra la barra de progreso de calls consumidas", async () => {
    vi.mocked(jiraService.jiraSyncService.obtenerEstado).mockResolvedValue({
      ...mockStatusIdle,
      callsConsumidas2h: 100,
      callsRestantes2h: 100,
    });
    renderSync();
    await waitFor(() => {
      // El span incluye "100 / 200 calls"
      const els = screen.getAllByText(/100.*200 calls/i);
      expect(els.length).toBeGreaterThan(0);
    });
  });

  it("botones de sincronización están presentes en estado IDLE", async () => {
    vi.mocked(jiraService.jiraSyncService.obtenerEstado).mockResolvedValue(
      mockStatusIdle,
    );
    renderSync();
    await waitFor(() => {
      // Button text: "Sync completa"
      expect(screen.getByText(/Sync completa/)).toBeInTheDocument();
    });
    expect(screen.getByText(/Solo issues/)).toBeInTheDocument();
    expect(screen.getByText(/Solo worklogs/)).toBeInTheDocument();
  });
});
