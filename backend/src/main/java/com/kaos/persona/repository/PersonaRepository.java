package com.kaos.persona.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.entity.Rol;
import com.kaos.persona.entity.Seniority;

/**
 * Repositorio JPA para {@link Persona}.
 * Usa JpaSpecificationExecutor para filtros dinámicos.
 */
@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long>, JpaSpecificationExecutor<Persona> {

    java.util.Optional<Persona> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByIdJira(String idJira);

    boolean existsByIdJiraAndIdNot(String idJira, Long id);

    boolean existsByPerfilHorarioId(Long perfilHorarioId);

    /**
     * Busca personas con filtros dinámicos.
     * Los filtros nulos son ignorados.
     */
    @Query("""
            SELECT DISTINCT p FROM Persona p
            LEFT JOIN SquadMember sm ON sm.persona.id = p.id
            LEFT JOIN p.perfilHorario ph
            WHERE (:squadId IS NULL OR sm.squad.id = :squadId)
            AND (:rol IS NULL OR sm.rol = :rol)
            AND (:seniority IS NULL OR p.seniority = :seniority)
            AND (:ubicacion IS NULL OR LOWER(ph.zonaHoraria) LIKE LOWER(CONCAT('%', CAST(:ubicacion AS string), '%')))
            AND (:activo IS NULL OR p.activo = :activo)
            """)
    Page<Persona> findWithFilters(
            @Param("squadId") Long squadId,
            @Param("rol") Rol rol,
            @Param("seniority") Seniority seniority,
            @Param("ubicacion") String ubicacion,
            @Param("activo") Boolean activo,
            Pageable pageable
    );

    /** Búsqueda exacta por nombre, sin distinción de mayúsculas. */
    java.util.Optional<Persona> findByNombreIgnoreCase(String nombre);

    /** Búsqueda parcial por nombre, sin distinción de mayúsculas. */
    java.util.List<Persona> findByNombreContainingIgnoreCase(String nombre);
}
