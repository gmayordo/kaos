package com.kaos.squad.dto;

import java.time.LocalDateTime;
import com.kaos.squad.entity.EstadoSquad;

/**
 * Response de squad.
 */
public record SquadResponse(
        Long id,
        String nombre,
        String descripcion,
        EstadoSquad estado,
        String idSquadCorrJira,
        String idSquadEvolJira,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
