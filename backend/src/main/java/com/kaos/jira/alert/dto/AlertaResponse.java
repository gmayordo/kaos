package com.kaos.jira.alert.dto;

import java.time.LocalDateTime;
import com.kaos.jira.alert.entity.JiraAlertRule.Severidad;

/**
 * Respuesta de una alerta generada por el motor SpEL.
 *
 * @param id              ID de la alerta
 * @param sprintId        ID del sprint
 * @param squadId         ID del squad
 * @param reglaId         ID de la regla que la generó
 * @param reglaNombre     Nombre legible de la regla
 * @param severidad       CRITICO / AVISO / INFO
 * @param mensaje         Mensaje resuelto con los valores reales
 * @param jiraKey         Issue afectada (nullable)
 * @param personaNombre   Persona afectada (nullable)
 * @param resuelta        Si el Lead Tech la ha marcado como atendida
 * @param notificadaEmail Si ya fue incluida en el resumen email
 * @param createdAt       Cuándo se generó
 */
public record AlertaResponse(
        Long id,
        Long sprintId,
        Long squadId,
        Long reglaId,
        String reglaNombre,
        Severidad severidad,
        String mensaje,
        String jiraKey,
        String personaNombre,
        boolean resuelta,
        boolean notificadaEmail,
        LocalDateTime createdAt
) {}
