package com.kaos.planificacion.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Request para crear o actualizar una plantilla de asignación.
 */
public record PlantillaAsignacionRequest(
        @NotBlank String nombre,
        @NotBlank String tipoJira,
        Boolean activo,
        @NotEmpty @Valid List<LineaRequest> lineas
) {
    /**
     * Línea dentro del request de plantilla.
     */
    public record LineaRequest(
            @NotNull String rol,
            @NotNull Integer porcentajeHoras,
            @NotNull Integer orden,
            Integer dependeDeOrden
    ) {}
}
