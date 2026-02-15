package com.kaos.calendario.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kaos.calendario.entity.TipoAusencia;

/**
 * Response de ausencia.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AusenciaResponse(
        Long id,
        Long personaId,
        String personaNombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        TipoAusencia tipo,
        String comentario,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
