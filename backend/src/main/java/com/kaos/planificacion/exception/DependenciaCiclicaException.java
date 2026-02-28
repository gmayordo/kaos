package com.kaos.planificacion.exception;

/**
 * Excepción lanzada cuando crear una dependencia introduciría un ciclo en el grafo.
 * HTTP 409 Conflict.
 */
public class DependenciaCiclicaException extends RuntimeException {

    private final Long tareaOrigenId;
    private final Long tareaDestinoId;

    public DependenciaCiclicaException(Long tareaOrigenId, Long tareaDestinoId) {
        super("La dependencia de tarea " + tareaOrigenId + " → " + tareaDestinoId
                + " introduciría un ciclo en el grafo de dependencias");
        this.tareaOrigenId = tareaOrigenId;
        this.tareaDestinoId = tareaDestinoId;
    }

    public Long getTareaOrigenId() { return tareaOrigenId; }
    public Long getTareaDestinoId() { return tareaDestinoId; }
}
