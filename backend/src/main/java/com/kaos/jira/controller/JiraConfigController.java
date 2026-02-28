package com.kaos.jira.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.kaos.jira.config.JiraLoadMethod;
import com.kaos.jira.dto.JiraConfigRequest;
import com.kaos.jira.dto.JiraConfigResponse;
import com.kaos.jira.dto.JiraMethodRequest;
import com.kaos.jira.service.JiraConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para gestión de la configuración Jira.
 *
 * <p>Proporciona endpoints para consultar la configuración activa de un squad,
 * cambiar el método de carga en caliente y verificar la conectividad.</p>
 *
 * <p>Base URL: {@code /api/v1/jira/config}</p>
 */
@RestController
@RequestMapping("/api/v1/jira/config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "JiraConfig", description = "Gestión de la configuración de integración con Jira")
public class JiraConfigController {

    private final JiraConfigService jiraConfigService;

    // ── GET /api/v1/jira/config/{squadId} ────────────────────────────────────

    /**
     * Obtiene la configuración Jira activa de un squad.
     * El token se devuelve ofuscado ("****").
     *
     * @param squadId ID del squad
     * @return configuración activa del squad (HTTP 200) o 404 si no existe
     */
    @GetMapping("/{squadId}")
    @Operation(summary = "Obtiene la configuración Jira activa de un squad")
    public ResponseEntity<JiraConfigResponse> obtenerConfig(@PathVariable Long squadId) {
        log.debug("GET /api/v1/jira/config/{}", squadId);
        return ResponseEntity.ok(jiraConfigService.obtenerConfig(squadId));
    }

    // ── PUT /api/v1/jira/config/{squadId} ────────────────────────────────────

    /**
     * Crea o actualiza la configuración Jira de un squad.
     *
     * <p>Si no existe configuración previa para el squad, la crea.
     * Si ya existe, la actualiza. En ambos casos el token solo se modifica
     * si se envía en el body (campo {@code token} no vacío).</p>
     *
     * <p>Cuerpo de ejemplo:
     * <pre>{@code
     * {
     *   "url": "https://jira.empresa.com",
     *   "usuario": "usuario@empresa.com",
     *   "token": "ATATxxxxxxxxxxxxxxxx",
     *   "boardCorrectivoId": 42,
     *   "boardEvolutivoId": 43,
     *   "loadMethod": "API_REST",
     *   "activa": true,
     *   "mapeoEstados": "{\"Done\":\"COMPLETADA\",\"In Progress\":\"EN_PROGRESO\"}"
     * }
     * }</pre>
     *
     * @param squadId ID del squad
     * @param request datos de la configuración
     * @return configuración guardada (HTTP 200)
     */
    @PutMapping("/{squadId}")
    @Operation(summary = "Crea o actualiza la configuración Jira de un squad")
    public ResponseEntity<JiraConfigResponse> guardarConfig(
            @PathVariable Long squadId,
            @Valid @RequestBody JiraConfigRequest request) {
        log.info("PUT /api/v1/jira/config/{}", squadId);
        return ResponseEntity.ok(jiraConfigService.guardarConfig(squadId, request));
    }

    // ── PATCH /api/v1/jira/config/method ─────────────────────────────────────

    /**
     * Cambia el método de carga de Jira en caliente (sin reiniciar).
     *
     * <p>El cambio es global: afecta a todos los squads inmediatamente.</p>
     *
     * <p>Body de ejemplo: {@code {"method": "SELENIUM"}}</p>
     *
     * @param request cuerpo con el nuevo método
     * @return método activo tras el cambio
     */
    @PatchMapping("/method")
    @Operation(summary = "Cambia el método de carga de Jira en caliente")
    public ResponseEntity<Map<String, String>> cambiarMetodo(
            @Valid @RequestBody JiraMethodRequest request) {
        log.info("PATCH /api/v1/jira/config/method — nuevoMetodo: {}", request.method());
        JiraLoadMethod metodo = jiraConfigService.cambiarMetodo(request.method());
        return ResponseEntity.ok(Map.of(
                "method", metodo.name(),
                "description", metodo.getDescription()
        ));
    }

    // ── POST /api/v1/jira/config/{squadId}/test ───────────────────────────────

    /**
     * Prueba la conectividad con el servidor Jira de un squad.
     *
     * <p>Realiza una llamada a {@code /rest/api/2/myself} y devuelve si la
     * autenticación fue exitosa. Solo funciona con el método API_REST.</p>
     *
     * @param squadId ID del squad a probar
     * @return {@code {"ok": true}} si la conexión es exitosa, {@code false} si falla
     */
    @PostMapping("/{squadId}/test")
    @Operation(summary = "Prueba la conectividad con Jira para un squad")
    public ResponseEntity<Map<String, Boolean>> probarConexion(@PathVariable Long squadId) {
        log.info("POST /api/v1/jira/config/{}/test", squadId);
        boolean ok = jiraConfigService.probarConexion(squadId);
        return ResponseEntity.ok(Map.of("ok", ok));
    }
}
