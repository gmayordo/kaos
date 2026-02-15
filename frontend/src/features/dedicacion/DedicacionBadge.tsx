/**
 * DedicacionBadge — Badge visual para mostrar dedicación en squad
 * Componente presentacional para mostrar asignaciones persona-squad
 */

import { X } from "lucide-react";
import type { FC } from "react";

interface Props {
  /** Nombre del squad */
  squadNombre: string;
  /** Porcentaje de dedicación */
  porcentaje: number;
  /** Rol en el squad */
  rol: string;
  /** Callback al hacer click en eliminar (opcional) */
  onRemove?: () => void;
  /** Estado de carga */
  isRemoving?: boolean;
}

/**
 * Badge para mostrar la dedicación de una persona en un squad.
 * Muestra % + rol + squad con posibilidad de eliminar.
 */
export const DedicacionBadge: FC<Props> = ({
  squadNombre,
  porcentaje,
  rol,
  onRemove,
  isRemoving = false,
}) => {
  return (
    <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-primary/10 border border-primary/20 rounded-full text-sm">
      <div className="flex items-center gap-2">
        <span className="font-semibold text-primary">{porcentaje}%</span>
        <span className="text-muted-foreground">·</span>
        <span className="font-medium">{squadNombre}</span>
        <span className="text-muted-foreground text-xs">
          ({rol.replace("_", " ")})
        </span>
      </div>

      {onRemove && (
        <button
          onClick={onRemove}
          disabled={isRemoving}
          className="text-muted-foreground hover:text-destructive transition-colors disabled:opacity-50"
          title="Eliminar dedicación"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      )}
    </div>
  );
};
