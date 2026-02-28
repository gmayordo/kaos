package com.kaos.jira.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kaos.jira.dto.JiraIssueResponse;
import com.kaos.jira.dto.PlanificarIssueRequest;
import com.kaos.jira.service.PlanificarIssueService;
import com.kaos.planificacion.dto.TareaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para planificación de issues Jira en sprints KAOS.
 *
 * <p>Permite listar issues planificables con jerarquía y convertirlos
 * en Tareas KAOS asociadas a un sprint.</p>
 *
 * <p>Base URL: {@code /api/v1/jira/issues}</p>
 */
@RestController
@RequestMapping("/api/v1/jira/issues")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "JiraIssues", description = "Planificación de issues Jira en sprints KAOS")
public class JiraIssueController {

    private final PlanificarIssueService planificarIssueService;

    // ── GET — Listado de issues planificables ─────────────────────────────────

    /**
     * Lista los issues Jira de un squad en un sprint, organizados en jerarquía.
     *
     * @param squadId      ID del squad
     * @param sprintId     ID del sprint KAOS
     * @param soloSinTarea si true (por defecto), excluye issues que ya tienen Tarea KAOS
     * @return Lista de issues con subtareas anidadas
     */
    @GetMapping
    @Operation(
            summary = "Lista issues Jira planificables",
            description = "Devuelve issues del squad en el sprint, organizados en jerarquía (parent → subtareas). " +
                    "Con soloSinTarea=true se excluyen los ya planificados."
    )
    public ResponseEntity<List<JiraIssueResponse>> listar(
            @RequestParam Long squadId,
            @RequestParam Long sprintId,
            @RequestParam(defaultValue = "true") boolean soloSinTarea) {
        log.info("GET /api/v1/jira/issues - squadId: {}, sprintId: {}, soloSinTarea: {}",
                squadId, sprintId, soloSinTarea);
        List<JiraIssueResponse> issues = planificarIssueService.listarIssuesPlanificables(squadId, sprintId, soloSinTarea);
        return ResponseEntity.ok(issues);
    }

    // ── POST /planificar — Crear tareas KAOS desde issues ───────────────────

    /**
     * Planifica uno o varios issues Jira como Tareas KAOS en un sprint.
     *
     * <p>Cada asignación especifica el issue (jiraKey), la persona, la estimación
     * y el día del sprint. Se vincula automáticamente la jerarquía de tareas.</p>
     *
     * @param request Request con sprintId y lista de asignaciones
     * @return Lista de TareaResponse creadas (HTTP 201)
     */
    @PostMapping("/planificar")
    @Operation(
            summary = "Planifica issues Jira como Tareas KAOS",
            description = "Crea Tareas KAOS a partir de issues Jira, vinculando jerarquía y asignación. " +
                    "Devuelve las tareas creadas."
    )
    public ResponseEntity<List<TareaResponse>> planificar(@Valid @RequestBody PlanificarIssueRequest request) {
        log.info("POST /api/v1/jira/issues/planificar - sprint: {}, issues: {}",
                request.sprintId(), request.asignaciones().size());
        List<TareaResponse> tareas = planificarIssueService.planificar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tareas);
    }
}
