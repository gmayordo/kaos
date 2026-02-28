package com.kaos.planificacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kaos.planificacion.entity.TareaDependencia;

/**
 * Repositorio para {@link TareaDependencia}.
 */
public interface TareaDependenciaRepository extends JpaRepository<TareaDependencia, Long> {

    /**
     * Dependencias que salen de una tarea (tarea bloquea a otras).
     */
    List<TareaDependencia> findByTareaOrigenId(Long tareaOrigenId);

    /**
     * Dependencias que llegan a una tarea (tarea es bloqueada por otras).
     */
    List<TareaDependencia> findByTareaDestinoId(Long tareaDestinoId);

    /**
     * Comprueba si ya existe la dependencia directa entre dos tareas.
     * Usado para evitar duplicados antes de persistir.
     */
    boolean existsByTareaOrigenIdAndTareaDestinoId(Long tareaOrigenId, Long tareaDestinoId);

    /**
     * Elimina todas las dependencias en las que una tarea participa (origen o destino).
     * Llamado al eliminar una tarea para mantener integridad.
     */
    @Modifying
    @Query("DELETE FROM TareaDependencia d WHERE d.tareaOrigen.id = :tareaId OR d.tareaDestino.id = :tareaId")
    void deleteByTareaParticipante(@Param("tareaId") Long tareaId);
}
