package com.kaos.planificacion.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.dto.BloqueoRequest;
import com.kaos.planificacion.dto.BloqueoResponse;
import com.kaos.planificacion.entity.Bloqueo;
import com.kaos.planificacion.entity.EstadoBloqueo;
import com.kaos.planificacion.mapper.BloqueoMapper;
import com.kaos.planificacion.repository.BloqueoRepository;

/**
 * Servicio de negocio para gestión de {@link Bloqueo}.
 * Maneja impedimentos que afectan tareas y su resolución.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BloqueoService {

    private final BloqueoRepository bloqueoRepository;
    private final PersonaRepository personaRepository;
    private final BloqueoMapper bloqueoMapper;

    /**
     * Lista bloqueos con filtros opcionales.
     *
     * @param estado   Estado del bloqueo (opcional)
     * @param pageable Paginación
     * @return Página de BloqueoResponse
     */
    public Page<BloqueoResponse> listar(EstadoBloqueo estado, Pageable pageable) {
        log.debug("Listando bloqueos - estado: {}", estado);

        Page<Bloqueo> bloqueos;
        if (estado != null) {
            bloqueos = bloqueoRepository.findByEstado(estado, pageable);
        } else {
            bloqueos = bloqueoRepository.findAll(pageable);
        }

        return bloqueos.map(bloqueoMapper::toResponse);
    }

    /**
     * Obtiene un bloqueo por su ID.
     *
     * @param id ID del bloqueo
     * @return BloqueoResponse
     * @throws EntityNotFoundException si no existe
     */
    public BloqueoResponse obtener(Long id) {
        log.debug("Obteniendo bloqueo con id: {}", id);
        Bloqueo bloqueo = bloqueoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bloqueo no encontrado con id: " + id));

        return bloqueoMapper.toResponse(bloqueo);
    }

    /**
     * Crea un nuevo bloqueo.
     * Validaciones:
     * - Título no vacío
     * - Responsable existe (si se proporciona)
     *
     * @param request BloqueoRequest con datos del bloqueo
     * @return BloqueoResponse del bloqueo creado
     * @throws EntityNotFoundException si el responsable no existe
     * @throws IllegalArgumentException si el título está vacío
     */
    @Transactional
    public BloqueoResponse crear(BloqueoRequest request) {
        log.info("Creando bloqueo: {}", request.titulo());

        // Validar título
        if (request.titulo() == null || request.titulo().trim().isEmpty()) {
            throw new IllegalArgumentException("El título del bloqueo no puede estar vacío");
        }

        // Validar responsable si se proporciona
        if (request.responsableId() != null && !personaRepository.existsById(request.responsableId())) {
            throw new EntityNotFoundException("Responsable no encontrado con id: " + request.responsableId());
        }

        // Crear entidad
        Bloqueo bloqueo = bloqueoMapper.toEntity(request);
        bloqueo.setEstado(EstadoBloqueo.ABIERTO);

        Bloqueo saved = bloqueoRepository.save(bloqueo);
        log.info("Bloqueo creado con id: {}", saved.getId());

        return bloqueoMapper.toResponse(saved);
    }

    /**
     * Actualiza un bloqueo existente.
     *
     * @param id      ID del bloqueo
     * @param request BloqueoRequest con datos actualizados
     * @return BloqueoResponse actualizado
     * @throws EntityNotFoundException si el bloqueo no existe
     */
    @Transactional
    public BloqueoResponse actualizar(Long id, BloqueoRequest request) {
        log.info("Actualizando bloqueo con id: {}", id);

        Bloqueo bloqueo = bloqueoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bloqueo no encontrado con id: " + id));

        // Validar título
        if (request.titulo() == null || request.titulo().trim().isEmpty()) {
            throw new IllegalArgumentException("El título del bloqueo no puede estar vacío");
        }

        // Validar responsable si cambia
        if (request.responsableId() != null && !personaRepository.existsById(request.responsableId())) {
            throw new EntityNotFoundException("Responsable no encontrado con id: " + request.responsableId());
        }

        // Actualizar
        bloqueoMapper.updateEntity(request, bloqueo);
        Bloqueo saved = bloqueoRepository.save(bloqueo);

        return bloqueoMapper.toResponse(saved);
    }

    /**
     * Cambia el estado de un bloqueo.
     * Transiciones permitidas:
     * - ABIERTO → EN_GESTION
     * - EN_GESTION → RESUELTO
     * - ABIERTO → RESUELTO (directo)
     *
     * @param id         ID del bloqueo
     * @param nuevoEstado Nuevo estado deseado
     * @return BloqueoResponse con estado actualizado
     * @throws EntityNotFoundException si no existe
     * @throws IllegalStateException si la transición no es válida
     */
    @Transactional
    public BloqueoResponse cambiarEstado(Long id, EstadoBloqueo nuevoEstado) {
        log.info("Cambiando estado de bloqueo {} a {}", id, nuevoEstado);

        Bloqueo bloqueo = bloqueoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bloqueo no encontrado con id: " + id));

        EstadoBloqueo estadoActual = bloqueo.getEstado();

        // Validar transición
        boolean transicionValida = false;

        if (estadoActual == EstadoBloqueo.ABIERTO && 
            (nuevoEstado == EstadoBloqueo.EN_GESTION || nuevoEstado == EstadoBloqueo.RESUELTO)) {
            transicionValida = true;
        } else if (estadoActual == EstadoBloqueo.EN_GESTION && nuevoEstado == EstadoBloqueo.RESUELTO) {
            transicionValida = true;
        }

        if (!transicionValida) {
            throw new IllegalStateException(
                    "Transición inválida de " + estadoActual + " a " + nuevoEstado);
        }

        bloqueo.setEstado(nuevoEstado);

        // Si se resuelve, registrar timestamp
        if (nuevoEstado == EstadoBloqueo.RESUELTO) {
            bloqueo.setFechaResolucion(LocalDateTime.now());
        }

        Bloqueo saved = bloqueoRepository.save(bloqueo);
        return bloqueoMapper.toResponse(saved);
    }

    /**
     * Elimina un bloqueo.
     *
     * @param id ID del bloqueo
     * @throws EntityNotFoundException si no existe
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando bloqueo con id: {}", id);

        if (!bloqueoRepository.existsById(id)) {
            throw new EntityNotFoundException("Bloqueo no encontrado con id: " + id);
        }

        bloqueoRepository.deleteById(id);
        log.info("Bloqueo {} eliminado", id);
    }

    /**
     * Cuenta bloqueos activos (ABIERTO o EN_GESTION).
     *
     * @return Cantidad de bloqueos activos
     */
    public Long contarActivos() {
        return bloqueoRepository.countByEstadoAbiertosOEnGestion();
    }
}
