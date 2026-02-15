package com.kaos.persona.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaos.horario.entity.PerfilHorario;
import com.kaos.horario.repository.PerfilHorarioRepository;
import com.kaos.persona.dto.PersonaRequest;
import com.kaos.persona.dto.PersonaResponse;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.entity.Rol;
import com.kaos.persona.entity.Seniority;
import com.kaos.persona.mapper.PersonaMapper;
import com.kaos.persona.repository.PersonaRepository;

/**
 * Servicio de negocio para gestión de {@link Persona}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PersonaService {

    private final PersonaRepository repository;
    private final PersonaMapper mapper;
    private final PerfilHorarioRepository perfilHorarioRepository;

    /**
     * Lista personas con paginación y filtros opcionales.
     *
     * @param squadId   filtrar por squad (nullable)
     * @param rol       filtrar por rol en squad (nullable)
     * @param seniority filtrar por seniority (nullable)
     * @param ubicacion filtrar por zona horaria parcial (nullable)
     * @param activo    filtrar por estado activo/inactivo (nullable)
     * @param pageable  parámetros de paginación
     */
    public Page<PersonaResponse> listar(Long squadId, Rol rol, Seniority seniority,
                                         String ubicacion, Boolean activo, Pageable pageable) {
        log.debug("Listando personas - squadId: {}, rol: {}, seniority: {}, ubicacion: {}, activo: {}",
                squadId, rol, seniority, ubicacion, activo);
        return repository.findWithFilters(squadId, rol, seniority, ubicacion, activo, pageable)
                .map(mapper::toResponse);
    }

    /**
     * Obtiene una persona por su ID.
     *
     * @throws EntityNotFoundException si no existe
     */
    public PersonaResponse obtener(Long id) {
        log.debug("Obteniendo persona con id: {}", id);
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada con id: " + id));
    }

    /**
     * Crea una nueva persona.
     * Valida email único, idJira único y existencia del perfil de horario.
     */
    @Transactional
    public PersonaResponse crear(PersonaRequest request) {
        log.info("Creando persona: {}", request.nombre());
        validarEmailUnico(request.email(), null);
        validarIdJiraUnico(request.idJira(), null);

        PerfilHorario perfilHorario = perfilHorarioRepository.findById(request.perfilHorarioId())
                .orElseThrow(() -> new IllegalArgumentException("Configure perfil de horario primero. No existe perfil con id: " + request.perfilHorarioId()));

        Persona entity = mapper.toEntity(request);
        entity.setPerfilHorario(perfilHorario);
        entity.setActivo(true);
        if (request.sendNotifications() == null) {
            entity.setSendNotifications(true);
        }

        Persona saved = repository.save(entity);
        log.info("Persona creada con id: {}", saved.getId());
        return mapper.toResponse(saved);
    }

    /**
     * Actualiza una persona existente.
     *
     * @throws EntityNotFoundException si no existe
     */
    @Transactional
    public PersonaResponse actualizar(Long id, PersonaRequest request) {
        log.info("Actualizando persona con id: {}", id);
        Persona entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada con id: " + id));

        validarEmailUnico(request.email(), id);
        validarIdJiraUnico(request.idJira(), id);

        PerfilHorario perfilHorario = perfilHorarioRepository.findById(request.perfilHorarioId())
                .orElseThrow(() -> new IllegalArgumentException("Configure perfil de horario primero. No existe perfil con id: " + request.perfilHorarioId()));

        mapper.updateEntity(request, entity);
        entity.setPerfilHorario(perfilHorario);
        Persona saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    /**
     * Desactiva una persona (soft delete).
     * Marca activo=false — no aparecerá en planificación futura.
     *
     * @throws EntityNotFoundException si no existe
     */
    @Transactional
    public PersonaResponse desactivar(Long id) {
        log.info("Desactivando persona con id: {}", id);
        Persona entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada con id: " + id));

        entity.setActivo(false);
        Persona saved = repository.save(entity);
        log.info("Persona desactivada: {}", saved.getNombre());
        return mapper.toResponse(saved);
    }

    private void validarEmailUnico(String email, Long excludeId) {
        boolean exists = excludeId == null
                ? repository.existsByEmail(email)
                : repository.existsByEmailAndIdNot(email, excludeId);
        if (exists) {
            throw new IllegalArgumentException("Email ya registrado: " + email);
        }
    }

    private void validarIdJiraUnico(String idJira, Long excludeId) {
        if (idJira == null || idJira.isBlank()) return;
        boolean exists = excludeId == null
                ? repository.existsByIdJira(idJira)
                : repository.existsByIdJiraAndIdNot(idJira, excludeId);
        if (exists) {
            throw new IllegalArgumentException("ID Jira ya registrado: " + idJira);
        }
    }
}
