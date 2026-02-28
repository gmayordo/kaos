package com.kaos.planificacion.repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.kaos.planificacion.entity.EstadoTarea;
import com.kaos.planificacion.entity.Tarea;

/**
 * Repositorio para operaciones sobre Tarea.
 */
@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {

    /**
     * Lista tareas de un sprint.
     */
    Page<Tarea> findBySprintId(Long sprintId, Pageable pageable);

    /**
     * Lista tareas de un sprint con filtro de persona.
     */
    Page<Tarea> findBySprintIdAndPersonaId(Long sprintId, Long personaId, Pageable pageable);

    /**
     * Lista tareas por estado.
     */
    Page<Tarea> findByEstado(EstadoTarea estado, Pageable pageable);

    /**
     * Lista tareas de un sprint por estado.
     */
    Page<Tarea> findBySprintIdAndEstado(Long sprintId, EstadoTarea estado, Pageable pageable);

    /**
     * Lista tareas de un sprint con filtros de persona y estado.
     */
    Page<Tarea> findBySprintIdAndPersonaIdAndEstado(Long sprintId, Long personaId, EstadoTarea estado, Pageable pageable);

    /**
     * Lista tareas de una persona.
     */
    Page<Tarea> findByPersonaId(Long personaId, Pageable pageable);

    /**
     * Suma horas asignadas a una persona en un día específico del sprint.
     */
    @Query("SELECT SUM(t.estimacion) FROM Tarea t " +
           "WHERE t.sprint.id = :sprintId " +
           "AND t.persona.id = :personaId " +
           "AND t.diaAsignado = :dia " +
           "AND t.estado != 'COMPLETADA'")
    BigDecimal sumEstimacionPorPersonaYDia(Long sprintId, Long personaId, Integer dia);

    /**
     * Lista tareas asignadas a una persona en un día específico.
     */
    @Query("SELECT t FROM Tarea t " +
           "WHERE t.sprint.id = :sprintId " +
           "AND t.persona.id = :personaId " +
           "AND t.diaAsignado = :dia")
    List<Tarea> findTareasPorPersonaYDia(Long sprintId, Long personaId, Integer dia);

    /**
     * Cuenta tareas no completadas de un sprint.
     */
    @Query("SELECT COUNT(t) FROM Tarea t " +
           "WHERE t.sprint.id = :sprintId " +
           "AND t.estado != 'COMPLETADA'")
    int countTareasActivas(Long sprintId);

    /**
     * Suma horas estimadas por sprint.
     */
    @Query("SELECT SUM(t.estimacion) FROM Tarea t WHERE t.sprint.id = :sprintId")
    BigDecimal sumEstimacionPorSprint(Long sprintId);

    /**
     * Suma horas estimadas por sprint y categoría.
     */
    @Query("SELECT SUM(t.estimacion) FROM Tarea t " +
           "WHERE t.sprint.id = :sprintId " +
           "AND t.categoria = :categoria")
    BigDecimal sumEstimacionPorSprintYCategoria(Long sprintId, String categoria);

    /**
     * Cuenta todas las tareas de un sprint.
     */
    Long countBySprintId(Long sprintId);

    /**
     * Cuenta tareas de un sprint con estado específico.
     */
    Long countBySprintIdAndEstado(Long sprintId, EstadoTarea estado);

    /**
     * Todas las tareas de un sprint sin paginación.
     * Usado por el motor de alertas SpEL para cargar el contexto completo.
     *
     * @param sprintId ID del sprint
     * @return lista de tareas del sprint
     */
    List<Tarea> findAllBySprintId(Long sprintId);

    /**
     * Tareas de un sprint vinculadas a una issue Jira (jiraKey no nulo).
     * Usado para construir el mapa jiraKey→Tarea en el motor de alertas.
     *
     * @param sprintId ID del sprint
     * @return tareas con jiraKey asignado
     */
    @Query("""
            SELECT t FROM Tarea t
            WHERE t.sprint.id = :sprintId
              AND t.jiraKey IS NOT NULL
            """)
    List<Tarea> findBySprintIdWithJiraKey(
            @org.springframework.data.repository.query.Param("sprintId") Long sprintId);

    /**
     * Busca una tarea por su jira_key vinculado.
     * Usado para enlazar jerarquía tareaParent al importar subtareas.
     */
    java.util.Optional<Tarea> findByJiraKey(String jiraKey);
}
