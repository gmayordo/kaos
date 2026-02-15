package com.kaos.persona.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.kaos.persona.entity.Seniority;

/**
 * Response de persona.
 */
public record PersonaResponse(
        Long id,
        String nombre,
        String email,
        String idJira,
        Long perfilHorarioId,
        String perfilHorarioNombre,
        Seniority seniority,
        String skills,
        BigDecimal costeHora,
        Boolean activo,
        LocalDate fechaIncorporacion,
        Boolean sendNotifications,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
