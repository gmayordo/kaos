/**
 * AlertasPanel — Panel de alertas Jira del sprint
 * Agrupa por severidad (CRITICO → AVISO → INFO),
 * permite resolver alertas y filtrar por tipo.
 */

import { jiraAlertService } from "@/services/jiraService";
import type { AlertaResponse, Severidad } from "@/types/jira";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Info,
  X,
} from "lucide-react";
import type { FC } from "react";
import { useState } from "react";

// ─── Config de severidad ────────────────────────────────────────

interface SeveridadConfig {
  label: string;
  icon: FC<{ className?: string }>;
  borderClass: string;
  bgClass: string;
  badgeClass: string;
  headerClass: string;
}

const SEVERIDAD_CONFIG: Record<Severidad, SeveridadConfig> = {
  CRITICO: {
    label: "Crítico",
    icon: AlertCircle,
    borderClass: "border-l-red-600",
    bgClass: "bg-red-50 dark:bg-red-950/20",
    badgeClass: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400",
    headerClass: "text-red-700 dark:text-red-400",
  },
  AVISO: {
    label: "Aviso",
    icon: AlertTriangle,
    borderClass: "border-l-amber-500",
    bgClass: "bg-amber-50 dark:bg-amber-950/20",
    badgeClass:
      "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-400",
    headerClass: "text-amber-700 dark:text-amber-400",
  },
  INFO: {
    label: "Info",
    icon: Info,
    borderClass: "border-l-blue-500",
    bgClass: "bg-blue-50 dark:bg-blue-950/20",
    badgeClass:
      "bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-400",
    headerClass: "text-blue-700 dark:text-blue-400",
  },
};

const SEVERIDAD_ORDER: Severidad[] = ["CRITICO", "AVISO", "INFO"];

// ─── Props ───────────────────────────────────────────────────────

interface AlertasPanelProps {
  /** ID del sprint cuyos alertas se muestran */
  sprintId: number;
}

// ─── Componente ──────────────────────────────────────────────────

export const AlertasPanel: FC<AlertasPanelProps> = ({ sprintId }) => {
  const queryClient = useQueryClient();
  const [reglaNombreFiltro, setReglaNombreFiltro] = useState<string | "TODAS">(
    "TODAS",
  );

  const { data, isLoading, error } = useQuery({
    queryKey: ["jira-alertas", sprintId],
    queryFn: () => jiraAlertService.listarAlertas(sprintId, 0, 200, false),
  });

  const resolver = useMutation({
    mutationFn: (id: number) => jiraAlertService.resolverAlerta(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jira-alertas", sprintId] });
    },
  });

  const alertas: AlertaResponse[] = data?.content ?? [];

  // Tipos (reglaNombre) disponibles para filtro
  const reglasDisponibles: string[] = [
    ...new Set(alertas.map((a) => a.reglaNombre)),
  ];

  // Filtrar por regla
  const alertasFiltradas =
    reglaNombreFiltro === "TODAS"
      ? alertas
      : alertas.filter((a) => a.reglaNombre === reglaNombreFiltro);

  // Agrupar por severidad (orden: CRITICO → AVISO → INFO)
  const grupos: Record<Severidad, AlertaResponse[]> = {
    CRITICO: [],
    AVISO: [],
    INFO: [],
  };
  alertasFiltradas.forEach((a) => {
    const sev = a.severidad as Severidad;
    if (grupos[sev]) grupos[sev].push(a);
  });

  if (isLoading) {
    return (
      <div className="space-y-2">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-14 rounded bg-muted animate-pulse" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-sm text-muted-foreground p-4 border border-border rounded-lg">
        Error al cargar alertas del sprint.
      </div>
    );
  }

  if (alertas.length === 0) {
    return (
      <div className="flex flex-col items-center py-10 text-muted-foreground">
        <CheckCircle2 className="h-10 w-10 mb-3 text-emerald-500 opacity-80" />
        <p className="text-sm">No hay alertas pendientes en este sprint.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Filtro por regla */}
      {reglasDisponibles.length > 1 && (
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs text-muted-foreground">Tipo:</span>
          <button
            onClick={() => setReglaNombreFiltro("TODAS")}
            className={`px-2.5 py-1 rounded-full text-xs font-medium transition-colors ${
              reglaNombreFiltro === "TODAS"
                ? "bg-primary text-primary-foreground"
                : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
            }`}
          >
            Todas ({alertas.length})
          </button>
          {reglasDisponibles.map((regla) => {
            const count = alertas.filter((a) => a.reglaNombre === regla).length;
            return (
              <button
                key={regla}
                onClick={() => setReglaNombreFiltro(regla)}
                className={`px-2.5 py-1 rounded-full text-xs font-medium transition-colors ${
                  reglaNombreFiltro === regla
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                {regla} ({count})
              </button>
            );
          })}
        </div>
      )}

      {/* Grupos por severidad */}
      {SEVERIDAD_ORDER.map((sev) => {
        const items = grupos[sev];
        if (items.length === 0) return null;

        const cfg = SEVERIDAD_CONFIG[sev];
        const SevIcon = cfg.icon;

        return (
          <div key={sev}>
            {/* Cabecera de grupo */}
            <div className={`flex items-center gap-2 mb-2 ${cfg.headerClass}`}>
              <SevIcon className="h-4 w-4" aria-hidden="true" />
              <span className="text-sm font-semibold">{cfg.label}</span>
              <span
                className={`ml-1 rounded-full px-1.5 py-0.5 text-[10px] font-bold ${cfg.badgeClass}`}
              >
                {items.length}
              </span>
            </div>

            {/* Lista de alertas */}
            <ul className="space-y-2">
              {items.map((alerta) => (
                <li
                  key={alerta.id}
                  className={`rounded-lg border border-l-4 p-3 flex items-start gap-3 ${cfg.borderClass} ${cfg.bgClass} border-border`}
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-semibold text-foreground mb-0.5">
                      {alerta.reglaNombre}
                    </p>
                    <p className="text-xs text-muted-foreground leading-relaxed">
                      {alerta.mensaje}
                    </p>
                    {(alerta.jiraKey || alerta.personaNombre) && (
                      <p className="text-[10px] text-muted-foreground/70 mt-1 font-mono truncate">
                        {alerta.jiraKey && <span>{alerta.jiraKey} </span>}
                        {alerta.personaNombre && (
                          <span>· {alerta.personaNombre}</span>
                        )}
                      </p>
                    )}
                  </div>
                  <button
                    onClick={() => resolver.mutate(alerta.id)}
                    disabled={resolver.isPending}
                    className="shrink-0 p-1 rounded hover:bg-black/10 dark:hover:bg-white/10 text-muted-foreground hover:text-foreground transition-colors"
                    title="Resolver alerta"
                    aria-label={`Resolver alerta: ${alerta.mensaje}`}
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                </li>
              ))}
            </ul>
          </div>
        );
      })}
    </div>
  );
};
