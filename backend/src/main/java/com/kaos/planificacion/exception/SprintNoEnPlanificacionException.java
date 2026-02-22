package com.kaos.planificacion.exception;

/**
 * Excepción lanzada cuando una operación requiere que el sprint esté en estado PLANIFICACION.
 */
public class SprintNoEnPlanificacionException extends RuntimeException {

    private final Long sprintId;
    private final String estadoActual;

    public SprintNoEnPlanificacionException(Long sprintId, String estadoActual) {
        super("Sprint " + sprintId + " no está en estado PLANIFICACION. Estado actual: " + estadoActual);
        this.sprintId = sprintId;
        this.estadoActual = estadoActual;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public String getEstadoActual() {
        return estadoActual;
    }
}
