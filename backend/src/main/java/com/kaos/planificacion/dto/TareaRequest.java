package com.kaos.planificacion.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request para crear/actualizar una Tarea.
 */
public record TareaRequest(
        @NotBlank(message = "El título de la tarea no puede estar vacío")
        String titulo,

        @NotNull(message = "El sprintId es requerido")
        Long sprintId,

        String descripcion,

        @NotNull(message = "El tipo de tarea es requerido")
        String tipo,

        @NotNull(message = "La categoría es requerida")
        String categoria,

        @NotNull(message = "La estimación es requerida")
        @Positive(message = "La estimación debe ser mayor a 0")
        BigDecimal estimacion,

        @NotNull(message = "La prioridad es requerida")
        String prioridad,

        Long personaId,

        @Min(value = 1, message = "El día asignado debe ser entre 1 y 10")
        @Max(value = 10, message = "El día asignado debe ser entre 1 y 10")
        Integer diaAsignado,

        String referenciaJira,

        String estado
) {
}
