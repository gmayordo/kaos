package com.kaos.planificacion.entity;

/**
 * Niveles de prioridad de una tarea.
 */
public enum Prioridad {
    /**
     * Baja prioridad
     */
    BAJA,

    /**
     * Prioridad normal (default)
     */
    NORMAL,

    /**
     * Prioridad alta, requiere atención
     */
    ALTA,

    /**
     * Crítica, bloquea otros trabajos
     */
    BLOQUEANTE
}
