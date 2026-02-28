package com.kaos.planificacion.exception;

/**
 * Excepción lanzada cuando ya existe la dependencia entre las dos tareas.
 * HTTP 409 Conflict.
 */
public class DependenciaDuplicadaException extends RuntimeException {

    private final Long tareaOrigenId;
    private final Long tareaDestinoId;

    public DependenciaDuplicadaException(Long tareaOrigenId, Long tareaDestinoId) {
        super("Ya existe una dependencia entre la tarea " + tareaOrigenId
                + " y la tarea " + tareaDestinoId);
        this.tareaOrigenId = tareaOrigenId;
        this.tareaDestinoId = tareaDestinoId;
    }

    public Long getTareaOrigenId() { return tareaOrigenId; }
    public Long getTareaDestinoId() { return tareaDestinoId; }
}
