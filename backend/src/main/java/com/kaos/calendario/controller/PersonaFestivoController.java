package com.kaos.calendario.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaos.calendario.dto.FestivoResponse;
import com.kaos.calendario.service.FestivoService;

/**
 * Controller REST para endpoints de Festivos por Persona.
 * Separado del FestivoController para organización.
 */
@RestController
@RequestMapping("/api/v1/personas/{personaId}/festivos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Festivo", description = "Gestión de festivos con asignación a personas")
public class PersonaFestivoController {

    private final FestivoService service;

    @GetMapping
    @Operation(summary = "Lista festivos asignados a una persona")
    public ResponseEntity<List<FestivoResponse>> listarFestivosPorPersona(
            @PathVariable Long personaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.debug("GET /api/v1/personas/{}/festivos - fechaInicio: {}, fechaFin: {}", 
                personaId, fechaInicio, fechaFin);
        return ResponseEntity.ok(service.listarPorPersona(personaId, fechaInicio, fechaFin));
    }
}
