package com.kaos.jira.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.kaos.jira.entity.JiraSyncQueue;
import com.kaos.jira.entity.JiraSyncQueue.EstadoOperacion;
import com.kaos.jira.entity.JiraSyncQueue.TipoOperacion;

/**
 * Repositorio para la cola de operaciones Jira pendientes.
 *
 * <p>Gestiona el ciclo de vida de las operaciones encoladas: consulta de
 * pendientes, actualización de estado y limpieza de entradas antiguas.</p>
 */
@Repository
public interface JiraSyncQueueRepository extends JpaRepository<JiraSyncQueue, Long> {

    /**
     * Devuelve las operaciones de un squad en un estado dado, ordenadas por fecha
     * de creación (FIFO) y filtrando por {@code programadaPara} <= ahora.
     */
    @Query("""
            SELECT q FROM JiraSyncQueue q
            WHERE q.squad.id = :squadId
              AND q.estado = :estado
              AND (q.programadaPara IS NULL OR q.programadaPara <= :ahora)
            ORDER BY q.createdAt ASC
            """)
    List<JiraSyncQueue> findPendientesBySquad(
            @Param("squadId") Long squadId,
            @Param("estado") EstadoOperacion estado,
            @Param("ahora") LocalDateTime ahora);

    /**
     * Devuelve todas las operaciones pendientes de todos los squads listas para
     * ejecutar (para el batch scheduler).
     */
    @Query("""
            SELECT q FROM JiraSyncQueue q
            WHERE q.estado = :estado
              AND (q.programadaPara IS NULL OR q.programadaPara <= :ahora)
            ORDER BY q.createdAt ASC
            """)
    List<JiraSyncQueue> findAllPendientes(
            @Param("estado") EstadoOperacion estado,
            @Param("ahora") LocalDateTime ahora);

    /** Cuenta las operaciones PENDIENTE de un squad. */
    long countBySquadIdAndEstado(Long squadId, EstadoOperacion estado);

    /** Búsqueda por squad y tipo de operación (para deduplicar encoles). */
    Optional<JiraSyncQueue> findBySquadIdAndTipoOperacionAndEstado(
            Long squadId,
            TipoOperacion tipoOperacion,
            EstadoOperacion estado);

    /** Devuelve todas las operaciones de un squad ordenadas por fecha de creación. */
    List<JiraSyncQueue> findBySquadIdOrderByCreatedAtDesc(Long squadId);

    /** Limpia operaciones completadas más antiguas que la fecha indicada. */
    @Modifying
    @Query("DELETE FROM JiraSyncQueue q WHERE q.estado = :estado AND q.ejecutadaAt < :antes")
    int limpiarCompletadas(
            @Param("antes") LocalDateTime antes,
            @Param("estado") EstadoOperacion estado);
}
