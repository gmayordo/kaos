package com.kaos.jira.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del método de carga de Jira.
 * Permite cambiar el método en caliente sin reiniciar la aplicación.
 *
 * <p>El método inicial se toma de la propiedad {@code jira.load.method} (por defecto API_REST).
 * Puede cambiarse en tiempo de ejecución via {@link #setCurrentMethod(JiraLoadMethod)},
 * por ejemplo desde el endpoint PATCH /api/v1/jira/config/method.
 */
@Configuration
public class JiraLoadConfig {

    private static final Logger log = LoggerFactory.getLogger(JiraLoadConfig.class);

    private JiraLoadMethod currentMethod;

    public JiraLoadConfig(
            @Value("${jira.load.method:API_REST}") String loadMethodValue) {
        this.currentMethod = JiraLoadMethod.fromString(loadMethodValue);
        log.info("[JiraLoadConfig] Método de carga inicial: {} — {}", currentMethod, currentMethod.getDescription());
    }

    /**
     * Retorna el método de carga activo.
     */
    public JiraLoadMethod getCurrentMethod() {
        return currentMethod;
    }

    /**
     * Cambia el método de carga en caliente.
     * El cambio es inmediato y afecta a todas las sincronizaciones siguientes.
     *
     * @param newMethod nuevo método de carga a aplicar
     */
    public void setCurrentMethod(JiraLoadMethod newMethod) {
        JiraLoadMethod previous = this.currentMethod;
        this.currentMethod = newMethod;
        log.info("[JiraLoadConfig] Método de carga cambiado: {} → {} — {}",
                previous, newMethod, newMethod.getDescription());
    }
}
