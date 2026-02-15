package com.kaos.calendario.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.kaos.calendario.entity.TipoAusencia;

/**
 * Response de ausencia.
 */
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
