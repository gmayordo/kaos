package com.kaos.planificacion.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.dto.TareaContinuaRequest;
import com.kaos.planificacion.dto.TareaContinuaResponse;
import com.kaos.planificacion.entity.TareaContinua;
import com.kaos.planificacion.mapper.TareaContinuaMapper;
import com.kaos.planificacion.repository.TareaContinuaRepository;
import com.kaos.squad.repository.SquadRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio CRUD para tareas continuas multi-sprint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TareaContinuaService {

    private final TareaContinuaRepository repository;
    private final SquadRepository squadRepository;
    private final PersonaRepository personaRepository;
    private final TareaContinuaMapper mapper;

    /**
     * Lista tareas continuas activas de un squad.
     */
    public List<TareaContinuaResponse> listarPorSquad(Long squadId) {
        log.debug("Listando tareas continuas activas para squad {}", squadId);
        return mapper.toResponseList(repository.findBySquadIdAndActivaTrue(squadId));
    }

    /**
     * Obtiene una tarea continua por ID.
     */
    public TareaContinuaResponse obtener(Long id) {
        return mapper.toResponse(findById(id));
    }

    /**
     * Crea una nueva tarea continua.
     */
    @Transactional
    public TareaContinuaResponse crear(TareaContinuaRequest request) {
        log.info("Creando tarea continua: {}", request.titulo());

        validarFechas(request);

        TareaContinua tarea = TareaContinua.builder()
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .squad(squadRepository.findById(request.squadId())
                        .orElseThrow(() -> new EntityNotFoundException("Squad no encontrado: " + request.squadId())))
                .persona(request.personaId() != null
                        ? personaRepository.findById(request.personaId())
                                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada: " + request.personaId()))
                        : null)
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .horasPorDia(request.horasPorDia() != null ? BigDecimal.valueOf(request.horasPorDia()) : null)
                .esInformativa(Boolean.TRUE.equals(request.esInformativa()))
                .color(request.color() != null ? request.color() : "#6366f1")
                .activa(request.activa() == null || request.activa())
                .build();

        return mapper.toResponse(repository.save(tarea));
    }

    /**
     * Actualiza una tarea continua existente.
     */
    @Transactional
    public TareaContinuaResponse actualizar(Long id, TareaContinuaRequest request) {
        log.info("Actualizando tarea continua {}", id);
        TareaContinua tarea = findById(id);

        validarFechas(request);

        tarea.setTitulo(request.titulo());
        tarea.setDescripcion(request.descripcion());
        tarea.setSquad(squadRepository.findById(request.squadId())
                .orElseThrow(() -> new EntityNotFoundException("Squad no encontrado: " + request.squadId())));
        tarea.setPersona(request.personaId() != null
                ? personaRepository.findById(request.personaId())
                        .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada: " + request.personaId()))
                : null);
        tarea.setFechaInicio(request.fechaInicio());
        tarea.setFechaFin(request.fechaFin());
        tarea.setHorasPorDia(request.horasPorDia() != null ? BigDecimal.valueOf(request.horasPorDia()) : null);
        tarea.setEsInformativa(Boolean.TRUE.equals(request.esInformativa()));
        if (request.color() != null) {
            tarea.setColor(request.color());
        }
        if (request.activa() != null) {
            tarea.setActiva(request.activa());
        }

        return mapper.toResponse(repository.save(tarea));
    }

    /**
     * Elimina (soft delete) una tarea continua.
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando tarea continua {}", id);
        TareaContinua tarea = findById(id);
        tarea.setActiva(false);
        repository.save(tarea);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private TareaContinua findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tarea continua no encontrada: " + id));
    }

    private void validarFechas(TareaContinuaRequest request) {
        if (request.fechaFin() != null && request.fechaFin().isBefore(request.fechaInicio())) {
            throw new IllegalArgumentException(
                    "fechaFin (" + request.fechaFin() + ") no puede ser anterior a fechaInicio (" + request.fechaInicio() + ")");
        }
    }
}
