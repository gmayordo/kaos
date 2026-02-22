package com.kaos.calendario.dto;

import java.time.LocalDate;
import com.kaos.calendario.entity.MotivoReduccion;

/**
 * Capacidad de una persona en un día específico.
 */
public record CapacidadDiaResponse(
        LocalDate fecha,
        Double horasDisponibles,
        Double horasTeoricasMaximas,
        Integer porcentajeCapacidad,
        MotivoReduccion motivoReduccion
) {}
