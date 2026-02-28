package com.kaos.planificacion.exception;

/**
 * Excepción lanzada cuando se intenta planificar un issue sin asignaciones
 * y no existe ninguna plantilla automática para su tipo.
 * HTTP 400 Bad Request.
 */
public class AsignacionRequeridaException extends RuntimeException {

    private final String jiraKey;
    private final String tipoJira;

    public AsignacionRequeridaException(String jiraKey, String tipoJira) {
        super("El issue " + jiraKey + " (tipo: " + tipoJira
                + ") no tiene asignaciones y no existe una plantilla activa para ese tipo");
        this.jiraKey = jiraKey;
        this.tipoJira = tipoJira;
    }

    public String getJiraKey() { return jiraKey; }
    public String getTipoJira() { return tipoJira; }
}
