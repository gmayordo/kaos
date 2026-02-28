package com.kaos.planificacion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kaos.planificacion.entity.TareaColaborador;
import com.kaos.planificacion.entity.TareaColaboradorId;

/**
 * Repositorio para {@link TareaColaborador}.
 */
public interface TareaColaboradorRepository extends JpaRepository<TareaColaborador, TareaColaboradorId> {

    /**
     * Todos los colaboradores de una tarea.
     */
    List<TareaColaborador> findByTareaId(Long tareaId);

    /**
     * Todas las tareas en las que ha colaborado una persona.
     */
    List<TareaColaborador> findByPersonaId(Long personaId);

    /**
     * Colaborador concreto de una tarea.
     */
    Optional<TareaColaborador> findByTareaIdAndPersonaId(Long tareaId, Long personaId);

    /**
     * Comprueba si una persona ya colabora en una tarea.
     */
    boolean existsByTareaIdAndPersonaId(Long tareaId, Long personaId);

    /**
     * Elimina todos los colaboradores de una tarea (para re-importar limpio).
     */
    void deleteByTareaId(Long tareaId);

    /**
     * Suma de horas imputadas por una persona en todas las tareas de un sprint.
     */
    @Query("""
            SELECT COALESCE(SUM(tc.horasImputadas), 0)
            FROM TareaColaborador tc
            WHERE tc.persona.id = :personaId
              AND tc.tarea.sprint.id = :sprintId
            """)
    java.math.BigDecimal sumHorasByPersonaAndSprint(
            @Param("personaId") Long personaId,
            @Param("sprintId") Long sprintId
    );

    /**
     * Actualiza las horas imputadas de un colaborador, sumando el delta.
     */
    @Modifying
    @Query("""
            UPDATE TareaColaborador tc
            SET tc.horasImputadas = tc.horasImputadas + :deltaHoras
            WHERE tc.id.tareaId = :tareaId
              AND tc.id.personaId = :personaId
            """)
    void incrementarHoras(
            @Param("tareaId") Long tareaId,
            @Param("personaId") Long personaId,
            @Param("deltaHoras") java.math.BigDecimal deltaHoras
    );
}
