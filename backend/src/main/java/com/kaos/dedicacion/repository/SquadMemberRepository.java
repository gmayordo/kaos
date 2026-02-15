package com.kaos.dedicacion.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.kaos.dedicacion.entity.SquadMember;

/**
 * Repositorio JPA para {@link SquadMember}.
 */
@Repository
public interface SquadMemberRepository extends JpaRepository<SquadMember, Long> {

    /**
     * Lista miembros activos de un squad.
     */
    List<SquadMember> findBySquadId(Long squadId);

    /**
     * Lista asignaciones de una persona.
     */
    List<SquadMember> findByPersonaId(Long personaId);

    /**
     * Verifica si existe una asignación persona-squad.
     */
    boolean existsByPersonaIdAndSquadId(Long personaId, Long squadId);

    /**
     * Verifica si existe una asignación persona-squad excluyendo un registro.
     */
    boolean existsByPersonaIdAndSquadIdAndIdNot(Long personaId, Long squadId, Long excludeId);

    /**
     * Suma el porcentaje total de dedicación de una persona en todas sus asignaciones activas.
     * Excluye opcionalmente un registro (para updates).
     *
     * @param personaId ID de la persona
     * @param excludeId ID a excluir (null para incluir todos)
     * @return suma de porcentajes (null si no hay registros)
     */
    @Query("""
            SELECT COALESCE(SUM(sm.porcentaje), 0)
            FROM SquadMember sm
            WHERE sm.persona.id = :personaId
            AND (sm.fechaFin IS NULL OR sm.fechaFin >= CURRENT_DATE)
            AND (:excludeId IS NULL OR sm.id != :excludeId)
            """)
    int sumPorcentajeByPersonaId(@Param("personaId") Long personaId,
                                  @Param("excludeId") Long excludeId);
}
