package com.kaos.squad.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaos.squad.dto.SquadRequest;
import com.kaos.squad.dto.SquadResponse;
import com.kaos.squad.entity.EstadoSquad;
import com.kaos.squad.service.SquadService;

/**
 * Controller REST para Squads.
 */
@RestController
@RequestMapping("/api/v1/squads")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Squad", description = "Gestión de equipos de desarrollo (squads)")
public class SquadController {

    private final SquadService service;

    @GetMapping
    @Operation(summary = "Lista squads con filtro opcional por estado")
    public ResponseEntity<List<SquadResponse>> listarSquads(
            @RequestParam(required = false) EstadoSquad estado) {
        log.debug("GET /api/v1/squads - estado: {}", estado);
        return ResponseEntity.ok(service.listar(estado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un squad por ID")
    public ResponseEntity<SquadResponse> obtenerSquad(@PathVariable Long id) {
        log.debug("GET /api/v1/squads/{}", id);
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea un nuevo squad")
    public ResponseEntity<SquadResponse> crearSquad(@Valid @RequestBody SquadRequest request) {
        log.info("POST /api/v1/squads");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un squad")
    public ResponseEntity<SquadResponse> actualizarSquad(
            @PathVariable Long id,
            @Valid @RequestBody SquadRequest request) {
        log.info("PUT /api/v1/squads/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactiva un squad (soft delete)")
    public ResponseEntity<SquadResponse> desactivarSquad(@PathVariable Long id) {
        log.info("PATCH /api/v1/squads/{}/desactivar", id);
        return ResponseEntity.ok(service.desactivar(id));
    }
}
