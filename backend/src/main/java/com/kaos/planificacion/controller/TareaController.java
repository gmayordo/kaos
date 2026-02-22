package com.kaos.planificacion.controller;

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
import com.kaos.planificacion.dto.TareaRequest;
import com.kaos.planificacion.dto.TareaResponse;
import com.kaos.planificacion.entity.EstadoTarea;
import com.kaos.planificacion.service.TareaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para Tareas.
 * Gestión de tareas dentro de sprints de planificación.
 */
@RestController
@RequestMapping("/api/v1/tareas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tarea", description = "Gestión de tareas dentro de sprints")
public class TareaController {

    private final TareaService tareaService;

    /**
     * Lista tareas con filtros opcionales.
     * GET /api/v1/tareas?sprintId=1&personaId=2&estado=PENDIENTE&page=0&size=20
     */
    @GetMapping
    @Operation(summary = "Lista tareas con filtros opcionales")
    public ResponseEntity<Page<TareaResponse>> listarTareas(
            @RequestParam(required = false) Long sprintId,
            @RequestParam(required = false) Long personaId,
            @RequestParam(required = false) EstadoTarea estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/v1/tareas - sprintId: {}, personaId: {}, estado: {}, page: {}", 
                sprintId, personaId, estado, page);
        Page<TareaResponse> tareas = tareaService.listar(sprintId, personaId, estado, PageRequest.of(page, size));
        return ResponseEntity.ok(tareas);
    }

    /**
     * Obtiene una tarea por su ID.
     * GET /api/v1/tareas/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una tarea por ID")
    public ResponseEntity<TareaResponse> obtenerTarea(@PathVariable Long id) {
        log.debug("GET /api/v1/tareas/{}", id);
        TareaResponse tarea = tareaService.obtener(id);
        return ResponseEntity.ok(tarea);
    }

    /**
     * Crea una nueva tarea.
     * POST /api/v1/tareas
     */
    @PostMapping
    @Operation(summary = "Crea una nueva tarea")
    public ResponseEntity<TareaResponse> crearTarea(@Valid @RequestBody TareaRequest request) {
        log.info("POST /api/v1/tareas - titulo: {}", request.titulo());
        TareaResponse tarea = tareaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tarea);
    }

    /**
     * Actualiza una tarea existente.
     * PATCH /api/v1/tareas/{id}
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Actualiza una tarea (valida capacidad si cambia asignación)")
    public ResponseEntity<TareaResponse> actualizarTarea(
            @PathVariable Long id,
            @Valid @RequestBody TareaRequest request) {
        log.info("PATCH /api/v1/tareas/{}", id);
        TareaResponse tarea = tareaService.actualizar(id, request);
        return ResponseEntity.ok(tarea);
    }

    /**
     * Cambia el estado de una tarea.
     * PATCH /api/v1/tareas/{id}/estado
     */
    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado de la tarea (máquina de estados)")
    public ResponseEntity<TareaResponse> cambiarEstadoTarea(
            @PathVariable Long id,
            @RequestParam EstadoTarea estado) {
        log.info("PATCH /api/v1/tareas/{}/estado - nuevoEstado: {}", id, estado);
        TareaResponse tarea = tareaService.cambiarEstado(id, estado);
        return ResponseEntity.ok(tarea);
    }

    /**
     * Elimina una tarea (solo si está en PENDIENTE).
     * DELETE /api/v1/tareas/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una tarea (solo si está en PENDIENTE)")
    public ResponseEntity<Void> eliminarTarea(@PathVariable Long id) {
        log.info("DELETE /api/v1/tareas/{}", id);
        tareaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
