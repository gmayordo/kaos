package com.kaos.planificacion.dto;

import com.kaos.planificacion.entity.TipoDependencia;
import jakarta.validation.constraints.NotNull;

/**
 * Request para crear una dependencia desde una tarea origen hacia una destino.
 */
public record CrearDependenciaRequest(
        /** ID de la tarea destino (la que será bloqueada) */
        @NotNull Long destinoId,
        /** Tipo de dependencia */
        @NotNull TipoDependencia tipo
) {
}
