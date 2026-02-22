package com.kaos.planificacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request para crear/actualizar un Bloqueo (impedimento).
 */
public record BloqueoRequest(
        @NotBlank(message = "El título del bloqueo no puede estar vacío")
        String titulo,

        String descripcion,

        @NotNull(message = "El tipo de bloqueo es requerido")
        String tipo,

        String estado,

        Long responsableId,

        String notas
) {
}
