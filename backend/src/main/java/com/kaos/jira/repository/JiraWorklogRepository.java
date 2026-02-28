package com.kaos.jira.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kaos.jira.entity.JiraWorklog;
import com.kaos.jira.entity.WorklogOrigen;

/**
 * Repositorio para {@link JiraWorklog}.
 *
 * <p>Proporciona acceso al cache de imputaciones con los métodos necesarios
 * para las vistas de Mi Día, Mi Semana y el motor de alertas.</p>
 */
public interface JiraWorklogRepository extends JpaRepository<JiraWorklog, Long> {

    /**
     * Encuentra un worklog por su ID externo de Jira.
     * Usado para la importación idempotente (upsert por jiraWorklogId).
     */
    Optional<JiraWorklog> findByJiraWorklogId(String jiraWorklogId);

    /**
     * Comprueba si existe un worklog con ese ID de Jira.
     */
    boolean existsByJiraWorklogId(String jiraWorklogId);

    /**
     * Todos los worklogs de una issue KAOS.
     * Usado para construir el registro de tiempos de una issue.
     */
    List<JiraWorklog> findByJiraIssueId(Long jiraIssueId);

    /**
     * Worklogs de una persona en un rango de fechas.
     * Base para las vistas Mi Día y Mi Semana.
     *
     * @param personaId ID de la persona KAOS
     * @param desde     fecha de inicio (inclusive)
     * @param hasta     fecha de fin (inclusive)
     */
    @Query("""
            SELECT w FROM JiraWorklog w
            WHERE w.persona.id = :personaId
              AND w.fecha BETWEEN :desde AND :hasta
            ORDER BY w.fecha ASC, w.jiraIssue.jiraKey ASC
            """)
    List<JiraWorklog> findByPersonaAndFechaRange(
            @Param("personaId") Long personaId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    /**
     * Worklogs de una persona en una fecha concreta (Mi Día).
     */
    List<JiraWorklog> findByPersonaIdAndFecha(Long personaId, LocalDate fecha);

    /**
     * Suma de horas imputadas por una persona en un sprint.
     * Usado para calcular la ocupación real del sprint en el dashboard.
     *
     * @param personaId ID de la persona KAOS
     * @param sprintId  ID del sprint KAOS
     */
    @Query("""
            SELECT COALESCE(SUM(w.horas), 0)
            FROM JiraWorklog w
            WHERE w.persona.id = :personaId
              AND w.jiraIssue.sprint.id = :sprintId
            """)
    BigDecimal sumHorasByPersonaAndSprint(
            @Param("personaId") Long personaId,
            @Param("sprintId") Long sprintId
    );

    /**
     * Suma total de horas imputadas en un sprint (todos los desarrolladores).
     */
    @Query("""
            SELECT COALESCE(SUM(w.horas), 0)
            FROM JiraWorklog w
            WHERE w.jiraIssue.sprint.id = :sprintId
            """)
    BigDecimal sumHorasBySprintId(@Param("sprintId") Long sprintId);

    /**
     * Worklogs pendientes de sincronizar con Jira (origen=KAOS, sincronizado=false).
     */
    List<JiraWorklog> findByOrigenAndSincronizado(WorklogOrigen origen, boolean sincronizado);

    /**
     * Todos los worklogs de una issue identificada por su jiraKey.
     * Usado para el endpoint GET /worklogs/issue/{jiraKey}.
     */
    @Query("""
            SELECT w FROM JiraWorklog w
            WHERE w.jiraIssue.jiraKey = :jiraKey
            ORDER BY w.fecha ASC, w.persona.nombre ASC
            """)
    List<JiraWorklog> findByJiraIssueJiraKey(@Param("jiraKey") String jiraKey);

    /**
     * Suma de horas imputadas por una persona en un día concreto.
     * Usado para validar que no se supera la capacidad diaria.
     */
    @Query("""
            SELECT COALESCE(SUM(w.horas), 0)
            FROM JiraWorklog w
            WHERE w.persona.id = :personaId
              AND w.fecha = :fecha
            """)
    BigDecimal sumHorasByPersonaAndFecha(
            @Param("personaId") Long personaId,
            @Param("fecha") LocalDate fecha
    );
}
