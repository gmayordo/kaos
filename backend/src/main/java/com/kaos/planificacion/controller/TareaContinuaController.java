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
import com.kaos.planificacion.dto.TareaContinuaRequest;
import com.kaos.planificacion.dto.TareaContinuaResponse;
import com.kaos.planificacion.service.TareaContinuaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para tareas continuas multi-sprint (tipo Gantt).
 * Gestiona tareas de larga duración como seguimiento, formación, etc.
 */
@RestController
@RequestMapping("/api/v1/tareas-continuas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "TareasContinuas", description = "Tareas de larga duración que cruzan múltiples sprints (Gantt)")
public class TareaContinuaController {

    private final TareaContinuaService service;

    /**
     * Lista tareas continuas activas de un squad.
     * GET /api/v1/tareas-continuas?squadId=1
     */
    @GetMapping
    @Operation(summary = "Lista tareas continuas activas de un squad")
    public ResponseEntity<List<TareaContinuaResponse>> listar(@RequestParam Long squadId) {
        log.debug("GET /api/v1/tareas-continuas?squadId={}", squadId);
        return ResponseEntity.ok(service.listarPorSquad(squadId));
    }

    /**
     * Obtiene una tarea continua por ID.
     * GET /api/v1/tareas-continuas/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una tarea continua por ID")
    public ResponseEntity<TareaContinuaResponse> obtener(@PathVariable Long id) {
        log.debug("GET /api/v1/tareas-continuas/{}", id);
        return ResponseEntity.ok(service.obtener(id));
    }

    /**
     * Crea una nueva tarea continua.
     * POST /api/v1/tareas-continuas
     */
    @PostMapping
    @Operation(summary = "Crea una nueva tarea continua")
    public ResponseEntity<TareaContinuaResponse> crear(@Valid @RequestBody TareaContinuaRequest request) {
        log.info("POST /api/v1/tareas-continuas - titulo: {}", request.titulo());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    /**
     * Actualiza una tarea continua.
     * PUT /api/v1/tareas-continuas/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una tarea continua")
    public ResponseEntity<TareaContinuaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TareaContinuaRequest request) {
        log.info("PUT /api/v1/tareas-continuas/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    /**
     * Elimina (soft delete) una tarea continua.
     * DELETE /api/v1/tareas-continuas/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina (soft delete) una tarea continua")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/v1/tareas-continuas/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
