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
import com.kaos.calendario.dto.FestivoResponse;
import com.kaos.calendario.service.FestivoService;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para endpoints de Festivos por Persona.
 * Lista festivos según la ciudad de la persona.
 */
@RestController
@RequestMapping("/api/v1/personas/{personaId}/festivos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Festivo", description = "Gestión de festivos por ciudad")
public class PersonaFestivoController {

    private final FestivoService service;
    private final PersonaRepository personaRepository;

    @GetMapping
    @Operation(summary = "Lista festivos de la ciudad de una persona")
    public ResponseEntity<List<FestivoResponse>> listarFestivosPorPersona(
            @PathVariable Long personaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.debug("GET /api/v1/personas/{}/festivos - fechaInicio: {}, fechaFin: {}", 
                personaId, fechaInicio, fechaFin);
        
        // Obtener ciudad de la persona
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada con id: " + personaId));
        
        // Listar festivos por ciudad
        return ResponseEntity.ok(service.listarPorCiudad(persona.getCiudad(), fechaInicio, fechaFin));
    }
}
