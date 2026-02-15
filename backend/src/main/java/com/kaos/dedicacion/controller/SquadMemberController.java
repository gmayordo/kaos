package com.kaos.dedicacion.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaos.dedicacion.dto.SquadMemberRequest;
import com.kaos.dedicacion.dto.SquadMemberResponse;
import com.kaos.dedicacion.service.SquadMemberService;

/**
 * Controller REST para asignaciones persona-squad (dedicación).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SquadMember", description = "Gestión de asignaciones persona-squad y dedicación")
public class SquadMemberController {

    private final SquadMemberService service;

    @GetMapping("/api/v1/squads/{squadId}/miembros")
    @Operation(summary = "Lista miembros de un squad")
    public ResponseEntity<List<SquadMemberResponse>> listarMiembrosSquad(@PathVariable Long squadId) {
        log.debug("GET /api/v1/squads/{}/miembros", squadId);
        return ResponseEntity.ok(service.listarMiembrosSquad(squadId));
    }

    @GetMapping("/api/v1/personas/{personaId}/squads")
    @Operation(summary = "Lista squads a los que pertenece una persona")
    public ResponseEntity<List<SquadMemberResponse>> listarSquadsDePersona(@PathVariable Long personaId) {
        log.debug("GET /api/v1/personas/{}/squads", personaId);
        return ResponseEntity.ok(service.listarSquadsDePersona(personaId));
    }

    @PostMapping("/api/v1/squad-members")
    @Operation(summary = "Asigna una persona a un squad con rol y porcentaje")
    public ResponseEntity<SquadMemberResponse> asignarPersonaASquad(
            @Valid @RequestBody SquadMemberRequest request) {
        log.info("POST /api/v1/squad-members");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.asignar(request));
    }

    @PutMapping("/api/v1/squad-members/{id}")
    @Operation(summary = "Modifica rol o porcentaje de dedicación")
    public ResponseEntity<SquadMemberResponse> actualizarDedicacion(
            @PathVariable Long id,
            @Valid @RequestBody SquadMemberRequest request) {
        log.info("PUT /api/v1/squad-members/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/api/v1/squad-members/{id}")
    @Operation(summary = "Elimina asignación de persona a squad")
    public ResponseEntity<Void> eliminarDedicacion(@PathVariable Long id) {
        log.info("DELETE /api/v1/squad-members/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
