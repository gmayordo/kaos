package com.kaos.jira.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.entity.JiraSyncQueue;
import com.kaos.jira.entity.JiraSyncQueue.EstadoOperacion;
import com.kaos.jira.entity.SyncMode;
import com.kaos.jira.repository.JiraConfigRepository;
import com.kaos.jira.repository.JiraSyncQueueRepository;
import com.kaos.jira.service.JiraRateLimiter;
import com.kaos.jira.service.JiraSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler batch que procesa la cola de operaciones Jira pendientes.
 *
 * <p>Se activa únicamente si {@code jira.batch.enabled=true} en
 * {@code application.yml}. Por defecto está desactivado para no consumir
 * cuota en entornos de desarrollo.</p>
 *
 * <p>Flujo de cada ejecución (DT-33):
 * <ol>
 *   <li>Comprueba cuota disponible — si &lt; {@value #UMBRAL_CUOTA_MINIMA} calls, salta.</li>
 *   <li>Obtiene operaciones PENDIENTE de la cola (cualquier squad), FIFO.</li>
 *   <li>Ejecuta cada operación llamando a {@link JiraSyncService#procesarOperacionCola}.</li>
 *   <li>Si una operación falla, se registra el error y se continúa con la siguiente.</li>
 *   <li>Si la cuota se agota durante el procesamiento, se detiene hasta la siguiente ejecución.</li>
 * </ol>
 * </p>
 */
@Component
@ConditionalOnProperty(name = "jira.batch.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class JiraBatchScheduler {

    private static final int UMBRAL_CUOTA_MINIMA = 10;

    private final JiraSyncService jiraSyncService;
    private final JiraSyncQueueRepository syncQueueRepository;
    private final JiraConfigRepository jiraConfigRepository;
    private final JiraRateLimiter rateLimiter;

    // ── Batch principal — cada 30 minutos ────────────────────────────────────

    /**
     * Procesa las operaciones pendientes de la cola Jira.
     *
     * <p>Ejecutado automáticamente cada 30 minutos (1800000 ms).
     * El delay empieza a contar desde que termina la ejecución anterior,
     * no desde un cron fijo, para evitar solapamiento.</p>
     */
    @Scheduled(fixedDelayString = "${jira.batch.fixed-delay-ms:1800000}")
    public void procesarCola() {
        log.info("[JiraBatchScheduler] Iniciando procesamiento de cola Jira");

        // ── Verificar cuota global ────────────────────────────────────────────
        long consumidas = rateLimiter.llamadasConsumidas();
        long disponibles = 200 - consumidas;

        if (disponibles < UMBRAL_CUOTA_MINIMA) {
            log.info("[JiraBatchScheduler] Cuota insuficiente ({}/200 consumidas, {} disponibles). Saltando ejecución.",
                    consumidas, disponibles);
            return;
        }

        // ── Obtener operaciones listas para ejecutar ──────────────────────────
        List<JiraSyncQueue> pendientes = syncQueueRepository.findAllPendientes(
                EstadoOperacion.PENDIENTE,
                LocalDateTime.now()
        );

        if (pendientes.isEmpty()) {
            log.debug("[JiraBatchScheduler] Cola vacía. Nada que procesar.");
            return;
        }

        log.info("[JiraBatchScheduler] {} operaciones pendientes en cola. Cuota disponible: {}/200",
                pendientes.size(), disponibles);

        int procesadas = 0;
        int errores = 0;

        for (JiraSyncQueue operacion : pendientes) {
            // Verificar cuota antes de cada operación
            if (!rateLimiter.canMakeCall()) {
                log.warn("[JiraBatchScheduler] Cuota agotada tras procesar {} operaciones. Deteniendo batch.", procesadas);
                break;
            }

            try {
                jiraSyncService.procesarOperacionCola(operacion);
                procesadas++;
            } catch (Exception e) {
                log.error("[JiraBatchScheduler] Error en operación id={} tipo={}: {}",
                        operacion.getId(), operacion.getTipoOperacion(), e.getMessage(), e);
                errores++;
                // Continuar con la siguiente operación — un error no detiene el batch
            }
        }

        log.info("[JiraBatchScheduler] Batch finalizado — procesadas: {}, errores: {}, cuota restante: {}/200",
                procesadas, errores, rateLimiter.llamadasRestantes());
    }

    // ── Sync FULL diaria — cada madrugada a las 04:00 ──────────────────────

    /**
     * Ejecuta una sincronización FULL para todos los squads con configuración activa.
     * Se ejecuta a las 04:00 AM cuando la carga es mínima y la cuota está fresca.
     */
    @Scheduled(cron = "${jira.batch.full-cron:0 0 4 * * *}")
    public void syncFullDiaria() {
        log.info("[JiraBatchScheduler] Iniciando sync FULL diaria para todos los squads");

        long consumidas = rateLimiter.llamadasConsumidas();
        if (consumidas >= 200 - UMBRAL_CUOTA_MINIMA) {
            log.warn("[JiraBatchScheduler] Cuota insuficiente ({}/200) para sync FULL. Saltando.", consumidas);
            return;
        }

        var configs = jiraConfigRepository.findAllByActivaTrue();
        int procesados = 0;

        for (var config : configs) {
            if (!rateLimiter.canMakeCall()) {
                log.warn("[JiraBatchScheduler] Cuota agotada tras {} squads FULL. Deteniendo.", procesados);
                break;
            }
            try {
                jiraSyncService.syncCompleta(config.getSquad().getId(), SyncMode.FULL);
                procesados++;
            } catch (Exception e) {
                log.error("[JiraBatchScheduler] Error sync FULL squadId={}: {}", config.getSquad().getId(), e.getMessage());
            }
        }

        log.info("[JiraBatchScheduler] Sync FULL diaria finalizada — squads procesados: {}/{}", procesados, configs.size());
    }

    // ── Sync INCREMENTAL — cada 4 horas en horario laboral ───────────────────

    /**
     * Ejecuta una sincronización INCREMENTAL para todos los squads.
     * Solo trae issues actualizadas desde la última sync exitosa.
     * Consume muy pocas llamadas API (típicamente 1-3 por squad).
     */
    @Scheduled(cron = "${jira.batch.incremental-cron:0 0 8,12,16,20 * * MON-FRI}")
    public void syncIncrementalPeriodica() {
        log.info("[JiraBatchScheduler] Iniciando sync INCREMENTAL periódica");

        long consumidas = rateLimiter.llamadasConsumidas();
        if (consumidas >= 200 - UMBRAL_CUOTA_MINIMA) {
            log.warn("[JiraBatchScheduler] Cuota insuficiente ({}/200) para sync INCREMENTAL. Saltando.", consumidas);
            return;
        }

        var configs = jiraConfigRepository.findAllByActivaTrue();
        int procesados = 0;

        for (var config : configs) {
            if (!rateLimiter.canMakeCall()) {
                log.warn("[JiraBatchScheduler] Cuota agotada tras {} squads INCREMENTAL. Deteniendo.", procesados);
                break;
            }
            try {
                jiraSyncService.syncCompleta(config.getSquad().getId(), SyncMode.INCREMENTAL);
                procesados++;
            } catch (Exception e) {
                log.error("[JiraBatchScheduler] Error sync INCREMENTAL squadId={}: {}", config.getSquad().getId(), e.getMessage());
            }
        }

        log.info("[JiraBatchScheduler] Sync INCREMENTAL finalizada — squads procesados: {}/{}", procesados, configs.size());
    }

    // ── Purga de logs antiguos — cada 24 horas ───────────────────────────────

    /**
     * Limpia operaciones completadas con más de 7 días de antigüedad.
     *
     * <p>Evita que la tabla {@code jira_sync_queue} crezca indefinidamente.
     * Se ejecuta una vez al día a las 3:00 AM.</p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limpiarColaAntigua() {
        LocalDateTime hace7Dias = LocalDateTime.now().minusDays(7);
        int eliminadas = syncQueueRepository.limpiarCompletadas(hace7Dias, EstadoOperacion.COMPLETADA);
        if (eliminadas > 0) {
            log.info("[JiraBatchScheduler] Purga de cola: {} operaciones completadas eliminadas (> 7 días)", eliminadas);
        }
    }
}
