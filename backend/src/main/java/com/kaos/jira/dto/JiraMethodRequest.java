package com.kaos.jira.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Petición para cambiar el método de carga de Jira en caliente.
 *
 * <p>Valores válidos: {@code API_REST}, {@code SELENIUM}, {@code LOCAL}.</p>
 *
 * @param method nombre del nuevo método (insensible a mayúsculas)
 */
public record JiraMethodRequest(
        @NotBlank(message = "El método no puede estar vacío")
        String method
) {}
