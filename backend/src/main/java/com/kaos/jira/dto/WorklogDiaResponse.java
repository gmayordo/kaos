package com.kaos.jira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para el endpoint "Mi Día".
 *
 * <p>Devuelve las tareas asignadas a la persona en la fecha indicada,
 * con el desglose de horas imputadas. Incluye la capacidad total del día
 * para validar si la jornada está completa.</p>
 */
public record WorklogDiaResponse(

        LocalDate fecha,
        Long personaId,
        String personaNombre,

        /** Horas disponibles en el día según perfil horario (jornada máxima). */
        BigDecimal horasCapacidad,

        /** Total de horas imputadas ese día (suma de todos los worklogs). */
        BigDecimal horasImputadas,

        /** TRUE si las horas imputadas cubren la capacidad del día. */
        boolean jornadaCompleta,

        /** Worklogs del día ordenados por issue. */
        List<WorklogLineaResponse> worklogs
) {

    /**
     * Detalle de un worklog individual en "Mi Día".
     */
    public record WorklogLineaResponse(
            Long worklogId,
            String jiraKey,
            String issueSummary,
            BigDecimal horas,
            String comentario,
            boolean sincronizado
    ) {}
}
