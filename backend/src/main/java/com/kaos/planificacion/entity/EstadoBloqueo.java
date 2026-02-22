package com.kaos.planificacion.entity;

/**
 * Estados posibles de un bloqueo.
 */
public enum EstadoBloqueo {
    /**
     * Bloqueo abierto, sin resolver
     */
    ABIERTO,

    /**
     * Bloqueo en gestión, siendo trabajado
     */
    EN_GESTION,

    /**
     * Bloqueo resuelto
     */
    RESUELTO
}
