package com.kaos.jira.alert.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.alert.entity.JiraAlertRule;
import com.kaos.jira.alert.entity.JiraAlerta;
import com.kaos.jira.alert.repository.JiraAlertRuleRepository;
import com.kaos.jira.alert.repository.JiraAlertaRepository;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.jira.repository.JiraWorklogRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.entity.Tarea;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.planificacion.repository.TareaRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Motor de evaluación de reglas de coherencia Jira←→KAOS.
 *
 * <p>Evalúa las reglas activas configuradas en {@code jira_alert_rule} usando
 * Spring Expression Language (SpEL). Cada regla recibe un contexto de evaluación
 * adaptado a su tipo (por issue, por persona, a nivel de sprint) y genera una
 * {@link JiraAlerta} por cada condición que se cumpla.</p>
 *
 * <p>Se invoca automáticamente al final de cada sync en
 * {@code JiraSyncService.syncCompleta()}.</p>
 */
@Service
@Transactional(readOnly = true)
public class JiraAlertEngineService {

    private static final Logger log = LoggerFactory.getLogger(JiraAlertEngineService.class);

    private final JiraAlertRuleRepository ruleRepository;
    private final JiraAlertaRepository alertaRepository;
    private final JiraIssueRepository issueRepository;
    private final TareaRepository tareaRepository;
    private final PersonaRepository personaRepository;
    private final SprintRepository sprintRepository;
    private final JiraWorklogRepository worklogRepository;

    /** Parser SpEL reutilizable (thread-safe). */
    private final ExpressionParser spelParser = new SpelExpressionParser();

    public JiraAlertEngineService(
            JiraAlertRuleRepository ruleRepository,
            JiraAlertaRepository alertaRepository,
            JiraIssueRepository issueRepository,
            TareaRepository tareaRepository,
            PersonaRepository personaRepository,
            SprintRepository sprintRepository,
            JiraWorklogRepository worklogRepository) {
        this.ruleRepository = ruleRepository;
        this.alertaRepository = alertaRepository;
        this.issueRepository = issueRepository;
        this.tareaRepository = tareaRepository;
        this.personaRepository = personaRepository;
        this.sprintRepository = sprintRepository;
        this.worklogRepository = worklogRepository;
    }

    // ─────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────

    /**
     * Evalúa todas las reglas activas para el sprint indicado y persiste las alertas generadas.
     * Invocado al final de cada sincronización completa.
     *
     * @param sprintId ID del sprint a evaluar
     * @param squadId  ID del squad propietario
     * @return lista de alertas generadas (ya persistidas)
     */
    @Transactional
    public List<JiraAlerta> evaluar(Long sprintId, Long squadId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado: " + sprintId));

        List<JiraAlertRule> reglas = ruleRepository.findActivasBySquadIdOrGlobal(squadId);
        if (reglas.isEmpty()) {
            log.debug("evaluarReglas: no hay reglas activas para squad={}", squadId);
            return List.of();
        }

        SprintAlertContext ctx = buildContext(sprint, squadId);

        List<JiraAlerta> alertas = reglas.stream()
                .flatMap(regla -> evaluarRegla(regla, ctx).stream())
                .toList();

        if (!alertas.isEmpty()) {
            alertaRepository.saveAll(alertas);
            log.info("Alertas generadas: {} (sprint={} squad={})", alertas.size(), sprintId, squadId);
        } else {
            log.debug("Evaluación sin alertas: sprint={} squad={}", sprintId, squadId);
        }

        return alertas;
    }

    /**
     * Evalúa las reglas del sprint activo del squad (si existe).
     * Método de conveniencia para invocar desde JiraSyncService pasando solo squadId.
     *
     * @param squadId ID del squad
     * @return lista de alertas generadas, vacía si no hay sprint activo
     */
    @Transactional
    public List<JiraAlerta> evaluarSprintActivo(Long squadId) {
        List<Sprint> activos = sprintRepository.findBySquadIdAndEstado(squadId, SprintEstado.ACTIVO);
        if (activos.isEmpty()) {
            log.debug("evaluarSprintActivo: squad={} sin sprint ACTIVO", squadId);
            return List.of();
        }
        return evaluar(activos.get(0).getId(), squadId);
    }

    // ─────────────────────────────────────────────
    // Construcción del contexto
    // ─────────────────────────────────────────────

    /**
     * Carga los datos necesarios para evaluar todas las reglas del sprint.
     */
    private SprintAlertContext buildContext(Sprint sprint, Long squadId) {
        List<JiraIssue> issues = issueRepository.findBySprintId(sprint.getId());

        // Mapa jiraKey → Tarea KAOS para cruzar estados
        Map<String, Tarea> jiraKeyToTarea = tareaRepository
                .findBySprintIdWithJiraKey(sprint.getId())
                .stream()
                .collect(Collectors.toMap(Tarea::getJiraKey, t -> t, (a, b) -> a));

        List<Persona> personas = personaRepository
                .findWithFilters(squadId, null, null, null, true, Pageable.unpaged())
                .getContent();

        LocalDate today = LocalDate.now();
        LocalDate inicio = sprint.getFechaInicio();
        LocalDate fin = sprint.getFechaFin() != null ? sprint.getFechaFin() : today;

        long totalDias = Math.max(1, ChronoUnit.DAYS.between(inicio, fin) + 1);
        long diasTranscurridos = Math.max(0,
                Math.min(ChronoUnit.DAYS.between(inicio, today) + 1, totalDias));

        int pctTiempo = (int) (diasTranscurridos * 100 / totalDias);

        long totalIssues = issues.size();
        long completadas = issues.stream()
                .filter(i -> "Done".equalsIgnoreCase(i.getEstadoJira()))
                .count();
        int pctCompletitud = totalIssues > 0 ? (int) (completadas * 100 / totalIssues) : 0;

        return new SprintAlertContext(
                sprint, squadId,
                issues, jiraKeyToTarea, personas,
                today, inicio, fin,
                totalDias, diasTranscurridos,
                pctTiempo, pctCompletitud
        );
    }

    // ─────────────────────────────────────────────
    // Despacho por tipo de regla
    // ─────────────────────────────────────────────

    private List<JiraAlerta> evaluarRegla(JiraAlertRule regla, SprintAlertContext ctx) {
        try {
            return switch (regla.getTipo()) {
                case ESTADO_INCOHERENTE,
                     DESVIACION_HORAS,
                     ESTIMACION_CERO,
                     TAREA_ESTANCADA,
                     CUSTOM         -> evaluarPorIssue(regla, ctx);
                case IMPUTACION_FALTANTE -> evaluarPorPersona(regla, ctx);
                case SPRINT_EN_RIESGO    -> evaluarSprintNivel(regla, ctx);
            };
        } catch (Exception e) {
            log.warn("Error al evaluar regla '{}': {}", regla.getNombre(), e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────
    // Evaluadores por tipo
    // ─────────────────────────────────────────────

    /** Itera sobre las issues del sprint y dispara la regla si la condición SpEL es true. */
    private List<JiraAlerta> evaluarPorIssue(JiraAlertRule regla, SprintAlertContext ctx) {
        List<JiraAlerta> resultado = new ArrayList<>();
        Expression expr = spelParser.parseExpression(regla.getCondicionSpel());

        for (JiraIssue issue : ctx.issues()) {
            Tarea tarea = ctx.jiraKeyToTarea().get(issue.getJiraKey());
            long diasEnProgreso = calcularDiasEnProgreso(issue, ctx.today());

            StandardEvaluationContext spelCtx = new StandardEvaluationContext();
            spelCtx.setVariable("issue", issue);
            spelCtx.setVariable("tarea", tarea);
            spelCtx.setVariable("regla", regla);
            spelCtx.setVariable("diasEnProgreso", diasEnProgreso);

            Boolean dispara = evaluarExpresion(expr, spelCtx);
            if (Boolean.TRUE.equals(dispara)) {
                String mensaje = resolverMensaje(regla.getMensajeTemplate(), issue, null, diasEnProgreso, 0, 0);
                resultado.add(construirAlerta(regla, ctx.sprint(), ctx.squadId(), mensaje,
                        issue.getJiraKey(), null));
            }
        }
        return resultado;
    }

    /** Itera sobre las personas del squad y dispara si se supera el umbral de días sin imputar. */
    private List<JiraAlerta> evaluarPorPersona(JiraAlertRule regla, SprintAlertContext ctx) {
        List<JiraAlerta> resultado = new ArrayList<>();
        Expression expr = spelParser.parseExpression(regla.getCondicionSpel());

        for (Persona persona : ctx.personas()) {
            long diasSinImputar = calcularDiasSinImputar(persona.getId(), ctx.inicio(), ctx.today());

            StandardEvaluationContext spelCtx = new StandardEvaluationContext();
            spelCtx.setVariable("persona", persona);
            spelCtx.setVariable("diasSinImputar", diasSinImputar);
            spelCtx.setVariable("regla", regla);

            Boolean dispara = evaluarExpresion(expr, spelCtx);
            if (Boolean.TRUE.equals(dispara)) {
                String mensaje = resolverMensajPersona(regla.getMensajeTemplate(), persona, diasSinImputar);
                resultado.add(construirAlerta(regla, ctx.sprint(), ctx.squadId(), mensaje,
                        null, persona));
            }
        }
        return resultado;
    }

    /** Evalúa la regla una sola vez a nivel de sprint (pctTiempo vs pctCompletitud). */
    private List<JiraAlerta> evaluarSprintNivel(JiraAlertRule regla, SprintAlertContext ctx) {
        int pctTiempo = ctx.pctTiempo();
        int pctCompletitud = ctx.pctCompletitud();
        int delta = pctTiempo - pctCompletitud;

        StandardEvaluationContext spelCtx = new StandardEvaluationContext();
        spelCtx.setVariable("pctTiempo", pctTiempo);
        spelCtx.setVariable("pctCompletitud", pctCompletitud);
        spelCtx.setVariable("delta", delta);
        spelCtx.setVariable("regla", regla);

        Expression expr = spelParser.parseExpression(regla.getCondicionSpel());
        Boolean dispara = evaluarExpresion(expr, spelCtx);

        if (Boolean.TRUE.equals(dispara)) {
            String mensaje = regla.getMensajeTemplate()
                    .replace("{pctTiempo}", String.valueOf(pctTiempo))
                    .replace("{pctCompletitud}", String.valueOf(pctCompletitud))
                    .replace("{delta}", String.valueOf(delta));
            return List.of(construirAlerta(regla, ctx.sprint(), ctx.squadId(), mensaje, null, null));
        }
        return List.of();
    }

    // ─────────────────────────────────────────────
    // Utilidades de evaluación
    // ─────────────────────────────────────────────

    private Boolean evaluarExpresion(Expression expr, StandardEvaluationContext ctx) {
        try {
            return expr.getValue(ctx, Boolean.class);
        } catch (Exception e) {
            log.debug("SpEL evalError: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Días laborables en rango sin imputación para la persona.
     * Laborables = lunes a viernes; los sábados y domingos se ignoran.
     */
    private long calcularDiasSinImputar(Long personaId, LocalDate inicio, LocalDate today) {
        if (today.isBefore(inicio)) {
            return 0L;
        }
        LocalDate limite = today.isBefore(inicio) ? inicio : today;

        // Obtener fechas con worklogs en el rango sprint
        var worklogs = worklogRepository.findByPersonaAndFechaRange(personaId, inicio, limite);
        var fechasConImputacion = worklogs.stream()
                .map(w -> w.getFecha())
                .collect(Collectors.toSet());

        // Contar días laborables sin imputación
        long diasSin = 0;
        LocalDate d = inicio;
        while (!d.isAfter(limite)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                if (!fechasConImputacion.contains(d)) {
                    diasSin++;
                }
            }
            d = d.plusDays(1);
        }
        return diasSin;
    }

    /**
     * Días aproximados que el issue lleva en estado "In Progress" en Jira.
     * Se aproxima por el tiempo desde la última actualización de la entidad.
     */
    private long calcularDiasEnProgreso(JiraIssue issue, LocalDate today) {
        if (issue.getUpdatedAt() == null) {
            return 0L;
        }
        LocalDate ultimoUpdate = issue.getUpdatedAt().toLocalDate();
        return Math.max(0, ChronoUnit.DAYS.between(ultimoUpdate, today));
    }

    private String resolverMensaje(String template, JiraIssue issue, Tarea tarea,
                                    long diasEnProgreso, int pctTiempo, int pctCompletitud) {
        String msg = template
                .replace("{jiraKey}", nvl(issue.getJiraKey()))
                .replace("{estadoJira}", nvl(issue.getEstadoJira()))
                .replace("{estadoKaos}", tarea != null ? nvl(tarea.getEstado().name()) : "N/A")
                .replace("{estimacion}", nvl(issue.getEstimacionHoras()))
                .replace("{horasConsumidas}", nvl(issue.getHorasConsumidas()))
                .replace("{diasEnProgreso}", String.valueOf(diasEnProgreso));
        // Porcentaje de desviación
        if (issue.getEstimacionHoras() != null && issue.getHorasConsumidas() != null
                && issue.getEstimacionHoras().compareTo(BigDecimal.ZERO) > 0) {
            int pct = issue.getHorasConsumidas()
                    .multiply(new BigDecimal("100"))
                    .divide(issue.getEstimacionHoras(), 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
            msg = msg.replace("{pct}", String.valueOf(pct));
        } else {
            msg = msg.replace("{pct}", "N/A");
        }
        return msg;
    }

    private String resolverMensajPersona(String template, Persona persona, long diasSinImputar) {
        return template
                .replace("{persona}", nvl(persona.getNombre()))
                .replace("{diasSinImputar}", String.valueOf(diasSinImputar));
    }

    private JiraAlerta construirAlerta(JiraAlertRule regla, Sprint sprint, Long squadId,
                                        String mensaje, String jiraKey, Persona persona) {
        return JiraAlerta.builder()
                .regla(regla)
                .sprint(sprint)
                .squad(sprint.getSquad())
                .severidad(regla.getSeveridad())
                .mensaje(mensaje)
                .jiraKey(jiraKey)
                .persona(persona)
                .resuelta(false)
                .notificadaEmail(false)
                .build();
    }

    private String nvl(Object val) {
        return val != null ? val.toString() : "";
    }

    // ─────────────────────────────────────────────
    // Contexto del sprint (record interno)
    // ─────────────────────────────────────────────

    /**
     * Datos cargados una vez por sprint para evaluar todas las reglas.
     * Se pasa por valor a cada evaluador para evitar re-consultas.
     */
    private record SprintAlertContext(
            Sprint sprint,
            Long squadId,
            List<JiraIssue> issues,
            Map<String, Tarea> jiraKeyToTarea,
            List<Persona> personas,
            LocalDate today,
            LocalDate inicio,
            LocalDate fin,
            long totalDias,
            long diasTranscurridos,
            int pctTiempo,
            int pctCompletitud
    ) {}
}
