package com.kaos.jira.alert.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.kaos.jira.alert.entity.JiraAlertRule.Severidad;
import com.kaos.jira.alert.entity.JiraAlerta;

/**
 * Repositorio para {@link JiraAlerta}.
 *
 * <p>Permite consultar alertas por sprint, squad, severidad y estado de resolución,
 * así como marcarlas masivamente como notificadas por email.</p>
 */
public interface JiraAlertaRepository extends JpaRepository<JiraAlerta, Long> {

    /**
     * Alertas de un sprint, opcionalmente filtrando por estado de resolución.
     *
     * @param sprintId  ID del sprint
     * @param resuelta  true=solo resueltas, false=solo pendientes, null=todas
     * @param pageable  paginación
     * @return página de alertas
     */
    @Query("""
            SELECT a FROM JiraAlerta a
            WHERE a.sprint.id = :sprintId
              AND (:resuelta IS NULL OR a.resuelta = :resuelta)
            ORDER BY
              CASE a.severidad
                WHEN 'CRITICO' THEN 1
                WHEN 'AVISO'   THEN 2
                ELSE                3
              END ASC,
              a.createdAt DESC
            """)
    Page<JiraAlerta> findBySprintId(
            @Param("sprintId") Long sprintId,
            @Param("resuelta") Boolean resuelta,
            Pageable pageable);

    /**
     * Alertas de un squad, opcionalmente filtrando por resolución.
     *
     * @param squadId  ID del squad
     * @param resuelta filtro de resolución (nullable)
     * @return lista de alertas
     */
    @Query("""
            SELECT a FROM JiraAlerta a
            WHERE a.squad.id = :squadId
              AND (:resuelta IS NULL OR a.resuelta = :resuelta)
            ORDER BY a.createdAt DESC
            """)
    List<JiraAlerta> findBySquadId(
            @Param("squadId") Long squadId,
            @Param("resuelta") Boolean resuelta);

    /**
     * Alertas críticas no resueltas de un sprint — usadas en el resumen email.
     *
     * @param sprintId  ID del sprint
     * @param severidad severidad buscada
     * @return lista de alertas
     */
    @Query("""
            SELECT a FROM JiraAlerta a
            WHERE a.sprint.id = :sprintId
              AND a.severidad = :severidad
              AND a.resuelta = false
            ORDER BY a.createdAt DESC
            """)
    List<JiraAlerta> findPendientesBySeveridad(
            @Param("sprintId") Long sprintId,
            @Param("severidad") Severidad severidad);

    /**
     * Alertas pendientes de notificación por email tras la última sync.
     *
     * @param sprintId ID del sprint
     * @return alertas no notificadas aún por email
     */
    @Query("""
            SELECT a FROM JiraAlerta a
            WHERE a.sprint.id = :sprintId
              AND a.notificadaEmail = false
            ORDER BY
              CASE a.severidad
                WHEN 'CRITICO' THEN 1
                WHEN 'AVISO'   THEN 2
                ELSE                3
              END ASC
            """)
    List<JiraAlerta> findPendientesEmail(@Param("sprintId") Long sprintId);

    /**
     * Número de alertas no resueltas de un sprint.
     *
     * @param sprintId ID del sprint
     * @return número de alertas pendientes
     */
    long countBySprintIdAndResueltaFalse(Long sprintId);

    /**
     * Marca todas las alertas no notificadas de un sprint como notificadas por email.
     *
     * @param sprintId ID del sprint
     * @return número de registros actualizados
     */
    @Modifying
    @Query("""
            UPDATE JiraAlerta a
            SET a.notificadaEmail = true
            WHERE a.sprint.id = :sprintId AND a.notificadaEmail = false
            """)
    int marcarNotificadasEmail(@Param("sprintId") Long sprintId);
}
