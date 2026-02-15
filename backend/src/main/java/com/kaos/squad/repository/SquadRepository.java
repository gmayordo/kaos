package com.kaos.squad.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kaos.squad.entity.EstadoSquad;
import com.kaos.squad.entity.Squad;

/**
 * Repositorio JPA para {@link Squad}.
 */
@Repository
public interface SquadRepository extends JpaRepository<Squad, Long> {

    List<Squad> findByEstado(EstadoSquad estado);

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
