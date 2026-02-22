package com.kaos.calendario.controller;

import com.kaos.calendario.dto.VacacionResponse;
import com.kaos.calendario.service.VacacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller REST para vacaciones de un squad específico.
 * Endpoints: /api/v1/squads/{squadId}/vacaciones
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/squads/{squadId}/vacaciones")
@RequiredArgsConstructor
@Tag(name = "Vacacion", description = "Gestión de vacaciones y permisos")
public class SquadVacacionController {

    private final VacacionService service;

    @GetMapping
    @Operation(summary = "Lista vacaciones de un squad con filtro opcional de fechas")
    public ResponseEntity<List<VacacionResponse>> listarVacacionesSquad(
            @PathVariable Long squadId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.info("GET /api/v1/squads/{}/vacaciones?fechaInicio={}&fechaFin={}", squadId, fechaInicio, fechaFin);
        return ResponseEntity.ok(service.listarPorSquad(squadId, fechaInicio, fechaFin));
    }
}
