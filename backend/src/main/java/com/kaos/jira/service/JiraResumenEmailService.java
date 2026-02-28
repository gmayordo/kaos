package com.kaos.jira.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.common.email.EmailService;
import com.kaos.jira.alert.entity.JiraAlertRule.Severidad;
import com.kaos.jira.alert.entity.JiraAlerta;
import com.kaos.jira.alert.repository.JiraAlertaRepository;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.entity.JiraSyncStatus;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.repository.SprintRepository;

/**
 * Genera y envía el correo HTML de resumen post-sincronización Jira.
 *
 * <p>Invocado desde {@link JiraSyncService} tras cada sync exitosa y opcionalmente
 * desde el batch scheduler si {@code kaos.email.resumen-sync.enviar-en-batch=true}.</p>
 *
 * <p>Control de envío:</p>
 * <ul>
 *   <li>{@code kaos.email.habilitado=false} → EmailService descarta silenciosamente</li>
 *   <li>{@code kaos.email.resumen-sync.enviar-solo-si-alertas=true} → envía solo si hay alertas CRITICO pendientes</li>
 * </ul>
 */
@Service
public class JiraResumenEmailService {

    private static final Logger log = LoggerFactory.getLogger(JiraResumenEmailService.class);
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailService emailService;
    private final SprintRepository sprintRepository;
    private final JiraIssueRepository issueRepository;
    private final JiraAlertaRepository alertaRepository;

    @Value("${kaos.email.resumen-sync.destinatarios:}")
    private String destinatarios;

    @Value("${kaos.email.resumen-sync.enviar-solo-si-alertas:false}")
    private boolean enviarSoloSiAlertas;

    public JiraResumenEmailService(
            EmailService emailService,
            SprintRepository sprintRepository,
            JiraIssueRepository issueRepository,
            JiraAlertaRepository alertaRepository) {
        this.emailService = emailService;
        this.sprintRepository = sprintRepository;
        this.issueRepository = issueRepository;
        this.alertaRepository = alertaRepository;
    }

    /**
     * Genera y envía el resumen de sincronización para el sprint activo del squad.
     *
     * @param status   estado de la sync recién completada
     * @param squadId  ID del squad
     */
    @Transactional(readOnly = true)
    public void enviarResumenSync(JiraSyncStatus status, Long squadId) {
        try {
            List<Sprint> sprintsActivos = sprintRepository.findBySquadIdAndEstado(squadId, SprintEstado.ACTIVO);
            if (sprintsActivos.isEmpty()) {
                log.debug("[JiraResumenEmailService] Sin sprint activo para squadId={}", squadId);
                return;
            }
            Sprint sprint = sprintsActivos.get(0);

            List<JiraIssue> issues = issueRepository.findBySprintId(sprint.getId());
            List<JiraAlerta> alertasPendientes = alertaRepository
                    .findBySprintId(sprint.getId(), false, Pageable.unpaged())
                    .getContent();

            // Condición de envío condicional
            if (enviarSoloSiAlertas) {
                boolean hayCriticos = alertasPendientes.stream()
                        .anyMatch(a -> Severidad.CRITICO.equals(a.getSeveridad()));
                if (!hayCriticos) {
                    log.debug("[JiraResumenEmailService] Sin alertas CRITICO — omitiendo envío (enviar-solo-si-alertas=true)");
                    return;
                }
            }

            String html = generarHtml(status, sprint, issues, alertasPendientes);
            String asunto = String.format("KAOS — Resumen Sync Jira · %s · %s",
                    status.getSquad() != null ? status.getSquad().getNombre() : "Squad " + squadId,
                    LocalDate.now().format(FMT_DATE));

            emailService.enviarHtml(destinatarios, asunto, html);

        } catch (Exception e) {
            // Email no debe interrumpir el flujo de sync
            log.error("[JiraResumenEmailService] Error al generar/enviar resumen squadId={}: {}", squadId, e.getMessage(), e);
        }
    }

    /**
     * Genera el HTML del email de resumen.
     *
     * @param status   estado de la sync
     * @param sprint   sprint activo
     * @param issues   issues del sprint
     * @param alertas  alertas pendientes del sprint
     * @return HTML completo como String
     */
    String generarHtml(JiraSyncStatus status, Sprint sprint,
                       List<JiraIssue> issues, List<JiraAlerta> alertas) {

        LocalDate hoy = LocalDate.now();
        LocalDate inicio = sprint.getFechaInicio();
        LocalDate fin = sprint.getFechaFin();

        long totalDias = ChronoUnit.DAYS.between(inicio, fin) + 1;
        long diasTranscurridos = Math.min(ChronoUnit.DAYS.between(inicio, hoy) + 1, totalDias);
        int pctTiempo = totalDias > 0 ? (int) (diasTranscurridos * 100 / totalDias) : 0;

        // Estadísticas de issues por estado Jira
        Map<String, Long> porEstadoJira = issues.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getEstadoJira() != null ? i.getEstadoJira() : "Sin estado",
                        Collectors.counting()));

        long completadas  = porEstadoJira.getOrDefault("Done", 0L);
        long enProgreso   = porEstadoJira.getOrDefault("In Progress", 0L);
        long pendientes   = issues.size() - completadas - enProgreso;
        int  pctCompleto  = issues.isEmpty() ? 0 : (int) (completadas * 100 / issues.size());

        // Horas estimadas totales
        double horasEstimadas = issues.stream()
                .mapToDouble(i -> i.getEstimacionHoras() != null ? i.getEstimacionHoras().doubleValue() : 0)
                .sum();
        double horasConsumidas = issues.stream()
                .mapToDouble(i -> i.getHorasConsumidas() != null ? i.getHorasConsumidas().doubleValue() : 0)
                .sum();

        // Alertas por severidad
        Map<Severidad, List<JiraAlerta>> porSeveridad = alertas.stream()
                .collect(Collectors.groupingBy(JiraAlerta::getSeveridad));
        List<JiraAlerta> criticos = porSeveridad.getOrDefault(Severidad.CRITICO, List.of());
        List<JiraAlerta> avisos   = porSeveridad.getOrDefault(Severidad.AVISO,   List.of());
        List<JiraAlerta> infos    = porSeveridad.getOrDefault(Severidad.INFO,    List.of());

        // ── Estado por persona (issues agrupadas por asignadoJira) ────────────
        Map<String, Long> porPersona = issues.stream()
                .filter(i -> i.getAsignadoJira() != null)
                .collect(Collectors.groupingBy(JiraIssue::getAsignadoJira, Collectors.counting()));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>")
            .append("<meta charset=\"UTF-8\">")
            .append("<style>")
            .append("body{font-family:Arial,sans-serif;font-size:14px;color:#333;background:#f5f5f5;margin:0;padding:0}")
            .append(".container{max-width:700px;margin:20px auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}")
            .append(".header{background:#1a237e;color:#fff;padding:20px 24px}")
            .append(".header h1{margin:0;font-size:18px}")
            .append(".header p{margin:4px 0 0;opacity:.8;font-size:12px}")
            .append(".section{padding:16px 24px;border-bottom:1px solid #eee}")
            .append(".section h2{font-size:14px;margin:0 0 12px;color:#1a237e}")
            .append(".kpi{display:inline-block;margin:4px 8px 4px 0;padding:4px 10px;border-radius:4px;font-size:13px}")
            .append(".kpi-blue{background:#e3f2fd;color:#1565c0}")
            .append(".kpi-green{background:#e8f5e9;color:#2e7d32}")
            .append(".kpi-red{background:#ffebee;color:#c62828}")
            .append(".kpi-yellow{background:#fff8e1;color:#f57f17}")
            .append(".kpi-grey{background:#f5f5f5;color:#555}")
            .append(".alerta{padding:6px 10px;margin:4px 0;border-radius:4px;font-size:12px}")
            .append(".alerta-critico{background:#ffebee;border-left:4px solid #c62828}")
            .append(".alerta-aviso{background:#fff8e1;border-left:4px solid #f57f17}")
            .append(".alerta-info{background:#e8f5e9;border-left:4px solid #388e3c}")
            .append(".progress{height:10px;background:#e0e0e0;border-radius:5px;overflow:hidden;margin:6px 0}")
            .append(".progress-fill{height:100%;background:#1a237e;border-radius:5px}")
            .append(".footer{padding:12px 24px;background:#f5f5f5;font-size:11px;color:#888}")
            .append("</style></head><body><div class=\"container\">");

        // ── Cabecera ──────────────────────────────────────────────────────────
        String squadNombre = status.getSquad() != null ? status.getSquad().getNombre() : "Squad";
        html.append("<div class=\"header\">")
            .append("<h1>&#128231; KAOS — Resumen Sincronización Jira</h1>")
            .append("<p>").append(squadNombre).append(" · ").append(hoy.format(FMT_DATE))
            .append(" · ").append(status.getIssuesImportadas()).append(" issues · ")
            .append(status.getWorklogsImportados()).append(" worklogs</p>")
            .append("</div>");

        // ── Estado del sprint ─────────────────────────────────────────────────
        html.append("<div class=\"section\">")
            .append("<h2>&#128309; Estado del Sprint</h2>")
            .append("<span class=\"kpi kpi-blue\">").append(sprint.getNombre()).append("</span>")
            .append("<span class=\"kpi kpi-blue\">Día ").append(diasTranscurridos).append("/").append(totalDias).append("</span>")
            .append("<span class=\"kpi kpi-blue\">").append(inicio.format(FMT_DATE)).append(" → ").append(fin.format(FMT_DATE)).append("</span>")
            .append("<br><br>")
            .append("<strong>Tiempo transcurrido: ").append(pctTiempo).append("%</strong>")
            .append("<div class=\"progress\"><div class=\"progress-fill\" style=\"width:").append(pctTiempo).append("%\"></div></div>")
            .append("<strong>Completitud: ").append(pctCompleto).append("%</strong>")
            .append("<div class=\"progress\"><div class=\"progress-fill\" style=\"width:").append(pctCompleto).append("%;background:#2e7d32\"></div></div>")
            .append("</div>");

        // ── Tareas ────────────────────────────────────────────────────────────
        html.append("<div class=\"section\">")
            .append("<h2>&#128202; Estado de Tareas (").append(issues.size()).append(" issues)</h2>")
            .append("<span class=\"kpi kpi-green\">&#9989; Done: ").append(completadas).append("</span>")
            .append("<span class=\"kpi kpi-blue\">&#9654; En progreso: ").append(enProgreso).append("</span>")
            .append("<span class=\"kpi kpi-grey\">&#9711; Pendientes: ").append(pendientes).append("</span>")
            .append("<br><br>")
            .append("<span class=\"kpi kpi-blue\">Estimado: ").append(String.format("%.1f", horasEstimadas)).append("h</span>")
            .append("<span class=\"kpi kpi-yellow\">Consumido: ").append(String.format("%.1f", horasConsumidas)).append("h</span>")
            .append("</div>");

        // ── Novedades ─────────────────────────────────────────────────────────
        html.append("<div class=\"section\">")
            .append("<h2>&#9889; Novedades desde último sync</h2>")
            .append("<span class=\"kpi kpi-green\">+ ").append(status.getIssuesImportadas()).append(" issues importadas</span>")
            .append("<span class=\"kpi kpi-blue\">&#9650; ").append(status.getWorklogsImportados()).append(" worklogs importados</span>")
            .append("</div>");

        // ── Alertas ───────────────────────────────────────────────────────────
        html.append("<div class=\"section\">")
            .append("<h2>&#128680; Alertas de Incoherencia (").append(alertas.size()).append(" pendientes)</h2>");

        if (alertas.isEmpty()) {
            html.append("<span class=\"kpi kpi-green\">&#9989; Sin alertas pendientes</span>");
        } else {
            for (JiraAlerta a : criticos) {
                html.append("<div class=\"alerta alerta-critico\"><strong>[CRÍTICO]</strong> ").append(escapeHtml(a.getMensaje())).append("</div>");
            }
            for (JiraAlerta a : avisos) {
                html.append("<div class=\"alerta alerta-aviso\"><strong>[AVISO]</strong> ").append(escapeHtml(a.getMensaje())).append("</div>");
            }
            for (JiraAlerta a : infos) {
                html.append("<div class=\"alerta alerta-info\"><strong>[INFO]</strong> ").append(escapeHtml(a.getMensaje())).append("</div>");
            }
        }
        html.append("</div>");

        // ── Estado por persona ────────────────────────────────────────────────
        if (!porPersona.isEmpty()) {
            html.append("<div class=\"section\">")
                .append("<h2>&#128101; Estado por Persona</h2>");
            porPersona.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(e -> html
                            .append("<span class=\"kpi kpi-blue\">")
                            .append(escapeHtml(e.getKey()))
                            .append(": ").append(e.getValue()).append(" issue(s)</span> "));
            html.append("</div>");
        }

        // ── Pie ───────────────────────────────────────────────────────────────
        html.append("<div class=\"footer\">")
            .append("Generado automáticamente por KAOS · Integración Jira · ")
            .append(hoy.format(FMT_DATE))
            .append("</div></div></body></html>");

        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
