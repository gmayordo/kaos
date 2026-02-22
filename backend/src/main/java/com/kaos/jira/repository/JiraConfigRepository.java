package com.kaos.jira.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kaos.jira.entity.JiraConfig;

/**
 * Repositorio para operaciones sobre JiraConfig.
 */
@Repository
public interface JiraConfigRepository extends JpaRepository<JiraConfig, Long> {

    /**
     * Busca todas las configuraciones activas para un squad.
     */
    List<JiraConfig> findBySquadId(Long squadId);
}
