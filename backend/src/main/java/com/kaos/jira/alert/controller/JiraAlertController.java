package com.kaos.jira.alert.controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.kaos.jira.alert.dto.AlertRuleRequest;
import com.kaos.jira.alert.dto.AlertRuleResponse;
import com.kaos.jira.alert.dto.AlertaResponse;
import com.kaos.jira.alert.entity.JiraAlertRule;
import com.kaos.jira.alert.entity.JiraAlerta;
import com.kaos.jira.alert.repository.JiraAlertRuleRepository;
import com.kaos.jira.alert.repository.JiraAlertaRepository;
import com.kaos.jira.alert.service.JiraAlertEngineService;
import com.kaos.squad.repository.SquadRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

/**
 * Endpoints para gestión de alertas de coherencia Jira←→KAOS.
 *
 * <p>Base URL: {@code /api/v1/jira/alertas}</p>
 *
 * <ul>
 *   <li>GET  /                         — alertas de un sprint (filtrable por resuelta)</li>
 *   <li>PATCH /{id}/resolver            — marcar alerta como resuelta</li>
 *   <li>GET  /reglas                   — listar reglas de alerta</li>
 *   <li>POST /reglas                   — crear regla</li>
 *   <li>PUT  /reglas/{id}              — actualizar regla</li>
 *   <li>DELETE /reglas/{id}            — eliminar regla</li>
 *   <li>POST /evaluar/{sprintId}       — trigger manual del motor SpEL</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/jira/alertas")
@Tag(name = "JiraAlertas", description = "Alertas de coherencia Jira←→KAOS configuradas con SpEL")
public class JiraAlertController {

    private static final Logger log = LoggerFactory.getLogger(JiraAlertController.class);

    private final JiraAlertaRepository alertaRepository;
    private final JiraAlertRuleRepository ruleRepository;
    private final JiraAlertEngineService alertEngineService;
    private final SquadRepository squadRepository;

    public JiraAlertController(
            JiraAlertaRepository alertaRepository,
            JiraAlertRuleRepository ruleRepository,
            JiraAlertEngineService alertEngineService,
            SquadRepository squadRepository) {
        this.alertaRepository = alertaRepository;
        this.ruleRepository = ruleRepository;
        this.alertEngineService = alertEngineService;
        this.squadRepository = squadRepository;
    }

    // ─────────────────────────────────────────────
    // Alertas Generadas
    // ─────────────────────────────────────────────

    /**
     * Lista alertas de un sprint, ordenadas por severidad.
     * Filtrable por estado de resolución.
     *
     * @param sprintId ID del sprint (requerido)
     * @param resuelta true=solo resueltas, false=solo pendientes, omitir=todas
     * @param page     página (0-based)
     * @param size     tamaño de página (default 20)
     * @return página de alertas
     */
    @GetMapping
    @Operation(
        summary = "Listar alertas",
        description = "Alertas del sprint ordenadas por severidad (CRITICO primero). Filtrable por resuelta.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Página de alertas")
        }
    )
    public ResponseEntity<Page<AlertaResponse>> listarAlertas(
            @Parameter(description = "ID del sprint", required = true)
            @RequestParam Long sprintId,
            @Parameter(description = "Filtro: true=resueltas, false=pendientes, omitir=todas")
            @RequestParam(required = false) Boolean resuelta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AlertaResponse> resultado = alertaRepository
                .findBySprintId(sprintId, resuelta, pageable)
                .map(this::toResponse);

        log.info("GET /alertas sprint={} resuelta={} total={}", sprintId, resuelta, resultado.getTotalElements());
        return ResponseEntity.ok(resultado);
    }

    /**
     * Marca una alerta como resuelta por el Lead Tech.
     *
     * @param id ID de la alerta
     * @return alerta actualizada
     */
    @PatchMapping("/{id}/resolver")
    @Operation(
        summary = "Resolver alerta",
        description = "El Lead Tech marca la alerta como atendida. No se puede deshacer desde la API.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Alerta resuelta"),
            @ApiResponse(responseCode = "404", description = "Alerta no encontrada")
        }
    )
    public ResponseEntity<AlertaResponse> resolver(
            @Parameter(description = "ID de la alerta", required = true)
            @PathVariable Long id) {

        JiraAlerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada: " + id));

        alerta.resolver();
        alerta = alertaRepository.save(alerta);

        log.info("PATCH /alertas/{}/resolver OK", id);
        return ResponseEntity.ok(toResponse(alerta));
    }

    /**
     * Dispara manualmente el motor SpEL sobre un sprint.
     * Útil para pruebas o para re-evaluar tras cambios en reglas.
     *
     * @param sprintId ID del sprint
     * @param squadId  ID del squad
     * @return lista de alertas generadas
     */
    @PostMapping("/evaluar/{sprintId}")
    @Operation(
        summary = "Evaluar alertas manualmente",
        description = "Dispara el motor SpEL sobre el sprint indicado. Genera y persiste las alertas.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Alertas generadas"),
            @ApiResponse(responseCode = "404", description = "Sprint no encontrado")
        }
    )
    public ResponseEntity<List<AlertaResponse>> evaluar(
            @Parameter(description = "ID del sprint a evaluar", required = true)
            @PathVariable Long sprintId,
            @Parameter(description = "ID del squad", required = true)
            @RequestParam Long squadId) {

        log.info("POST /alertas/evaluar/{} squad={}", sprintId, squadId);
        List<AlertaResponse> alertas = alertEngineService.evaluar(sprintId, squadId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(alertas);
    }

    // ─────────────────────────────────────────────
    // Gestión de Reglas
    // ─────────────────────────────────────────────

    /**
     * Lista todas las reglas de alerta, opcionalmente filtradas por squad.
     *
     * @param squadId filtro de squad (null = todas, incluyendo globales)
     * @return lista de reglas
     */
    @GetMapping("/reglas")
    @Operation(
        summary = "Listar reglas de alerta",
        description = "Devuelve todas las reglas configuradas, con filtro opcional por squad.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lista de reglas")
        }
    )
    public ResponseEntity<List<AlertRuleResponse>> listarReglas(
            @Parameter(description = "Filtrar por squad (omitir para ver todas)")
            @RequestParam(required = false) Long squadId) {

        List<AlertRuleResponse> reglas;
        if (squadId != null) {
            reglas = ruleRepository.findActivasBySquadIdOrGlobal(squadId)
                    .stream().map(this::toRuleResponse).toList();
        } else {
            reglas = ruleRepository.findAll()
                    .stream().map(this::toRuleResponse).toList();
        }
        return ResponseEntity.ok(reglas);
    }

    /**
     * Crea una nueva regla de alerta.
     *
     * @param request datos de la regla
     * @return regla creada con HTTP 201
     */
    @PostMapping("/reglas")
    @Operation(
        summary = "Crear regla de alerta",
        description = "Crea una regla de coherencia configurable con SpEL.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Regla creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
        }
    )
    public ResponseEntity<AlertRuleResponse> crearRegla(
            @Valid @RequestBody AlertRuleRequest request) {

        JiraAlertRule regla = JiraAlertRule.builder()
                .squad(request.squadId() != null
                        ? squadRepository.findById(request.squadId())
                                .orElseThrow(() -> new EntityNotFoundException("Squad no encontrado: " + request.squadId()))
                        : null)
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .tipo(request.tipo())
                .condicionSpel(request.condicionSpel())
                .mensajeTemplate(request.mensajeTemplate())
                .severidad(request.severidad())
                .umbralValor(request.umbralValor())
                .activa(request.activa())
                .build();

        regla = ruleRepository.save(regla);
        log.info("POST /alertas/reglas creada id={} nombre={}", regla.getId(), regla.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(toRuleResponse(regla));
    }

    /**
     * Actualiza una regla existente.
     *
     * @param id      ID de la regla
     * @param request nuevos datos
     * @return regla actualizada
     */
    @PutMapping("/reglas/{id}")
    @Operation(
        summary = "Actualizar regla de alerta",
        description = "Modifica los campos de una regla de coherencia existente.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Regla actualizada"),
            @ApiResponse(responseCode = "404", description = "Regla no encontrada")
        }
    )
    public ResponseEntity<AlertRuleResponse> actualizarRegla(
            @Parameter(description = "ID de la regla", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AlertRuleRequest request) {

        JiraAlertRule regla = ruleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Regla no encontrada: " + id));

        regla.setNombre(request.nombre());
        regla.setDescripcion(request.descripcion());
        regla.setTipo(request.tipo());
        regla.setCondicionSpel(request.condicionSpel());
        regla.setMensajeTemplate(request.mensajeTemplate());
        regla.setSeveridad(request.severidad());
        regla.setUmbralValor(request.umbralValor());
        regla.setActiva(request.activa());

        regla = ruleRepository.save(regla);
        log.info("PUT /alertas/reglas/{} actualizada", id);
        return ResponseEntity.ok(toRuleResponse(regla));
    }

    /**
     * Elimina una regla de alerta.
     *
     * @param id ID de la regla
     */
    @DeleteMapping("/reglas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Eliminar regla de alerta",
        description = "Elimina permanentemente la regla. Considere desactivarla (activa=false) en su lugar.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Regla eliminada"),
            @ApiResponse(responseCode = "404", description = "Regla no encontrada")
        }
    )
    public void eliminarRegla(
            @Parameter(description = "ID de la regla", required = true)
            @PathVariable Long id) {

        if (!ruleRepository.existsById(id)) {
            throw new EntityNotFoundException("Regla no encontrada: " + id);
        }
        ruleRepository.deleteById(id);
        log.info("DELETE /alertas/reglas/{}", id);
    }

    // ─────────────────────────────────────────────
    // Mappers
    // ─────────────────────────────────────────────

    private AlertaResponse toResponse(JiraAlerta a) {
        return new AlertaResponse(
                a.getId(),
                a.getSprint().getId(),
                a.getSquad().getId(),
                a.getRegla().getId(),
                a.getRegla().getNombre(),
                a.getSeveridad(),
                a.getMensaje(),
                a.getJiraKey(),
                a.getPersona() != null ? a.getPersona().getNombre() : null,
                a.isResuelta(),
                a.isNotificadaEmail(),
                a.getCreatedAt()
        );
    }

    private AlertRuleResponse toRuleResponse(JiraAlertRule r) {
        return new AlertRuleResponse(
                r.getId(),
                r.getSquad() != null ? r.getSquad().getId() : null,
                r.getNombre(),
                r.getDescripcion(),
                r.getTipo(),
                r.getCondicionSpel(),
                r.getMensajeTemplate(),
                r.getSeveridad(),
                r.getUmbralValor(),
                r.isActiva()
        );
    }
}
