/**
 * useDocumentTitle — Actualiza el título del documento al montar la ruta.
 * Restaura el título por defecto al desmontar.
 */
import { useEffect } from "react";

const DEFAULT_TITLE = "KAOS — Gestión de Equipos";

export function useDocumentTitle(title: string) {
  useEffect(() => {
    const fullTitle = title ? `${title} — KAOS` : DEFAULT_TITLE;
    document.title = fullTitle;

    return () => {
      document.title = DEFAULT_TITLE;
    };
  }, [title]);
}
