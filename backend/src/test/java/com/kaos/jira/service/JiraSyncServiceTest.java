package com.kaos.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Collections;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;
import com.kaos.jira.dto.JiraSyncStatus;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.squad.entity.Squad;

/**
 * Tests unitarios para JiraSyncService.
 * Cubre la construcción de JQL filtrada por projectKey y la resolución del sprint activo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JiraSyncService")
class JiraSyncServiceTest {

    @Mock
    private JiraImportService jiraImportService;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private RestClient.Builder restClientBuilder;

    @InjectMocks
    private JiraSyncService jiraSyncService;

    private JiraConfig config;
    private Squad squad;
    private Sprint sprintActivo;

    @BeforeEach
    void setUp() {
        squad = new Squad();
        squad.setId(1L);
        squad.setNombre("RED");

        config = new JiraConfig();
        config.setId(1L);
        config.setSquad(squad);
        config.setJiraUrl("https://myorg.atlassian.net");
        config.setApiToken("secret-token");
        config.setUsuarioEmail("user@example.com");
        config.setProjectKey("RED");
        config.setBoardEvolutivoId(10L);
        config.setBoardCorrectivoId(20L);

        sprintActivo = new Sprint();
        sprintActivo.setId(5L);
        sprintActivo.setNombre("RED-Sprint-1");
        sprintActivo.setSquad(squad);
        sprintActivo.setEstado(SprintEstado.ACTIVO);
    }

    @Nested
    @DisplayName("construirJqlEvolutivo()")
    class ConstruirJqlEvolutivoTests {

        @Test
        @DisplayName("JQL evolutivo contiene filtro de proyecto")
        void testJqlEvolutivoContieneProjectKey() {
            String jql = jiraSyncService.construirJqlEvolutivo(config);

            assertThat(jql).contains("project = RED");
            assertThat(jql).contains("sprint in openSprints()");
            assertThat(jql).contains("issuetype not in (Sub-task)");
        }

        @Test
        @DisplayName("JQL evolutivo varía según el projectKey del config")
        void testJqlEvolutivoUsaProjectKeyDelConfig() {
            config.setProjectKey("GREEN");
            String jql = jiraSyncService.construirJqlEvolutivo(config);

            assertThat(jql).contains("project = GREEN");
            assertThat(jql).doesNotContain("project = RED");
        }
    }

    @Nested
    @DisplayName("construirJqlCorrectivo()")
    class ConstruirJqlCorrectivoTests {

        @Test
        @DisplayName("JQL correctivo contiene filtro de proyecto")
        void testJqlCorrectivoContieneProjectKey() {
            String jql = jiraSyncService.construirJqlCorrectivo(config);

            assertThat(jql).contains("project = RED");
            assertThat(jql).contains("sprint in openSprints()");
        }

        @Test
        @DisplayName("JQL correctivo varía según el projectKey del config")
        void testJqlCorrectivoUsaProjectKeyDelConfig() {
            config.setProjectKey("BLUE");
            String jql = jiraSyncService.construirJqlCorrectivo(config);

            assertThat(jql).contains("project = BLUE");
            assertThat(jql).doesNotContain("project = RED");
        }
    }

    @Nested
    @DisplayName("sincronizarIssues()")
    class SincronizarIssuesTests {

        @Test
        @DisplayName("Pasa sprint activo a processIssues cuando existe")
        void testPasaSprintActivoCuandoExiste() {
            when(sprintRepository.findFirstBySquadIdAndEstado(1L, SprintEstado.ACTIVO))
                    .thenReturn(Optional.of(sprintActivo));

            JiraSyncService spySyncService = org.mockito.Mockito.spy(jiraSyncService);
            org.mockito.Mockito.doReturn(Collections.emptyList())
                    .when(spySyncService).buscarIssues(any(), any());

            JiraSyncStatus status = new JiraSyncStatus();
            spySyncService.sincronizarIssues(config, status);

            verify(jiraImportService, org.mockito.Mockito.times(2))
                    .processIssues(anyList(), eq(config), eq(sprintActivo), eq(status));
        }

        @Test
        @DisplayName("Pasa null como sprint cuando no hay sprint activo")
        void testPasaNullSprintCuandoNoExiste() {
            when(sprintRepository.findFirstBySquadIdAndEstado(1L, SprintEstado.ACTIVO))
                    .thenReturn(Optional.empty());

            JiraSyncService spySyncService = org.mockito.Mockito.spy(jiraSyncService);
            org.mockito.Mockito.doReturn(Collections.emptyList())
                    .when(spySyncService).buscarIssues(any(), any());

            JiraSyncStatus status = new JiraSyncStatus();
            spySyncService.sincronizarIssues(config, status);

            verify(jiraImportService, org.mockito.Mockito.times(2))
                    .processIssues(anyList(), eq(config), eq((Sprint) null), eq(status));
        }

        @Test
        @DisplayName("No lanza excepción si no hay sprint activo")
        void testNoLanzaExcepcionSinSprintActivo() {
            when(sprintRepository.findFirstBySquadIdAndEstado(1L, SprintEstado.ACTIVO))
                    .thenReturn(Optional.empty());

            JiraSyncService spySyncService = org.mockito.Mockito.spy(jiraSyncService);
            org.mockito.Mockito.doReturn(Collections.emptyList())
                    .when(spySyncService).buscarIssues(any(), any());

            JiraSyncStatus status = new JiraSyncStatus();

            org.assertj.core.api.Assertions.assertThatNoException()
                    .isThrownBy(() -> spySyncService.sincronizarIssues(config, status));
        }

        @Test
        @DisplayName("Llama a processIssues dos veces: evolutivo y correctivo")
        void testLlamaProcessIssuesDosVeces() {
            when(sprintRepository.findFirstBySquadIdAndEstado(1L, SprintEstado.ACTIVO))
                    .thenReturn(Optional.of(sprintActivo));

            JiraSyncService spySyncService = org.mockito.Mockito.spy(jiraSyncService);
            List<Map<String, Object>> issuesMock = List.of(Map.of("key", "RED-1"));
            org.mockito.Mockito.doReturn(issuesMock)
                    .when(spySyncService).buscarIssues(any(), any());

            JiraSyncStatus status = new JiraSyncStatus();
            spySyncService.sincronizarIssues(config, status);

            verify(jiraImportService, org.mockito.Mockito.times(2))
                    .processIssues(anyList(), eq(config), eq(sprintActivo), eq(status));
        }
    }

    @Nested
    @DisplayName("JQL no hardcoded (sin projectKey causaría error)")
    class JqlNoGenericoTests {

        @Test
        @DisplayName("JQL evolutivo NO contiene filtro genérico sin proyecto")
        void testJqlEvolutivoNoEsGenerico() {
            String jql = jiraSyncService.construirJqlEvolutivo(config);
            // El JQL antiguo no contenía "project = "
            assertThat(jql).contains("project =");
        }

        @Test
        @DisplayName("JQL correctivo NO contiene filtro genérico sin proyecto")
        void testJqlCorrectivoNoEsGenerico() {
            String jql = jiraSyncService.construirJqlCorrectivo(config);
            assertThat(jql).contains("project =");
        }
    }
}
