import { FestivoForm } from "@/features/calendario/FestivoForm";
import { festivoService, type FestivoFilters } from "@/services/festivoService";
import type { FestivoRequest, FestivoResponse } from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";

export const Route = createFileRoute("/configuracion/festivos")({
  component: FestivosPage,
});

function FestivosPage() {
  const queryClient = useQueryClient();
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
    },
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: FestivoRequest }) =>
      festivoService.actualizar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["festivos"] });
      setIsFormOpen(false);
      setEditingFestivo(null);
    },
  });

  const eliminarMutation = useMutation({
    mutationFn: festivoService.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["festivos"] });
    },
  });

  const csvMutation = useMutation({
    mutationFn: festivoService.cargarCsv,
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["festivos"] });
      setCsvResult({ ok: result.exitosas, errors: result.errores.length });
    },
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
    if (confirm(`¿Eliminar el festivo "${descripcion}"?`)) {
      eliminarMutation.mutate(id);
    }
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
      <div className="flex items-center justify-center h-full py-16">
        <div className="text-lg text-muted-foreground">
          Cargando festivos...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-full py-16">
        <div className="text-red-500">Error al cargar festivos</div>
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
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            + Nuevo festivo
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
      <div className="flex items-center gap-4">
        <label className="text-sm font-medium">Año:</label>
        <select
          value={filters.anio || ""}
          onChange={(e) =>
            setFilters((prev) => ({
              ...prev,
              anio: e.target.value ? Number(e.target.value) : undefined,
            }))
          }
          className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm"
        >
          {[2023, 2024, 2025, 2026, 2027].map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </select>

        <label className="text-sm font-medium">Tipo:</label>
        <select
          value={filters.tipo || ""}
          onChange={(e) =>
            setFilters((prev) => ({
              ...prev,
              tipo: e.target.value || undefined,
            }))
          }
          className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm"
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
      <div className="rounded-md border border-zinc-200 bg-white overflow-hidden">
        {!festivos?.content?.length ? (
          <div className="px-4 py-12 text-center text-sm text-zinc-500">
            No hay festivos para los filtros seleccionados
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="border-b border-zinc-200 bg-zinc-50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-zinc-700">
                  Fecha
                </th>
                <th className="px-4 py-3 text-left font-medium text-zinc-700">
                  Descripción
                </th>
                <th className="px-4 py-3 text-left font-medium text-zinc-700">
                  Tipo
                </th>
                <th className="px-4 py-3 text-left font-medium text-zinc-700">
                  Ciudad
                </th>
                <th className="px-4 py-3 text-right font-medium text-zinc-700">
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {festivos.content.map((festivo) => (
                <tr key={festivo.id} className="hover:bg-zinc-50">
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
                        tipoColor[festivo.tipo] ?? "bg-zinc-100 text-zinc-700"
                      }`}
                    >
                      {festivo.tipo}
                    </span>
                  </td>
                  <td className="px-4 py-3">{festivo.ciudad}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleEdit(festivo)}
                      className="mr-2 text-sm text-zinc-500 hover:text-zinc-900"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() =>
                        handleDelete(festivo.id, festivo.descripcion)
                      }
                      disabled={eliminarMutation.isPending}
                      className="text-sm text-red-400 hover:text-red-700 disabled:opacity-50"
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

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
