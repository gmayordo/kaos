package com.kaos.planificacion.entity;

/**
 * Estados posibles de un sprint.
 */
public enum SprintEstado {
    /**
     * Sprint en fase de planificación, aún editable
     */
    PLANIFICACION,

    /**
     * Sprint activo, en ejecución
     */
    ACTIVO,

    /**
     * Sprint cerrado, historial
     */
    CERRADO
}
