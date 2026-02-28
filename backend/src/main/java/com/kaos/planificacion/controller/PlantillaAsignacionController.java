package com.kaos.planificacion.controller;

import java.math.BigDecimal;
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
import com.kaos.jira.dto.PlanificarAsignacionItem;
import com.kaos.planificacion.dto.PlantillaAsignacionRequest;
import com.kaos.planificacion.dto.PlantillaAsignacionResponse;
import com.kaos.planificacion.service.PlantillaAsignacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para plantillas de asignación automática.
 * Base URL: {@code /api/v1/plantillas}
 */
@RestController
@RequestMapping("/api/v1/plantillas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Plantillas", description = "Gestión de plantillas de distribución de horas para planificación automática")
public class PlantillaAsignacionController {

    private final PlantillaAsignacionService service;

    @GetMapping
    @Operation(summary = "Lista todas las plantillas")
    public ResponseEntity<List<PlantillaAsignacionResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una plantilla por ID")
    public ResponseEntity<PlantillaAsignacionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva plantilla de asignación")
    public ResponseEntity<PlantillaAsignacionResponse> crear(@Valid @RequestBody PlantillaAsignacionRequest request) {
        log.info("POST /api/v1/plantillas - nombre: {}", request.nombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una plantilla existente (reemplaza líneas)")
    public ResponseEntity<PlantillaAsignacionResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PlantillaAsignacionRequest request) {
        log.info("PUT /api/v1/plantillas/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una plantilla")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/v1/plantillas/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Devuelve los ítems pre-calculados si existe una plantilla activa para el tipo Jira dado.
     * GET /api/v1/plantillas/aplicar?tipoJira=Story&estimacion=10
     */
    @GetMapping("/aplicar")
    @Operation(
            summary = "Aplica una plantilla a una estimación",
            description = "Devuelve PlanificarAsignacionItem con horas por rol según la plantilla activa para el tipo Jira dado."
    )
    public ResponseEntity<List<PlanificarAsignacionItem>> aplicar(
            @RequestParam String tipoJira,
            @RequestParam BigDecimal estimacion) {
        log.debug("GET /api/v1/plantillas/aplicar - tipoJira: {}, estimacion: {}", tipoJira, estimacion);
        return ResponseEntity.ok(service.aplicar(tipoJira, estimacion));
    }
}
