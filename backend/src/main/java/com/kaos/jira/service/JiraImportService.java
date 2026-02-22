package com.kaos.jira.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.dto.JiraSyncStatus;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.planificacion.entity.Sprint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de importación de issues desde Jira.
 * Procesa y persiste las issues recibidas de la API de Jira,
 * vinculándolas al sprint KAOS activo cuando corresponda.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JiraImportService {

    private final JiraIssueRepository jiraIssueRepository;

    /**
     * Procesa e importa una lista de issues de Jira.
     * Crea o actualiza cada issue en la base de datos y la vincula
     * al sprint KAOS activo si se proporciona.
     *
     * @param issues  Lista de issues obtenidas de la API Jira (campo "issues" del JSON)
     * @param config  Configuración Jira del squad
     * @param sprint  Sprint KAOS activo (puede ser null si no hay sprint activo)
     * @param status  Objeto de estado para acumular resultados y errores
     */
    @Transactional
    public void processIssues(
            List<Map<String, Object>> issues,
            JiraConfig config,
            Sprint sprint,
            JiraSyncStatus status) {

        if (issues == null || issues.isEmpty()) {
            log.debug("No hay issues para procesar en config {}", config.getId());
            return;
        }

        for (Map<String, Object> issue : issues) {
            try {
                String jiraKey = (String) issue.get("key");
                if (jiraKey == null) {
                    log.warn("Issue sin clave Jira, se omite");
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                if (fields == null) {
                    log.warn("Issue {} sin campos, se omite", jiraKey);
                    continue;
                }

                String titulo = (String) fields.get("summary");
                String tipo = extraerNombre(fields, "issuetype");
                String estado = extraerNombre(fields, "status");

                JiraIssue jiraIssue = jiraIssueRepository.findByJiraKey(jiraKey)
                        .orElse(JiraIssue.builder()
                                .jiraKey(jiraKey)
                                .config(config)
                                .build());

                boolean esNueva = jiraIssue.getId() == null;
                jiraIssue.setTitulo(titulo != null ? titulo : jiraKey);
                jiraIssue.setTipo(tipo);
                jiraIssue.setEstado(estado);
                jiraIssue.setSprint(sprint);

                jiraIssueRepository.save(jiraIssue);

                if (esNueva) {
                    status.setIssuesImportadas(status.getIssuesImportadas() + 1);
                    log.debug("Issue importada: {}", jiraKey);
                } else {
                    status.setIssuesActualizadas(status.getIssuesActualizadas() + 1);
                    log.debug("Issue actualizada: {}", jiraKey);
                }

            } catch (Exception e) {
                String msg = "Error procesando issue: " + e.getMessage();
                log.error(msg, e);
                status.addError(msg);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String extraerNombre(Map<String, Object> fields, String campo) {
        Object obj = fields.get(campo);
        if (obj instanceof Map) {
            return (String) ((Map<String, Object>) obj).get("name");
        }
        return null;
    }
}
