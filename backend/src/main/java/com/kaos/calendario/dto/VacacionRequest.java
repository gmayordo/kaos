package com.kaos.calendario.dto;

import com.kaos.calendario.entity.EstadoVacacion;
import com.kaos.calendario.entity.TipoVacacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request para crear o actualizar una vacación.
 */
public record VacacionRequest(
        @NotNull(message = "El ID de la persona es obligatorio")
        Long personaId,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin,

        @NotNull(message = "El tipo es obligatorio")
        TipoVacacion tipo,

        @NotNull(message = "El estado es obligatorio")
        EstadoVacacion estado,

        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String comentario
) {}
