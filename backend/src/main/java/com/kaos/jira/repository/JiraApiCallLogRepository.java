package com.kaos.jira.repository;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.kaos.jira.entity.JiraApiCallLog;

/**
 * Repositorio JPA para {@link JiraApiCallLog}.
 * Proporciona las queries necesarias para el rate limiting de la API Jira.
 */
@Repository
public interface JiraApiCallLogRepository extends JpaRepository<JiraApiCallLog, Long> {

    /**
     * Cuenta las llamadas realizadas desde un momento dado hasta ahora.
     * Usado para controlar la ventana de 200 llamadas cada 2 horas.
     *
     * <p>Ejemplo de uso:
     * <pre>
     *   long calls = repo.countCallsSince(LocalDateTime.now().minusHours(2));
     * </pre>
     *
     * @param desde límite inferior de la ventana temporal
     * @return número de llamadas en el periodo
     */
    @Query("SELECT COUNT(c) FROM JiraApiCallLog c WHERE c.executedAt >= :desde")
    long countCallsSince(@Param("desde") LocalDateTime desde);

    /**
     * Cuenta las llamadas de un squad concreto en la ventana temporal.
     *
     * @param squadId identificador del squad
     * @param desde   límite inferior de la ventana temporal
     * @return número de llamadas del squad en el periodo
     */
    @Query("SELECT COUNT(c) FROM JiraApiCallLog c WHERE c.squadId = :squadId AND c.executedAt >= :desde")
    long countCallsBySquadSince(@Param("squadId") Long squadId, @Param("desde") LocalDateTime desde);

    /**
     * Elimina registros anteriores a un timestamp.
     * Para purgar logs antiguos y mantener la tabla ligera.
     *
     * @param antes límite superior: se borran registros con executedAt anterior a este valor
     */
    void deleteByExecutedAtBefore(LocalDateTime antes);
}
