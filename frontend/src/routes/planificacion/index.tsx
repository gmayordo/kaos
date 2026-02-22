/**
 * Página principal de Planificación
 * Permite seleccionar squad + sprint y navegar a las vistas especializadas.
 */

import { ModalTarea, SprintSelector } from "@/features/planificacion";
import { personaService } from "@/services/personaService";
import { sprintService } from "@/services/sprintService";
import { squadService } from "@/services/squadService";
import { tareaService } from "@/services/tareaService";
import type {
  SprintEstado,
  SprintRequest,
  SprintResponse,
  SquadRequest,
  SquadResponse,
} from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { CalendarRange, Kanban, LayoutDashboard, Plus, X } from "lucide-react";
import { useEffect, useState } from "react";

/** Modal inline para crear un sprint */
function ModalNuevoSprint({
  squads,
  onClose,
  onCreated,
}: {
  squads: SquadResponse[];
  onClose: () => void;
  onCreated: () => void;
}) {
  const [nombre, setNombre] = useState("");
  const [squadId, setSquadId] = useState<number | "">("");
  const [fechaInicio, setFechaInicio] = useState("");
  const [objetivo, setObjetivo] = useState("");

  const crearSprint = useMutation({
    mutationFn: sprintService.crear,
    onSuccess: () => {
      onCreated();
      onClose();
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!nombre.trim() || !fechaInicio || squadId === "") return;
    const req: SprintRequest = {
      nombre,
      squadId: Number(squadId),
      fechaInicio,
      objetivo: objetivo || undefined,
    };
    crearSprint.mutate(req);
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-card border border-border rounded-lg p-6 w-full max-w-md shadow-xl">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-foreground">
            Nuevo Sprint
          </h2>
          <button
            onClick={onClose}
            className="text-muted-foreground hover:text-foreground"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Nombre <span className="text-destructive">*</span>
            </label>
            <input
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              className="w-full px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              placeholder="Sprint 1"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Squad <span className="text-destructive">*</span>
            </label>
            <select
              value={squadId}
              onChange={(e) =>
                setSquadId(e.target.value ? Number(e.target.value) : "")
              }
              className="w-full px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              required
            >
              <option value="">Seleccionar squad</option>
              {squads.map((squad) => (
                <option key={squad.id} value={squad.id}>
                  {squad.nombre}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Fecha inicio <span className="text-destructive">*</span>
            </label>
            <input
              type="date"
              value={fechaInicio}
              onChange={(e) => setFechaInicio(e.target.value)}
              className="w-full px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1">
              Objetivo
            </label>
            <textarea
              value={objetivo}
              onChange={(e) => setObjetivo(e.target.value)}
              rows={2}
              className="w-full px-3 py-2 rounded-md border border-border bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary resize-none"
              placeholder="¿Qué queremos lograr en este sprint?"
            />
          </div>
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={
                crearSprint.isPending ||
                !nombre.trim() ||
                !fechaInicio ||
                squadId === ""
              }
              className="px-4 py-2 text-sm rounded-md bg-primary text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
            >
              {crearSprint.isPending ? "Creando..." : "Crear Sprint"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export const Route = createFileRoute("/planificacion/")({
  component: PlanificacionPage,
});

function PlanificacionPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [selectedSprint, setSelectedSprint] = useState<SprintResponse | null>(
    null,
  );
  const [isNuevoSprintOpen, setIsNuevoSprintOpen] = useState(false);
  const [isModalTareaOpen, setIsModalTareaOpen] = useState(false);
  const [isGestionSquadsOpen, setIsGestionSquadsOpen] = useState(false);
  const [editingSquad, setEditingSquad] = useState<SquadResponse | null>(null);
  const [isSquadFormOpen, setIsSquadFormOpen] = useState(false);
  const [filtroSprintEstado, setFiltroSprintEstado] = useState<
    SprintEstado | ""
  >("");
  const [filtroEstado, setFiltroEstado] = useState<
    "ACTIVO" | "INACTIVO" | undefined
  >(undefined);

  // ── Squads ──────────────────────────────────────────────────────────────────
  const { data: squadsData } = useQuery({
    queryKey: ["squads", "activos"],
    queryFn: () => squadService.listar(0, 100, "ACTIVO"),
  });
  const squads = squadsData?.content ?? [];

  const {
    data: squadsGestionData,
    isLoading: isLoadingSquadsGestion,
    error: errorSquadsGestion,
  } = useQuery({
    queryKey: ["squads", "gestion", filtroEstado],
    queryFn: () => squadService.listar(0, 100, filtroEstado),
    enabled: isGestionSquadsOpen,
  });
  const squadsGestion = squadsGestionData?.content ?? [];

  // ── Sprints globales (comunes a todos los squads) ───────────────────────────
  const { data: sprintsData } = useQuery({
    queryKey: ["sprints", filtroSprintEstado],
    queryFn: () =>
      sprintService.listar(0, 50, {
        estado: filtroSprintEstado || undefined,
      }),
  });
  const sprints = sprintsData?.content ?? [];

  // Sincronizar selectedSprint cuando la lista se refresca (ej. tras cambio de estado)
  useEffect(() => {
    if (selectedSprint) {
      const updated = sprints.find((s) => s.id === selectedSprint.id);
      if (updated && updated.estado !== selectedSprint.estado) {
        setSelectedSprint(updated);
      }
    }
  }, [sprints]);

  // ── Personas (para ModalTarea) ───────────────────────────────────────────────
  const { data: personasData } = useQuery({
    queryKey: ["personas"],
    queryFn: () => personaService.listar(0, 200),
  });
  const personas = personasData?.content ?? [];

  // ── Conteo tareas del sprint ─────────────────────────────────────────────────
  const { data: tareasData } = useQuery({
    queryKey: ["tareas", selectedSprint?.id],
    queryFn: () =>
      tareaService.listar(0, 200, { sprintId: selectedSprint?.id }),
    enabled: selectedSprint !== null,
  });
  const tareas = tareasData?.content ?? [];

  // ── Mutations sprint ─────────────────────────────────────────────────────────
  const activarSprint = useMutation({
    mutationFn: (id: number) => sprintService.cambiarEstado(id, "ACTIVO"),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sprints"] });
    },
  });

  const cerrarSprint = useMutation({
    mutationFn: (id: number) => sprintService.cambiarEstado(id, "CERRADO"),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sprints"] });
    },
  });

  const replanificarSprint = useMutation({
    mutationFn: (id: number) => sprintService.cambiarEstado(id, "PLANIFICACION"),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sprints"] });
    },
  });

  const eliminarSprint = useMutation({
    mutationFn: (id: number) => sprintService.eliminar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sprints"] });
      setSelectedSprint(null);
    },
  });

  // ── Mutations squads ───────────────────────────────────────────────────────
  const crearSquad = useMutation({
    mutationFn: squadService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["squads", "activos"] });
      queryClient.invalidateQueries({ queryKey: ["squads", "gestion"] });
      setEditingSquad(null);
      setIsSquadFormOpen(false);
    },
  });

  const actualizarSquad = useMutation({
    mutationFn: ({ id, data }: { id: number; data: SquadRequest }) =>
      squadService.actualizar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["squads", "activos"] });
      queryClient.invalidateQueries({ queryKey: ["squads", "gestion"] });
      setEditingSquad(null);
      setIsSquadFormOpen(false);
    },
  });

  const desactivarSquad = useMutation({
    mutationFn: squadService.desactivar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["squads", "activos"] });
      queryClient.invalidateQueries({ queryKey: ["squads", "gestion"] });
    },
  });

  // ── Mutation crear tarea ─────────────────────────────────────────────────────
  const crearTarea = useMutation({
    mutationFn: tareaService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["tareas", selectedSprint?.id],
      });
      setIsModalTareaOpen(false);
    },
  });

  // ── Handlers navegación ──────────────────────────────────────────────────────
  const goToTimeline = () => {
    if (selectedSprint) {
      navigate({
        to: "/planificacion/timeline/$sprintId",
        params: { sprintId: String(selectedSprint.id) },
      });
    }
  };

  const goToKanban = () => {
    if (selectedSprint) {
      navigate({
        to: "/planificacion/kanban/$sprintId",
        params: { sprintId: String(selectedSprint.id) },
      });
    }
  };

  const goToDashboard = () => {
    if (selectedSprint) {
      navigate({
        to: "/planificacion/dashboard/$sprintId",
        params: { sprintId: String(selectedSprint.id) },
      });
    }
  };

  const handleSubmitSquad = (data: SquadRequest) => {
    if (editingSquad) {
      actualizarSquad.mutate({ id: editingSquad.id, data });
    } else {
      crearSquad.mutate(data);
    }
  };

  const handleDesactivarSquad = (id: number) => {
    const confirmed = window.confirm("¿Estás seguro de desactivar este squad?");
    if (confirmed) desactivarSquad.mutate(id);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Planificación</h1>
          <p className="text-muted-foreground mt-1">
            Gestiona sprints, tareas y capacidad de los equipos
          </p>
        </div>
      </div>

      {/* Gestión de squads */}
      <div className="bg-card border border-border rounded-lg p-6 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-foreground">Squads</h2>
          </div>
          <button
            onClick={() => setIsGestionSquadsOpen((prev) => !prev)}
            className="px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
          >
            {isGestionSquadsOpen ? "Ocultar gestión" : "Gestionar squads"}
          </button>
        </div>

        {squads.length === 0 && (
          <div className="flex items-center justify-between gap-4 text-sm text-muted-foreground">
            <span>No hay squads activos disponibles.</span>
            <button
              onClick={() => setIsGestionSquadsOpen(true)}
              className="px-3 py-1.5 text-sm rounded-md bg-primary text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              Crear squad
            </button>
          </div>
        )}
      </div>

      {/* Gestión de squads */}
      {isGestionSquadsOpen && (
        <div className="bg-card border border-border rounded-lg p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold text-foreground">
                Gestionar squads
              </h2>
              <p className="text-sm text-muted-foreground">
                Crea, edita o desactiva squads sin salir de planificación.
              </p>
            </div>
            <button
              onClick={() => {
                setEditingSquad(null);
                setIsSquadFormOpen(true);
              }}
              className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
            >
              + Nuevo Squad
            </button>
          </div>

          <div className="flex gap-2">
            <button
              onClick={() => setFiltroEstado(undefined)}
              className={`px-3 py-1.5 text-sm rounded-md transition-colors ${
                filtroEstado === undefined
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted hover:bg-muted/80"
              }`}
            >
              Todos
            </button>
            <button
              onClick={() => setFiltroEstado("ACTIVO")}
              className={`px-3 py-1.5 text-sm rounded-md transition-colors ${
                filtroEstado === "ACTIVO"
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted hover:bg-muted/80"
              }`}
            >
              Activos
            </button>
            <button
              onClick={() => setFiltroEstado("INACTIVO")}
              className={`px-3 py-1.5 text-sm rounded-md transition-colors ${
                filtroEstado === "INACTIVO"
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted hover:bg-muted/80"
              }`}
            >
              Inactivos
            </button>
          </div>

          {isLoadingSquadsGestion && (
            <div className="text-sm text-muted-foreground">
              Cargando squads...
            </div>
          )}

          {errorSquadsGestion && (
            <div className="text-sm text-destructive">
              Error al cargar squads:{" "}
              {errorSquadsGestion instanceof Error
                ? errorSquadsGestion.message
                : "Error desconocido"}
            </div>
          )}

          {!isLoadingSquadsGestion && squadsGestion.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {squadsGestion.map((squad) => (
                <div
                  key={squad.id}
                  className="bg-card border rounded-lg p-4 hover:shadow-md transition-shadow"
                >
                  <div className="flex items-start justify-between mb-3">
                    <div>
                      <h3 className="text-lg font-semibold">{squad.nombre}</h3>
                      <span
                        className={`inline-block px-2 py-0.5 text-xs rounded-full mt-1 ${
                          squad.estado === "ACTIVO"
                            ? "bg-green-100 text-green-800"
                            : "bg-gray-100 text-gray-800"
                        }`}
                      >
                        {squad.estado}
                      </span>
                    </div>
                    <button
                      onClick={() => {
                        setEditingSquad(squad);
                        setIsSquadFormOpen(true);
                      }}
                      className="text-sm text-primary hover:underline"
                    >
                      Editar
                    </button>
                  </div>

                  {squad.descripcion && (
                    <p className="text-sm text-muted-foreground mb-3 line-clamp-2">
                      {squad.descripcion}
                    </p>
                  )}

                  <div className="flex gap-2 pt-3 border-t">
                    {squad.estado === "ACTIVO" ? (
                      <button
                        onClick={() => handleDesactivarSquad(squad.id)}
                        className="text-sm text-destructive hover:underline"
                      >
                        Desactivar
                      </button>
                    ) : (
                      <span className="text-sm text-muted-foreground">
                        Squad inactivo
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}

          {!isLoadingSquadsGestion && squadsGestion.length === 0 && (
            <div className="bg-muted/50 border border-dashed rounded-lg px-4 py-10 text-center text-muted-foreground">
              No hay squads{" "}
              {filtroEstado ? `en estado ${filtroEstado}` : "disponibles"}.
            </div>
          )}
        </div>
      )}

      {/* Sprint Selector */}
      <div className="bg-card border border-border rounded-lg p-6 space-y-4">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <h2 className="text-lg font-semibold text-foreground">
            Seleccionar Sprint
          </h2>
          <div className="flex items-center gap-2 text-sm">
            <span className="text-muted-foreground">Estado:</span>
            <select
              value={filtroSprintEstado}
              onChange={(e) => {
                const value = e.target.value as SprintEstado | "";
                setFiltroSprintEstado(value);
                setSelectedSprint(null);
              }}
              className="px-2.5 py-1.5 rounded-md border border-border bg-background text-foreground"
            >
              <option value="">Todos</option>
              <option value="PLANIFICACION">Planificados</option>
              <option value="ACTIVO">Activos</option>
              <option value="CERRADO">Inactivos</option>
            </select>
          </div>
        </div>
        <SprintSelector
          sprints={sprints}
          sprintSeleccionado={selectedSprint}
          onSprintChange={setSelectedSprint}
          onCrearSprint={() => setIsNuevoSprintOpen(true)}
          onActivarSprint={() => {
            if (selectedSprint) activarSprint.mutate(selectedSprint.id);
          }}
          onCerrarSprint={() => {
            if (selectedSprint) cerrarSprint.mutate(selectedSprint.id);
          }}
          onReplanificarSprint={() => {
            if (selectedSprint) replanificarSprint.mutate(selectedSprint.id);
          }}
          onEliminarSprint={() => {
            if (selectedSprint) {
              const confirm = window.confirm(
                "¿Eliminar el sprint seleccionado? Esta accion no se puede deshacer.",
              );
              if (confirm) eliminarSprint.mutate(selectedSprint.id);
            }
          }}
        />
      </div>

      {/* Vistas del Sprint */}
      {selectedSprint && (
        <div className="space-y-4">
          {/* Sprint info */}
          <div className="bg-card border border-border rounded-lg p-4 flex items-center justify-between">
            <div>
              <span className="text-sm text-muted-foreground">
                Sprint seleccionado:
              </span>
              <span className="ml-2 font-semibold text-foreground">
                {selectedSprint.nombre}
              </span>
              {selectedSprint.squadNombre && (
                <span className="ml-3 text-sm text-muted-foreground">
                  Squad: {selectedSprint.squadNombre}
                </span>
              )}
              {selectedSprint.fechaInicio && (
                <span className="ml-3 text-sm text-muted-foreground">
                  {selectedSprint.fechaInicio}
                  {selectedSprint.fechaFin
                    ? ` → ${selectedSprint.fechaFin}`
                    : ""}
                </span>
              )}
            </div>
            <div className="flex items-center gap-3 text-sm">
              <span className="text-muted-foreground">
                {tareas.length} tarea(s)
              </span>
              <button
                onClick={() => setIsModalTareaOpen(true)}
                className="flex items-center gap-1 px-3 py-1.5 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors text-sm"
              >
                <Plus className="h-4 w-4" />
                Nueva tarea
              </button>
            </div>
          </div>

          {/* Navegación a vistas */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <button
              onClick={goToTimeline}
              className="bg-card border border-border rounded-lg p-6 text-left hover:bg-accent transition-colors group"
            >
              <div className="flex items-center gap-3 mb-3">
                <CalendarRange className="h-8 w-8 text-blue-500 group-hover:text-blue-600" />
                <h3 className="text-lg font-semibold text-foreground">
                  Timeline
                </h3>
              </div>
              <p className="text-sm text-muted-foreground">
                Vista de calendario con distribución de tareas por persona y
                día.
              </p>
            </button>

            <button
              onClick={goToKanban}
              className="bg-card border border-border rounded-lg p-6 text-left hover:bg-accent transition-colors group"
            >
              <div className="flex items-center gap-3 mb-3">
                <Kanban className="h-8 w-8 text-purple-500 group-hover:text-purple-600" />
                <h3 className="text-lg font-semibold text-foreground">
                  Kanban
                </h3>
              </div>
              <p className="text-sm text-muted-foreground">
                Tablero de estados para gestionar el flujo de las tareas.
              </p>
            </button>

            <button
              onClick={goToDashboard}
              className="bg-card border border-border rounded-lg p-6 text-left hover:bg-accent transition-colors group"
            >
              <div className="flex items-center gap-3 mb-3">
                <LayoutDashboard className="h-8 w-8 text-green-500 group-hover:text-green-600" />
                <h3 className="text-lg font-semibold text-foreground">
                  Dashboard
                </h3>
              </div>
              <p className="text-sm text-muted-foreground">
                Métricas y alertas de capacidad del sprint en tiempo real.
              </p>
            </button>
          </div>
        </div>
      )}

      {/* Modal crear sprint */}
      {isNuevoSprintOpen && (
        <ModalNuevoSprint
          squads={squads}
          onClose={() => setIsNuevoSprintOpen(false)}
          onCreated={() =>
            queryClient.invalidateQueries({
              queryKey: ["sprints"],
            })
          }
        />
      )}

      {/* Modal crear tarea */}
      {isModalTareaOpen && selectedSprint && (
        <ModalTarea
          sprintId={selectedSprint.id}
          personas={personas}
          onSubmit={(req) => crearTarea.mutate(req)}
          onCancel={() => setIsModalTareaOpen(false)}
          isSubmitting={crearTarea.isPending}
        />
      )}

      {isSquadFormOpen && (
        <SquadFormModal
          squad={editingSquad}
          onSubmit={handleSubmitSquad}
          onCancel={() => {
            setIsSquadFormOpen(false);
            setEditingSquad(null);
          }}
          isSubmitting={crearSquad.isPending || actualizarSquad.isPending}
        />
      )}
    </div>
  );
}

// --- Form Modal Component ---

interface SquadFormModalProps {
  squad: SquadResponse | null;
  onSubmit: (data: SquadRequest) => void;
  onCancel: () => void;
  isSubmitting: boolean;
}

function SquadFormModal({
  squad,
  onSubmit,
  onCancel,
  isSubmitting,
}: SquadFormModalProps) {
  const [formData, setFormData] = useState<SquadRequest>({
    nombre: squad?.nombre || "",
    descripcion: squad?.descripcion || "",
    estado: squad?.estado || "ACTIVO",
    idSquadCorrJira: squad?.idSquadCorrJira || "",
    idSquadEvolJira: squad?.idSquadEvolJira || "",
  });

  const handleChange = (field: keyof SquadRequest, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmitForm = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-card border rounded-lg shadow-lg w-full max-w-md p-6">
        <h2 className="text-2xl font-bold mb-4">
          {squad ? "Editar Squad" : "Nuevo Squad"}
        </h2>

        <form onSubmit={handleSubmitForm} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-1">
              Nombre del Squad
            </label>
            <input
              type="text"
              value={formData.nombre}
              onChange={(e) => handleChange("nombre", e.target.value)}
              className="w-full px-3 py-2 border rounded-md"
              required
              placeholder="Squad Red, Squad Blue, etc."
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">
              Descripción
            </label>
            <input
              type="text"
              value={formData.descripcion || ""}
              onChange={(e) => handleChange("descripcion", e.target.value)}
              className="w-full px-3 py-2 border rounded-md"
              placeholder="Descripción del squad"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium mb-1">
                ID Squad Correctivos (Jira)
              </label>
              <input
                type="text"
                value={formData.idSquadCorrJira || ""}
                onChange={(e) =>
                  handleChange("idSquadCorrJira", e.target.value)
                }
                className="w-full px-3 py-2 border rounded-md"
                placeholder="22517"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">
                ID Squad Evolutivos (Jira)
              </label>
              <input
                type="text"
                value={formData.idSquadEvolJira || ""}
                onChange={(e) =>
                  handleChange("idSquadEvolJira", e.target.value)
                }
                className="w-full px-3 py-2 border rounded-md"
                placeholder="97993"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Estado</label>
            <select
              value={formData.estado}
              onChange={(e) => handleChange("estado", e.target.value)}
              className="w-full px-3 py-2 border rounded-md"
              required
            >
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </div>

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
