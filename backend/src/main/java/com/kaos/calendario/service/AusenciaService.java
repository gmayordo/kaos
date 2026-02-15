package com.kaos.calendario.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.calendario.dto.AusenciaRequest;
import com.kaos.calendario.dto.AusenciaResponse;
import com.kaos.calendario.entity.Ausencia;
import com.kaos.calendario.mapper.AusenciaMapper;
import com.kaos.calendario.repository.AusenciaRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para gestión de ausencias.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AusenciaService {

    private final AusenciaRepository repository;
    private final PersonaRepository personaRepository;
    private final AusenciaMapper mapper;

    /**
     * Lista todas las ausencias con filtros opcionales.
     */
    public List<AusenciaResponse> listar(Long personaId, Long squadId) {
        if (personaId != null) {
            return mapper.toResponseList(repository.findByPersonaId(personaId));
        }
        if (squadId != null) {
            return mapper.toResponseList(repository.findBySquadIdAndFechaRange(squadId, null, null));
        }
        return mapper.toResponseList(repository.findAll());
    }

    /**
     * Obtiene una ausencia por ID.
     */
    public AusenciaResponse obtener(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Ausencia no encontrada: " + id));
    }

    /**
     * Crea una nueva ausencia.
     */
    @Transactional
    public AusenciaResponse crear(AusenciaRequest request) {
        log.info("Creando ausencia para persona {}: {} - {}", 
                request.personaId(), request.fechaInicio(), request.fechaFin());

        // Validar fechas si ambas están presentes
        if (request.fechaFin() != null && request.fechaFin().isBefore(request.fechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior o igual a la fecha de inicio");
        }

        // Validar persona existe
        Persona persona = personaRepository.findById(request.personaId())
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada: " + request.personaId()));

        // Crear entity
        Ausencia entity = mapper.toEntity(request);
        entity.setPersona(persona);

        Ausencia saved = repository.save(entity);
        log.info("Ausencia creada con ID: {}", saved.getId());
        return mapper.toResponse(saved);
    }

    /**
     * Actualiza una ausencia existente.
     */
    @Transactional
    public AusenciaResponse actualizar(Long id, AusenciaRequest request) {
        log.info("Actualizando ausencia {}", id);

        Ausencia entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ausencia no encontrada: " + id));

        // Validar fechas si ambas están presentes
        if (request.fechaFin() != null && request.fechaFin().isBefore(request.fechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior o igual a la fecha de inicio");
        }

        // Si cambió persona, validar existe
        if (!entity.getPersona().getId().equals(request.personaId())) {
            Persona persona = personaRepository.findById(request.personaId())
                    .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada: " + request.personaId()));
            entity.setPersona(persona);
        }

        // Actualizar campos
        mapper.updateEntity(request, entity);

        Ausencia saved = repository.save(entity);
        log.info("Ausencia {} actualizada", id);
        return mapper.toResponse(saved);
    }

    /**
     * Elimina una ausencia.
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando ausencia {}", id);

        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Ausencia no encontrada: " + id);
        }

        repository.deleteById(id);
        log.info("Ausencia {} eliminada", id);
    }

    /**
     * Lista ausencias de un squad en un rango de fechas.
     */
    public List<AusenciaResponse> listarPorSquad(Long squadId, LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Listando ausencias del squad {} entre {} y {}", squadId, fechaInicio, fechaFin);
        return mapper.toResponseList(
                repository.findBySquadIdAndFechaRange(squadId, fechaInicio, fechaFin)
        );
    }
}
