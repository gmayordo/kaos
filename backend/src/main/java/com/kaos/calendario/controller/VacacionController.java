package com.kaos.calendario.controller;

import com.kaos.calendario.dto.VacacionRequest;
import com.kaos.calendario.dto.VacacionResponse;
import com.kaos.calendario.service.VacacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para operaciones CRUD de Vacacion.
 * Endpoints: /api/v1/vacaciones
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vacaciones")
@RequiredArgsConstructor
@Tag(name = "Vacacion", description = "Gestión de vacaciones y permisos")
public class VacacionController {

    private final VacacionService service;

    @GetMapping
    @Operation(summary = "Lista vacaciones con filtros opcionales")
    public ResponseEntity<List<VacacionResponse>> listarVacaciones(
            @RequestParam(required = false) Long personaId,
            @RequestParam(required = false) Long squadId) {
        log.info("GET /api/v1/vacaciones?personaId={}&squadId={}", personaId, squadId);
        return ResponseEntity.ok(service.listar(personaId, squadId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una vacación por ID")
    public ResponseEntity<VacacionResponse> obtenerVacacion(@PathVariable Long id) {
        log.info("GET /api/v1/vacaciones/{}", id);
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva vacación")
    public ResponseEntity<VacacionResponse> crearVacacion(@Valid @RequestBody VacacionRequest request) {
        log.info("POST /api/v1/vacaciones");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una vacación existente")
    public ResponseEntity<VacacionResponse> actualizarVacacion(
            @PathVariable Long id,
            @Valid @RequestBody VacacionRequest request) {
        log.info("PUT /api/v1/vacaciones/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una vacación")
    public ResponseEntity<Void> eliminarVacacion(@PathVariable Long id) {
        log.info("DELETE /api/v1/vacaciones/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
