package com.kaos.jira.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.kaos.jira.entity.JiraConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Cliente Jira basado en Selenium / ChromeDriver.
 *
 * <p>Activo en todos los perfiles excepto {@code test} para no requerir
 * Chrome instalado en los pipelines de CI/CD.</p>
 *
 * <p>Navega a la interfaz web de Jira, autentica con usuario+token y extrae
 * issues y worklogs mediante scraping de la API REST embebida que expone
 * Jira a través del navegador ya autenticado.</p>
 *
 * <p><b>Nota:</b> Este cliente es el fallback cuando el acceso directo a la
 * API REST no está disponible (VPN, restricciones de red, etc.).</p>
 */
@Slf4j
@Component
@Profile("!test")
public class JiraSeleniumClient {

    /** Tiempo máximo de espera para que un elemento aparezca en pantalla. */
    private static final Duration ESPERA_ELEMENTO = Duration.ofSeconds(15);

    /** Sufijo del endpoint de login de Jira Server / Data Center. */
    private static final String PATH_LOGIN = "/login";

    /** Sufijo del endpoint de búsqueda REST (accesible via cookie de sesión). */
    private static final String PATH_SEARCH = "/rest/api/2/search";

    /** Sufijo del endpoint de worklogs. */
    private static final String PATH_WORKLOG = "/rest/api/2/issue/%s/worklog";

    /** Selector del campo usuario en el formulario de login de Jira. */
    private static final By SELECTOR_USUARIO = By.id("login-form-username");

    /** Selector del campo contraseña en el formulario de login de Jira. */
    private static final By SELECTOR_PASSWORD = By.id("login-form-password");

    /** Selector del botón "Iniciar sesión" de Jira. */
    private static final By SELECTOR_SUBMIT = By.id("login");

    // ── Ciclo de vida del driver ──────────────────────────────────────────────

    /**
     * Inicializa un ChromeDriver headless con las opciones recomendadas para
     * entornos de servidor.
     *
     * <p>Llama a {@link WebDriverManager} para descargar/actualizar el binario
     * de ChromeDriver automáticamente.</p>
     *
     * @return driver envuelto en {@link RetryWebDriver}
     */
    public RetryWebDriver crearDriver() {
        log.info("┌─────────────────────────────────────────────┐");
        log.info("│  JiraSeleniumClient — inicializando Chrome  │");
        log.info("└─────────────────────────────────────────────┘");

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--disable-extensions",
                "--disable-popup-blocking"
        );

        ChromeDriver driver = new ChromeDriver(options);
        log.info("[JiraSeleniumClient] ChromeDriver iniciado correctamente");
        return new RetryWebDriver(driver);
    }

    /**
     * Cierra y destruye el driver liberando el proceso de Chrome.
     *
     * @param driver driver a cerrar (puede ser null, en ese caso no hace nada)
     */
    public void cerrarDriver(RetryWebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
                log.info("[JiraSeleniumClient] ChromeDriver cerrado");
            } catch (Exception e) {
                log.warn("[JiraSeleniumClient] Error al cerrar ChromeDriver: {}", e.getMessage());
            }
        }
    }

    // ── Autenticación ─────────────────────────────────────────────────────────

    /**
     * Autentica en la interfaz web de Jira usando usuario y token de API.
     *
     * <p>El token de API de Jira Data Center / Server funciona como contraseña
     * en el formulario de login web.</p>
     *
     * @param driver driver ya inicializado
     * @param config configuración con URL, usuario y token del squad
     * @throws RuntimeException si el login falla tras el timeout configurado
     */
    public void autenticar(RetryWebDriver driver, JiraConfig config) {
        String urlLogin = config.getUrl() + PATH_LOGIN;
        log.info("[JiraSeleniumClient] Autenticando en {}", urlLogin);

        driver.get(urlLogin);

        WebDriverWait wait = new WebDriverWait(driver, ESPERA_ELEMENTO);
        wait.until(ExpectedConditions.visibilityOfElementLocated(SELECTOR_USUARIO));

        WebElement campoUsuario = driver.findElement(SELECTOR_USUARIO);
        WebElement campoPassword = driver.findElement(SELECTOR_PASSWORD);

        campoUsuario.clear();
        campoUsuario.sendKeys(config.getUsuario());
        campoPassword.clear();
        campoPassword.sendKeys(config.getToken());

        driver.findElement(SELECTOR_SUBMIT).click();

        // Esperar a que desaparezca el formulario de login (indica sesión iniciada)
        wait.until(ExpectedConditions.invisibilityOfElementLocated(SELECTOR_SUBMIT));
        log.info("[JiraSeleniumClient] Sesión autenticada para squad {}", config.getSquad().getId());
    }

    // ── Extracción de datos ───────────────────────────────────────────────────

    /**
     * Busca issues en Jira usando JQL a través del endpoint REST interno
     * (accesible con la cookie de sesión del browser autenticado).
     *
     * <p>La paginación se maneja automáticamente con PAGE_SIZE=50.</p>
     *
     * @param driver driver ya autenticado
     * @param config configuración del squad
     * @param jql    consulta JQL
     * @return lista de maps con los datos raw de cada issue
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> buscarIssues(RetryWebDriver driver, JiraConfig config, String jql) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        int startAt = 0;
        int total = Integer.MAX_VALUE;
        int pageSize = 50;

        log.info("[JiraSeleniumClient] buscarIssues — squad: {}, jql: {}",
                config.getSquad().getId(), jql);

        while (startAt < total) {
            String url = config.getUrl() + PATH_SEARCH
                    + "?jql=" + encodeUrl(jql)
                    + "&maxResults=" + pageSize
                    + "&startAt=" + startAt
                    + "&fields=summary,status,assignee,priority,issuetype,"
                    + "parent,subtasks,customfield_10016,customfield_10028,timetracking";

            String jsonStr = obtenerJsonDesdeUrl(driver, url);
            if (jsonStr == null || jsonStr.isBlank()) {
                log.warn("[JiraSeleniumClient] Respuesta vacía en startAt={}", startAt);
                break;
            }

            Map<String, Object> body = parsearJson(jsonStr);
            if (body == null) break;

            total = (Integer) body.getOrDefault("total", 0);
            List<Map<String, Object>> issues = (List<Map<String, Object>>) body.getOrDefault("issues", List.of());
            resultado.addAll(issues);
            startAt += issues.size();

            log.debug("[JiraSeleniumClient] buscarIssues págs — obtenidos: {}/{}", resultado.size(), total);

            if (issues.isEmpty()) break;
        }

        log.info("[JiraSeleniumClient] buscarIssues completado — total: {}", resultado.size());
        return resultado;
    }

    /**
     * Obtiene los worklogs de una issue concreta.
     *
     * @param driver   driver ya autenticado
     * @param config   configuración del squad
     * @param issueKey clave de la issue (ej: RED-123)
     * @return lista de maps con datos de cada worklog
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> obtenerWorklogs(RetryWebDriver driver, JiraConfig config, String issueKey) {
        String url = config.getUrl() + String.format(PATH_WORKLOG, issueKey);
        log.debug("[JiraSeleniumClient] obtenerWorklogs — issue: {}", issueKey);

        String jsonStr = obtenerJsonDesdeUrl(driver, url);
        if (jsonStr == null || jsonStr.isBlank()) {
            return List.of();
        }

        Map<String, Object> body = parsearJson(jsonStr);
        if (body == null) return List.of();

        return (List<Map<String, Object>>) body.getOrDefault("worklogs", List.of());
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Navega a la URL dada y extrae el contenido de la página (JSON raw).
     * Jira devuelve JSON directamente cuando el navegador accede a endpoints REST.
     */
    private String obtenerJsonDesdeUrl(RetryWebDriver driver, String url) {
        try {
            driver.get(url);
            // El cuerpo de la respuesta REST viene en el elemento <body> o <pre>
            WebElement body;
            try {
                body = driver.findElement(By.tagName("pre"));
            } catch (Exception e) {
                body = driver.findElement(By.tagName("body"));
            }
            return body != null ? body.getText() : null;
        } catch (Exception e) {
            log.error("[JiraSeleniumClient] Error obteniendo JSON de {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Parsea un string JSON a Map usando Jackson.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parsearJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("[JiraSeleniumClient] Error parseando JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Codifica una cadena para usarla como parámetro de URL.
     */
    private String encodeUrl(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
