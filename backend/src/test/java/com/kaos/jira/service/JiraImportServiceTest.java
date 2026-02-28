package com.kaos.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.entity.JiraWorklog;
import com.kaos.jira.repository.JiraCommentRepository;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.jira.repository.JiraRemoteLinkRepository;
import com.kaos.jira.repository.JiraWorklogRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.Tarea;
import com.kaos.planificacion.repository.TareaColaboradorRepository;
import com.kaos.planificacion.repository.TareaRepository;
import com.kaos.squad.entity.Squad;

/**
 * Tests unitarios para JiraImportService.
 * Cubre: upsert issues, mapeo de estados, worklogs, co-devs (DT-35/DT-39).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JiraImportService")
class JiraImportServiceTest {

    @Mock private JiraIssueRepository jiraIssueRepository;
    @Mock private JiraWorklogRepository jiraWorklogRepository;
    @Mock private JiraCommentRepository jiraCommentRepository;
    @Mock private JiraRemoteLinkRepository jiraRemoteLinkRepository;
    @Mock private TareaColaboradorRepository tareaColaboradorRepository;
    @Mock private TareaRepository tareaRepository;
    @Mock private PersonaRepository personaRepository;

    @InjectMocks
    private JiraImportService importService;

    private JiraConfig config;
    private Sprint sprint;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Inject real ObjectMapper via field (InjectMocks uses @Mock precedence, need manual set)
        importService = new JiraImportService(
                jiraIssueRepository, jiraWorklogRepository,
                jiraCommentRepository, jiraRemoteLinkRepository,
                tareaColaboradorRepository, tareaRepository,
                personaRepository, objectMapper);

        Squad squad = new Squad();
        squad.setId(1L);
        squad.setNombre("Red Squad");

        sprint = new Sprint();
        sprint.setId(10L);
        sprint.setNombre("Sprint-1");
        sprint.setSquad(squad);

        config = new JiraConfig();
        config.setSquad(squad);
        config.setUrl("https://jira.empresa.com");
        config.setMapeoEstados("{\"Done\":\"COMPLETADA\",\"In Progress\":\"EN_PROGRESO\",\"To Do\":\"PENDIENTE\"}");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildRawIssue(String key, String assigneeKey, String estado,
                                               Double estimacionSecs, String tipoJira) {
        return Map.of(
                "key", key,
                "fields", Map.of(
                        "summary", "Issue " + key,
                        "issuetype", Map.of("name", tipoJira),
                        "status", Map.of("name", estado),
                        "assignee", Map.of("key", assigneeKey, "displayName", assigneeKey),
                        "timeoriginalestimate", (int)(estimacionSecs * 3600),
                        "timespent", 0,
                        "priority", Map.of("name", "Medium")
                )
        );
    }

    private Map<String, Object> buildRawWorklog(String worklogId, String authorKey, double hours) {
        return Map.of(
                "id", worklogId,
                "author", Map.of("key", authorKey),
                "started", "2026-02-20T09:00:00.000+0100",
                "timeSpentSeconds", (int)(hours * 3600),
                "comment", "trabajo en la tarea"
        );
    }

    @Nested
    @DisplayName("processIssues()")
    class ProcessIssues {

        @Test
        @DisplayName("debería crear una nueva issue cuando no existe en BD")
        void shouldCreateIssue_whenNotExists() {
            when(jiraIssueRepository.findByJiraKey("PROJ-1")).thenReturn(Optional.empty());
            when(jiraIssueRepository.save(any(JiraIssue.class))).thenAnswer(inv -> inv.getArgument(0));
            when(personaRepository.findByIdJira("devuser")).thenReturn(Optional.empty());
            when(tareaRepository.save(any())).thenAnswer(inv -> {
                Tarea t = inv.getArgument(0); t.setId(1L); return t;
            });

            List<Map<String, Object>> rawIssues = List.of(buildRawIssue("PROJ-1", "devuser", "To Do", 8.0, "Story"));
            int result = importService.processIssues(rawIssues, config, sprint);

            assertThat(result).isEqualTo(1);
            verify(jiraIssueRepository, atLeast(1)).save(any(JiraIssue.class));
        }

        @Test
        @DisplayName("debería actualizar issue existente (upsert idempotente)")
        void shouldUpdateIssue_whenAlreadyExists() {
            JiraIssue existing = new JiraIssue();
            existing.setJiraKey("PROJ-1");
            existing.setEstadoJira("To Do");

            when(jiraIssueRepository.findByJiraKey("PROJ-1")).thenReturn(Optional.of(existing));
            when(jiraIssueRepository.save(any(JiraIssue.class))).thenAnswer(inv -> inv.getArgument(0));
            when(personaRepository.findByIdJira("devuser")).thenReturn(Optional.empty());
            when(tareaRepository.save(any())).thenAnswer(inv -> {
                Tarea t = inv.getArgument(0); t.setId(2L); return t;
            });

            List<Map<String, Object>> rawIssues = List.of(buildRawIssue("PROJ-1", "devuser", "In Progress", 8.0, "Story"));
            int result = importService.processIssues(rawIssues, config, sprint);

            assertThat(result).isEqualTo(1);
            assertThat(existing.getEstadoJira()).isEqualTo("In Progress");
        }

        @Test
        @DisplayName("debería mapear estado Jira → estado KAOS usando mapeoEstados")
        void shouldMapJiraStatusToKaosStatus() {
            when(jiraIssueRepository.findByJiraKey("PROJ-2")).thenReturn(Optional.empty());
            when(jiraIssueRepository.save(any(JiraIssue.class))).thenAnswer(inv -> inv.getArgument(0));
            when(personaRepository.findByIdJira(anyString())).thenReturn(Optional.empty());
            when(tareaRepository.save(any())).thenAnswer(inv -> {
                Tarea t = inv.getArgument(0); t.setId(3L); return t;
            });

            List<Map<String, Object>> rawIssues = List.of(buildRawIssue("PROJ-2", "dev", "Done", 4.0, "Bug"));
            int result = importService.processIssues(rawIssues, config, sprint);

            assertThat(result).isEqualTo(1);
            verify(jiraIssueRepository, atLeast(1)).save(any(JiraIssue.class));
        }

        @Test
        @DisplayName("debería continuar procesando siguientes issues si una falla")
        void shouldContinueOnSingleIssueError() {
            when(jiraIssueRepository.findByJiraKey("PROJ-BAD")).thenThrow(new RuntimeException("DB error"));
            when(jiraIssueRepository.findByJiraKey("PROJ-OK")).thenReturn(Optional.empty());
            when(jiraIssueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(personaRepository.findByIdJira(anyString())).thenReturn(Optional.empty());
            when(tareaRepository.save(any())).thenAnswer(inv -> { Tarea t = inv.getArgument(0); t.setId(4L); return t; });

            List<Map<String, Object>> rawIssues = List.of(
                    buildRawIssue("PROJ-BAD", "dev", "To Do", 0.0, "Task"),
                    buildRawIssue("PROJ-OK", "dev", "To Do", 4.0, "Task")
            );

            int result = importService.processIssues(rawIssues, config, sprint);
            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("debería devolver 0 si lista de issues es nula o vacía")
        void shouldReturnZero_whenEmptyInput() {
            assertThat(importService.processIssues(null, config, sprint)).isEqualTo(0);
            assertThat(importService.processIssues(List.of(), config, sprint)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("processWorklogs() — co-devs (DT-35/DT-39)")
    class ProcessWorklogs {

        private JiraIssue issue;

        @BeforeEach
        void setUpIssue() {
            issue = new JiraIssue();
            issue.setId(1L);
            issue.setJiraKey("PROJ-10");
            issue.setAsignadoJira("gmayordo");
        }

        @Test
        @DisplayName("debería persistir worklog del asignado principal sin crear co-dev")
        void shouldPersistWorklog_forMainAssignee() {
            when(jiraWorklogRepository.findByJiraWorklogId("wl-1")).thenReturn(Optional.empty());
            when(jiraWorklogRepository.save(any(JiraWorklog.class))).thenAnswer(inv -> inv.getArgument(0));
            when(personaRepository.findByIdJira("gmayordo")).thenReturn(Optional.empty());
            when(jiraIssueRepository.save(any())).thenReturn(issue);

            List<Map<String, Object>> rawWorklogs = List.of(buildRawWorklog("wl-1", "gmayordo", 4.0));
            int result = importService.processWorklogs(issue, rawWorklogs);

            assertThat(result).isEqualTo(1);
            verify(jiraWorklogRepository).save(any(JiraWorklog.class));
            // No se debe crear co-dev: mismo assignee
            verify(tareaColaboradorRepository, never()).save(any());
        }

        @Test
        @DisplayName("debería detectar co-dev cuando author ≠ assignee y issue tiene tarea vinculada (DT-39)")
        void shouldDetectCollaborator_whenAuthorDiffersFromAssignee() {
            // Tarea vinculada al issue
            Tarea tarea = new Tarea();
            tarea.setId(55L);
            issue.setTarea(tarea);

            Persona codev = new Persona();
            codev.setId(99L);
            codev.setIdJira("jperez");

            when(jiraWorklogRepository.findByJiraWorklogId("wl-2")).thenReturn(Optional.empty());
            when(jiraWorklogRepository.save(any(JiraWorklog.class))).thenAnswer(inv -> inv.getArgument(0));
            when(personaRepository.findByIdJira("jperez")).thenReturn(Optional.of(codev));
            when(personaRepository.findByIdJira("gmayordo")).thenReturn(Optional.empty());
            when(jiraIssueRepository.save(any())).thenReturn(issue);
            when(tareaColaboradorRepository.existsByTareaIdAndPersonaId(55L, 99L)).thenReturn(false);
            when(tareaColaboradorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<Map<String, Object>> rawWorklogs = List.of(buildRawWorklog("wl-2", "jperez", 3.0));
            importService.processWorklogs(issue, rawWorklogs);

            verify(tareaColaboradorRepository).save(any());
        }

        @Test
        @DisplayName("debería actualizar worklog existente (upsert idempotente)")
        void shouldUpdateExistingWorklog() {
            JiraWorklog existing = new JiraWorklog();
            existing.setJiraWorklogId("wl-3");
            existing.setHoras(new BigDecimal("2.0"));
            existing.setJiraIssue(issue);

            when(jiraWorklogRepository.findByJiraWorklogId("wl-3")).thenReturn(Optional.of(existing));
            when(jiraWorklogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(personaRepository.findByIdJira("gmayordo")).thenReturn(Optional.empty());
            when(jiraIssueRepository.save(any())).thenReturn(issue);

            List<Map<String, Object>> rawWorklogs = List.of(buildRawWorklog("wl-3", "gmayordo", 4.0));
            importService.processWorklogs(issue, rawWorklogs);

            verify(jiraWorklogRepository).save(any(JiraWorklog.class));
            assertThat(existing.getHoras()).isGreaterThan(new BigDecimal("2.0"));
        }

        @Test
        @DisplayName("debería devolver 0 cuando la lista de worklogs es vacía")
        void shouldReturnZero_whenNoWorklogs() {
            int result = importService.processWorklogs(issue, List.of());
            assertThat(result).isEqualTo(0);
        }
    }
}
