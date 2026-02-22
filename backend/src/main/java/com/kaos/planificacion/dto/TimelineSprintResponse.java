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
     */
    @Builder
    public record TareaEnLinea(
            Long tareaId,
            String titulo,
            Double estimacion,
            String estado,
            String prioridad,
            Boolean bloqueada
    ) {
    }
}
