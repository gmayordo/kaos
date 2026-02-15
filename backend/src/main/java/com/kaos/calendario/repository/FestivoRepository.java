package com.kaos.calendario.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.kaos.calendario.entity.Festivo;
import com.kaos.calendario.entity.TipoFestivo;

/**
 * Repositorio para {@link Festivo}.
 * Festivos están vinculados a ciudad (calendario laboral por ubicación).
 */
@Repository
public interface FestivoRepository extends JpaRepository<Festivo, Long> {

    /**
     * Busca festivos por año.
     */
    @Query("SELECT f FROM Festivo f WHERE YEAR(f.fecha) = :anio")
    List<Festivo> findByAnio(@Param("anio") int anio);

    /**
     * Busca festivos por tipo.
     */
    List<Festivo> findByTipo(TipoFestivo tipo);

    /**
     * Busca festivos por año y tipo.
     */
    @Query("SELECT f FROM Festivo f WHERE YEAR(f.fecha) = :anio AND f.tipo = :tipo")
    List<Festivo> findByAnioAndTipo(@Param("anio") int anio, @Param("tipo") TipoFestivo tipo);

    /**
     * Busca festivos de una ciudad en un rango de fechas.
     */
    @Query("SELECT f FROM Festivo f WHERE f.ciudad = :ciudad " +
           "AND (:fechaInicio IS NULL OR f.fecha >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR f.fecha <= :fechaFin)")
    List<Festivo> findByCiudadAndFechaRange(
            @Param("ciudad") String ciudad,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    /**
     * Verifica si ya existe un festivo con fecha, descripción y ciudad.
     */
    boolean existsByFechaAndDescripcionAndCiudad(LocalDate fecha, String descripcion, String ciudad);
}
