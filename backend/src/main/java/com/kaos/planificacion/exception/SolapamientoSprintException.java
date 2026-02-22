package com.kaos.planificacion.exception;

/**
 * Excepción lanzada cuando se intenta crear un sprint que se solapa con otro del mismo squad.
 */
public class SolapamientoSprintException extends RuntimeException {

    private final Long squadId;
    private final String mensaje;

    public SolapamientoSprintException(Long squadId, String mensaje) {
        super(mensaje);
        this.squadId = squadId;
        this.mensaje = mensaje;
    }

    public Long getSquadId() {
        return squadId;
    }

    public String getMensaje() {
        return mensaje;
    }
}
