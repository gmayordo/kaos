package com.kaos.planificacion.exception;

/**
 * Excepción lanzada cuando se intenta operar sobre una tarea que no está en estado PENDIENTE.
 */
public class TareaNoEnPendienteException extends RuntimeException {

    private final Long tareaId;
    private final String estadoActual;

    public TareaNoEnPendienteException(Long tareaId, String estadoActual) {
        super("Tarea " + tareaId + " no está en estado PENDIENTE. Estado actual: " + estadoActual);
        this.tareaId = tareaId;
        this.estadoActual = estadoActual;
    }

    public Long getTareaId() {
        return tareaId;
    }

    public String getEstadoActual() {
        return estadoActual;
    }
}
