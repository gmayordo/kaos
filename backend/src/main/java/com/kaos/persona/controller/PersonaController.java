package com.kaos.persona.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaos.persona.dto.PersonaRequest;
import com.kaos.persona.dto.PersonaResponse;
import com.kaos.persona.entity.Rol;
import com.kaos.persona.entity.Seniority;
import com.kaos.persona.service.PersonaService;

/**
 * Controller REST para Personas.
 */
@RestController
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Persona", description = "Gestión de miembros del equipo")
public class PersonaController {

    private final PersonaService service;

    @GetMapping
    @Operation(summary = "Lista personas con paginación y filtros")
    public ResponseEntity<Page<PersonaResponse>> listarPersonas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Long squadId,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) Seniority seniority,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) Boolean activo) {
        log.debug("GET /api/v1/personas - page: {}, size: {}, squadId: {}, rol: {}", page, size, squadId, rol);
        Pageable pageable = buildPageable(page, size, sort);
        return ResponseEntity.ok(service.listar(squadId, rol, seniority, ubicacion, activo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una persona por ID")
    public ResponseEntity<PersonaResponse> obtenerPersona(@PathVariable Long id) {
        log.debug("GET /api/v1/personas/{}", id);
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva persona")
    public ResponseEntity<PersonaResponse> crearPersona(@Valid @RequestBody PersonaRequest request) {
        log.info("POST /api/v1/personas");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una persona")
    public ResponseEntity<PersonaResponse> actualizarPersona(
            @PathVariable Long id,
            @Valid @RequestBody PersonaRequest request) {
        log.info("PUT /api/v1/personas/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactiva una persona (soft delete)")
    public ResponseEntity<PersonaResponse> desactivarPersona(@PathVariable Long id) {
        log.info("PATCH /api/v1/personas/{}/desactivar", id);
        return ResponseEntity.ok(service.desactivar(id));
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String property = parts[0];
            Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, size, Sort.by(direction, property));
        }
        return PageRequest.of(page, size, Sort.by("nombre"));
    }
}
