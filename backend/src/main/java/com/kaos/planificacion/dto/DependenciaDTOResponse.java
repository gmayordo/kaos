package com.kaos.planificacion.dto;

import com.kaos.planificacion.entity.TipoDependencia;

/**
 * Response DTO para una dependencia entre tareas.
 */
public record DependenciaDTOResponse(
        Long id,
        Long tareaOrigenId,
        String tareaOrigenTitulo,
        Long tareaDestinoId,
        String tareaDestinoTitulo,
        TipoDependencia tipo
) {
}
