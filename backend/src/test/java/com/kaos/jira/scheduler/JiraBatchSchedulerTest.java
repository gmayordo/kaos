package com.kaos.jira.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.kaos.jira.entity.JiraSyncQueue;
import com.kaos.jira.entity.JiraSyncQueue.EstadoOperacion;
import com.kaos.jira.entity.JiraSyncQueue.TipoOperacion;
import com.kaos.jira.repository.JiraConfigRepository;
import com.kaos.jira.repository.JiraSyncQueueRepository;
import com.kaos.jira.service.JiraRateLimiter;
import com.kaos.jira.service.JiraSyncService;
import com.kaos.squad.entity.Squad;

/**
 * Unit tests para JiraBatchScheduler (TASK-124 / DT-33).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JiraBatchScheduler")
class JiraBatchSchedulerTest {

    @Mock JiraSyncService jiraSyncService;
    @Mock JiraSyncQueueRepository syncQueueRepository;
    @Mock JiraConfigRepository jiraConfigRepository;
    @Mock JiraRateLimiter rateLimiter;

    @InjectMocks
    JiraBatchScheduler scheduler;

    @Nested
    @DisplayName("procesarCola()")
    class ProcesarCola {

        @Test
        @DisplayName("debería saltarse el procesamiento cuando cuota disponible < 10 (DT-33)")
        void shouldSkip_whenQuotaInsufficient() {
            when(rateLimiter.llamadasConsumidas()).thenReturn(195L); // 200-195 = 5 < 10

            scheduler.procesarCola();

            verify(syncQueueRepository, never()).findAllPendientes(any(), any());
            verify(jiraSyncService, never()).procesarOperacionCola(any());
        }

        @Test
        @DisplayName("debería retornar sin procesar cuando la cola está vacía")
        void shouldReturn_whenQueueEmpty() {
            when(rateLimiter.llamadasConsumidas()).thenReturn(50L);
            when(syncQueueRepository.findAllPendientes(eq(EstadoOperacion.PENDIENTE), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            scheduler.procesarCola();

            verify(jiraSyncService, never()).procesarOperacionCola(any());
        }

        @Test
        @DisplayName("debería procesar cada operación de la cola cuando hay cuota (DT-33)")
        void shouldProcessAllOperations_whenQueueHasItems() {
            when(rateLimiter.llamadasConsumidas()).thenReturn(50L);
            when(rateLimiter.canMakeCall()).thenReturn(true);

            JiraSyncQueue op1 = buildOperacion(1L, TipoOperacion.SYNC_ISSUES);
            JiraSyncQueue op2 = buildOperacion(2L, TipoOperacion.SYNC_WORKLOGS);

            when(syncQueueRepository.findAllPendientes(eq(EstadoOperacion.PENDIENTE), any()))
                    .thenReturn(List.of(op1, op2));

            scheduler.procesarCola();

            verify(jiraSyncService, times(2)).procesarOperacionCola(any());
        }

        @Test
        @DisplayName("debería detener el procesamiento cuando la cuota se agota a mitad de cola")
        void shouldStop_whenQuotaExhaustedMidway() {
            when(rateLimiter.llamadasConsumidas()).thenReturn(50L);
            // Primera comprobación: puede hacer llamada; segunda: ya no
            when(rateLimiter.canMakeCall()).thenReturn(true, false);

            JiraSyncQueue op1 = buildOperacion(1L, TipoOperacion.SYNC_ISSUES);
            JiraSyncQueue op2 = buildOperacion(2L, TipoOperacion.SYNC_WORKLOGS);

            when(syncQueueRepository.findAllPendientes(eq(EstadoOperacion.PENDIENTE), any()))
                    .thenReturn(List.of(op1, op2));

            scheduler.procesarCola();

            // Solo procesa la primera; la segunda la corta el cuota check
            verify(jiraSyncService, times(1)).procesarOperacionCola(any());
        }

        @Test
        @DisplayName("debería continuar con la siguiente operación cuando una falla (DT-33)")
        void shouldContinue_onOperationError() {
            when(rateLimiter.llamadasConsumidas()).thenReturn(50L);
            when(rateLimiter.canMakeCall()).thenReturn(true);

            JiraSyncQueue op1 = buildOperacion(1L, TipoOperacion.SYNC_ISSUES);
            JiraSyncQueue op2 = buildOperacion(2L, TipoOperacion.SYNC_WORKLOGS);

            when(syncQueueRepository.findAllPendientes(eq(EstadoOperacion.PENDIENTE), any()))
                    .thenReturn(List.of(op1, op2));
            // Primera operación falla
            org.mockito.Mockito.doThrow(new RuntimeException("Sync failed"))
                    .when(jiraSyncService).procesarOperacionCola(op1);

            // No debe lanzar excepción; procesa ambas (la segunda OK)
            scheduler.procesarCola();

            verify(jiraSyncService, times(2)).procesarOperacionCola(any());
        }
    }

    @Nested
    @DisplayName("limpiarColaAntigua()")
    class LimpiarColaAntigua {

        @Test
        @DisplayName("debería eliminar operaciones completadas con más de 7 días")
        void shouldDeleteOldCompletedOperations() {
            when(syncQueueRepository.limpiarCompletadas(any(LocalDateTime.class), eq(EstadoOperacion.COMPLETADA)))
                    .thenReturn(5);

            scheduler.limpiarColaAntigua();

            verify(syncQueueRepository).limpiarCompletadas(any(LocalDateTime.class), eq(EstadoOperacion.COMPLETADA));
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private JiraSyncQueue buildOperacion(Long id, TipoOperacion tipo) {
        Squad squad = new Squad();
        squad.setId(1L);

        JiraSyncQueue op = new JiraSyncQueue();
        op.setId(id);
        op.setSquad(squad);
        op.setTipoOperacion(tipo);
        op.setEstado(EstadoOperacion.PENDIENTE);
        return op;
    }
}
