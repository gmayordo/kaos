package com.kaos.jira.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kaos.jira.entity.JiraRemoteLink;

/**
 * Repositorio para {@link JiraRemoteLink}.
 *
 * <p>Proporciona acceso al cache local de remote links de Jira
 * con los métodos de búsqueda necesarios para la importación idempotente.</p>
 */
public interface JiraRemoteLinkRepository extends JpaRepository<JiraRemoteLink, Long> {

    /** Encuentra un remote link por su ID en Jira (upsert idempotente). */
    Optional<JiraRemoteLink> findByJiraLinkId(String jiraLinkId);

    /** Devuelve todos los remote links de una issue. */
    List<JiraRemoteLink> findByJiraIssueId(Long jiraIssueId);

    /** Cuenta remote links de una issue. */
    long countByJiraIssueId(Long jiraIssueId);
}
