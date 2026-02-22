package com.kaos.planificacion.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.kaos.planificacion.dto.DashboardSprintResponse;
import com.kaos.planificacion.dto.TimelineSprintResponse;
import com.kaos.planificacion.service.PlanificacionService;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests de integración para {@link PlanificacionController}.
 * Cubre CA-17 (dashboard métricas) y CA-14 (timeline).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PlanificacionController Integration Tests")
class PlanificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanificacionService planificacionService;

    private DashboardSprintResponse buildDashboard(Long sprintId) {
        return DashboardSprintResponse.builder()
                .sprintId(sprintId)
                .sprintNombre("Sprint " + sprintId)
                .estado("ACTIVO")
                .tareasTotal(15L)
                .tareasPendientes(8L)
                .tareasEnProgreso(4L)
                .tareasCompletadas(3L)
                .tareasBloqueadas(0L)
                .progresoEsperado(30.0)
                .progresoReal(20.0)
                .capacidadTotalHoras(160.0)
                .capacidadAsignadaHoras(120.0)
                .ocupacionPorcentaje(75.0)
                .bloqueosActivos(0L)
                .alertas(List.of())
                .fechaInicio(LocalDate.of(2026, 3, 2))
                .fechaFin(LocalDate.of(2026, 3, 13))
                .build();
    }

    private TimelineSprintResponse buildTimeline(Long sprintId) {
        return TimelineSprintResponse.builder()
                .sprintId(sprintId)
                .sprintNombre("Sprint " + sprintId)
                .fechaInicio(LocalDate.of(2026, 3, 2))
                .fechaFin(LocalDate.of(2026, 3, 13))
                .personas(List.of())
                .build();
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/planificacion/{sprintId}/dashboard
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /{sprintId}/dashboard - Dashboard")
    class DashboardTests {

        @Test
        @DisplayName("CA-17: Obtener dashboard de sprint activo retorna 200 con métricas")
        void obtenerDashboard_sprintActivo_retorna200() throws Exception {
            when(planificacionService.obtenerDashboard(1L)).thenReturn(buildDashboard(1L));

            mockMvc.perform(get("/api/v1/planificacion/1/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sprintId").value(1))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"))
                    .andExpect(jsonPath("$.tareasTotal").value(15))
                    .andExpect(jsonPath("$.ocupacionPorcentaje").value(75.0))
                    .andExpect(jsonPath("$.progresoReal").value(20.0));
        }

        @Test
        @DisplayName("CA-17: Dashboard incluye alertas cuando hay sobreasignación")
        void obtenerDashboard_conAlertas_retornaAlertas() throws Exception {
            var dashboardConAlerta = DashboardSprintResponse.builder()
                    .sprintId(2L).sprintNombre("Sprint 2").estado("ACTIVO")
                    .tareasTotal(20L).tareasPendientes(5L).tareasEnProgreso(10L)
                    .tareasCompletadas(5L).tareasBloqueadas(2L)
                    .progresoEsperado(50.0).progresoReal(25.0)
                    .capacidadTotalHoras(160.0).capacidadAsignadaHoras(180.0)
                    .ocupacionPorcentaje(112.5)
                    .bloqueosActivos(2L)
                    .alertas(List.of("Sobreasignación en persona 1 día 5", "Bloqueos activos: 2"))
                    .fechaInicio(LocalDate.of(2026, 3, 2))
                    .fechaFin(LocalDate.of(2026, 3, 13))
                    .build();
            when(planificacionService.obtenerDashboard(2L)).thenReturn(dashboardConAlerta);

            mockMvc.perform(get("/api/v1/planificacion/2/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alertas").isArray())
                    .andExpect(jsonPath("$.alertas.length()").value(2))
                    .andExpect(jsonPath("$.ocupacionPorcentaje").value(112.5));
        }

        @Test
        @DisplayName("Dashboard de sprint inexistente retorna 404")
        void obtenerDashboard_sprintInexistente_retorna404() throws Exception {
            when(planificacionService.obtenerDashboard(99L))
                    .thenThrow(new EntityNotFoundException("Sprint no encontrado con id: 99"));

            mockMvc.perform(get("/api/v1/planificacion/99/dashboard"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/planificacion/{sprintId}/timeline
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /{sprintId}/timeline - Timeline")
    class TimelineTests {

        @Test
        @DisplayName("CA-14: Obtener timeline de sprint retorna 200 con personas y días")
        void obtenerTimeline_sprintValido_retorna200() throws Exception {
            var timeline = TimelineSprintResponse.builder()
                    .sprintId(1L).sprintNombre("Sprint 1")
                    .fechaInicio(LocalDate.of(2026, 3, 2))
                    .fechaFin(LocalDate.of(2026, 3, 13))
                    .personas(List.of(
                            TimelineSprintResponse.PersonaEnLinea.builder()
                                    .personaId(1L).personaNombre("Juan Pérez")
                                    .dias(List.of(
                                            TimelineSprintResponse.DiaConTareas.builder()
                                                    .dia(1).horasDisponibles(8.0).tareas(List.of())
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();
            when(planificacionService.obtenerTimeline(1L)).thenReturn(timeline);

            mockMvc.perform(get("/api/v1/planificacion/1/timeline"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sprintId").value(1))
                    .andExpect(jsonPath("$.personas").isArray())
                    .andExpect(jsonPath("$.personas.length()").value(1))
                    .andExpect(jsonPath("$.personas[0].personaNombre").value("Juan Pérez"));
        }

        @Test
        @DisplayName("Timeline de sprint inexistente retorna 404")
        void obtenerTimeline_sprintInexistente_retorna404() throws Exception {
            when(planificacionService.obtenerTimeline(99L))
                    .thenThrow(new EntityNotFoundException("Sprint no encontrado con id: 99"));

            mockMvc.perform(get("/api/v1/planificacion/99/timeline"))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/planificacion/{sprintId}/timeline/export
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /{sprintId}/timeline/export - Exportar Excel")
    class ExportTests {

        @Test
        @DisplayName("Exportar timeline retorna 200 con Content-Disposition")
        void exportarTimeline_sprintValido_retorna200ConExcel() throws Exception {
            byte[] excelBytes = new byte[]{0x50, 0x4B, 0x03, 0x04}; // ZIP header (xlsx)
            when(planificacionService.exportarTimelineExcel(1L)).thenReturn(excelBytes);

            mockMvc.perform(get("/api/v1/planificacion/1/timeline/export"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            org.hamcrest.Matchers.containsString("timeline-sprint-1.xlsx")))
                    .andExpect(content().contentType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }
    }
}
