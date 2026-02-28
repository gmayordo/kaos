package com.kaos.jira.client;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;

/**
 * Wrapper de WebDriver que reintenta automáticamente operaciones
 * de búsqueda de elementos cuando fallan (ej. StaleElementReferenceException).
 *
 * <p>Patrón adaptado del proyecto gestion para uso en KAOS integración Jira.
 * Configurado con 150 reintentos y 200ms de espera entre intentos (30s total máx).</p>
 *
 * <p><b>Uso:</b></p>
 * <pre>
 *   WebDriver driver = new RetryWebDriver(new ChromeDriver(options));
 * </pre>
 */
public class RetryWebDriver implements WebDriver, JavascriptExecutor, TakesScreenshot, Interactive {

    private final WebDriver delegate;
    private final long retryDelayMs = 200L;
    private final int maxRetries = 150;

    public RetryWebDriver(WebDriver delegate) {
        this.delegate = delegate;
    }

    // ── Métodos con retry ─────────────────────────────────────────────────────

    @Override
    public void get(String url) {
        retry(() -> {
            delegate.get(url);
            return null;
        });
    }

    @Override
    public WebElement findElement(By by) {
        return retry(() -> delegate.findElement(by));
    }

    @Override
    public List<WebElement> findElements(By by) {
        return retry(() -> delegate.findElements(by));
    }

    // ── Métodos delegados directamente ────────────────────────────────────────

    @Override
    public String getCurrentUrl() {
        return delegate.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public String getPageSource() {
        return delegate.getPageSource();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void quit() {
        delegate.quit();
    }

    @Override
    public Set<String> getWindowHandles() {
        return delegate.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return delegate.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return delegate.switchTo();
    }

    @Override
    public Navigation navigate() {
        return delegate.navigate();
    }

    @Override
    public Options manage() {
        return delegate.manage();
    }

    // ── JavascriptExecutor delegado ───────────────────────────────────────────

    @Override
    public Object executeScript(String script, Object... args) {
        if (delegate instanceof JavascriptExecutor js) {
            return js.executeScript(script, args);
        }
        throw new UnsupportedOperationException("El driver no soporta JavascriptExecutor");
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        if (delegate instanceof JavascriptExecutor js) {
            return js.executeAsyncScript(script, args);
        }
        throw new UnsupportedOperationException("El driver no soporta JavascriptExecutor async");
    }

    // ── TakesScreenshot delegado ──────────────────────────────────────────────

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        if (delegate instanceof TakesScreenshot ts) {
            return ts.getScreenshotAs(target);
        }
        throw new UnsupportedOperationException("El driver no soporta screenshots");
    }

    // ── Interactive delegado ──────────────────────────────────────────────────

    @Override
    public void perform(Collection<Sequence> actions) {
        if (delegate instanceof Interactive interactive) {
            interactive.perform(actions);
        }
    }

    @Override
    public void resetInputState() {
        if (delegate instanceof Interactive interactive) {
            interactive.resetInputState();
        }
    }

    // ── Lógica de retry ───────────────────────────────────────────────────────

    /**
     * Ejecuta la acción con reintentos automáticos.
     * Reintenta ante cualquier excepción hasta alcanzar maxRetries.
     *
     * @param action acción a ejecutar
     * @param <T>    tipo de retorno
     * @return resultado de la acción
     * @throws RuntimeException si se agotan todos los reintentos
     */
    private <T> T retry(Retryable<T> action) {
        int attempts = 0;
        Exception lastException = null;
        while (attempts < maxRetries) {
            try {
                return action.run();
            } catch (Exception e) {
                lastException = e;
                attempts++;
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrumpido", ie);
                }
            }
        }
        throw new RuntimeException(
                "RetryWebDriver: operación fallida tras " + maxRetries + " intentos", lastException);
    }

    @FunctionalInterface
    private interface Retryable<T> {
        T run() throws Exception;
    }
}
