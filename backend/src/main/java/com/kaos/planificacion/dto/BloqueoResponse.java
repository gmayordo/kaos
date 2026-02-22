package com.kaos.planificacion.dto;

import java.time.LocalDateTime;

/**
 * Response para consultar un Bloqueo (impedimento).
 */
public record BloqueoResponse(
        Long id,
        String titulo,
        String descripcion,
        String tipo,
        String estado,
        Long responsableId,
        String responsableNombre,
        LocalDateTime fechaResolucion,
        String notas,
        Long tareasAfectadas,
        LocalDateTime createdAt
) {
}
