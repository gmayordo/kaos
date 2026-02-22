package com.kaos.planificacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request para crear/actualizar un Sprint.
 * Generado desde OpenAPI spec.
 */
public record SprintRequest(
        @NotBlank(message = "El nombre del sprint no puede estar vacío")
        String nombre,

        @NotNull(message = "El squadId es requerido")
        Long squadId,

        @NotNull(message = "La fecha de inicio es requerida")
        LocalDate fechaInicio,

        String objetivo
) {
}
