package com.kaos.planificacion.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.kaos.planificacion.dto.DashboardSprintResponse;
import com.kaos.planificacion.dto.TimelineSprintResponse;
import com.kaos.planificacion.service.PlanificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para Planificación.
 * Proporciona dashboard y timeline para análisis de sprints.
 */
@RestController
@RequestMapping("/api/v1/planificacion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Planificacion", description = "Dashboard y timeline para análisis de sprints")
public class PlanificacionController {

    private final PlanificacionService planificacionService;

    /**
     * Obtiene el dashboard de un sprint.
     * Incluye métricas, ocupación de capacidad y alertas.
     * GET /api/v1/planificacion/{sprintId}/dashboard
     */
    @GetMapping("/{sprintId}/dashboard")
    @Operation(summary = "Obtiene el dashboard con métricas del sprint")
    public ResponseEntity<DashboardSprintResponse> obtenerDashboard(@PathVariable Long sprintId) {
        log.debug("GET /api/v1/planificacion/{}/dashboard", sprintId);
        DashboardSprintResponse dashboard = planificacionService.obtenerDashboard(sprintId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Obtiene la timeline (grid) de un sprint.
     * Matriz [personas] x [días] con tareas asignadas.
     * GET /api/v1/planificacion/{sprintId}/timeline
     */
    @GetMapping("/{sprintId}/timeline")
    @Operation(summary = "Obtiene la timeline (grid personas x días) del sprint")
    public ResponseEntity<TimelineSprintResponse> obtenerTimeline(@PathVariable Long sprintId) {
        log.debug("GET /api/v1/planificacion/{}/timeline", sprintId);
        TimelineSprintResponse timeline = planificacionService.obtenerTimeline(sprintId);
        return ResponseEntity.ok(timeline);
    }

    /**
     * Exporta la timeline del sprint a Excel.
     * GET /api/v1/planificacion/{sprintId}/timeline/export
     */
    @GetMapping("/{sprintId}/timeline/export")
    @Operation(summary = "Exporta la timeline a Excel")
    public ResponseEntity<byte[]> exportarTimeline(@PathVariable Long sprintId) {
        log.debug("GET /api/v1/planificacion/{}/timeline/export", sprintId);
        byte[] file = planificacionService.exportarTimelineExcel(sprintId);
        String filename = "timeline-sprint-" + sprintId + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}
