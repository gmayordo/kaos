package com.kaos.jira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.kaos.jira.entity.WorklogOrigen;

/**
 * DTO de respuesta para un worklog (imputación de horas).
 */
public record JiraWorklogResponse(

        Long id,

        /** Clave Jira del issue al que pertenece (ej: PROJ-123). */
        String jiraKey,
        String issueSummary,

        Long personaId,
        String personaNombre,

        LocalDate fecha,
        BigDecimal horas,
        String comentario,

        /** JIRA: importado de Jira — KAOS: registrado en KAOS (pendiente de enviar). */
        WorklogOrigen origen,

        /** Solo relevante para origen=KAOS. TRUE una vez enviado a Jira. */
        boolean sincronizado
) {}
