package com.kaos.jira.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response con los datos de un issue Jira para la pantalla de planificación.
 * Incluye estado de vinculación con KAOS y subtareas anidadas.
 */
public record JiraIssueResponse(
        Long id,
        String jiraKey,
        String summary,
        String tipoJira,
        String estadoJira,
        String estadoKaos,
        String asignadoJira,
        Long personaId,
        String personaNombre,
        BigDecimal estimacionHoras,
        BigDecimal horasConsumidas,
        String parentKey,
        /** true si ya tiene una Tarea KAOS asociada en el sprint dado */
        boolean tieneTarea,
        /** ID de la Tarea KAOS vinculada (null si no existe) */
        Long tareaId,
        /** Subtareas hijo (solo relleno para issues raíz) */
        List<JiraIssueResponse> subtareas
) {
}
