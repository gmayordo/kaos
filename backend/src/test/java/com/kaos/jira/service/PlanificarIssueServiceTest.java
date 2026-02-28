package com.kaos.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
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
import com.kaos.jira.dto.PlanificarAsignacionItem;
import com.kaos.jira.dto.PlanificarIssueRequest;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.dto.TareaResponse;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.entity.Tarea;
import com.kaos.planificacion.exception.SprintNoEnPlanificacionException;
import com.kaos.planificacion.mapper.TareaMapper;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.planificacion.repository.TareaRepository;
import com.kaos.planificacion.service.PlantillaAsignacionService;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para {@link PlanificarIssueService}.
 * Cubre planificación de issues Jira → Tareas KAOS.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PlanificarIssueService")
class PlanificarIssueServiceTest {

    @Mock private JiraIssueRepository jiraIssueRepository;
    @Mock private TareaRepository tareaRepository;
    @Mock private SprintRepository sprintRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private TareaMapper tareaMapper;
    @Mock private PlantillaAsignacionService plantillaService;

    @InjectMocks
    private PlanificarIssueService service;

    private Sprint sprint;
    private Squad squad;
    private JiraIssue issue;
    private Persona persona;

    @BeforeEach
    void setUp() {
        squad = new Squad();
        squad.setId(1L);
        squad.setNombre("Backend");

        sprint = new Sprint();
        sprint.setId(10L);
        sprint.setNombre("Sprint 1");
        sprint.setEstado(SprintEstado.PLANIFICACION);
        sprint.setSquad(squad);

        persona = new Persona();
        persona.setId(5L);
        persona.setNombre("Ana García");

        issue = new JiraIssue();
        issue.setId(100L);
        issue.setJiraKey("RED-42");
        issue.setSummary("Implementar endpoint de login");
        issue.setTipoJira("Story");
        issue.setEstadoJira("To Do");
        issue.setEstimacionHoras(BigDecimal.valueOf(8));
        issue.setTarea(null);
        issue.setPersona(persona);
        issue.setParentKey(null);
    }

    // ══════════════════════════════════════════════════════════
    // listarIssuesPlanificables
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listarIssuesPlanificables")
    class ListarTests {

        @Test
        @DisplayName("soloSinTarea=false usa query sin filtro de tarea")
        void listar_soloSinTareaFalse_usaQueryCompleta() {
            when(jiraIssueRepository.findBySquadIdAndSprintId(1L, 10L))
                    .thenReturn(List.of(issue));

            var result = service.listarIssuesPlanificables(1L, 10L, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).jiraKey()).isEqualTo("RED-42");
            verify(jiraIssueRepository).findBySquadIdAndSprintId(1L, 10L);
            verify(jiraIssueRepository, never()).findBySquadIdAndSprintIdAndTareaIsNull(any(), any());
        }

        @Test
        @DisplayName("soloSinTarea=true usa query filtrada por tarea nula")
        void listar_soloSinTareaTrue_usaQueryFiltrada() {
            when(jiraIssueRepository.findBySquadIdAndSprintIdAndTareaIsNull(1L, 10L))
                    .thenReturn(List.of(issue));

            var result = service.listarIssuesPlanificables(1L, 10L, true);

            assertThat(result).hasSize(1);
            verify(jiraIssueRepository).findBySquadIdAndSprintIdAndTareaIsNull(1L, 10L);
            verify(jiraIssueRepository, never()).findBySquadIdAndSprintId(any(), any());
        }

        @Test
        @DisplayName("construye jerarquía parent → subtarea")
        void listar_conSubtarea_construyeJerarquia() {
            JiraIssue padre = new JiraIssue();
            padre.setId(101L);
            padre.setJiraKey("RED-40");
            padre.setSummary("Historia padre");
            padre.setTipoJira("Story");
            padre.setEstadoJira("In Progress");
            padre.setParentKey(null);
            padre.setTarea(null);

            JiraIssue subtarea = new JiraIssue();
            subtarea.setId(102L);
            subtarea.setJiraKey("RED-41");
            subtarea.setSummary("Subtarea de login");
            subtarea.setTipoJira("Sub-task");
            subtarea.setEstadoJira("To Do");
            subtarea.setParentKey("RED-40");  // hijo de padre
            subtarea.setTarea(null);

            when(jiraIssueRepository.findBySquadIdAndSprintId(1L, 10L))
                    .thenReturn(List.of(padre, subtarea));

            var result = service.listarIssuesPlanificables(1L, 10L, false);

            // Solo el padre aparece como raíz
            assertThat(result).hasSize(1);
            assertThat(result.get(0).jiraKey()).isEqualTo("RED-40");
            assertThat(result.get(0).subtareas()).hasSize(1);
            assertThat(result.get(0).subtareas().get(0).jiraKey()).isEqualTo("RED-41");
        }

        @Test
        @DisplayName("lista vacía devuelve lista vacía sin excepción")
        void listar_listaVacia_devuelveLista() {
            when(jiraIssueRepository.findBySquadIdAndSprintIdAndTareaIsNull(1L, 10L))
                    .thenReturn(List.of());

            var result = service.listarIssuesPlanificables(1L, 10L, true);

            assertThat(result).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════
    // planificar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("planificar")
    class PlanificarTests {

        private PlanificarAsignacionItem itemBase;
        private TareaResponse tareaResponseMock;
        private Tarea tareaCreada;

        @BeforeEach
        void setUp() {
            itemBase = new PlanificarAsignacionItem(
                    "RED-42", 5L, BigDecimal.valueOf(8), 2, null, null, null);

            tareaCreada = new Tarea();
            tareaCreada.setId(200L);
            tareaCreada.setTitulo("Implementar endpoint de login");
            tareaCreada.setSprint(sprint);

            tareaResponseMock = mock(TareaResponse.class);
            when(tareaResponseMock.id()).thenReturn(200L);

            when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
            when(jiraIssueRepository.findByJiraKeyIn(anyList())).thenReturn(List.of(issue));
            when(tareaRepository.save(any())).thenReturn(tareaCreada);
            when(tareaMapper.toResponse(any())).thenReturn(tareaResponseMock);
            when(personaRepository.getReferenceById(5L)).thenReturn(persona);
        }

        @Test
        @DisplayName("planificación exitosa crea tarea y vincula issue")
        void planificar_issueValido_creaYVincula() {
            var request = new PlanificarIssueRequest(10L, List.of(itemBase));

            var result = service.planificar(request);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(200L);
            verify(tareaRepository).save(any(Tarea.class));
            verify(jiraIssueRepository).save(issue);
        }

        @Test
        @DisplayName("sprint no encontrado → EntityNotFoundException")
        void planificar_sprintNoEncontrado_lanzaEntityNotFound() {
            when(sprintRepository.findById(99L)).thenReturn(Optional.empty());
            var request = new PlanificarIssueRequest(99L, List.of(itemBase));

            assertThatThrownBy(() -> service.planificar(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Sprint no encontrado");
        }

        @Test
        @DisplayName("sprint en estado CERRADO → SprintNoEnPlanificacionException")
        void planificar_sprintCerrado_lanzaSprintNoEnPlanificacion() {
            sprint.setEstado(SprintEstado.CERRADO);
            var request = new PlanificarIssueRequest(10L, List.of(itemBase));

            assertThatThrownBy(() -> service.planificar(request))
                    .isInstanceOf(SprintNoEnPlanificacionException.class);
        }

        @Test
        @DisplayName("issue no encontrado → EntityNotFoundException")
        void planificar_issueNoEncontrado_lanzaEntityNotFound() {
            when(jiraIssueRepository.findByJiraKeyIn(anyList())).thenReturn(List.of());
            var request = new PlanificarIssueRequest(10L, List.of(itemBase));

            assertThatThrownBy(() -> service.planificar(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("RED-42");
        }

        @Test
        @DisplayName("issue ya tiene tarea → IllegalStateException")
        void planificar_issueConTareaExistente_lanzaIllegalState() {
            Tarea tareaExistente = new Tarea();
            tareaExistente.setId(1L);
            issue.setTarea(tareaExistente);

            var request = new PlanificarIssueRequest(10L, List.of(itemBase));

            assertThatThrownBy(() -> service.planificar(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RED-42")
                    .hasMessageContaining("ya tiene una tarea KAOS");
        }

        @Test
        @DisplayName("sin estimación en item usa plantilla activa")
        void planificar_sinEstimacion_usaPlantilla() {
            var itemSinEstimacion = new PlanificarAsignacionItem(
                    "RED-42", 5L, null, 2, null, null, null);

            var itemPlantilla = new PlanificarAsignacionItem(
                    null, null, BigDecimal.valueOf(4), null, null, null, null);
            when(plantillaService.aplicar(anyString(), any())).thenReturn(List.of(itemPlantilla));

            var request = new PlanificarIssueRequest(10L, List.of(itemSinEstimacion));

            var result = service.planificar(request);

            assertThat(result).hasSize(1);
            verify(plantillaService).aplicar("Story", BigDecimal.valueOf(8));
        }

        @Test
        @DisplayName("jerarquía padre-hijo: subtarea vincula tareaParent")
        void planificar_jerarquiaPadreHijo_vinculaTareaParent() {
            // Padre
            JiraIssue issuePadre = new JiraIssue();
            issuePadre.setId(101L);
            issuePadre.setJiraKey("RED-40");
            issuePadre.setSummary("Historia padre");
            issuePadre.setTipoJira("Story");
            issuePadre.setEstimacionHoras(BigDecimal.valueOf(16));
            issuePadre.setTarea(null);
            issuePadre.setParentKey(null);

            // Hijo
            JiraIssue issueHijo = new JiraIssue();
            issueHijo.setId(102L);
            issueHijo.setJiraKey("RED-41");
            issueHijo.setSummary("Subtarea");
            issueHijo.setTipoJira("Sub-task");
            issueHijo.setEstimacionHoras(BigDecimal.valueOf(4));
            issueHijo.setTarea(null);
            issueHijo.setParentKey("RED-40");

            when(jiraIssueRepository.findByJiraKeyIn(anyList()))
                    .thenReturn(List.of(issuePadre, issueHijo));

            Tarea tareaPadre = new Tarea();
            tareaPadre.setId(300L);
            tareaPadre.setJiraKey("RED-40");

            Tarea tareaHijo = new Tarea();
            tareaHijo.setId(301L);
            tareaHijo.setJiraKey("RED-41");

            when(tareaRepository.save(any()))
                    .thenReturn(tareaPadre)
                    .thenReturn(tareaHijo);
            when(tareaMapper.toResponse(any())).thenReturn(tareaResponseMock);

            var itemPadre = new PlanificarAsignacionItem(
                    "RED-40", 5L, BigDecimal.valueOf(16), 1, null, null, null);
            var itemHijo = new PlanificarAsignacionItem(
                    "RED-41", 5L, BigDecimal.valueOf(4), 1, null, null, null);

            var request = new PlanificarIssueRequest(10L, List.of(itemPadre, itemHijo));
            var result = service.planificar(request);

            assertThat(result).hasSize(2);
            // Verificamos que se guardaron 2 tareas
            verify(tareaRepository, org.mockito.Mockito.times(2)).save(any(Tarea.class));
        }
    }
}
