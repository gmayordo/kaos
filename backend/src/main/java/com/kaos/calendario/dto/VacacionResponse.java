package com.kaos.calendario.dto;

import com.kaos.calendario.entity.EstadoVacacion;
import com.kaos.calendario.entity.TipoVacacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response de vacación.
 */
public record VacacionResponse(
        Long id,
        Long personaId,
        String personaNombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer diasLaborables,
        TipoVacacion tipo,
        EstadoVacacion estado,
        String comentario,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
