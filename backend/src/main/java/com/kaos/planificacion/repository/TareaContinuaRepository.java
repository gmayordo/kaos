package com.kaos.planificacion.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.kaos.planificacion.entity.TareaContinua;

/**
 * Repositorio para operaciones sobre TareaContinua.
 */
@Repository
public interface TareaContinuaRepository extends JpaRepository<TareaContinua, Long> {

    /**
     * Lista tareas continuas activas de un squad.
     */
    List<TareaContinua> findBySquadIdAndActivaTrue(Long squadId);

    /**
     * Lista tareas continuas activas de un squad cuyas fechas se solapan con el rango dado.
     * Una tarea se solapa si: fecha_inicio <= rangoFin AND (fecha_fin IS NULL OR fecha_fin >= rangoInicio)
     */
    @Query("""
            SELECT tc FROM TareaContinua tc
            WHERE tc.squad.id = :squadId
              AND tc.activa = true
              AND tc.fechaInicio <= :rangoFin
              AND (tc.fechaFin IS NULL OR tc.fechaFin >= :rangoInicio)
            """)
    List<TareaContinua> findActivasEnRango(
            @org.springframework.data.repository.query.Param("squadId") Long squadId,
            @org.springframework.data.repository.query.Param("rangoInicio") LocalDate rangoInicio,
            @org.springframework.data.repository.query.Param("rangoFin") LocalDate rangoFin);
}
