package com.kaos.planificacion.entity;

/**
 * Tipos de bloqueos (impedimentos).
 */
public enum TipoBloqueo {
    /**
     * Espera de cliente o proveedor externo
     */
    DEPENDENCIA_EXTERNA,

    /**
     * Falta recurso (máquina, espacio, tiempo)
     */
    RECURSO,

    /**
     * Bloqueo técnico (bug, ambiente, infra)
     */
    TECNICO,

    /**
     * Falta comunicación, decisión pendiente
     */
    COMUNICACION,

    /**
     * Otro tipo no clasificado
     */
    OTRO
}
