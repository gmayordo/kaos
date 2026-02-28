/**
 * Configuración de Plantillas de Asignación (Bloque 5)
 * CRUD de plantillas que asignan horas automáticamente según el tipo de issue Jira.
 */

import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { toast } from "@/lib/toast";
import { useDocumentTitle } from "@/lib/useDocumentTitle";
import { plantillaService } from "@/services/plantillaService";
import type {
  PlantillaAsignacionLineaRequest,
  PlantillaAsignacionRequest,
  PlantillaAsignacionResponse,
  RolPlantilla,
} from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  ChevronDown,
  ChevronRight,
  Plus,
  Save,
  Settings,
  Trash2,
  X,
} from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/configuracion/plantillas")({
  component: PlantillasPage,
});

// ─────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────

const TIPOS_ISSUE_JIRA = ["Story", "Task", "Bug", "Spike", "Sub-task"];

const ROLES_PLANTILLA: { value: RolPlantilla; label: string }[] = [
  { value: "DESARROLLADOR", label: "👨‍💻 Desarrollador" },
  { value: "QA", label: "🧪 QA" },
  { value: "TECH_LEAD", label: "🧭 Tech Lead" },
  { value: "FUNCIONAL", label: "📋 Funcional" },
  { value: "OTRO", label: "🔹 Otro" },
];

// ─────────────────────────────────────────────────────────────────
// Form types
// ─────────────────────────────────────────────────────────────────

type LineaForm = {
  rol: RolPlantilla;
  porcentajeHoras: string;
  orden: string;
  dependeDeOrden: string;
};

type PlantillaForm = {
  nombre: string;
  tipoJira: string;
  activo: boolean;
  lineas: LineaForm[];
};

const emptyLinea = (orden: number): LineaForm => ({
  rol: "DESARROLLADOR",
  porcentajeHoras: "",
  orden: String(orden),
  dependeDeOrden: "",
});

const emptyForm = (): PlantillaForm => ({
  nombre: "",
  tipoJira: "Story",
  activo: true,
  lineas: [emptyLinea(1)],
});

function plantillaToForm(p: PlantillaAsignacionResponse): PlantillaForm {
  return {
    nombre: p.nombre,
    tipoJira: p.tipoJira,
    activo: p.activo,
    lineas: p.lineas.map((l) => ({
      rol: l.rol,
      porcentajeHoras: String(l.porcentajeHoras),
      orden: String(l.orden),
      dependeDeOrden: l.dependeDeOrden != null ? String(l.dependeDeOrden) : "",
    })),
  };
}

function formToRequest(form: PlantillaForm): PlantillaAsignacionRequest {
  const lineas: PlantillaAsignacionLineaRequest[] = form.lineas.map((l) => ({
    rol: l.rol,
    porcentajeHoras: Number(l.porcentajeHoras),
    orden: Number(l.orden),
    dependeDeOrden: l.dependeDeOrden ? Number(l.dependeDeOrden) : undefined,
  }));
  return {
    nombre: form.nombre.trim(),
    tipoJira: form.tipoJira,
    activo: form.activo,
    lineas,
  };
}

// ─────────────────────────────────────────────────────────────────
// Componente formulario (crear / editar)
// ─────────────────────────────────────────────────────────────────

interface PlantillaFormPanelProps {
  initial?: PlantillaAsignacionResponse;
  onSubmit: (data: PlantillaAsignacionRequest) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}

export function PlantillaFormPanel({
  initial,
  onSubmit,
  onCancel,
  isSubmitting,
}: PlantillaFormPanelProps) {
  const [form, setForm] = useState<PlantillaForm>(
    initial ? plantillaToForm(initial) : emptyForm(),
  );

  const totalPct = form.lineas.reduce(
    (acc, l) => acc + (Number(l.porcentajeHoras) || 0),
    0,
  );

  function addLinea() {
    const nextOrden = form.lineas.length + 1;
    setForm((f) => ({ ...f, lineas: [...f.lineas, emptyLinea(nextOrden)] }));
  }

  function removeLinea(idx: number) {
    setForm((f) => ({
      ...f,
      lineas: f.lineas.filter((_, i) => i !== idx),
    }));
  }

  function updateLinea(idx: number, patch: Partial<LineaForm>) {
    setForm((f) => ({
      ...f,
      lineas: f.lineas.map((l, i) => (i === idx ? { ...l, ...patch } : l)),
    }));
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.nombre.trim()) {
      toast.error("El nombre es obligatorio");
      return;
    }
    if (form.lineas.length === 0) {
      toast.error("Añade al menos una línea a la plantilla");
      return;
    }
    onSubmit(formToRequest(form));
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="border border-border rounded-lg bg-card p-5 space-y-4"
    >
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-foreground">
          {initial ? "Editar plantilla" : "Nueva plantilla"}
        </h3>
        <button
          type="button"
          onClick={onCancel}
          className="text-muted-foreground hover:text-foreground transition-colors"
          aria-label="Cancelar"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Nombre + tipoJira + activo */}
      <div className="grid grid-cols-3 gap-3">
        <div className="col-span-2">
          <label className="block text-xs text-muted-foreground mb-1">
            Nombre <span className="text-destructive">*</span>
          </label>
          <input
            type="text"
            value={form.nombre}
            onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))}
            placeholder="Ej: Story estándar"
            className="w-full px-3 py-2 rounded border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            required
          />
        </div>
        <div>
          <label className="block text-xs text-muted-foreground mb-1">
            Tipo Jira <span className="text-destructive">*</span>
          </label>
          <select
            value={form.tipoJira}
            onChange={(e) =>
              setForm((f) => ({ ...f, tipoJira: e.target.value }))
            }
            className="w-full px-3 py-2 rounded border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          >
            {TIPOS_ISSUE_JIRA.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>
      </div>

      <label className="flex items-center gap-2 cursor-pointer select-none">
        <input
          type="checkbox"
          checked={form.activo}
          onChange={(e) => setForm((f) => ({ ...f, activo: e.target.checked }))}
          className="h-4 w-4 rounded border-border"
        />
        <span className="text-sm text-foreground">Activa</span>
      </label>

      {/* Líneas */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-foreground">
            Líneas de asignación
          </span>
          <span
            className={`text-xs ${
              totalPct > 100
                ? "text-destructive font-semibold"
                : totalPct === 100
                  ? "text-green-600 font-semibold"
                  : "text-muted-foreground"
            }`}
          >
            Total: {totalPct}%{totalPct !== 100 && " (debe ser 100%)"}
          </span>
        </div>

        <div className="space-y-2">
          {form.lineas.map((linea, idx) => (
            <div
              key={idx}
              className="grid grid-cols-[auto_1fr_1fr_auto_auto] gap-2 items-end"
            >
              {/* Orden */}
              <div>
                <label className="block text-xs text-muted-foreground mb-1">
                  Orden
                </label>
                <input
                  type="number"
                  value={linea.orden}
                  onChange={(e) => updateLinea(idx, { orden: e.target.value })}
                  min="1"
                  className="w-14 px-2 py-1.5 rounded border border-border bg-background text-foreground text-sm text-center focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              {/* Rol */}
              <div>
                <label className="block text-xs text-muted-foreground mb-1">
                  Rol
                </label>
                <select
                  value={linea.rol}
                  onChange={(e) =>
                    updateLinea(idx, { rol: e.target.value as RolPlantilla })
                  }
                  className="w-full px-2 py-1.5 rounded border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                >
                  {ROLES_PLANTILLA.map((r) => (
                    <option key={r.value} value={r.value}>
                      {r.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* % */}
              <div>
                <label className="block text-xs text-muted-foreground mb-1">
                  % horas
                </label>
                <input
                  type="number"
                  value={linea.porcentajeHoras}
                  onChange={(e) =>
                    updateLinea(idx, { porcentajeHoras: e.target.value })
                  }
                  min="1"
                  max="100"
                  placeholder="50"
                  className="w-full px-2 py-1.5 rounded border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              {/* Depende de */}
              <div>
                <label className="block text-xs text-muted-foreground mb-1">
                  Dep. orden
                </label>
                <input
                  type="number"
                  value={linea.dependeDeOrden}
                  onChange={(e) =>
                    updateLinea(idx, { dependeDeOrden: e.target.value })
                  }
                  min="1"
                  placeholder="—"
                  className="w-16 px-2 py-1.5 rounded border border-border bg-background text-foreground text-sm text-center focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              {/* Eliminar */}
              <button
                type="button"
                onClick={() => removeLinea(idx)}
                disabled={form.lineas.length < 2}
                className="mb-0.5 p-1.5 text-muted-foreground hover:text-destructive transition-colors disabled:opacity-30"
                aria-label={`Eliminar línea ${idx + 1}`}
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={addLinea}
          className="mt-2 flex items-center gap-1 text-xs text-primary hover:underline"
        >
          <Plus className="h-3.5 w-3.5" />
          Añadir línea
        </button>
      </div>

      {/* Botones */}
      <div className="flex gap-2 pt-1">
        <button
          type="submit"
          disabled={isSubmitting || totalPct !== 100}
          className="flex items-center gap-1.5 px-4 py-2 bg-primary text-primary-foreground rounded text-sm font-medium hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          title={
            totalPct !== 100
              ? "El total de porcentajes debe ser 100%"
              : undefined
          }
        >
          <Save className="h-4 w-4" />
          {isSubmitting
            ? "Guardando..."
            : initial
              ? "Actualizar"
              : "Crear plantilla"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={isSubmitting}
          className="px-4 py-2 bg-secondary text-secondary-foreground rounded text-sm font-medium hover:bg-secondary/80 transition-colors disabled:opacity-50"
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}

// ─────────────────────────────────────────────────────────────────
// Tarjeta de plantilla (vista)
// ─────────────────────────────────────────────────────────────────

interface PlantillaCardProps {
  plantilla: PlantillaAsignacionResponse;
  onEdit: () => void;
  onDelete: () => void;
  onToggleActivo: () => void;
}

function PlantillaCard({
  plantilla,
  onEdit,
  onDelete,
  onToggleActivo,
}: PlantillaCardProps) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="border border-border rounded-lg bg-card overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3">
        {/* Expand toggle */}
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          className="flex items-center gap-2 text-left flex-1 min-w-0"
          aria-expanded={expanded}
        >
          {expanded ? (
            <ChevronDown className="h-4 w-4 text-muted-foreground shrink-0" />
          ) : (
            <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0" />
          )}
          <div className="min-w-0">
            <span className="font-medium text-foreground text-sm">
              {plantilla.nombre}
            </span>
            <span className="ml-2 text-xs font-mono bg-muted text-muted-foreground rounded px-1.5 py-0.5">
              {plantilla.tipoJira}
            </span>
          </div>
        </button>

        {/* Badges + acciones */}
        <div className="flex items-center gap-2 shrink-0 ml-3">
          <span className="text-xs text-muted-foreground">
            {plantilla.lineas.length} línea
            {plantilla.lineas.length !== 1 ? "s" : ""}
          </span>
          <button
            type="button"
            onClick={onToggleActivo}
            className={`text-xs px-2 py-0.5 rounded border transition-colors ${
              plantilla.activo
                ? "bg-green-500/10 text-green-600 border-green-400/30 hover:bg-green-500/20"
                : "bg-muted text-muted-foreground border-border hover:bg-muted/80"
            }`}
            title={plantilla.activo ? "Desactivar" : "Activar"}
          >
            {plantilla.activo ? "Activa" : "Inactiva"}
          </button>
          <button
            type="button"
            onClick={onEdit}
            className="text-xs text-muted-foreground hover:text-primary transition-colors px-1.5 py-1 rounded border border-border hover:border-primary"
            aria-label={`Editar ${plantilla.nombre}`}
          >
            Editar
          </button>
          <button
            type="button"
            onClick={onDelete}
            className="text-muted-foreground hover:text-destructive transition-colors p-1"
            aria-label={`Eliminar ${plantilla.nombre}`}
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Líneas expandidas */}
      {expanded && (
        <div className="border-t border-border px-4 py-3">
          <table className="w-full text-xs">
            <thead>
              <tr className="text-muted-foreground border-b border-border/50">
                <th className="text-left pb-1.5 pr-3 w-12">Orden</th>
                <th className="text-left pb-1.5 pr-3">Rol</th>
                <th className="text-right pb-1.5 pr-3 w-24">% horas</th>
                <th className="text-right pb-1.5 w-20">Dep. orden</th>
              </tr>
            </thead>
            <tbody>
              {plantilla.lineas.map((linea) => (
                <tr
                  key={linea.id}
                  className="border-b border-border/30 last:border-0"
                >
                  <td className="py-1.5 pr-3 text-center font-mono">
                    {linea.orden}
                  </td>
                  <td className="py-1.5 pr-3">
                    {ROLES_PLANTILLA.find((r) => r.value === linea.rol)
                      ?.label ?? linea.rol}
                  </td>
                  <td className="py-1.5 pr-3 text-right font-semibold tabular-nums">
                    {linea.porcentajeHoras}%
                  </td>
                  <td className="py-1.5 text-right text-muted-foreground">
                    {linea.dependeDeOrden ?? "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────
// Página principal
// ─────────────────────────────────────────────────────────────────

export function PlantillasPage() {
  useDocumentTitle("Plantillas de asignación");
  const queryClient = useQueryClient();

  const [showForm, setShowForm] = useState(false);
  const [editingPlantilla, setEditingPlantilla] =
    useState<PlantillaAsignacionResponse | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<{
    id: number;
    nombre: string;
  } | null>(null);

  // ── Queries ────────────────────────────────────────────────────
  const {
    data: plantillas = [],
    isLoading,
    error,
  } = useQuery({
    queryKey: ["plantillas"],
    queryFn: plantillaService.listar,
  });

  // ── Mutations ──────────────────────────────────────────────────
  const crearMutation = useMutation({
    mutationFn: plantillaService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["plantillas"] });
      setShowForm(false);
      toast.success("Plantilla creada correctamente");
    },
    onError: () => toast.error("Error al crear la plantilla"),
  });

  const actualizarMutation = useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: number;
      data: PlantillaAsignacionRequest;
    }) => plantillaService.actualizar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["plantillas"] });
      setEditingPlantilla(null);
      toast.success("Plantilla actualizada");
    },
    onError: () => toast.error("Error al actualizar la plantilla"),
  });

  const eliminarMutation = useMutation({
    mutationFn: plantillaService.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["plantillas"] });
      toast.success("Plantilla eliminada");
    },
    onError: () => toast.error("Error al eliminar la plantilla"),
  });

  function toggleActivo(plantilla: PlantillaAsignacionResponse) {
    actualizarMutation.mutate({
      id: plantilla.id,
      data: {
        nombre: plantilla.nombre,
        tipoJira: plantilla.tipoJira,
        activo: !plantilla.activo,
        lineas: plantilla.lineas.map((l) => ({
          rol: l.rol,
          porcentajeHoras: l.porcentajeHoras,
          orden: l.orden,
          dependeDeOrden: l.dependeDeOrden,
        })),
      },
    });
  }

  // ── Render ────────────────────────────────────────────────────
  return (
    <div className="space-y-6 max-w-3xl">
      {/* Breadcrumb */}
      <nav
        className="text-xs text-muted-foreground flex items-center gap-1"
        aria-label="breadcrumb"
      >
        <Link
          to="/configuracion"
          className="hover:text-foreground transition-colors"
        >
          Configuración
        </Link>
        <span>/</span>
        <span className="text-foreground">Plantillas</span>
      </nav>

      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
            <Settings className="h-6 w-6 text-primary" aria-hidden />
            Plantillas de asignación
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Define cómo se reparte el tiempo de cada tipo de issue Jira entre
            roles al planificar automáticamente.
          </p>
        </div>
        {!showForm && !editingPlantilla && (
          <button
            type="button"
            onClick={() => setShowForm(true)}
            className="flex items-center gap-1.5 px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 transition-colors shrink-0"
          >
            <Plus className="h-4 w-4" />
            Nueva plantilla
          </button>
        )}
      </div>

      {/* Formulario crear */}
      {showForm && (
        <PlantillaFormPanel
          onSubmit={(data) => crearMutation.mutate(data)}
          onCancel={() => setShowForm(false)}
          isSubmitting={crearMutation.isPending}
        />
      )}

      {/* Loading */}
      {isLoading && (
        <div className="p-8 text-center">
          <div className="animate-spin h-6 w-6 border-2 border-primary border-t-transparent rounded-full mx-auto mb-2" />
          <p className="text-sm text-muted-foreground">
            Cargando plantillas...
          </p>
        </div>
      )}

      {/* Error */}
      {error && (
        <div
          className="p-4 text-sm text-destructive bg-destructive/10 rounded border border-destructive/20"
          role="alert"
        >
          Error al cargar las plantillas
        </div>
      )}

      {/* Lista */}
      {!isLoading && !error && (
        <div className="space-y-3">
          {plantillas.length === 0 ? (
            <div className="p-8 text-center border border-dashed border-border rounded-lg">
              <Settings
                className="h-10 w-10 text-muted-foreground/40 mx-auto mb-3"
                aria-hidden
              />
              <p className="text-sm text-muted-foreground">
                No hay plantillas creadas todavía.
              </p>
              <button
                type="button"
                onClick={() => setShowForm(true)}
                className="mt-3 text-sm text-primary hover:underline"
              >
                Crear primera plantilla
              </button>
            </div>
          ) : (
            plantillas.map((plantilla) =>
              editingPlantilla?.id === plantilla.id ? (
                <PlantillaFormPanel
                  key={plantilla.id}
                  initial={editingPlantilla}
                  onSubmit={(data) =>
                    actualizarMutation.mutate({ id: plantilla.id, data })
                  }
                  onCancel={() => setEditingPlantilla(null)}
                  isSubmitting={actualizarMutation.isPending}
                />
              ) : (
                <PlantillaCard
                  key={plantilla.id}
                  plantilla={plantilla}
                  onEdit={() => setEditingPlantilla(plantilla)}
                  onDelete={() =>
                    setDeleteConfirm({
                      id: plantilla.id,
                      nombre: plantilla.nombre,
                    })
                  }
                  onToggleActivo={() => toggleActivo(plantilla)}
                />
              ),
            )
          )}
        </div>
      )}

      {/* Confirm delete */}
      <ConfirmDialog
        isOpen={deleteConfirm != null}
        onConfirm={() => {
          if (deleteConfirm) {
            eliminarMutation.mutate(deleteConfirm.id);
            setDeleteConfirm(null);
          }
        }}
        onCancel={() => setDeleteConfirm(null)}
        title="Eliminar plantilla"
        description={`¿Eliminar la plantilla "${deleteConfirm?.nombre}"? Esta acción no se puede deshacer.`}
        variant="danger"
        confirmText="Eliminar"
      />
    </div>
  );
}
