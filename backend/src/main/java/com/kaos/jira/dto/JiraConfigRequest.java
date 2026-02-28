package com.kaos.jira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request para crear o actualizar la configuración Jira de un squad.
 *
 * <p>Si {@code token} se envía vacío o null al actualizar, el token existente
 * no se modifica (permite editar otros campos sin re-introducir el token).</p>
 *
 * <p>Ejemplo de cuerpo:
 * <pre>{@code
 * {
 *   "url": "https://jira.empresa.com",
 *   "usuario": "usuario@empresa.com",
 *   "token": "ATATxxxxxxxxxxxxxxxx",
 *   "boardCorrectivoId": 42,
 *   "boardEvolutivoId": 43,
 *   "loadMethod": "API_REST",
 *   "activa": true,
 *   "mapeoEstados": "{\"Done\":\"COMPLETADA\",\"In Progress\":\"EN_PROGRESO\",\"To Do\":\"PENDIENTE\"}"
 * }
 * }</pre>
 *
 * @param url               URL base del servidor Jira (ej: https://jira.empresa.com)
 * @param usuario           Correo o usuario Jira con acceso API
 * @param token             Token API de Jira (vacío = mantener el existente)
 * @param boardCorrectivoId ID del board de correctivos en Jira (opcional)
 * @param boardEvolutivoId  ID del board de evolutivos en Jira (opcional)
 * @param loadMethod        Método de carga: API_REST, SELENIUM o LOCAL
 * @param activa            Si la configuración debe activarse
 * @param mapeoEstados      JSON con mapeo estado Jira → estado KAOS (opcional)
 */
public record JiraConfigRequest(
        @NotBlank(message = "La URL de Jira es obligatoria")
        String url,

        @NotBlank(message = "El usuario de Jira es obligatorio")
        String usuario,

        /** Si es null o vacío al hacer PUT, el token existente no se modifica */
        String token,

        Long boardCorrectivoId,

        Long boardEvolutivoId,

        @NotBlank(message = "El método de carga es obligatorio")
        @Pattern(regexp = "API_REST|SELENIUM|LOCAL", message = "loadMethod debe ser API_REST, SELENIUM o LOCAL")
        String loadMethod,

        boolean activa,

        String mapeoEstados
) {}
