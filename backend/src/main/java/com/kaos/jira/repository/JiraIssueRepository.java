package com.kaos.jira.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kaos.jira.entity.JiraIssue;

/**
 * Repositorio para operaciones sobre JiraIssue.
 */
@Repository
public interface JiraIssueRepository extends JpaRepository<JiraIssue, Long> {

    /**
     * Busca un issue por su clave Jira.
     */
    Optional<JiraIssue> findByJiraKey(String jiraKey);
}
