package com.kaos.horario.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaos.horario.dto.PerfilHorarioRequest;
import com.kaos.horario.dto.PerfilHorarioResponse;
import com.kaos.horario.entity.PerfilHorario;
import com.kaos.horario.mapper.PerfilHorarioMapper;
import com.kaos.horario.repository.PerfilHorarioRepository;
import com.kaos.persona.repository.PersonaRepository;

/**
 * Servicio de negocio para gestión de {@link PerfilHorario}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PerfilHorarioService {

    private final PerfilHorarioRepository repository;
    private final PerfilHorarioMapper mapper;
    private final PersonaRepository personaRepository;

    /**
     * Lista todos los perfiles de horario.
     */
    public List<PerfilHorarioResponse> listar() {
        log.debug("Listando todos los perfiles de horario");
        return mapper.toResponseList(repository.findAll());
    }

    /**
     * Obtiene un perfil de horario por su ID.
     *
     * @throws EntityNotFoundException si no existe
     */
    public PerfilHorarioResponse obtener(Long id) {
        log.debug("Obteniendo perfil de horario con id: {}", id);
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Perfil de horario no encontrado con id: " + id));
    }

    /**
     * Crea un nuevo perfil de horario.
     * Valida que el nombre no esté duplicado.
     */
    @Transactional
    public PerfilHorarioResponse crear(PerfilHorarioRequest request) {
        log.info("Creando perfil de horario: {}", request.nombre());
        validarNombreUnico(request.nombre(), null);

        PerfilHorario entity = mapper.toEntity(request);
        PerfilHorario saved = repository.save(entity);
        log.info("Perfil de horario creado con id: {}", saved.getId());
        return mapper.toResponse(saved);
    }

    /**
     * Actualiza un perfil de horario existente.
     *
     * @throws EntityNotFoundException si no existe
     */
    @Transactional
    public PerfilHorarioResponse actualizar(Long id, PerfilHorarioRequest request) {
        log.info("Actualizando perfil de horario con id: {}", id);
        PerfilHorario entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Perfil de horario no encontrado con id: " + id));

        validarNombreUnico(request.nombre(), id);
        mapper.updateEntity(request, entity);
        PerfilHorario saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    /**
     * Elimina un perfil de horario.
     * No se puede eliminar si tiene personas asignadas.
     *
     * @throws EntityNotFoundException si no existe
     * @throws IllegalStateException   si tiene personas asignadas
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando perfil de horario con id: {}", id);
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Perfil de horario no encontrado con id: " + id);
        }
        if (personaRepository.existsByPerfilHorarioId(id)) {
            throw new IllegalStateException("No se puede eliminar: hay personas asignadas a este perfil de horario");
        }
        repository.deleteById(id);
    }

    private void validarNombreUnico(String nombre, Long excludeId) {
        boolean exists = excludeId == null
                ? repository.existsByNombre(nombre)
                : repository.existsByNombreAndIdNot(nombre, excludeId);
        if (exists) {
            throw new IllegalArgumentException("Ya existe un perfil de horario con el nombre: " + nombre);
        }
    }
}
