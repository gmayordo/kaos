package com.kaos.calendario.controller;

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
import com.kaos.calendario.dto.AusenciaRequest;
import com.kaos.calendario.dto.AusenciaResponse;
import com.kaos.calendario.service.AusenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para operaciones CRUD de Ausencia.
 * Endpoints: /api/v1/ausencias
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ausencias")
@RequiredArgsConstructor
@Tag(name = "Ausencia", description = "Gestión de ausencias y bajas médicas")
public class AusenciaController {

    private final AusenciaService service;

    @GetMapping
    @Operation(summary = "Lista ausencias con filtros opcionales")
    public ResponseEntity<List<AusenciaResponse>> listarAusencias(
            @RequestParam(required = false) Long personaId,
            @RequestParam(required = false) Long squadId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        log.info("GET /api/v1/ausencias?personaId={}&squadId={}&fechaInicio={}&fechaFin={}", 
                personaId, squadId, fechaInicio, fechaFin);
        return ResponseEntity.ok(service.listar(personaId, squadId, fechaInicio, fechaFin));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una ausencia por ID")
    public ResponseEntity<AusenciaResponse> obtenerAusencia(@PathVariable Long id) {
        log.info("GET /api/v1/ausencias/{}", id);
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva ausencia")
    public ResponseEntity<AusenciaResponse> crearAusencia(@Valid @RequestBody AusenciaRequest request) {
        log.info("POST /api/v1/ausencias");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una ausencia existente")
    public ResponseEntity<AusenciaResponse> actualizarAusencia(
            @PathVariable Long id,
            @Valid @RequestBody AusenciaRequest request) {
        log.info("PUT /api/v1/ausencias/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una ausencia")
    public ResponseEntity<Void> eliminarAusencia(@PathVariable Long id) {
        log.info("DELETE /api/v1/ausencias/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
