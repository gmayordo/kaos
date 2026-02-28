package com.kaos.planificacion.entity;

/**
 * Tipos de dependencia entre tareas.
 * <ul>
 *   <li>ESTRICTA — la tarea destino no puede iniciarse hasta que la origen esté COMPLETADA</li>
 *   <li>SUAVE    — relación informativa, no bloquea el flujo de trabajo</li>
 * </ul>
 */
public enum TipoDependencia {
    ESTRICTA,
    SUAVE
}
