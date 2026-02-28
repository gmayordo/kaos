package com.kaos.planificacion.dto;

import java.util.List;

/**
 * Response DTO para una plantilla de asignación.
 */
public record PlantillaAsignacionResponse(
        Long id,
        String nombre,
        String tipoJira,
        Boolean activo,
        List<LineaResponse> lineas
) {
    /**
     * Línea de la plantilla (una por rol).
     */
    public record LineaResponse(
            Long id,
            String rol,
            Integer porcentajeHoras,
            Integer orden,
            Integer dependeDeOrden
    ) {}
}
