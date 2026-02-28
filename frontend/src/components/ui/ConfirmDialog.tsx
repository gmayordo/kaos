/**
 * ConfirmDialog — Diálogo de confirmación accesible
 * Reemplaza window.confirm() con un modal accesible.
 *
 * Features:
 * - role="alertdialog" para alertas destructivas
 * - Focus en botón "Cancelar" al abrir (acción segura)
 * - Escape cierra (equivale a cancelar)
 * - Variantes: danger (destructiva), warning, default
 */
import { AlertTriangle, HelpCircle, Trash2 } from "lucide-react";
import { useCallback, useEffect, useRef } from "react";
import { cn } from "../../lib/utils";

interface ConfirmDialogProps {
  /** Controla si el diálogo está visible */
  isOpen: boolean;
  /** Callback al cancelar */
  onCancel: () => void;
  /** Callback al confirmar */
  onConfirm: () => void;
  /** Título del diálogo */
  title: string;
  /** Mensaje descriptivo */
  description: string;
  /** Texto del botón de confirmación (default: "Confirmar") */
  confirmText?: string;
  /** Texto del botón de cancelación (default: "Cancelar") */
  cancelText?: string;
  /** Variante visual */
  variant?: "danger" | "warning" | "default";
  /** Si la acción está en progreso */
  isLoading?: boolean;
}

const variantConfig = {
  danger: {
    icon: Trash2,
    iconClassName: "text-destructive bg-destructive/10",
    confirmClassName:
      "bg-destructive text-destructive-foreground hover:bg-destructive/90",
  },
  warning: {
    icon: AlertTriangle,
    iconClassName: "text-amber-600 bg-amber-50",
    confirmClassName: "bg-amber-600 text-white hover:bg-amber-700",
  },
  default: {
    icon: HelpCircle,
    iconClassName: "text-primary bg-primary/10",
    confirmClassName: "bg-primary text-primary-foreground hover:bg-primary/90",
  },
};

export function ConfirmDialog({
  isOpen,
  onCancel,
  onConfirm,
  title,
  description,
  confirmText = "Confirmar",
  cancelText = "Cancelar",
  variant = "default",
  isLoading = false,
}: ConfirmDialogProps) {
  const cancelRef = useRef<HTMLButtonElement>(null);
  const dialogRef = useRef<HTMLDivElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  const config = variantConfig[variant];
  const Icon = config.icon;

  // Focus management
  useEffect(() => {
    if (isOpen) {
      previousFocusRef.current = document.activeElement as HTMLElement;
      requestAnimationFrame(() => {
        cancelRef.current?.focus();
      });
    } else if (previousFocusRef.current) {
      previousFocusRef.current.focus();
      previousFocusRef.current = null;
    }
  }, [isOpen]);

  // Escape to close
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onCancel();
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onCancel]);

  // Focus trap
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key !== "Tab") return;
    const focusable = dialogRef.current?.querySelectorAll<HTMLElement>(
      "button:not(:disabled)",
    );
    if (!focusable || focusable.length === 0) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first.focus();
    }
  }, []);

  // Bloquear scroll
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
        onClick={onCancel}
        aria-hidden="true"
      />

      {/* Dialog */}
      <div
        ref={dialogRef}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        aria-describedby="confirm-dialog-description"
        onKeyDown={handleKeyDown}
        className="relative z-10 w-full max-w-md mx-4 bg-card rounded-lg shadow-xl border border-border overflow-hidden"
      >
        <div className="p-6">
          {/* Icon + Title */}
          <div className="flex items-start gap-4">
            <div
              className={cn(
                "flex-shrink-0 flex items-center justify-center w-10 h-10 rounded-full",
                config.iconClassName,
              )}
            >
              <Icon className="h-5 w-5" aria-hidden="true" />
            </div>
            <div className="flex-1 min-w-0">
              <h3
                id="confirm-dialog-title"
                className="text-lg font-semibold text-foreground"
              >
                {title}
              </h3>
              <p
                id="confirm-dialog-description"
                className="mt-2 text-sm text-muted-foreground"
              >
                {description}
              </p>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3 px-6 py-4 bg-muted/30 border-t border-border">
          <button
            ref={cancelRef}
            onClick={onCancel}
            disabled={isLoading}
            className="px-4 py-2 text-sm font-medium text-foreground bg-background border border-border rounded-md hover:bg-muted transition-colors disabled:opacity-50"
          >
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            disabled={isLoading}
            className={cn(
              "px-4 py-2 text-sm font-medium rounded-md transition-colors disabled:opacity-50",
              config.confirmClassName,
            )}
          >
            {isLoading ? "Procesando..." : confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
