package com.kaos.jira.alert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.kaos.jira.alert.entity.JiraAlertRule;
import com.kaos.jira.alert.entity.JiraAlertRule.Severidad;
import com.kaos.jira.alert.entity.JiraAlertRule.TipoAlerta;
import com.kaos.jira.alert.entity.JiraAlerta;
import com.kaos.jira.alert.repository.JiraAlertRuleRepository;
import com.kaos.jira.alert.repository.JiraAlertaRepository;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.jira.repository.JiraWorklogRepository;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.planificacion.repository.TareaRepository;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests para JiraAlertEngineService (TASK-124 / DT-43..DT-45).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JiraAlertEngineService")
class JiraAlertEngineServiceTest {

    @Mock JiraAlertRuleRepository ruleRepository;
    @Mock JiraAlertaRepository alertaRepository;
    @Mock JiraIssueRepository issueRepository;
    @Mock TareaRepository tareaRepository;
    @Mock PersonaRepository personaRepository;
    @Mock SprintRepository sprintRepository;
    @Mock JiraWorklogRepository worklogRepository;

    @InjectMocks
    JiraAlertEngineService alertEngine;

    private Squad squad;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        squad = new Squad();
        squad.setId(1L);
        squad.setNombre("Squad Alpha");

        sprint = new Sprint();
        sprint.setId(10L);
        sprint.setSquad(squad);
        sprint.setFechaInicio(LocalDate.now().minusDays(14));
        sprint.setFechaFin(LocalDate.now().plusDays(7));
        sprint.setEstado(SprintEstado.ACTIVO);
    }

    // ── evaluarSprintActivo ──────────────────────────────────────────────────

    @Nested
    @DisplayName("evaluarSprintActivo()")
    class EvaluarSprintActivo {

        @Test
        @DisplayName("debería devolver lista vacía cuando no hay sprint ACTIVO")
        void shouldReturnEmpty_whenNoActiveSprint() {
            when(sprintRepository.findBySquadIdAndEstado(1L, SprintEstado.ACTIVO)).thenReturn(List.of());

            List<JiraAlerta> result = alertEngine.evaluarSprintActivo(1L);

            assertThat(result).isEmpty();
            verify(ruleRepository, never()).findActivasBySquadIdOrGlobal(anyLong());
        }

        @Test
        @DisplayName("debería delegar en evaluar() cuando hay sprint ACTIVO")
        void shouldDelegateToEvaluar_whenActiveSprint() {
            when(sprintRepository.findBySquadIdAndEstado(1L, SprintEstado.ACTIVO)).thenReturn(List.of(sprint));
            when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
            when(ruleRepository.findActivasBySquadIdOrGlobal(1L)).thenReturn(List.of());
            when(issueRepository.findBySprintId(10L)).thenReturn(List.of());
            when(tareaRepository.findBySprintIdWithJiraKey(10L)).thenReturn(List.of());
            when(personaRepository.findWithFilters(eq(1L), any(), any(), any(), eq(true), any(Pageable.class)))
                    .thenReturn(Page.empty());

            List<JiraAlerta> result = alertEngine.evaluarSprintActivo(1L);

            assertThat(result).isEmpty();
            verify(ruleRepository).findActivasBySquadIdOrGlobal(1L);
        }
    }

    // ── evaluar ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("evaluar()")
    class Evaluar {

        @Test
        @DisplayName("debería lanzar EntityNotFoundException cuando el sprint no existe")
        void shouldThrow_whenSprintNotFound() {
            when(sprintRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertEngine.evaluar(999L, 1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("debería devolver lista vacía cuando no hay reglas activas")
        void shouldReturnEmpty_whenNoActiveRules() {
            when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
            when(ruleRepository.findActivasBySquadIdOrGlobal(1L)).thenReturn(List.of());

            List<JiraAlerta> result = alertEngine.evaluar(10L, 1L);

            assertThat(result).isEmpty();
            verify(alertaRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("debería persistir alertas con saveAll cuando SpEL dispara para SPRINT_EN_RIESGO")
        void shouldPersistAlerts_whenSprintRiskRuleFires() {
            stubBuildContext();

            JiraAlertRule regla = buildRule(TipoAlerta.SPRINT_EN_RIESGO, Severidad.CRITICO, "true",
                    "Sprint en riesgo: pct={pctTiempo} vs completitud={pctCompletitud}");

            when(ruleRepository.findActivasBySquadIdOrGlobal(1L)).thenReturn(List.of(regla));
            when(alertaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<JiraAlerta> result = alertEngine.evaluar(10L, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSeveridad()).isEqualTo(Severidad.CRITICO);
            verify(alertaRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("debería generar alerta ESTADO_INCOHERENTE cuando SpEL evalúa true para cada issue")
        void shouldGenerateAlertPerIssue_whenEstadoIncoherenteRuleFires() {
            stubBuildContext();

            JiraIssue issueA = new JiraIssue();
            issueA.setJiraKey("PROJ-1");
            issueA.setEstadoJira("In Progress");
            issueA.setEstadoKaos("ToDo");

            when(issueRepository.findBySprintId(10L)).thenReturn(List.of(issueA));

            JiraAlertRule regla = buildRule(TipoAlerta.ESTADO_INCOHERENTE, Severidad.AVISO, "true",
                    "Estado incoherente: {jiraKey} jira={estadoJira} kaos={estadoKaos}");

            when(ruleRepository.findActivasBySquadIdOrGlobal(1L)).thenReturn(List.of(regla));
            when(alertaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<JiraAlerta> result = alertEngine.evaluar(10L, 1L);

            assertThat(result).hasSize(1);
            verify(alertaRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("debería NO llamar saveAll cuando la condición SpEL nunca dispara")
        void shouldNotSave_whenSpelConditionFalse() {
            stubBuildContext();

            JiraAlertRule regla = buildRule(TipoAlerta.SPRINT_EN_RIESGO, Severidad.AVISO, "false",
                    "Sprint en riesgo");

            when(ruleRepository.findActivasBySquadIdOrGlobal(1L)).thenReturn(List.of(regla));

            List<JiraAlerta> result = alertEngine.evaluar(10L, 1L);

            assertThat(result).isEmpty();
            verify(alertaRepository, never()).saveAll(anyList());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void stubBuildContext() {
        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
        when(issueRepository.findBySprintId(10L)).thenReturn(List.of());
        when(tareaRepository.findBySprintIdWithJiraKey(10L)).thenReturn(List.of());
        when(personaRepository.findWithFilters(eq(1L), any(), any(), any(), eq(true), any(Pageable.class)))
                .thenReturn(Page.empty());
    }

    private JiraAlertRule buildRule(TipoAlerta tipo, Severidad severidad,
                                    String condicion, String mensajeTemplate) {
        JiraAlertRule rule = new JiraAlertRule();
        rule.setId(1L);
        rule.setNombre("Regla de prueba");
        rule.setTipo(tipo);
        rule.setSeveridad(severidad);
        rule.setCondicionSpel(condicion);
        rule.setMensajeTemplate(mensajeTemplate);
        rule.setActiva(true);
        return rule;
    }
}
