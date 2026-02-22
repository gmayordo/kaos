import { DedicacionBadge } from "@/features/dedicacion";
import { perfilHorarioService } from "@/services/perfilHorarioService";
import { personaService, type PersonaFilters } from "@/services/personaService";
import { squadMemberService } from "@/services/squadMemberService";
import { squadService } from "@/services/squadService";
import type { PersonaRequest, PersonaResponse } from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Mail, User } from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/personas")({
  component: PersonasPage,
});

function PersonasPage() {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<PersonaFilters>({});
  const [editingPersona, setEditingPersona] = useState<PersonaResponse | null>(
    null,
  );
  const [isFormOpen, setIsFormOpen] = useState(false);

  // Query personas
  const {
    data: personas,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["personas", filters],
    queryFn: () => personaService.listar(0, 100, filters),
  });

  // Query squads (para filtro)
  const { data: squads } = useQuery({
    queryKey: ["squads"],
    queryFn: () => squadService.listar(0, 100),
  });

  // Mutations
  const crearMutation = useMutation({
    mutationFn: personaService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["personas"] });
      setIsFormOpen(false);
    },
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: PersonaRequest }) =>
      personaService.actualizar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["personas"] });
      setEditingPersona(null);
      setIsFormOpen(false);
    },
  });

  const eliminarMutation = useMutation({
    mutationFn: personaService.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["personas"] });
    },
  });

  const handleSubmit = (data: PersonaRequest) => {
    if (editingPersona) {
      actualizarMutation.mutate({ id: editingPersona.id, data });
    } else {
      crearMutation.mutate(data);
    }
  };

  const handleEdit = (persona: PersonaResponse) => {
    setEditingPersona(persona);
    setIsFormOpen(true);
  };

  const handleDelete = (id: number) => {
    if (confirm("¿Estás seguro de eliminar esta persona?")) {
      eliminarMutation.mutate(id);
    }
  };

  const handleFilterChange = (key: keyof PersonaFilters, value: any) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value === "" ? undefined : value,
    }));
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-muted-foreground">
          Cargando personas...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-destructive">
          Error: {error instanceof Error ? error.message : "Error desconocido"}
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Personas</h1>
          <p className="text-muted-foreground mt-1">
            Gestión de integrantes de los equipos
          </p>
        </div>
        <button
          onClick={() => {
            setEditingPersona(null);
            setIsFormOpen(true);
          }}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
        >
          + Nueva Persona
        </button>
      </div>

      {/* Filtros */}
      <div className="bg-card border rounded-lg p-4">
        <h3 className="font-semibold mb-3">Filtros</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {/* Squad */}
          <div>
            <label className="block text-sm font-medium mb-1">Squad</label>
            <select
              value={filters.squadId || ""}
              onChange={(e) =>
                handleFilterChange(
                  "squadId",
                  e.target.value ? Number(e.target.value) : undefined,
                )
              }
              className="w-full px-3 py-1.5 text-sm border rounded-md"
            >
              <option value="">Todos</option>
              {squads?.content.map((squad) => (
                <option key={squad.id} value={squad.id}>
                  {squad.nombre}
                </option>
              ))}
            </select>
          </div>

          {/* Seniority */}
          <div>
            <label className="block text-sm font-medium mb-1">Seniority</label>
            <select
              value={filters.seniority || ""}
              onChange={(e) => handleFilterChange("seniority", e.target.value)}
              className="w-full px-3 py-1.5 text-sm border rounded-md"
            >
              <option value="">Todos</option>
              <option value="JUNIOR">Junior</option>
              <option value="MID">Mid</option>
              <option value="SENIOR">Senior</option>
              <option value="LEAD">Lead</option>
            </select>
          </div>

          {/* Activo */}
          <div>
            <label className="block text-sm font-medium mb-1">Estado</label>
            <select
              value={
                filters.activo === undefined ? "" : filters.activo.toString()
              }
              onChange={(e) =>
                handleFilterChange(
                  "activo",
                  e.target.value === "" ? undefined : e.target.value === "true",
                )
              }
              className="w-full px-3 py-1.5 text-sm border rounded-md"
            >
              <option value="">Todos</option>
              <option value="true">Activos</option>
              <option value="false">Inactivos</option>
            </select>
          </div>
        </div>

        {/* Botón limpiar filtros */}
        {Object.keys(filters).length > 0 && (
          <button
            onClick={() => setFilters({})}
            className="mt-3 text-sm text-primary hover:underline"
          >
            Limpiar filtros
          </button>
        )}
      </div>

      {/* Tabla */}
      <div className="bg-card border rounded-lg overflow-hidden">
        <table className="w-full">
          <thead className="bg-muted">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium">
                Nombre
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium">Email</th>
              <th className="px-4 py-3 text-left text-sm font-medium">
                Seniority
              </th>
              <th className="px-4 py-3 text-left text-sm font-medium">
                Dedicaciones
              </th>
              <th className="px-4 py-3 text-center text-sm font-medium">
                Estado
              </th>
              <th className="px-4 py-3 text-right text-sm font-medium">
                Acciones
              </th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {personas?.content.map((persona) => (
              <tr
                key={persona.id}
                className="hover:bg-muted/50 transition-colors"
              >
                <td className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    <User className="w-4 h-4 text-muted-foreground" />
                    <span className="font-medium">{persona.nombre}</span>
                  </div>
                </td>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-2 text-muted-foreground text-sm">
                    <Mail className="w-3.5 h-3.5" />
                    {persona.email}
                  </div>
                </td>
                <td className="px-4 py-3">
                  <span className="text-sm">{persona.seniority}</span>
                </td>
                <td className="px-4 py-3">
                  <PersonaDedicaciones personaId={persona.id} />
                </td>
                <td className="px-4 py-3 text-center">
                  <span
                    className={`inline-block px-2 py-0.5 text-xs rounded-full ${
                      persona.activo
                        ? "bg-green-100 text-green-800"
                        : "bg-gray-100 text-gray-800"
                    }`}
                  >
                    {persona.activo ? "Activo" : "Inactivo"}
                  </span>
                </td>
                <td className="px-4 py-3 text-right space-x-2">
                  <button
                    onClick={() => handleEdit(persona)}
                    className="text-sm text-primary hover:underline"
                  >
                    Editar
                  </button>
                  <button
                    onClick={() => handleDelete(persona.id)}
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

        {personas?.content.length === 0 && (
          <div className="px-4 py-12 text-center text-muted-foreground">
            No hay personas que coincidan con los filtros.
          </div>
        )}
      </div>

      {/* Formulario Modal */}
      {isFormOpen && (
        <PersonaFormModal
          persona={editingPersona}
          onSubmit={handleSubmit}
          onCancel={() => {
            setIsFormOpen(false);
            setEditingPersona(null);
          }}
          isSubmitting={crearMutation.isPending || actualizarMutation.isPending}
        />
      )}
    </div>
  );
}

// --- Dedicaciones Component ---

interface PersonaDedicacionesProps {
  personaId: number;
}

function PersonaDedicaciones({ personaId }: PersonaDedicacionesProps) {
  const { data: dedicaciones } = useQuery({
    queryKey: ["persona-dedicaciones", personaId],
    queryFn: () => squadMemberService.listarPorPersona(personaId),
  });

  if (!dedicaciones || dedicaciones.length === 0) {
    return (
      <span className="text-xs text-muted-foreground">Sin asignaciones</span>
    );
  }

  return (
    <div className="flex flex-wrap gap-1">
      {dedicaciones.map((d) => (
        <DedicacionBadge
          key={d.id}
          squadNombre={d.squadNombre}
          porcentaje={d.porcentaje}
          rol={d.rol}
        />
      ))}
    </div>
  );
}

// --- Form Modal ---

interface PersonaFormModalProps {
  persona: PersonaResponse | null;
  onSubmit: (data: PersonaRequest) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}

function PersonaFormModal({
  persona,
  onSubmit,
  onCancel,
  isSubmitting,
}: PersonaFormModalProps) {
  // Query perfiles horario para el select
  const { data: perfiles } = useQuery({
    queryKey: ["perfiles-horario"],
    queryFn: () => perfilHorarioService.listar(0, 100),
  });

  const [formData, setFormData] = useState<PersonaRequest>({
    nombre: persona?.nombre || "",
    email: persona?.email || "",
    idJira: persona?.idJira || "",
    ciudad: persona?.ciudad || "",
    perfilHorarioId: persona?.perfilHorarioId || 1,
    seniority: persona?.seniority || "MID",
  });

  const handleChange = (field: keyof PersonaRequest, value: any) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmitForm = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-card border rounded-lg shadow-lg w-full max-w-2xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 className="text-2xl font-bold mb-4">
          {persona ? "Editar Persona" : "Nueva Persona"}
        </h2>

        <form onSubmit={handleSubmitForm} className="space-y-4">
          {/* Nombre */}
          <div>
            <label className="block text-sm font-medium mb-1">
              Nombre Completo *
            </label>
            <input
              type="text"
              value={formData.nombre}
              onChange={(e) => handleChange("nombre", e.target.value)}
              className="w-full px-3 py-2 border rounded-md"
              required
            />
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm font-medium mb-1">Email *</label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) => handleChange("email", e.target.value)}
              className="w-full px-3 py-2 border rounded-md"
              required
            />
          </div>

          {/* Ciudad */}
          <div>
            <label className="block text-sm font-medium mb-1">Ciudad *</label>
            <input
              type="text"
              value={formData.ciudad}
              onChange={(e) => handleChange("ciudad", e.target.value)}
              className="w-full px-3 py-2 border rounded-md"
              required
              placeholder="Madrid, Zaragoza, Temuco..."
            />
          </div>

          {/* ID Jira */}
          <div>
            <label className="block text-sm font-medium mb-1">ID Jira *</label>
            <input
              type="text"
              value={formData.idJira}
              onChange={(e) => handleChange("idJira", e.target.value)}
              className="w-full px-3 py-2 border rounded-md"
              required
              placeholder="username.jira"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            {/* Seniority */}
            <div>
              <label className="block text-sm font-medium mb-1">
                Seniority *
              </label>
              <select
                value={formData.seniority}
                onChange={(e) => handleChange("seniority", e.target.value)}
                className="w-full px-3 py-2 border rounded-md"
                required
              >
                <option value="JUNIOR">Junior</option>
                <option value="MID">Mid</option>
                <option value="SENIOR">Senior</option>
                <option value="LEAD">Lead</option>
              </select>
            </div>

            {/* Perfil Horario */}
            <div>
              <label className="block text-sm font-medium mb-1">
                Perfil Horario *
              </label>
              <select
                value={formData.perfilHorarioId}
                onChange={(e) =>
                  handleChange("perfilHorarioId", Number(e.target.value))
                }
                className="w-full px-3 py-2 border rounded-md"
                required
              >
                {perfiles?.content.map((perfil) => (
                  <option key={perfil.id} value={perfil.id}>
                    {perfil.nombre} ({perfil.totalSemanal}h)
                  </option>
                ))}
              </select>
            </div>
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
