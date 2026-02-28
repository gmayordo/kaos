package com.kaos.jira.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Request para planificar uno o varios issues Jira como Tareas KAOS.
 */
public record PlanificarIssueRequest(
        /** Sprint KAOS destino */
        @NotNull Long sprintId,
        /** Lista de issues a planificar con su configuración de tarea */
        @NotEmpty @Valid List<PlanificarAsignacionItem> asignaciones
) {
}
