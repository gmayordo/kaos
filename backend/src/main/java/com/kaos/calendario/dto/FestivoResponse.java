package com.kaos.calendario.dto;

import com.kaos.calendario.entity.TipoFestivo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response de festivo con personas asignadas.
 */
public record FestivoResponse(
        Long id,
        LocalDate fecha,
        String descripcion,
        TipoFestivo tipo,
        List<PersonaBasicInfo> personas,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
