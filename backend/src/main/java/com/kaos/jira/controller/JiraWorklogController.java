package com.kaos.jira.controller;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.kaos.jira.dto.JiraWorklogResponse;
import com.kaos.jira.dto.WorklogDiaResponse;
import com.kaos.jira.dto.WorklogRequest;
import com.kaos.jira.dto.WorklogSemanaResponse;
import com.kaos.jira.service.JiraWorklogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints para la gestión de imputaciones de tiempo sobre issues Jira.
 *
 * <p>Base URL: {@code /api/v1/jira/worklogs}</p>
 *
 * <ul>
 *   <li>GET  /mia        — vista diaria de imputaciones</li>
 *   <li>GET  /semana     — vista semanal en grid</li>
 *   <li>POST /           — registrar nueva imputación</li>
 *   <li>PUT  /{id}       — editar imputación no sincronizada</li>
 *   <li>DELETE /{id}     — eliminar imputación no sincronizada</li>
 *   <li>GET  /issue/{k}  — todas las imputaciones de una issue</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/jira/worklogs")
@Tag(name = "JiraWorklogs", description = "Gestión de imputaciones de tiempo (worklogs) sobre issues Jira")
public class JiraWorklogController {

    private static final Logger log = LoggerFactory.getLogger(JiraWorklogController.class);

    private final JiraWorklogService worklogService;

    public JiraWorklogController(JiraWorklogService worklogService) {
        this.worklogService = worklogService;
    }

    // ─────────────────────────────────────────────
    // Consultas de vista
    // ─────────────────────────────────────────────

    /**
     * Vista "Mi Día": imputaciones de una persona en una fecha concreta.
     *
     * @param personaId ID de la persona
     * @param fecha     fecha en formato yyyy-MM-dd; si se omite, se usa hoy
     * @return resumen del día con líneas de worklog y porcentaje de jornada
     */
    @GetMapping("/mia")
    @Operation(
        summary = "Vista Mi Día",
        description = "Devuelve las imputaciones de una persona para una fecha, junto con la capacidad diaria calculada.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Resumen del día"),
            @ApiResponse(responseCode = "404", description = "Persona no encontrada")
        }
    )
    public ResponseEntity<WorklogDiaResponse> getMiDia(
            @Parameter(description = "ID de la persona", required = true)
            @RequestParam Long personaId,
            @Parameter(description = "Fecha (yyyy-MM-dd); por defecto hoy")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        log.info("GET /worklogs/mia personaId={} fecha={}", personaId, fecha);
        return ResponseEntity.ok(worklogService.getMiDia(personaId, fecha));
    }

    /**
     * Vista "Mi Semana": grid de imputaciones de una persona para la semana ISO
     * que contiene la fecha indicada.
     *
     * @param personaId  ID de la persona
     * @param semana     cualquier día dentro de la semana; si se omite, semana actual
     * @return grid semana×tarea con totales
     */
    @GetMapping("/semana")
    @Operation(
        summary = "Vista Mi Semana",
        description = "Grid de imputaciones por tarea para la semana ISO que contiene la fecha indicada.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Grid semanal"),
            @ApiResponse(responseCode = "404", description = "Persona no encontrada")
        }
    )
    public ResponseEntity<WorklogSemanaResponse> getMiSemana(
            @Parameter(description = "ID de la persona", required = true)
            @RequestParam Long personaId,
            @Parameter(description = "Cualquier día de la semana (yyyy-MM-dd); por defecto semana actual")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {

        log.info("GET /worklogs/semana personaId={} semana={}", personaId, semana);
        return ResponseEntity.ok(worklogService.getMiSemana(personaId, semana));
    }

    /**
     * Lista todas las imputaciones registradas para una issue concreta.
     *
     * @param jiraKey clave de la issue (p.ej. KAOS-123)
     * @return lista de worklogs ordenados por fecha
     */
    @GetMapping("/issue/{jiraKey}")
    @Operation(
        summary = "Worklogs por issue",
        description = "Devuelve todos los worklogs registrados para una issue Jira.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lista de worklogs")
        }
    )
    public ResponseEntity<List<JiraWorklogResponse>> getByIssue(
            @Parameter(description = "Clave de la issue Jira, p.ej. KAOS-123", required = true)
            @PathVariable String jiraKey) {

        log.info("GET /worklogs/issue/{}", jiraKey);
        return ResponseEntity.ok(worklogService.getByIssue(jiraKey));
    }

    // ─────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────

    /**
     * Registra una nueva imputación desde KAOS. El worklog se encola para
     * sincronización diferida con Jira.
     *
     * @param request datos de la imputación
     * @return worklog creado con HTTP 201
     */
    @PostMapping
    @Operation(
        summary = "Registrar imputación",
        description = "Crea un nuevo worklog (origen=KAOS) y lo encola para sincronización diferida.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Worklog creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o supera capacidad diaria"),
            @ApiResponse(responseCode = "404", description = "Issue o persona no encontrada")
        }
    )
    public ResponseEntity<JiraWorklogResponse> registrar(
            @Valid @RequestBody WorklogRequest request) {

        log.info("POST /worklogs issue={} persona={} fecha={}", request.jiraKey(),
                request.personaId(), request.fecha());
        JiraWorklogResponse response = worklogService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Edita una imputación existente. Solo es posible si aún no fue sincronizada.
     *
     * @param id      ID del worklog
     * @param request nuevos datos
     * @return worklog actualizado
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Editar imputación",
        description = "Modifica un worklog no sincronizado. Si ya fue enviado a Jira devuelve 409.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Worklog actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o supera capacidad"),
            @ApiResponse(responseCode = "404", description = "Worklog no encontrado"),
            @ApiResponse(responseCode = "409", description = "Worklog ya sincronizado con Jira")
        }
    )
    public ResponseEntity<JiraWorklogResponse> editar(
            @Parameter(description = "ID del worklog", required = true)
            @PathVariable Long id,
            @Valid @RequestBody WorklogRequest request) {

        log.info("PUT /worklogs/{}", id);
        return ResponseEntity.ok(worklogService.editar(id, request));
    }

    /**
     * Elimina una imputación. Solo es posible si aún no fue sincronizada.
     *
     * @param id ID del worklog
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Eliminar imputación",
        description = "Elimina un worklog no sincronizado. Si ya fue enviado a Jira devuelve 409.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Worklog eliminado"),
            @ApiResponse(responseCode = "404", description = "Worklog no encontrado"),
            @ApiResponse(responseCode = "409", description = "Worklog ya sincronizado con Jira")
        }
    )
    public void eliminar(
            @Parameter(description = "ID del worklog", required = true)
            @PathVariable Long id) {

        log.info("DELETE /worklogs/{}", id);
        worklogService.eliminar(id);
    }
}
