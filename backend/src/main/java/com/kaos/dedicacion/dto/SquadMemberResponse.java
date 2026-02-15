package com.kaos.dedicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.kaos.persona.entity.Rol;

/**
 * Response de asignación persona-squad.
 * Incluye capacidad diaria calculada.
 */
public record SquadMemberResponse(
        Long id,
        Long personaId,
        String personaNombre,
        Long squadId,
        String squadNombre,
        Rol rol,
        Integer porcentaje,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal capacidadDiariaLunes,
        BigDecimal capacidadDiariaMartes,
        BigDecimal capacidadDiariaMiercoles,
        BigDecimal capacidadDiariaJueves,
        BigDecimal capacidadDiariaViernes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
