package com.kaos.jira.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kaos.jira.entity.JiraComment;

/**
 * Repositorio para {@link JiraComment}.
 *
 * <p>Proporciona acceso al cache local de comentarios de Jira
 * con los métodos de búsqueda necesarios para la importación idempotente.</p>
 */
public interface JiraCommentRepository extends JpaRepository<JiraComment, Long> {

    /** Encuentra un comentario por su ID en Jira (upsert idempotente). */
    Optional<JiraComment> findByJiraCommentId(String jiraCommentId);

    /** Devuelve todos los comentarios de una issue. */
    List<JiraComment> findByJiraIssueId(Long jiraIssueId);

    /** Cuenta comentarios de una issue. */
    long countByJiraIssueId(Long jiraIssueId);
}
