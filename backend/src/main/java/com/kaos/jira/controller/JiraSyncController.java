package com.kaos.jira.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kaos.jira.dto.JiraSyncQueueResponse;
import com.kaos.jira.dto.JiraSyncStatusResponse;
import com.kaos.jira.entity.SyncMode;
import com.kaos.jira.service.JiraSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para la sincronización Jira → KAOS.
 *
 * <p>Proporciona endpoints para disparar sincronizaciones manuales, consultar
 * el estado actual de la sync y gestionar la cola de operaciones pendientes.</p>
 *
 * <p>Base URL: {@code /api/v1/jira/sync}</p>
 */
@RestController
@RequestMapping("/api/v1/jira/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "JiraSync", description = "Sincronización Jira → KAOS: issues, worklogs y co-desarrolladores")
public class JiraSyncController {

    private final JiraSyncService jiraSyncService;

    // ── POST /{squadId} — Sync completa ─────────────────────────────────────

    /**
     * Dispara una sincronización completa (issues + worklogs) para el squad.
     *
     * <p>Si no hay cuota API disponible, la operación se encola y se retorna
     * el estado actual con {@code estado=CUOTA_AGOTADA} y
     * {@code operacionesPendientes > 0}.</p>
     *
     * <p>El resultado se retorna con HTTP 202 Accepted porque la sync puede
     * durar varios segundos (paginación de issues).</p>
     *
     * @param squadId ID del squad a sincronizar
     * @return estado de la sync tras iniciar la operación
     */
    @PostMapping("/{squadId}")
    @Operation(summary = "Sync issues + worklogs del squad",
            description = "Modos: FULL (todo), INCREMENTAL (solo actualizados), DRY_RUN (sin persistir)")
    public ResponseEntity<JiraSyncStatusResponse> syncCompleta(
            @PathVariable Long squadId,
            @RequestParam(defaultValue = "FULL") SyncMode mode) {
        log.info("POST /api/v1/jira/sync/{} mode={}", squadId, mode);
        JiraSyncStatusResponse resultado = jiraSyncService.syncCompleta(squadId, mode);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resultado);
    }

    // ── POST /{squadId}/issues — Solo issues ─────────────────────────────────

    /**
     * Sincroniza solo las issues del squad (sin importar worklogs).
     *
     * <p>Útil cuando se quiere actualizar estados y estimaciones sin consumir
     * las llamadas adicionales de los worklogs.</p>
     *
     * @param squadId ID del squad
     * @return estado de la sync tras la operación
     */
    @PostMapping("/{squadId}/issues")
    @Operation(summary = "Sync solo issues del squad (sin worklogs)")
    public ResponseEntity<JiraSyncStatusResponse> syncIssues(@PathVariable Long squadId) {
        log.info("POST /api/v1/jira/sync/{}/issues", squadId);
        JiraSyncStatusResponse resultado = jiraSyncService.syncSoloIssues(squadId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resultado);
    }

    // ── POST /{squadId}/worklogs — Solo worklogs ─────────────────────────────

    /**
     * Importa worklogs de issues ya cacheadas en BD para el squad.
     *
     * <p>No hace llamadas para actualizar issues — solo imputaciones.</p>
     *
     * @param squadId ID del squad
     * @return estado de la sync tras la operación
     */
    @PostMapping("/{squadId}/worklogs")
    @Operation(summary = "Sync solo worklogs de issues ya importadas")
    public ResponseEntity<JiraSyncStatusResponse> syncWorklogs(@PathVariable Long squadId) {
        log.info("POST /api/v1/jira/sync/{}/worklogs", squadId);
        JiraSyncStatusResponse resultado = jiraSyncService.syncSoloWorklogs(squadId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resultado);
    }

    // ── GET /{squadId}/status — Estado de la sync ────────────────────────────

    /**
     * Devuelve el estado actual de sincronización del squad.
     *
     * <p>Incluye: última sync, cuota consumida/restante en las últimas 2h,
     * estado del proceso y operaciones pendientes en cola.</p>
     *
     * @param squadId ID del squad
     * @return estado actual de la sync
     */
    @GetMapping("/{squadId}/status")
    @Operation(summary = "Estado de la última sync y cuota disponible para el squad")
    public ResponseEntity<JiraSyncStatusResponse> obtenerEstado(@PathVariable Long squadId) {
        log.debug("GET /api/v1/jira/sync/{}/status", squadId);
        return ResponseEntity.ok(jiraSyncService.obtenerEstado(squadId));
    }

    // ── GET /queue — Cola global ─────────────────────────────────────────────

    /**
     * Devuelve todas las operaciones encoladas (global, todos los squads).
     *
     * <p>Útil para el panel de administración y diagnóstico de operaciones
     * bloqueadas por cuota o en estado ERROR.</p>
     *
     * @return lista de operaciones ordenadas por fecha de creación
     */
    @GetMapping("/queue")
    @Operation(summary = "Lista todas las operaciones pendientes en la cola")
    public ResponseEntity<List<JiraSyncQueueResponse>> obtenerCola() {
        log.debug("GET /api/v1/jira/sync/queue");
        return ResponseEntity.ok(jiraSyncService.obtenerCola());
    }

    // ── POST /queue/{id}/retry — Reintentar operación ────────────────────────

    /**
     * Fuerza el reintento de una operación en estado ERROR.
     *
     * <p>Solo funciona si la operación está en estado ERROR. Si está COMPLETADA
     * o PENDIENTE devuelve 400 Bad Request.</p>
     *
     * @param id ID de la operación a reintentar
     * @return estado actualizado de la operación
     */
    @PostMapping("/queue/{id}/retry")
    @Operation(summary = "Fuerza el reintento de una operación en estado ERROR")
    public ResponseEntity<JiraSyncQueueResponse> reintentarOperacion(@PathVariable Long id) {
        log.info("POST /api/v1/jira/sync/queue/{}/retry", id);
        return ResponseEntity.ok(jiraSyncService.reintentarOperacion(id));
    }
}
