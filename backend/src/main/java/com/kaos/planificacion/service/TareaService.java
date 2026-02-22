package com.kaos.planificacion.service;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.calendario.dto.CapacidadDiaResponse;
import com.kaos.calendario.dto.CapacidadPersonaResponse;
import com.kaos.calendario.dto.CapacidadSquadResponse;
import com.kaos.calendario.service.CapacidadService;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.dto.TareaRequest;
import com.kaos.planificacion.dto.TareaResponse;
import com.kaos.planificacion.entity.EstadoTarea;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.entity.Tarea;
import com.kaos.planificacion.exception.CapacidadInsuficienteException;
import com.kaos.planificacion.exception.SprintNoEnPlanificacionException;
import com.kaos.planificacion.exception.TareaNoEnPendienteException;
import com.kaos.planificacion.mapper.TareaMapper;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.planificacion.repository.TareaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de negocio para gestión de {@link Tarea}.
 * Valida capacidad disponible, transiciones de estado y restricciones de sprint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TareaService {

    private final TareaRepository tareaRepository;
    private final SprintRepository sprintRepository;
    private final PersonaRepository personaRepository;
    private final TareaMapper tareaMapper;
    private final CapacidadService capacidadService;

    /**
     * Lista tareas con filtros opcionales.
     *
     * @param sprintId   ID del sprint (opcional)
     * @param personaId  ID de la persona (opcional)
     * @param estado     Estado de la tarea (opcional)
     * @param pageable   Paginación
     * @return Página de TareaResponse
     */
    public Page<TareaResponse> listar(Long sprintId, Long personaId, EstadoTarea estado, Pageable pageable) {
        log.debug("Listando tareas - sprintId: {}, personaId: {}, estado: {}", sprintId, personaId, estado);

        Page<Tarea> tareas;
        if (sprintId != null && personaId != null && estado != null) {
            tareas = tareaRepository.findBySprintIdAndPersonaIdAndEstado(sprintId, personaId, estado, pageable);
        } else if (sprintId != null && personaId != null) {
            tareas = tareaRepository.findBySprintIdAndPersonaId(sprintId, personaId, pageable);
        } else if (sprintId != null && estado != null) {
            tareas = tareaRepository.findBySprintIdAndEstado(sprintId, estado, pageable);
        } else if (sprintId != null) {
            tareas = tareaRepository.findBySprintId(sprintId, pageable);
        } else if (personaId != null) {
            tareas = tareaRepository.findByPersonaId(personaId, pageable);
        } else if (estado != null) {
            tareas = tareaRepository.findByEstado(estado, pageable);
        } else {
            tareas = tareaRepository.findAll(pageable);
        }

        return tareas.map(tareaMapper::toResponse);
    }

    /**
     * Obtiene una tarea por su ID.
     *
     * @param id ID de la tarea
     * @return TareaResponse
     * @throws EntityNotFoundException si no existe
     */
    public TareaResponse obtener(Long id) {
        log.debug("Obteniendo tarea con id: {}", id);
        Tarea tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tarea no encontrada con id: " + id));

        return tareaMapper.toResponse(tarea);
    }

    /**
     * Crea una nueva tarea.
     * Validaciones:
     * - Sprint existe y está en PLANIFICACION o ACTIVO
     * - Estimación > 0
     * Si la tarea incluye asignación (personaId + diaAsignado), valida capacidad disponible.
     *
     * @param request TareaRequest con datos de la tarea
     * @return TareaResponse de la tarea creada
     * @throws EntityNotFoundException si sprint/persona no existen
     * @throws SprintNoEnPlanificacionException si el sprint no está en estado válido
     * @throws IllegalArgumentException si la estimación es inválida
     * @throws CapacidadInsuficienteException si la capacidad es insuficiente
     */
    @Transactional
    public TareaResponse crear(TareaRequest request) {
        log.info("Creando tarea: {} en sprint: {}", request.titulo(), request.sprintId());

        // Validar que el sprint existe
        Sprint sprint = sprintRepository.findById(request.sprintId())
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado con id: " + request.sprintId()));

        // Validar que el sprint está en PLANIFICACION o ACTIVO
        if (sprint.getEstado() != SprintEstado.PLANIFICACION && sprint.getEstado() != SprintEstado.ACTIVO) {
            throw new SprintNoEnPlanificacionException(sprint.getId(), sprint.getEstado().toString());
        }

        // Validar estimación
        if (request.estimacion() == null || request.estimacion().doubleValue() <= 0) {
            throw new IllegalArgumentException("La estimación debe ser mayor a 0");
        }

        // Crear entidad
        Tarea tarea = tareaMapper.toEntity(request);
        tarea.setSprint(sprint);
        tarea.setEstado(EstadoTarea.PENDIENTE);

        if (request.personaId() != null) {
            tarea.setPersona(personaRepository.getReferenceById(request.personaId()));
        }

        // Si se proporciona asignación, validar capacidad
        if (request.personaId() != null && request.diaAsignado() != null) {
            // Validar que la persona existe
            if (!personaRepository.existsById(request.personaId())) {
                throw new EntityNotFoundException("Persona no encontrada con id: " + request.personaId());
            }

            // Validar capacidad disponible
            validarCapacidadDisponible(sprint, request.personaId(), request.diaAsignado(), request.estimacion());
        }

        Tarea saved = tareaRepository.save(tarea);
        log.info("Tarea creada con id: {}", saved.getId());

        return tareaMapper.toResponse(saved);
    }

    /**
     * Actualiza una tarea existente.
     * Si cambia la asignación (personaId + diaAsignado), valida capacidad disponible.
     *
     * @param id      ID de la tarea
     * @param request TareaRequest con datos actualizados
     * @return TareaResponse actualizado
     * @throws EntityNotFoundException si tarea/sprint/persona no existen
     * @throws IllegalStateException si la tarea está completada
     * @throws CapacidadInsuficienteException si sobrecarga la capacidad
     */
    @Transactional
    public TareaResponse actualizar(Long id, TareaRequest request) {
        log.info("Actualizando tarea con id: {}", id);

        Tarea tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tarea no encontrada con id: " + id));

        // Validar que no está completada
        if (tarea.getEstado() == EstadoTarea.COMPLETADA) {
            throw new IllegalStateException("No se puede editar una tarea completada");
        }

        Sprint sprint = tarea.getSprint();

        // Validar que el sprint no está cerrado
        if (sprint.getEstado() == SprintEstado.CERRADO) {
            throw new IllegalStateException("No se puede editar tareas de un sprint cerrado");
        }

        // Si cambia la asignación, validar capacidad
        boolean cambioAsignacion = !request.personaId().equals(tarea.getPersona() == null ? null : tarea.getPersona().getId()) ||
                !request.diaAsignado().equals(tarea.getDiaAsignado());

        if (cambioAsignacion && request.personaId() != null && request.diaAsignado() != null) {
            if (!personaRepository.existsById(request.personaId())) {
                throw new EntityNotFoundException("Persona no encontrada con id: " + request.personaId());
            }

            // Validar capacidad (considerando la asignación anterior)
            BigDecimal estimacionAnterior = tarea.getEstimacion();
            validarCapacidadDisponibleParaActualizacion(
                    sprint,
                    tarea.getPersona() == null ? null : tarea.getPersona().getId(),
                    request.personaId(),
                    request.diaAsignado(),
                    request.estimacion(),
                    estimacionAnterior);
        }

        // Validar estimación
        if (request.estimacion() == null || request.estimacion().doubleValue() <= 0) {
            throw new IllegalArgumentException("La estimación debe ser mayor a 0");
        }

        // Actualizar
        tareaMapper.updateEntity(request, tarea);

        if (request.personaId() != null) {
            tarea.setPersona(personaRepository.getReferenceById(request.personaId()));
        } else {
            tarea.setPersona(null);
        }
        Tarea saved = tareaRepository.save(tarea);

        return tareaMapper.toResponse(saved);
    }

    /**
     * Cambia el estado de una tarea.
     * Transiciones permitidas:
     * - PENDIENTE → EN_PROGRESO | BLOQUEADO
     * - EN_PROGRESO → BLOQUEADO | COMPLETADA
     * - BLOQUEADO → EN_PROGRESO
     * - No se puede cambiar desde COMPLETADA
     *
     * @param id         ID de la tarea
     * @param nuevoEstado Nuevo estado deseado
     * @return TareaResponse con estado actualizado
     * @throws EntityNotFoundException si la tarea no existe
     * @throws IllegalStateException si la transición no es válida
     */
    @Transactional
    public TareaResponse cambiarEstado(Long id, EstadoTarea nuevoEstado) {
        log.info("Cambiando estado de tarea {} a {}", id, nuevoEstado);

        Tarea tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tarea no encontrada con id: " + id));

        EstadoTarea estadoActual = tarea.getEstado();

        // Validar transición
        boolean transicionValida = false;

        if (estadoActual == EstadoTarea.PENDIENTE && (nuevoEstado == EstadoTarea.EN_PROGRESO || nuevoEstado == EstadoTarea.BLOQUEADO)) {
            transicionValida = true;
        } else if (estadoActual == EstadoTarea.EN_PROGRESO && (nuevoEstado == EstadoTarea.BLOQUEADO || nuevoEstado == EstadoTarea.COMPLETADA)) {
            transicionValida = true;
        } else if (estadoActual == EstadoTarea.BLOQUEADO && nuevoEstado == EstadoTarea.EN_PROGRESO) {
            transicionValida = true;
        }

        if (!transicionValida) {
            throw new IllegalStateException(
                    "Transición inválida de " + estadoActual + " a " + nuevoEstado);
        }

        tarea.setEstado(nuevoEstado);
        Tarea saved = tareaRepository.save(tarea);

        return tareaMapper.toResponse(saved);
    }

    /**
     * Elimina una tarea.
     * Solo permite eliminar si la tarea está en estado PENDIENTE.
     *
     * @param id ID de la tarea
     * @throws EntityNotFoundException si no existe
     * @throws TareaNoEnPendienteException si no está en PENDIENTE
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando tarea con id: {}", id);

        Tarea tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tarea no encontrada con id: " + id));

        if (tarea.getEstado() != EstadoTarea.PENDIENTE) {
            throw new TareaNoEnPendienteException(id, tarea.getEstado().toString());
        }

        tareaRepository.deleteById(id);
        log.info("Tarea {} eliminada", id);
    }

    /**
     * Valida que hay capacidad disponible para asignar una tarea a una persona en un día específico.
     * Lanza excepción si no hay capacidad suficiente.
     *
     * @param sprint      Sprint de la tarea
     * @param personaId   ID de la persona
     * @param dia         Día del sprint (1-10)
     * @param estimacion  Estimación en horas
     * @throws CapacidadInsuficienteException si no hay capacidad
     */
    private void validarCapacidadDisponible(Sprint sprint, Long personaId, Integer dia, BigDecimal estimacion) {
        CapacidadSquadResponse capacidad = capacidadService.calcularCapacidad(
                sprint.getSquad().getId(),
                sprint.getFechaInicio(),
                sprint.getFechaFin());

        // Buscar capacidad de la persona para el día especificado
        CapacidadPersonaResponse capacidadPersona = capacidad.personas().stream()
                .filter(cp -> cp.personaId().equals(personaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Persona no pertenece al squad del sprint"));

        // Obtener capacidad del día (index: día-1, porque días son 1-10)
        CapacidadDiaResponse capacidadDia = capacidadPersona.detalles().get(dia - 1);
        Double horasDisponibles = capacidadDia.horasDisponibles();

        if (estimacion.doubleValue() > horasDisponibles) {
            throw new CapacidadInsuficienteException(
                    "Capacidad insuficiente para asignar " + estimacion + " horas a la persona " + personaId +
                            " en el día " + dia + ". Disponibles: " + horasDisponibles,
                    personaId,
                    dia,
                    horasDisponibles,
                    estimacion.doubleValue());
        }

        log.debug("Validación de capacidad exitosa para persona {} día {} - Disponibles: {}, Requeridas: {}",
                personaId, dia, horasDisponibles, estimacion);
    }

    /**
     * Valida capacidad disponible para actualizar una asignación existente.
     * Considera la liberación de horas de la asignación anterior.
     *
     * @param sprint                Sprint
     * @param personaIdAnterior     ID de persona anterior (null si no estaba asignada)
     * @param personaIdNueva        ID de nueva persona
     * @param diaNuevo              Día nuevo
     * @param estimacionNueva       Estimación nueva
     * @param estimacionAnterior    Estimación anterior
     */
    private void validarCapacidadDisponibleParaActualizacion(
            Sprint sprint,
            Long personaIdAnterior,
            Long personaIdNueva,
            Integer diaNuevo,
            BigDecimal estimacionNueva,
            BigDecimal estimacionAnterior) {

        // Si es la misma persona y el mismo día, solo validar la diferencia
        if (personaIdAnterior != null && personaIdAnterior.equals(personaIdNueva) && diaNuevo != null) {
            BigDecimal diferencia = estimacionNueva.subtract(estimacionAnterior);
            if (diferencia.doubleValue() > 0) {
                // Solo validar si aumenta la estimación
                validarCapacidadDisponible(sprint, personaIdNueva, diaNuevo, diferencia);
            }
        } else {
            // Total cambio de asignación, validar completamente
            validarCapacidadDisponible(sprint, personaIdNueva, diaNuevo, estimacionNueva);
        }
    }
}
