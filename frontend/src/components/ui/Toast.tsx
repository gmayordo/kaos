/**
 * Toast — Sistema de notificaciones accesible
 *
 * Uso:
 *   import { toast } from '../../lib/toast';
 *   toast.success("Persona creada");
 *   toast.error("Error de conexión");
 *
 * Requiere <ToastProvider /> en el root de la app.
 */
import { AlertTriangle, CheckCircle2, Info, X, XCircle } from "lucide-react";
import { useEffect, useRef } from "react";
import { useToastStore, type Toast as ToastType } from "../../lib/toast";
import { cn } from "../../lib/utils";

const variantConfig = {
  success: {
    icon: CheckCircle2,
    className: "bg-emerald-50 border-l-emerald-500 text-emerald-800",
    iconClassName: "text-emerald-600",
    duration: 5000,
  },
  error: {
    icon: XCircle,
    className: "bg-red-50 border-l-red-500 text-red-800",
    iconClassName: "text-red-600",
    duration: 8000,
  },
  warning: {
    icon: AlertTriangle,
    className: "bg-amber-50 border-l-amber-500 text-amber-800",
    iconClassName: "text-amber-600",
    duration: 8000,
  },
  info: {
    icon: Info,
    className: "bg-blue-50 border-l-blue-500 text-blue-800",
    iconClassName: "text-blue-600",
    duration: 5000,
  },
};

function ToastItem({
  toast,
  onDismiss,
}: {
  toast: ToastType;
  onDismiss: (id: string) => void;
}) {
  const config = variantConfig[toast.variant];
  const Icon = config.icon;
  const timerRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    timerRef.current = setTimeout(() => {
      onDismiss(toast.id);
    }, config.duration);

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [toast.id, config.duration, onDismiss]);

  return (
    <div
      className={cn(
        "flex items-start gap-3 p-4 rounded-lg shadow-lg border-l-4 min-w-[320px] max-w-md",
        "animate-in slide-in-from-right-full duration-300",
        config.className,
      )}
      role="status"
      aria-live={toast.variant === "error" ? "assertive" : "polite"}
    >
      <Icon
        className={cn("h-5 w-5 flex-shrink-0 mt-0.5", config.iconClassName)}
        aria-hidden="true"
      />
      <p className="text-sm font-medium flex-1">{toast.message}</p>
      <button
        onClick={() => onDismiss(toast.id)}
        className="flex-shrink-0 p-0.5 rounded hover:bg-black/10 transition-colors"
        aria-label="Cerrar notificación"
      >
        <X className="h-4 w-4" aria-hidden="true" />
      </button>
    </div>
  );
}

/**
 * ToastProvider — Renderiza los toasts activos.
 * Colocar una vez en el root de la app (main.tsx o __root.tsx)
 */
export function ToastProvider() {
  const { toasts, dismiss } = useToastStore();

  if (toasts.length === 0) return null;

  return (
    <div
      className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2"
      aria-label="Notificaciones"
    >
      {toasts.slice(0, 3).map((t) => (
        <ToastItem key={t.id} toast={t} onDismiss={dismiss} />
      ))}
    </div>
  );
}
