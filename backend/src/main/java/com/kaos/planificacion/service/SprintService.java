package com.kaos.planificacion.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.calendario.dto.CapacidadSquadResponse;
import com.kaos.calendario.service.CapacidadService;
import com.kaos.planificacion.dto.SprintRequest;
import com.kaos.planificacion.dto.SprintResponse;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.exception.SolapamientoSprintException;
import com.kaos.planificacion.exception.SprintNoEnPlanificacionException;
import com.kaos.planificacion.mapper.SprintMapper;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.squad.repository.SquadRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de negocio para gestión de {@link Sprint}.
 * Encargado de crear, actualizar y validar sprints, incluyendo el cálculo de capacidad.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SprintService {

    private final SprintRepository sprintRepository;
    private final SquadRepository squadRepository;
    private final SprintMapper sprintMapper;
    private final CapacidadService capacidadService;

    /**
     * Lista sprints con filtros opcionales de squad y estado.
     *
     * @param squadId   ID del squad (opcional)
     * @param estado    Estado del sprint (opcional)
     * @param pageable  Paginación
     * @return Página de SprintResponse
     */
    public Page<SprintResponse> listar(Long squadId, SprintEstado estado, Pageable pageable) {
        log.debug("Listando sprints - squadId: {}, estado: {}", squadId, estado);

        Page<Sprint> sprints;
        if (squadId != null && estado != null) {
            sprints = sprintRepository.findBySquadIdAndEstado(squadId, estado, pageable);
        } else if (squadId != null) {
            sprints = sprintRepository.findBySquadId(squadId, pageable);
        } else if (estado != null) {
            sprints = sprintRepository.findByEstado(estado, pageable);
        } else {
            sprints = sprintRepository.findAll(pageable);
        }

        return sprints.map(sprintMapper::toResponse);
    }

    /**
     * Obtiene un sprint por su ID.
     *
     * @param id ID del sprint
     * @return SprintResponse con datos completos
     * @throws EntityNotFoundException si no existe
     */
    public SprintResponse obtener(Long id) {
        log.debug("Obteniendo sprint con id: {}", id);
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado con id: " + id));

        return sprintMapper.toResponse(sprint);
    }

    /**
    * Crea un nuevo sprint.
     * Validaciones:
     * - Fecha inicio es lunes
    * - Fecha fin = fecha inicio + 13 días (domingo)
    * - No hay solapamiento con otros sprints del mismo squad
    * - Se crea un sprint por cada squad
     *
     * @param request SprintRequest con los datos del sprint
     * @return SprintResponse del sprint creado
     * @throws IllegalArgumentException si las validaciones fallan
     * @throws SolapamientoSprintException si hay solapamiento
     * @throws EntityNotFoundException si el squad no existe
     */
    @Transactional
    public SprintResponse crear(SprintRequest request) {
        log.info("Creando sprint global: {}", request.nombre());

        var squads = squadRepository.findAll();
        if (squads.isEmpty()) {
            throw new EntityNotFoundException("No hay squads registrados");
        }

        var squadRespuesta = squads.stream()
                .filter(squad -> squad.getId().equals(request.squadId()))
                .findFirst()
                .orElseThrow(() ->
                        new EntityNotFoundException("Squad no encontrado con id: " + request.squadId()));

        // Validar que la fecha inicio es lunes
        if (request.fechaInicio().getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("La fecha de inicio debe ser un lunes");
        }

        // Calcular fecha fin (13 días después, que será domingo)
        LocalDate fechaFin = request.fechaInicio().plusDays(13);

        // Validar no solapamiento por cada squad
        for (var squad : squads) {
            boolean solapamiento = sprintRepository.existsSolapamiento(
                squad.getId(),
                request.fechaInicio(),
                fechaFin,
                -1L);

            if (solapamiento) {
            throw new SolapamientoSprintException(
                squad.getId(),
                "Ya existe un sprint del squad " + squad.getId() +
                    " que se solapa con el rango " + request.fechaInicio() + " - " + fechaFin);
            }
        }

        var nuevosSprints = squads.stream().map(squad -> {
            CapacidadSquadResponse capacidadSquad = capacidadService.calcularCapacidad(
                squad.getId(),
                request.fechaInicio(),
                fechaFin);

            Sprint sprint = sprintMapper.toEntity(request);
            sprint.setSquad(squad);
            sprint.setFechaFin(fechaFin);
            sprint.setEstado(SprintEstado.PLANIFICACION);
            sprint.setCapacidadTotal(BigDecimal.valueOf(capacidadSquad.horasTotales()));
            return sprint;
        }).toList();

        var savedSprints = sprintRepository.saveAll(nuevosSprints);
        var sprintRespuesta = savedSprints.stream()
            .filter(sprint -> sprint.getSquad().getId().equals(squadRespuesta.getId()))
            .findFirst()
            .orElse(savedSprints.getFirst());

        log.info("Sprint creado para {} squads", savedSprints.size());

        return sprintMapper.toResponse(sprintRespuesta);
    }

    /**
     * Actualiza un sprint existente.
     * Solo permite editar si el sprint está en estado PLANIFICACION.
     *
     * @param id      ID del sprint
     * @param request SprintRequest con datos actualizados
     * @return SprintResponse actualizado
     * @throws EntityNotFoundException si el sprint no existe
     * @throws SprintNoEnPlanificacionException si no está en PLANIFICACION
     */
    @Transactional
    public SprintResponse actualizar(Long id, SprintRequest request) {
        log.info("Actualizando sprint con id: {}", id);

        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado con id: " + id));

        // Validar que está en planificación
        if (sprint.getEstado() != SprintEstado.PLANIFICACION) {
            throw new SprintNoEnPlanificacionException(id, sprint.getEstado().toString());
        }

        // Validar que el squad existe si cambió
        if (!request.squadId().equals(sprint.getSquad().getId())) {
            if (!squadRepository.existsById(request.squadId())) {
                throw new EntityNotFoundException("Squad no encontrado con id: " + request.squadId());
            }

            sprint.setSquad(squadRepository.getReferenceById(request.squadId()));

            // Validar no solapamiento en la nueva squad
            LocalDate fechaFin = request.fechaInicio().plusDays(13);
            boolean solapamiento = sprintRepository.existsSolapamiento(
                    request.squadId(),
                    request.fechaInicio(),
                    fechaFin,
                    id); // Excluir el sprint actual en la validación

            if (solapamiento) {
                throw new SolapamientoSprintException(
                        request.squadId(),
                        "Ya existe un sprint del squad que se solapa con el rango especificado");
            }
        }

        // Actualizar entidad
        sprintMapper.updateEntity(request, sprint);
        sprint.setFechaFin(request.fechaInicio().plusDays(13));

        Sprint saved = sprintRepository.save(sprint);
        return sprintMapper.toResponse(saved);
    }

    /**
     * Cambia el estado del sprint.
     * Transiciones permitidas:
     * - PLANIFICACION → ACTIVO
     * - ACTIVO → CERRADO
     *
     * @param id         ID del sprint
     * @param nuevoEstado Nuevo estado deseado
     * @return SprintResponse con estado actualizado
     * @throws EntityNotFoundException si el sprint no existe
     * @throws IllegalStateException si la transición no es válida
     */
    @Transactional
    public java.util.List<SprintResponse> cambiarEstado(Long id, SprintEstado nuevoEstado) {
        log.info("Cambiando estado del sprint {} a {}", id, nuevoEstado);

        Sprint sprintBase = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado con id: " + id));

        var relacionados = sprintRepository.findByNombreAndFechaInicioAndFechaFin(
                sprintBase.getNombre(),
                sprintBase.getFechaInicio(),
                sprintBase.getFechaFin());

        // Validar transición usando el primer sprint no idempotente
        for (Sprint sprint : relacionados) {
            SprintEstado estadoActual = sprint.getEstado();
            if (estadoActual == nuevoEstado) {
                continue;
            }
            boolean transicionValida = (estadoActual == SprintEstado.PLANIFICACION && nuevoEstado == SprintEstado.ACTIVO)
                    || (estadoActual == SprintEstado.ACTIVO && nuevoEstado == SprintEstado.CERRADO);
            if (!transicionValida) {
                throw new IllegalStateException(
                        "Transición de estado inválida: " + estadoActual + " → " + nuevoEstado);
            }
        }

        for (Sprint sprint : relacionados) {
            if (sprint.getEstado() != nuevoEstado) {
                sprint.setEstado(nuevoEstado);
            }
        }

        var saved = sprintRepository.saveAll(relacionados);
        return sprintMapper.toResponseList(saved);
    }

    /**
     * Elimina un sprint.
     * Solo permite eliminar si el sprint está en estado PLANIFICACION.
     *
     * @param id ID del sprint
     * @throws EntityNotFoundException si el sprint no existe
     * @throws SprintNoEnPlanificacionException si no está en PLANIFICACION
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando sprint con id: {}", id);

        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado con id: " + id));

        if (sprint.getEstado() != SprintEstado.PLANIFICACION) {
            throw new SprintNoEnPlanificacionException(id, sprint.getEstado().toString());
        }

        sprintRepository.deleteById(id);
        log.info("Sprint {} eliminado", id);
    }
}
