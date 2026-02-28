/**
 * Página de Calendario del Squad
 * Vista de eventos de calendario con filtro por squad
 */

import { AccessibleModal } from "@/components/ui/AccessibleModal";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { AusenciaForm, EventoBadge, VacacionForm } from "@/features/calendario";
import { toast } from "@/lib/toast";
import { useDocumentTitle } from "@/lib/useDocumentTitle";
import { ausenciaService } from "@/services/ausenciaService";
import { personaService } from "@/services/personaService";
import { squadService } from "@/services/squadService";
import { vacacionService } from "@/services/vacacionService";
import type {
  AusenciaRequest,
  AusenciaResponse,
  VacacionRequest,
  VacacionResponse,
} from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Plus,
} from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/calendario")({
  component: CalendarioPage,
});

function CalendarioPage() {
  const queryClient = useQueryClient();
  useDocumentTitle("Calendario");
  const [squadId, setSquadId] = useState<number | null>(null);
  const [personaFiltroId, setPersonaFiltroId] = useState<number | null>(null);
  const [mes, setMes] = useState(new Date().getMonth());
  const [anio, setAnio] = useState(new Date().getFullYear());
  const [isVacacionFormOpen, setIsVacacionFormOpen] = useState(false);
  const [isAusenciaFormOpen, setIsAusenciaFormOpen] = useState(false);
  const [tipoRegistroSeleccionado, setTipoRegistroSeleccionado] = useState<
    "vacacion" | "ausencia" | null
  >(null);
  const [deleteConfirm, setDeleteConfirm] = useState<{
    id: number;
    nombre: string;
    tipo: "vacacion" | "ausencia";
  } | null>(null);

  const fechaInicio = `${anio}-${String(mes + 1).padStart(2, "0")}-01`;
  const ultimoDia = new Date(anio, mes + 1, 0).getDate();
  const fechaFin = `${anio}-${String(mes + 1).padStart(2, "0")}-${ultimoDia}`;

  // Query squads
  const { data: squads } = useQuery({
    queryKey: ["squads"],
    queryFn: () => squadService.listar(0, 100),
  });

  // Query personas del squad seleccionado (para el filtro)
  const { data: personasSquad } = useQuery({
    queryKey: ["personas", "squad", squadId],
    queryFn: () => personaService.listar(0, 200, { squadId: squadId! }),
    enabled: !!squadId,
  });

  // Query personas (para formularios)
  const { data: personas } = useQuery({
    queryKey: ["personas"],
    queryFn: () => personaService.listar(0, 200),
  });

  // Query vacaciones
  const { data: vacaciones, isLoading: isLoadingVacaciones } = useQuery<
    VacacionResponse[]
  >({
    queryKey: ["vacaciones", squadId, mes, anio],
    queryFn: () => {
      if (!squadId) return Promise.resolve([]);
      return vacacionService.porSquad(squadId, fechaInicio, fechaFin);
    },
    enabled: !!squadId,
  });

  // Query ausencias
  const { data: ausencias, isLoading: isLoadingAusencias } = useQuery<
    AusenciaResponse[]
  >({
    queryKey: ["ausencias", squadId, mes, anio],
    queryFn: () => {
      if (!squadId) return Promise.resolve([]);
      return ausenciaService.porSquad(squadId, fechaInicio, fechaFin);
    },
    enabled: !!squadId,
  });

  // Mutations
  const crearVacacionMutation = useMutation({
    mutationFn: vacacionService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["vacaciones"] });
      setIsVacacionFormOpen(false);
      setTipoRegistroSeleccionado(null);
      toast.success("Vacación registrada correctamente");
    },
    onError: () => toast.error("Error al registrar la vacación"),
  });

  const crearAusenciaMutation = useMutation({
    mutationFn: ausenciaService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ausencias"] });
      setIsAusenciaFormOpen(false);
      setTipoRegistroSeleccionado(null);
      toast.success("Ausencia registrada correctamente");
    },
    onError: () => toast.error("Error al registrar la ausencia"),
  });

  const eliminarVacacionMutation = useMutation({
    mutationFn: vacacionService.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["vacaciones"] });
      toast.success("Vacación eliminada");
    },
    onError: () => toast.error("Error al eliminar la vacación"),
  });

  const eliminarAusenciaMutation = useMutation({
    mutationFn: ausenciaService.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ausencias"] });
      toast.success("Ausencia eliminada");
    },
    onError: () => toast.error("Error al eliminar la ausencia"),
  });

  const handleMesAnterior = () => {
    if (mes === 0) {
      setMes(11);
      setAnio(anio - 1);
    } else {
      setMes(mes - 1);
    }
  };

  const handleMesSiguiente = () => {
    if (mes === 11) {
      setMes(0);
      setAnio(anio + 1);
    } else {
      setMes(mes + 1);
    }
  };

  const handleRegistrar = () => {
    setTipoRegistroSeleccionado("vacacion"); // Muestra el selector de tipo
  };

  const handleSubmitVacacion = (data: VacacionRequest) => {
    crearVacacionMutation.mutate(data);
  };

  const handleSubmitAusencia = (data: AusenciaRequest) => {
    crearAusenciaMutation.mutate(data);
  };

  const handleDeleteVacacion = (id: number, personaNombre: string) => {
    setDeleteConfirm({ id, nombre: personaNombre, tipo: "vacacion" });
  };

  const handleDeleteAusencia = (id: number, personaNombre: string) => {
    setDeleteConfirm({ id, nombre: personaNombre, tipo: "ausencia" });
  };

  const formatFecha = (fecha: string) => {
    return new Intl.DateTimeFormat("es-ES", {
      day: "numeric",
      month: "short",
      year: "numeric",
    }).format(new Date(fecha));
  };

  const nombreMes = new Intl.DateTimeFormat("es-ES", { month: "long" }).format(
    new Date(anio, mes),
  );

  const isLoading = isLoadingVacaciones || isLoadingAusencias;

  // Vacaciones y ausencias ya están tipadas como arrays — aplicar filtro por persona
  const vacacionesList = (vacaciones ?? []).filter(
    (v) => !personaFiltroId || v.personaId === personaFiltroId,
  );
  const ausenciasList = (ausencias ?? []).filter(
    (a) => !personaFiltroId || a.personaId === personaFiltroId,
  );

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Calendario del Equipo</h1>
          <p className="text-muted-foreground">
            Visualiza ausencias y capacidad disponible
          </p>
        </div>
        <button
          onClick={handleRegistrar}
          disabled={!squadId}
          className="flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        >
          <Plus className="h-4 w-4" aria-hidden="true" />
          Registrar
        </button>
      </div>

      {/* Filter Bar */}
      <div
        className="flex flex-wrap items-center gap-4"
        role="group"
        aria-label="Filtros del calendario"
      >
        <div className="flex items-center gap-2">
          <label
            htmlFor="cal-filter-squad"
            className="text-sm font-medium text-muted-foreground"
          >
            Squad:
          </label>
          <select
            id="cal-filter-squad"
            value={squadId || ""}
            onChange={(e) => {
              setSquadId(
                e.target.value ? Number.parseInt(e.target.value) : null,
              );
              setPersonaFiltroId(null); // reset persona al cambiar squad
            }}
            className="rounded-md border border-border px-3 py-2 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="">Seleccionar squad...</option>
            {squads?.content.map((squad) => (
              <option key={squad.id} value={squad.id}>
                {squad.nombre}
              </option>
            ))}
          </select>
        </div>

        {/* Filtro por persona — solo visible cuando hay squad */}
        {squadId && (
          <div className="flex items-center gap-2">
            <label
              htmlFor="cal-filter-persona"
              className="text-sm font-medium text-muted-foreground"
            >
              Persona:
            </label>
            <select
              id="cal-filter-persona"
              value={personaFiltroId || ""}
              onChange={(e) =>
                setPersonaFiltroId(
                  e.target.value ? Number.parseInt(e.target.value) : null,
                )
              }
              className="rounded-md border border-border px-3 py-2 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="">Todos</option>
              {personasSquad?.content.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nombre}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="flex items-center gap-2">
          <button
            onClick={handleMesAnterior}
            aria-label="Mes anterior"
            className="rounded p-1 text-muted-foreground hover:bg-muted"
          >
            <ChevronLeft className="h-5 w-5" aria-hidden="true" />
          </button>
          <span
            className="min-w-[150px] text-center text-sm font-medium capitalize"
            aria-live="polite"
          >
            {nombreMes} {anio}
          </span>
          <button
            onClick={handleMesSiguiente}
            aria-label="Mes siguiente"
            className="rounded p-1 text-muted-foreground hover:bg-muted"
          >
            <ChevronRight className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
      </div>

      {/* Leyenda */}
      <div
        className="flex items-center gap-4 rounded-md border border-border bg-muted/50 p-3 text-sm"
        role="legend"
        aria-label="Leyenda de colores"
      >
        <span className="font-medium text-muted-foreground">Leyenda:</span>
        <EventoBadge tipo="vacacion">🔵 Vacaciones</EventoBadge>
        <EventoBadge tipo="ausencia">🟠 Ausencias</EventoBadge>
        <EventoBadge tipo="festivo">⚪ Festivos</EventoBadge>
        <EventoBadge tipo="libre">🟢 Libre disposición</EventoBadge>
      </div>

      {/* Sin squad seleccionado */}
      {!squadId && (
        <div className="flex flex-col items-center justify-center rounded-md border-2 border-dashed border-border py-16">
          <CalendarIcon
            className="mb-4 h-12 w-12 text-muted-foreground"
            aria-hidden="true"
          />
          <p className="text-muted-foreground">
            Selecciona un squad para ver el calendario
          </p>
        </div>
      )}

      {/* Loading */}
      {squadId && isLoading && (
        <div
          className="space-y-2"
          role="status"
          aria-label="Cargando calendario"
        >
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-16 animate-pulse rounded-md bg-muted" />
          ))}
          <span className="sr-only">Cargando datos del calendario...</span>
        </div>
      )}

      {/* Content */}
      {squadId && !isLoading && (
        <div className="space-y-4">
          {/* Vacaciones */}
          <div className="rounded-md border border-border bg-card">
            <div className="border-b border-border bg-muted/50 px-4 py-3">
              <h3 className="font-semibold">Vacaciones</h3>
            </div>
            <div className="divide-y divide-border">
              {vacacionesList.length > 0 ? (
                vacacionesList.map((vacacion: VacacionResponse) => (
                  <div
                    key={vacacion.id}
                    className="flex items-center justify-between px-4 py-3 hover:bg-muted/50"
                  >
                    <div className="flex items-center gap-3">
                      <EventoBadge tipo="vacacion">
                        {vacacion.tipo === "VACACIONES"
                          ? "Vacaciones"
                          : vacacion.tipo === "LIBRE_DISPOSICION"
                            ? "Libre disposición"
                            : vacacion.tipo}
                      </EventoBadge>
                      <div>
                        <div className="font-medium">
                          {vacacion.personaNombre}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {formatFecha(vacacion.fechaInicio)} -{" "}
                          {formatFecha(vacacion.fechaFin)} (
                          {vacacion.diasLaborables} días)
                        </div>
                        {vacacion.comentario && (
                          <div className="text-sm text-muted-foreground/70">
                            {vacacion.comentario}
                          </div>
                        )}
                      </div>
                    </div>
                    <button
                      onClick={() =>
                        handleDeleteVacacion(
                          vacacion.id,
                          vacacion.personaNombre,
                        )
                      }
                      aria-label={`Eliminar vacación de ${vacacion.personaNombre}`}
                      className="rounded p-1 text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
                    >
                      ×
                    </button>
                  </div>
                ))
              ) : (
                <div className="px-4 py-8 text-center text-sm text-muted-foreground">
                  No hay vacaciones registradas este mes
                </div>
              )}
            </div>
          </div>

          {/* Ausencias */}
          <div className="rounded-md border border-border bg-card">
            <div className="border-b border-border bg-muted/50 px-4 py-3">
              <h3 className="font-semibold">Ausencias</h3>
            </div>
            <div className="divide-y divide-border">
              {ausenciasList.length > 0 ? (
                ausenciasList.map((ausencia: AusenciaResponse) => (
                  <div
                    key={ausencia.id}
                    className="flex items-center justify-between px-4 py-3 hover:bg-muted/50"
                  >
                    <div className="flex items-center gap-3">
                      <EventoBadge tipo="ausencia">{ausencia.tipo}</EventoBadge>
                      <div>
                        <div className="font-medium">
                          {ausencia.personaNombre}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {formatFecha(ausencia.fechaInicio)}
                          {ausencia.fechaFin
                            ? ` - ${formatFecha(ausencia.fechaFin)}`
                            : " (sin fecha fin)"}
                        </div>
                        {ausencia.comentario && (
                          <div className="text-sm text-muted-foreground/70">
                            {ausencia.comentario}
                          </div>
                        )}
                      </div>
                    </div>
                    <button
                      onClick={() =>
                        handleDeleteAusencia(
                          ausencia.id,
                          ausencia.personaNombre,
                        )
                      }
                      aria-label={`Eliminar ausencia de ${ausencia.personaNombre}`}
                      className="rounded p-1 text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
                    >
                      ×
                    </button>
                  </div>
                ))
              ) : (
                <div className="px-4 py-8 text-center text-sm text-muted-foreground">
                  No hay ausencias registradas este mes
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Selector de tipo de registro */}
      <AccessibleModal
        isOpen={tipoRegistroSeleccionado === "vacacion" && !isVacacionFormOpen}
        onClose={() => setTipoRegistroSeleccionado(null)}
        title="¿Qué deseas registrar?"
        size="sm"
      >
        <div className="space-y-2">
          <button
            onClick={() => {
              setTipoRegistroSeleccionado(null);
              setIsVacacionFormOpen(true);
            }}
            className="w-full rounded-md border border-blue-300 bg-blue-50 px-4 py-3 text-left font-medium text-blue-700 hover:bg-blue-100 focus:outline-none focus:ring-2 focus:ring-primary"
          >
            📅 Vacación
          </button>
          <button
            onClick={() => {
              setTipoRegistroSeleccionado(null);
              setIsAusenciaFormOpen(true);
            }}
            className="w-full rounded-md border border-amber-300 bg-amber-50 px-4 py-3 text-left font-medium text-amber-700 hover:bg-amber-100 focus:outline-none focus:ring-2 focus:ring-primary"
          >
            🚑 Ausencia
          </button>
        </div>
      </AccessibleModal>

      {/* ConfirmDialog for deleting vacaciones/ausencias */}
      <ConfirmDialog
        isOpen={!!deleteConfirm}
        onCancel={() => setDeleteConfirm(null)}
        onConfirm={() => {
          if (deleteConfirm) {
            if (deleteConfirm.tipo === "vacacion") {
              eliminarVacacionMutation.mutate(deleteConfirm.id);
            } else {
              eliminarAusenciaMutation.mutate(deleteConfirm.id);
            }
            setDeleteConfirm(null);
          }
        }}
        title={`Eliminar ${deleteConfirm?.tipo === "vacacion" ? "vacación" : "ausencia"}`}
        description={`¿Estás seguro de eliminar ${deleteConfirm?.tipo === "vacacion" ? "la vacación" : "la ausencia"} de ${deleteConfirm?.nombre ?? ""}?`}
        confirmText="Eliminar"
        variant="danger"
      />

      {/* Form Dialogs */}
      {isVacacionFormOpen && personas && (
        <VacacionForm
          personas={personas.content}
          onSubmit={handleSubmitVacacion}
          onCancel={() => {
            setIsVacacionFormOpen(false);
            setTipoRegistroSeleccionado(null);
          }}
          isSubmitting={crearVacacionMutation.isPending}
          squadId={squadId || undefined}
        />
      )}

      {isAusenciaFormOpen && personas && (
        <AusenciaForm
          personas={personas.content}
          onSubmit={handleSubmitAusencia}
          onCancel={() => {
            setIsAusenciaFormOpen(false);
            setTipoRegistroSeleccionado(null);
          }}
          isSubmitting={crearAusenciaMutation.isPending}
          squadId={squadId || undefined}
        />
      )}
    </div>
  );
}
