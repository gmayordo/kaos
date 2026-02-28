package com.kaos.jira.alert.dto;

import java.math.BigDecimal;
import com.kaos.jira.alert.entity.JiraAlertRule.Severidad;
import com.kaos.jira.alert.entity.JiraAlertRule.TipoAlerta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request para crear o modificar una regla de alerta.
 *
 * @param squadId         Squad al que aplica (null = global)
 * @param nombre          Identificador corto único
 * @param descripcion     Descripción detallada
 * @param tipo            Tipo lógico de la regla
 * @param condicionSpel   Expresión SpEL
 * @param mensajeTemplate Plantilla con placeholders
 * @param severidad       Severidad de la alerta generada
 * @param umbralValor     Umbral numérico configurable (nullable)
 * @param activa          Si la regla está activa
 */
public record AlertRuleRequest(
        Long squadId,
        @NotBlank String nombre,
        String descripcion,
        @NotNull TipoAlerta tipo,
        @NotBlank String condicionSpel,
        @NotBlank String mensajeTemplate,
        @NotNull Severidad severidad,
        BigDecimal umbralValor,
        boolean activa
) {}
