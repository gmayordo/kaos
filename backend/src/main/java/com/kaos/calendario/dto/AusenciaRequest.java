package com.kaos.calendario.dto;

import java.time.LocalDate;
import com.kaos.calendario.entity.TipoAusencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request para crear o actualizar una ausencia.
 */
public record AusenciaRequest(
        @NotNull(message = "El ID de la persona es obligatorio")
        Long personaId,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        /** Fecha de fin opcional (null = ausencia indefinida) */
        LocalDate fechaFin,

        @NotNull(message = "El tipo es obligatorio")
        TipoAusencia tipo,

        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String comentario
) {}
