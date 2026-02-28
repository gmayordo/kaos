package com.kaos.planificacion.dto;

import java.time.LocalDateTime;

/**
 * Response DTO para una asignación de tarea padre en el timeline.
 */
public record TareaAsignacionTimelineResponse(
        Long id,
        Long tareaId,
        String tareaTitulo,
        String tareaJiraKey,
        Long personaId,
        String personaNombre,
        Long sprintId,
        Integer diaInicio,
        Integer diaFin,
        Double horasPorDia,
        Boolean esInformativa,
        LocalDateTime createdAt
) {}
