import { DedicacionForm } from "@/features/dedicacion";
import { personaService } from "@/services/personaService";
import { squadMemberService } from "@/services/squadMemberService";
import { squadService } from "@/services/squadService";
import type { SquadMemberRequest } from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowLeft, Plus, Users } from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/squads/$squadId")({
  component: SquadDetailPage,
});

function SquadDetailPage() {
  const queryClient = useQueryClient();
  const { squadId } = Route.useParams();
  const id = Number(squadId);
  const [isFormOpen, setIsFormOpen] = useState(false);

  const {
    data: squad,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["squad", id],
    queryFn: () => squadService.obtener(id),
  });

  // Query miembros del squad
  const { data: miembros } = useQuery({
    queryKey: ["squad-members", id],
    queryFn: () => squadMemberService.listarPorSquad(id),
  });

  // Query personas (para el form)
  const { data: personas } = useQuery({
    queryKey: ["personas"],
    queryFn: () => personaService.listar(0, 200),
  });

  // Mutation crear
  const crearMutation = useMutation({
    mutationFn: squadMemberService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["squad-members", id] });
      queryClient.invalidateQueries({ queryKey: ["squad", id] });
      setIsFormOpen(false);
    },
  });

  // Mutation eliminar
  const eliminarMutation = useMutation({
    mutationFn: squadMemberService.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["squad-members", id] });
      queryClient.invalidateQueries({ queryKey: ["squad", id] });
    },
  });

  const handleSubmit = (data: SquadMemberRequest) => {
    crearMutation.mutate(data);
  };

  const handleRemove = (memberId: number) => {
    if (confirm("¿Estás seguro de eliminar esta asignación?")) {
      eliminarMutation.mutate(memberId);
    }
  };

  const capacidadTotal =
    miembros?.reduce(
      (sum, m) =>
        sum +
        (m.capacidadDiariaLunes || 0) +
        (m.capacidadDiariaMartes || 0) +
        (m.capacidadDiariaMiercoles || 0) +
        (m.capacidadDiariaJueves || 0) +
        (m.capacidadDiariaViernes || 0),
      0,
    ) || 0;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-muted-foreground">Cargando squad...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-destructive">
          Error al cargar squad:{" "}
          {error instanceof Error ? error.message : "Error desconocido"}
        </div>
      </div>
    );
  }

  if (!squad) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-muted-foreground">Squad no encontrado</div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div>
        <Link
          to="/squads"
          className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          Volver a Squads
        </Link>

        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">{squad.nombre}</h1>
            <div className="flex items-center gap-3 mt-2">
              <span
                className={`inline-block px-2 py-0.5 text-xs rounded-full ${
                  squad.estado === "ACTIVO"
                    ? "bg-green-100 text-green-800"
                    : "bg-gray-100 text-gray-800"
                }`}
              >
                {squad.estado}
              </span>
              <span className="text-sm text-muted-foreground">
                {squad.descripcion || "Sin descripción"}
              </span>
            </div>
          </div>
          <button
            onClick={() => setIsFormOpen(true)}
            className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            Añadir Miembro
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-card border rounded-lg p-4">
          <div className="flex items-center gap-2 text-muted-foreground mb-1">
            <Users className="w-4 h-4" />
            <span className="text-sm">Total Miembros</span>
          </div>
          <div className="text-2xl font-bold">{miembros?.length || 0}</div>
        </div>

        <div className="bg-card border rounded-lg p-4">
          <div className="text-sm text-muted-foreground mb-1">
            Capacidad Total
          </div>
          <div className="text-2xl font-bold">
            {capacidadTotal.toFixed(1)} h/sem
          </div>
        </div>

        <div className="bg-card border rounded-lg p-4">
          <div className="text-sm text-muted-foreground mb-1">
            Dedicación Promedio
          </div>
          <div className="text-2xl font-bold">
            {miembros?.length
              ? (
                  miembros.reduce((sum, m) => sum + m.porcentaje, 0) /
                  miembros.length
                ).toFixed(0)
              : "0"}
            %
          </div>
        </div>
      </div>

      {/* Miembros del Squad */}
      <div className="bg-card border rounded-lg">
        <div className="px-4 py-3 border-b">
          <h2 className="text-lg font-semibold">Miembros del Squad</h2>
        </div>

        {miembros && miembros.length > 0 ? (
          <div className="divide-y">
            {miembros.map((miembro) => (
              <div
                key={miembro.id}
                className="p-4 hover:bg-muted/50 transition-colors"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="font-semibold">{miembro.personaNombre}</h3>
                      <span className="text-sm text-muted-foreground">
                        {miembro.rol.replace("_", " ")}
                      </span>
                    </div>

                    <div className="flex items-center gap-3 mt-1">
                      <span className="text-sm font-medium">
                        Dedicación: {miembro.porcentaje}%
                      </span>
                    </div>

                    <div className="mt-2 text-xs text-muted-foreground space-y-1">
                      <div>
                        Capacidad diaria: L{" "}
                        {miembro.capacidadDiariaLunes?.toFixed(1)}h · Ma{" "}
                        {miembro.capacidadDiariaMartes?.toFixed(1)}h · Mi{" "}
                        {miembro.capacidadDiariaMiercoles?.toFixed(1)}h · J{" "}
                        {miembro.capacidadDiariaJueves?.toFixed(1)}h · V{" "}
                        {miembro.capacidadDiariaViernes?.toFixed(1)}h
                      </div>
                      <div>
                        Capacidad semanal:{" "}
                        {(
                          (miembro.capacidadDiariaLunes || 0) +
                          (miembro.capacidadDiariaMartes || 0) +
                          (miembro.capacidadDiariaMiercoles || 0) +
                          (miembro.capacidadDiariaJueves || 0) +
                          (miembro.capacidadDiariaViernes || 0)
                        ).toFixed(1)}
                        h
                      </div>
                    </div>
                  </div>

                  <button
                    onClick={() => handleRemove(miembro.id)}
                    disabled={eliminarMutation.isPending}
                    className="text-sm text-destructive hover:underline"
                  >
                    Eliminar
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-6 text-center text-muted-foreground">
            <Users className="w-12 h-12 mx-auto mb-2 opacity-50" />
            <p>No hay miembros asignados a este squad</p>
            <p className="text-sm mt-1">
              Usa el botón "Añadir Miembro" para comenzar
            </p>
          </div>
        )}
      </div>

      {/* Formulario Modal */}
      {isFormOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-card border rounded-lg shadow-lg w-full max-w-2xl p-6">
            <h2 className="text-2xl font-bold mb-4">
              Añadir Miembro a {squad.nombre}
            </h2>
            <DedicacionForm
              squads={[squad]}
              personas={personas?.content || []}
              onSubmit={handleSubmit}
              onCancel={() => setIsFormOpen(false)}
              isSubmitting={crearMutation.isPending}
            />
          </div>
        </div>
      )}
    </div>
  );
}
