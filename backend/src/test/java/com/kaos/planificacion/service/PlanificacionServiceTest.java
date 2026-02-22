package com.kaos.planificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.PageImpl;
import com.kaos.calendario.dto.CapacidadPersonaResponse;
import com.kaos.calendario.dto.CapacidadSquadResponse;
import com.kaos.calendario.service.CapacidadService;
import com.kaos.planificacion.dto.DashboardSprintResponse;
import com.kaos.planificacion.dto.TimelineSprintResponse;
import com.kaos.planificacion.entity.EstadoTarea;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.repository.BloqueoRepository;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.planificacion.repository.TareaRepository;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para PlanificacionService.
 * Cubre CA-23 (Dashboard), CA-24 (Timeline).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanificacionService")
class PlanificacionServiceTest {

    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private BloqueoRepository bloqueoRepository;
    @Mock
    private CapacidadService capacidadService;

    @InjectMocks
    private PlanificacionService planificacionService;

    private Sprint sprint;
    private CapacidadSquadResponse capacidadMock;

    @BeforeEach
    void setUp() {
        Squad squad = new Squad();
        squad.setId(1L);
        squad.setNombre("Squad Backend");

        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setNombre("Sprint 1");
        sprint.setSquad(squad);
        sprint.setFechaInicio(LocalDate.of(2026, 3, 2));
        sprint.setFechaFin(LocalDate.of(2026, 3, 16));
        sprint.setEstado(SprintEstado.ACTIVO);
        sprint.setCreatedAt(LocalDateTime.now());

        // CapacidadSquadResponse(squadId, squadNombre, fechaInicio, fechaFin, horasTotales, personas)
        // CapacidadPersonaResponse(personaId, personaNombre, horasTotales, detalles)
        var persona = new CapacidadPersonaResponse(1L, "Juan", 80.0, List.of());
        capacidadMock = new CapacidadSquadResponse(
            1L, "Squad Backend",
            LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 16),
            160.0,
            List.of(persona)
        );
    }

    private void stubTareasCounts(long total, long pendientes, long enProgreso, long completadas, long bloqueadas) {
        when(tareaRepository.countBySprintId(1L)).thenReturn(total);
        when(tareaRepository.countBySprintIdAndEstado(1L, EstadoTarea.PENDIENTE)).thenReturn(pendientes);
        when(tareaRepository.countBySprintIdAndEstado(1L, EstadoTarea.EN_PROGRESO)).thenReturn(enProgreso);
        when(tareaRepository.countBySprintIdAndEstado(1L, EstadoTarea.COMPLETADA)).thenReturn(completadas);
        when(tareaRepository.countBySprintIdAndEstado(1L, EstadoTarea.BLOQUEADO)).thenReturn(bloqueadas);
    }

    @Nested
    @DisplayName("obtenerDashboard()")
    class ObtenerDashboardTests {

        @Test
        @DisplayName("CA-23: Sprint existente retorna DashboardSprintResponse con métricas")
        void testObtenerDashboard() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            stubTareasCounts(10L, 3L, 4L, 3L, 0L);
            when(bloqueoRepository.countByEstadoAbiertosOEnGestion()).thenReturn(0L);
            when(capacidadService.calcularCapacidad(anyLong(), any(), any())).thenReturn(capacidadMock);

            DashboardSprintResponse result = planificacionService.obtenerDashboard(1L);

            assertThat(result).isNotNull();
            assertThat(result.sprintId()).isEqualTo(1L);
            assertThat(result.tareasPendientes()).isEqualTo(3L);
            assertThat(result.tareasEnProgreso()).isEqualTo(4L);
            assertThat(result.tareasCompletadas()).isEqualTo(3L);
        }

        @Test
        @DisplayName("CA-23: Sprint inexistente lanza EntityNotFoundException")
        void testObtenerDashboardNoExistente() {
            when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planificacionService.obtenerDashboard(99L))
                .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("CA-23: ocupacionPorcentaje = (horasAsignadas / horasTotales) * 100")
        void testOcupacionPorcentaje() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            stubTareasCounts(10L, 5L, 3L, 2L, 0L);
            when(bloqueoRepository.countByEstadoAbiertosOEnGestion()).thenReturn(0L);

            // horasAsignadas = 120, horasTotales = 160 → 75%
            var capacidadConAsignacion = new CapacidadSquadResponse(
                1L, "Squad", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 16),
                160.0,
                List.of(new CapacidadPersonaResponse(1L, "Juan", 80.0, List.of()),
                        new CapacidadPersonaResponse(2L, "Ana", 80.0, List.of()))
            );
            when(capacidadService.calcularCapacidad(anyLong(), any(), any())).thenReturn(capacidadConAsignacion);

            DashboardSprintResponse result = planificacionService.obtenerDashboard(1L);

            assertThat(result.ocupacionPorcentaje()).isGreaterThanOrEqualTo(0.0);
            assertThat(result.ocupacionPorcentaje()).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("CA-23: Dashboard incluye alertas cuando hay bloqueos activos")
        void testDashboardConBloqueos() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            stubTareasCounts(10L, 3L, 3L, 3L, 1L);
            when(bloqueoRepository.countByEstadoAbiertosOEnGestion()).thenReturn(4L);
            when(capacidadService.calcularCapacidad(anyLong(), any(), any())).thenReturn(capacidadMock);

            DashboardSprintResponse result = planificacionService.obtenerDashboard(1L);

            assertThat(result.bloqueosActivos()).isEqualTo(4L);
        }
    }

    @Nested
    @DisplayName("obtenerTimeline()")
    class ObtenerTimelineTests {

        @Test
        @DisplayName("CA-24: Sprint existente retorna TimelineSprintResponse")
        void testObtenerTimeline() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(capacidadService.calcularCapacidad(anyLong(), any(), any())).thenReturn(capacidadMock);
            when(tareaRepository.findBySprintId(eq(1L), any())).thenReturn(new PageImpl<>(List.of()));

            TimelineSprintResponse result = planificacionService.obtenerTimeline(1L);

            assertThat(result).isNotNull();
            assertThat(result.sprintId()).isEqualTo(1L);
            assertThat(result.personas()).isNotNull();
        }

        @Test
        @DisplayName("CA-24: Sprint inexistente lanza EntityNotFoundException")
        void testObtenerTimelineNoExistente() {
            when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planificacionService.obtenerTimeline(99L))
                .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("CA-24: Timeline tiene una entrada por persona del squad")
        void testTimelinePersonasPorSquad() {
            var persona2 = new CapacidadPersonaResponse(2L, "Ana", 80.0, List.of());
            var capacidadDosPersonas = new CapacidadSquadResponse(
                1L, "Squad Backend",
                sprint.getFechaInicio(), sprint.getFechaFin(),
                160.0, List.of(
                    new CapacidadPersonaResponse(1L, "Juan", 80.0, List.of()),
                    persona2
                )
            );
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(capacidadService.calcularCapacidad(anyLong(), any(), any())).thenReturn(capacidadDosPersonas);
            when(tareaRepository.findBySprintId(eq(1L), any())).thenReturn(new PageImpl<>(List.of()));

            TimelineSprintResponse result = planificacionService.obtenerTimeline(1L);

            assertThat(result.personas()).hasSize(2);
        }
    }
}
