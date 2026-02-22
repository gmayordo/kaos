package com.kaos.calendario.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.kaos.calendario.entity.TipoFestivo;

/**
 * Response de festivo por ciudad.
 */
public record FestivoResponse(
        Long id,
        LocalDate fecha,
        String descripcion,
        TipoFestivo tipo,
        String ciudad,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
