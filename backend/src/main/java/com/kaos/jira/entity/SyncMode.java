package com.kaos.jira.entity;

/**
 * Modo de ejecución de la sincronización Jira.
 *
 * <ul>
 *   <li>{@code FULL} — descarga todas las issues del sprint abierto sin filtro de fecha.</li>
 *   <li>{@code INCREMENTAL} — solo issues actualizadas desde la última sync exitosa
 *       ({@code updated >= ultimaSync}), minimizando llamadas API.</li>
 *   <li>{@code DRY_RUN} — ejecuta la búsqueda sin persistir datos; útil para diagnóstico
 *       de JQL y verificación de cuota.</li>
 * </ul>
 */
public enum SyncMode {

    /** Descarga completa: todas las issues del sprint abierto. */
    FULL,

    /** Solo issues actualizadas desde la última sync exitosa. */
    INCREMENTAL,

    /** Búsqueda sin persistencia — solo conteo y log de resultados. */
    DRY_RUN
}
