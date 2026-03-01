package com.kaos.planificacion.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.dto.TareaAsignacionTimelineRequest;
import com.kaos.planificacion.dto.TareaAsignacionTimelineResponse;
import com.kaos.planificacion.entity.TareaAsignacionTimeline;
import com.kaos.planificacion.mapper.TareaAsignacionTimelineMapper;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.planificacion.repository.TareaAsignacionTimelineRepository;
import com.kaos.planificacion.repository.TareaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio CRUD para asignaciones de tareas padre en el timeline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TareaAsignacionTimelineService {

    private final TareaAsignacionTimelineRepository repository;
    private final TareaRepository tareaRepository;
    private final PersonaRepository personaRepository;
    private final SprintRepository sprintRepository;
    private final TareaAsignacionTimelineMapper mapper;

    /**
     * Lista todas las asignaciones de un sprint.
     */
    public List<TareaAsignacionTimelineResponse> listarPorSprint(Long sprintId) {
        log.debug("Listando asignaciones timeline para sprint {}", sprintId);
        return mapper.toResponseList(repository.findBySprintId(sprintId));
    }

    /**
     * Obtiene una asignación por ID.
     */
    public TareaAsignacionTimelineResponse obtener(Long id) {
        return mapper.toResponse(findById(id));
    }

    /**
     * Crea una nueva asignación de tarea padre en el timeline.
     */
    @Transactional
    public TareaAsignacionTimelineResponse crear(TareaAsignacionTimelineRequest request) {
        log.info("Creando asignación timeline — tarea: {}, persona: {}, sprint: {}",
                request.tareaId(), request.personaId(), request.sprintId());

        validarRangoDias(request.diaInicio(), request.diaFin());

        TareaAsignacionTimeline asignacion = TareaAsignacionTimeline.builder()
                .tarea(tareaRepository.findById(request.tareaId())
                        .orElseThrow(() -> new EntityNotFoundException("Tarea no encontrada: " + request.tareaId())))
                .persona(personaRepository.findById(request.personaId())
                        .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada: " + request.personaId())))
                .sprint(sprintRepository.findById(request.sprintId())
                        .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado: " + request.sprintId())))
                .diaInicio(request.diaInicio())
                .diaFin(request.diaFin())
                .horasPorDia(request.horasPorDia() != null ? BigDecimal.valueOf(request.horasPorDia()) : null)
                .esInformativa(Boolean.TRUE.equals(request.esInformativa()))
                .build();

        return mapper.toResponse(repository.save(asignacion));
    }

    /**
     * Actualiza una asignación existente.
     */
    @Transactional
    public TareaAsignacionTimelineResponse actualizar(Long id, TareaAsignacionTimelineRequest request) {
        log.info("Actualizando asignación timeline {}", id);
        TareaAsignacionTimeline asignacion = findById(id);

        validarRangoDias(request.diaInicio(), request.diaFin());

        asignacion.setTarea(tareaRepository.findById(request.tareaId())
                .orElseThrow(() -> new EntityNotFoundException("Tarea no encontrada: " + request.tareaId())));
        asignacion.setPersona(personaRepository.findById(request.personaId())
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada: " + request.personaId())));
        asignacion.setSprint(sprintRepository.findById(request.sprintId())
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado: " + request.sprintId())));
        asignacion.setDiaInicio(request.diaInicio());
        asignacion.setDiaFin(request.diaFin());
        asignacion.setHorasPorDia(request.horasPorDia() != null ? BigDecimal.valueOf(request.horasPorDia()) : null);
        asignacion.setEsInformativa(Boolean.TRUE.equals(request.esInformativa()));

        return mapper.toResponse(repository.save(asignacion));
    }

    /**
     * Elimina una asignación.
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando asignación timeline {}", id);
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Asignación timeline no encontrada: " + id);
        }
        repository.deleteById(id);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private TareaAsignacionTimeline findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Asignación timeline no encontrada: " + id));
    }

    private void validarRangoDias(Integer diaInicio, Integer diaFin) {
        if (diaInicio < 1 || diaFin > 10 || diaInicio > diaFin) {
            throw new IllegalArgumentException(
                    "Rango de días inválido: diaInicio=" + diaInicio + ", diaFin=" + diaFin
                    + " (debe ser 1 <= diaInicio <= diaFin <= 10)");
        }
    }
}
