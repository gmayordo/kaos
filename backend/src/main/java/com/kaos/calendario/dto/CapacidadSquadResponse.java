package com.kaos.calendario.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Capacidad total de un squad en un rango de fechas.
 */
public record CapacidadSquadResponse(
        Long squadId,
        String squadNombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Double horasTotales,
        List<CapacidadPersonaResponse> personas
) {}
