package com.kaos.planificacion.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * Response para la timeline (grid) de un sprint.
 * Matriz [personas] x [días] con tareas asignadas.
 */
@Builder
public record TimelineSprintResponse(
        Long sprintId,
        String sprintNombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        List<PersonaEnLinea> personas
) {

    /**
     * Persona en la timeline con sus tareas por día.
     */
    @Builder
    public record PersonaEnLinea(
            Long personaId,
            String personaNombre,
            List<DiaConTareas> dias
    ) {
    }

    /**
     * Día en la timeline de una persona con tareas asignadas.
     */
    @Builder
    public record DiaConTareas(
            Integer dia,
            Double horasDisponibles,
            List<TareaEnLinea> tareas
    ) {
    }

    /**
     * Tarea en la timeline.
     * Puede representar una tarea puntual (1 día) o una barra multi-día (diaInicio/diaFin).
     */
    @Builder
    public record TareaEnLinea(
            Long tareaId,
            String titulo,
            Double estimacion,
            String tipo,
            String estado,
            String prioridad,
            Boolean bloqueada,
            // ── Campos para Timeline Avanzado ──────────────────────────────
            /** Origen: "SPRINT" (tarea normal), "JIRA_PADRE" (via asignacion), "CONTINUA" */
            String origen,
            /** Día de inicio (null para tareas puntuales de 1 día) */
            Integer diaInicio,
            /** Día de fin (null para tareas puntuales de 1 día) */
            Integer diaFin,
            /** Horas dedicadas por día (null = capacidad completa) */
            Double horasPorDia,
            /** Si true, no descuenta capacidad del sprint */
            Boolean esInformativa,
            /** Clave Jira para enlace directo (ej: PROJ-123) */
            String jiraIssueKey,
            /** Color personalizado para tareas continuas (#RRGGBB) */
            String color
    ) {
    }
}
