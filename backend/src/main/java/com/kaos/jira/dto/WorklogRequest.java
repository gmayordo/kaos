package com.kaos.jira.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request para registrar o editar una imputación de horas en KAOS.
 *
 * <p>La imputación se crea con origen {@code KAOS} y {@code sincronizado=false}
 * hasta que el batch la envíe a Jira.</p>
 */
public record WorklogRequest(

        @NotBlank(message = "La clave Jira del issue es obligatoria")
        String jiraKey,

        @NotNull(message = "La persona es obligatoria")
        Long personaId,

        @NotNull(message = "La fecha es obligatoria")
        String fecha, // formato yyyy-MM-dd

        @NotNull(message = "Las horas son obligatorias")
        @DecimalMin(value = "0.25", message = "El mínimo imputable es 0.25 horas (15 min)")
        BigDecimal horas,

        String comentario
) {}
