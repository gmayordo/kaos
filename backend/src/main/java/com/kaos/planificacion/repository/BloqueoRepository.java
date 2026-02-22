package com.kaos.planificacion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.kaos.planificacion.entity.Bloqueo;
import com.kaos.planificacion.entity.EstadoBloqueo;

/**
 * Repositorio para operaciones sobre Bloqueo.
 */
@Repository
public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    /**
     * Lista bloqueos por estado.
     */
    Page<Bloqueo> findByEstado(EstadoBloqueo estado, Pageable pageable);

    /**
     * Cuenta bloqueos abiertos.
     */
    @Query("SELECT COUNT(b) FROM Bloqueo b WHERE b.estado = 'ABIERTO'")
    int countBloqueosAbiertos();

    /**
     * Cuenta bloqueos abiertos en gestión.
     */
    @Query("SELECT COUNT(b) FROM Bloqueo b WHERE b.estado IN ('ABIERTO', 'EN_GESTION')")
    int countBloqueosActivos();

    /**
     * Cuenta bloqueos con estado ABIERTO o EN_GESTION (alias para countBloqueosActivos).
     */
    @Query("SELECT COUNT(b) FROM Bloqueo b WHERE b.estado IN ('ABIERTO', 'EN_GESTION')")
    Long countByEstadoAbiertosOEnGestion();
}
