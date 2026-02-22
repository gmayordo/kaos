package com.kaos.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.jira.entity.JiraSyncStatus;
import com.kaos.jira.entity.TipoSincronizacion;
import com.kaos.jira.repository.JiraSyncStatusRepository;

/**
 * Tests unitarios para JiraSyncService.
 * Cubre los criterios de aceptación del incremento Jira:
 *   - Primera sync (sin ultimaSync): JQL sin filtro de fecha
 *   - Sync posterior: JQL incluye AND updated >= "<ultimaSync>"
 *   - Logs indican modo [INCREMENTAL] vs [FULL]
 *   - ultima_sync se actualiza al completar OK
 *   - Si sync falla, ultima_sync NO se actualiza
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JiraSyncService")
class JiraSyncServiceTest {

    @Mock
    private JiraSyncStatusRepository jiraSyncStatusRepository;

    @InjectMocks
    @Spy
    private JiraSyncService jiraSyncService;

    private JiraConfig configEvolutivo;
    private JiraConfig configCorrectivo;

    @BeforeEach
    void setUp() {
        configEvolutivo = new JiraConfig();
        configEvolutivo.setProjectKey("BACK");
        configEvolutivo.setTipo(TipoSincronizacion.EVOLUTIVO);
        configEvolutivo.setActivo(true);

        configCorrectivo = new JiraConfig();
        configCorrectivo.setProjectKey("INFRA");
        configCorrectivo.setTipo(TipoSincronizacion.CORRECTIVO);
        configCorrectivo.setActivo(true);
    }

    // ─── construirJqlEvolutivo ────────────────────────────────────────────────

    @Nested
    @DisplayName("construirJqlEvolutivo()")
    class ConstruirJqlEvolutivoTests {

        @Test
        @DisplayName("Sin lastSync genera JQL de carga completa sin filtro de fecha")
        void sinLastSync_generaJqlCompleto() {
            String jql = jiraSyncService.construirJqlEvolutivo(configEvolutivo, null);

            assertThat(jql)
                    .contains("project = BACK")
                    .contains("sprint in openSprints()")
                    .contains("issuetype not in (Sub-task)")
                    .doesNotContain("updated >=")
                    .endsWith("ORDER BY updated ASC");
        }

        @Test
        @DisplayName("Con lastSync genera JQL incremental con filtro AND updated >=")
        void conLastSync_generaJqlIncremental() {
            LocalDateTime lastSync = LocalDateTime.of(2026, 2, 20, 10, 0);

            String jql = jiraSyncService.construirJqlEvolutivo(configEvolutivo, lastSync);

            assertThat(jql)
                    .contains("project = BACK")
                    .contains("sprint in openSprints()")
                    .contains("issuetype not in (Sub-task)")
                    .contains("AND updated >= \"2026-02-20 10:00\"")
                    .endsWith("ORDER BY updated ASC");
        }

        @Test
        @DisplayName("El formato de fecha JQL sigue el patrón yyyy-MM-dd HH:mm")
        void formatoFechaJql_esYyyyMmDdHhMm() {
            LocalDateTime lastSync = LocalDateTime.of(2026, 1, 5, 9, 5);

            String jql = jiraSyncService.construirJqlEvolutivo(configEvolutivo, lastSync);

            assertThat(jql).contains("\"2026-01-05 09:05\"");
        }
    }

    // ─── construirJqlCorrectivo ───────────────────────────────────────────────

    @Nested
    @DisplayName("construirJqlCorrectivo()")
    class ConstruirJqlCorrectivoTests {

        @Test
        @DisplayName("Sin lastSync genera JQL de carga completa sin filtro de fecha")
        void sinLastSync_generaJqlCompleto() {
            String jql = jiraSyncService.construirJqlCorrectivo(configCorrectivo, null);

            assertThat(jql)
                    .contains("project = INFRA")
                    .contains("issuetype in (Bug, Incident)")
                    .contains("statusCategory != Done")
                    .doesNotContain("updated >=")
                    .endsWith("ORDER BY updated ASC");
        }

        @Test
        @DisplayName("Con lastSync genera JQL incremental con filtro AND updated >=")
        void conLastSync_generaJqlIncremental() {
            LocalDateTime lastSync = LocalDateTime.of(2026, 2, 20, 10, 0);

            String jql = jiraSyncService.construirJqlCorrectivo(configCorrectivo, lastSync);

            assertThat(jql)
                    .contains("project = INFRA")
                    .contains("issuetype in (Bug, Incident)")
                    .contains("AND updated >= \"2026-02-20 10:00\"")
                    .endsWith("ORDER BY updated ASC");
        }
    }

    // ─── sincronizarIssues ────────────────────────────────────────────────────

    @Nested
    @DisplayName("sincronizarIssues()")
    class SincronizarIssuesTests {

        @Test
        @DisplayName("Primera sync: no hay ultimaSync, se crea registro con nueva fecha tras completar")
        void primeraSync_creaRegistroUltimaSync() {
            when(jiraSyncStatusRepository.findByProjectKey("BACK")).thenReturn(Optional.empty());
            when(jiraSyncStatusRepository.save(any(JiraSyncStatus.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            jiraSyncService.sincronizarIssues(configEvolutivo);

            ArgumentCaptor<JiraSyncStatus> captor = ArgumentCaptor.forClass(JiraSyncStatus.class);
            verify(jiraSyncStatusRepository).save(captor.capture());
            assertThat(captor.getValue().getProjectKey()).isEqualTo("BACK");
            assertThat(captor.getValue().getUltimaSync()).isNotNull();
        }

        @Test
        @DisplayName("Sync posterior: ultimaSync existente se actualiza al completar")
        void syncPosterior_actualizaUltimaSync() {
            LocalDateTime lastSync = LocalDateTime.of(2026, 2, 20, 10, 0);
            JiraSyncStatus status = new JiraSyncStatus();
            status.setProjectKey("BACK");
            status.setUltimaSync(lastSync);

            when(jiraSyncStatusRepository.findByProjectKey("BACK")).thenReturn(Optional.of(status));
            when(jiraSyncStatusRepository.save(any(JiraSyncStatus.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            jiraSyncService.sincronizarIssues(configEvolutivo);

            ArgumentCaptor<JiraSyncStatus> captor = ArgumentCaptor.forClass(JiraSyncStatus.class);
            verify(jiraSyncStatusRepository).save(captor.capture());
            assertThat(captor.getValue().getUltimaSync()).isAfter(lastSync);
        }

        @Test
        @DisplayName("Si la sync falla, ultima_sync NO se actualiza")
        void syncFalla_noActualizaUltimaSync() {
            when(jiraSyncStatusRepository.findByProjectKey("BACK")).thenReturn(Optional.empty());
            // Forzamos que ejecutarSincronizacion lance una excepción
            org.mockito.Mockito.doThrow(new RuntimeException("Jira API error"))
                    .when(jiraSyncService).ejecutarSincronizacion(any(), any());

            assertThatThrownBy(() -> jiraSyncService.sincronizarIssues(configEvolutivo))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Jira API error");

            verify(jiraSyncStatusRepository, never()).save(any(JiraSyncStatus.class));
        }

        @Test
        @DisplayName("Primera sync usa JQL de carga completa (sin updated >=)")
        void primeraSync_usaJqlCompleto() {
            when(jiraSyncStatusRepository.findByProjectKey("BACK")).thenReturn(Optional.empty());
            when(jiraSyncStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<String> jqlCaptor = ArgumentCaptor.forClass(String.class);
            org.mockito.Mockito.doNothing()
                    .when(jiraSyncService).ejecutarSincronizacion(jqlCaptor.capture(), any());

            jiraSyncService.sincronizarIssues(configEvolutivo);

            assertThat(jqlCaptor.getValue()).doesNotContain("updated >=");
        }

        @Test
        @DisplayName("Sync posterior usa JQL incremental (con updated >=)")
        void syncPosterior_usaJqlIncremental() {
            LocalDateTime lastSync = LocalDateTime.of(2026, 2, 20, 10, 0);
            JiraSyncStatus status = new JiraSyncStatus();
            status.setProjectKey("BACK");
            status.setUltimaSync(lastSync);

            when(jiraSyncStatusRepository.findByProjectKey("BACK")).thenReturn(Optional.of(status));
            when(jiraSyncStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<String> jqlCaptor = ArgumentCaptor.forClass(String.class);
            org.mockito.Mockito.doNothing()
                    .when(jiraSyncService).ejecutarSincronizacion(jqlCaptor.capture(), any());

            jiraSyncService.sincronizarIssues(configEvolutivo);

            assertThat(jqlCaptor.getValue()).contains("updated >= \"2026-02-20 10:00\"");
        }
    }
}
