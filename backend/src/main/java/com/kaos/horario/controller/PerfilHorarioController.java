package com.kaos.horario.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaos.horario.dto.PerfilHorarioRequest;
import com.kaos.horario.dto.PerfilHorarioResponse;
import com.kaos.horario.service.PerfilHorarioService;

/**
 * Controller REST para Perfiles de Horario.
 */
@RestController
@RequestMapping("/api/v1/perfiles-horario")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PerfilHorario", description = "Gestión de perfiles de horario laboral")
public class PerfilHorarioController {

    private final PerfilHorarioService service;

    @GetMapping
    @Operation(summary = "Lista todos los perfiles de horario")
    public ResponseEntity<List<PerfilHorarioResponse>> listarPerfilesHorario() {
        log.debug("GET /api/v1/perfiles-horario");
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un perfil de horario por ID")
    public ResponseEntity<PerfilHorarioResponse> obtenerPerfilHorario(@PathVariable Long id) {
        log.debug("GET /api/v1/perfiles-horario/{}", id);
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea un nuevo perfil de horario")
    public ResponseEntity<PerfilHorarioResponse> crearPerfilHorario(@Valid @RequestBody PerfilHorarioRequest request) {
        log.info("POST /api/v1/perfiles-horario");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un perfil de horario")
    public ResponseEntity<PerfilHorarioResponse> actualizarPerfilHorario(
            @PathVariable Long id,
            @Valid @RequestBody PerfilHorarioRequest request) {
        log.info("PUT /api/v1/perfiles-horario/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un perfil de horario (si no tiene personas asignadas)")
    public ResponseEntity<Void> eliminarPerfilHorario(@PathVariable Long id) {
        log.info("DELETE /api/v1/perfiles-horario/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
