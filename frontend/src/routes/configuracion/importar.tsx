import { personaService } from "@/services/personaService";
import { vacacionService } from "@/services/vacacionService";
import type {
  ExcelAnalysisResponse,
  ExcelImportResponse,
  PersonaResponse,
} from "@/types/api";
import { useMutation, useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useRef, useState } from "react";

export const Route = createFileRoute("/configuracion/importar")({
  component: ImportarExcelPage,
});

type Step = "upload" | "mapping" | "result";
type TooltipState = { show: string; x: number; y: number } | null;

function ImportarExcelPage() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [tooltip, setTooltip] = useState<TooltipState>(null);

  // ── Step state ────────────────────────────────────────────────────────
  const [step, setStep] = useState<Step>("upload");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [año, setAño] = useState<number>(new Date().getFullYear());
  const [analysis, setAnalysis] = useState<ExcelAnalysisResponse | null>(null);
  /** nombre-excel → personaId (resolucion manual para no-resueltos) */
  const [manualMappings, setManualMappings] = useState<Record<string, number>>(
    {},
  );
  const [result, setResult] = useState<ExcelImportResponse | null>(null);

  // ── Personas (para el selector) ───────────────────────────────────────
  const { data: personasPage } = useQuery({
    queryKey: ["personas-all"],
    queryFn: () => personaService.listar(0, 300),
  });
  const personas: PersonaResponse[] = personasPage?.content ?? [];

  // ── Mutations ─────────────────────────────────────────────────────────
  const analizarMutation = useMutation({
    mutationFn: ({ file, año }: { file: File; año: number }) =>
      vacacionService.analizarExcel(file, año),
    onSuccess: (data) => {
      setAnalysis(data);
      // Pre-rellenar mappings con las resueltas automáticamente
      const auto: Record<string, number> = {};
      data.personasResueltas.forEach((m) => {
        auto[m.nombreExcel] = m.personaId;
      });
      setManualMappings(auto);
      setStep("mapping");
    },
  });

  const importarMutation = useMutation({
    mutationFn: ({
      file,
      año,
      mappings,
    }: {
      file: File;
      año: number;
      mappings: Record<string, number>;
    }) => vacacionService.importarExcel(file, año, mappings),
    onSuccess: (data) => {
      setResult(data);
      setStep("result");
    },
  });

  // ── Handlers ──────────────────────────────────────────────────────────
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSelectedFile(e.target.files?.[0] ?? null);
    setAnalysis(null);
    setResult(null);
    setStep("upload");
  };

  const handleAnalizar = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) return;
    analizarMutation.mutate({ file: selectedFile, año });
  };

  const handleSetMapping = (nombreExcel: string, personaId: number | null) => {
    setManualMappings((prev) => {
      const next = { ...prev };
      if (personaId === null) {
        delete next[nombreExcel];
      } else {
        next[nombreExcel] = personaId;
      }
      return next;
    });
  };

  const pendingMappings =
    analysis?.personasNoResueltas.filter(
      (n) => manualMappings[n] === undefined,
    ) ?? [];

  // Validación en tiempo real
  const [yearError, setYearError] = useState<string>("");

  const handleYearChange = (value: number) => {
    if (value < 2020) setYearError("El año debe ser ≥ 2020");
    else if (value > 2040) setYearError("El año debe ser ≤ 2040");
    else setYearError("");
    setAño(value);
  };

  const showTooltip = (
    e: React.MouseEvent<HTMLButtonElement>,
    message: string
  ) => {
    const rect = e.currentTarget.getBoundingClientRect();
    setTooltip({ show: message, x: rect.left, y: rect.top - 40 });
    setTimeout(() => setTooltip(null), 3000);
  };

  const handleImportar = () => {
    if (!selectedFile) return;
    importarMutation.mutate({ file: selectedFile, año, mappings: manualMappings });
  };

  const handleReset = () => {
    setSelectedFile(null);
    setAnalysis(null);
    setResult(null);
    setManualMappings({});
    setStep("upload");
    analizarMutation.reset();
    importarMutation.reset();
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  // ── Render ────────────────────────────────────────────────────────────
  return (
    <div className="p-4 md:p-6 space-y-6 max-w-3xl mx-auto">
      {/* Tooltip flotante */}
      {tooltip && (
        <div
          className="fixed bg-foreground text-background text-xs px-2 py-1 rounded pointer-events-none z-50 animate-in fade-in duration-200"
          style={{
            left: `${tooltip.x}px`,
            top: `${tooltip.y}px`,
            transform: "translateX(-50%)",
          }}
        >
          ℹ {tooltip.show}
        </div>
      )}

      {/* Breadcrumb */}
      <div className="flex items-center gap-3 text-xs md:text-sm text-muted-foreground">
        <Link
          to="/configuracion"
          className="hover:text-foreground transition-colors"
        >
          Configuración
        </Link>
        <span>›</span>
        <span className="text-foreground font-medium">Importar Vacaciones</span>
      </div>

      <div>
        <h1 className="text-2xl md:text-3xl font-bold">Importar Vacaciones</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Carga masiva desde ficheros Excel — España FY o Chile CAR.
        </p>
      </div>

      {/* Indicador de paso */}
      <div className="animate-in fade-in duration-300">
        <StepIndicator current={step} />
      </div>

      {/* ── PASO 1: subir fichero ── */}
      {step === "upload" && (
        <form onSubmit={handleAnalizar} className="space-y-5 animate-in fade-in duration-300">
          {/* Instrucciones */}
          <div className="bg-muted/40 border rounded-lg p-4 space-y-2 text-sm" role="region" aria-label="Instrucciones de formato">
            <p className="font-semibold">Formatos admitidos:</p>
            <ul className="list-disc list-inside space-y-1 text-muted-foreground">
              <li>
                <span className="font-medium text-foreground">España</span> —{" "}
                <code className="bg-muted px-1 rounded text-xs">
                  Vacaciones_Syntphony_Connected_Health_FYxx.xlsx
                </code>
              </li>
              <li>
                <span className="font-medium text-foreground">Chile</span> —{" "}
                <code className="bg-muted px-1 rounded text-xs">
                  seguimiento_ehCOS_CAR.xlsx
                </code>
              </li>
            </ul>
            <div className="text-muted-foreground pt-2 space-y-1">
              <p className="text-xs">Códigos automáticos:</p>
              <div className="flex flex-wrap gap-2 text-xs">
                <CodeBadge code="V" label="Vacaciones" />
                <CodeBadge code="LD" label="Libre Disposición" />
                <CodeBadge code="AP" label="Asuntos Propios" />
                <CodeBadge code="LC" label="Permiso/Licencia" />
                <CodeBadge code="B" label="Baja Médica" />
              </div>
            </div>
          </div>

          {/* Selector de fichero */}
          <div className="space-y-1.5">
            <label className="block text-sm font-medium">Fichero Excel</label>
            <div
              className="flex items-center gap-3 border-2 border-dashed border-border rounded-lg px-4 py-5 cursor-pointer hover:border-primary/60 hover:bg-accent/30 transition-colors"
              onClick={() => fileInputRef.current?.click()}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === "Enter") fileInputRef.current?.click();
              }}
              aria-label="Seleccionar fichero Excel"
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".xlsx,.xls"
                className="hidden"
                onChange={handleFileChange}
                aria-label="Carga de fichero Excel para importación masiva"
              />
              <div className="text-2xl grid-center">📊</div>
              <div className="flex-1 min-w-0">
                {selectedFile ? (
                  <>
                    <p className="font-medium text-sm truncate">
                      {selectedFile.name}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {(selectedFile.size / 1024).toFixed(1)} KB
                    </p>
                  </>
                ) : (
                  <>
                    <p className="font-medium text-sm">
                      Haz clic para seleccionar
                    </p>
                    <p className="text-xs text-muted-foreground">.xlsx o .xls</p>
                  </>
                )}
              </div>
              {selectedFile && (
                <button
                  type="button"
                  className="text-muted-foreground hover:text-destructive transition-colors text-sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleReset();
                  }}
                  aria-label="Eliminar fichero seleccionado"
                >
                  ✕
                </button>
              )}
            </div>
          </div>

          {/* Año fiscal */}
          <div className="space-y-1.5">
            <label className="block text-sm font-medium flex items-center gap-2">
              Año fiscal
              <button
                type="button"
                className="text-muted-foreground hover:text-foreground text-xs cursor-help transition-colors"
                onClick={(e) =>
                  showTooltip(e, "Formato fiscal: 2026 es FY2026 (ene-dic 2026)")
                }
              >
                ℹ
              </button>
            </label>
            <input
              type="number"
              min={2020}
              max={2040}
              value={año}
              onChange={(e) => handleYearChange(Number(e.target.value))}
              className={`w-full sm:w-36 px-3 py-2 border rounded-md text-sm bg-background transition-colors ${
                yearError ? "border-destructive focus:ring-destructive/20" : "border-border"
              }`}
              aria-label="Año fiscal de referencia"
              aria-describedby={yearError ? "year-error" : undefined}
            />
            {yearError ? (
              <p className="text-xs text-destructive" id="year-error">
                ✗ {yearError}
              </p>
            ) : (
              <p className="text-xs text-muted-foreground">
                Año de referencia (ej. 2026 para FY2026)
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={!selectedFile || analizarMutation.isPending || !!yearError}
            className="px-5 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
          >
            {analizarMutation.isPending && <span className="inline-block w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />}
            {analizarMutation.isPending ? "Analizando…" : "Analizar Excel →"}
          </button>

          {analizarMutation.isPending && (
            <div className="space-y-3 animate-in fade-in">
              <div className="text-xs text-muted-foreground">Cargando análisis...</div>
              <div className="space-y-2">
                <div className="h-4 bg-muted rounded animate-pulse" />
                <div className="h-4 bg-muted rounded animate-pulse w-5/6" />
                <div className="h-4 bg-muted rounded animate-pulse w-4/6" />
              </div>
            </div>
          )}

          {analizarMutation.isError && (
            <ErrorBox error={analizarMutation.error} />
          )}
        </form>
      )}

      {/* ── PASO 2: revisión y mapeo ── */}
      {step === "mapping" && analysis && (
        <div className="space-y-6 animate-in fade-in duration-300">
          {/* Resumen del análisis */}
          <div className="grid grid-cols-3 gap-3">
            <StatCard
              label="Personas en Excel"
              value={analysis.totalFilasPersona}
              color="neutral"
            />
            <StatCard
              label="Auto-resueltas"
              value={analysis.personasResueltas.length}
              color="success"
            />
            <StatCard
              label="Sin resolver"
              value={analysis.personasNoResueltas.length}
              color={analysis.personasNoResueltas.length > 0 ? "warn" : "success"}
            />
          </div>

          {/* Personas auto-resueltas */}
          {analysis.personasResueltas.length > 0 && (
            <div className="space-y-2">
              <p className="text-sm font-semibold flex items-center gap-2">
                <span className="text-green-600">✓</span> Auto-resueltas (
                {analysis.personasResueltas.length})
              </p>
              <div className="border rounded-lg divide-y text-sm max-h-52 overflow-y-auto">
                {analysis.personasResueltas.map((m) => (
                  <div
                    key={m.nombreExcel}
                    className="flex items-center gap-2 px-3 py-1.5"
                  >
                    <span className="text-muted-foreground flex-1 truncate font-mono text-xs">
                      {m.nombreExcel}
                    </span>
                    <span className="text-xs text-muted-foreground">→</span>
                    <span className="font-medium">{m.personaNombre}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Personas no resueltas → selector */}
          {analysis.personasNoResueltas.length > 0 && (
            <div className="space-y-3">
              <p className="text-sm font-semibold flex items-center gap-2">
                <span className="text-yellow-500">⚠</span> Requieren mapeo
                manual ({analysis.personasNoResueltas.length})
              </p>
              <p className="text-xs text-muted-foreground">
                Asigna cada nombre del Excel a la persona correspondiente en la
                BD, o déjalo en blanco para omitirlo.
              </p>
              <div className="border rounded-lg divide-y text-sm">
                {analysis.personasNoResueltas.map((nombre) => (
                  <div
                    key={nombre}
                    className="flex items-center gap-3 px-3 py-2"
                  >
                    <span className="flex-1 truncate font-mono text-xs text-muted-foreground">
                      {nombre}
                    </span>
                    <span className="text-xs text-muted-foreground">→</span>
                    <select
                      value={manualMappings[nombre] ?? ""}
                      onChange={(e) =>
                        handleSetMapping(
                          nombre,
                          e.target.value ? Number(e.target.value) : null,
                        )
                      }
                      className="border rounded px-2 py-1 text-xs bg-background max-w-[200px]"
                    >
                      <option value="">— omitir —</option>
                      {personas.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.nombre}
                        </option>
                      ))}
                    </select>
                  </div>
                ))}
              </div>
              {pendingMappings.length > 0 && (
                <p className="text-xs text-yellow-600 dark:text-yellow-400">
                  {pendingMappings.length} nombre(s) sin asignar serán omitidos
                  durante la importación.
                </p>
              )}
            </div>
          )}

          {/* Acciones */}
          <div className="flex items-center gap-3 pt-2">
            <button
              onClick={handleImportar}
              disabled={importarMutation.isPending}
              className="px-5 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {importarMutation.isPending ? "Importando…" : "Confirmar e Importar"}
            </button>
            <button
              onClick={handleReset}
              className="px-4 py-2 border rounded-md text-sm hover:bg-accent transition-colors"
            >
              Cancelar
            </button>
          </div>

          {importarMutation.isError && (
            <ErrorBox error={importarMutation.error} />
          )}
        </div>
      )}

      {/* ── PASO 3: resultado ── */}
      {step === "result" && result && (
        <div className="space-y-4 animate-in fade-in duration-300">
          {/* Resumen */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <StatCard
              label="Personas procesadas"
              value={result.personasProcesadas}
              color="neutral"
            />
            <StatCard
              label="Vacaciones creadas"
              value={result.vacacionesCreadas}
              color="success"
            />
            <StatCard
              label="Ausencias creadas"
              value={result.ausenciasCreadas}
              color="success"
            />
          </div>

          {result.personasNoEncontradas.length > 0 && (
            <div className="rounded-lg border border-yellow-400/40 bg-yellow-50 dark:bg-yellow-950/20 p-4 animate-in slide-in-from-bottom">
              <p className="text-sm font-semibold text-yellow-700 dark:text-yellow-400 mb-2 flex items-center gap-2">
                <span className="text-lg">⚠</span>
                {result.personasNoEncontradas.length} persona(s) omitidas
              </p>
              <ul className="text-xs space-y-0.5 text-yellow-600 dark:text-yellow-300">
                {result.personasNoEncontradas.map((n) => (
                  <li key={n} className="font-mono">
                    · {n}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {result.errores.length > 0 && (
            <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 animate-in slide-in-from-bottom">
              <p className="text-sm font-semibold text-destructive mb-2 flex items-center gap-2">
                <span className="text-lg">✕</span>
                {result.errores.length} error(es)
              </p>
              <ul className="text-xs space-y-1 text-destructive/80">
                {result.errores.map((e, i) => (
                  <li key={i} className="font-mono">
                    · {e}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {result.personasNoEncontradas.length === 0 &&
            result.errores.length === 0 && (
              <div className="rounded-lg border border-green-400/40 bg-green-50 dark:bg-green-950/20 p-4 text-sm text-green-700 dark:text-green-400 font-medium animate-in slide-in-from-bottom">
                <div className="flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-green-500 text-white flex items-center justify-center animate-in zoom-in">
                    ✓
                  </span>
                  Importación completada sin errores
                </div>
              </div>
            )}

          <div className="flex gap-3 pt-2 flex-wrap">
            <button
              onClick={handleReset}
              className="px-4 py-2 border rounded-md text-sm hover:bg-accent transition-colors"
              aria-label="Realizar nueva importación"
            >
              Nueva importación
            </button>
            <Link
              to="/calendario"
              className="px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm hover:bg-primary/90 transition-colors"
            >
              Ver Calendario
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}

// ──────────────────────────────────────────
// Componentes auxiliares
// ──────────────────────────────────────────

function StepIndicator({ current }: { current: Step }) {
  const steps: { id: Step; label: string }[] = [
    { id: "upload", label: "1. Subir fichero" },
    { id: "mapping", label: "2. Revisar mapeo" },
    { id: "result", label: "3. Resultado" },
  ];
  return (
    <nav className="flex items-center gap-0 flex-wrap" aria-label="Pasos del proceso de importación">
      {steps.map((s, i) => (
        <div key={s.id} className="flex items-center">
          <div
            className={`px-3 py-1 rounded-full text-xs font-medium transition-all duration-200 ${
              current === s.id
                ? "bg-primary text-primary-foreground shadow-md scale-105"
                : current > s.id
                  ? "bg-green-500/20 text-green-700 dark:text-green-400"
                  : "bg-muted text-muted-foreground"
            }`}
            aria-current={current === s.id ? "step" : undefined}
          >
            {current > s.id ? "✓" : s.label}
          </div>
          {i < steps.length - 1 && (
            <div className={`w-6 h-px mx-1 transition-colors ${
              current > s.id ? "bg-green-500/40" : "bg-border"
            }`} />
          )}
        </div>
      ))}
    </nav>
  );
}

function StatCard({
  label,
  value,
  color,
}: {
  label: string;
  value: number;
  color: "neutral" | "success" | "warn";
}) {
  const styles = {
    neutral: "bg-card border-border text-foreground hover:shadow-md",
    success: "bg-green-50 dark:bg-green-950/20 border-green-400/40 text-green-700 dark:text-green-400 hover:shadow-md hover:shadow-green-200/50 dark:hover:shadow-green-950/50",
    warn: "bg-yellow-50 dark:bg-yellow-950/20 border-yellow-400/40 text-yellow-700 dark:text-yellow-400 hover:shadow-md hover:shadow-yellow-200/50 dark:hover:shadow-yellow-950/50",
  };
  return (
    <div className={`rounded-lg border p-4 transition-all duration-200 ${styles[color]}`}>
      <p className="text-2xl md:text-3xl font-bold">{value}</p>
      <p className="text-xs text-muted-foreground mt-0.5">{label}</p>
    </div>
  );
}

function ErrorBox({ error }: { error: unknown }) {
  return (
    <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive animate-in slide-in-from-top" role="alert">
      <p className="font-semibold flex items-center gap-2">
        <span className="text-lg">⚠</span>
        Error
      </p>
      <p className="mt-1">
        {error instanceof Error ? error.message : "Error desconocido"}
      </p>
    </div>
  );
}

function CodeBadge({ code, label }: { code: string; label: string }) {
  const [showLabel, setShowLabel] = useState(false);
  return (
    <div
      className="relative"
      onMouseEnter={() => setShowLabel(true)}
      onMouseLeave={() => setShowLabel(false)}
    >
      <code className="bg-muted px-2 py-1 rounded text-xs font-mono hover:bg-muted/80 transition-colors cursor-help border border-muted-foreground/20">
        {code}
      </code>
      {showLabel && (
        <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-2 py-1 rounded bg-foreground text-background text-xs whitespace-nowrap z-10 animate-in fade-in zoom-in-95 duration-100">
          {label}
          <div className="absolute top-full left-1/2 transform -translate-x-1/2 border-4 border-transparent border-t-foreground" />
        </div>
      )}
    </div>
  );
}
