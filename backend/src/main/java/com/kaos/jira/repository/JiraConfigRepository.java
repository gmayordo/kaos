package com.kaos.jira.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kaos.jira.entity.JiraConfig;

/**
 * Repositorio JPA para {@link JiraConfig}.
 */
@Repository
public interface JiraConfigRepository extends JpaRepository<JiraConfig, Long> {

    /**
     * Busca la configuración Jira activa para un squad concreto.
     *
     * @param squadId identificador del squad
     * @return configuración Jira si existe
     */
    Optional<JiraConfig> findBySquadIdAndActivaTrue(Long squadId);

    /**
     * Devuelve todas las configuraciones Jira activas.
     * Usado por el batch scheduler para sincronizar todos los squads.
     *
     * @return lista de configuraciones activas
     */
    List<JiraConfig> findAllByActivaTrue();

    /**
     * Comprueba si existe configuración activa para un squad.
     *
     * @param squadId identificador del squad
     * @return true si existe configuración activa
     */
    boolean existsBySquadIdAndActivaTrue(Long squadId);

    /**
     * Busca la configuración Jira de un squad (activa o inactiva).
     * Útil al actualizar para no perder configuraciones inactivas.
     *
     * @param squadId identificador del squad
     * @return configuración Jira si existe
     */
    Optional<JiraConfig> findBySquadId(Long squadId);
}
