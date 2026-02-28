package com.kaos.jira.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaos.jira.entity.JiraComment;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.entity.JiraRemoteLink;
import com.kaos.jira.entity.JiraWorklog;
import com.kaos.jira.entity.WorklogOrigen;
import com.kaos.jira.repository.JiraCommentRepository;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.jira.repository.JiraRemoteLinkRepository;
import com.kaos.jira.repository.JiraWorklogRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.entity.Categoria;
import com.kaos.planificacion.entity.EstadoTarea;
import com.kaos.planificacion.entity.Prioridad;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.Tarea;
import com.kaos.planificacion.entity.TareaColaborador;
import com.kaos.planificacion.entity.TareaColaboradorId;
import com.kaos.planificacion.entity.TipoTarea;
import com.kaos.planificacion.repository.TareaColaboradorRepository;
import com.kaos.planificacion.repository.TareaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de importación de datos Jira a las tablas cache de KAOS.
 *
 * <p>Implementa la lógica de upsert idempotente para issues y worklogs,
 * así como la detección automática de co-desarrolladores (DT-35 / DT-39).</p>
 *
 * <p>Todos los métodos son {@code @Transactional}: cada llamada es atómica
 * y puede ser reintentada sin efectos no deseados.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JiraImportService {

    private static final DateTimeFormatter JIRA_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final JiraIssueRepository jiraIssueRepository;
    private final JiraWorklogRepository jiraWorklogRepository;
    private final JiraCommentRepository jiraCommentRepository;
    private final JiraRemoteLinkRepository jiraRemoteLinkRepository;
    private final TareaColaboradorRepository tareaColaboradorRepository;
    private final TareaRepository tareaRepository;
    private final PersonaRepository personaRepository;
    private final ObjectMapper objectMapper;

    // ── Issues ───────────────────────────────────────────────────────────────

    /**
     * Procesa una lista de issues raw de Jira y los persiste en {@code jira_issue}.
     *
     * <p>Upsert idempotente por {@code jiraKey}: si el issue ya existe se actualiza,
     * si no existe se crea. Si la issue es nueva y hay un sprint KAOS activo,
     * se genera también una {@link Tarea} vinculada.</p>
     *
     * @param rawIssues lista de maps con la estructura JSON de Jira
     * @param config    configuración del squad (para mapeo de estados)
     * @param sprint    sprint KAOS activo (puede ser null)
     * @return número de issues procesadas
     */
    @Transactional
    public int processIssues(List<Map<String, Object>> rawIssues, JiraConfig config, Sprint sprint) {
        if (rawIssues == null || rawIssues.isEmpty()) return 0;

        int count = 0;
        for (Map<String, Object> rawIssue : rawIssues) {
            try {
                processIssue(rawIssue, config, sprint);
                count++;
            } catch (Exception e) {
                String key = extraerString(rawIssue, "key");
                log.error("[JiraImportService] Error procesando issue {}: {}", key, e.getMessage());
            }
        }
        log.info("[JiraImportService] processIssues completado — procesadas: {}/{}", count, rawIssues.size());
        return count;
    }

    /**
     * Procesa un único issue Jira (upsert).
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public JiraIssue processIssue(Map<String, Object> rawIssue, JiraConfig config, Sprint sprint) {
        String jiraKey = extraerString(rawIssue, "key");
        String jiraId  = extraerString(rawIssue, "id");
        Map<String, Object> fields = (Map<String, Object>) rawIssue.getOrDefault("fields", Map.of());

        JiraIssue issue = jiraIssueRepository.findByJiraKey(jiraKey)
                .orElse(JiraIssue.builder()
                        .jiraKey(jiraKey)
                        .jiraId(jiraId)
                        .squad(config.getSquad())
                        .build());

        // Actualizar campos
        issue.setSummary(extraerNestedString(fields, "summary"));
        issue.setDescripcion(extraerNestedString(fields, "description"));
        issue.setTipoJira(extraerNestedString(fields, "issuetype", "name"));
        issue.setEstadoJira(extraerNestedString(fields, "status", "name"));
        issue.setEstadoKaos(mapearEstadoKaos(issue.getEstadoJira(), config.getMapeoEstados()));
        issue.setAsignadoJira(extraerUsuarioJira(fields, "assignee"));
        issue.setPrioridadJira(extraerNestedString(fields, "priority", "name"));
        issue.setUltimaSync(LocalDateTime.now());

        // Parent key (subtareas)
        Object parent = fields.get("parent");
        if (parent instanceof Map<?, ?> parentMap) {
            issue.setParentKey((String) parentMap.get("key"));
        }

        // Sprint linkado
        if (sprint != null) issue.setSprint(sprint);

        // Categoría según tipo de issue
        issue.setCategoria(inferirCategoria(issue.getTipoJira()));

        // Estimación (timeoriginalestimate en segundos)
        Object timeOriginal = ((Map<String, Object>) fields.getOrDefault("timetracking", Map.of()))
                .get("originalEstimateSeconds");
        if (timeOriginal instanceof Number n) {
            issue.setEstimacionHoras(BigDecimal.valueOf(n.longValue())
                    .divide(BigDecimal.valueOf(3600), 2, java.math.RoundingMode.HALF_UP));
        }

        // Persona KAOS asignada
        if (issue.getAsignadoJira() != null) {
            personaRepository.findByIdJira(issue.getAsignadoJira())
                    .ifPresent(issue::setPersona);
        }

        issue = jiraIssueRepository.save(issue);

        // Crear Tarea KAOS si es nueva y hay sprint
        if (issue.getTarea() == null && sprint != null) {
            crearTareaDesdeIssue(issue, sprint);
        }

        return issue;
    }

    // ── Worklogs ─────────────────────────────────────────────────────────────

    /**
     * Procesa los worklogs de una issue y persiste en {@code jira_worklog}.
     *
     * <p>Detección automática de co-desarrolladores: si {@code worklog.author.key}
     * difiere del {@code assignee.key} del issue, se crea/actualiza un registro
     * en {@code tarea_colaborador}.</p>
     *
     * @param jiraIssue   issue KAOS ya persistida
     * @param rawWorklogs lista de worklogs raw de Jira
     * @return número de worklogs procesados
     */
    @Transactional
    public int processWorklogs(JiraIssue jiraIssue, List<Map<String, Object>> rawWorklogs) {
        if (rawWorklogs == null || rawWorklogs.isEmpty()) return 0;

        int count = 0;
        BigDecimal horasTotal = BigDecimal.ZERO;

        for (Map<String, Object> rawWorklog : rawWorklogs) {
            try {
                JiraWorklog wl = processWorklog(jiraIssue, rawWorklog);
                horasTotal = horasTotal.add(wl.getHoras() != null ? wl.getHoras() : BigDecimal.ZERO);
                count++;
            } catch (Exception e) {
                log.error("[JiraImportService] Error procesando worklog de {}: {}", jiraIssue.getJiraKey(), e.getMessage());
            }
        }

        // Actualizar horas consumidas en el issue
        jiraIssue.setHorasConsumidas(horasTotal);
        jiraIssueRepository.save(jiraIssue);

        log.debug("[JiraImportService] processWorklogs — issue: {}, worklogs: {}, horas: {}",
                jiraIssue.getJiraKey(), count, horasTotal);
        return count;
    }

    /**
     * Procesa un único worklog (upsert por jiraWorklogId).
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public JiraWorklog processWorklog(JiraIssue jiraIssue, Map<String, Object> rawWorklog) {
        String wlId      = extraerString(rawWorklog, "id");
        String autorKey  = extraerUsuarioJira(rawWorklog, "author");

        JiraWorklog worklog = jiraWorklogRepository.findByJiraWorklogId(wlId)
                .orElse(JiraWorklog.builder()
                        .jiraWorklogId(wlId)
                        .jiraIssue(jiraIssue)
                        .origen(WorklogOrigen.JIRA)
                        .sincronizado(true)
                        .build());

        worklog.setAutorJira(autorKey != null ? autorKey : "");
        worklog.setComentario(extraerString(rawWorklog, "comment"));

        // Fecha (campo started: "2024-03-15T10:00:00.000+0200" → solo la fecha)
        String started = extraerString(rawWorklog, "started");
        if (started != null && !started.isBlank()) {
            worklog.setFecha(parsearFechaJira(started));
        } else {
            worklog.setFecha(LocalDate.now());
        }

        // Horas (timeSpentSeconds / 3600)
        Object tss = rawWorklog.get("timeSpentSeconds");
        if (tss instanceof Number n) {
            worklog.setHoras(BigDecimal.valueOf(n.longValue())
                    .divide(BigDecimal.valueOf(3600), 2, java.math.RoundingMode.HALF_UP));
        }

        // Persona KAOS
        if (autorKey != null) {
            personaRepository.findByIdJira(autorKey).ifPresent(worklog::setPersona);
        }

        worklog = jiraWorklogRepository.save(worklog);

        // Detectar co-desarrolladores (DT-35 / DT-39)
        detectarCoDesarrollador(jiraIssue, worklog, autorKey);

        return worklog;
    }

    // ── Comentarios ─────────────────────────────────────────────────────────

    /**
     * Procesa los comentarios inline de una issue y persiste en {@code jira_comment}.
     *
     * <p>Upsert idempotente por {@code jiraCommentId}: si el comentario ya existe
     * se actualiza, si no existe se crea. Vincula automáticamente con la
     * {@link Persona} KAOS cuando se detecta {@code author.key} en BD.</p>
     *
     * @param jiraIssue   issue KAOS ya persistida
     * @param rawComments lista de comentarios raw de Jira (del campo fields.comment.comments)
     * @return número de comentarios procesados
     */
    @Transactional
    public int processComments(JiraIssue jiraIssue, List<Map<String, Object>> rawComments) {
        if (rawComments == null || rawComments.isEmpty()) return 0;

        int count = 0;
        for (Map<String, Object> rawComment : rawComments) {
            try {
                processComment(jiraIssue, rawComment);
                count++;
            } catch (Exception e) {
                log.error("[JiraImportService] Error procesando comentario de {}: {}", jiraIssue.getJiraKey(), e.getMessage());
            }
        }

        log.debug("[JiraImportService] processComments — issue: {}, comments: {}", jiraIssue.getJiraKey(), count);
        return count;
    }

    /**
     * Procesa un único comentario Jira (upsert por jiraCommentId).
     */
    @Transactional
    public JiraComment processComment(JiraIssue jiraIssue, Map<String, Object> rawComment) {
        String commentId = extraerString(rawComment, "id");
        String autorKey = extraerUsuarioJira(rawComment, "author");
        String body = extraerString(rawComment, "body");
        String created = extraerString(rawComment, "created");
        String updated = extraerString(rawComment, "updated");

        JiraComment comment = jiraCommentRepository.findByJiraCommentId(commentId)
                .orElse(JiraComment.builder()
                        .jiraCommentId(commentId)
                        .jiraIssue(jiraIssue)
                        .build());

        comment.setAutorJira(autorKey != null ? autorKey : "");
        comment.setBody(body);

        // Fechas
        if (created != null && !created.isBlank()) {
            comment.setFechaCreacion(parsearTimestampJira(created));
        }
        if (updated != null && !updated.isBlank()) {
            comment.setFechaActualizacion(parsearTimestampJira(updated));
        }

        // Persona KAOS
        if (autorKey != null) {
            personaRepository.findByIdJira(autorKey).ifPresent(comment::setPersona);
        }

        return jiraCommentRepository.save(comment);
    }

    // ── Remote Links ─────────────────────────────────────────────────────────

    /**
     * Procesa los remote links de una issue y persiste en {@code jira_remote_link}.
     *
     * <p>Upsert idempotente por {@code jiraLinkId}. Cada remote link tiene un
     * objeto anidado {@code object} con url, title, summary e icon.</p>
     *
     * @param jiraIssue    issue KAOS ya persistida
     * @param rawLinks     lista de remote links raw de Jira
     * @return número de remote links procesados
     */
    @Transactional
    public int processRemoteLinks(JiraIssue jiraIssue, List<Map<String, Object>> rawLinks) {
        if (rawLinks == null || rawLinks.isEmpty()) return 0;

        int count = 0;
        for (Map<String, Object> rawLink : rawLinks) {
            try {
                processRemoteLink(jiraIssue, rawLink);
                count++;
            } catch (Exception e) {
                log.error("[JiraImportService] Error procesando remote link de {}: {}", jiraIssue.getJiraKey(), e.getMessage());
            }
        }

        log.debug("[JiraImportService] processRemoteLinks — issue: {}, links: {}", jiraIssue.getJiraKey(), count);
        return count;
    }

    /**
     * Procesa un único remote link Jira (upsert por jiraLinkId).
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public JiraRemoteLink processRemoteLink(JiraIssue jiraIssue, Map<String, Object> rawLink) {
        String linkId = String.valueOf(rawLink.get("id"));
        String relationship = extraerString(rawLink, "relationship");

        // El objeto remoto está anidado en "object"
        Map<String, Object> obj = (Map<String, Object>) rawLink.getOrDefault("object", Map.of());
        String url = extraerString(obj, "url");
        String titulo = extraerString(obj, "title");
        String resumen = extraerString(obj, "summary");

        // Icono (object.icon.url16x16)
        String iconoUrl = null;
        Object icon = obj.get("icon");
        if (icon instanceof Map<?, ?> iconMap) {
            Object url16 = iconMap.get("url16x16");
            if (url16 instanceof String s) iconoUrl = s;
        }

        JiraRemoteLink link = jiraRemoteLinkRepository.findByJiraLinkId(linkId)
                .orElse(JiraRemoteLink.builder()
                        .jiraLinkId(linkId)
                        .jiraIssue(jiraIssue)
                        .build());

        link.setUrl(url);
        link.setTitulo(titulo);
        link.setResumen(resumen);
        link.setIconoUrl(iconoUrl);
        link.setRelacion(relationship);

        return jiraRemoteLinkRepository.save(link);
    }

    // ── Co-desarrolladores ────────────────────────────────────────────────────

    /**
     * Si el autor del worklog difiere del assignee del issue → co-desarrollador.
     *
     * <p>Busca la Tarea KAOS vinculada al issue y hace upsert en
     * {@code tarea_colaborador} con {@code detectado_via=WORKLOG}.</p>
     */
    private void detectarCoDesarrollador(JiraIssue issue, JiraWorklog worklog, String autorJira) {
        if (autorJira == null || autorJira.equals(issue.getAsignadoJira())) return;
        if (issue.getTarea() == null || worklog.getPersona() == null) return;

        Long tareaId   = issue.getTarea().getId();
        Long personaId = worklog.getPersona().getId();
        BigDecimal horas = worklog.getHoras() != null ? worklog.getHoras() : BigDecimal.ZERO;

        if (tareaColaboradorRepository.existsByTareaIdAndPersonaId(tareaId, personaId)) {
            tareaColaboradorRepository.incrementarHoras(tareaId, personaId, horas);
        } else {
            TareaColaborador colaborador = TareaColaborador.builder()
                    .id(new TareaColaboradorId(tareaId, personaId))
                    .tarea(issue.getTarea())
                    .persona(worklog.getPersona())
                    .rol("DESARROLLADOR")
                    .horasImputadas(horas)
                    .detectadoVia(TareaColaborador.DetectadoVia.WORKLOG)
                    .build();
            tareaColaboradorRepository.save(colaborador);
            log.info("[JiraImportService] Co-desarrollador detectado — tarea: {}, persona: {}",
                    tareaId, personaId);
        }
    }

    // ── Tarea KAOS desde issue Jira ───────────────────────────────────────────

    /**
     * Crea una {@link Tarea} KAOS vinculada a un issue Jira recién importado.
     */
    private void crearTareaDesdeIssue(JiraIssue issue, Sprint sprint) {
        // Default estimation: 1h si Jira no tiene estimación (constraint chk_estimacion > 0)
        BigDecimal estimacion = issue.getEstimacionHoras() != null && issue.getEstimacionHoras().compareTo(BigDecimal.ZERO) > 0
                ? issue.getEstimacionHoras()
                : BigDecimal.ONE;

        Tarea tarea = Tarea.builder()
                .sprint(sprint)
                .jiraKey(issue.getJiraKey())
                .jiraIssue(issue)
                .esDeJira(true)
                .titulo(truncar(issue.getSummary(), 255))
                .tipo(mapearTipoTarea(issue.getTipoJira()))
                .categoria(mapearCategoria(issue.getCategoria()))
                .estimacion(estimacion)
                .prioridad(mapearPrioridad(issue.getPrioridadJira()))
                .estado(mapearEstadoTarea(issue.getEstadoKaos()))
                .persona(issue.getPersona())
                .build();

        tarea = tareaRepository.save(tarea);
        issue.setTarea(tarea);
        jiraIssueRepository.save(issue);

        log.debug("[JiraImportService] Tarea KAOS creada — id: {}, jiraKey: {}",
                tarea.getId(), issue.getJiraKey());
    }

    // ── Mapeos y helpers privados ────────────────────────────────────────────

    /**
     * Mapea el estado de Jira al estado KAOS usando el JSON de configuración.
     * Si no hay mapeo o el estado no está en la tabla, devuelve null.
     */
    private String mapearEstadoKaos(String estadoJira, String mapeoJson) {
        if (estadoJira == null || mapeoJson == null || mapeoJson.isBlank()) return null;
        try {
            Map<String, String> mapeo = objectMapper.readValue(mapeoJson, new TypeReference<>() {});
            return mapeo.get(estadoJira);
        } catch (Exception e) {
            log.warn("[JiraImportService] Error parseando mapeoEstados: {}", e.getMessage());
            return null;
        }
    }

    private TipoTarea mapearTipoTarea(String tipoJira) {
        if (tipoJira == null) return TipoTarea.TAREA;
        return switch (tipoJira.toLowerCase()) {
            case "story", "historia"    -> TipoTarea.HISTORIA;
            case "bug"                   -> TipoTarea.BUG;
            case "spike"                 -> TipoTarea.SPIKE;
            default                      -> TipoTarea.TAREA;
        };
    }

    private Categoria mapearCategoria(String categoriaStr) {
        if ("EVOLUTIVO".equals(categoriaStr)) return Categoria.EVOLUTIVO;
        return Categoria.CORRECTIVO;
    }

    private Prioridad mapearPrioridad(String prioridadJira) {
        if (prioridadJira == null) return Prioridad.NORMAL;
        return switch (prioridadJira.toLowerCase()) {
            case "highest", "high" -> Prioridad.ALTA;
            case "low", "lowest"   -> Prioridad.BAJA;
            default                -> Prioridad.NORMAL;
        };
    }

    private EstadoTarea mapearEstadoTarea(String estadoKaos) {
        if (estadoKaos == null) return EstadoTarea.PENDIENTE;
        return switch (estadoKaos.toUpperCase()) {
            case "EN_PROGRESO"  -> EstadoTarea.EN_PROGRESO;
            case "COMPLETADA"   -> EstadoTarea.COMPLETADA;
            case "BLOQUEADO"    -> EstadoTarea.BLOQUEADO;
            default             -> EstadoTarea.PENDIENTE;
        };
    }

    private String inferirCategoria(String tipoJira) {
        if (tipoJira == null) return null;
        return switch (tipoJira.toLowerCase()) {
            case "bug"              -> "CORRECTIVO";
            case "story", "spike"   -> "EVOLUTIVO";
            default                 -> null;
        };
    }

    private LocalDate parsearFechaJira(String started) {
        try {
            // Intentar parseo completo primero
            return LocalDateTime.parse(started, JIRA_DATE_FORMAT).toLocalDate();
        } catch (Exception e1) {
            try {
                // Fallback: extraer solo la parte de fecha (yyyy-MM-dd)
                return LocalDate.parse(started.substring(0, 10));
            } catch (Exception e2) {
                log.warn("[JiraImportService] No se pudo parsear fecha Jira: {}", started);
                return LocalDate.now();
            }
        }
    }

    private LocalDateTime parsearTimestampJira(String timestamp) {
        try {
            return LocalDateTime.parse(timestamp, JIRA_DATE_FORMAT);
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(timestamp.substring(0, 19));
            } catch (Exception e2) {
                log.warn("[JiraImportService] No se pudo parsear timestamp Jira: {}", timestamp);
                return LocalDateTime.now();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String extraerNestedString(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> m)) return null;
            current = m.get(key);
        }
        return current instanceof String s ? s : null;
    }

    /**
     * Extrae el identificador de usuario de un objeto Jira (assignee, author, etc).
     * Jira Server devuelve 'name' (username real) y 'key' (a veces JIRAUSER...).
     * Prioridad: name → key.
     *
     * @param fields    mapa de campos del issue/worklog/comment
     * @param userField nombre del campo usuario (ej: "assignee", "author")
     * @return username o null si no existe
     */
    @SuppressWarnings("unchecked")
    private String extraerUsuarioJira(Map<String, Object> fields, String userField) {
        Object userObj = fields.get(userField);
        if (!(userObj instanceof Map<?, ?> userMap)) return null;
        
        // Prioridad: name (username real) → key (fallback, puede ser JIRAUSERxxxx)
        Object name = userMap.get("name");
        if (name instanceof String s && !s.isBlank()) return s;
        
        Object key = userMap.get("key");
        return key instanceof String s ? s : null;
    }

    private String extraerString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String s ? s : null;
    }

    private String truncar(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
