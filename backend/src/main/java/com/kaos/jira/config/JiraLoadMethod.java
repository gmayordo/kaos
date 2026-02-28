package com.kaos.jira.config;

/**
 * Metodología de carga de datos desde Jira.
 * Permite configurar cómo se sincronizan los datos sin recompilar.
 *
 * <ul>
 *   <li>{@link #API_REST} — Usa la API REST oficial de Jira (sujeta a rate limit 200 calls/2h)</li>
 *   <li>{@link #SELENIUM} — Usa ChromeDriver headless para scraping (sin límite de llamadas)</li>
 *   <li>{@link #LOCAL}    — Lee solo desde caché local en BD (sin llamadas externas)</li>
 * </ul>
 */
public enum JiraLoadMethod {

    API_REST("API REST v2 de Jira (limitada a 200 llamadas cada 2 horas)"),
    SELENIUM("Selenium/ChromeDriver headless (sin límite de llamadas, requiere Chrome)"),
    LOCAL("Caché local en base de datos (sin llamadas externas, datos pueden no estar actualizados)");

    private final String description;

    JiraLoadMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parsea el valor de string al enum correspondiente.
     * Retorna {@link #API_REST} como fallback si el valor es inválido o nulo.
     *
     * @param value valor string a parsear (case-insensitive)
     * @return método de carga correspondiente, o API_REST si no se reconoce
     */
    public static JiraLoadMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            return API_REST;
        }
        try {
            return JiraLoadMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return API_REST;
        }
    }
}
