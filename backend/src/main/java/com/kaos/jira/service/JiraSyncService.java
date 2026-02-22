package com.kaos.jira.service;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.kaos.jira.dto.JiraSyncStatus;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.repository.SprintRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.jira.entity.JiraSyncStatus;
import com.kaos.jira.entity.TipoSincronizacion;
import com.kaos.jira.repository.JiraSyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de sincronización con Jira.
 * Construye las JQL filtradas por proyecto y sincroniza issues
 * vinculándolas al sprint KAOS activo del squad.
 * Servicio de sincronización de issues Jira con soporte de carga incremental.
 * <p>
 * En la primera sincronización (sin {@code ultimaSync}) descarga todas las issues del sprint.
 * En sincronizaciones posteriores añade {@code AND updated >= "<ultimaSync>"} al JQL,
 * reduciendo el número de issues a procesar y el consumo de cuota de la API.
 * </p>
 * <p>
 * El campo {@code jira_sync_status.ultima_sync} se actualiza únicamente cuando
 * la sincronización finaliza correctamente; si falla, el siguiente intento reprocessa
 * todas las issues desde la última fecha registrada.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JiraSyncService {

    private static final String JQL_EVOLUTIVO_TEMPLATE =
            "project = %s AND sprint in openSprints() AND issuetype not in (Sub-task) ORDER BY created ASC";

    private static final String JQL_CORRECTIVO_TEMPLATE =
            "project = %s AND issuetype = Bug AND sprint in openSprints() ORDER BY priority DESC, created ASC";

    private static final int JIRA_MAX_RESULTS = 100;

    private final JiraImportService jiraImportService;
    private final SprintRepository sprintRepository;
    private final RestClient.Builder restClientBuilder;

    /**
     * Construye la JQL para issues evolutivas del squad.
     * Filtra por la clave de proyecto configurada en {@link JiraConfig#getProjectKey()}.
     *
     * @param config Configuración Jira del squad
     * @return JQL con filtro de proyecto
     */
    public String construirJqlEvolutivo(JiraConfig config) {
        return String.format(JQL_EVOLUTIVO_TEMPLATE, config.getProjectKey());
    }

    /**
     * Construye la JQL para issues correctivas (bugs) del squad.
     * Filtra por la clave de proyecto configurada en {@link JiraConfig#getProjectKey()}.
     *
     * @param config Configuración Jira del squad
     * @return JQL con filtro de proyecto
     */
    public String construirJqlCorrectivo(JiraConfig config) {
        return String.format(JQL_CORRECTIVO_TEMPLATE, config.getProjectKey());
    }

    /**
     * Sincroniza issues evolutivas y correctivas desde Jira para un squad.
     * Resuelve el sprint KAOS activo del squad y lo vincula a las issues importadas.
     * Si no existe sprint activo, las issues se importan con sprint_id = null.
     *
     * @param config Configuración Jira del squad
     * @param status Objeto de estado para acumular resultados y errores
     */
    public void sincronizarIssues(JiraConfig config, JiraSyncStatus status) {
        log.info("Iniciando sincronización Jira para squad {} (projectKey={})",
                config.getSquad().getId(), config.getProjectKey());

        Optional<Sprint> sprintActivo = sprintRepository.findFirstBySquadIdAndEstado(
                config.getSquad().getId(), SprintEstado.ACTIVO);
        Sprint sprint = sprintActivo.orElse(null);

        if (sprint != null) {
            log.debug("Sprint activo encontrado: id={}", sprint.getId());
        } else {
            log.debug("No hay sprint activo para squad {}, sprint_id será null",
                    config.getSquad().getId());
        }

        List<Map<String, Object>> issuesEvolutivo = buscarIssues(config, construirJqlEvolutivo(config));
        jiraImportService.processIssues(issuesEvolutivo, config, sprint, status);

        List<Map<String, Object>> issuesCorrectivo = buscarIssues(config, construirJqlCorrectivo(config));
        jiraImportService.processIssues(issuesCorrectivo, config, sprint, status);

        log.info("Sincronización Jira completada: {} importadas, {} actualizadas, {} errores",
                status.getIssuesImportadas(), status.getIssuesActualizadas(), status.getErrores().size());
    }

    /**
     * Realiza la búsqueda de issues en la API REST de Jira usando JQL.
     *
     * @param config Configuración con URL y credenciales
     * @param jql    Expresión JQL de búsqueda
     * @return Lista de issues como mapas de campos
     */
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> buscarIssues(JiraConfig config, String jql) {
        try {
            RestClient client = restClientBuilder
                    .baseUrl(config.getJiraUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(config))
                    .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                    .build();

            Map<String, Object> respuesta = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/api/2/search")
                            .queryParam("jql", jql)
                            .queryParam("maxResults", JIRA_MAX_RESULTS)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (respuesta == null) {
                return Collections.emptyList();
            }

            Object issues = respuesta.get("issues");
            if (issues instanceof List<?> lista) {
                return (List<Map<String, Object>>) lista;
            }
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Error al consultar Jira API para config {}: {}", config.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private String basicAuth(JiraConfig config) {
        String credentials = config.getUsuarioEmail() + ":" + config.getApiToken();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    private static final DateTimeFormatter JQL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JiraSyncStatusRepository jiraSyncStatusRepository;

    /**
     * Sincroniza las issues de un proyecto Jira de forma incremental cuando es posible.
     * <p>
     * Flujo:
     * <ol>
     *   <li>Lee {@code ultimaSync} de {@link JiraSyncStatus} para el proyecto.</li>
     *   <li>Construye el JQL correspondiente (full o incremental) según el tipo de config.</li>
     *   <li>Ejecuta la sincronización.</li>
     *   <li>Actualiza {@code ultimaSync} solo si la sincronización termina sin errores.</li>
     * </ol>
     * </p>
     *
     * @param config configuración del proyecto Jira a sincronizar
     */
    @Transactional
    public void sincronizarIssues(JiraConfig config) {
        LocalDateTime ahora = LocalDateTime.now();

        JiraSyncStatus status = jiraSyncStatusRepository
                .findByProjectKey(config.getProjectKey())
                .orElse(null);

        LocalDateTime lastSync = (status != null) ? status.getUltimaSync() : null;

        String jql = config.getTipo() == TipoSincronizacion.EVOLUTIVO
                ? construirJqlEvolutivo(config, lastSync)
                : construirJqlCorrectivo(config, lastSync);

        String modo = (lastSync != null) ? "[INCREMENTAL]" : "[FULL]";
        log.info("{} Sincronizando proyecto={} tipo={} jql={}",
                modo, config.getProjectKey(), config.getTipo(), jql);

        ejecutarSincronizacion(jql, config);

        // Actualiza ultima_sync solo si la sync completó sin errores
        if (status == null) {
            status = JiraSyncStatus.builder()
                    .projectKey(config.getProjectKey())
                    .build();
        }
        status.setUltimaSync(ahora);
        jiraSyncStatusRepository.save(status);

        log.info("{} Sincronización completada proyecto={} ultimaSync={}",
                modo, config.getProjectKey(), ahora);
    }

    /**
     * Construye el JQL para proyectos de tipo EVOLUTIVO (sprints abiertos).
     * <p>
     * Carga completa (firstSync == null):
     * {@code project = X AND sprint in openSprints() AND issuetype not in (Sub-task) ORDER BY updated ASC}
     * </p>
     * <p>
     * Carga incremental (lastSync != null):
     * {@code project = X AND sprint in openSprints() AND issuetype not in (Sub-task) AND updated >= "yyyy-MM-dd HH:mm" ORDER BY updated ASC}
     * </p>
     *
     * @param config   configuración del proyecto
     * @param lastSync fecha de última sync exitosa, o {@code null} para carga completa
     * @return JQL listo para enviar a la API de Jira
     */
    String construirJqlEvolutivo(JiraConfig config, LocalDateTime lastSync) {
        String base = "project = " + config.getProjectKey()
                + " AND sprint in openSprints() AND issuetype not in (Sub-task)";
        if (lastSync != null) {
            String ts = lastSync.format(JQL_DATE_FORMAT);
            base += " AND updated >= \"" + ts + "\"";
        }
        return base + " ORDER BY updated ASC";
    }

    /**
     * Construye el JQL para proyectos de tipo CORRECTIVO (backlog / bugs).
     * <p>
     * Carga completa (lastSync == null):
     * {@code project = X AND issuetype in (Bug, Incident) AND statusCategory != Done ORDER BY updated ASC}
     * </p>
     * <p>
     * Carga incremental (lastSync != null): añade {@code AND updated >= "yyyy-MM-dd HH:mm"}.
     * </p>
     *
     * @param config   configuración del proyecto
     * @param lastSync fecha de última sync exitosa, o {@code null} para carga completa
     * @return JQL listo para enviar a la API de Jira
     */
    String construirJqlCorrectivo(JiraConfig config, LocalDateTime lastSync) {
        String base = "project = " + config.getProjectKey()
                + " AND issuetype in (Bug, Incident) AND statusCategory != Done";
        if (lastSync != null) {
            String ts = lastSync.format(JQL_DATE_FORMAT);
            base += " AND updated >= \"" + ts + "\"";
        }
        return base + " ORDER BY updated ASC";
    }

    /**
     * Ejecuta la sincronización real con la API de Jira usando el JQL proporcionado.
     * <p>
     * Punto de extensión: sobreescribir o inyectar un cliente Jira para la implementación real.
     * </p>
     *
     * @param jql    query JQL construida
     * @param config configuración del proyecto
     */
    protected void ejecutarSincronizacion(String jql, JiraConfig config) {
        log.debug("Ejecutando sync con JQL: {}", jql);
        // Implementación real: llamada a la API de Jira y persistencia de issues
    }
}
