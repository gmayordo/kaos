package com.kaos.planificacion.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kaos.planificacion.dto.TareaAsignacionTimelineRequest;
import com.kaos.planificacion.dto.TareaAsignacionTimelineResponse;
import com.kaos.planificacion.service.TareaAsignacionTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para asignaciones de tareas padre en el timeline.
 * Permite vincular issues Jira padre (HISTORIA) a personas con rango de días.
 */
@RestController
@RequestMapping("/api/v1/timeline-asignaciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "TimelineAsignaciones", description = "Asignaciones de tareas padre Jira en el timeline con rango de días")
public class TareaAsignacionTimelineController {

    private final TareaAsignacionTimelineService service;

    /**
     * Lista asignaciones de un sprint.
     * GET /api/v1/timeline-asignaciones?sprintId=1
     */
    @GetMapping
    @Operation(summary = "Lista asignaciones de tareas padre para un sprint")
    public ResponseEntity<List<TareaAsignacionTimelineResponse>> listar(
            @RequestParam Long sprintId) {
        log.debug("GET /api/v1/timeline-asignaciones?sprintId={}", sprintId);
        return ResponseEntity.ok(service.listarPorSprint(sprintId));
    }

    /**
     * Obtiene una asignación por ID.
     * GET /api/v1/timeline-asignaciones/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una asignación de timeline por ID")
    public ResponseEntity<TareaAsignacionTimelineResponse> obtener(@PathVariable Long id) {
        log.debug("GET /api/v1/timeline-asignaciones/{}", id);
        return ResponseEntity.ok(service.obtener(id));
    }

    /**
     * Crea una asignación de tarea padre en el timeline.
     * POST /api/v1/timeline-asignaciones
     */
    @PostMapping
    @Operation(summary = "Crea una asignación de tarea padre en el timeline")
    public ResponseEntity<TareaAsignacionTimelineResponse> crear(
            @Valid @RequestBody TareaAsignacionTimelineRequest request) {
        log.info("POST /api/v1/timeline-asignaciones - tarea: {}, persona: {}", request.tareaId(), request.personaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    /**
     * Actualiza una asignación existente.
     * PUT /api/v1/timeline-asignaciones/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una asignación de timeline")
    public ResponseEntity<TareaAsignacionTimelineResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TareaAsignacionTimelineRequest request) {
        log.info("PUT /api/v1/timeline-asignaciones/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    /**
     * Elimina una asignación.
     * DELETE /api/v1/timeline-asignaciones/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una asignación de timeline")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/v1/timeline-asignaciones/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
