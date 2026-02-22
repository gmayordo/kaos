package com.kaos.calendario.controller;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kaos.calendario.dto.CapacidadSquadResponse;
import com.kaos.calendario.service.CapacidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para cálculo de capacidad.
 * Endpoints: /api/v1/capacidad
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/capacidad")
@RequiredArgsConstructor
@Tag(name = "Capacidad", description = "Cálculo de capacidad disponible")
public class CapacidadController {

    private final CapacidadService service;

    @GetMapping("/squad/{squadId}")
    @Operation(summary = "Calcula capacidad disponible de un squad en un rango de fechas")
    public ResponseEntity<CapacidadSquadResponse> calcularCapacidadSquad(
            @PathVariable Long squadId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.info("GET /api/v1/capacidad/squad/{}?fechaInicio={}&fechaFin={}", squadId, fechaInicio, fechaFin);
        return ResponseEntity.ok(service.calcularCapacidad(squadId, fechaInicio, fechaFin));
    }
}
