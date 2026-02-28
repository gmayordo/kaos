package com.kaos.jira.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.kaos.jira.entity.JiraIssue;

/**
 * Repositorio para {@link JiraIssue}.
 *
 * <p>Proporciona acceso al cache local de issues de Jira con los métodos
 * de búsqueda más habituales para la sincronización e importación.</p>
 */
public interface JiraIssueRepository extends JpaRepository<JiraIssue, Long> {

    /**
     * Encuentra un issue por su clave Jira única (ej: PROJ-123).
     * Es el principal identificador para la importación idempotente.
     */
    Optional<JiraIssue> findByJiraKey(String jiraKey);

    /**
     * Comprueba si existe una issue con esa clave Jira.
     * Usado en el upsert de JiraImportService.
     */
    boolean existsByJiraKey(String jiraKey);

    /**
     * Devuelve todas las issues de un sprint (para construir el estado del sprint).
     */
    List<JiraIssue> findBySprintId(Long sprintId);

    /**
     * Devuelve las sub-issues (sub-tasks) de un issue padre.
     * Usado para reconstruir la jerarquía HU → Subtareas.
     */
    List<JiraIssue> findByParentKey(String parentKey);

    /**
     * Devuelve todas las issues de un squad (sin filtro de estado).
     */
    List<JiraIssue> findBySquadId(Long squadId);

    /**
     * Issues de un squad en un estado KAOS concreto.
     * Útil para contar tareas por estado en el dashboard.
     */
    List<JiraIssue> findBySquadIdAndEstadoKaos(Long squadId, String estadoKaos);

    /**
     * Issues asignadas a una persona KAOS en un sprint.
     */
    List<JiraIssue> findByPersonaIdAndSprintId(Long personaId, Long sprintId);

    /**
     * Suma de horas consumidas de las issues de un sprint.
     * Usado para el cálculo de ocupación del dashboard.
     */
    @Query("SELECT COALESCE(SUM(i.horasConsumidas), 0) FROM JiraIssue i WHERE i.sprint.id = :sprintId")
    Double sumHorasConsumidasBySprintId(@Param("sprintId") Long sprintId);

    /**
     * Suma de horas estimadas de las issues de un sprint.
     */
    @Query("SELECT COALESCE(SUM(i.estimacionHoras), 0) FROM JiraIssue i WHERE i.sprint.id = :sprintId")
    Double sumEstimacionHorasBySprintId(@Param("sprintId") Long sprintId);

    // ── Queries para planificación (Bloque 5) ────────────────────────────────

    /**
     * Issues de un squad en un sprint concreto.
     * Usado para mostrar el backlog del sprint en la pantalla de planificación.
     */
    List<JiraIssue> findBySquadIdAndSprintId(Long squadId, Long sprintId);

    /**
     * Issues de un squad que aún no tienen Tarea KAOS asociada.
     * Estas son las "planificables" independientemente del sprint.
     */
    @Query("SELECT i FROM JiraIssue i WHERE i.squad.id = :squadId AND i.tarea IS NULL ORDER BY i.tipoJira, i.jiraKey")
    List<JiraIssue> findBySquadIdAndTareaIsNull(@Param("squadId") Long squadId);

    /**
     * Issues de un squad en un sprint que aún no tienen Tarea KAOS asociada.
     * Permite mostrar solo las issues pendientes de planificar en el sprint activo.
     */
    @Query("SELECT i FROM JiraIssue i WHERE i.squad.id = :squadId AND i.sprint.id = :sprintId AND i.tarea IS NULL ORDER BY i.tipoJira, i.jiraKey")
    List<JiraIssue> findBySquadIdAndSprintIdAndTareaIsNull(@Param("squadId") Long squadId, @Param("sprintId") Long sprintId);

    /**
     * Busca issues por una lista de claves Jira.
     * Usado en el endpoint de planificación para validar y cargar los issues a planificar.
     */
    List<JiraIssue> findByJiraKeyIn(List<String> jiraKeys);
}
