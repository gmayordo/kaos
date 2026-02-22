package com.kaos.planificacion.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;

/**
 * Repositorio para operaciones sobre Sprint.
 */
@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    /**
     * Valida si hay solapamiento de sprints para un squad.
     * Devuelve true si hay un sprint activo o en planificación que se solape.
     */
    @Query("SELECT COUNT(s) > 0 FROM Sprint s " +
           "WHERE s.squad.id = :squadId " +
           "AND s.id != :sprintId " +
           "AND s.estado != 'CERRADO' " +
           "AND s.fechaInicio <= :fechaFin " +
           "AND s.fechaFin >= :fechaInicio")
    boolean existsSolapamiento(Long squadId, LocalDate fechaInicio, LocalDate fechaFin, Long sprintId);

    /**
     * Lista sprints por squad y estado.
     */
    Page<Sprint> findBySquadIdAndEstado(Long squadId, SprintEstado estado, Pageable pageable);

    /**
     * Lista sprints por squad.
     */
    Page<Sprint> findBySquadId(Long squadId, Pageable pageable);

    /**
     * Lista sprints por estado.
     */
    Page<Sprint> findByEstado(SprintEstado estado, Pageable pageable);

    /**
     * Lista sprints activos para un squad.
     */
    List<Sprint> findBySquadIdAndEstado(Long squadId, SprintEstado estado);

    /**
     * Lista sprints por nombre y rango exacto de fechas.
     */
    List<Sprint> findByNombreAndFechaInicioAndFechaFin(
            String nombre,
            LocalDate fechaInicio,
            LocalDate fechaFin);
}
