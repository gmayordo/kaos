package com.kaos.planificacion.entity;

/**
 * Tipos de tareas en un sprint.
 */
public enum TipoTarea {
    /**
     * User story o requisito
     */
    HISTORIA,

    /**
     * Tarea o subtarea
     */
    TAREA,

    /**
     * Defecto o issue a corregir
     */
    BUG,

    /**
     * Investigación o spike técnico
     */
    SPIKE
}
