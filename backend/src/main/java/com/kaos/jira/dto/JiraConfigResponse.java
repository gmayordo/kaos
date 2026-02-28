package com.kaos.jira.dto;

/**
 * Respuesta con la configuración activa de Jira para un squad.
 *
 * <p>El token no se incluye en la respuesta (seguridad).
 * Se muestra como {@code "****"} para indicar que existe.</p>
 *
 * @param squadId       ID del squad propietario de la configuración
 * @param squadNombre   Nombre del squad
 * @param url           URL base del servidor Jira
 * @param usuario       Nombre de usuario de Jira
 * @param tokenOculto   Indicador de que el token existe ("****" o "no configurado")
 * @param loadMethod    Método de carga activo (API_REST / SELENIUM / LOCAL)
 * @param activa        Si la configuración está activa
 * @param mapeoEstados  JSON con el mapeo de estados Jira → estados KAOS (puede ser null)
 */
public record JiraConfigResponse(
        Long squadId,
        String squadNombre,
        String url,
        String usuario,
        String tokenOculto,
        String loadMethod,
        boolean activa,
        String mapeoEstados
) {}
