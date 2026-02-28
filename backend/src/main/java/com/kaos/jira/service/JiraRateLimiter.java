package com.kaos.jira.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.entity.JiraApiCallLog;
import com.kaos.jira.repository.JiraApiCallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de control de rate limiting para la API REST de Jira.
 *
 * <p>Jira Server/DC limita a 200 llamadas por ventana de 2 horas.
 * Este servicio mantiene el umbral en 195 (buffer de 5) para evitar alcanzar
 * el límite exacto y permitir llamadas de emergencia.
 *
 * <p>Uso típico:
 * <pre>
 *   if (rateLimiter.canMakeCall()) {
 *       rateLimiter.registrarLlamada("/rest/api/2/search", "GET", 200, squadId);
 *       // hacer la llamada
 *   } else {
 *       // encolar o cambiar a Selenium
 *   }
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JiraRateLimiter {

    /** Umbral de llamadas antes de considerar la cuota agotada (buffer de 5 sobre el límite real de 200). */
    private static final long UMBRAL_LLAMADAS = 195L;

    /** Tamaño de la ventana temporal en horas. */
    private static final int VENTANA_HORAS = 2;

    private final JiraApiCallLogRepository callLogRepository;

    /**
     * Indica si queda cuota disponible para hacer una nueva llamada a la API Jira.
     * Consulta el número de llamadas en la ventana de las últimas 2 horas.
     *
     * @return {@code true} si el contador está por debajo del umbral (195)
     */
    public boolean canMakeCall() {
        long llamadasRecientes = callLogRepository.countCallsSince(ventanaDesde());
        boolean disponible = llamadasRecientes < UMBRAL_LLAMADAS;
        log.debug("[JiraRateLimiter] Llamadas en última ventana de {}h: {}/{} — cuota {}",
                VENTANA_HORAS, llamadasRecientes, UMBRAL_LLAMADAS,
                disponible ? "DISPONIBLE" : "AGOTADA");
        return disponible;
    }

    /**
     * Devuelve el número de llamadas consumidas en la ventana actual.
     *
     * @return llamadas realizadas en las últimas 2 horas
     */
    public long llamadasConsumidas() {
        return callLogRepository.countCallsSince(ventanaDesde());
    }

    /**
     * Devuelve las llamadas restantes antes de alcanzar el umbral.
     *
     * @return llamadas disponibles (mínimo 0)
     */
    public long llamadasRestantes() {
        return Math.max(0L, UMBRAL_LLAMADAS - llamadasConsumidas());
    }

    /**
     * Registra una llamada realizada a la API Jira.
     * Debe invocarse DESPUÉS de cada llamada exitosa o fallida.
     *
     * @param endpoint   endpoint llamado (ej: {@code /rest/api/2/search})
     * @param metodo     método HTTP (GET, POST, etc.)
     * @param statusCode código de respuesta HTTP
     * @param squadId    squad que originó la llamada (puede ser null)
     */
    @Transactional
    public void registrarLlamada(String endpoint, String metodo, Integer statusCode, Long squadId) {
        JiraApiCallLog log = JiraApiCallLog.builder()
                .endpoint(endpoint)
                .metodo(metodo)
                .statusCode(statusCode)
                .squadId(squadId)
                .executedAt(LocalDateTime.now())
                .build();
        callLogRepository.save(log);
        JiraRateLimiter.log.debug("[JiraRateLimiter] Llamada registrada: {} {} → {}",
                metodo, endpoint, statusCode);
    }

    /**
     * Limpia registros de log anteriores a 24 horas (mantenimiento).
     * Puede llamarse desde un @Scheduled semanal.
     */
    @Transactional
    public void purgarLogsAntiguos() {
        LocalDateTime hace24h = LocalDateTime.now().minusHours(24);
        callLogRepository.deleteByExecutedAtBefore(hace24h);
        log.info("[JiraRateLimiter] Registros de llamadas API anteriores a 24h eliminados.");
    }

    // ── privado ──────────────────────────────────────────────────────────────

    private LocalDateTime ventanaDesde() {
        return LocalDateTime.now().minusHours(VENTANA_HORAS);
    }
}
