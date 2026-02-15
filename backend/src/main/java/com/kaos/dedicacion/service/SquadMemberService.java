package com.kaos.dedicacion.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaos.dedicacion.dto.SquadMemberRequest;
import com.kaos.dedicacion.dto.SquadMemberResponse;
import com.kaos.dedicacion.entity.SquadMember;
import com.kaos.dedicacion.mapper.SquadMemberMapper;
import com.kaos.dedicacion.repository.SquadMemberRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.squad.entity.Squad;
import com.kaos.squad.repository.SquadRepository;

/**
 * Servicio de negocio para gestión de {@link SquadMember}.
 * Valida porcentaje total de dedicación ≤ 100%.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SquadMemberService {

    private final SquadMemberRepository repository;
    private final SquadMemberMapper mapper;
    private final PersonaRepository personaRepository;
    private final SquadRepository squadRepository;

    /**
     * Lista miembros de un squad.
     *
     * @param squadId ID del squad
     * @throws EntityNotFoundException si el squad no existe
     */
    public List<SquadMemberResponse> listarMiembrosSquad(Long squadId) {
        log.debug("Listando miembros del squad: {}", squadId);
        if (!squadRepository.existsById(squadId)) {
            throw new EntityNotFoundException("Squad no encontrado con id: " + squadId);
        }
        return mapper.toResponseList(repository.findBySquadId(squadId));
    }

    /**
     * Lista squads a los que pertenece una persona.
     *
     * @param personaId ID de la persona
     * @throws EntityNotFoundException si la persona no existe
     */
    public List<SquadMemberResponse> listarSquadsDePersona(Long personaId) {
        log.debug("Listando squads de persona: {}", personaId);
        if (!personaRepository.existsById(personaId)) {
            throw new EntityNotFoundException("Persona no encontrada con id: " + personaId);
        }
        return mapper.toResponseList(repository.findByPersonaId(personaId));
    }

    /**
     * Asigna una persona a un squad con rol y porcentaje.
     * Valida: unique constraint (persona_id, squad_id), SUM(porcentaje) ≤ 100%.
     */
    @Transactional
    public SquadMemberResponse asignar(SquadMemberRequest request) {
        log.info("Asignando persona {} a squad {} con {}% dedicación",
                request.personaId(), request.squadId(), request.porcentaje());

        Persona persona = personaRepository.findById(request.personaId())
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada con id: " + request.personaId()));

        Squad squad = squadRepository.findById(request.squadId())
                .orElseThrow(() -> new EntityNotFoundException("Squad no encontrado con id: " + request.squadId()));

        // Validar que no exista ya la asignación
        if (repository.existsByPersonaIdAndSquadId(request.personaId(), request.squadId())) {
            throw new IllegalArgumentException(
                    "La persona ya está asignada a este squad. Use PUT para actualizar.");
        }

        // Validar que el porcentaje total no supere 100%
        validarPorcentajeTotal(request.personaId(), request.porcentaje(), null);

        SquadMember entity = mapper.toEntity(request);
        entity.setPersona(persona);
        entity.setSquad(squad);

        SquadMember saved = repository.save(entity);
        log.info("Asignación creada con id: {}", saved.getId());
        return mapper.toResponse(saved);
    }

    /**
     * Actualiza rol o porcentaje de una asignación existente.
     */
    @Transactional
    public SquadMemberResponse actualizar(Long id, SquadMemberRequest request) {
        log.info("Actualizando asignación con id: {}", id);

        SquadMember entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Asignación no encontrada con id: " + id));

        // Si cambia persona+squad, verificar unicidad
        if (!entity.getPersona().getId().equals(request.personaId()) ||
                !entity.getSquad().getId().equals(request.squadId())) {
            if (repository.existsByPersonaIdAndSquadIdAndIdNot(
                    request.personaId(), request.squadId(), id)) {
                throw new IllegalArgumentException("Ya existe una asignación para esta persona en este squad.");
            }
        }

        // Validar porcentaje total (excluyendo registro actual)
        validarPorcentajeTotal(request.personaId(), request.porcentaje(), id);

        // Resolver entidades si cambiaron
        if (!entity.getPersona().getId().equals(request.personaId())) {
            Persona persona = personaRepository.findById(request.personaId())
                    .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada con id: " + request.personaId()));
            entity.setPersona(persona);
        }
        if (!entity.getSquad().getId().equals(request.squadId())) {
            Squad squad = squadRepository.findById(request.squadId())
                    .orElseThrow(() -> new EntityNotFoundException("Squad no encontrado con id: " + request.squadId()));
            entity.setSquad(squad);
        }

        mapper.updateEntity(request, entity);
        SquadMember saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    /**
     * Elimina una asignación persona-squad.
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando asignación con id: {}", id);
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Asignación no encontrada con id: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Valida que la suma total de porcentajes de la persona no supere 100%.
     *
     * @param personaId  ID de la persona
     * @param nuevoPorcentaje porcentaje a añadir/establecer
     * @param excludeId  ID del registro a excluir (para updates), null para creates
     * @throws IllegalArgumentException si se supera el 100%
     */
    private void validarPorcentajeTotal(Long personaId, Integer nuevoPorcentaje, Long excludeId) {
        int porcentajeActual = repository.sumPorcentajeByPersonaId(personaId, excludeId);
        int porcentajeTotal = porcentajeActual + nuevoPorcentaje;

        if (porcentajeTotal > 100) {
            int disponible = 100 - porcentajeActual;
            throw new IllegalArgumentException(
                    String.format("El porcentaje total de dedicación sería %d%%, superando el 100%%. " +
                            "Porcentaje actual: %d%%. Disponible: %d%%.",
                            porcentajeTotal, porcentajeActual, disponible));
        }
    }
}
