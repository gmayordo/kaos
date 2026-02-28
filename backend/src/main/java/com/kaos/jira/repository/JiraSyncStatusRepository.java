package com.kaos.jira.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kaos.jira.entity.JiraSyncStatus;

/**
 * Repositorio para el estado de sincronización Jira por squad.
 *
 * <p>Un único registro por squad (UNIQUE constraint en squad_id).
 * Se usa findBySquadId + upsert manual (save) para actualizar el estado.</p>
 */
@Repository
public interface JiraSyncStatusRepository extends JpaRepository<JiraSyncStatus, Long> {

    /** Busca el estado de sync del squad. Devuelve vacío si nunca se ha sincronizado. */
    Optional<JiraSyncStatus> findBySquadId(Long squadId);
}
