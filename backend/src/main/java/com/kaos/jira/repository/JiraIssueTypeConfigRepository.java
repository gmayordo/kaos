package com.kaos.jira.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kaos.jira.entity.JiraIssueTypeConfig;

/**
 * Repositorio JPA para {@link JiraIssueTypeConfig}.
 */
@Repository
public interface JiraIssueTypeConfigRepository extends JpaRepository<JiraIssueTypeConfig, Long> {

    /**
     * Obtiene las configuraciones activas de un squad para un tipo de issue Jira específico.
     *
     * @param squadId  ID del squad
     * @param tipoJira tipo de issue en Jira (ej: "Sub-task")
     * @return lista de configuraciones activas ordenadas por id
     */
    List<JiraIssueTypeConfig> findBySquadIdAndTipoJiraAndActivaTrue(Long squadId, String tipoJira);

    /**
     * Obtiene todas las configuraciones activas de un squad.
     *
     * @param squadId ID del squad
     * @return lista de configuraciones activas
     */
    List<JiraIssueTypeConfig> findBySquadIdAndActivaTrue(Long squadId);
}
