package com.kaos.jira.dto;

/**
 * Ítem de asignación dentro de una solicitud de planificación.
 * Cada ítem convierte un issue Jira en una Tarea KAOS con los datos dados.
 */
public record PlanificarAsignacionItem(
        /** Clave del issue Jira a planificar (ej: RED-101) */
        String jiraKey,
        /** Persona KAOS a quien se asignará la tarea */
        Long personaId,
        /** Estimación en horas para la tarea KAOS */
        java.math.BigDecimal estimacion,
        /** Día del sprint (1-N) en que se ubicará la tarea */
        Integer diaAsignado,
        /** Tipo de tarea KAOS (EVOLUTIVO, CORRECTIVO, etc.) */
        String tipo,
        /** Categoría de la tarea */
        String categoria,
        /** Prioridad (ALTA, MEDIA, BAJA) */
        String prioridad
) {
}
