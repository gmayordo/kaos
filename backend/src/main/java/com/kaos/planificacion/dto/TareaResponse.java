package com.kaos.planificacion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response para consultar una Tarea.
 */
public record TareaResponse(
        Long id,
        String titulo,
        Long sprintId,
        Long personaId,
        String personaNombre,
        String tipo,
        String categoria,
        BigDecimal estimacion,
        String prioridad,
        String estado,
        Integer diaAsignado,
        Double diaCapacidadDisponible,
        Boolean bloqueada,
        String referenciaJira,
        LocalDateTime createdAt
) {
}
