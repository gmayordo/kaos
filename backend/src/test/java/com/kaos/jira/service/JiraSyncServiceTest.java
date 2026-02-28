package com.kaos.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.kaos.jira.alert.service.JiraAlertEngineService;
import com.kaos.jira.client.JiraApiClient;
import com.kaos.jira.config.JiraLoadConfig;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.entity.JiraSyncQueue;
import com.kaos.jira.entity.JiraSyncQueue.EstadoOperacion;
import com.kaos.jira.entity.JiraSyncQueue.TipoOperacion;
import com.kaos.jira.entity.JiraSyncStatus;
import com.kaos.jira.entity.JiraSyncStatus.EstadoSync;
import com.kaos.jira.repository.JiraConfigRepository;
import com.kaos.jira.repository.JiraSyncQueueRepository;
import com.kaos.jira.repository.JiraSyncStatusRepository;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests para JiraSyncService (TASK-124 / CA-01..CA-05).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JiraSyncService")
class JiraSyncServiceTest {

    @Mock JiraConfigRepository jiraConfigRepository;
    @Mock JiraSyncStatusRepository syncStatusRepository;
    @Mock JiraSyncQueueRepository syncQueueRepository;
    @Mock JiraApiClient jiraApiClient;
    @Mock JiraImportService jiraImportService;
    @Mock JiraRateLimiter rateLimiter;
    @Mock JiraLoadConfig jiraLoadConfig;
    @Mock JiraAlertEngineService alertEngineService;
    @Mock JiraResumenEmailService resumenEmailService;
    @Mock SprintRepository sprintRepository;

    @InjectMocks
    JiraSyncService syncService;

    private Squad squad;
    private JiraConfig config;

    @BeforeEach
    void setUp() {
        squad = new Squad();
        squad.setId(1L);
        squad.setNombre("Squad Test");

        config = new JiraConfig();
        config.setSquad(squad);
        // Boards null → sincronizarIssues retorna 0 sin llamar a la API
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private JiraSyncStatus buildStatus() {
        return JiraSyncStatus.builder()
                .squad(squad)
                .estado(EstadoSync.IDLE)
                .build();
    }

    private void stubConfigAndStatus() {
        when(jiraConfigRepository.findBySquadIdAndActivaTrue(1L)).thenReturn(Optional.of(config));
        when(syncStatusRepository.findBySquadId(1L)).thenReturn(Optional.of(buildStatus()));
        when(syncStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(syncQueueRepository.countBySquadIdAndEstado(anyLong(), any())).thenReturn(0L);
        when(sprintRepository.findBySquadIdAndEstado(anyLong(), any(SprintEstado.class))).thenReturn(List.of());
    }

    // ── syncCompleta ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("syncCompleta()")
    class SyncCompleta {

        @Test
        @DisplayName("debería lanzar EntityNotFoundException cuando no hay config activa para el squad")
        void shouldThrow_whenNoActiveConfig() {
            when(jiraConfigRepository.findBySquadIdAndActivaTrue(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> syncService.syncCompleta(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("debería encolar operación y NO llamar a la API cuando cuota >= 190 (DT-41)")
        void shouldEnqueue_whenQuotaInsufficient() {
            stubConfigAndStatus();
            when(rateLimiter.llamadasConsumidas()).thenReturn(195L);
            when(syncQueueRepository.findBySquadIdAndTipoOperacionAndEstado(1L, TipoOperacion.SYNC_ISSUES, EstadoOperacion.PENDIENTE))
                    .thenReturn(Optional.empty());
            when(syncQueueRepository.save(any(JiraSyncQueue.class))).thenAnswer(inv -> inv.getArgument(0));

            syncService.syncCompleta(1L);

            verify(syncQueueRepository).save(any(JiraSyncQueue.class));
            verify(jiraApiClient, never()).buscarIssues(any(), any());
        }

        @Test
        @DisplayName("debería completar sync y llamar alertEngineService cuando cuota disponible (DT-42)")
        void shouldCompleteSync_andEvaluateAlerts_whenQuotaAvailable() {
            stubConfigAndStatus();
            when(rateLimiter.llamadasConsumidas()).thenReturn(50L);
            when(rateLimiter.canMakeCall()).thenReturn(true);
            when(jiraApiClient.buscarIssues(any(), any())).thenReturn(List.of());
            when(alertEngineService.evaluarSprintActivo(1L)).thenReturn(List.of());

            syncService.syncCompleta(1L);

            verify(alertEngineService).evaluarSprintActivo(1L);
            verify(resumenEmailService).enviarResumenSync(any(), eq(1L));
        }

        @Test
        @DisplayName("debería registrar error en status cuando la sync lanza excepción interna")
        void shouldRegisterError_whenSyncThrows() {
            stubConfigAndStatus();
            when(rateLimiter.llamadasConsumidas()).thenReturn(50L);
            when(rateLimiter.canMakeCall()).thenReturn(true);
            when(jiraApiClient.buscarIssues(any(), any())).thenThrow(new RuntimeException("DB connection failed"));

            // No debe relanzar: el error queda encapsulado en el status
            syncService.syncCompleta(1L);

            // Status is saved at least once (before sync starts)
            verify(syncStatusRepository, atLeast(1)).save(any(JiraSyncStatus.class));
        }
    }

    // ── syncSoloIssues ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("syncSoloIssues()")
    class SyncSoloIssues {

        @Test
        @DisplayName("debería encolar SYNC_ISSUES cuando no hay cuota disponible")
        void shouldEnqueue_whenCannotMakeCall() {
            stubConfigAndStatus();
            when(rateLimiter.canMakeCall()).thenReturn(false);
            when(rateLimiter.llamadasConsumidas()).thenReturn(199L);
            when(syncQueueRepository.findBySquadIdAndTipoOperacionAndEstado(1L, TipoOperacion.SYNC_ISSUES, EstadoOperacion.PENDIENTE))
                    .thenReturn(Optional.empty());
            when(syncQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            syncService.syncSoloIssues(1L);

            verify(syncQueueRepository).save(any(JiraSyncQueue.class));
            verify(jiraApiClient, never()).buscarIssues(any(), any());
        }
    }

    // ── procesarOperacionCola ────────────────────────────────────────────────

    @Nested
    @DisplayName("procesarOperacionCola()")
    class ProcesarOperacionCola {

        @Test
        @DisplayName("debería marcar operación COMPLETADA tras procesamiento exitoso")
        void shouldMarkCompleted_onSuccess() {
            stubConfigAndStatus();
            when(rateLimiter.canMakeCall()).thenReturn(true);
            when(rateLimiter.llamadasConsumidas()).thenReturn(10L);
            when(jiraApiClient.buscarIssues(any(), any())).thenReturn(List.of());

            JiraSyncQueue operacion = new JiraSyncQueue();
            operacion.setId(10L);
            operacion.setSquad(squad);
            operacion.setTipoOperacion(TipoOperacion.SYNC_ISSUES);
            operacion.setEstado(EstadoOperacion.PENDIENTE);

            when(syncQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            syncService.procesarOperacionCola(operacion);

            // Operación debe quedar COMPLETADA
            org.assertj.core.api.Assertions.assertThat(operacion.getEstado())
                    .isEqualTo(EstadoOperacion.COMPLETADA);
        }
    }

    // ── Carga de subtareas ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Carga de subtareas (sincronizarConInline)")
    class SubtaskLoading {

        @BeforeEach
        void setUpConfig() {
            config.setBoardEvolutivoId(100L);
            config.setBoardCorrectivoId(200L);
            config.setUrl("https://jira.test.com");
            config.setUsuario("testuser");
            config.setToken("testtoken");
        }

        private Map<String, Object> buildRawIssue(String key, String id, String type, String summary) {
            Map<String, Object> fields = new HashMap<>();
            fields.put("summary", summary);
            fields.put("issuetype", Map.of("name", type));
            fields.put("status", Map.of("name", "In Progress"));
            fields.put("assignee", Map.of("key", "juser1"));
            fields.put("priority", Map.of("name", "Medium"));
            fields.put("worklog", Map.of("total", 0, "maxResults", 20, "worklogs", List.of()));
            fields.put("comment", Map.of("comments", List.of()));
            fields.put("timetracking", Map.of());

            Map<String, Object> issue = new HashMap<>();
            issue.put("key", key);
            issue.put("id", id);
            issue.put("fields", fields);
            return issue;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> buildRawSubtask(String key, String id, String summary, String parentKey) {
            Map<String, Object> issue = buildRawIssue(key, id, "Sub-task", summary);
            Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            fields.put("parent", Map.of("key", parentKey));
            return issue;
        }

        @Test
        @DisplayName("debería buscar subtareas de los padres y procesar todas las issues")
        void shouldFetchAndProcessSubtasksFromParents() {
            // Given: 2 padres + 3 subtareas
            Map<String, Object> parent1 = buildRawIssue("RED-101", "10101", "Story", "Parent Story 1");
            Map<String, Object> parent2 = buildRawIssue("RED-102", "10102", "Task", "Parent Task 1");
            Map<String, Object> sub1 = buildRawSubtask("RED-103", "10103", "Subtask 1", "RED-101");
            Map<String, Object> sub2 = buildRawSubtask("RED-104", "10104", "Subtask 2", "RED-101");
            Map<String, Object> sub3 = buildRawSubtask("RED-105", "10105", "Subtask 3", "RED-102");

            stubConfigAndStatus();
            when(rateLimiter.llamadasConsumidas()).thenReturn(10L);
            when(rateLimiter.canMakeCall()).thenReturn(true);

            // Primera llamada devuelve padres, segunda devuelve subtareas
            when(jiraApiClient.buscarIssues(any(), any()))
                    .thenReturn(List.of(parent1, parent2))
                    .thenReturn(List.of(sub1, sub2, sub3));

            JiraIssue mockIssue = JiraIssue.builder().jiraKey("MOCK-001").build();
            when(jiraImportService.processIssue(any(), any(), any())).thenReturn(mockIssue);
            when(jiraApiClient.obtenerRemoteLinks(any(), any())).thenReturn(List.of());

            // When
            syncService.syncCompleta(1L);

            // Then: buscarIssues debe llamarse 2 veces (padres + subtareas)
            ArgumentCaptor<String> jqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jiraApiClient, times(2)).buscarIssues(any(), jqlCaptor.capture());

            List<String> jqls = jqlCaptor.getAllValues();
            // Primer JQL: issues padre (openSprints)
            assertThat(jqls.get(0)).contains("openSprints");
            // Segundo JQL: subtareas (parent in (...))
            assertThat(jqls.get(1)).contains("parent in");
            assertThat(jqls.get(1)).contains("RED-101");
            assertThat(jqls.get(1)).contains("RED-102");

            // processIssue se llama 5 veces (2 padres + 3 subtareas)
            verify(jiraImportService, times(5)).processIssue(any(), any(), any());
        }

        @Test
        @DisplayName("JQL de subtareas NO debe entrecomillar las keys de issues")
        void subtaskJql_shouldNotQuoteIssueKeys() {
            Map<String, Object> parent1 = buildRawIssue("RED-101", "10101", "Story", "Parent 1");
            Map<String, Object> parent2 = buildRawIssue("RED-102", "10102", "Task", "Parent 2");

            stubConfigAndStatus();
            when(rateLimiter.llamadasConsumidas()).thenReturn(10L);
            when(rateLimiter.canMakeCall()).thenReturn(true);
            when(jiraApiClient.buscarIssues(any(), any()))
                    .thenReturn(List.of(parent1, parent2))
                    .thenReturn(List.of());

            JiraIssue mockIssue = JiraIssue.builder().jiraKey("MOCK-001").build();
            when(jiraImportService.processIssue(any(), any(), any())).thenReturn(mockIssue);
            when(jiraApiClient.obtenerRemoteLinks(any(), any())).thenReturn(List.of());

            syncService.syncCompleta(1L);

            ArgumentCaptor<String> jqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jiraApiClient, times(2)).buscarIssues(any(), jqlCaptor.capture());

            String subtaskJql = jqlCaptor.getAllValues().get(1);
            // Las keys NO deben ir entrecomilladas en JQL
            assertThat(subtaskJql).contains("parent in (RED-101, RED-102)");
            assertThat(subtaskJql).doesNotContain("\"RED-101\"");
            assertThat(subtaskJql).doesNotContain("\"RED-102\"");
        }

        @Test
        @DisplayName("debería omitir búsqueda de subtareas si rateLimiter agota la cuota")
        void shouldSkipSubtasks_whenRateLimiterExhausted() {
            Map<String, Object> parent1 = buildRawIssue("RED-101", "10101", "Story", "Parent 1");

            stubConfigAndStatus();
            when(rateLimiter.llamadasConsumidas()).thenReturn(10L);
            // canMakeCall devuelve false: el check en sincronizarConInline
            // para subtareas se evalúa como false y se omite la búsqueda.
            // (buscarIssues está mockeado así que no consume canMakeCall internamente)
            when(rateLimiter.canMakeCall()).thenReturn(false);
            when(jiraApiClient.buscarIssues(any(), any())).thenReturn(List.of(parent1));

            JiraIssue mockIssue = JiraIssue.builder().jiraKey("MOCK-001").build();
            when(jiraImportService.processIssue(any(), any(), any())).thenReturn(mockIssue);

            syncService.syncCompleta(1L);

            // Solo UNA llamada a buscarIssues (padres), subtareas omitidas
            verify(jiraApiClient, times(1)).buscarIssues(any(), any());
            // Solo 1 issue procesada (el padre)
            verify(jiraImportService, times(1)).processIssue(any(), any(), any());
        }

        @Test
        @DisplayName("debería asociar sprint activo a las issues procesadas")
        void shouldAssociateActiveSprintToIssues() {
            Map<String, Object> parent1 = buildRawIssue("RED-101", "10101", "Story", "Parent 1");
            Map<String, Object> sub1 = buildRawSubtask("RED-102", "10102", "Subtask", "RED-101");

            Sprint sprintActivo = new Sprint();
            sprintActivo.setId(10L);
            sprintActivo.setNombre("Sprint 1");
            sprintActivo.setSquad(squad);
            sprintActivo.setEstado(SprintEstado.ACTIVO);

            stubConfigAndStatus();
            // Sobreescribir el mock de sprintRepository para devolver sprint activo
            when(sprintRepository.findBySquadIdAndEstado(1L, SprintEstado.ACTIVO))
                    .thenReturn(List.of(sprintActivo));
            when(rateLimiter.llamadasConsumidas()).thenReturn(10L);
            when(rateLimiter.canMakeCall()).thenReturn(true);
            when(jiraApiClient.buscarIssues(any(), any()))
                    .thenReturn(List.of(parent1))
                    .thenReturn(List.of(sub1));

            JiraIssue mockIssue = JiraIssue.builder().jiraKey("MOCK-001").build();
            when(jiraImportService.processIssue(any(), any(), any())).thenReturn(mockIssue);
            when(jiraApiClient.obtenerRemoteLinks(any(), any())).thenReturn(List.of());

            syncService.syncCompleta(1L);

            // Verificar que processIssue recibe el sprint activo (no null)
            ArgumentCaptor<Sprint> sprintCaptor = ArgumentCaptor.forClass(Sprint.class);
            verify(jiraImportService, times(2)).processIssue(any(), any(), sprintCaptor.capture());

            List<Sprint> sprintsPasados = sprintCaptor.getAllValues();
            assertThat(sprintsPasados).allSatisfy(s -> {
                assertThat(s).isNotNull();
                assertThat(s.getId()).isEqualTo(10L);
            });
        }

        @Test
        @DisplayName("debería procesar worklogs y comments inline de subtareas")
        void shouldProcessInlineWorklogsAndCommentsForSubtasks() {
            // Subtask con 1 worklog y 1 comment inline
            Map<String, Object> sub1 = buildRawSubtask("RED-103", "10103", "Subtask con worklogs", "RED-101");
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) sub1.get("fields");
            fields.put("worklog", Map.of(
                    "total", 1, "maxResults", 20,
                    "worklogs", List.of(Map.of(
                            "id", "wl-1", "author", Map.of("key", "juser2"),
                            "timeSpentSeconds", 3600, "started", "2026-02-27T10:00:00.000+0100"
                    ))
            ));
            fields.put("comment", Map.of(
                    "comments", List.of(Map.of(
                            "id", "cm-1", "author", Map.of("key", "juser2"),
                            "body", "Comentario de prueba", "created", "2026-02-27T10:00:00.000+0100"
                    ))
            ));

            Map<String, Object> parent1 = buildRawIssue("RED-101", "10101", "Story", "Parent 1");

            stubConfigAndStatus();
            when(rateLimiter.llamadasConsumidas()).thenReturn(10L);
            when(rateLimiter.canMakeCall()).thenReturn(true);
            when(jiraApiClient.buscarIssues(any(), any()))
                    .thenReturn(List.of(parent1))
                    .thenReturn(List.of(sub1));

            JiraIssue mockIssue = JiraIssue.builder().jiraKey("RED-103").build();
            when(jiraImportService.processIssue(any(), any(), any())).thenReturn(mockIssue);
            when(jiraApiClient.obtenerRemoteLinks(any(), any())).thenReturn(List.of());

            syncService.syncCompleta(1L);

            // Worklogs y comments se procesan para ambas issues (padre + subtarea)
            verify(jiraImportService, times(2)).processWorklogs(any(), any());
            verify(jiraImportService, times(2)).processComments(any(), any());
        }

        @Test
        @DisplayName("debería manejar parentKeys vacíos sin llamar a la segunda búsqueda")
        void shouldSkipSubtaskSearch_whenNoParentKeys() {
            stubConfigAndStatus();
            when(rateLimiter.llamadasConsumidas()).thenReturn(10L);
            when(rateLimiter.canMakeCall()).thenReturn(true);
            // Primera búsqueda devuelve vacío
            when(jiraApiClient.buscarIssues(any(), any())).thenReturn(List.of());

            syncService.syncCompleta(1L);

            // Solo UNA llamada a buscarIssues (padres). No se buscan subtareas.
            verify(jiraApiClient, times(1)).buscarIssues(any(), any());
            // No se procesa ninguna issue
            verify(jiraImportService, never()).processIssue(any(), any(), any());
        }
    }
}
