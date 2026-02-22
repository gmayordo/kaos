package com.kaos.calendario.dto;

import java.util.List;

/**
 * Capacidad de una persona en un rango de fechas.
 */
public record CapacidadPersonaResponse(
        Long personaId,
        String personaNombre,
        Double horasTotales,
        List<CapacidadDiaResponse> detalles
) {}
