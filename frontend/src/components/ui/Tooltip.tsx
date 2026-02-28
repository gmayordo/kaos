/**
 * Tooltip — Tooltip accesible con hover y focus
 * Componente ligero que no depende de bibliotecas externas.
 *
 * Features:
 * - Trigger: hover (300ms delay) + focus
 * - role="tooltip" + aria-describedby
 * - Escapa con Escape
 * - Posicionamiento: top (por defecto)
 * - Accesible por teclado (aparece con focus)
 */
import { useCallback, useId, useRef, useState, type ReactElement } from "react";
import { cn } from "../../lib/utils";

interface TooltipProps {
  /** Texto del tooltip */
  content: string;
  /** Elemento trigger (debe poder recibir focus) */
  children: ReactElement;
  /** Posición del tooltip */
  position?: "top" | "bottom" | "left" | "right";
  /** Delay antes de mostrar (ms) */
  delay?: number;
  /** Clases adicionales */
  className?: string;
}

const positionClasses: Record<string, string> = {
  top: "bottom-full left-1/2 -translate-x-1/2 mb-2",
  bottom: "top-full left-1/2 -translate-x-1/2 mt-2",
  left: "right-full top-1/2 -translate-y-1/2 mr-2",
  right: "left-full top-1/2 -translate-y-1/2 ml-2",
};

export function Tooltip({
  content,
  children,
  position = "top",
  delay = 300,
  className,
}: TooltipProps) {
  const [isVisible, setIsVisible] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout>>();
  const tooltipId = useId();

  const show = useCallback(() => {
    timeoutRef.current = setTimeout(() => setIsVisible(true), delay);
  }, [delay]);

  const hide = useCallback(() => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    setIsVisible(false);
  }, []);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Escape") {
        hide();
      }
    },
    [hide],
  );

  return (
    <div
      className="relative inline-flex"
      onMouseEnter={show}
      onMouseLeave={hide}
      onFocus={show}
      onBlur={hide}
      onKeyDown={handleKeyDown}
    >
      {/* Clonar children para añadir aria-describedby */}
      <div aria-describedby={isVisible ? tooltipId : undefined}>{children}</div>

      {/* Tooltip */}
      {isVisible && (
        <div
          id={tooltipId}
          role="tooltip"
          className={cn(
            "absolute z-50 px-2.5 py-1.5 text-xs font-medium",
            "bg-foreground text-background rounded-md shadow-md",
            "whitespace-nowrap pointer-events-none",
            "animate-in fade-in-0 zoom-in-95 duration-150",
            positionClasses[position],
            className,
          )}
        >
          {content}
        </div>
      )}
    </div>
  );
}
