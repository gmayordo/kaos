package com.kaos.calendario.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kaos.calendario.dto.AusenciaResponse;
import com.kaos.calendario.service.AusenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para ausencias de un squad específico.
 * Endpoints: /api/v1/squads/{squadId}/ausencias
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/squads/{squadId}/ausencias")
@RequiredArgsConstructor
@Tag(name = "Ausencia", description = "Gestión de ausencias y bajas médicas")
public class SquadAusenciaController {

    private final AusenciaService service;

    @GetMapping
    @Operation(summary = "Lista ausencias de un squad con filtro opcional de fechas")
    public ResponseEntity<List<AusenciaResponse>> listarAusenciasSquad(
            @PathVariable Long squadId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.info("GET /api/v1/squads/{}/ausencias?fechaInicio={}&fechaFin={}", squadId, fechaInicio, fechaFin);
        return ResponseEntity.ok(service.listarPorSquad(squadId, fechaInicio, fechaFin));
    }
}
