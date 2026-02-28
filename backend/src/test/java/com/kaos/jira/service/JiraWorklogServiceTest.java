package com.kaos.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
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
import com.kaos.jira.dto.WorklogDiaResponse;
import com.kaos.jira.dto.WorklogRequest;
import com.kaos.jira.dto.WorklogSemanaResponse;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.entity.JiraSyncQueue.EstadoOperacion;
import com.kaos.jira.entity.JiraSyncQueue.TipoOperacion;
import com.kaos.jira.entity.JiraWorklog;
import com.kaos.jira.entity.WorklogOrigen;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.jira.repository.JiraSyncQueueRepository;
import com.kaos.jira.repository.JiraWorklogRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests para JiraWorklogService (TASK-124).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JiraWorklogService")
class JiraWorklogServiceTest {

    @Mock JiraWorklogRepository worklogRepository;
    @Mock JiraIssueRepository issueRepository;
    @Mock PersonaRepository personaRepository;
    @Mock JiraSyncQueueRepository syncQueueRepository;

    @InjectMocks
    JiraWorklogService worklogService;

    private Persona persona;
    private JiraIssue issue;

    @BeforeEach
    void setUp() {
        persona = new Persona();
        persona.setId(1L);
        persona.setNombre("Test User");
        // perfilHorario = null → calcularCapacidadDiaria devuelve 8.00h

        Squad squad = new Squad();
        squad.setId(1L);
        squad.setNombre("Squad A");

        issue = new JiraIssue();
        issue.setId(10L);
        issue.setJiraKey("PROJ-42");
        issue.setSummary("Tarea de prueba");
        issue.setSquad(squad);
    }

    // ── getMiDia ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMiDia()")
    class GetMiDia {

        @Test
        @DisplayName("debería devolver día con lista vacía cuando no hay worklogs")
        void shouldReturnEmptyDay_whenNoWorklogs() {
            LocalDate hoy = LocalDate.now();
            when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));
            when(worklogRepository.findByPersonaIdAndFecha(1L, hoy)).thenReturn(List.of());

            WorklogDiaResponse result = worklogService.getMiDia(1L, hoy);

            assertThat(result).isNotNull();
            assertThat(result.worklogs()).isEmpty();
            assertThat(result.horasImputadas()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("debería usar la fecha de hoy cuando se pasa null")
        void shouldUseToday_whenFechaIsNull() {
            LocalDate hoy = LocalDate.now();
            when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));
            when(worklogRepository.findByPersonaIdAndFecha(eq(1L), any(LocalDate.class))).thenReturn(List.of());

            WorklogDiaResponse result = worklogService.getMiDia(1L, null);

            assertThat(result.fecha()).isEqualTo(hoy);
        }

        @Test
        @DisplayName("debería lanzar EntityNotFoundException cuando la persona no existe")
        void shouldThrow_whenPersonaNotFound() {
            when(personaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> worklogService.getMiDia(99L, LocalDate.now()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── getMiSemana ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMiSemana()")
    class GetMiSemana {

        @Test
        @DisplayName("debería devolver grid de 5 días aunque no haya imputaciones")
        void shouldReturnEmptyGrid_whenNoWorklogs() {
            LocalDate lunes = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            LocalDate viernes = lunes.plusDays(4);

            when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));
            when(worklogRepository.findByPersonaAndFechaRange(eq(1L), any(), any())).thenReturn(List.of());

            WorklogSemanaResponse result = worklogService.getMiSemana(1L, null);

            assertThat(result).isNotNull();
            assertThat(result.filas()).isEmpty();
            assertThat(result.semanaInicio()).isEqualTo(lunes);
            assertThat(result.semanaFin()).isEqualTo(viernes);
        }
    }

    // ── registrar ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("debería guardar worklog y encolarlo cuando los datos son válidos")
        void shouldSaveAndEnqueue_whenValidRequest() {
            LocalDate fecha = LocalDate.now();
            WorklogRequest request = new WorklogRequest("PROJ-42", 1L, fecha.toString(), new BigDecimal("3.0"), "Desarrollo");

            when(issueRepository.findByJiraKey("PROJ-42")).thenReturn(Optional.of(issue));
            when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));
            when(worklogRepository.sumHorasByPersonaAndFecha(1L, fecha)).thenReturn(BigDecimal.ZERO);
            when(worklogRepository.save(any(JiraWorklog.class))).thenAnswer(inv -> {
                JiraWorklog wl = inv.getArgument(0);
                wl.setId(55L);
                return wl;
            });
            when(syncQueueRepository.findBySquadIdAndTipoOperacionAndEstado(1L, TipoOperacion.POST_WORKLOG, EstadoOperacion.PENDIENTE))
                    .thenReturn(Optional.empty());
            when(syncQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = worklogService.registrar(request);

            assertThat(result).isNotNull();
            assertThat(result.jiraKey()).isEqualTo("PROJ-42");
            verify(worklogRepository).save(any(JiraWorklog.class));
            verify(syncQueueRepository).save(any());
        }

        @Test
        @DisplayName("debería lanzar EntityNotFoundException cuando la issue no existe")
        void shouldThrow_whenIssueNotFound() {
            WorklogRequest request = new WorklogRequest("UNKNOWN-1", 1L,
                    LocalDate.now().toString(), new BigDecimal("2.0"), null);
            when(issueRepository.findByJiraKey("UNKNOWN-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> worklogService.registrar(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("UNKNOWN-1");
        }

        @Test
        @DisplayName("debería lanzar IllegalArgumentException cuando supera capacidad diaria")
        void shouldThrow_whenCapacityExceeded() {
            LocalDate fecha = LocalDate.now();
            WorklogRequest request = new WorklogRequest("PROJ-42", 1L, fecha.toString(), new BigDecimal("9.0"), null);

            when(issueRepository.findByJiraKey("PROJ-42")).thenReturn(Optional.of(issue));
            when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));
            // Ya imputadas 7 horas; añadir 9 supera las 8h de capacidad
            when(worklogRepository.sumHorasByPersonaAndFecha(1L, fecha)).thenReturn(new BigDecimal("7.0"));

            assertThatThrownBy(() -> worklogService.registrar(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("capacidad diaria");
        }
    }

    // ── eliminar ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("eliminar()")
    class Eliminar {

        @Test
        @DisplayName("debería eliminar worklog no sincronizado")
        void shouldDelete_whenNotSynchronized() {
            JiraWorklog worklog = buildWorklog(false);
            when(worklogRepository.findById(1L)).thenReturn(Optional.of(worklog));

            worklogService.eliminar(1L);

            verify(worklogRepository).delete(worklog);
        }

        @Test
        @DisplayName("debería lanzar IllegalStateException cuando el worklog ya fue sincronizado")
        void shouldThrow_whenAlreadySynchronized() {
            JiraWorklog worklog = buildWorklog(true);
            when(worklogRepository.findById(1L)).thenReturn(Optional.of(worklog));

            assertThatThrownBy(() -> worklogService.eliminar(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sincronizado");
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private JiraWorklog buildWorklog(boolean sincronizado) {
        JiraWorklog wl = new JiraWorklog();
        wl.setId(1L);
        wl.setJiraIssue(issue);
        wl.setPersona(persona);
        wl.setFecha(LocalDate.now());
        wl.setHoras(new BigDecimal("4.0"));
        wl.setOrigen(WorklogOrigen.KAOS);
        wl.setSincronizado(sincronizado);
        return wl;
    }
}
