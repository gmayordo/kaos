/**
 * Página principal de Integración Jira
 * Configuración del squad y widget de estado de sincronización.
 */

import { toast } from "@/lib/toast";
import { useDocumentTitle } from "@/lib/useDocumentTitle";
import { jiraConfigService, jiraSyncService } from "@/services/jiraService";
import { squadService } from "@/services/squadService";
import type {
  EstadoSync,
  JiraConfigRequest,
  JiraSyncStatusResponse,
} from "@/types/jira";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import {
  Activity,
  AlertTriangle,
  CheckCircle,
  Clock,
  Edit2,
  Eye,
  EyeOff,
  RefreshCw,
  Save,
  Wifi,
  X,
  XCircle,
  Zap,
} from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/jira/")({
  component: JiraPage,
});

// ─────────────────────────────────────────────────────────────────
// Helpers: semáforo de estado
// ─────────────────────────────────────────────────────────────────

function estadoColor(estado: EstadoSync): string {
  switch (estado) {
    case "IDLE":
      return "text-green-500";
    case "SINCRONIZANDO":
      return "text-blue-500";
    case "CUOTA_AGOTADA":
      return "text-amber-500";
    case "ERROR":
      return "text-destructive";
  }
}

function estadoBgColor(estado: EstadoSync): string {
  switch (estado) {
    case "IDLE":
      return "bg-green-500/10 border-green-500/30";
    case "SINCRONIZANDO":
      return "bg-blue-500/10 border-blue-500/30";
    case "CUOTA_AGOTADA":
      return "bg-amber-500/10 border-amber-500/30";
    case "ERROR":
      return "bg-destructive/10 border-destructive/30";
  }
}

function estadoLabel(estado: EstadoSync): string {
  switch (estado) {
    case "IDLE":
      return "Sincronizado";
    case "SINCRONIZANDO":
      return "Sincronizando...";
    case "CUOTA_AGOTADA":
      return "Cuota agotada";
    case "ERROR":
      return "Error";
  }
}

function EstadoIcon({
  estado,
  className = "h-5 w-5",
}: {
  estado: EstadoSync;
  className?: string;
}) {
  switch (estado) {
    case "IDLE":
      return <CheckCircle className={className} />;
    case "SINCRONIZANDO":
      return <RefreshCw className={`${className} animate-spin`} />;
    case "CUOTA_AGOTADA":
      return <AlertTriangle className={className} />;
    case "ERROR":
      return <XCircle className={className} />;
  }
}

function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("es-ES", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

// ─────────────────────────────────────────────────────────────────
// JiraSyncStatus Widget (presentacional + acciones)
// ─────────────────────────────────────────────────────────────────

interface JiraSyncStatusWidgetProps {
  squadId: number;
}

function JiraSyncStatusWidget({ squadId }: JiraSyncStatusWidgetProps) {
  const queryClient = useQueryClient();

  const { data: status, isLoading } = useQuery({
    queryKey: ["jira-sync-status", squadId],
    queryFn: () => jiraSyncService.obtenerEstado(squadId),
    refetchInterval: 10_000, // polling cada 10s
  });

  const syncMutation = useMutation({
    mutationFn: () => jiraSyncService.syncCompleta(squadId),
    onSuccess: (data: JiraSyncStatusResponse) => {
      queryClient.setQueryData(["jira-sync-status", squadId], data);
      toast.success("Sincronización iniciada");
    },
    onError: () => toast.error("Error al iniciar sincronización"),
  });

  if (isLoading || !status) {
    return (
      <div className="border border-border rounded-lg p-4 bg-card">
        <div className="animate-pulse space-y-2">
          <div className="h-4 bg-muted rounded w-1/3" />
          <div className="h-3 bg-muted rounded w-2/3" />
        </div>
      </div>
    );
  }

  const cuotaPct = Math.round((status.callsConsumidas2h / 200) * 100);
  const isSyncing = syncMutation.isPending || status.estado === "SINCRONIZANDO";

  return (
    <div
      className={`border rounded-lg p-5 bg-card ${estadoBgColor(status.estado)}`}
    >
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-2">
          <span className={estadoColor(status.estado)}>
            <EstadoIcon estado={status.estado} />
          </span>
          <div>
            <h3 className="font-semibold text-foreground">
              Estado de Sincronización
            </h3>
            <p className={`text-sm font-medium ${estadoColor(status.estado)}`}>
              {estadoLabel(status.estado)}
            </p>
          </div>
        </div>
        {status.operacionesPendientes > 0 && (
          <span className="text-xs bg-amber-500/20 text-amber-600 border border-amber-400/40 rounded-full px-2 py-0.5">
            {status.operacionesPendientes} pendiente
            {status.operacionesPendientes !== 1 ? "s" : ""}
          </span>
        )}
      </div>

      {/* Cuota API */}
      <div className="mb-4">
        <div className="flex justify-between text-xs text-muted-foreground mb-1">
          <span className="flex items-center gap-1">
            <Activity className="h-3 w-3" aria-hidden="true" />
            Cuota API (últimas 2h)
          </span>
          <span>
            {status.callsConsumidas2h} / 200 calls
            <span className="text-muted-foreground ml-1">
              ({status.callsRestantes2h} restantes)
            </span>
          </span>
        </div>
        <div
          className="h-2 bg-muted rounded-full overflow-hidden"
          role="progressbar"
          aria-valuenow={cuotaPct}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={`Cuota API: ${cuotaPct}% consumido`}
        >
          <div
            className={`h-2 rounded-full transition-all ${
              cuotaPct >= 90
                ? "bg-destructive"
                : cuotaPct >= 70
                  ? "bg-amber-500"
                  : "bg-green-500"
            }`}
            style={{ width: `${Math.min(cuotaPct, 100)}%` }}
          />
        </div>
        <div className="text-xs text-muted-foreground mt-0.5 text-right">
          {cuotaPct}% consumido
        </div>
      </div>

      {/* Última sync + resultado */}
      <div className="grid grid-cols-5 gap-3 mb-4 text-xs">
        <div className="bg-background/60 rounded p-2">
          <p className="text-muted-foreground flex items-center gap-1 mb-0.5">
            <Clock className="h-3 w-3" /> Última sync
          </p>
          <p className="font-medium text-foreground">
            {formatDateTime(status.ultimaSync)}
          </p>
        </div>
        <div className="bg-background/60 rounded p-2">
          <p className="text-muted-foreground mb-0.5">Issues</p>
          <p className="font-medium text-foreground">
            {status.issuesImportadas.toLocaleString()}
          </p>
        </div>
        <div className="bg-background/60 rounded p-2">
          <p className="text-muted-foreground mb-0.5">Worklogs</p>
          <p className="font-medium text-foreground">
            {status.worklogsImportados.toLocaleString()}
          </p>
        </div>
        <div className="bg-background/60 rounded p-2">
          <p className="text-muted-foreground mb-0.5">Comentarios</p>
          <p className="font-medium text-foreground">
            {status.commentsImportados.toLocaleString()}
          </p>
        </div>
        <div className="bg-background/60 rounded p-2">
          <p className="text-muted-foreground mb-0.5">Remote Links</p>
          <p className="font-medium text-foreground">
            {status.remoteLinksImportados.toLocaleString()}
          </p>
        </div>
      </div>

      {/* Error */}
      {status.ultimoError && (
        <div className="mb-4 text-xs text-destructive bg-destructive/10 rounded p-2 border border-destructive/20">
          <span className="font-medium">Error:</span> {status.ultimoError}
        </div>
      )}

      {/* Botón de sincronización único */}
      <div className="flex gap-2 flex-wrap">
        <button
          onClick={() => syncMutation.mutate()}
          disabled={isSyncing}
          aria-label="Sincronizar todo: issues, worklogs, comentarios y remote links"
          className="flex items-center gap-1.5 px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSyncing ? (
            <RefreshCw className="h-4 w-4 animate-spin" aria-hidden="true" />
          ) : (
            <Zap className="h-4 w-4" aria-hidden="true" />
          )}
          {isSyncing ? "Sincronizando..." : "Sincronizar todo"}
        </button>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────
// Sección de configuración Jira del squad (ver + crear/editar)
// ─────────────────────────────────────────────────────────────────

const EMPTY_FORM: JiraConfigRequest = {
  url: "",
  usuario: "",
  token: "",
  boardCorrectivoId: null,
  boardEvolutivoId: null,
  loadMethod: "API_REST",
  activa: true,
  mapeoEstados:
    '{"Done":"COMPLETADA","In Progress":"EN_PROGRESO","To Do":"PENDIENTE","In Review":"EN_REVISION"}',
};

interface JiraConfigSectionProps {
  squadId: number;
}

function JiraConfigSection({ squadId }: JiraConfigSectionProps) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [showToken, setShowToken] = useState(false);
  const [form, setForm] = useState<JiraConfigRequest>(EMPTY_FORM);

  const {
    data: config,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["jira-config", squadId],
    queryFn: () => jiraConfigService.obtenerConfig(squadId),
    retry: false, // no reintentar si el squad no tiene config
  });

  const testMutation = useMutation({
    mutationFn: () => jiraConfigService.probarConexion(squadId),
  });

  const saveMutation = useMutation({
    mutationFn: (data: JiraConfigRequest) =>
      jiraConfigService.guardarConfig(squadId, data),
    onSuccess: (saved) => {
      queryClient.setQueryData(["jira-config", squadId], saved);
      setEditing(false);
      setShowToken(false);
      toast.success("Configuración Jira guardada");
    },
    onError: () => toast.error("Error al guardar la configuración"),
  });

  // Abrir formulario de edición con datos actuales
  function startEdit() {
    setForm({
      url: config?.url ?? "",
      usuario: config?.usuario ?? "",
      token: "", // vacío = no cambiar el token
      boardCorrectivoId: (config as any)?.boardCorrectivoId ?? null,
      boardEvolutivoId: (config as any)?.boardEvolutivoId ?? null,
      loadMethod:
        (config?.loadMethod as JiraConfigRequest["loadMethod"]) ?? "API_REST",
      activa: config?.activa ?? true,
      mapeoEstados: config?.mapeoEstados ?? EMPTY_FORM.mapeoEstados,
    });
    setEditing(true);
  }

  function cancelEdit() {
    setEditing(false);
    setShowToken(false);
  }

  if (isLoading) {
    return (
      <div className="border border-border rounded-lg p-4 bg-card">
        <div className="animate-pulse space-y-3">
          <div className="h-4 bg-muted rounded w-1/3" />
          <div className="h-3 bg-muted rounded" />
          <div className="h-3 bg-muted rounded w-2/3" />
        </div>
      </div>
    );
  }

  // ── Formulario de creación / edición ──────────────────────────────────────

  if (editing || error || !config) {
    const isCreate = !config;
    return (
      <div className="border border-border rounded-lg p-5 bg-card space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold text-foreground">
            {isCreate
              ? "Nueva configuración Jira"
              : "Editar configuración Jira"}
          </h3>
          {!isCreate && (
            <button
              onClick={cancelEdit}
              className="text-muted-foreground hover:text-foreground transition-colors"
              title="Cancelar"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>

        <div className="space-y-3 text-sm">
          {/* URL */}
          <div>
            <label className="text-xs text-muted-foreground mb-1 block">
              URL Jira <span className="text-destructive">*</span>
            </label>
            <input
              type="url"
              value={form.url}
              onChange={(e) =>
                setForm((f: JiraConfigRequest) => ({
                  ...f,
                  url: e.target.value,
                }))
              }
              placeholder="https://jira.empresa.com"
              className="w-full px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Usuario */}
          <div>
            <label className="text-xs text-muted-foreground mb-1 block">
              Usuario / Email <span className="text-destructive">*</span>
            </label>
            <input
              type="text"
              value={form.usuario}
              onChange={(e) =>
                setForm((f: JiraConfigRequest) => ({
                  ...f,
                  usuario: e.target.value,
                }))
              }
              placeholder="usuario@empresa.com"
              className="w-full px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Token */}
          <div>
            <label className="text-xs text-muted-foreground mb-1 block">
              Token API Jira{" "}
              {isCreate && <span className="text-destructive">*</span>}
              {!isCreate && (
                <span className="text-muted-foreground ml-1">
                  (vacío = mantener el existente)
                </span>
              )}
            </label>
            <div className="relative">
              <input
                type={showToken ? "text" : "password"}
                value={form.token}
                onChange={(e) =>
                  setForm((f: JiraConfigRequest) => ({
                    ...f,
                    token: e.target.value,
                  }))
                }
                placeholder={
                  isCreate
                    ? "ATATxxxxxxxxxxxxxxxx"
                    : "Deja vacío para no cambiar el token"
                }
                className="w-full px-3 py-2 pr-9 rounded-md border border-border bg-background text-foreground font-mono text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              />
              <button
                type="button"
                onClick={() => setShowToken((v) => !v)}
                aria-label={showToken ? "Ocultar token" : "Mostrar token"}
                className="absolute right-2.5 top-2.5 text-muted-foreground hover:text-foreground transition-colors"
              >
                {showToken ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
            {isCreate && (
              <p className="text-xs text-muted-foreground mt-1">
                Genera el token en Jira:{" "}
                <span className="font-mono">
                  Perfil → Seguridad → Tokens de API
                </span>
              </p>
            )}
          </div>

          {/* Boards */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">
                Board ID Correctivos
              </label>
              <input
                type="number"
                value={form.boardCorrectivoId ?? ""}
                onChange={(e) =>
                  setForm((f: JiraConfigRequest) => ({
                    ...f,
                    boardCorrectivoId: e.target.value
                      ? Number(e.target.value)
                      : null,
                  }))
                }
                placeholder="Ej: 42"
                className="w-full px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">
                Board ID Evolutivos
              </label>
              <input
                type="number"
                value={form.boardEvolutivoId ?? ""}
                onChange={(e) =>
                  setForm((f: JiraConfigRequest) => ({
                    ...f,
                    boardEvolutivoId: e.target.value
                      ? Number(e.target.value)
                      : null,
                  }))
                }
                placeholder="Ej: 43"
                className="w-full px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
          </div>

          {/* Método + Activa */}
          <div className="flex items-end gap-4">
            <div className="flex-1">
              <label className="text-xs text-muted-foreground mb-1 block">
                Método de carga <span className="text-destructive">*</span>
              </label>
              <select
                value={form.loadMethod}
                onChange={(e) =>
                  setForm((f: JiraConfigRequest) => ({
                    ...f,
                    loadMethod: e.target
                      .value as JiraConfigRequest["loadMethod"],
                  }))
                }
                className="w-full px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="API_REST">API_REST — Recomendado</option>
                <option value="SELENIUM">SELENIUM — Sin token API</option>
                <option value="LOCAL">LOCAL — Fichero JSON local</option>
              </select>
            </div>
            <label className="flex items-center gap-2 pb-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={form.activa}
                onChange={(e) =>
                  setForm((f: JiraConfigRequest) => ({
                    ...f,
                    activa: e.target.checked,
                  }))
                }
                className="h-4 w-4 rounded border-border"
              />
              <span className="text-sm text-foreground">Activa</span>
            </label>
          </div>

          {/* Mapeo de estados */}
          <div>
            <label className="text-xs text-muted-foreground mb-1 block">
              Mapeo de estados Jira → KAOS{" "}
              <span className="text-muted-foreground">(JSON, opcional)</span>
            </label>
            <textarea
              value={form.mapeoEstados ?? ""}
              onChange={(e) =>
                setForm((f: JiraConfigRequest) => ({
                  ...f,
                  mapeoEstados: e.target.value || null,
                }))
              }
              rows={3}
              placeholder={
                '{"Done":"COMPLETADA","In Progress":"EN_PROGRESO","To Do":"PENDIENTE","In Review":"EN_REVISION"}'
              }
              className="w-full px-3 py-2 rounded-md border border-border bg-background text-foreground font-mono text-xs focus:outline-none focus:ring-2 focus:ring-primary resize-none"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Clave = nombre de estado en Jira · Valor = estado KAOS
            </p>
          </div>
        </div>

        {/* Error guardado */}
        {saveMutation.isError && (
          <div className="text-xs text-destructive bg-destructive/10 rounded p-2 border border-destructive/20">
            Error al guardar:{" "}
            {(saveMutation.error as Error)?.message ?? "Error desconocido"}
          </div>
        )}

        {/* Botones guardar */}
        <div className="flex gap-2 pt-1">
          <button
            onClick={() => saveMutation.mutate(form)}
            disabled={saveMutation.isPending || !form.url || !form.usuario}
            className="flex items-center gap-1.5 px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {saveMutation.isPending ? (
              <RefreshCw className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            {isCreate ? "Guardar configuración" : "Actualizar"}
          </button>
          {!isCreate && (
            <button
              onClick={cancelEdit}
              className="px-4 py-2 bg-secondary text-secondary-foreground rounded-md text-sm font-medium hover:bg-secondary/80 transition-colors"
            >
              Cancelar
            </button>
          )}
        </div>
      </div>
    );
  }

  // ── Vista de solo lectura (config existe) ─────────────────────────────────

  const testResult = testMutation.data;

  return (
    <div className="border border-border rounded-lg p-5 bg-card space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-foreground">Configuración Jira</h3>
        <div className="flex items-center gap-2">
          <span
            className={`text-xs rounded-full px-2 py-0.5 border ${
              config.activa
                ? "bg-green-500/10 text-green-600 border-green-500/30"
                : "bg-muted text-muted-foreground border-border"
            }`}
          >
            {config.activa ? "Activa" : "Inactiva"}
          </span>
          <button
            onClick={startEdit}
            className="flex items-center gap-1 text-xs text-muted-foreground hover:text-primary transition-colors px-2 py-1 rounded border border-border hover:border-primary"
            title="Editar configuración"
          >
            <Edit2 className="h-3 w-3" />
            Editar
          </button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 text-sm">
        <div className="col-span-2">
          <p className="text-xs text-muted-foreground mb-0.5">URL Jira</p>
          <p className="font-medium text-foreground break-all">{config.url}</p>
        </div>
        <div>
          <p className="text-xs text-muted-foreground mb-0.5">Usuario</p>
          <p className="font-medium text-foreground">{config.usuario}</p>
        </div>
        <div>
          <p className="text-xs text-muted-foreground mb-0.5">Token API</p>
          <p className="font-medium text-muted-foreground font-mono">
            {config.tokenOculto}
          </p>
        </div>
        <div>
          <p className="text-xs text-muted-foreground mb-0.5">
            Método de carga
          </p>
          <span className="inline-flex items-center gap-1 text-xs font-mono bg-muted text-foreground rounded px-1.5 py-0.5">
            {config.loadMethod}
          </span>
        </div>
        {config.mapeoEstados && (
          <div className="col-span-2">
            <p className="text-xs text-muted-foreground mb-0.5">
              Mapeo de estados
            </p>
            <p className="font-mono text-xs text-foreground bg-muted rounded px-2 py-1 break-all">
              {config.mapeoEstados}
            </p>
          </div>
        )}
      </div>

      {/* Probar conexión */}
      <div className="flex items-center gap-3 pt-1">
        <button
          onClick={() => testMutation.mutate()}
          disabled={testMutation.isPending}
          className="flex items-center gap-1.5 px-4 py-2 bg-secondary text-secondary-foreground rounded-md text-sm font-medium hover:bg-secondary/80 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {testMutation.isPending ? (
            <RefreshCw className="h-4 w-4 animate-spin" />
          ) : (
            <Wifi className="h-4 w-4" />
          )}
          Probar conexión
        </button>

        {testMutation.isSuccess && (
          <span
            className={`text-sm font-medium flex items-center gap-1 ${
              testResult ? "text-green-600" : "text-destructive"
            }`}
          >
            {testResult ? (
              <>
                <CheckCircle className="h-4 w-4" />
                Conexión OK
              </>
            ) : (
              <>
                <XCircle className="h-4 w-4" />
                Sin conexión
              </>
            )}
          </span>
        )}

        {testMutation.isError && (
          <span className="text-sm text-destructive flex items-center gap-1">
            <XCircle className="h-4 w-4" />
            Error al probar conexión
          </span>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────
// Página principal
// ─────────────────────────────────────────────────────────────────

function JiraPage() {
  useDocumentTitle("Integración Jira");
  const [selectedSquadId, setSelectedSquadId] = useState<number | null>(null);

  const { data: squadsPage, isLoading: loadingSquads } = useQuery({
    queryKey: ["squads-activos"],
    queryFn: () => squadService.listar(0, 100, "ACTIVO"),
  });

  const squads = squadsPage?.content ?? [];

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-foreground">Integración Jira</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Configura la sincronización Jira ↔ KAOS por squad y monitoriza el
          estado de la cuota API.
        </p>
      </div>

      {/* Selector de squad */}
      <div className="flex items-center gap-3">
        <label
          htmlFor="squad-select"
          className="text-sm font-medium text-foreground"
        >
          Squad:
        </label>
        <select
          id="squad-select"
          value={selectedSquadId ?? ""}
          onChange={(e) =>
            setSelectedSquadId(e.target.value ? Number(e.target.value) : null)
          }
          className="px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary min-w-[200px]"
          disabled={loadingSquads}
        >
          <option value="">
            {loadingSquads ? "Cargando squads..." : "Seleccionar squad"}
          </option>
          {squads.map((squad) => (
            <option key={squad.id} value={squad.id}>
              {squad.nombre}
            </option>
          ))}
        </select>
      </div>

      {/* Contenido por squad */}
      {selectedSquadId ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {/* Columna izquierda: Configuración */}
          <div className="space-y-4">
            <JiraConfigSection squadId={selectedSquadId} />
          </div>

          {/* Columna derecha: Estado de sync */}
          <div className="space-y-4">
            <JiraSyncStatusWidget squadId={selectedSquadId} />
          </div>
        </div>
      ) : (
        <div className="border border-dashed border-border rounded-lg p-10 text-center text-muted-foreground">
          <Activity className="h-10 w-10 mx-auto mb-3 opacity-40" />
          <p className="text-sm">
            Selecciona un squad para ver su configuración y estado de
            sincronización.
          </p>
        </div>
      )}
    </div>
  );
}

// Named exports for testing
export { JiraConfigSection, JiraPage, JiraSyncStatusWidget };
