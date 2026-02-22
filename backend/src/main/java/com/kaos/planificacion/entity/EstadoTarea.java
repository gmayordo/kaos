package com.kaos.planificacion.entity;

/**
 * Estados posibles de una tarea.
 */
public enum EstadoTarea {
    /**
     * Tarea por iniciar
     */
    PENDIENTE,

    /**
     * Tarea en progress
     */
    EN_PROGRESO,

    /**
     * Tarea bloqueada
     */
    BLOQUEADO,

    /**
     * Tarea completada
     */
    COMPLETADA
}
