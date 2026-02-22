/**
 * EventoBadge — Badge para eventos de calendario
 * Muestra tipo de evento con color distintivo
 */

import type { FC } from "react";

type EventoTipo = "festivo" | "vacacion" | "ausencia" | "libre";

interface Props {
  tipo: EventoTipo;
  children: React.ReactNode;
  variant?: "default" | "outline";
  size?: "sm" | "md";
}

const colorClasses: Record<EventoTipo, string> = {
  festivo: "bg-zinc-100 text-zinc-700 border-zinc-300",
  vacacion: "bg-blue-100 text-blue-700 border-blue-300",
  ausencia: "bg-amber-100 text-amber-700 border-amber-300",
  libre: "bg-emerald-100 text-emerald-700 border-emerald-300",
};

const outlineClasses: Record<EventoTipo, string> = {
  festivo: "bg-white text-zinc-700 border-zinc-400",
  vacacion: "bg-white text-blue-700 border-blue-500",
  ausencia: "bg-white text-amber-700 border-amber-500",
  libre: "bg-white text-emerald-700 border-emerald-500",
};

export const EventoBadge: FC<Props> = ({
  tipo,
  children,
  variant = "default",
  size = "sm",
}) => {
  const colorClass =
    variant === "outline" ? outlineClasses[tipo] : colorClasses[tipo];
  const sizeClass = size === "sm" ? "text-xs px-2 py-0.5" : "text-sm px-3 py-1";

  return (
    <span
      className={`inline-flex items-center rounded-md border font-medium ${colorClass} ${sizeClass}`}
    >
      {children}
    </span>
  );
};
