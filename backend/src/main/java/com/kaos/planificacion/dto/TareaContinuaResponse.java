package com.kaos.planificacion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO para una tarea continua.
 */
public record TareaContinuaResponse(
        Long id,
        String titulo,
        String descripcion,
        Long squadId,
        String squadNombre,
        Long personaId,
        String personaNombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Double horasPorDia,
        Boolean esInformativa,
        String color,
        Boolean activa,
        LocalDateTime createdAt
) {}
