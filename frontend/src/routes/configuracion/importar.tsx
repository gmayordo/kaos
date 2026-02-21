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

function ImportarExcelPage() {
  const fileInputRef = useRef<HTMLInputElement>(null);

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
    <div className="p-6 space-y-6 max-w-3xl">
      {/* Breadcrumb */}
      <div className="flex items-center gap-3 text-sm text-muted-foreground">
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
        <h1 className="text-3xl font-bold">Importar Vacaciones</h1>
        <p className="text-muted-foreground mt-1">
          Carga masiva desde ficheros Excel — España FY o Chile CAR.
        </p>
      </div>

      {/* Indicador de paso */}
      <StepIndicator current={step} />

      {/* ── PASO 1: subir fichero ── */}
      {step === "upload" && (
        <form onSubmit={handleAnalizar} className="space-y-5">
          {/* Instrucciones */}
          <div className="bg-muted/40 border rounded-lg p-4 space-y-2 text-sm">
            <p className="font-semibold">Formatos admitidos:</p>
            <ul className="list-disc list-inside space-y-1 text-muted-foreground">
              <li>
                <span className="font-medium text-foreground">España</span> —{" "}
                <code className="bg-muted px-1 rounded">
                  Vacaciones_Syntphony_Connected_Health_FYxx.xlsx
                </code>
              </li>
              <li>
                <span className="font-medium text-foreground">Chile</span> —{" "}
                <code className="bg-muted px-1 rounded">
                  seguimiento_ehCOS_CAR.xlsx
                </code>
              </li>
            </ul>
            <p className="text-muted-foreground pt-1">
              Códigos importados:{" "}
              <code className="bg-muted px-1 rounded">V</code> Vacaciones ·{" "}
              <code className="bg-muted px-1 rounded">LD</code> Libre
              Disposición ·{" "}
              <code className="bg-muted px-1 rounded">AP</code> Asuntos Propios
              · <code className="bg-muted px-1 rounded">LC</code> Licencia ·{" "}
              <code className="bg-muted px-1 rounded">B</code> Baja
            </p>
          </div>

          {/* Selector de fichero */}
          <div className="space-y-1.5">
            <label className="block text-sm font-medium">Fichero Excel</label>
            <div
              className="flex items-center gap-3 border-2 border-dashed border-border rounded-lg px-4 py-5 cursor-pointer hover:border-primary/60 hover:bg-accent/30 transition-colors"
              onClick={() => fileInputRef.current?.click()}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".xlsx,.xls"
                className="hidden"
                onChange={handleFileChange}
              />
              <div className="text-2xl">📊</div>
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
                >
                  ✕
                </button>
              )}
            </div>
          </div>

          {/* Año fiscal */}
          <div className="space-y-1.5">
            <label className="block text-sm font-medium">Año fiscal</label>
            <input
              type="number"
              min={2020}
              max={2040}
              value={año}
              onChange={(e) => setAño(Number(e.target.value))}
              className="w-36 px-3 py-2 border rounded-md text-sm bg-background"
            />
            <p className="text-xs text-muted-foreground">
              Año de referencia (ej. 2026 para FY2026)
            </p>
          </div>

          <button
            type="submit"
            disabled={!selectedFile || analizarMutation.isPending}
            className="px-5 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {analizarMutation.isPending ? "Analizando…" : "Analizar Excel →"}
          </button>

          {analizarMutation.isError && (
            <ErrorBox error={analizarMutation.error} />
          )}
        </form>
      )}

      {/* ── PASO 2: revisión y mapeo ── */}
      {step === "mapping" && analysis && (
        <div className="space-y-6">
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
        <div className="space-y-4">
          {/* Resumen */}
          <div className="grid grid-cols-3 gap-3">
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
            <div className="rounded-lg border border-yellow-400/40 bg-yellow-50 dark:bg-yellow-950/20 p-4">
              <p className="text-sm font-semibold text-yellow-700 dark:text-yellow-400 mb-2">
                ⚠ {result.personasNoEncontradas.length} persona(s) omitidas
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
            <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4">
              <p className="text-sm font-semibold text-destructive mb-2">
                ✕ {result.errores.length} error(es)
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
              <div className="rounded-lg border border-green-400/40 bg-green-50 dark:bg-green-950/20 p-3 text-sm text-green-700 dark:text-green-400 font-medium">
                ✓ Importación completada sin errores
              </div>
            )}

          <button
            onClick={handleReset}
            className="px-4 py-2 border rounded-md text-sm hover:bg-accent transition-colors"
          >
            Nueva importación
          </button>
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
    <div className="flex items-center gap-0">
      {steps.map((s, i) => (
        <div key={s.id} className="flex items-center">
          <div
            className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
              current === s.id
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground"
            }`}
          >
            {s.label}
          </div>
          {i < steps.length - 1 && (
            <div className="w-6 h-px bg-border mx-1" />
          )}
        </div>
      ))}
    </div>
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
    neutral: "bg-card border-border text-foreground",
    success: "bg-green-50 dark:bg-green-950/20 border-green-400/40 text-green-700 dark:text-green-400",
    warn: "bg-yellow-50 dark:bg-yellow-950/20 border-yellow-400/40 text-yellow-700 dark:text-yellow-400",
  };
  return (
    <div className={`rounded-lg border p-4 ${styles[color]}`}>
      <p className="text-2xl font-bold">{value}</p>
      <p className="text-xs text-muted-foreground mt-0.5">{label}</p>
    </div>
  );
}

function ErrorBox({ error }: { error: unknown }) {
  return (
    <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
      <p className="font-semibold">Error</p>
      <p className="mt-1">
        {error instanceof Error ? error.message : "Error desconocido"}
      </p>
    </div>
  );
}
