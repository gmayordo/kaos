package com.kaos.jira.service;

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
