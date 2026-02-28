package com.kaos.jira.alert.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.kaos.jira.alert.entity.JiraAlertRule;

/**
 * Repositorio para {@link JiraAlertRule}.
 *
 * <p>Proporciona acceso a las reglas de coherencia configuradas en BD,
 * filtrando por squad y estado activo para el motor SpEL.</p>
 */
public interface JiraAlertRuleRepository extends JpaRepository<JiraAlertRule, Long> {

    /**
     * Reglas activas aplicables a un squad: las específicas del squad más las globales (squad null).
     * Ordenadas por tipo y severidad para ejecución reproducible.
     *
     * @param squadId ID del squad
     * @return reglas activas ordenadas
     */
    @Query("""
            SELECT r FROM JiraAlertRule r
            WHERE r.activa = true
              AND (r.squad.id = :squadId OR r.squad IS NULL)
            ORDER BY r.tipo ASC, r.severidad DESC
            """)
    List<JiraAlertRule> findActivasBySquadIdOrGlobal(@Param("squadId") Long squadId);

    /**
     * Todas las reglas activas globales (sin squad específico).
     *
     * @return reglas globales activas
     */
    @Query("""
            SELECT r FROM JiraAlertRule r
            WHERE r.activa = true AND r.squad IS NULL
            ORDER BY r.tipo ASC, r.severidad DESC
            """)
    List<JiraAlertRule> findActivasGlobales();

    /**
     * Busca una regla por su nombre único.
     *
     * @param nombre identificador corto de la regla
     * @return optional con la regla si existe
     */
    Optional<JiraAlertRule> findByNombre(String nombre);

    /**
     * Todas las reglas de un tipo concreto, independientemente de su estado.
     *
     * @param tipo tipo lógico de alerta
     * @return reglas del tipo indicado
     */
    List<JiraAlertRule> findByTipo(JiraAlertRule.TipoAlerta tipo);
}
