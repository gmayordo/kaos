/**
 * SprintSelector — Selector de Sprint con acciones
 * Dropdown de sprints del squad con botones: Crear, Activar, Inactivar
 * Componente presentacional: recibe datos por props, emite eventos.
 */

import type { SprintResponse } from "@/types/api";
import {
  CheckCircle,
  ChevronDown,
  Play,
  Plus,
  RotateCcw,
  Trash2,
} from "lucide-react";
import type { FC } from "react";
import { useState } from "react";

interface Props {
  /** Lista de sprints disponibles */
  sprints: SprintResponse[];
  /** Sprint actualmente seleccionado */
  sprintSeleccionado: SprintResponse | null;
  /** Callback al cambiar sprint */
  onSprintChange: (sprint: SprintResponse) => void;
  /** Callback para crear nuevo sprint */
  onCrearSprint: () => void;
  /** Callback para activar el sprint seleccionado */
  onActivarSprint: () => void;
  /** Callback para inactivar el sprint seleccionado */
  onCerrarSprint: () => void;
  /** Callback para volver a planificación el sprint seleccionado */
  onReplanificarSprint: () => void;
  /** Callback para eliminar el sprint seleccionado */
  onEliminarSprint: () => void;
  /** Estado de carga */
  isLoading?: boolean;
}

const estadoBadgeClasses: Record<string, string> = {
  PLANIFICACION: "bg-gray-100 text-gray-700",
  ACTIVO: "bg-blue-100 text-blue-700",
  CERRADO: "bg-gray-200 text-gray-500",
};

const estadoLabel: Record<string, string> = {
  PLANIFICACION: "Planificación",
  ACTIVO: "Activo",
  CERRADO: "Inactivo",
};

const estadoDotClasses: Record<string, string> = {
  PLANIFICACION: "bg-gray-400",
  ACTIVO: "bg-blue-500",
  CERRADO: "bg-gray-500",
};

/**
 * Selector de sprint con acciones contextuales.
 * Los botones Activar/Inactivar se muestran según el estado del sprint.
 */
export const SprintSelector: FC<Props> = ({
  sprints,
  sprintSeleccionado,
  onSprintChange,
  onCrearSprint,
  onActivarSprint,
  onCerrarSprint,
  onReplanificarSprint,
  onEliminarSprint,
  isLoading = false,
}) => {
  const [open, setOpen] = useState(false);

  const handleSelect = (sprint: SprintResponse) => {
    onSprintChange(sprint);
    setOpen(false);
  };

  const puedeActivar =
    sprintSeleccionado?.estado === "PLANIFICACION" ||
    sprintSeleccionado?.estado === "CERRADO";
  const puedeCerrar = sprintSeleccionado?.estado === "ACTIVO";
  const puedeReplanificar =
    sprintSeleccionado?.estado === "ACTIVO" ||
    sprintSeleccionado?.estado === "CERRADO";
  const puedeEliminar = sprintSeleccionado?.estado === "PLANIFICACION";

  if (isLoading) {
    return (
      <div
        className="h-10 w-64 animate-pulse rounded-lg bg-gray-200"
        aria-label="Cargando sprints..."
      />
    );
  }

  return (
    <div className="flex items-center gap-2 flex-wrap">
      {/* Dropdown de selección */}
      <div className="relative">
        <button
          type="button"
          onClick={() => setOpen((prev) => !prev)}
          className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-900 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700"
          aria-haspopup="listbox"
          aria-expanded={open}
        >
          {sprintSeleccionado && (
            <span
              className={`h-2.5 w-2.5 rounded-full ${estadoDotClasses[sprintSeleccionado.estado] ?? "bg-gray-400"}`}
              aria-hidden="true"
            />
          )}
          <span className="max-w-[200px] truncate">
            {sprintSeleccionado
              ? `${sprintSeleccionado.nombre} · ${sprintSeleccionado.squadNombre}`
              : "Seleccionar sprint"}
          </span>
          {sprintSeleccionado && (
            <span
              className={`rounded px-1.5 py-0.5 text-xs font-medium ${estadoBadgeClasses[sprintSeleccionado.estado] ?? "bg-gray-100 text-gray-700"}`}
            >
              {estadoLabel[sprintSeleccionado.estado] ??
                sprintSeleccionado.estado}
            </span>
          )}
          <ChevronDown className="h-4 w-4 text-gray-400" aria-hidden="true" />
        </button>

        {open && (
          <ul
            role="listbox"
            aria-label="Sprints disponibles"
            className="absolute left-0 top-full z-20 mt-1 max-h-60 w-72 overflow-auto rounded-lg border border-gray-200 bg-white shadow-lg dark:border-gray-600 dark:bg-gray-800"
          >
            {sprints.length === 0 && (
              <li className="px-4 py-3 text-sm text-gray-500">
                No hay sprints disponibles
              </li>
            )}
            {sprints.map((sprint) => (
              <li
                key={sprint.id}
                role="option"
                aria-selected={sprint.id === sprintSeleccionado?.id}
                onClick={() => handleSelect(sprint)}
                className={`flex cursor-pointer items-center justify-between px-4 py-2 text-sm hover:bg-gray-50 dark:hover:bg-gray-700 ${
                  sprint.id === sprintSeleccionado?.id
                    ? "bg-blue-50 font-medium text-blue-700 dark:bg-blue-900/30 dark:text-blue-300"
                    : "text-gray-900 dark:text-gray-100"
                }`}
              >
                <div className="flex items-center gap-2 min-w-0">
                  <span
                    className={`h-2.5 w-2.5 rounded-full ${estadoDotClasses[sprint.estado] ?? "bg-gray-400"}`}
                    aria-hidden="true"
                  />
                  <span className="truncate">{sprint.nombre}</span>
                  <span className="text-xs text-muted-foreground">
                    {sprint.squadNombre}
                  </span>
                </div>
                <span
                  className={`ml-2 shrink-0 rounded px-1.5 py-0.5 text-xs font-medium ${estadoBadgeClasses[sprint.estado] ?? "bg-gray-100 text-gray-700"}`}
                >
                  {estadoLabel[sprint.estado] ?? sprint.estado}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Acción: Crear sprint */}
      <button
        type="button"
        onClick={onCrearSprint}
        className="flex items-center gap-1.5 rounded-lg bg-blue-500 px-3 py-2 text-sm font-medium text-white hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
        aria-label="Crear nuevo sprint"
      >
        <Plus className="h-4 w-4" aria-hidden="true" />
        Nuevo sprint
      </button>

      {/* Acción: Activar sprint (si está en PLANIFICACION o CERRADO) */}
      {puedeActivar && (
        <button
          type="button"
          onClick={onActivarSprint}
          className="flex items-center gap-1.5 rounded-lg bg-emerald-500 px-3 py-2 text-sm font-medium text-white hover:bg-emerald-600 focus:outline-none focus:ring-2 focus:ring-emerald-500"
          aria-label="Activar sprint seleccionado"
        >
          <Play className="h-4 w-4" aria-hidden="true" />
          Activar
        </button>
      )}

      {/* Acción: Inactivar sprint (solo si está ACTIVO) */}
      {puedeCerrar && (
        <button
          type="button"
          onClick={onCerrarSprint}
          className="flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 dark:hover:bg-gray-700"
          aria-label="Inactivar sprint seleccionado"
        >
          <CheckCircle className="h-4 w-4" aria-hidden="true" />
          Inactivar sprint
        </button>
      )}

      {/* Acción: Volver a planificación (si está ACTIVO o CERRADO) */}
      {puedeReplanificar && (
        <button
          type="button"
          onClick={onReplanificarSprint}
          className="flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-200 dark:hover:bg-gray-700"
          aria-label="Volver a estado planificacion"
        >
          <RotateCcw className="h-4 w-4" aria-hidden="true" />
          Planificación
        </button>
      )}

      {/* Acción: Eliminar sprint (solo si está en PLANIFICACION) */}
      {puedeEliminar && (
        <button
          type="button"
          onClick={onEliminarSprint}
          className="flex items-center gap-1.5 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-700 hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-500 dark:border-red-900 dark:bg-red-900/20 dark:text-red-300 dark:hover:bg-red-900/30"
          aria-label="Eliminar sprint seleccionado"
        >
          <Trash2 className="h-4 w-4" aria-hidden="true" />
          Eliminar
        </button>
      )}
    </div>
  );
};
