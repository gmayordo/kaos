package com.kaos.horario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response de perfil de horario.
 */
public record PerfilHorarioResponse(
        Long id,
        String nombre,
        String zonaHoraria,
        BigDecimal horasLunes,
        BigDecimal horasMartes,
        BigDecimal horasMiercoles,
        BigDecimal horasJueves,
        BigDecimal horasViernes,
        BigDecimal totalSemanal,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
