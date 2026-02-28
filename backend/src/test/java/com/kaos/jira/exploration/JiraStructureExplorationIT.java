package com.kaos.jira.exploration;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.jira.repository.JiraConfigRepository;

/**
 * Test de EXPLORACIÓN contra Jira real.
 *
 * <p>NO es un test unitario. Es una herramienta de diagnóstico que:
 * <ol>
 *   <li>Lee la config de jira_config (BD real, perfil dev)</li>
 *   <li>Lanza JQLs para evolutivo y correctivo</li>
 *   <li>Para cada issue padre, descarga: subtasks, worklogs, comments</li>
 *   <li>Guarda todo en un JSON para analizar la estructura real</li>
 * </ol>
 *
 * <p>Ejecución manual:
 * <pre>
 *   ./mvnw test -Dtest="JiraStructureExplorationIT#explorarSquadRed" -Dspring.profiles.active=dev -Djira.token=TU_PASSWORD
 * </pre>
 *
 * <p>Requiere: PostgreSQL local corriendo.
 * El token se pasa como system property {@code -Djira.token=...} (NO necesita AES_SECRET_KEY).
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("exploration")
@DisplayName("Exploración de estructura Jira real")
class JiraStructureExplorationIT {

    private static final String ENDPOINT_SEARCH = "/rest/api/2/search";
    private static final int PAGE_SIZE = 50;

    @Autowired
    private JiraConfigRepository jiraConfigRepository;

    @Autowired
    private ObjectMapper baseObjectMapper;

    private final RestTemplate restTemplate = new RestTemplate();
    private PrintWriter errorLog;

    /**
     * Token/contraseña pasada por línea de comandos: {@code -Djira.token=...}
     * Si no se proporciona, intenta usar el token descifrado de la BD.
     */
    private String resolverToken(JiraConfig config) {
        String tokenParam = System.getProperty("jira.token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            System.out.println("  🔑 Usando token de -Djira.token (parámetro manual)");
            return tokenParam;
        }
        System.out.println("  🔑 Usando token descifrado de BD (AES_SECRET_KEY requerida)");
        return config.getToken();
    }

    // ── Test principal ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Squad RED — Explorar estructura completa: issues + subtasks + worklogs + comments")
    void explorarSquadRed() throws Exception {
        Long squadId = 1L; // red
        explorarSquad(squadId);
    }

    @Test
    @DisplayName("Squad GREEN — Explorar estructura completa")
    void explorarSquadGreen() throws Exception {
        Long squadId = 2L; // green
        explorarSquad(squadId);
    }

    // ── Lógica de exploración ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void explorarSquad(Long squadId) throws Exception {
        ObjectMapper mapper = baseObjectMapper.copy()
                .enable(SerializationFeature.INDENT_OUTPUT);

        // 0. Abrir fichero de errores
        String errorFilename = "target/jira-exploration-errors-squad-" + squadId + ".log";
        errorLog = new PrintWriter(new FileWriter(errorFilename), true);
        errorLog.println("═══ Exploración Jira — Squad " + squadId + " — " + LocalDateTime.now() + " ═══");

        // 1. Cargar config
        JiraConfig config = jiraConfigRepository.findBySquadIdAndActivaTrue(squadId)
                .orElseThrow(() -> new RuntimeException("No hay config Jira activa para squad " + squadId));

        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("  EXPLORACIÓN JIRA — Squad ID: " + squadId);
        System.out.println("  URL: " + config.getUrl());
        System.out.println("  Usuario: " + config.getUsuario());
        System.out.println("  Board Evolutivo: " + config.getBoardEvolutivoId());
        System.out.println("  Board Correctivo: " + config.getBoardCorrectivoId());

        // Resolver token: preferir -Djira.token, fallback a BD
        String token = resolverToken(config);

        System.out.println("════════════════════════════════════════════════════════════════\n");

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("squadId", squadId);
        resultado.put("url", config.getUrl());
        resultado.put("boardEvolutivoId", config.getBoardEvolutivoId());
        resultado.put("boardCorrectivoId", config.getBoardCorrectivoId());
        resultado.put("timestamp", LocalDateTime.now().toString());

        // 2. Query evolutivo
        String jqlEvol = "sprint in openSprints() AND cf[24140] = \""
                + config.getBoardEvolutivoId()
                + "\" AND issuetype not in (Sub-task) ORDER BY cf[10705] ASC";

        System.out.println("── Query EVOLUTIVO ──");
        System.out.println("JQL: " + jqlEvol);
        List<Map<String, Object>> issuesEvol = buscarIssuesCompletas(config, token, jqlEvol);
        System.out.println("Issues encontradas: " + issuesEvol.size());

        List<Map<String, Object>> detalleEvol = new ArrayList<>();
        for (Map<String, Object> issue : issuesEvol) {
            detalleEvol.add(explorarIssueCompleta(config, token, issue, mapper));
        }
        resultado.put("evolutivo", Map.of(
                "jql", jqlEvol,
                "totalIssues", issuesEvol.size(),
                "issues", detalleEvol
        ));

        // 3. Query correctivo
        if (config.getBoardCorrectivoId() != null
                && !config.getBoardCorrectivoId().equals(config.getBoardEvolutivoId())) {

            String jqlCorr = "sprint in openSprints() AND cf[24140] = \""
                    + config.getBoardCorrectivoId()
                    + "\" AND issuetype not in (Sub-task) ORDER BY cf[10705] ASC";

            System.out.println("\n── Query CORRECTIVO ──");
            System.out.println("JQL: " + jqlCorr);
            List<Map<String, Object>> issuesCorr = buscarIssuesCompletas(config, token, jqlCorr);
            System.out.println("Issues encontradas: " + issuesCorr.size());

            List<Map<String, Object>> detalleCorr = new ArrayList<>();
            for (Map<String, Object> issue : issuesCorr) {
                detalleCorr.add(explorarIssueCompleta(config, token, issue, mapper));
            }
            resultado.put("correctivo", Map.of(
                    "jql", jqlCorr,
                    "totalIssues", issuesCorr.size(),
                    "issues", detalleCorr
            ));
        } else {
            System.out.println("\n⚠ Board correctivo = evolutivo (" + config.getBoardCorrectivoId()
                    + "). Saltando query duplicada.");
            resultado.put("correctivo", Map.of(
                    "jql", "SKIPPED — mismo board que evolutivo",
                    "totalIssues", 0,
                    "nota", "board_correctivo_id == board_evolutivo_id en jira_config"
            ));
        }

        // 4. Guardar JSON de resultados
        String filename = "jira-exploration-squad-" + squadId + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".json";
        File outputFile = new File("target/" + filename);
        mapper.writeValue(outputFile, resultado);

        System.out.println("\n════════════════════════════════════════════════════════════════");
        System.out.println("  ✅ Resultado guardado en: " + outputFile.getAbsolutePath());
        System.out.println("════════════════════════════════════════════════════════════════\n");

        // 5. Resumen en consola
        imprimirResumen(resultado);

        // 6. Cerrar fichero de errores
        errorLog.println("═══ Fin exploración ═══");
        errorLog.close();
        System.out.println("  📋 Log de errores en: " + new File(errorFilename).getAbsolutePath());
    }

    /**
     * Explora una issue completa: datos base + subtasks + worklogs + comments.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> explorarIssueCompleta(
            JiraConfig config, String token, Map<String, Object> rawIssue, ObjectMapper mapper) {

        String key = (String) rawIssue.get("key");
        Map<String, Object> fields = (Map<String, Object>) rawIssue.getOrDefault("fields", Map.of());

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("key", key);
        detalle.put("id", rawIssue.get("id"));
        detalle.put("summary", extraerNested(fields, "summary"));
        detalle.put("issuetype", extraerNested(fields, "issuetype", "name"));
        detalle.put("status", extraerNested(fields, "status", "name"));
        detalle.put("assignee_key", extraerNested(fields, "assignee", "key"));
        detalle.put("assignee_name", extraerNested(fields, "assignee", "displayName"));
        detalle.put("priority", extraerNested(fields, "priority", "name"));
        detalle.put("parent_key", extraerNested(fields, "parent", "key"));

        // Sprint info (campo customfield_10016 suele ser sprint en Jira)
        detalle.put("sprint_raw", fields.get("customfield_10016"));

        // Timetracking
        detalle.put("timetracking", fields.get("timetracking"));

        // ── Subtasks (del JSON del padre) ───────────────────────────────────
        List<Map<String, Object>> subtasksRaw = (List<Map<String, Object>>)
                fields.getOrDefault("subtasks", List.of());

        System.out.println("  " + key + " — " + extraerNested(fields, "summary")
                + " [" + extraerNested(fields, "status", "name") + "]"
                + " — subtasks: " + subtasksRaw.size());

        // Descargar detalle completo de cada subtask
        List<Map<String, Object>> subtasksDetalle = new ArrayList<>();
        for (Map<String, Object> st : subtasksRaw) {
            String stKey = (String) st.get("key");
            Map<String, Object> subtaskCompleta = obtenerIssueCompleta(config, token, stKey);
            if (subtaskCompleta != null) {
                Map<String, Object> stFields = (Map<String, Object>)
                        subtaskCompleta.getOrDefault("fields", Map.of());

                Map<String, Object> stDetalle = new LinkedHashMap<>();
                stDetalle.put("key", stKey);
                stDetalle.put("summary", extraerNested(stFields, "summary"));
                stDetalle.put("status", extraerNested(stFields, "status", "name"));
                stDetalle.put("assignee_key", extraerNested(stFields, "assignee", "key"));
                stDetalle.put("assignee_name", extraerNested(stFields, "assignee", "displayName"));
                stDetalle.put("timetracking", stFields.get("timetracking"));

                // Worklogs de la subtask
                List<Map<String, Object>> stWorklogs = obtenerWorklogs(config, token, stKey);
                stDetalle.put("worklogs_count", stWorklogs.size());
                stDetalle.put("worklogs", resumirWorklogs(stWorklogs));

                // Comments de la subtask
                List<Map<String, Object>> stComments = obtenerComments(config, token, stKey);
                stDetalle.put("comments_count", stComments.size());
                stDetalle.put("comments", resumirComments(stComments));

                subtasksDetalle.add(stDetalle);

                System.out.println("    └─ " + stKey + " — " + extraerNested(stFields, "summary")
                        + " [" + extraerNested(stFields, "status", "name") + "]"
                        + " — worklogs: " + stWorklogs.size()
                        + ", comments: " + stComments.size());
            }
        }
        detalle.put("subtasks_count", subtasksDetalle.size());
        detalle.put("subtasks", subtasksDetalle);

        // ── Worklogs del padre ──────────────────────────────────────────────
        List<Map<String, Object>> worklogs = obtenerWorklogs(config, token, key);
        detalle.put("worklogs_count", worklogs.size());
        detalle.put("worklogs", resumirWorklogs(worklogs));

        // ── Comments del padre ──────────────────────────────────────────────
        List<Map<String, Object>> comments = obtenerComments(config, token, key);
        detalle.put("comments_count", comments.size());
        detalle.put("comments", resumirComments(comments));

        return detalle;
    }

    // ── Llamadas a Jira ─────────────────────────────────────────────────────

    /**
     * Busca issues con TODOS los campos (no solo los mínimos).
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buscarIssuesCompletas(JiraConfig config, String token, String jql) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        int startAt = 0;
        int total = Integer.MAX_VALUE;

        while (startAt < total) {
            String url = config.getUrl() + ENDPOINT_SEARCH
                    + "?jql=" + encodeUrl(jql)
                    + "&maxResults=" + PAGE_SIZE
                    + "&startAt=" + startAt
                    + "&fields=summary,status,assignee,priority,issuetype,parent,"
                    + "subtasks,customfield_10016,customfield_10028,timetracking,"
                    + "description,comment,worklog,created,updated,resolutiondate,"
                    + "labels,components,fixVersions,epic,issuelinks";

            ResponseEntity<Map<String, Object>> response = ejecutarGet(config, token, url);

            if (response != null && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                total = (Integer) body.getOrDefault("total", 0);
                List<Map<String, Object>> issues = (List<Map<String, Object>>)
                        body.getOrDefault("issues", List.of());
                resultado.addAll(issues);
                startAt += issues.size();
            } else {
                errorLog.println("[WARN] Respuesta nula buscando issues. startAt=" + startAt + ", jql=" + jql);
                System.err.println("⚠ Respuesta nula buscando issues. startAt=" + startAt);
                break;
            }
        }
        return resultado;
    }

    /**
     * Obtiene el detalle completo de una issue (con todos los campos).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> obtenerIssueCompleta(JiraConfig config, String token, String issueKey) {
        String url = config.getUrl() + "/rest/api/2/issue/" + issueKey
                + "?fields=summary,status,assignee,priority,issuetype,parent,"
                + "subtasks,customfield_10016,customfield_10028,timetracking,"
                + "description,comment,worklog,created,updated,resolutiondate,"
                + "labels,components,fixVersions,issuelinks";

        ResponseEntity<Map<String, Object>> response = ejecutarGet(config, token, url);
        return response != null ? response.getBody() : null;
    }

    /**
     * Obtiene worklogs de una issue.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> obtenerWorklogs(JiraConfig config, String token, String issueKey) {
        String url = config.getUrl() + "/rest/api/2/issue/" + issueKey + "/worklog";
        ResponseEntity<Map<String, Object>> response = ejecutarGet(config, token, url);
        if (response != null && response.getBody() != null) {
            return (List<Map<String, Object>>) response.getBody().getOrDefault("worklogs", List.of());
        }
        return List.of();
    }

    /**
     * Obtiene comments de una issue.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> obtenerComments(JiraConfig config, String token, String issueKey) {
        String url = config.getUrl() + "/rest/api/2/issue/" + issueKey + "/comment";
        ResponseEntity<Map<String, Object>> response = ejecutarGet(config, token, url);
        if (response != null && response.getBody() != null) {
            return (List<Map<String, Object>>) response.getBody().getOrDefault("comments", List.of());
        }
        return List.of();
    }

    // ── Resumen helpers ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resumirWorklogs(List<Map<String, Object>> worklogs) {
        List<Map<String, Object>> resumen = new ArrayList<>();
        for (Map<String, Object> wl : worklogs) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", wl.get("id"));
            r.put("author_key", extraerNested(wl, "author", "key"));
            r.put("author_name", extraerNested(wl, "author", "displayName"));
            r.put("started", wl.get("started"));
            r.put("timeSpentSeconds", wl.get("timeSpentSeconds"));
            r.put("timeSpent", wl.get("timeSpent"));
            r.put("comment", wl.get("comment"));
            resumen.add(r);
        }
        return resumen;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resumirComments(List<Map<String, Object>> comments) {
        List<Map<String, Object>> resumen = new ArrayList<>();
        for (Map<String, Object> c : comments) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", c.get("id"));
            r.put("author_key", extraerNested(c, "author", "key"));
            r.put("author_name", extraerNested(c, "author", "displayName"));
            r.put("created", c.get("created"));
            r.put("body", c.get("body"));
            resumen.add(r);
        }
        return resumen;
    }

    // ── Infraestructura ─────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> ejecutarGet(JiraConfig config, String token, String url) {
        try {
            HttpHeaders headers = buildHeaders(config, token);
            // Usar URI para evitar doble codificación por RestTemplate
            java.net.URI uri = new java.net.URI(url);
            return restTemplate.exchange(
                    uri, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            String msg = "⚠ Error GET " + url + ": " + e.getMessage();
            System.err.println(msg);
            if (errorLog != null) {
                errorLog.println(msg);
                e.printStackTrace(errorLog);
                errorLog.println();
            }
            return null;
        }
    }

    private HttpHeaders buildHeaders(JiraConfig config, String token) {
        String credentials = config.getUsuario() + ":" + token;
        String encoded = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.set(HttpHeaders.ACCEPT, "application/json");
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Object extraerNested(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> m)) return null;
            current = m.get(key);
        }
        return current;
    }

    private String encodeUrl(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    // ── Resumen en consola ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void imprimirResumen(Map<String, Object> resultado) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                     RESUMEN EXPLORACIÓN                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        for (String tipo : List.of("evolutivo", "correctivo")) {
            Object bloqueObj = resultado.get(tipo);
            if (bloqueObj instanceof Map<?, ?> bloque) {
                int totalIssues = toInt(bloque.get("totalIssues"));
                System.out.println("║  " + tipo.toUpperCase());
                System.out.println("║    JQL: " + bloque.get("jql"));
                System.out.println("║    Issues padre: " + totalIssues);

                Object issuesObj = bloque.get("issues");
                if (issuesObj instanceof List<?> issues) {
                    int totalSubtasks = 0, totalWorklogs = 0, totalComments = 0;
                    int totalStWorklogs = 0, totalStComments = 0;

                    for (Object issueObj : issues) {
                        if (issueObj instanceof Map<?, ?> issue) {
                            totalWorklogs += toInt(issue.get("worklogs_count"));
                            totalComments += toInt(issue.get("comments_count"));
                            totalSubtasks += toInt(issue.get("subtasks_count"));

                            Object stsObj = issue.get("subtasks");
                            if (stsObj instanceof List<?> sts) {
                                for (Object stObj : sts) {
                                    if (stObj instanceof Map<?, ?> st) {
                                        totalStWorklogs += toInt(st.get("worklogs_count"));
                                        totalStComments += toInt(st.get("comments_count"));
                                    }
                                }
                            }
                        }
                    }

                    System.out.println("║    Subtasks totales: " + totalSubtasks);
                    System.out.println("║    Worklogs (padre): " + totalWorklogs);
                    System.out.println("║    Worklogs (subtasks): " + totalStWorklogs);
                    System.out.println("║    Comments (padre): " + totalComments);
                    System.out.println("║    Comments (subtasks): " + totalStComments);
                    System.out.println("║    ──────────────────────────────────");
                    System.out.println("║    TOTAL llamadas API: "
                            + (1 + totalSubtasks * 3 + totalIssues * 2)
                            + " (search + " + totalSubtasks + " subtask details"
                            + " + " + (totalIssues + totalSubtasks) + " worklogs"
                            + " + " + (totalIssues + totalSubtasks) + " comments)");
                }
                System.out.println("║");
            }
        }

        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
    }

    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        return 0;
    }
}
