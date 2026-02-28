/**
 * AccessibleModal — Modal accesible con focus trap, Escape y aria-modal
 * Componente base para TODOS los modales de la aplicación.
 *
 * Features:
 * - role="dialog" + aria-modal="true" + aria-labelledby
 * - Focus trap: Tab/Shift+Tab cicla dentro del modal
 * - Escape cierra el modal
 * - Focus al primer elemento focusable al abrir
 * - Devuelve foco al trigger al cerrar
 * - Click en overlay cierra
 */
import { useCallback, useEffect, useRef, type ReactNode } from "react";
import { cn } from "../../lib/utils";

interface AccessibleModalProps {
  /** Controla si el modal está visible */
  isOpen: boolean;
  /** Callback al cerrar (Escape, click overlay, botón cerrar) */
  onClose: () => void;
  /** Título del modal (se usa para aria-labelledby) */
  title: string;
  /** Contenido del modal */
  children: ReactNode;
  /** Clases adicionales del contenedor del modal */
  className?: string;
  /** Tamaño del modal */
  size?: "sm" | "md" | "lg" | "xl";
  /** ID personalizado para aria-labelledby (por defecto auto-generado) */
  titleId?: string;
}

const sizeClasses: Record<string, string> = {
  sm: "max-w-sm",
  md: "max-w-lg",
  lg: "max-w-2xl",
  xl: "max-w-4xl",
};

export function AccessibleModal({
  isOpen,
  onClose,
  title,
  children,
  className,
  size = "md",
  titleId,
}: AccessibleModalProps) {
  const modalRef = useRef<HTMLDivElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);
  const resolvedTitleId = titleId ?? "modal-title";

  // Guardar foco anterior y restaurar al cerrar
  useEffect(() => {
    if (isOpen) {
      previousFocusRef.current = document.activeElement as HTMLElement;

      // Focus al primer elemento focusable del modal
      requestAnimationFrame(() => {
        const focusable = modalRef.current?.querySelectorAll<HTMLElement>(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
        );
        if (focusable && focusable.length > 0) {
          focusable[0].focus();
        }
      });
    } else if (previousFocusRef.current) {
      previousFocusRef.current.focus();
      previousFocusRef.current = null;
    }
  }, [isOpen]);

  // Escape para cerrar
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onClose();
      }
    };

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  // Focus trap
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key !== "Tab") return;

    const focusableElements = modalRef.current?.querySelectorAll<HTMLElement>(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
    );
    if (!focusableElements || focusableElements.length === 0) return;

    const firstEl = focusableElements[0];
    const lastEl = focusableElements[focusableElements.length - 1];

    if (e.shiftKey) {
      if (document.activeElement === firstEl) {
        e.preventDefault();
        lastEl.focus();
      }
    } else {
      if (document.activeElement === lastEl) {
        e.preventDefault();
        firstEl.focus();
      }
    }
  }, []);

  // Bloquear scroll del body
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="presentation"
    >
      {/* Overlay */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal */}
      <div
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={resolvedTitleId}
        onKeyDown={handleKeyDown}
        className={cn(
          "relative z-10 w-full mx-4 bg-card rounded-lg shadow-xl border border-border",
          sizeClasses[size],
          className,
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border">
          <h2
            id={resolvedTitleId}
            className="text-lg font-semibold text-foreground"
          >
            {title}
          </h2>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
            aria-label="Cerrar diálogo"
          >
            <svg
              className="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="p-6">{children}</div>
      </div>
    </div>
  );
}
