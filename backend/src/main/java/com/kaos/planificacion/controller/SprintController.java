package com.kaos.planificacion.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kaos.planificacion.dto.SprintRequest;
import com.kaos.planificacion.dto.SprintResponse;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.service.SprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para Sprints.
 * Gestión de sprints de planificación.
 */
@RestController
@RequestMapping("/api/v1/sprints")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sprint", description = "Gestión de sprints de planificación")
public class SprintController {

    private final SprintService sprintService;

    /**
     * Lista sprints con filtros opcionales.
     * GET /api/v1/sprints?squadId=1&estado=ACTIVO&page=0&size=20
     */
    @GetMapping
    @Operation(summary = "Lista sprints con filtros opcionales")
    public ResponseEntity<Page<SprintResponse>> listarSprints(
            @RequestParam(required = false) Long squadId,
            @RequestParam(required = false) SprintEstado estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/v1/sprints - squadId: {}, estado: {}, page: {}, size: {}", squadId, estado, page, size);
        Page<SprintResponse> sprints = sprintService.listar(squadId, estado, PageRequest.of(page, size));
        return ResponseEntity.ok(sprints);
    }

    /**
     * Obtiene un sprint por su ID.
     * GET /api/v1/sprints/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un sprint por ID")
    public ResponseEntity<SprintResponse> obtenerSprint(@PathVariable Long id) {
        log.debug("GET /api/v1/sprints/{}", id);
        SprintResponse sprint = sprintService.obtener(id);
        return ResponseEntity.ok(sprint);
    }

    /**
     * Crea un nuevo sprint.
     * POST /api/v1/sprints
     */
    @PostMapping
    @Operation(summary = "Crea un nuevo sprint")
    public ResponseEntity<SprintResponse> crearSprint(@Valid @RequestBody SprintRequest request) {
        log.info("POST /api/v1/sprints - nombre: {}", request.nombre());
        SprintResponse sprint = sprintService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sprint);
    }

    /**
     * Actualiza un sprint existente.
     * PATCH /api/v1/sprints/{id}
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Actualiza un sprint (solo si está en PLANIFICACION)")
    public ResponseEntity<SprintResponse> actualizarSprint(
            @PathVariable Long id,
            @Valid @RequestBody SprintRequest request) {
        log.info("PATCH /api/v1/sprints/{}", id);
        SprintResponse sprint = sprintService.actualizar(id, request);
        return ResponseEntity.ok(sprint);
    }

    /**
     * Cambia el estado de un sprint.
     * PATCH /api/v1/sprints/{id}/estado
     */
    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado del sprint (PLANIFICACION→ACTIVO→CERRADO)")
    public ResponseEntity<List<SprintResponse>> cambiarEstadoSprint(
            @PathVariable Long id,
            @RequestParam SprintEstado estado) {
        log.info("PATCH /api/v1/sprints/{}/estado - nuevoEstado: {}", id, estado);
        List<SprintResponse> sprints = sprintService.cambiarEstado(id, estado);
        return ResponseEntity.ok(sprints);
    }

    /**
     * Elimina un sprint (solo si está en PLANIFICACION).
     * DELETE /api/v1/sprints/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un sprint (solo si está en PLANIFICACION)")
    public ResponseEntity<Void> eliminarSprint(@PathVariable Long id) {
        log.info("DELETE /api/v1/sprints/{}", id);
        sprintService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
