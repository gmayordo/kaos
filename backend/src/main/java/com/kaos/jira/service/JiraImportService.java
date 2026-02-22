package com.kaos.jira.service;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.entity.JiraIssueTypeConfig;
import com.kaos.jira.repository.JiraIssueTypeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de importación de issues desde Jira.
 * Detecta el sub-tipo kaos de las sub-tasks según los patrones configurados
 * en {@link JiraIssueTypeConfig} y los asigna al campo {@code subtipoJira}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JiraImportService {

    private static final String SUBTIPO_OTROS = "OTROS";

    private final JiraIssueTypeConfigRepository jiraIssueTypeConfigRepository;

    /**
     * Procesa un issue importado desde Jira.
     * Si el issue es una sub-task (parentKey != null), detecta su subtipo
     * y lo asigna al campo {@code subtipoJira}.
     * Issues que no son sub-tasks quedan con {@code subtipoJira = null}.
     *
     * @param issue issue a procesar (se modifica en sitio)
     */
    @Transactional
    public void processIssue(JiraIssue issue) {
        if (issue.getParentKey() == null) {
            log.debug("Issue {} no es sub-task, subtipoJira queda null", issue.getIssueKey());
            issue.setSubtipoJira(null);
            return;
        }

        Long squadId = issue.getSquad().getId();
        String tipoJira = issue.getTipoJira();

        List<JiraIssueTypeConfig> configs =
                jiraIssueTypeConfigRepository.findBySquadIdAndTipoJiraAndActivaTrue(squadId, tipoJira);

        String subtipo = detectarSubtipo(issue.getSummary(), configs);
        issue.setSubtipoJira(subtipo);
        log.debug("Issue {} (sub-task) → subtipoJira={}", issue.getIssueKey(), subtipo);
    }

    /**
     * Detecta el sub-tipo kaos de una sub-task comparando su summary
     * con los patrones de las configuraciones activas del squad.
     * El primer patrón que encaje (regex evaluado sobre el summary completo,
     * con bandera {@code CASE_INSENSITIVE}) determina el sub-tipo.
     * Si ninguno encaja devuelve {@value #SUBTIPO_OTROS}.
     *
     * @param summary resumen/título del issue
     * @param configs configuraciones activas para el tipo de issue
     * @return sub-tipo kaos detectado
     */
    String detectarSubtipo(String summary, List<JiraIssueTypeConfig> configs) {
        if (summary == null || configs == null) {
            return SUBTIPO_OTROS;
        }
        for (JiraIssueTypeConfig cfg : configs) {
            if (cfg.getPatronNombre() != null
                    && Pattern.compile(cfg.getPatronNombre(), Pattern.CASE_INSENSITIVE)
                              .matcher(summary)
                              .matches()) {
                return cfg.getSubtipoKaos();
            }
        }
        return SUBTIPO_OTROS;
    }
}
