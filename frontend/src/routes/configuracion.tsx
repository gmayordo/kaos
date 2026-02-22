import { perfilHorarioService } from "@/services/perfilHorarioService";
import type { PerfilHorarioRequest, PerfilHorarioResponse } from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createFileRoute,
  Link,
  Outlet,
  useRouterState,
} from "@tanstack/react-router";
import { useState } from "react";

export const Route = createFileRoute("/configuracion")({
  component: ConfiguracionPage,
});

function ConfiguracionPage() {
  const { location } = useRouterState();
  const queryClient = useQueryClient();
  const [editingPerfil, setEditingPerfil] =
    useState<PerfilHorarioResponse | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);

  // Query para listar perfiles
  const {
    data: perfiles,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["perfiles-horario"],
    queryFn: () => perfilHorarioService.listar(0, 100),
  });

  // Mutation para crear
  const crearMutation = useMutation({
    mutationFn: perfilHorarioService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["perfiles-horario"] });
      setIsFormOpen(false);
    },
  });

  // Mutation para actualizar
  const actualizarMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: PerfilHorarioRequest }) =>
      perfilHorarioService.actualizar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["perfiles-horario"] });
      setEditingPerfil(null);
      setIsFormOpen(false);
    },
  });

  // Mutation para eliminar
  const eliminarMutation = useMutation({
    mutationFn: perfilHorarioService.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["perfiles-horario"] });
    },
  });

  // Si la ruta activa es una sub-ruta de configuración, ceder el render
  if (location.pathname.startsWith("/configuracion/")) {
    return <Outlet />;
  }

  const handleSubmit = (data: PerfilHorarioRequest) => {
    if (editingPerfil) {
      actualizarMutation.mutate({ id: editingPerfil.id, data });
    } else {
      crearMutation.mutate(data);
    }
  };

  const handleEdit = (perfil: PerfilHorarioResponse) => {
    setEditingPerfil(perfil);
    setIsFormOpen(true);
  };

  const handleDelete = (id: number) => {
    if (confirm("¿Estás seguro de eliminar este perfil horario?")) {
      eliminarMutation.mutate(id);
    }
  };

  const handleNewPerfil = () => {
    setEditingPerfil(null);
    setIsFormOpen(true);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-muted-foreground">
          Cargando perfiles horario...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-destructive">
          Error al cargar perfiles:{" "}
          {error instanceof Error ? error.message : "Error desconocido"}
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Sección nav: sub-páginas de Configuración */}
      <div className="grid grid-cols-3 gap-3">
        <div className="rounded-lg border-2 border-primary bg-primary/5 px-4 py-3">
          <p className="text-xs font-semibold text-primary uppercase tracking-wide">
            Activo
          </p>
          <p className="font-semibold mt-0.5">Perfiles Horario</p>
          <p className="text-xs text-muted-foreground mt-1">
            Jornadas laborales por zona horaria
          </p>
        </div>
        <Link
          to="/configuracion/festivos"
          className="rounded-lg border border-border bg-card px-4 py-3 hover:bg-accent hover:border-primary/40 transition-colors"
        >
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
            Ir a
          </p>
          <p className="font-semibold mt-0.5">Festivos</p>
          <p className="text-xs text-muted-foreground mt-1">
            Gestionar festivos · Importar CSV
          </p>
        </Link>
        <Link
          to="/configuracion/importar"
          className="rounded-lg border border-border bg-card px-4 py-3 hover:bg-accent hover:border-primary/40 transition-colors"
        >
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
            Ir a
          </p>
          <p className="font-semibold mt-0.5">Importar Vacaciones</p>
          <p className="text-xs text-muted-foreground mt-1">
            Carga masiva desde Excel
          </p>
        </Link>
      </div>

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Configuración</h1>
          <p className="text-muted-foreground mt-1">
            Gestión de perfiles horario y zonas horarias
          </p>
        </div>
        <button
          onClick={handleNewPerfil}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
        >
          + Nuevo Perfil
        </button>
      </div>

      {/* Tabla de perfiles */}
      <div className="bg-card border rounded-lg overflow-hidden">
        <table className="w-full">
          <thead className="bg-muted">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium">
                Nombre
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium">
                Zona Horaria
              </th>
              <th className="px-4 py-3 text-center text-sm font-medium">L</th>
              <th className="px-4 py-3 text-center text-sm font-medium">M</th>
              <th className="px-4 py-3 text-center text-sm font-medium">X</th>
              <th className="px-4 py-3 text-center text-sm font-medium">J</th>
              <th className="px-4 py-3 text-center text-sm font-medium">V</th>
              <th className="px-4 py-3 text-center text-sm font-medium">
                Total
              </th>
              <th className="px-4 py-3 text-right text-sm font-medium">
                Acciones
              </th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {perfiles?.content.map((perfil) => (
              <tr
                key={perfil.id}
                className="hover:bg-muted/50 transition-colors"
              >
                <td className="px-4 py-3 font-medium">{perfil.nombre}</td>
                <td className="px-4 py-3 text-muted-foreground">
                  {perfil.zonaHoraria}
                </td>
                <td className="px-4 py-3 text-center text-sm">
                  {perfil.horasLunes}
                </td>
                <td className="px-4 py-3 text-center text-sm">
                  {perfil.horasMartes}
                </td>
                <td className="px-4 py-3 text-center text-sm">
                  {perfil.horasMiercoles}
                </td>
                <td className="px-4 py-3 text-center text-sm">
                  {perfil.horasJueves}
                </td>
                <td className="px-4 py-3 text-center text-sm">
                  {perfil.horasViernes}
                </td>
                <td className="px-4 py-3 text-center font-semibold">
                  {perfil.totalSemanal}h
                </td>
                <td className="px-4 py-3 text-right space-x-2">
                  <button
                    onClick={() => handleEdit(perfil)}
                    className="text-sm text-primary hover:underline"
                  >
                    Editar
                  </button>
                  <button
                    onClick={() => handleDelete(perfil.id)}
                    className="text-sm text-destructive hover:underline"
                    disabled={eliminarMutation.isPending}
                  >
                    Eliminar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {perfiles?.content.length === 0 && (
          <div className="px-4 py-12 text-center text-muted-foreground">
            No hay perfiles horario configurados. Crea el primero.
          </div>
        )}
      </div>

      {/* Formulario Modal */}
      {isFormOpen && (
        <PerfilHorarioFormModal
          perfil={editingPerfil}
          onSubmit={handleSubmit}
          onCancel={() => {
            setIsFormOpen(false);
            setEditingPerfil(null);
          }}
          isSubmitting={crearMutation.isPending || actualizarMutation.isPending}
        />
      )}
    </div>
  );
}

// --- Modal Form Component ---

interface PerfilHorarioFormModalProps {
  perfil: PerfilHorarioResponse | null;
  onSubmit: (data: PerfilHorarioRequest) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}

function PerfilHorarioFormModal({
  perfil,
  onSubmit,
  onCancel,
  isSubmitting,
}: PerfilHorarioFormModalProps) {
  const [formData, setFormData] = useState<PerfilHorarioRequest>({
    nombre: perfil?.nombre || "",
    zonaHoraria: perfil?.zonaHoraria || "Europe/Madrid",
    horasLunes: perfil?.horasLunes || 8,
    horasMartes: perfil?.horasMartes || 8,
    horasMiercoles: perfil?.horasMiercoles || 8,
    horasJueves: perfil?.horasJueves || 8,
    horasViernes: perfil?.horasViernes || 8,
  });

  const totalSemanal =
    formData.horasLunes +
    formData.horasMartes +
    formData.horasMiercoles +
    formData.horasJueves +
    formData.horasViernes;

  const handleChange = (
    field: keyof PerfilHorarioRequest,
    value: string | number,
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmitForm = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-card border rounded-lg shadow-lg w-full max-w-2xl p-6">
        <h2 className="text-2xl font-bold mb-4">
          {perfil ? "Editar Perfil Horario" : "Nuevo Perfil Horario"}
        </h2>

        <form onSubmit={handleSubmitForm} className="space-y-4">
          {/* Nombre */}
          <div>
            <label className="block text-sm font-medium mb-1">Nombre</label>
            <input
              type="text"
              value={formData.nombre}
              onChange={(e) => handleChange("nombre", e.target.value)}
              className="w-full px-3 py-2 border rounded-md"
              required
              placeholder="España (40h), Chile (45h), etc."
            />
          </div>

          {/* Zona Horaria */}
          <div>
            <label className="block text-sm font-medium mb-1">
              Zona Horaria
            </label>
            <select
              value={formData.zonaHoraria}
              onChange={(e) => handleChange("zonaHoraria", e.target.value)}
              className="w-full px-3 py-2 border rounded-md"
              required
            >
              <option value="Europe/Madrid">Europe/Madrid (España)</option>
              <option value="America/Santiago">America/Santiago (Chile)</option>
              <option value="America/Bogota">America/Bogota (Colombia)</option>
              <option value="America/Mexico_City">
                America/Mexico_City (México)
              </option>
              <option value="America/Argentina/Buenos_Aires">
                America/Argentina/Buenos_Aires (Argentina)
              </option>
            </select>
          </div>

          {/* Horas por día */}
          <div className="grid grid-cols-5 gap-3">
            <div>
              <label className="block text-sm font-medium mb-1">Lunes</label>
              <input
                type="number"
                min="0"
                max="24"
                step="0.5"
                value={formData.horasLunes}
                onChange={(e) =>
                  handleChange("horasLunes", Number(e.target.value))
                }
                className="w-full px-3 py-2 border rounded-md"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Martes</label>
              <input
                type="number"
                min="0"
                max="24"
                step="0.5"
                value={formData.horasMartes}
                onChange={(e) =>
                  handleChange("horasMartes", Number(e.target.value))
                }
                className="w-full px-3 py-2 border rounded-md"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">
                Miércoles
              </label>
              <input
                type="number"
                min="0"
                max="24"
                step="0.5"
                value={formData.horasMiercoles}
                onChange={(e) =>
                  handleChange("horasMiercoles", Number(e.target.value))
                }
                className="w-full px-3 py-2 border rounded-md"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Jueves</label>
              <input
                type="number"
                min="0"
                max="24"
                step="0.5"
                value={formData.horasJueves}
                onChange={(e) =>
                  handleChange("horasJueves", Number(e.target.value))
                }
                className="w-full px-3 py-2 border rounded-md"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Viernes</label>
              <input
                type="number"
                min="0"
                max="24"
                step="0.5"
                value={formData.horasViernes}
                onChange={(e) =>
                  handleChange("horasViernes", Number(e.target.value))
                }
                className="w-full px-3 py-2 border rounded-md"
                required
              />
            </div>
          </div>

          {/* Total Semanal (calculado) */}
          <div className="bg-muted/50 border rounded-md p-3">
            <div className="text-sm font-medium text-muted-foreground">
              Total Semanal
            </div>
            <div className="text-2xl font-bold">{totalSemanal} horas</div>
          </div>

          {/* Botones */}
          <div className="flex justify-end gap-3 pt-4">
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 border rounded-md hover:bg-muted transition-colors"
              disabled={isSubmitting}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors disabled:opacity-50"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Guardando..." : "Guardar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
