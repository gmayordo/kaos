package com.kaos.jira.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.alert.service.JiraAlertEngineService;
import com.kaos.jira.client.JiraApiClient;
import com.kaos.jira.config.JiraLoadConfig;
import com.kaos.jira.config.JiraLoadMethod;
import com.kaos.jira.dto.JiraSyncQueueResponse;
import com.kaos.jira.dto.JiraSyncStatusResponse;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.entity.JiraSyncQueue;
import com.kaos.jira.entity.JiraSyncQueue.EstadoOperacion;
import com.kaos.jira.entity.JiraSyncQueue.TipoOperacion;
import com.kaos.jira.entity.JiraSyncStatus;
import com.kaos.jira.entity.JiraSyncStatus.EstadoSync;
import com.kaos.jira.entity.SyncMode;
import com.kaos.jira.repository.JiraConfigRepository;
import com.kaos.jira.repository.JiraSyncQueueRepository;
import com.kaos.jira.repository.JiraSyncStatusRepository;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio central de sincronización Jira → KAOS.
 *
 * <p>Orquesta el ciclo completo de sync para un squad:
 * <ol>
 *   <li>Verifica cuota disponible (rate limiter).</li>
 *   <li>Si cuota agotada: encola en {@code jira_sync_queue} y retorna el estado.</li>
 *   <li>Importa issues pagina a pagina (50 por página) con control de cuota.</li>
 *   <li>Importa worklogs de cada issue → detecta co-desarrolladores.</li>
 *   <li>Actualiza {@code jira_sync_status} con el resultado.</li>
 * </ol>
 *
 * <p>Implementa el fallback automático (DT-41/DT-42):
 * cuota agotada + Chrome disponible → cambia a SELENIUM automáticamente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraSyncService {

    private static final int UMBRAL_CUOTA_MINIMA = 10;
    private static final DateTimeFormatter JQL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JiraConfigRepository jiraConfigRepository;
    private final JiraSyncStatusRepository syncStatusRepository;
    private final JiraSyncQueueRepository syncQueueRepository;
    private final JiraApiClient jiraApiClient;
    private final JiraImportService jiraImportService;
    private final JiraRateLimiter rateLimiter;
    private final JiraLoadConfig jiraLoadConfig;
    private final JiraAlertEngineService alertEngineService;
    private final JiraResumenEmailService resumenEmailService;
    private final SprintRepository sprintRepository;

    // ── Sync completa (issues + worklogs inline) ──────────────────────────

    /**
     * Ejecuta una sincronización completa (issues + worklogs inline) para el squad.
     * Usa SyncMode.FULL por defecto.
     *
     * @param squadId ID del squad a sincronizar
     * @return estado de la sync tras la operación
     */
    @Transactional
    public JiraSyncStatusResponse syncCompleta(Long squadId) {
        return syncCompleta(squadId, SyncMode.FULL);
    }

    /**
     * Ejecuta una sincronización para el squad con el modo indicado.
     *
     * <p>Flujo optimizado: la búsqueda JQL incluye subtasks (sin filtro
     * {@code issuetype not in (Sub-task)}), y los worklogs/comments vienen
     * inline en la respuesta de búsqueda (hasta 20). Si hay más de 20 worklogs,
     * se hace UNA llamada extra por issue afectado.</p>
     *
     * <p>En modo {@code INCREMENTAL} se añade {@code updated >= "ultimaSync"}
     * al JQL para traer solo lo modificado.</p>
     *
     * <p>En modo {@code DRY_RUN} se ejecuta la búsqueda pero no se persisten datos.</p>
     *
     * @param squadId  ID del squad a sincronizar
     * @param syncMode modo de ejecución (FULL, INCREMENTAL, DRY_RUN)
     * @return estado de la sync tras la operación
     */
    @Transactional
    public JiraSyncStatusResponse syncCompleta(Long squadId, SyncMode syncMode) {
        log.info("[JiraSyncService] Iniciando sync {} — squadId={}", syncMode, squadId);

        JiraConfig config = cargarConfigOException(squadId);
        JiraSyncStatus status = obtenerOCrearStatus(config.getSquad());

        // ── Verificar cuota ──────────────────────────────────────────────────
        long consumidas = rateLimiter.llamadasConsumidas();
        status.actualizarCuota((int) consumidas);

        if (consumidas >= 200 - UMBRAL_CUOTA_MINIMA) {
            log.warn("[JiraSyncService] Cuota insuficiente ({}/200) para squadId={}. Encolando sync.", consumidas, squadId);
            encolarOperacion(config.getSquad(), TipoOperacion.SYNC_ISSUES, null);
            status.actualizarCuota((int) consumidas);
            actualizarPendientes(status, squadId);
            return toStatusResponse(syncStatusRepository.save(status));
        }

        // ── Fallback API → SELENIUM si cuota casi agotada ───────────────────
        aplicarFallbackSiNecesario(consumidas, squadId);

        status.iniciarSync();
        syncStatusRepository.save(status);

        try {
            SyncResult result = sincronizarConInline(config, status, syncMode);

            status.actualizarCuota((int) rateLimiter.llamadasConsumidas());
            status.completarSync(result.issues, result.worklogs, result.comments, result.remoteLinks);
            actualizarPendientes(status, squadId);
            log.info("[JiraSyncService] Sync {} OK — squadId={}, issues={}, worklogs={}, comments={}, remoteLinks={}",
                    syncMode, squadId, result.issues, result.worklogs, result.comments, result.remoteLinks);

            // Evaluar reglas de coherencia tras sync exitosa
            alertEngineService.evaluarSprintActivo(squadId);
            // Enviar resumen email si está habilitado
            resumenEmailService.enviarResumenSync(status, squadId);

        } catch (Exception e) {
            log.error("[JiraSyncService] Error durante sync squadId={}: {}", squadId, e.getMessage(), e);
            status.registrarError(e.getMessage());
        }

        return toStatusResponse(syncStatusRepository.save(status));
    }

    /**
     * Ejecuta solo la importación de issues (sin worklogs extra).
     * Con la nueva estrategia, worklogs vienen inline en la búsqueda.
     *
     * @param squadId ID del squad
     * @return estado de la sync tras la operación
     */
    @Transactional
    public JiraSyncStatusResponse syncSoloIssues(Long squadId) {
        return syncCompleta(squadId, SyncMode.INCREMENTAL);
    }

    /**
     * Ejecuta solo la importación de worklogs de issues ya importadas.
     * Con la nueva estrategia, llama a syncCompleta en modo INCREMENTAL
     * ya que worklogs vienen inline con las issues actualizadas.
     *
     * @param squadId ID del squad
     * @return estado de la sync tras la operación
     */
    @Transactional
    public JiraSyncStatusResponse syncSoloWorklogs(Long squadId) {
        return syncCompleta(squadId, SyncMode.INCREMENTAL);
    }

    // ── Estado y cola ────────────────────────────────────────────────────────

    /**
     * Devuelve el estado actual de sync del squad (sin ejecutar ninguna llamada).
     *
     * @param squadId ID del squad
     * @return estado actual de la sync
     */
    @Transactional(readOnly = true)
    public JiraSyncStatusResponse obtenerEstado(Long squadId) {
        JiraConfig config = cargarConfigOException(squadId);
        JiraSyncStatus status = obtenerOCrearStatus(config.getSquad());
        // Actualizar cuota en memoria (no persistida) para que la respuesta sea fresca
        long consumidas = rateLimiter.llamadasConsumidas();
        status.actualizarCuota((int) consumidas);
        actualizarPendientes(status, squadId);
        return toStatusResponse(status);
    }

    /**
     * Devuelve todas las operaciones de la cola global.
     *
     * @return lista de operaciones ordenadas por fecha de creación DESC
     */
    @Transactional(readOnly = true)
    public List<JiraSyncQueueResponse> obtenerCola() {
        return syncQueueRepository.findAll().stream()
                .map(this::toQueueResponse)
                .toList();
    }

    /**
     * Fuerza el reintento de una operación en estado ERROR.
     *
     * @param operacionId ID de la operación a reintentar
     * @return estado actualizado de la operación
     */
    @Transactional
    public JiraSyncQueueResponse reintentarOperacion(Long operacionId) {
        JiraSyncQueue operacion = syncQueueRepository.findById(operacionId)
                .orElseThrow(() -> new EntityNotFoundException("Operación no encontrada: " + operacionId));

        if (operacion.getEstado() != EstadoOperacion.ERROR) {
            throw new IllegalStateException("Solo se pueden reintentar operaciones en estado ERROR");
        }

        operacion.setEstado(EstadoOperacion.PENDIENTE);
        operacion.setProgramadaPara(null);
        operacion.setErrorMensaje(null);
        log.info("[JiraSyncService] Operación {} marcada para reintento", operacionId);

        return toQueueResponse(syncQueueRepository.save(operacion));
    }

    // ── Procesamiento interno ────────────────────────────────────────────────

    /**
     * Procesa una operación de la cola (llamado por el batch scheduler).
     *
     * @param operacion operación a ejecutar
     */
    @Transactional
    public void procesarOperacionCola(JiraSyncQueue operacion) {
        log.info("[JiraSyncService] Procesando operación cola id={} tipo={}", operacion.getId(), operacion.getTipoOperacion());

        operacion.setEstado(EstadoOperacion.EN_PROGRESO);
        operacion.registrarIntento();
        syncQueueRepository.save(operacion);

        try {
            Long squadId = operacion.getSquad().getId();
            switch (operacion.getTipoOperacion()) {
                case SYNC_ISSUES -> syncSoloIssues(squadId);
                case SYNC_WORKLOGS -> syncSoloWorklogs(squadId);
                default -> throw new IllegalArgumentException("Tipo de operación no soportado en batch: " + operacion.getTipoOperacion());
            }

            operacion.setEstado(EstadoOperacion.COMPLETADA);
            log.info("[JiraSyncService] Operación cola id={} completada OK", operacion.getId());

        } catch (Exception e) {
            log.error("[JiraSyncService] Error procesando operación cola id={}: {}", operacion.getId(), e.getMessage(), e);
            operacion.setEstado(operacion.puedeReintentar() ? EstadoOperacion.ERROR : EstadoOperacion.ERROR);
            operacion.setErrorMensaje(e.getMessage());
            // Delay de reintento: 15 minutos
            if (operacion.puedeReintentar()) {
                operacion.setProgramadaPara(LocalDateTime.now().plusMinutes(15));
            }
        }

        syncQueueRepository.save(operacion);
    }

    // ── Métodos privados ─────────────────────────────────────────────────────

    // ── Resultado interno ────────────────────────────────────────────────────

    /** Encapsula el resultado de una sincronización (issues + worklogs + comments + remote links). */
    private record SyncResult(int issues, int worklogs, int comments, int remoteLinks) {}

    // ── Método unificado: search con worklogs inline ─────────────────────────

    /**
     * Sincroniza issues + worklogs en dos fases de búsqueda.
     *
     * <p><b>Fase 1</b>: Busca issues padre con JQL {@code sprint in openSprints()}.
     * Si los boards evolutivo y correctivo son distintos, usa {@code cf[24140] in (...)}.</p>
     *
     * <p><b>Fase 2</b>: Con las keys de los padres, hace UNA búsqueda extra
     * {@code parent in (KEY-1, KEY-2, ...)} para traer todas las subtareas.
     * En modo INCREMENTAL añade {@code updated >= \"ultimaSync\"} para traer
     * solo subtareas modificadas.</p>
     *
     * <p>Ambas búsquedas incluyen worklogs y comments inline (hasta 20 por issue).
     * Si un issue tiene más de 20 worklogs, se hace 1 llamada extra.</p>
     *
     * @param config   configuración Jira del squad
     * @param status   estado de sync actual
     * @param syncMode modo de ejecución
     * @return resultado con conteo de issues y worklogs procesados
     */
    @SuppressWarnings("unchecked")
    private SyncResult sincronizarConInline(JiraConfig config, JiraSyncStatus status, SyncMode syncMode) {
        String jql = construirJql(config, status, syncMode);
        log.info("[JiraSyncService] JQL padres ({}): {}", syncMode, jql);

        // 1. Buscar issues padre (sprint in openSprints)
        List<Map<String, Object>> parentIssues = jiraApiClient.buscarIssues(config, jql);
        log.info("[JiraSyncService] Issues padre recuperadas: {}", parentIssues.size());

        // 2. Recopilar keys de padres para buscar subtareas
        List<String> parentKeys = parentIssues.stream()
                .map(i -> (String) i.get("key"))
                .filter(k -> k != null)
                .toList();

        // 3. Segunda búsqueda: subtareas de esos padres (1 sola llamada API)
        List<Map<String, Object>> subtaskIssues = List.of();
        if (!parentKeys.isEmpty() && rateLimiter.canMakeCall()) {
            String subtaskJql = construirJqlSubtasks(parentKeys, status, syncMode);
            if (subtaskJql != null) {
                log.info("[JiraSyncService] JQL subtareas ({}): {}", syncMode, subtaskJql);
                subtaskIssues = jiraApiClient.buscarIssues(config, subtaskJql);
                log.info("[JiraSyncService] Subtareas recuperadas: {}", subtaskIssues.size());
            }
        }

        // Unir padres + subtareas
        List<Map<String, Object>> allIssues = new ArrayList<>(parentIssues.size() + subtaskIssues.size());
        allIssues.addAll(parentIssues);
        allIssues.addAll(subtaskIssues);
        log.info("[JiraSyncService] Total issues (padres + subtareas): {}", allIssues.size());

        if (syncMode == SyncMode.DRY_RUN) {
            log.info("[JiraSyncService] DRY_RUN — padres: {}, subtareas: {}. No se persisten datos.",
                    parentIssues.size(), subtaskIssues.size());
            return new SyncResult(allIssues.size(), 0, 0, 0);
        }

        // Buscar sprint activo para asociar a las issues
        Sprint sprintActivo = null;
        List<Sprint> sprintsActivos = sprintRepository.findBySquadIdAndEstado(
                config.getSquad().getId(), SprintEstado.ACTIVO);
        if (sprintsActivos != null && !sprintsActivos.isEmpty()) {
            sprintActivo = sprintsActivos.get(0);
            log.debug("[JiraSyncService] Sprint activo detectado: {} (id={})",
                    sprintActivo.getNombre(), sprintActivo.getId());
        }

        // 4. Procesar todas las issues (padres + subtareas) de forma uniforme
        int[] counters = {0, 0, 0, 0}; // issues, worklogs, comments, remoteLinks
        for (Map<String, Object> rawIssue : allIssues) {
            procesarIssueInline(rawIssue, config, sprintActivo, counters);
        }

        return new SyncResult(counters[0], counters[1], counters[2], counters[3]);
    }

    /**
     * Procesa una issue individual: persiste el issue + worklogs + comments + remote links.
     *
     * <p>Extrae worklogs y comments del JSON inline de la búsqueda JQL.
     * Si hay más de 20 worklogs, hace 1 llamada extra. Los remote links
     * siempre requieren 1 llamada extra por issue.</p>
     *
     * @param rawIssue  JSON crudo de la issue (de /rest/api/2/search)
     * @param config    configuración Jira del squad
     * @param counters  array [issues, worklogs, comments, remoteLinks] — se actualiza in-place
     */
    @SuppressWarnings("unchecked")
    private void procesarIssueInline(Map<String, Object> rawIssue, JiraConfig config, Sprint sprint, int[] counters) {
        try {
            // 1. Persistir/actualizar el issue
            JiraIssue issue = jiraImportService.processIssue(rawIssue, config, sprint);
            counters[0]++;

            // 2. Extraer worklogs inline del campo "fields.worklog"
            Map<String, Object> fields = (Map<String, Object>) rawIssue.getOrDefault("fields", Map.of());
            Map<String, Object> worklogField = (Map<String, Object>) fields.get("worklog");

            List<Map<String, Object>> inlineWorklogs;
            if (worklogField != null) {
                int wlTotal = worklogField.get("total") instanceof Number n ? n.intValue() : 0;
                int wlMaxResults = worklogField.get("maxResults") instanceof Number n ? n.intValue() : 20;
                inlineWorklogs = (List<Map<String, Object>>) worklogField.getOrDefault("worklogs", List.of());

                log.info("[JiraSyncService] Worklog inline {} — total:{}, maxResults:{}, inlineSize:{}, keys:{}",
                        issue.getJiraKey(), wlTotal, wlMaxResults, inlineWorklogs.size(), worklogField.keySet());

                // Overflow: si hay más worklogs que los inline, fetch completo
                if (wlTotal > wlMaxResults && rateLimiter.canMakeCall()) {
                    log.info("[JiraSyncService] Overflow worklogs en {} ({}/{}). Fetch completo.",
                            issue.getJiraKey(), wlMaxResults, wlTotal);
                    inlineWorklogs = jiraApiClient.obtenerWorklogs(config, issue.getJiraKey());
                }
            } else {
                log.warn("[JiraSyncService] Worklog field NULL para {} — fields keys: {}",
                        issue.getJiraKey(), fields.keySet());
                inlineWorklogs = List.of();
            }

            // 3. Procesar worklogs
            counters[1] += jiraImportService.processWorklogs(issue, inlineWorklogs);

            // 4. Extraer comentarios inline del campo "fields.comment"
            Map<String, Object> commentField = (Map<String, Object>) fields.get("comment");
            if (commentField != null) {
                List<Map<String, Object>> inlineComments =
                        (List<Map<String, Object>>) commentField.getOrDefault("comments", List.of());
                counters[2] += jiraImportService.processComments(issue, inlineComments);
            }

            // 5. Obtener remote links (requiere 1 call extra por issue)
            if (rateLimiter.canMakeCall()) {
                List<Map<String, Object>> remoteLinks =
                        jiraApiClient.obtenerRemoteLinks(config, issue.getJiraKey());
                counters[3] += jiraImportService.processRemoteLinks(issue, remoteLinks);
            }

        } catch (Exception e) {
            String key = rawIssue.get("key") instanceof String s ? s : "?";
            log.error("[JiraSyncService] Error procesando issue {}: {}", key, e.getMessage());
        }
    }

    // ── JQL Builders ───────────────────────────────────────────────────────

    /**
     * Construye el JQL para issues padre del squad.
     *
     * <p>Soporta ambos boards (evolutivo y correctivo) si son distintos:
     * {@code cf[24140] in ("evolId", "corrId")}. Si son iguales, usa
     * {@code cf[24140] = "boardId"}.</p>
     *
     * <p>En modo INCREMENTAL añade {@code updated >= "ultimaSync"}.</p>
     *
     * @param config   configuración Jira del squad
     * @param status   estado de sync (para obtener ultimaSync)
     * @param syncMode modo de ejecución
     * @return JQL listo para enviar a la API
     */
    private String construirJql(JiraConfig config, JiraSyncStatus status, SyncMode syncMode) {
        StringBuilder jql = new StringBuilder();
        jql.append("sprint in openSprints()");

        // Filtro por squad (cf[24140]) — ambos boards si son distintos
        Long evolId = config.getBoardEvolutivoId();
        Long corrId = config.getBoardCorrectivoId();
        if (evolId != null && corrId != null && !evolId.equals(corrId)) {
            jql.append(" AND cf[24140] in (\"").append(evolId).append("\", \"").append(corrId).append("\")");
        } else {
            Long boardId = evolId != null ? evolId : corrId;
            if (boardId != null) {
                jql.append(" AND cf[24140] = \"").append(boardId).append("\"");
            }
        }

        // Incremental: solo issues actualizadas desde la última sync
        if (syncMode == SyncMode.INCREMENTAL && status.getUltimaSync() != null) {
            String desde = status.getUltimaSync().format(JQL_DATE_FORMAT);
            jql.append(" AND updated >= \"").append(desde).append("\"");
        }

        jql.append(" ORDER BY cf[10705] ASC");
        return jql.toString();
    }

    /**
     * Construye un JQL para recuperar subtareas de las issues padre obtenidas.
     *
     * <p>Usa {@code parent in (KEY-1, KEY-2, ...)} para traer todas las subtareas
     * en una sola llamada. En modo INCREMENTAL añade filtro {@code updated >= "ultimaSync"}
     * para traer solo las modificadas.</p>
     *
     * @param parentKeys  claves de las issues padre (ej: RED-101, RED-102)
     * @param status      estado de sync (para obtener ultimaSync)
     * @param syncMode    modo de ejecución
     * @return JQL listo para enviar a la API, o null si no hay parents
     */
    private String construirJqlSubtasks(List<String> parentKeys, JiraSyncStatus status, SyncMode syncMode) {
        if (parentKeys == null || parentKeys.isEmpty()) {
            return null;
        }

        StringBuilder jql = new StringBuilder();
        String keys = parentKeys.stream()
                .collect(Collectors.joining(", "));
        jql.append("parent in (").append(keys).append(")");

        // En INCREMENTAL, solo subtareas modificadas desde la última sync
        if (syncMode == SyncMode.INCREMENTAL && status.getUltimaSync() != null) {
            String desde = status.getUltimaSync().format(JQL_DATE_FORMAT);
            jql.append(" AND updated >= \"").append(desde).append("\"");
        }

        jql.append(" ORDER BY key ASC");
        return jql.toString();
    }

    // ── Helpers privados ───────────────────────────────────────────────────

    private void aplicarFallbackSiNecesario(long consumidas, Long squadId) {
        if (consumidas >= 180 && jiraLoadConfig.getCurrentMethod() == JiraLoadMethod.API_REST) {
            boolean chromeDisponible = esChromeDisponible();
            if (chromeDisponible) {
                log.warn("[JiraSyncService] Cuota casi agotada ({}/200). Cambiando a SELENIUM para squadId={}", consumidas, squadId);
                jiraLoadConfig.setCurrentMethod(JiraLoadMethod.SELENIUM);
                jiraConfigRepository.findBySquadIdAndActivaTrue(squadId).ifPresent(cfg -> {
                    cfg.setLoadMethod(JiraLoadMethod.SELENIUM.name());
                    jiraConfigRepository.save(cfg);
                });
            } else {
                log.warn("[JiraSyncService] Cuota casi agotada ({}/200) pero Chrome no disponible para squadId={}", consumidas, squadId);
            }
        }
    }

    private boolean esChromeDisponible() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"which", "google-chrome"});
            return p.waitFor() == 0;
        } catch (Exception e) {
            log.debug("[JiraSyncService] Chrome no detectado: {}", e.getMessage());
            return false;
        }
    }

    private void encolarOperacion(Squad squad, TipoOperacion tipo, String payload) {
        Optional<JiraSyncQueue> existente = syncQueueRepository
                .findBySquadIdAndTipoOperacionAndEstado(squad.getId(), tipo, EstadoOperacion.PENDIENTE);

        if (existente.isPresent()) {
            log.debug("[JiraSyncService] Ya existe una operación PENDIENTE de tipo {} para squad {}", tipo, squad.getId());
            return;
        }

        JiraSyncQueue operacion = JiraSyncQueue.builder()
                .squad(squad)
                .tipoOperacion(tipo)
                .payload(payload)
                .estado(EstadoOperacion.PENDIENTE)
                .build();

        syncQueueRepository.save(operacion);
        log.info("[JiraSyncService] Operación {} encolada para squad {}", tipo, squad.getId());
    }

    private JiraSyncStatus obtenerOCrearStatus(Squad squad) {
        return syncStatusRepository.findBySquadId(squad.getId())
                .orElseGet(() -> JiraSyncStatus.builder()
                        .squad(squad)
                        .estado(EstadoSync.IDLE)
                        .build());
    }

    private void actualizarPendientes(JiraSyncStatus status, Long squadId) {
        long pendientes = syncQueueRepository.countBySquadIdAndEstado(squadId, EstadoOperacion.PENDIENTE);
        status.setOperacionesPendientes((int) pendientes);
    }

    private JiraConfig cargarConfigOException(Long squadId) {
        return jiraConfigRepository.findBySquadIdAndActivaTrue(squadId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No hay configuración Jira activa para el squad: " + squadId));
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private JiraSyncStatusResponse toStatusResponse(JiraSyncStatus s) {
        return new JiraSyncStatusResponse(
                s.getSquad().getId(),
                s.getSquad().getNombre(),
                s.getEstado(),
                s.getUltimaSync(),
                s.getIssuesImportadas(),
                s.getWorklogsImportados(),
                s.getCommentsImportados(),
                s.getRemoteLinksImportados(),
                s.getCallsConsumidas2h(),
                s.getCallsRestantes2h(),
                s.getUltimoError(),
                s.getOperacionesPendientes(),
                s.getUpdatedAt()
        );
    }

    private JiraSyncQueueResponse toQueueResponse(JiraSyncQueue q) {
        return new JiraSyncQueueResponse(
                q.getId(),
                q.getSquad().getId(),
                q.getSquad().getNombre(),
                q.getTipoOperacion(),
                q.getEstado(),
                q.getIntentos(),
                q.getMaxIntentos(),
                q.getProgramadaPara(),
                q.getEjecutadaAt(),
                q.getErrorMensaje(),
                q.getCreatedAt()
        );
    }
}
