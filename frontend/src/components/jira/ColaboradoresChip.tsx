/**
 * ColaboradoresChip — Avatares apilados de colaboradores
 * Presentacional. Muestra hasta `maxVisible` avatares de iniciales,
 * luego "+N" si hay más. Tooltip con lista de nombres al hacer hover.
 */

import { clsx } from "clsx";
import type { FC } from "react";
import { useState } from "react";

// ─── Paleta de colores por índice ───────────────────────────────
const AVATAR_COLORS = [
  "bg-violet-500 text-white",
  "bg-sky-500 text-white",
  "bg-emerald-500 text-white",
  "bg-amber-500 text-white",
  "bg-rose-500 text-white",
  "bg-indigo-500 text-white",
  "bg-teal-500 text-white",
  "bg-orange-500 text-white",
];

function getColor(index: number): string {
  return AVATAR_COLORS[index % AVATAR_COLORS.length];
}

function getInitials(nombre: string): string {
  const parts = nombre.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

// ─── Props ───────────────────────────────────────────────────────

export interface ColaboradoresChipProps {
  /** Lista de personas a mostrar */
  personas: { nombre: string }[];
  /** Máximo de avatares visibles antes de mostrar "+N" (default: 3) */
  maxVisible?: number;
  /** Tamaño del avatar en px (default: 24) */
  size?: number;
}

// ─── Componente ──────────────────────────────────────────────────

export const ColaboradoresChip: FC<ColaboradoresChipProps> = ({
  personas,
  maxVisible = 3,
  size = 24,
}) => {
  const [showTooltip, setShowTooltip] = useState(false);

  if (!personas || personas.length === 0) return null;

  const visible = personas.slice(0, maxVisible);
  const overflow = personas.length - maxVisible;

  const fontSize = size <= 20 ? 9 : size <= 28 ? 10 : 12;
  const avatarStyle = {
    width: size,
    height: size,
    fontSize,
    marginLeft: -6,
  };

  return (
    <div
      className="relative inline-flex items-center"
      onMouseEnter={() => setShowTooltip(true)}
      onMouseLeave={() => setShowTooltip(false)}
      aria-label={`Colaboradores: ${personas.map((p) => p.nombre).join(", ")}`}
    >
      {/* Avatares visibles */}
      <div className="flex items-center" style={{ paddingLeft: 6 }}>
        {visible.map((persona, i) => (
          <div
            key={i}
            className={clsx(
              "rounded-full flex items-center justify-center font-semibold ring-2 ring-white dark:ring-gray-800 select-none shrink-0",
              getColor(i),
            )}
            style={avatarStyle}
            aria-hidden="true"
          >
            {getInitials(persona.nombre)}
          </div>
        ))}

        {/* Overflow badge */}
        {overflow > 0 && (
          <div
            className="rounded-full flex items-center justify-center font-semibold ring-2 ring-white dark:ring-gray-800 bg-gray-200 text-gray-600 dark:bg-gray-600 dark:text-gray-200 shrink-0"
            style={avatarStyle}
            aria-label={`${overflow} personas más`}
          >
            +{overflow}
          </div>
        )}
      </div>

      {/* Tooltip */}
      {showTooltip && personas.length > 0 && (
        <div
          role="tooltip"
          className="absolute z-50 bottom-full left-0 mb-1.5 min-w-max max-w-[220px] rounded-md bg-gray-900 text-white text-xs py-1.5 px-2.5 shadow-lg pointer-events-none"
        >
          <ul className="space-y-0.5">
            {personas.map((p, i) => (
              <li key={i} className="flex items-center gap-1.5">
                <span
                  className={clsx(
                    "w-2 h-2 rounded-full shrink-0",
                    AVATAR_COLORS[i % AVATAR_COLORS.length].split(" ")[0],
                  )}
                />
                <span className="truncate">{p.nombre}</span>
              </li>
            ))}
          </ul>
          {/* Flecha del tooltip */}
          <div className="absolute top-full left-3 border-4 border-transparent border-t-gray-900" />
        </div>
      )}
    </div>
  );
};
