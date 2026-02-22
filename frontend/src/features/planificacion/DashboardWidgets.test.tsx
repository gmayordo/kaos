/**
 * DashboardWidgets — Tests unitarios
 * Cubre métricas, alertas, loading skeleton y render sin tareas (gráficos vacíos)
 *
 * Nota: recharts se mockea para evitar errores de ResizeObserver en jsdom
 */

import type { DashboardSprintResponse, TareaResponse } from "@/types/api";
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardWidgets } from "./DashboardWidgets";

// Mock recharts — jsdom no soporta SVG/ResizeObserver
vi.mock("recharts", () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="recharts-container">{children}</div>
  ),
  BarChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="bar-chart">{children}</div>
  ),
  PieChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="pie-chart">{children}</div>
  ),
  Bar: () => null,
  Pie: () => null,
  Cell: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
  Legend: () => null,
}));

const mockDashboard: DashboardSprintResponse = {
  sprintId: 1,
  sprintNombre: "Sprint Backend Q1",
  estado: "ACTIVO",
  tareasTotal: 15,
  tareasPendientes: 8,
  tareasEnProgreso: 4,
  tareasCompletadas: 3,
  tareasBloqueadas: 0,
  progresoEsperado: 30,
  progresoReal: 20,
  capacidadTotalHoras: 160,
  capacidadAsignadaHoras: 120,
  ocupacionPorcentaje: 75,
  bloqueosActivos: 0,
  alertas: [],
  fechaInicio: "2026-03-02",
  fechaFin: "2026-03-13",
};

describe("DashboardWidgets", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("CA-17: renderiza las 4 métricas principales", () => {
    render(<DashboardWidgets dashboard={mockDashboard} />);
    expect(screen.getByText("Tareas totales")).toBeInTheDocument();
    expect(screen.getByText("Ocupación")).toBeInTheDocument();
    expect(screen.getByText("Progreso real")).toBeInTheDocument();
    expect(screen.getByText("Bloqueos activos")).toBeInTheDocument();
  });

  it("CA-17: muestra el valor de ocupación correcto", () => {
    render(<DashboardWidgets dashboard={mockDashboard} />);
    expect(screen.getByText("75%")).toBeInTheDocument();
  });

  it("muestra el número de tareas totales", () => {
    render(<DashboardWidgets dashboard={mockDashboard} />);
    expect(screen.getByText("15")).toBeInTheDocument();
  });

  it("muestra alertas cuando el dashboard contiene alertas", () => {
    const dashConAlertas: DashboardSprintResponse = {
      ...mockDashboard,
      alertas: ["Sobreasignación en día 5: Juan Pérez", "Hay 2 bloqueos sin resolver"],
    };
    render(<DashboardWidgets dashboard={dashConAlertas} />);
    const lista = screen.getByRole("list", { name: /alertas/i });
    expect(lista).toBeInTheDocument();
    expect(screen.getByText(/sobreasignación/i)).toBeInTheDocument();
    expect(screen.getByText(/hay 2 bloqueos/i)).toBeInTheDocument();
  });

  it("muestra skeleton de carga cuando isLoading=true", () => {
    const { container } = render(
      <DashboardWidgets dashboard={mockDashboard} isLoading={true} />,
    );
    expect(container.querySelector(".animate-pulse")).toBeInTheDocument();
  });

  it("renderiza los gráficos recharts cuando hay tareas", () => {
    const tareas: TareaResponse[] = [
      {
        id: 1,
        titulo: "Tarea test",
        sprintId: 1,
        personaId: 1,
        personaNombre: "Juan Pérez",
        tipo: "HISTORIA",
        categoria: "EVOLUTIVO",
        estimacion: 4,
        prioridad: "NORMAL",
        estado: "PENDIENTE",
        bloqueada: false,
        createdAt: "2026-02-22T10:00:00Z",
      },
    ];
    render(<DashboardWidgets dashboard={mockDashboard} tareas={tareas} />);
    expect(screen.getAllByTestId("recharts-container").length).toBeGreaterThan(
      0,
    );
  });
});
