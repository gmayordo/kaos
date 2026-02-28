package com.kaos.jira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para el endpoint "Mi Semana".
 *
 * <p>Devuelve una cuadrícula de 5 días laborables (lunes a viernes) × tareas,
 * con las horas imputadas en cada celda y los totales por día y por tarea.</p>
 *
 * <p>Pensado para el componente {@code MiSemanaPage} del frontend.</p>
 */
public record WorklogSemanaResponse(

        LocalDate semanaInicio,  // lunes
        LocalDate semanaFin,     // viernes
        Long personaId,
        String personaNombre,

        /** Horas disponibles por día (de perfil horario). */
        BigDecimal horasCapacidadDia,

        /** Total de horas imputadas en toda la semana. */
        BigDecimal totalHorasSemana,

        /** Capacidad total de la semana (5 × horasCapacidadDia). */
        BigDecimal totalCapacidadSemana,

        /** Filas: una por issue/tarea con imputaciones durante la semana. */
        List<FilaTareaResponse> filas
) {

    /**
     * Fila de la cuadrícula: una issue con horas imputadas por día.
     */
    public record FilaTareaResponse(
            String jiraKey,
            String issueSummary,

            /** Horas imputadas por fecha (mapeadas por fecha ISO yyyy-MM-dd). */
            List<CeldaDiaResponse> dias,

            /** Total de horas en esta issue durante la semana. */
            BigDecimal totalHorasTarea
    ) {}

    /**
     * Celda de la cuadrícula: horas imputadas en una issue en un día concreto.
     */
    public record CeldaDiaResponse(
            LocalDate fecha,
            BigDecimal horas,
            Long worklogId  // null si no hay imputación ese día
    ) {}
}
