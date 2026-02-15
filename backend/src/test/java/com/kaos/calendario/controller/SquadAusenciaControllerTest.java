package com.kaos.calendario.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import org.springframework.test.web.servlet.MockMvc;
import com.kaos.calendario.dto.AusenciaResponse;
import com.kaos.calendario.entity.TipoAusencia;
import com.kaos.calendario.service.AusenciaService;

/**
 * Tests de integración para {@link SquadAusenciaController}.
 * Valida endpoints REST con @SpringBootTest y MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SquadAusenciaController Integration Tests")
class SquadAusenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AusenciaService service;

    private AusenciaResponse createMockResponse(Long id, Long personaId, String personaNombre,
            LocalDate inicio, LocalDate fin, TipoAusencia tipo) {
        return new AusenciaResponse(
                id,
                personaId,
                personaNombre,
                inicio,
                fin,
                tipo,
                "Comentario de prueba",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/squads/{squadId}/ausencias - Listar por squad
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/squads/{squadId}/ausencias - Listar por squad")
    class ListarPorSquadTests {

        @Test
        @DisplayName("GET sin filtros retorna 200 con lista de ausencias del squad")
        void listar_sinFiltros_retorna200() throws Exception {
            // given
            Long squadId = 1L;
            AusenciaResponse a1 = createMockResponse(1L, 1L, "Juan Pérez",
                    LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 3), TipoAusencia.BAJA_MEDICA);
            AusenciaResponse a2 = createMockResponse(2L, 2L, "María García",
                    LocalDate.of(2024, 7, 15), LocalDate.of(2024, 7, 16), TipoAusencia.BAJA_MEDICA);
            when(service.listarPorSquad(squadId, null, null)).thenReturn(List.of(a1, a2));

            // when & then
            mockMvc.perform(get("/api/v1/squads/{squadId}/ausencias", squadId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].personaNombre").value("Juan Pérez"))
                    .andExpect(jsonPath("$[1].personaNombre").value("María García"));

            verify(service).listarPorSquad(eq(squadId), eq(null), eq(null));
        }

        @Test
        @DisplayName("GET con filtro fechaInicio retorna 200 filtrado")
        void listar_conFiltroFechaInicio_retorna200() throws Exception {
            // given
            Long squadId = 1L;
            LocalDate fechaInicio = LocalDate.of(2024, 7, 1);
            AusenciaResponse a1 = createMockResponse(1L, 1L, "Juan Pérez",
                    LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 3), TipoAusencia.BAJA_MEDICA);
            when(service.listarPorSquad(squadId, fechaInicio, null)).thenReturn(List.of(a1));

            // when & then
            mockMvc.perform(get("/api/v1/squads/{squadId}/ausencias", squadId)
                    .param("fechaInicio", fechaInicio.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].personaNombre").value("Juan Pérez"));

            verify(service).listarPorSquad(eq(squadId), eq(fechaInicio), eq(null));
        }

        @Test
        @DisplayName("GET con filtro fechaFin retorna 200 filtrado")
        void listar_conFiltroFechaFin_retorna200() throws Exception {
            // given
            Long squadId = 1L;
            LocalDate fechaFin = LocalDate.of(2024, 7, 31);
            AusenciaResponse a1 = createMockResponse(1L, 1L, "Juan Pérez",
                    LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 3), TipoAusencia.BAJA_MEDICA);
            when(service.listarPorSquad(squadId, null, fechaFin)).thenReturn(List.of(a1));

            // when & then
            mockMvc.perform(get("/api/v1/squads/{squadId}/ausencias", squadId)
                    .param("fechaFin", fechaFin.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].personaNombre").value("Juan Pérez"));

            verify(service).listarPorSquad(eq(squadId), eq(null), eq(fechaFin));
        }

        @Test
        @DisplayName("GET con ambos filtros retorna 200 filtrado")
        void listar_conAmbosFiltros_retorna200() throws Exception {
            // given
            Long squadId = 1L;
            LocalDate fechaInicio = LocalDate.of(2024, 7, 1);
            LocalDate fechaFin = LocalDate.of(2024, 7, 31);
            AusenciaResponse a1 = createMockResponse(1L, 1L, "Juan Pérez",
                    LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 3), TipoAusencia.BAJA_MEDICA);
            when(service.listarPorSquad(squadId, fechaInicio, fechaFin)).thenReturn(List.of(a1));

            // when & then
            mockMvc.perform(get("/api/v1/squads/{squadId}/ausencias", squadId)
                    .param("fechaInicio", fechaInicio.toString())
                    .param("fechaFin", fechaFin.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].personaNombre").value("Juan Pérez"));

            verify(service).listarPorSquad(eq(squadId), eq(fechaInicio), eq(fechaFin));
        }

        @Test
        @DisplayName("GET retorna lista vacía cuando no hay ausencias")
        void listar_sinAusencias_retornaListaVacia() throws Exception {
            // given
            Long squadId = 1L;
            when(service.listarPorSquad(squadId, null, null)).thenReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/squads/{squadId}/ausencias", squadId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(service).listarPorSquad(eq(squadId), eq(null), eq(null));
        }
    }
}