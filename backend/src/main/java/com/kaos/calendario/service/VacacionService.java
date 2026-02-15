package com.kaos.calendario.service;

import com.kaos.calendario.dto.VacacionRequest;
import com.kaos.calendario.dto.VacacionResponse;
import com.kaos.calendario.entity.Vacacion;
import com.kaos.calendario.mapper.VacacionMapper;
import com.kaos.calendario.repository.VacacionRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio para gestión de vacaciones.
 * Calcula días laborables excluyendo fines de semana, valida solapamientos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VacacionService {

    private final VacacionRepository repository;
    private final PersonaRepository personaRepository;
    private final VacacionMapper mapper;

    /**
     * Lista todas las vacaciones con filtros opcionales.
     */
    public List<VacacionResponse> listar(Long personaId, Long squadId) {
        if (personaId != null) {
            return mapper.toResponseList(repository.findByPersonaId(personaId));
        }
        if (squadId != null) {
            return mapper.toResponseList(repository.findBySquadIdAndFechaRange(squadId, null, null));
        }
        return mapper.toResponseList(repository.findAll());
    }

    /**
     * Obtiene una vacación por ID.
     */
    public VacacionResponse obtener(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Vacación no encontrada: " + id));
    }

    /**
     * Crea una nueva vacación.
     * Valida fechas, calcula días laborables, verifica solapamiento.
     */
    @Transactional
    public VacacionResponse crear(VacacionRequest request) {
        log.info("Creando vacación para persona {}: {} - {}", 
                request.personaId(), request.fechaInicio(), request.fechaFin());

        // Validar fechas
        if (request.fechaFin().isBefore(request.fechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior o igual a la fecha de inicio");
        }

        // Validar persona existe
        Persona persona = personaRepository.findById(request.personaId())
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada: " + request.personaId()));

        // Validar no hay solapamiento
        if (repository.existsSolapamiento(request.personaId(), null, request.fechaInicio(), request.fechaFin())) {
            throw new IllegalArgumentException("Ya existe una vacación para esta persona en el período indicado");
        }

        // Crear entity
        Vacacion entity = mapper.toEntity(request);
        entity.setPersona(persona);
        entity.setDiasLaborables(calcularDiasLaborables(request.fechaInicio(), request.fechaFin()));

        Vacacion saved = repository.save(entity);
        log.info("Vacación creada con ID: {} ({} días laborables)", saved.getId(), saved.getDiasLaborables());
        return mapper.toResponse(saved);
    }

    /**
     * Actualiza una vacación existente.
     */
    @Transactional
    public VacacionResponse actualizar(Long id, VacacionRequest request) {
        log.info("Actualizando vacación {}", id);

        Vacacion entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vacación no encontrada: " + id));

        // Validar fechas
        if (request.fechaFin().isBefore(request.fechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior o igual a la fecha de inicio");
        }

        // Si cambió persona, validar existe
        if (!entity.getPersona().getId().equals(request.personaId())) {
            Persona persona = personaRepository.findById(request.personaId())
                    .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada: " + request.personaId()));
            entity.setPersona(persona);
        }

        // Validar no hay solapamiento (excluyendo la vacación actual)
        if (repository.existsSolapamiento(request.personaId(), id, request.fechaInicio(), request.fechaFin())) {
            throw new IllegalArgumentException("Ya existe una vacación para esta persona en el período indicado");
        }

        // Actualizar campos
        mapper.updateEntity(request, entity);
        entity.setDiasLaborables(calcularDiasLaborables(request.fechaInicio(), request.fechaFin()));

        Vacacion saved = repository.save(entity);
        log.info("Vacación {} actualizada ({} días laborables)", id, saved.getDiasLaborables());
        return mapper.toResponse(saved);
    }

    /**
     * Elimina una vacación.
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando vacación {}", id);

        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Vacación no encontrada: " + id);
        }

        repository.deleteById(id);
        log.info("Vacación {} eliminada", id);
    }

    /**
     * Lista vacaciones de un squad en un rango de fechas.
     */
    public List<VacacionResponse> listarPorSquad(Long squadId, LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Listando vacaciones del squad {} entre {} y {}", squadId, fechaInicio, fechaFin);
        return mapper.toResponseList(
                repository.findBySquadIdAndFechaRange(squadId, fechaInicio, fechaFin)
        );
    }

    /**
     * Calcula días laborables entre dos fechas (excluyendo sábados y domingos).
     * @param inicio Fecha inicio (inclusive)
     * @param fin Fecha fin (inclusive)
     * @return Número de días laborables
     */
    private int calcularDiasLaborables(LocalDate inicio, LocalDate fin) {
        return (int) inicio.datesUntil(fin.plusDays(1))
                .filter(fecha -> {
                    DayOfWeek dia = fecha.getDayOfWeek();
                    return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY;
                })
                .count();
    }
}
