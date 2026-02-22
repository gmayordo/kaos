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
import com.kaos.planificacion.dto.BloqueoRequest;
import com.kaos.planificacion.dto.BloqueoResponse;
import com.kaos.planificacion.entity.EstadoBloqueo;
import com.kaos.planificacion.service.BloqueoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para Bloqueos (impedimentos).
 * Gestión de impedimentos que afectan tareas en sprints.
 */
@RestController
@RequestMapping("/api/v1/bloqueos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bloqueo", description = "Gestión de impedimentos (bloqueos) en tareas")
public class BloqueoController {

    private final BloqueoService bloqueoService;

    /**
     * Lista bloqueos con filtro opcional de estado.
     * GET /api/v1/bloqueos?estado=ABIERTO&page=0&size=20
     */
    @GetMapping
    @Operation(summary = "Lista bloqueos con filtro opcional por estado")
    public ResponseEntity<Page<BloqueoResponse>> listarBloqueos(
            @RequestParam(required = false) EstadoBloqueo estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/v1/bloqueos - estado: {}, page: {}", estado, page);
        Page<BloqueoResponse> bloqueos = bloqueoService.listar(estado, PageRequest.of(page, size));
        return ResponseEntity.ok(bloqueos);
    }

    /**
     * Obtiene un bloqueo por su ID.
     * GET /api/v1/bloqueos/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un bloqueo por ID")
    public ResponseEntity<BloqueoResponse> obtenerBloqueo(@PathVariable Long id) {
        log.debug("GET /api/v1/bloqueos/{}", id);
        BloqueoResponse bloqueo = bloqueoService.obtener(id);
        return ResponseEntity.ok(bloqueo);
    }

    /**
     * Crea un nuevo bloqueo.
     * POST /api/v1/bloqueos
     */
    @PostMapping
    @Operation(summary = "Crea un nuevo bloqueo (impedimento)")
    public ResponseEntity<BloqueoResponse> crearBloqueo(@Valid @RequestBody BloqueoRequest request) {
        log.info("POST /api/v1/bloqueos - titulo: {}", request.titulo());
        BloqueoResponse bloqueo = bloqueoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(bloqueo);
    }

    /**
     * Actualiza un bloqueo existente.
     * PATCH /api/v1/bloqueos/{id}
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Actualiza un bloqueo")
    public ResponseEntity<BloqueoResponse> actualizarBloqueo(
            @PathVariable Long id,
            @Valid @RequestBody BloqueoRequest request) {
        log.info("PATCH /api/v1/bloqueos/{}", id);
        BloqueoResponse bloqueo = bloqueoService.actualizar(id, request);
        return ResponseEntity.ok(bloqueo);
    }

    /**
     * Cambia el estado de un bloqueo.
     * PATCH /api/v1/bloqueos/{id}/estado
     */
    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado del bloqueo (ABIERTO→EN_GESTION→RESUELTO)")
    public ResponseEntity<BloqueoResponse> cambiarEstadoBloqueo(
            @PathVariable Long id,
            @RequestParam EstadoBloqueo estado) {
        log.info("PATCH /api/v1/bloqueos/{}/estado - nuevoEstado: {}", id, estado);
        BloqueoResponse bloqueo = bloqueoService.cambiarEstado(id, estado);
        return ResponseEntity.ok(bloqueo);
    }

    /**
     * Elimina un bloqueo.
     * DELETE /api/v1/bloqueos/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un bloqueo")
    public ResponseEntity<Void> eliminarBloqueo(@PathVariable Long id) {
        log.info("DELETE /api/v1/bloqueos/{}", id);
        bloqueoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cuenta bloqueos activos (ABIERTO o EN_GESTION).
     * GET /api/v1/bloqueos/activos/count
     */
    @GetMapping("/activos/count")
    @Operation(summary = "Cuenta bloqueos activos (ABIERTO o EN_GESTION)")
    public ResponseEntity<Long> contarActivos() {
        log.debug("GET /api/v1/bloqueos/activos/count");
        Long count = bloqueoService.contarActivos();
        return ResponseEntity.ok(count);
    }
}
