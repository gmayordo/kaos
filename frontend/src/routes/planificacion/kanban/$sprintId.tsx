/**
 * Página Kanban del Sprint
 * Tablero de 4 columnas (PENDIENTE | EN_PROGRESO | BLOQUEADO | COMPLETADA)
 * con drag-drop y filtro por persona.
 */

import { KanbanBoard, ModalTarea } from "@/features/planificacion";
import { personaService } from "@/services/personaService";
import { sprintService } from "@/services/sprintService";
import { tareaService } from "@/services/tareaService";
import type { EstadoTarea, TareaRequest, TareaResponse } from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  ArrowLeft,
  CalendarRange,
  LayoutDashboard,
  RefreshCw,
  Users,
} from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/planificacion/kanban/$sprintId")({
  component: KanbanPage,
});

function KanbanPage() {
  const queryClient = useQueryClient();
  const { sprintId } = Route.useParams();
  const id = Number(sprintId);

  // ── Local state ───────────────────────────────────────────────────────────
  const [personaFiltroId, setPersonaFiltroId] = useState<number | null>(null);
  const [tareaSeleccionada, setTareaSeleccionada] =
    useState<TareaResponse | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // ── Queries ───────────────────────────────────────────────────────────────
  const { data: sprint } = useQuery({
    queryKey: ["sprint", id],
    queryFn: () => sprintService.obtener(id),
  });

  const {
    data: tareasData,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ["tareas", id],
    queryFn: () => tareaService.listar(0, 200, { sprintId: id }),
  });
  const tareas = tareasData?.content ?? [];

  const { data: personasData } = useQuery({
    queryKey: ["personas"],
    queryFn: () => personaService.listar(0, 200),
  });
  const personas = personasData?.content ?? [];

  // ── Mutations ─────────────────────────────────────────────────────────────
  const cambiarEstado = useMutation({
    mutationFn: ({
      tareaId,
      estado,
    }: {
      tareaId: number;
      estado: EstadoTarea;
    }) => tareaService.cambiarEstado(tareaId, estado),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tareas", id] });
    },
  });

  const editarTarea = useMutation({
    mutationFn: ({ tareaId, req }: { tareaId: number; req: TareaRequest }) =>
      tareaService.actualizar(tareaId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tareas", id] });
      cerrarModal();
    },
  });

  const eliminarTarea = useMutation({
    mutationFn: (tareaId: number) => tareaService.eliminar(tareaId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tareas", id] });
      cerrarModal();
    },
  });

  // ── Handlers ──────────────────────────────────────────────────────────────
  const cerrarModal = () => {
    setIsModalOpen(false);
    setTareaSeleccionada(null);
  };

  const handleClickTarea = (tarea: TareaResponse) => {
    setTareaSeleccionada(tarea);
    setIsModalOpen(true);
  };

  const handleCambiarEstado = (tareaId: number, nuevoEstado: EstadoTarea) => {
    cambiarEstado.mutate({ tareaId, estado: nuevoEstado });
  };

  const handleSubmitTarea = (req: TareaRequest) => {
    if (tareaSeleccionada) {
      const reqConSprint: TareaRequest = { ...req, sprintId: id };
      editarTarea.mutate({ tareaId: tareaSeleccionada.id, req: reqConSprint });
    }
  };

  // personas que aparecen en las tareas actuales
  const personasEnTareas = personas.filter((p) =>
    tareas.some((t) => t.personaId === p.id),
  );

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <Link
            to="/planificacion"
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            Planificación
          </Link>
          <span className="text-muted-foreground">/</span>
          <h1 className="text-xl font-bold text-foreground">
            Kanban{sprint ? ` — ${sprint.nombre}` : ""}
          </h1>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => refetch()}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
          >
            <RefreshCw className="h-4 w-4" />
            Actualizar
          </button>
          <Link
            to="/planificacion/timeline/$sprintId"
            params={{ sprintId }}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
          >
            <CalendarRange className="h-4 w-4" />
            Timeline
          </Link>
          <Link
            to="/planificacion/dashboard/$sprintId"
            params={{ sprintId }}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
          >
            <LayoutDashboard className="h-4 w-4" />
            Dashboard
          </Link>
        </div>
      </div>

      {/* Filtro por persona */}
      {personasEnTareas.length > 0 && (
        <div className="flex items-center gap-2 flex-wrap">
          <Users className="h-4 w-4 text-muted-foreground" />
          <button
            onClick={() => setPersonaFiltroId(null)}
            className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
              personaFiltroId === null
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-accent"
            }`}
          >
            Todos
          </button>
          {personasEnTareas.map((p) => (
            <button
              key={p.id}
              onClick={() =>
                setPersonaFiltroId(personaFiltroId === p.id ? null : p.id)
              }
              className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                personaFiltroId === p.id
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted text-muted-foreground hover:bg-accent"
              }`}
            >
              {p.nombre}
            </button>
          ))}
        </div>
      )}

      {/* Kanban Board */}
      <KanbanBoard
        tareas={tareas}
        personaFiltroId={personaFiltroId}
        onCambiarEstado={handleCambiarEstado}
        onClickTarea={handleClickTarea}
        isLoading={isLoading}
      />

      {/* Modal editar tarea */}
      {isModalOpen && tareaSeleccionada && (
        <ModalTarea
          tarea={tareaSeleccionada}
          sprintId={id}
          personas={personas}
          onSubmit={handleSubmitTarea}
          onDelete={(tareaId) => eliminarTarea.mutate(tareaId)}
          onCancel={cerrarModal}
          isSubmitting={editarTarea.isPending || eliminarTarea.isPending}
        />
      )}
    </div>
  );
}
