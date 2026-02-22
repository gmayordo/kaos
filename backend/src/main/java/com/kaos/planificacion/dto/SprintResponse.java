package com.kaos.planificacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response para consultar un Sprint.
 */
public record SprintResponse(
        Long id,
        String nombre,
        Long squadId,
        String squadNombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String objetivo,
        String estado,
        BigDecimal capacidadTotal,
        Long tareasPendientes,
        Long tareasEnProgreso,
        Long tareasCompletadas,
        LocalDateTime createdAt
) {
}
