package com.kaos.planificacion.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kaos.planificacion.entity.TareaAsignacionTimeline;

/**
 * Repositorio para operaciones sobre TareaAsignacionTimeline.
 */
@Repository
public interface TareaAsignacionTimelineRepository extends JpaRepository<TareaAsignacionTimeline, Long> {

    /**
     * Lista todas las asignaciones de un sprint.
     */
    List<TareaAsignacionTimeline> findBySprintId(Long sprintId);

    /**
     * Lista las asignaciones de una tarea específica en un sprint.
     */
    List<TareaAsignacionTimeline> findByTareaIdAndSprintId(Long tareaId, Long sprintId);

    /**
     * Lista las asignaciones de una persona en un sprint.
     */
    List<TareaAsignacionTimeline> findByPersonaIdAndSprintId(Long personaId, Long sprintId);
}
