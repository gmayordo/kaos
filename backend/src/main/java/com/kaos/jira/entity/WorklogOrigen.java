package com.kaos.jira.entity;

/**
 * Origen de un worklog en KAOS.
 *
 * <ul>
 *   <li>{@link #JIRA} — worklog importado desde Jira (solo lectura desde KAOS)</li>
 *   <li>{@link #KAOS} — worklog registrado en KAOS, pendiente de enviar a Jira</li>
 * </ul>
 */
public enum WorklogOrigen {
    /** Importado desde Jira. No se modifica desde KAOS. */
    JIRA,
    /** Registrado en KAOS, puede estar pendiente de sincronización. */
    KAOS
}
