package com.kaos.planificacion.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * Response para el dashboard de un sprint.
 * Contiene métricas, ocupación y alertas.
 */
@Builder
public record DashboardSprintResponse(
        Long sprintId,
        String sprintNombre,
        String estado,
        Long tareasTotal,
        Long tareasPendientes,
        Long tareasEnProgreso,
        Long tareasCompletadas,
        Long tareasBloqueadas,
        Double progresoEsperado,
        Double progresoReal,
        Double capacidadTotalHoras,
        Double capacidadAsignadaHoras,
        Double ocupacionPorcentaje,
        Long bloqueosActivos,
        List<String> alertas,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {
}
