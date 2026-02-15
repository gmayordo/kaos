package com.kaos.squad.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaos.squad.dto.SquadRequest;
import com.kaos.squad.dto.SquadResponse;
import com.kaos.squad.entity.EstadoSquad;
import com.kaos.squad.entity.Squad;
import com.kaos.squad.mapper.SquadMapper;
import com.kaos.squad.repository.SquadRepository;

/**
 * Servicio de negocio para gestión de {@link Squad}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SquadService {

    private final SquadRepository repository;
    private final SquadMapper mapper;

    /**
     * Lista squads, opcionalmente filtrados por estado.
     */
    public List<SquadResponse> listar(EstadoSquad estado) {
        log.debug("Listando squads - filtro estado: {}", estado);
        List<Squad> squads = (estado != null)
                ? repository.findByEstado(estado)
                : repository.findAll();
        return mapper.toResponseList(squads);
    }

    /**
     * Obtiene un squad por su ID.
     *
     * @throws EntityNotFoundException si no existe
     */
    public SquadResponse obtener(Long id) {
        log.debug("Obteniendo squad con id: {}", id);
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Squad no encontrado con id: " + id));
    }

    /**
     * Crea un nuevo squad con estado ACTIVO.
     */
    @Transactional
    public SquadResponse crear(SquadRequest request) {
        log.info("Creando squad: {}", request.nombre());
        validarNombreUnico(request.nombre(), null);

        Squad entity = mapper.toEntity(request);
        entity.setEstado(EstadoSquad.ACTIVO);
        Squad saved = repository.save(entity);
        log.info("Squad creado con id: {}", saved.getId());
        return mapper.toResponse(saved);
    }

    /**
     * Actualiza un squad existente.
     *
     * @throws EntityNotFoundException si no existe
     */
    @Transactional
    public SquadResponse actualizar(Long id, SquadRequest request) {
        log.info("Actualizando squad con id: {}", id);
        Squad entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Squad no encontrado con id: " + id));

        validarNombreUnico(request.nombre(), id);
        mapper.updateEntity(request, entity);
        Squad saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    /**
     * Desactiva un squad (soft delete).
     * No se permite si tiene sprints activos.
     *
     * @throws EntityNotFoundException si no existe
     * @throws IllegalStateException   si tiene sprints activos
     */
    @Transactional
    public SquadResponse desactivar(Long id) {
        log.info("Desactivando squad con id: {}", id);
        Squad entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Squad no encontrado con id: " + id));

        if (tieneSprintsActivos(id)) {
            throw new IllegalStateException(
                    "No se puede desactivar el squad porque tiene sprints activos. Considere desactivar los sprints primero.");
        }

        entity.setEstado(EstadoSquad.INACTIVO);
        Squad saved = repository.save(entity);
        log.info("Squad desactivado: {}", saved.getNombre());
        return mapper.toResponse(saved);
    }

    /**
     * Verifica si un squad tiene sprints activos.
     * TODO: Implementar cuando exista la entidad Sprint (Bloque 3).
     * Por ahora siempre retorna false.
     */
    private boolean tieneSprintsActivos(Long squadId) {
        // TODO [Bloque 3]: Implementar check de sprints activos con SprintRepository
        return false;
    }

    private void validarNombreUnico(String nombre, Long excludeId) {
        boolean exists = excludeId == null
                ? repository.existsByNombre(nombre)
                : repository.existsByNombreAndIdNot(nombre, excludeId);
        if (exists) {
            throw new IllegalArgumentException("Ya existe un squad con el nombre: " + nombre);
        }
    }
}
