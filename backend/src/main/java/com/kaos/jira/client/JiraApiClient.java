package com.kaos.jira.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.jira.service.JiraRateLimiter;
import lombok.extern.slf4j.Slf4j;

/**
 * Cliente para la API REST v2 de Jira Server / Data Center.
 *
 * <p>Autenticación: Basic Auth con {@code usuario:token} codificado en Base64.
 * Cada llamada se registra en {@link JiraRateLimiter} automáticamente.
 *
 * <p>Timeouts configurados: connectTimeout=5s, readTimeout=30s.
 *
 * <p>Paginación Jira: los endpoints que devuelven listas usan
 * {@code maxResults} (hasta 50) y {@code startAt} para paginar.
 */
@Slf4j
@Component
public class JiraApiClient {

    private static final int PAGE_SIZE = 50;
    private static final String ENDPOINT_SEARCH = "/rest/api/2/search";
    private static final String ENDPOINT_WORKLOG = "/rest/api/2/issue/{issueKey}/worklog";
    private static final String ENDPOINT_ISSUE = "/rest/api/2/issue/{issueKey}";
    private static final String ENDPOINT_REMOTELINK = "/rest/api/2/issue/{issueKey}/remotelink";
    private static final String ENDPOINT_MYSELF = "/rest/api/2/myself";

    private final RestTemplate restTemplate;
    private final JiraRateLimiter rateLimiter;

    public JiraApiClient(RestTemplateBuilder builder, JiraRateLimiter rateLimiter) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.rateLimiter = rateLimiter;
    }

    // ── Issues ───────────────────────────────────────────────────────────────

    /**
     * Busca issues en Jira usando JQL, con paginación automática.
     * Si la cuota se agota a mitad de la descarga, devuelve los datos obtenidos hasta ese momento.
     *
     * @param config  configuración Jira del squad (URL + credenciales)
     * @param jql     consulta JQL (ej: {@code project=RED AND sprint in openSprints()})
     * @return lista completa de maps con los datos de cada issue
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> buscarIssues(JiraConfig config, String jql) {
        List<Map<String, Object>> resultado = new java.util.ArrayList<>();
        int startAt = 0;
        int total = Integer.MAX_VALUE;

        while (startAt < total && rateLimiter.canMakeCall()) {
            String url = config.getUrl() + ENDPOINT_SEARCH
                    + "?jql=" + encodeUrl(jql)
                    + "&maxResults=" + PAGE_SIZE
                    + "&startAt=" + startAt
                    + "&fields=summary,status,assignee,priority,issuetype,parent,"
                    + "subtasks,customfield_10016,customfield_10028,timetracking,"
                    + "worklog,comment,updated";

            log.debug("[JiraApiClient] buscarIssues — jql: {}, startAt: {}", jql, startAt);

            ResponseEntity<Map<String, Object>> response = ejecutarGet(config, url, ENDPOINT_SEARCH);

            if (response != null && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                total = (Integer) body.getOrDefault("total", 0);
                List<Map<String, Object>> issues = (List<Map<String, Object>>) body.getOrDefault("issues", List.of());
                resultado.addAll(issues);
                startAt += issues.size();
                log.debug("[JiraApiClient] buscarIssues — obtenidos: {}/{}", resultado.size(), total);
            } else {
                break;
            }
        }

        if (startAt < total) {
            log.warn("[JiraApiClient] buscarIssues interrumpida por cuota. Obtenidos: {}/{}", resultado.size(), total);
        }

        return resultado;
    }

    /**
     * Obtiene los worklogs de una issue concreta.
     *
     * @param config   configuración Jira del squad
     * @param issueKey clave de la issue (ej: RED-123)
     * @return lista de maps con datos de cada worklog
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> obtenerWorklogs(JiraConfig config, String issueKey) {
        if (!rateLimiter.canMakeCall()) {
            log.warn("[JiraApiClient] obtenerWorklogs({}) — cuota agotada, omitiendo.", issueKey);
            return List.of();
        }

        String url = config.getUrl() + ENDPOINT_WORKLOG.replace("{issueKey}", issueKey);
        log.debug("[JiraApiClient] obtenerWorklogs — issueKey: {}", issueKey);

        ResponseEntity<Map<String, Object>> response = ejecutarGet(config, url, ENDPOINT_WORKLOG);

        if (response != null && response.getBody() != null) {
            return (List<Map<String, Object>>) response.getBody().getOrDefault("worklogs", List.of());
        }
        return List.of();
    }

    /**
     * Obtiene el detalle completo de una issue por su clave.
     *
     * @param config   configuración Jira del squad
     * @param issueKey clave de la issue (ej: RED-123)
     * @return map con todos los campos de la issue, o null si no se encuentra o cuota agotada
     */
    public Map<String, Object> obtenerIssue(JiraConfig config, String issueKey) {
        if (!rateLimiter.canMakeCall()) {
            log.warn("[JiraApiClient] obtenerIssue({}) — cuota agotada.", issueKey);
            return null;
        }

        String url = config.getUrl() + ENDPOINT_ISSUE.replace("{issueKey}", issueKey);
        ResponseEntity<Map<String, Object>> response = ejecutarGet(config, url, ENDPOINT_ISSUE);
        return response != null ? response.getBody() : null;
    }

    /**
     * Obtiene los remote links de una issue concreta.
     *
     * <p>Endpoint: GET /rest/api/2/issue/{issueKey}/remotelink.
     * Devuelve un array directo (no paginado) con los remote links de la issue.</p>
     *
     * @param config   configuración Jira del squad
     * @param issueKey clave de la issue (ej: RED-123)
     * @return lista de maps con datos de cada remote link (puede estar vacía)
     */
    public List<Map<String, Object>> obtenerRemoteLinks(JiraConfig config, String issueKey) {
        if (!rateLimiter.canMakeCall()) {
            log.warn("[JiraApiClient] obtenerRemoteLinks({}) — cuota agotada, omitiendo.", issueKey);
            return List.of();
        }

        String url = config.getUrl() + ENDPOINT_REMOTELINK.replace("{issueKey}", issueKey);
        log.debug("[JiraApiClient] obtenerRemoteLinks — issueKey: {}", issueKey);

        try {
            HttpHeaders headers = buildHeaders(config);
            URI uri = new URI(url);
            Integer statusCode = null;
            try {
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                        uri, HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {});
                statusCode = response.getStatusCode().value();
                return response.getBody() != null ? response.getBody() : List.of();
            } catch (HttpClientErrorException e) {
                statusCode = e.getStatusCode().value();
                log.warn("[JiraApiClient] Error HTTP {} obteniendo remote links de {}: {}", statusCode, issueKey, e.getMessage());
                return List.of();
            } finally {
                rateLimiter.registrarLlamada(ENDPOINT_REMOTELINK, "GET", statusCode,
                        config.getSquad() != null ? config.getSquad().getId() : null);
            }
        } catch (Exception e) {
            log.error("[JiraApiClient] Error inesperado obteniendo remote links de {}: {}", issueKey, e.getMessage());
            return List.of();
        }
    }

    /**
     * Comprueba la conectividad con Jira y valida las credenciales.
     *
     * @param config configuración Jira a probar
     * @return true si la conexión es exitosa (HTTP 2xx)
     */
    public boolean probarConexion(JiraConfig config) {
        try {
            String url = config.getUrl() + ENDPOINT_MYSELF;
            HttpHeaders headers = buildHeaders(config);
            URI uri = new URI(url);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    uri, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {});
            log.info("[JiraApiClient] Conexión a Jira OK — usuario: {}, status: {}",
                    config.getUsuario(), response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("[JiraApiClient] Error al probar conexión Jira: {}", e.getMessage());
            return false;
        }
    }

    // ── privado ──────────────────────────────────────────────────────────────

    /**
     * Ejecuta una petición GET registrando la llamada en el rate limiter.
     * Maneja errores HTTP con log de advertencia (no lanza excepción al caller).
     */
    private ResponseEntity<Map<String, Object>> ejecutarGet(JiraConfig config, String url, String endpointKey) {
        Integer statusCode = null;
        try {
            HttpHeaders headers = buildHeaders(config);
            // Usar URI para evitar doble codificación: el URL ya llega encoded por encodeUrl()
            URI uri = new URI(url);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    uri, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {});
            statusCode = response.getStatusCode().value();
            return response;
        } catch (HttpClientErrorException e) {
            statusCode = e.getStatusCode().value();
            log.warn("[JiraApiClient] Error HTTP {} en {}: {}", statusCode, endpointKey, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[JiraApiClient] Error inesperado en {}: {}", endpointKey, e.getMessage(), e);
            return null;
        } finally {
            rateLimiter.registrarLlamada(endpointKey, "GET", statusCode, config.getSquad() != null ? config.getSquad().getId() : null);
        }
    }

    /**
     * Construye los headers HTTP con Basic Auth para el squad dado.
     */
    private HttpHeaders buildHeaders(JiraConfig config) {
        String credentials = config.getUsuario() + ":" + config.getToken();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.set(HttpHeaders.ACCEPT, "application/json");
        return headers;
    }

    /** Codifica un string para uso seguro en URL (espacios, operadores JQL). */
    private String encodeUrl(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }
}
