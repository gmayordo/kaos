package com.kaos.horario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kaos.horario.entity.PerfilHorario;

/**
 * Repositorio JPA para {@link PerfilHorario}.
 */
@Repository
public interface PerfilHorarioRepository extends JpaRepository<PerfilHorario, Long> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
