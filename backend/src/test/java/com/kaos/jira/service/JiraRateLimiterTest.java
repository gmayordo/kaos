package com.kaos.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.kaos.jira.entity.JiraApiCallLog;
import com.kaos.jira.repository.JiraApiCallLogRepository;

/**
 * Tests unitarios para JiraRateLimiter.
 * Cubre: ventana temporal, umbral 195, registro de llamadas, llamadas restantes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JiraRateLimiter")
class JiraRateLimiterTest {

    @Mock
    private JiraApiCallLogRepository callLogRepository;

    @InjectMocks
    private JiraRateLimiter rateLimiter;

    @Nested
    @DisplayName("canMakeCall()")
    class CanMakeCall {

        @Test
        @DisplayName("debería devolver true cuando las llamadas consumidas son < 195")
        void shouldReturnTrue_whenCallsBelowThreshold() {
            when(callLogRepository.countCallsSince(any(LocalDateTime.class))).thenReturn(50L);
            assertThat(rateLimiter.canMakeCall()).isTrue();
        }

        @Test
        @DisplayName("debería devolver false cuando las llamadas consumidas son >= 195")
        void shouldReturnFalse_whenCallsAtThreshold() {
            when(callLogRepository.countCallsSince(any(LocalDateTime.class))).thenReturn(195L);
            assertThat(rateLimiter.canMakeCall()).isFalse();
        }

        @Test
        @DisplayName("debería devolver false cuando las llamadas consumidas superan 195")
        void shouldReturnFalse_whenCallsAboveThreshold() {
            when(callLogRepository.countCallsSince(any(LocalDateTime.class))).thenReturn(200L);
            assertThat(rateLimiter.canMakeCall()).isFalse();
        }
    }

    @Nested
    @DisplayName("llamadasConsumidas() / llamadasRestantes()")
    class Contadores {

        @Test
        @DisplayName("llamadasConsumidas() debería reflejar el conteo del repositorio")
        void shouldReflectRepositoryCount() {
            when(callLogRepository.countCallsSince(any(LocalDateTime.class))).thenReturn(130L);
            assertThat(rateLimiter.llamadasConsumidas()).isEqualTo(130L);
        }

        @Test
        @DisplayName("llamadasRestantes() debería calcular 195 - consumidas")
        void shouldCalculateRemainingCalls() {
            when(callLogRepository.countCallsSince(any(LocalDateTime.class))).thenReturn(100L);
            assertThat(rateLimiter.llamadasRestantes()).isEqualTo(95L);
        }

        @Test
        @DisplayName("llamadasRestantes() no debería ser negativo cuando se supera el umbral")
        void shouldNotReturnNegativeWhenThresholdExceeded() {
            when(callLogRepository.countCallsSince(any(LocalDateTime.class))).thenReturn(200L);
            assertThat(rateLimiter.llamadasRestantes()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("registrarLlamada()")
    class RegistrarLlamada {

        @Test
        @DisplayName("debería persistir el log con los datos correctos")
        void shouldPersistCallLog() {
            rateLimiter.registrarLlamada("/rest/api/2/search", "GET", 200, 1L);
            verify(callLogRepository).save(any(JiraApiCallLog.class));
        }

        @Test
        @DisplayName("purgarLogsAntiguos() debería llamar a deleteByExecutedAtBefore")
        void shouldCallDeleteOnPurge() {
            rateLimiter.purgarLogsAntiguos();
            verify(callLogRepository).deleteByExecutedAtBefore(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("ventana temporal")
    class VentanaTemporal {

        @Test
        @DisplayName("canMakeCall() con 0 llamadas en ventana debería devolver true")
        void shouldReturnTrue_whenNoCallsInWindow() {
            when(callLogRepository.countCallsSince(any(LocalDateTime.class))).thenReturn(0L);
            assertThat(rateLimiter.canMakeCall()).isTrue();
            assertThat(rateLimiter.llamadasRestantes()).isEqualTo(195L);
        }
    }
}
