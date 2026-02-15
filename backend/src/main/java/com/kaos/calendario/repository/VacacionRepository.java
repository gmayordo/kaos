package com.kaos.calendario.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.kaos.calendario.entity.EstadoVacacion;
import com.kaos.calendario.entity.TipoVacacion;
import com.kaos.calendario.entity.Vacacion;

/**
 * Repositorio para {@link Vacacion}.
 */
@Repository
public interface VacacionRepository extends JpaRepository<Vacacion, Long> {

    /**
     * Busca vacaciones por persona.
     */
    List<Vacacion> findByPersonaId(Long personaId);

    /**
     * Busca vacaciones por persona, tipo y estado.
     */
    List<Vacacion> findByPersonaIdAndTipoAndEstado(Long personaId, TipoVacacion tipo, EstadoVacacion estado);

    /**
     * Busca vacaciones por persona en un rango de fechas.
     */
    @Query("SELECT v FROM Vacacion v WHERE v.persona.id = :personaId " +
           "AND (:fechaInicio IS NULL OR v.fechaFin >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR v.fechaInicio <= :fechaFin)")
    List<Vacacion> findByPersonaIdAndFechaRange(
            @Param("personaId") Long personaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    /**
     * Busca vacaciones de un squad en un rango de fechas.
     */
    @Query("SELECT v FROM Vacacion v JOIN SquadMember sm ON v.persona.id = sm.persona.id " +
           "WHERE sm.squad.id = :squadId " +
           "AND (CAST(:fechaInicio AS date) IS NULL OR v.fechaFin >= :fechaInicio) " +
           "AND (CAST(:fechaFin AS date) IS NULL OR v.fechaInicio <= :fechaFin)")
    List<Vacacion> findBySquadIdAndFechaRange(
            @Param("squadId") Long squadId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    /**
     * Verifica si existe solapamiento de vacaciones para una persona.
     * Retorna true si hay alguna vacación que se solape con el rango dado.
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN TRUE ELSE FALSE END FROM Vacacion v " +
           "WHERE v.persona.id = :personaId " +
           "AND v.id != :vacacionId " +
           "AND v.fechaInicio <= :fechaFin " +
           "AND v.fechaFin >= :fechaInicio")
    boolean existsSolapamiento(
            @Param("personaId") Long personaId,
            @Param("vacacionId") Long vacacionId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );
}
