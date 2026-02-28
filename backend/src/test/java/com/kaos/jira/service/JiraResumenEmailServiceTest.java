package com.kaos.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import com.kaos.common.email.EmailService;
import com.kaos.jira.alert.entity.JiraAlertRule.Severidad;
import com.kaos.jira.alert.entity.JiraAlerta;
import com.kaos.jira.alert.repository.JiraAlertaRepository;
import com.kaos.jira.entity.JiraSyncStatus;
import com.kaos.jira.entity.JiraSyncStatus.EstadoSync;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.squad.entity.Squad;

/**
 * Unit tests para JiraResumenEmailService (TASK-124 / DT-46).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JiraResumenEmailService")
class JiraResumenEmailServiceTest {

    @Mock EmailService emailService;
    @Mock SprintRepository sprintRepository;
    @Mock JiraIssueRepository issueRepository;
    @Mock JiraAlertaRepository alertaRepository;

    @InjectMocks
    JiraResumenEmailService resumenEmailService;

    private Squad squad;
    private Sprint sprint;
    private JiraSyncStatus syncStatus;

    @BeforeEach
    void setUp() {
        squad = new Squad();
        squad.setId(1L);
        squad.setNombre("Squad Test");

        sprint = new Sprint();
        sprint.setId(10L);
        sprint.setNombre("Sprint 1");
        sprint.setSquad(squad);
        sprint.setFechaInicio(LocalDate.now().minusDays(7));
        sprint.setFechaFin(LocalDate.now().plusDays(7));
        sprint.setEstado(SprintEstado.ACTIVO);

        syncStatus = JiraSyncStatus.builder()
                .squad(squad)
                .estado(EstadoSync.IDLE)
                .build();
        syncStatus.completarSync(5, 12, 0, 0);

        // Destinatarios por defecto vacío (no se evalúa en unit test)
        ReflectionTestUtils.setField(resumenEmailService, "destinatarios", "test@kaos.com");
        ReflectionTestUtils.setField(resumenEmailService, "enviarSoloSiAlertas", false);
    }

    // ── enviarResumenSync ────────────────────────────────────────────────────

    @Nested
    @DisplayName("enviarResumenSync()")
    class EnviarResumenSync {

        @Test
        @DisplayName("debería omitir el envío cuando no hay sprint activo")
        void shouldSkip_whenNoActiveSprint() {
            when(sprintRepository.findBySquadIdAndEstado(1L, SprintEstado.ACTIVO)).thenReturn(List.of());

            resumenEmailService.enviarResumenSync(syncStatus, 1L);

            verify(emailService, never()).enviarHtml(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("debería enviar email cuando hay sprint activo y enviarSoloSiAlertas=false")
        void shouldSendEmail_whenActiveSprint() {
            when(sprintRepository.findBySquadIdAndEstado(1L, SprintEstado.ACTIVO)).thenReturn(List.of(sprint));
            when(issueRepository.findBySprintId(10L)).thenReturn(List.of());
            when(alertaRepository.findBySprintId(eq(10L), eq(false), any(Pageable.class)))
                    .thenReturn(Page.empty());

            resumenEmailService.enviarResumenSync(syncStatus, 1L);

            verify(emailService).enviarHtml(
                    eq("test@kaos.com"),
                    anyString(),
                    anyString()
            );
        }

        @Test
        @DisplayName("debería omitir envío cuando enviarSoloSiAlertas=true y no hay alertas CRITICO")
        void shouldSkip_whenSoloSiAlertasAndNoCritico() {
            ReflectionTestUtils.setField(resumenEmailService, "enviarSoloSiAlertas", true);

            when(sprintRepository.findBySquadIdAndEstado(1L, SprintEstado.ACTIVO)).thenReturn(List.of(sprint));
            when(issueRepository.findBySprintId(10L)).thenReturn(List.of());
            // Alertas solo de tipo AVISO, no CRITICO
            JiraAlerta aviso = new JiraAlerta();
            aviso.setSeveridad(Severidad.AVISO);
            aviso.setMensaje("Aviso de prueba");

            when(alertaRepository.findBySprintId(eq(10L), eq(false), any(Pageable.class)))
                    .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(aviso)));

            resumenEmailService.enviarResumenSync(syncStatus, 1L);

            verify(emailService, never()).enviarHtml(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("debería enviar cuando enviarSoloSiAlertas=true y hay alertas CRITICO")
        void shouldSend_whenSoloSiAlertasAndHasCritico() {
            ReflectionTestUtils.setField(resumenEmailService, "enviarSoloSiAlertas", true);

            when(sprintRepository.findBySquadIdAndEstado(1L, SprintEstado.ACTIVO)).thenReturn(List.of(sprint));
            when(issueRepository.findBySprintId(10L)).thenReturn(List.of());

            JiraAlerta critico = new JiraAlerta();
            critico.setSeveridad(Severidad.CRITICO);
            critico.setMensaje("Alerta crítica de prueba");

            when(alertaRepository.findBySprintId(eq(10L), eq(false), any(Pageable.class)))
                    .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(critico)));

            resumenEmailService.enviarResumenSync(syncStatus, 1L);

            verify(emailService).enviarHtml(anyString(), anyString(), anyString());
        }
    }

    // ── generarHtml ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generarHtml()")
    class GenerarHtml {

        @Test
        @DisplayName("debería generar HTML con sección de sprint y alertas")
        void shouldGenerateHtml_withSprintAndAlertSections() {
            JiraAlerta critico = new JiraAlerta();
            critico.setSeveridad(Severidad.CRITICO);
            critico.setMensaje("Error crítico en PROJ-1");

            String html = resumenEmailService.generarHtml(syncStatus, sprint, List.of(), List.of(critico));

            assertThat(html).isNotBlank();
            assertThat(html).contains("<!DOCTYPE html>");
            assertThat(html).contains("Sprint 1");
            assertThat(html).contains("Error crítico en PROJ-1");
            assertThat(html).contains("CRÍTICO");
        }

        @Test
        @DisplayName("debería mostrar mensaje sin alertas cuando la lista está vacía")
        void shouldShowNoAlertsMessage_whenAlertsEmpty() {
            String html = resumenEmailService.generarHtml(syncStatus, sprint, List.of(), List.of());

            assertThat(html).contains("Sin alertas pendientes");
        }
    }
}
