package com.kaos.calendario.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.kaos.calendario.entity.Ausencia;

/**
 * Repository para {@link Ausencia}.
 */
@Repository
public interface AusenciaRepository extends JpaRepository<Ausencia, Long> {

    /**
     * Busca ausencias por persona.
     */
    List<Ausencia> findByPersonaId(Long personaId);

    /**
     * Busca ausencias activas (sin fecha fin o con fecha fin >= fecha consulta).
     */
    @Query("SELECT a FROM Ausencia a WHERE a.persona.id = :personaId " +
           "AND (a.fechaFin IS NULL OR a.fechaFin >= :fecha)")
    List<Ausencia> findActivasByPersonaId(@Param("personaId") Long personaId, @Param("fecha") LocalDate fecha);

    /**
     * Busca ausencias de un squad en un rango de fechas.
     * Incluye ausencias que se solapan con el rango (fechaFin null = indefinida = siempre solapa).
     */
    @Query("SELECT a FROM Ausencia a " +
           "JOIN a.persona p " +
           "JOIN SquadMember sm ON sm.persona.id = p.id " +
           "WHERE sm.squad.id = :squadId " +
           "AND (CAST(:fechaInicio AS date) IS NULL OR a.fechaFin IS NULL OR a.fechaFin >= :fechaInicio) " +
           "AND (CAST(:fechaFin AS date) IS NULL OR a.fechaInicio <= :fechaFin)")
    List<Ausencia> findBySquadIdAndFechaRange(
            @Param("squadId") Long squadId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );
}
