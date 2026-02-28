package com.kaos.planificacion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request para crear o actualizar una asignación de tarea padre en el timeline.
 */
public record TareaAsignacionTimelineRequest(
        @NotNull Long tareaId,
        @NotNull Long personaId,
        @NotNull Long sprintId,
        @NotNull @Min(1) @Max(10) Integer diaInicio,
        @NotNull @Min(1) @Max(10) Integer diaFin,
        Double horasPorDia,
        Boolean esInformativa
) {}
