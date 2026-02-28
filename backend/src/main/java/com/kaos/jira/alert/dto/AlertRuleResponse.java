package com.kaos.jira.alert.dto;

import java.math.BigDecimal;
import com.kaos.jira.alert.entity.JiraAlertRule.Severidad;
import com.kaos.jira.alert.entity.JiraAlertRule.TipoAlerta;

/**
 * Respuesta resumida de una regla de alerta.
 *
 * @param id              ID de la regla
 * @param squadId         Squad (null = global)
 * @param nombre          Identificador único
 * @param descripcion     Descripción
 * @param tipo            Tipo lógico
 * @param condicionSpel   Expresión SpEL
 * @param mensajeTemplate Plantilla del mensaje
 * @param severidad       Severidad generada
 * @param umbralValor     Umbral configurable
 * @param activa          Si está activa
 */
public record AlertRuleResponse(
        Long id,
        Long squadId,
        String nombre,
        String descripcion,
        TipoAlerta tipo,
        String condicionSpel,
        String mensajeTemplate,
        Severidad severidad,
        BigDecimal umbralValor,
        boolean activa
) {}
