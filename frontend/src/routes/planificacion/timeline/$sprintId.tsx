/**
 * Página Timeline del Sprint
 * Vista de calendario mostrando personas × días con sus tareas asignadas.
 * Permite crear y editar tareas haciendo click en celdas/tareas.
 */

import { ModalTarea, TimelineGrid } from "@/features/planificacion";
import { personaService } from "@/services/personaService";
import { planificacionService } from "@/services/planificacionService";
import { sprintService } from "@/services/sprintService";
import { tareaService } from "@/services/tareaService";
import type { TareaRequest, TareaResponse } from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowLeft, Download, Kanban, LayoutDashboard, RefreshCw } from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/planificacion/timeline/$sprintId")({
  component: TimelinePage,
});

function TimelinePage() {
  const queryClient = useQueryClient();
  const { sprintId } = Route.useParams();
  const id = Number(sprintId);

  // ── Modal state ───────────────────────────────────────────────────────────
  const [tareaSeleccionada, setTareaSeleccionada] =
    useState<TareaResponse | null>(null);
  const [preseleccionPersonaId, setPreseleccionPersonaId] = useState<
    number | undefined
  >(undefined);
  const [preseleccionDia, setPreseleccionDia] = useState<number | undefined>(
    undefined,
  );
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [personasOcultas] = useState<Set<number>>(new Set());
  const [isExporting, setIsExporting] = useState(false);

  // ── Queries ───────────────────────────────────────────────────────────────
  const { data: sprint, isLoading: sprintLoading } = useQuery({
    queryKey: ["sprint", id],
    queryFn: () => sprintService.obtener(id),
  });

  const {
    data: timeline,
    isLoading: timelineLoading,
    refetch: refetchTimeline,
  } = useQuery({
    queryKey: ["planificacion", "timeline", id],
    queryFn: () => planificacionService.obtenerTimeline(id),
  });

  const { data: tareasData } = useQuery({
    queryKey: ["tareas", id],
    queryFn: () => tareaService.listar(0, 200, { sprintId: id }),
  });
  const tareas = tareasData?.content ?? [];
  const tareasMap = new Map(tareas.map((t) => [t.id, t]));

  const { data: personasData } = useQuery({
    queryKey: ["personas"],
    queryFn: () => personaService.listar(0, 200),
  });
  const personas = personasData?.content ?? [];

  // ── Mutations ─────────────────────────────────────────────────────────────
  const moverTarea = useMutation({
    mutationFn: ({
      tareaId,
      nuevaPersonaId,
      nuevoDia,
    }: {
      tareaId: number;
      nuevaPersonaId: number;
      nuevoDia: number;
    }) => {
      const original = tareasMap.get(tareaId);
      if (!original) throw new Error("Tarea no encontrada");
      const req: TareaRequest = {
        titulo: original.titulo,
        sprintId: original.sprintId,
        tipo: original.tipo,
        categoria: original.categoria,
        estimacion: original.estimacion,
        prioridad: original.prioridad,
        estado: original.estado,
        personaId: nuevaPersonaId,
        diaAsignado: nuevoDia,
        referenciaJira: original.referenciaJira,
      };
      return tareaService.actualizar(tareaId, req);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tareas", id] });
      queryClient.invalidateQueries({
        queryKey: ["planificacion", "timeline", id],
      });
    },
  });

  const crearTarea = useMutation({
    mutationFn: tareaService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tareas", id] });
      queryClient.invalidateQueries({
        queryKey: ["planificacion", "timeline", id],
      });
      cerrarModal();
    },
  });

  const editarTarea = useMutation({
    mutationFn: ({ tareaId, req }: { tareaId: number; req: TareaRequest }) =>
      tareaService.actualizar(tareaId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tareas", id] });
      queryClient.invalidateQueries({
        queryKey: ["planificacion", "timeline", id],
      });
      cerrarModal();
    },
  });

  const eliminarTarea = useMutation({
    mutationFn: (tareaId: number) => tareaService.eliminar(tareaId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tareas", id] });
      queryClient.invalidateQueries({
        queryKey: ["planificacion", "timeline", id],
      });
      cerrarModal();
    },
  });

  // ── Handlers ──────────────────────────────────────────────────────────────
  const abrirCrearEnCelda = (personaId: number, dia: number) => {
    setTareaSeleccionada(null);
    setPreseleccionPersonaId(personaId);
    setPreseleccionDia(dia);
    setIsModalOpen(true);
  };

  const abrirEditarTarea = (tareaId: number) => {
    const tarea = tareasMap.get(tareaId);
    if (tarea) {
      setTareaSeleccionada(tarea);
      setPreseleccionPersonaId(undefined);
      setPreseleccionDia(undefined);
      setIsModalOpen(true);
    }
  };

  const cerrarModal = () => {
    setIsModalOpen(false);
    setTareaSeleccionada(null);
    setPreseleccionPersonaId(undefined);
    setPreseleccionDia(undefined);
  };

  const handleSubmitTarea = (req: TareaRequest) => {
    const reqConSprint: TareaRequest = { ...req, sprintId: id };
    if (tareaSeleccionada) {
      editarTarea.mutate({ tareaId: tareaSeleccionada.id, req: reqConSprint });
    } else {
      crearTarea.mutate(reqConSprint);
    }
  };

  const handleExportarTimeline = async () => {
    try {
      setIsExporting(true);
      const blob = await planificacionService.exportarTimelineExcel(id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `timeline-${sprint?.nombre || "sprint"}-${new Date().toISOString().split("T")[0]}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Error exportando timeline:", error);
    } finally {
      setIsExporting(false);
    }
  };

  const isLoading = sprintLoading || timelineLoading;

  return (
    <div className="space-y-4">
      {/* Header + navegación */}
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
          <div>
            <h1 className="text-xl font-bold text-foreground">
              Timeline{sprint ? ` — ${sprint.nombre}` : ""}
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => refetchTimeline()}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
          >
            <RefreshCw className="h-4 w-4" />
            Actualizar
          </button>
          <button
            onClick={handleExportarTimeline}
            disabled={isExporting}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Download className="h-4 w-4" />
            {isExporting ? "Descargando..." : "Descargar"}
          </button>
          <Link
            to="/planificacion/kanban/$sprintId"
            params={{ sprintId }}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md border border-border text-muted-foreground hover:bg-accent transition-colors"
          >
            <Kanban className="h-4 w-4" />
            Kanban
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

      {/* Timeline Grid */}
      {timeline ? (
        <TimelineGrid
          timeline={timeline}
          personasOcultas={personasOcultas}
          onMoverTarea={(tareaId, nuevaPersonaId, nuevoDia) =>
            moverTarea.mutate({ tareaId, nuevaPersonaId, nuevoDia })
          }
          onClickTarea={abrirEditarTarea}
          onCrearEnCelda={abrirCrearEnCelda}
          isLoading={isLoading}
        />
      ) : isLoading ? (
        <div className="h-64 flex items-center justify-center text-muted-foreground">
          Cargando timeline...
        </div>
      ) : (
        <div className="h-64 flex items-center justify-center text-muted-foreground">
          No hay datos de timeline disponibles.
        </div>
      )}

      {/* Modal tarea */}
      {isModalOpen && (
        <ModalTarea
          tarea={tareaSeleccionada}
          sprintId={id}
          personas={personas}
          diaPreseleccionado={preseleccionDia}
          personaPreseleccionadaId={preseleccionPersonaId}
          onSubmit={handleSubmitTarea}
          onDelete={(tareaId) => eliminarTarea.mutate(tareaId)}
          onCancel={cerrarModal}
          isSubmitting={
            crearTarea.isPending ||
            editarTarea.isPending ||
            eliminarTarea.isPending
          }
        />
      )}
    </div>
  );
}
