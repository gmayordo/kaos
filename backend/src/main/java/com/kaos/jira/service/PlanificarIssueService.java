package com.kaos.jira.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.dto.JiraIssueResponse;
import com.kaos.jira.dto.PlanificarAsignacionItem;
import com.kaos.jira.dto.PlanificarIssueRequest;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.dto.TareaResponse;
import com.kaos.planificacion.entity.Categoria;
import com.kaos.planificacion.entity.EstadoTarea;
import com.kaos.planificacion.entity.Prioridad;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.Tarea;
import com.kaos.planificacion.entity.TipoTarea;
import com.kaos.planificacion.exception.AsignacionRequeridaException;
import com.kaos.planificacion.exception.SprintNoEnPlanificacionException;
import com.kaos.planificacion.mapper.TareaMapper;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.planificacion.repository.TareaRepository;
import com.kaos.planificacion.service.PlantillaAsignacionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de planificación avanzada: integra issues Jira en el sprint KAOS.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Listar issues Jira planificables (con jerarquía parent → subtareas)</li>
 *   <li>Crear Tareas KAOS a partir de issues Jira con asignación y estimación</li>
 *   <li>Vincular automáticamente la jerarquía de tareas (tareaParent)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanificarIssueService {

    private final JiraIssueRepository jiraIssueRepository;
    private final TareaRepository tareaRepository;
    private final SprintRepository sprintRepository;
    private final PersonaRepository personaRepository;
    private final TareaMapper tareaMapper;
    private final PlantillaAsignacionService plantillaService;

    // ── Consulta ─────────────────────────────────────────────────────────────

    /**
     * Lista los issues Jira de un squad en un sprint, organizados en jerarquía
     * (issues raíz con sus subtareas anidadas).
     *
     * @param squadId       ID del squad
     * @param sprintId      ID del sprint KAOS
     * @param soloSinTarea  si true, excluye los que ya tienen Tarea KAOS
     * @return Lista de JiraIssueResponse con subtareas anidadas
     */
    public List<JiraIssueResponse> listarIssuesPlanificables(Long squadId, Long sprintId, boolean soloSinTarea) {
        log.debug("Listando issues planificables - squadId: {}, sprintId: {}, soloSinTarea: {}",
                squadId, sprintId, soloSinTarea);

        List<JiraIssue> issues = soloSinTarea
                ? jiraIssueRepository.findBySquadIdAndSprintIdAndTareaIsNull(squadId, sprintId)
                : jiraIssueRepository.findBySquadIdAndSprintId(squadId, sprintId);

        return construirJerarquia(issues);
    }

    // ── Planificación ────────────────────────────────────────────────────────

    /**
     * Planifica varios issues Jira como Tareas KAOS en un sprint.
     *
     * <p>Para cada asignación:
     * <ol>
     *   <li>Valida que el issue existe y no tiene tarea ya asignada</li>
     *   <li>Valida que el sprint existe y está en estado válido</li>
     *   <li>Crea la Tarea KAOS vinculada al issue</li>
     *   <li>Si el issue tiene parentKey, enlaza tareaParent</li>
     * </ol>
     * </p>
     *
     * @param request Request con sprintId y lista de asignaciones
     * @return Lista de TareaResponse creadas
     * @throws EntityNotFoundException si sprint, persona o issue no existen
     * @throws SprintNoEnPlanificacionException si el sprint no está en estado planificación/activo
     * @throws IllegalStateException si un issue ya tiene tarea asignada
     */
    @Transactional
    public List<TareaResponse> planificar(PlanificarIssueRequest request) {
        log.info("Planificando {} issues en sprint {}", request.asignaciones().size(), request.sprintId());

        Sprint sprint = sprintRepository.findById(request.sprintId())
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado con id: " + request.sprintId()));

        validarEstadoSprint(sprint);

        // Pre-cargar todos los issues de una sola consulta
        List<String> jiraKeys = request.asignaciones().stream()
                .map(PlanificarAsignacionItem::jiraKey)
                .toList();
        Map<String, JiraIssue> issuesPorKey = jiraIssueRepository.findByJiraKeyIn(jiraKeys).stream()
                .collect(Collectors.toMap(JiraIssue::getJiraKey, i -> i));

        List<Tarea> creadas = new ArrayList<>();

        for (PlanificarAsignacionItem item : request.asignaciones()) {
            JiraIssue issue = issuesPorKey.get(item.jiraKey());
            if (issue == null) {
                throw new EntityNotFoundException("Issue Jira no encontrado: " + item.jiraKey());
            }
            if (issue.getTarea() != null) {
                throw new IllegalStateException(
                        "El issue " + item.jiraKey() + " ya tiene una tarea KAOS asignada (id: " + issue.getTarea().getId() + ")");
            }

            // Si el ítem no tiene estimación ni personaId, intentar aplicar plantilla automática
            PlanificarAsignacionItem itemEfectivo = resolverItemConPlantilla(item, issue);

            Tarea tarea = crearTareaDesdeItem(itemEfectivo, issue, sprint, creadas);
            creadas.add(tarea);

            // Vincular el issue a la tarea recién creada
            issue.setTarea(tarea);
            jiraIssueRepository.save(issue);
        }

        log.info("Planificación completada: {} tareas creadas en sprint {}", creadas.size(), sprint.getId());

        return creadas.stream().map(tareaMapper::toResponse).toList();
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Resuelve el item efectivo: si le faltan estimación o personaId, intenta aplicar
     * la plantilla activa para el tipoJira del issue. Si no hay plantilla y no hay datos
     * suficientes, lanza {@link AsignacionRequeridaException}.
     */
    private PlanificarAsignacionItem resolverItemConPlantilla(PlanificarAsignacionItem item, JiraIssue issue) {
        // Si el item ya trae estimación, usarlo tal cual
        if (item.estimacion() != null) {
            return item;
        }

        // Sin estimación: intentar plantilla
        BigDecimal estimacionBase = issue.getEstimacionHoras() != null ? issue.getEstimacionHoras() : BigDecimal.ONE;
        List<PlanificarAsignacionItem> sugeridos = plantillaService.aplicar(issue.getTipoJira(), estimacionBase);

        if (sugeridos.isEmpty()) {
            throw new AsignacionRequeridaException(issue.getJiraKey(), issue.getTipoJira());
        }

        // Tomar la primera línea de la plantilla y combinar con los datos del item
        PlanificarAsignacionItem base = sugeridos.get(0);
        return new PlanificarAsignacionItem(
                item.jiraKey(),
                item.personaId() != null ? item.personaId() : base.personaId(),
                base.estimacion(),
                item.diaAsignado(),
                item.tipo(),
                item.categoria(),
                item.prioridad()
        );
    }

    /**
     * Construye la jerarquía parent → subtareas a partir de una lista plana de issues.
     */
    private List<JiraIssueResponse> construirJerarquia(List<JiraIssue> issues) {
        // Mapear todos por jiraKey para lookup rápido
        Map<String, JiraIssueResponse> porKey = issues.stream()
                .collect(Collectors.toMap(JiraIssue::getJiraKey, this::toResponse));

        List<JiraIssueResponse> raices = new ArrayList<>();

        for (JiraIssue issue : issues) {
            JiraIssueResponse response = porKey.get(issue.getJiraKey());
            if (issue.getParentKey() != null && porKey.containsKey(issue.getParentKey())) {
                // Es hijo de otro issue en la lista: añadir a subtareas del padre
                JiraIssueResponse padre = porKey.get(issue.getParentKey());
                List<JiraIssueResponse> subtareasActualizadas = new ArrayList<>(padre.subtareas());
                subtareasActualizadas.add(response);
                porKey.put(issue.getParentKey(), new JiraIssueResponse(
                        padre.id(), padre.jiraKey(), padre.summary(), padre.tipoJira(),
                        padre.estadoJira(), padre.estadoKaos(), padre.asignadoJira(),
                        padre.personaId(), padre.personaNombre(), padre.estimacionHoras(),
                        padre.horasConsumidas(), padre.parentKey(), padre.tieneTarea(),
                        padre.tareaId(), subtareasActualizadas));
            } else {
                // Es issue raíz
                raices.add(response);
            }
        }

        // Reemplazar raíces con versión actualizada (con subtareas)
        return raices.stream()
                .map(r -> porKey.getOrDefault(r.jiraKey(), r))
                .toList();
    }

    /**
     * Mapea un {@link JiraIssue} a un {@link JiraIssueResponse} (sin subtareas inicialmente).
     */
    private JiraIssueResponse toResponse(JiraIssue issue) {
        return new JiraIssueResponse(
                issue.getId(),
                issue.getJiraKey(),
                issue.getSummary(),
                issue.getTipoJira(),
                issue.getEstadoJira(),
                issue.getEstadoKaos(),
                issue.getAsignadoJira(),
                issue.getPersona() != null ? issue.getPersona().getId() : null,
                issue.getPersona() != null ? issue.getPersona().getNombre() : null,
                issue.getEstimacionHoras(),
                issue.getHorasConsumidas(),
                issue.getParentKey(),
                issue.getTarea() != null,
                issue.getTarea() != null ? issue.getTarea().getId() : null,
                new ArrayList<>()
        );
    }

    /**
     * Crea y persiste una {@link Tarea} a partir de un item de planificación.
     * Enlaza tareaParent si el issue tiene parentKey y la tarea padre ya fue creada en este lote.
     */
    private Tarea crearTareaDesdeItem(PlanificarAsignacionItem item, JiraIssue issue,
                                      Sprint sprint, List<Tarea> creadasEnLote) {
        Tarea tarea = new Tarea();
        tarea.setSprint(sprint);
        tarea.setTitulo(issue.getSummary());
        tarea.setTipo(parseTipoTarea(item.tipo(), issue.getTipoJira()));
        tarea.setCategoria(parseCategoria(item.categoria(), issue.getTipoJira()));
        tarea.setPrioridad(parsePrioridad(item.prioridad()));
        BigDecimal est = item.estimacion() != null ? item.estimacion() : issue.getEstimacionHoras();
        tarea.setEstimacion(est != null ? est : BigDecimal.ONE);
        tarea.setEstado(EstadoTarea.PENDIENTE);
        tarea.setJiraKey(issue.getJiraKey());
        tarea.setJiraIssue(issue);
        tarea.setEsDeJira(true);

        if (item.personaId() != null) {
            tarea.setPersona(personaRepository.getReferenceById(item.personaId()));
        } else if (issue.getPersona() != null) {
            tarea.setPersona(issue.getPersona());
        }

        if (item.diaAsignado() != null) {
            tarea.setDiaAsignado(item.diaAsignado());
        }

        // Vincular jerarquía: buscar tareaParent en el lote actual
        if (issue.getParentKey() != null) {
            creadasEnLote.stream()
                    .filter(t -> issue.getParentKey().equals(t.getJiraKey()))
                    .findFirst()
                    .ifPresent(tarea::setTareaParent);
        }

        return tareaRepository.save(tarea);
    }

    /**
     * Valida que el sprint está en un estado que permite planificación.
     */
    private void validarEstadoSprint(Sprint sprint) {
        if (sprint.getEstado() == null) {
            throw new SprintNoEnPlanificacionException(sprint.getId(), "null");
        }
        switch (sprint.getEstado()) {
            case PLANIFICACION, ACTIVO -> {
                // OK
            }
            default -> throw new SprintNoEnPlanificacionException(sprint.getId(), sprint.getEstado().toString());
        }
    }

    /**
     * Parsea el tipo de tarea desde el string del request o lo deriva del tipo Jira.
     */
    private TipoTarea parseTipoTarea(String tipo, String tipoJira) {
        if (tipo != null) {
            try { return TipoTarea.valueOf(tipo.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        if (tipoJira == null) return TipoTarea.TAREA;
        return switch (tipoJira.toLowerCase()) {
            case "bug", "defect", "incidente" -> TipoTarea.BUG;
            case "spike" -> TipoTarea.SPIKE;
            case "historia", "story", "user story" -> TipoTarea.HISTORIA;
            default -> TipoTarea.TAREA;
        };
    }

    /**
     * Parsea la categoría desde el string del request o la deriva del tipo Jira.
     */
    private Categoria parseCategoria(String categoria, String tipoJira) {
        if (categoria != null) {
            try { return Categoria.valueOf(categoria.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        if (tipoJira == null) return Categoria.EVOLUTIVO;
        return switch (tipoJira.toLowerCase()) {
            case "bug", "defect", "incidente" -> Categoria.CORRECTIVO;
            default -> Categoria.EVOLUTIVO;
        };
    }

    /**
     * Parsea la prioridad desde el string del request.
     */
    private Prioridad parsePrioridad(String prioridad) {
        if (prioridad != null) {
            try { return Prioridad.valueOf(prioridad.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        return Prioridad.NORMAL;
    }
}
