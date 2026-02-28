package com.kaos.planificacion.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kaos.planificacion.entity.PlantillaAsignacion;

/**
 * Repositorio para {@link PlantillaAsignacion}.
 */
public interface PlantillaAsignacionRepository extends JpaRepository<PlantillaAsignacion, Long> {

    /**
     * Busca la plantilla activa para un tipo Jira dado.
     * Usada en la aplicación automática durante la planificación.
     */
    Optional<PlantillaAsignacion> findFirstByTipoJiraIgnoreCaseAndActivoTrue(String tipoJira);

    /**
     * Lista todas las plantillas activas.
     */
    List<PlantillaAsignacion> findAllByActivoTrue();
}
