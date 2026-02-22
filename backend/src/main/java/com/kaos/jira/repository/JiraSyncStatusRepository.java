package com.kaos.jira.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kaos.jira.entity.JiraSyncStatus;

/**
 * Repositorio para operaciones sobre JiraSyncStatus.
 */
@Repository
public interface JiraSyncStatusRepository extends JpaRepository<JiraSyncStatus, Long> {

    /**
     * Busca el estado de sincronización de un proyecto por su clave.
     *
     * @param projectKey clave del proyecto Jira
     * @return Optional con el estado, vacío si nunca se ha sincronizado
     */
    Optional<JiraSyncStatus> findByProjectKey(String projectKey);
}
