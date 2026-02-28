import { ConfirmDialog } from "@/components/ui";
import { FestivoForm } from "@/features/calendario/FestivoForm";
import { toast } from "@/lib/toast";
import { useDocumentTitle } from "@/lib/useDocumentTitle";
import { festivoService, type FestivoFilters } from "@/services/festivoService";
import type { FestivoRequest, FestivoResponse } from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/configuracion/festivos")({
  component: FestivosPage,
});

function FestivosPage() {
  const queryClient = useQueryClient();
  useDocumentTitle("Festivos");
  const [filters, setFilters] = useState<FestivoFilters>({
    anio: new Date().getFullYear(),
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingFestivo, setEditingFestivo] = useState<FestivoResponse | null>(
    null,
  );
  const [csvResult, setCsvResult] = useState<{
    ok: number;
    errors: number;
  } | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<{
    id: number;
    descripcion: string;
  } | null>(null);

  // Query festivos
  const {
    data: festivos,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["festivos", filters],
    queryFn: () => festivoService.listar(0, 200, filters),
  });

  // Mutations
  const crearMutation = useMutation({
    mutationFn: festivoService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["festivos"] });
      setIsFormOpen(false);
      setEditingFestivo(null);
      toast.success("Festivo creado correctamente");
    },
    onError: () => toast.error("Error al crear el festivo"),
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: FestivoRequest }) =>
      festivoService.actualizar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["festivos"] });
      setIsFormOpen(false);
      setEditingFestivo(null);
      toast.success("Festivo actualizado correctamente");
    },
    onError: () => toast.error("Error al actualizar el festivo"),
  });

  const eliminarMutation = useMutation({
    mutationFn: festivoService.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["festivos"] });
      toast.success("Festivo eliminado");
    },
    onError: () => toast.error("Error al eliminar el festivo"),
  });

  const csvMutation = useMutation({
    mutationFn: festivoService.cargarCsv,
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["festivos"] });
      setCsvResult({ ok: result.exitosas, errors: result.errores.length });
      toast.success(`${result.exitosas} festivos importados`);
    },
    onError: () => toast.error("Error al importar CSV"),
  });

  const handleSubmit = (data: FestivoRequest) => {
    if (editingFestivo) {
      actualizarMutation.mutate({ id: editingFestivo.id, data });
    } else {
      crearMutation.mutate(data);
    }
  };

  const handleEdit = (festivo: FestivoResponse) => {
    setEditingFestivo(festivo);
    setIsFormOpen(true);
  };

  const handleDelete = (id: number, descripcion: string) => {
    setDeleteConfirm({ id, descripcion });
  };

  const handleCsvUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setCsvResult(null);
      csvMutation.mutate(file);
    }
    e.target.value = "";
  };

  const isSubmitting = crearMutation.isPending || actualizarMutation.isPending;

  if (isLoading) {
    return (
      <div
        className="flex items-center justify-center h-full py-16"
        role="status"
      >
        <div className="text-lg text-muted-foreground animate-pulse">
          Cargando festivos...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-full py-16 gap-3">
        <div className="text-destructive">Error al cargar festivos</div>
        <button
          onClick={() =>
            queryClient.invalidateQueries({ queryKey: ["festivos"] })
          }
          className="text-sm text-primary hover:underline"
        >
          Reintentar
        </button>
      </div>
    );
  }

  const tipoColor: Record<string, string> = {
    NACIONAL: "bg-blue-100 text-blue-800",
    REGIONAL: "bg-purple-100 text-purple-800",
    LOCAL: "bg-green-100 text-green-800",
  };

  return (
    <div className="space-y-6">
      {/* Sección nav */}
      <div className="grid grid-cols-2 gap-3">
        <Link
          to="/configuracion"
          className="rounded-lg border border-border bg-card px-4 py-3 hover:bg-accent hover:border-primary/40 transition-colors"
        >
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
            Ir a
          </p>
          <p className="font-semibold mt-0.5">Perfiles Horario</p>
          <p className="text-xs text-muted-foreground mt-1">
            Jornadas laborales por zona horaria
          </p>
        </Link>
        <div className="rounded-lg border-2 border-primary bg-primary/5 px-4 py-3">
          <p className="text-xs font-semibold text-primary uppercase tracking-wide">
            Activo
          </p>
          <p className="font-semibold mt-0.5">Festivos</p>
          <p className="text-xs text-muted-foreground mt-1">
            Gestionar festivos · Importar CSV
          </p>
        </div>
      </div>

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold">Festivos</h2>
          <p className="text-sm text-muted-foreground">
            Gestiona los días festivos por ciudad y tipo
          </p>
        </div>
        <div className="flex items-center gap-2">
          {/* CSV Upload */}
          <label className="cursor-pointer rounded-md border border-border bg-background px-3 py-2 text-sm font-medium text-foreground hover:bg-accent transition-colors">
            {csvMutation.isPending ? "Importando..." : "📂 Importar CSV"}
            <input
              type="file"
              accept=".csv"
              className="hidden"
              onChange={handleCsvUpload}
              disabled={csvMutation.isPending}
            />
          </label>

          <button
            onClick={() => {
              setEditingFestivo(null);
              setIsFormOpen(true);
            }}
            className="flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            Nuevo festivo
          </button>
        </div>
      </div>

      {/* CSV result */}
      {csvResult && (
        <div className="rounded-md border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
          ✅ Importación completada: {csvResult.ok} festivos creados
          {csvResult.errors > 0 && (
            <span className="text-amber-700">
              {" "}
              · {csvResult.errors} con errores
            </span>
          )}
          <button
            onClick={() => setCsvResult(null)}
            className="ml-3 text-green-600 hover:text-green-800"
          >
            ✕
          </button>
        </div>
      )}

      {/* Filtros */}
      <div
        className="flex items-center gap-4"
        role="group"
        aria-label="Filtros de festivos"
      >
        <label htmlFor="festivo-filter-anio" className="text-sm font-medium">
          Año:
        </label>
        <select
          id="festivo-filter-anio"
          value={filters.anio || ""}
          onChange={(e) =>
            setFilters((prev) => ({
              ...prev,
              anio: e.target.value ? Number(e.target.value) : undefined,
            }))
          }
          className="rounded-md border border-border px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
        >
          {[2023, 2024, 2025, 2026, 2027].map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </select>

        <label htmlFor="festivo-filter-tipo" className="text-sm font-medium">
          Tipo:
        </label>
        <select
          id="festivo-filter-tipo"
          value={filters.tipo || ""}
          onChange={(e) =>
            setFilters((prev) => ({
              ...prev,
              tipo: e.target.value || undefined,
            }))
          }
          className="rounded-md border border-border px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
        >
          <option value="">Todos</option>
          <option value="NACIONAL">Nacional</option>
          <option value="REGIONAL">Regional</option>
          <option value="LOCAL">Local</option>
        </select>

        <span className="text-sm text-muted-foreground">
          {festivos?.content?.length ?? 0} festivos
        </span>
      </div>

      {/* Tabla */}
      <div
        className="rounded-md border border-border bg-card overflow-hidden"
        role="region"
        aria-label="Tabla de festivos"
      >
        {!festivos?.content?.length ? (
          <div className="px-4 py-12 text-center text-sm text-muted-foreground">
            No hay festivos para los filtros seleccionados
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm min-w-[600px]">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th
                    scope="col"
                    className="px-4 py-3 text-left font-medium text-muted-foreground"
                  >
                    Fecha
                  </th>
                  <th
                    scope="col"
                    className="px-4 py-3 text-left font-medium text-muted-foreground"
                  >
                    Descripción
                  </th>
                  <th
                    scope="col"
                    className="px-4 py-3 text-left font-medium text-muted-foreground"
                  >
                    Tipo
                  </th>
                  <th
                    scope="col"
                    className="px-4 py-3 text-left font-medium text-muted-foreground"
                  >
                    Ciudad
                  </th>
                  <th
                    scope="col"
                    className="px-4 py-3 text-right font-medium text-muted-foreground"
                  >
                    Acciones
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {festivos.content.map((festivo, index) => (
                  <tr
                    key={festivo.id}
                    className={`hover:bg-muted/50 ${index % 2 === 1 ? "bg-muted/20" : ""}`}
                  >
                    <td className="px-4 py-3 font-medium tabular-nums">
                      {new Intl.DateTimeFormat("es-ES", {
                        day: "2-digit",
                        month: "short",
                        year: "numeric",
                      }).format(new Date(festivo.fecha))}
                    </td>
                    <td className="px-4 py-3">{festivo.descripcion}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                          tipoColor[festivo.tipo] ??
                          "bg-muted text-muted-foreground"
                        }`}
                        title={`Tipo: ${festivo.tipo}`}
                      >
                        {festivo.tipo}
                      </span>
                    </td>
                    <td className="px-4 py-3">{festivo.ciudad}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => handleEdit(festivo)}
                        aria-label={`Editar festivo ${festivo.descripcion}`}
                        className="mr-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
                      >
                        Editar
                      </button>
                      <button
                        onClick={() =>
                          handleDelete(festivo.id, festivo.descripcion)
                        }
                        disabled={eliminarMutation.isPending}
                        aria-label={`Eliminar festivo ${festivo.descripcion}`}
                        className="text-sm text-destructive/70 hover:text-destructive disabled:opacity-50 transition-colors"
                      >
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ConfirmDialog for delete */}
      <ConfirmDialog
        isOpen={!!deleteConfirm}
        onCancel={() => setDeleteConfirm(null)}
        onConfirm={() => {
          if (deleteConfirm) {
            eliminarMutation.mutate(deleteConfirm.id);
            setDeleteConfirm(null);
          }
        }}
        title="Eliminar festivo"
        description={`¿Estás seguro de eliminar el festivo "${deleteConfirm?.descripcion ?? ""}"?`}
        confirmText="Eliminar"
        variant="danger"
      />

      {/* Form Modal */}
      {isFormOpen && (
        <FestivoForm
          festivo={
            editingFestivo ? { ...editingFestivo, id: editingFestivo.id } : null
          }
          onSubmit={handleSubmit}
          onCancel={() => {
            setIsFormOpen(false);
            setEditingFestivo(null);
          }}
          isSubmitting={isSubmitting}
        />
      )}
    </div>
  );
}
