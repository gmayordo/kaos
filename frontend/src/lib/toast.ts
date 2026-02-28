/**
 * Toast store — Estado global de notificaciones (sin dependencias externas)
 *
 * Uso:
 *   import { toast } from '../lib/toast';
 *   toast.success("Persona creada correctamente");
 *   toast.error("Error de conexión");
 *   toast.warning("Cuota API casi agotada");
 *   toast.info("Sincronización iniciada");
 */
import { useSyncExternalStore, useCallback } from "react";

export interface Toast {
  id: string;
  message: string;
  variant: "success" | "error" | "warning" | "info";
}

// Store global simple sin dependencias
let toasts: Toast[] = [];
let listeners: Array<() => void> = [];

function emitChange() {
  for (const listener of listeners) {
    listener();
  }
}

function subscribe(listener: () => void) {
  listeners = [...listeners, listener];
  return () => {
    listeners = listeners.filter((l) => l !== listener);
  };
}

function getSnapshot() {
  return toasts;
}

function addToast(message: string, variant: Toast["variant"]) {
  const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
  toasts = [...toasts, { id, message, variant }];
  emitChange();
  return id;
}

function dismissToast(id: string) {
  toasts = toasts.filter((t) => t.id !== id);
  emitChange();
}

/** API pública para crear toasts desde cualquier lugar */
export const toast = {
  success: (message: string) => addToast(message, "success"),
  error: (message: string) => addToast(message, "error"),
  warning: (message: string) => addToast(message, "warning"),
  info: (message: string) => addToast(message, "info"),
};

/** Hook para consumir toasts en el provider */
export function useToastStore() {
  const currentToasts = useSyncExternalStore(subscribe, getSnapshot);
  const dismiss = useCallback((id: string) => dismissToast(id), []);

  return { toasts: currentToasts, dismiss };
}
