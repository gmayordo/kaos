package com.kaos.planificacion.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request para crear o actualizar una tarea continua.
 */
public record TareaContinuaRequest(
        @NotBlank @Size(max = 255) String titulo,
        String descripcion,
        @NotNull Long squadId,
        Long personaId,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin,
        Double horasPorDia,
        Boolean esInformativa,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,
        Boolean activa
) {}
