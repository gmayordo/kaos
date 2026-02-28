import { AccessibleModal } from "@/components/ui/AccessibleModal";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Tooltip } from "@/components/ui/Tooltip";
import { toast } from "@/lib/toast";
import { useDocumentTitle } from "@/lib/useDocumentTitle";
import { squadService } from "@/services/squadService";
import type { SquadRequest, SquadResponse } from "@/types/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Calendar, Plus, Users } from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/squads/")({
  component: SquadsPage,
});

function SquadsPage() {
  const queryClient = useQueryClient();
  const [filtroEstado, setFiltroEstado] = useState<
    "ACTIVO" | "INACTIVO" | undefined
  >(undefined);
  const [editingSquad, setEditingSquad] = useState<SquadResponse | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [desactivarConfirm, setDesactivarConfirm] = useState<{
    id: number;
    nombre: string;
  } | null>(null);

  useDocumentTitle("Squads");

  // Query para listar squads
  const {
    data: squads,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["squads", filtroEstado],
    queryFn: () => squadService.listar(0, 100, filtroEstado),
  });

  // Mutation para crear
  const crearMutation = useMutation({
    mutationFn: squadService.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["squads"] });
      setIsFormOpen(false);
      toast.success("Squad creado correctamente");
    },
    onError: () => toast.error("Error al crear squad"),
  });

  // Mutation para actualizar
  const actualizarMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: SquadRequest }) =>
      squadService.actualizar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["squads"] });
      setEditingSquad(null);
      setIsFormOpen(false);
      toast.success("Squad actualizado");
    },
    onError: () => toast.error("Error al actualizar squad"),
  });

  // Mutation para desactivar
  const desactivarMutation = useMutation({
    mutationFn: squadService.desactivar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["squads"] });
      setDesactivarConfirm(null);
      toast.success("Squad desactivado");
    },
    onError: () => {
      setDesactivarConfirm(null);
      toast.error("Error al desactivar squad");
    },
  });

  const handleSubmit = (data: SquadRequest) => {
    if (editingSquad) {
      actualizarMutation.mutate({ id: editingSquad.id, data });
    } else {
      crearMutation.mutate(data);
    }
  };

  const handleEdit = (squad: SquadResponse) => {
    setEditingSquad(squad);
    setIsFormOpen(true);
  };

  const handleDesactivar = (squad: SquadResponse) => {
    setDesactivarConfirm({ id: squad.id, nombre: squad.nombre });
  };

  const handleNewSquad = () => {
    setEditingSquad(null);
    setIsFormOpen(true);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-full" role="status">
        <div className="text-lg text-muted-foreground">
          <span className="animate-pulse">Cargando squads...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-4">
        <div className="text-lg text-destructive">
          Error al cargar squads:{" "}
          {error instanceof Error ? error.message : "Error desconocido"}
        </div>
        <button
          onClick={() =>
            queryClient.invalidateQueries({ queryKey: ["squads"] })
          }
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
        >
          Reintentar
        </button>
      </div>
    );
  }

  const squadList = squads?.content ?? [];

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Squads</h1>
          <p className="text-muted-foreground mt-1">
            Gestión de equipos de desarrollo
          </p>
        </div>
        <button
          onClick={handleNewSquad}
          className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
        >
          <Plus className="h-4 w-4" aria-hidden="true" />
          Nuevo Squad
        </button>
      </div>

      {/* Filtros */}
      <div className="flex gap-2" role="group" aria-label="Filtrar por estado">
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

      {/* Grid de Squads */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {squadList.map((squad) => (
          <SquadCard
            key={squad.id}
            squad={squad}
            onEdit={() => handleEdit(squad)}
            onDesactivar={() => handleDesactivar(squad)}
          />
        ))}
      </div>

      {squadList.length === 0 && (
        <div className="bg-muted/50 border border-dashed rounded-lg px-4 py-12 text-center text-muted-foreground">
          <p>
            No hay squads{" "}
            {filtroEstado ? `en estado ${filtroEstado}` : "disponibles"}.
          </p>
          <button
            onClick={handleNewSquad}
            className="mt-3 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors text-sm"
          >
            Crear primer squad
          </button>
        </div>
      )}

      {/* ConfirmDialog para desactivar */}
      <ConfirmDialog
        isOpen={desactivarConfirm !== null}
        onCancel={() => setDesactivarConfirm(null)}
        onConfirm={() =>
          desactivarConfirm && desactivarMutation.mutate(desactivarConfirm.id)
        }
        title="Desactivar squad"
        description={`¿Estás seguro de desactivar el squad "${desactivarConfirm?.nombre ?? ""}"? Se podrá reactivar después.`}
        confirmText="Desactivar"
        variant="warning"
        isLoading={desactivarMutation.isPending}
      />

      {/* Formulario Modal */}
      {isFormOpen && (
        <SquadFormModal
          squad={editingSquad}
          onSubmit={handleSubmit}
          onCancel={() => {
            setIsFormOpen(false);
            setEditingSquad(null);
          }}
          isSubmitting={crearMutation.isPending || actualizarMutation.isPending}
        />
      )}
    </div>
  );
}

// --- Squad Card Component ---

interface SquadCardProps {
  squad: SquadResponse;
  onEdit: () => void;
  onDesactivar: () => void;
}

function SquadCard({ squad, onEdit, onDesactivar }: SquadCardProps) {
  return (
    <div className="bg-card border rounded-lg p-4 hover:shadow-md transition-shadow">
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div>
          <h3 className="text-lg font-semibold">{squad.nombre}</h3>
          <span
            className={`inline-block px-2 py-0.5 text-xs rounded-full mt-1 ${
              squad.estado === "ACTIVO"
                ? "bg-green-100 text-green-800"
                : "bg-gray-100 text-gray-800"
            }`}
            title={
              squad.estado === "ACTIVO"
                ? "El squad está activo"
                : "El squad fue desactivado"
            }
          >
            {squad.estado}
          </span>
        </div>
        <button
          onClick={(e) => {
            e.stopPropagation();
            onEdit();
          }}
          className="text-sm text-primary hover:underline"
          aria-label={`Editar squad ${squad.nombre}`}
        >
          Editar
        </button>
      </div>

      {/* Description */}
      {squad.descripcion && (
        <p className="text-sm text-muted-foreground mb-3 line-clamp-2">
          {squad.descripcion}
        </p>
      )}

      {/* Jira IDs */}
      <div className="space-y-2 mb-3">
        {squad.idSquadCorrJira && (
          <Tooltip content="ID del board de correctivos en Jira">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Calendar className="w-4 h-4" aria-hidden="true" />
              <span>Correctivos: {squad.idSquadCorrJira}</span>
            </div>
          </Tooltip>
        )}
        {squad.idSquadEvolJira && (
          <Tooltip content="ID del board de evolutivos en Jira">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Users className="w-4 h-4" aria-hidden="true" />
              <span>Evolutivos: {squad.idSquadEvolJira}</span>
            </div>
          </Tooltip>
        )}
      </div>

      {/* Acciones */}
      <div className="flex gap-2 pt-3 border-t">
        <Link
          to="/squads/$squadId"
          params={{ squadId: squad.id.toString() }}
          className="flex-1 text-center px-3 py-1.5 text-sm bg-muted hover:bg-muted/80 rounded-md transition-colors"
        >
          Ver Detalle
        </Link>
        {squad.estado === "ACTIVO" && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              onDesactivar();
            }}
            className="px-3 py-1.5 text-sm text-destructive hover:underline"
          >
            Desactivar
          </button>
        )}
      </div>
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
    <AccessibleModal
      isOpen={true}
      onClose={onCancel}
      title={squad ? "Editar Squad" : "Nuevo Squad"}
      size="md"
    >
      <form onSubmit={handleSubmitForm} className="space-y-4">
        {/* Nombre */}
        <div>
          <label
            htmlFor="squad-nombre"
            className="block text-sm font-medium mb-1"
          >
            Nombre del Squad
          </label>
          <input
            id="squad-nombre"
            type="text"
            value={formData.nombre}
            onChange={(e) => handleChange("nombre", e.target.value)}
            className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
            required
            placeholder="Squad Red, Squad Blue, etc."
            autoFocus
          />
        </div>

        {/* Descripción */}
        <div>
          <label
            htmlFor="squad-descripcion"
            className="block text-sm font-medium mb-1"
          >
            Descripción
          </label>
          <input
            id="squad-descripcion"
            type="text"
            value={formData.descripcion || ""}
            onChange={(e) => handleChange("descripcion", e.target.value)}
            className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
            placeholder="Descripción del squad"
          />
        </div>

        {/* IDs Jira */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label
              htmlFor="squad-corr-jira"
              className="block text-sm font-medium mb-1"
            >
              ID Squad Correctivos (Jira)
            </label>
            <input
              id="squad-corr-jira"
              type="text"
              value={formData.idSquadCorrJira || ""}
              onChange={(e) => handleChange("idSquadCorrJira", e.target.value)}
              className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
              placeholder="22517"
            />
          </div>
          <div>
            <label
              htmlFor="squad-evol-jira"
              className="block text-sm font-medium mb-1"
            >
              ID Squad Evolutivos (Jira)
            </label>
            <input
              id="squad-evol-jira"
              type="text"
              value={formData.idSquadEvolJira || ""}
              onChange={(e) => handleChange("idSquadEvolJira", e.target.value)}
              className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
              placeholder="97993"
            />
          </div>
        </div>

        {/* Estado */}
        <div>
          <label
            htmlFor="squad-estado"
            className="block text-sm font-medium mb-1"
          >
            Estado
          </label>
          <select
            id="squad-estado"
            value={formData.estado}
            onChange={(e) => handleChange("estado", e.target.value)}
            className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary"
            required
          >
            <option value="ACTIVO">Activo</option>
            <option value="INACTIVO">Inactivo</option>
          </select>
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
    </AccessibleModal>
  );
}
