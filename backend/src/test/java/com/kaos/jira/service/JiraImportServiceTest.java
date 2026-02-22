package com.kaos.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
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
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.entity.JiraIssueTypeConfig;
import com.kaos.jira.repository.JiraIssueTypeConfigRepository;
import com.kaos.squad.entity.Squad;

/**
 * Tests unitarios para JiraImportService.
 * Cubre la detección de subtipo_jira en sub-tasks y la lógica de processIssue().
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JiraImportService")
class JiraImportServiceTest {

    @Mock
    private JiraIssueTypeConfigRepository jiraIssueTypeConfigRepository;

    @InjectMocks
    private JiraImportService jiraImportService;

    private Squad squad;

    @BeforeEach
    void setUp() {
        squad = new Squad();
        squad.setId(1L);
        squad.setNombre("Squad Test");
    }

    // ── detectarSubtipo() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("detectarSubtipo()")
    class DetectarSubtipoTests {

        private JiraIssueTypeConfig config(String patron, String subtipo) {
            JiraIssueTypeConfig cfg = new JiraIssueTypeConfig();
            cfg.setPatronNombre(patron);
            cfg.setSubtipoKaos(subtipo);
            cfg.setActiva(true);
            return cfg;
        }

        @Test
        @DisplayName("Summary 'Desarrollo de login' → DESARROLLO")
        void testDesarrollo() {
            List<JiraIssueTypeConfig> configs = List.of(
                config("desarrollo.*", "DESARROLLO"),
                config("junit.*|pruebas unitarias.*", "JUNIT"),
                config("documentaci.*|doc.*", "DOCUMENTACION")
            );

            String result = jiraImportService.detectarSubtipo("Desarrollo de login", configs);

            assertThat(result).isEqualTo("DESARROLLO");
        }

        @Test
        @DisplayName("Summary 'JUnit validaciones' → JUNIT")
        void testJUnit() {
            List<JiraIssueTypeConfig> configs = List.of(
                config("desarrollo.*", "DESARROLLO"),
                config("junit.*|pruebas unitarias.*", "JUNIT"),
                config("documentaci.*|doc.*", "DOCUMENTACION")
            );

            String result = jiraImportService.detectarSubtipo("JUnit validaciones", configs);

            assertThat(result).isEqualTo("JUNIT");
        }

        @Test
        @DisplayName("Summary 'Pruebas unitarias del módulo' → JUNIT")
        void testPruebasUnitarias() {
            List<JiraIssueTypeConfig> configs = List.of(
                config("desarrollo.*", "DESARROLLO"),
                config("junit.*|pruebas unitarias.*", "JUNIT"),
                config("documentaci.*|doc.*", "DOCUMENTACION")
            );

            String result = jiraImportService.detectarSubtipo("Pruebas unitarias del módulo", configs);

            assertThat(result).isEqualTo("JUNIT");
        }

        @Test
        @DisplayName("Summary 'Documentación API' → DOCUMENTACION")
        void testDocumentacion() {
            List<JiraIssueTypeConfig> configs = List.of(
                config("desarrollo.*", "DESARROLLO"),
                config("junit.*|pruebas unitarias.*", "JUNIT"),
                config("documentaci.*|doc.*", "DOCUMENTACION")
            );

            String result = jiraImportService.detectarSubtipo("Documentación API", configs);

            assertThat(result).isEqualTo("DOCUMENTACION");
        }

        @Test
        @DisplayName("Summary sin patrón reconocido → OTROS")
        void testOtros() {
            List<JiraIssueTypeConfig> configs = List.of(
                config("desarrollo.*", "DESARROLLO"),
                config("junit.*|pruebas unitarias.*", "JUNIT")
            );

            String result = jiraImportService.detectarSubtipo("Revisión de código", configs);

            assertThat(result).isEqualTo("OTROS");
        }

        @Test
        @DisplayName("Lista de configs vacía → OTROS")
        void testConfigsVacias() {
            String result = jiraImportService.detectarSubtipo("Desarrollo de algo", List.of());

            assertThat(result).isEqualTo("OTROS");
        }

        @Test
        @DisplayName("Summary null → OTROS")
        void testSummaryNull() {
            String result = jiraImportService.detectarSubtipo(null, List.of(
                config("desarrollo.*", "DESARROLLO")
            ));

            assertThat(result).isEqualTo("OTROS");
        }

        @Test
        @DisplayName("Configs null → OTROS")
        void testConfigsNull() {
            String result = jiraImportService.detectarSubtipo("Desarrollo de algo", null);

            assertThat(result).isEqualTo("OTROS");
        }
    }

    // ── processIssue() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("processIssue()")
    class ProcessIssueTests {

        private JiraIssue buildIssue(String issueKey, String summary, String parentKey) {
            JiraIssue issue = new JiraIssue();
            issue.setSquad(squad);
            issue.setIssueKey(issueKey);
            issue.setSummary(summary);
            issue.setTipoJira("Sub-task");
            issue.setParentKey(parentKey);
            return issue;
        }

        @Test
        @DisplayName("Sub-task 'Desarrollo de login' → subtipoJira=DESARROLLO")
        void testSubtaskDesarrollo() {
            JiraIssue issue = buildIssue("KAOS-10", "Desarrollo de login", "KAOS-1");
            JiraIssueTypeConfig cfg = new JiraIssueTypeConfig();
            cfg.setPatronNombre("desarrollo.*");
            cfg.setSubtipoKaos("DESARROLLO");
            cfg.setActiva(true);

            when(jiraIssueTypeConfigRepository.findBySquadIdAndTipoJiraAndActivaTrue(1L, "Sub-task"))
                .thenReturn(List.of(cfg));

            jiraImportService.processIssue(issue);

            assertThat(issue.getSubtipoJira()).isEqualTo("DESARROLLO");
        }

        @Test
        @DisplayName("Sub-task sin patrón reconocido → subtipoJira=OTROS")
        void testSubtaskOtros() {
            JiraIssue issue = buildIssue("KAOS-11", "Revisión de código", "KAOS-1");

            when(jiraIssueTypeConfigRepository.findBySquadIdAndTipoJiraAndActivaTrue(1L, "Sub-task"))
                .thenReturn(List.of());

            jiraImportService.processIssue(issue);

            assertThat(issue.getSubtipoJira()).isEqualTo("OTROS");
        }

        @Test
        @DisplayName("Issue que no es sub-task (parentKey=null) → subtipoJira=null")
        void testNoSubtask() {
            JiraIssue issue = new JiraIssue();
            issue.setSquad(squad);
            issue.setIssueKey("KAOS-5");
            issue.setSummary("Historia de usuario");
            issue.setTipoJira("Story");
            issue.setParentKey(null);

            jiraImportService.processIssue(issue);

            assertThat(issue.getSubtipoJira()).isNull();
        }
    }
}
