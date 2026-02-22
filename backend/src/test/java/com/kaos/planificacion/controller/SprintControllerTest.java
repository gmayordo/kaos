package com.kaos.planificacion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaos.planificacion.dto.SprintRequest;
import com.kaos.planificacion.dto.SprintResponse;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.exception.SolapamientoSprintException;
import com.kaos.planificacion.exception.SprintNoEnPlanificacionException;
import com.kaos.planificacion.service.SprintService;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests de integración para {@link SprintController}.
 * Cubre CA-12, CA-13, CA-14 (endpoints REST).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SprintController Integration Tests")
class SprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SprintService sprintService;

    private SprintResponse buildSprint(Long id, String estado) {
        return new SprintResponse(
                id,
                "Sprint " + id,
                1L,
                "Squad Backend",
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 13),
                "Objetivo del sprint",
                estado,
                BigDecimal.valueOf(80),
                5L, 2L, 3L,
                LocalDateTime.now()
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/sprints
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/sprints - Listar")
    class ListarTests {

        @Test
        @DisplayName("Listar sin filtros retorna 200 con page")
        void listar_sinFiltros_retorna200() throws Exception {
            var sprints = List.of(buildSprint(1L, "PLANIFICACION"), buildSprint(2L, "ACTIVO"));
            when(sprintService.listar(any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(sprints));

            mockMvc.perform(get("/api/v1/sprints"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].nombre").value("Sprint 1"))
                    .andExpect(jsonPath("$.content[1].estado").value("ACTIVO"));

            verify(sprintService).listar(any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Listar con filtro squadId retorna 200")
        void listar_conSquadId_retorna200() throws Exception {
            var sprints = List.of(buildSprint(1L, "PLANIFICACION"));
            when(sprintService.listar(eq(1L), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(sprints));

            mockMvc.perform(get("/api/v1/sprints?squadId=1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(sprintService).listar(eq(1L), any(), any(Pageable.class));
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/sprints/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/sprints/{id} - Obtener")
    class ObtenerTests {

        @Test
        @DisplayName("Obtener sprint existente retorna 200")
        void obtener_sprintExistente_retorna200() throws Exception {
            when(sprintService.obtener(1L)).thenReturn(buildSprint(1L, "PLANIFICACION"));

            mockMvc.perform(get("/api/v1/sprints/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Sprint 1"))
                    .andExpect(jsonPath("$.estado").value("PLANIFICACION"));
        }

        @Test
        @DisplayName("Obtener sprint inexistente retorna 404")
        void obtener_sprintInexistente_retorna404() throws Exception {
            when(sprintService.obtener(99L))
                    .thenThrow(new EntityNotFoundException("Sprint no encontrado con id: 99"));

            mockMvc.perform(get("/api/v1/sprints/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    // ══════════════════════════════════════════════════════════
    // POST /api/v1/sprints
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/sprints - Crear")
    class CrearTests {

        @Test
        @DisplayName("CA-12: Crear sprint válido retorna 201")
        void crear_sprintValido_retorna201() throws Exception {
            var request = new SprintRequest("Sprint 1", 1L, LocalDate.of(2026, 3, 2), "Objetivo");
            when(sprintService.crear(any())).thenReturn(buildSprint(1L, "PLANIFICACION"));

            mockMvc.perform(post("/api/v1/sprints")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.estado").value("PLANIFICACION"));

            verify(sprintService).crear(any());
        }

        @Test
        @DisplayName("Crear sprint sin nombre retorna 400")
        void crear_sinNombre_retorna400() throws Exception {
            var bodyInvalido = "{\"squadId\":1,\"fechaInicio\":\"2026-03-02\"}";

            mockMvc.perform(post("/api/v1/sprints")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyInvalido))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Crear sprint con solapamiento retorna 409")
        void crear_solapamiento_retorna409() throws Exception {
            var request = new SprintRequest("Sprint 1", 1L, LocalDate.of(2026, 3, 2), null);
            when(sprintService.crear(any()))
                    .thenThrow(new SolapamientoSprintException(1L, "Sprint solapado"));

            mockMvc.perform(post("/api/v1/sprints")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SOLAPAMIENTO_SPRINT"));
        }
    }

    // ══════════════════════════════════════════════════════════
    // PATCH /api/v1/sprints/{id}/estado
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/v1/sprints/{id}/estado - Cambiar Estado")
    class CambiarEstadoTests {

        @Test
        @DisplayName("CA-13: Activar sprint en PLANIFICACION retorna 200")
        void cambiarEstado_planificacionAActivo_retorna200() throws Exception {
            var sprintActivo = buildSprint(1L, "ACTIVO");
            when(sprintService.cambiarEstado(1L, SprintEstado.ACTIVO))
                    .thenReturn(List.of(sprintActivo));

            mockMvc.perform(patch("/api/v1/sprints/1/estado?estado=ACTIVO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("CA-13: Transición inválida retorna 409")
        void cambiarEstado_transicionInvalida_retorna409() throws Exception {
            when(sprintService.cambiarEstado(1L, SprintEstado.PLANIFICACION))
                    .thenThrow(new IllegalStateException("Transición de estado inválida: CERRADO → PLANIFICACION"));

            mockMvc.perform(patch("/api/v1/sprints/1/estado?estado=PLANIFICACION"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }
    }

    // ══════════════════════════════════════════════════════════
    // DELETE /api/v1/sprints/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/sprints/{id} - Eliminar")
    class EliminarTests {

        @Test
        @DisplayName("Eliminar sprint en PLANIFICACION retorna 204")
        void eliminar_enPlanificacion_retorna204() throws Exception {
            doNothing().when(sprintService).eliminar(1L);

            mockMvc.perform(delete("/api/v1/sprints/1"))
                    .andExpect(status().isNoContent());

            verify(sprintService).eliminar(1L);
        }

        @Test
        @DisplayName("Eliminar sprint ACTIVO retorna 422")
        void eliminar_enActivo_retorna422() throws Exception {
            doThrow(new SprintNoEnPlanificacionException(1L, "ACTIVO"))
                    .when(sprintService).eliminar(1L);

            mockMvc.perform(delete("/api/v1/sprints/1"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("SPRINT_ESTADO_INVALIDO"));
        }
    }
}
