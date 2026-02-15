package com.kaos.calendario.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaos.calendario.dto.AusenciaRequest;
import com.kaos.calendario.dto.AusenciaResponse;
import com.kaos.calendario.entity.TipoAusencia;
import com.kaos.calendario.service.AusenciaService;

/**
 * Tests de integración para {@link AusenciaController}.
 * Valida endpoints REST con @SpringBootTest y MockMvc.
 * Incluye tests específicos para fechaFin nullable (CA-11).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AusenciaController Integration Tests")
class AusenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AusenciaService service;

    private AusenciaResponse createMockResponse(Long id, Long personaId, LocalDate inicio, LocalDate fin, TipoAusencia tipo) {
        return new AusenciaResponse(
                id,
                personaId,
                "Juan Pérez",
                inicio,
                fin,
                tipo,
                "Motivo test",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/ausencias - Listar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/ausencias - Listar")
    class ListarTests {

        @Test
        @DisplayName("GET sin filtros retorna 200 con lista completa")
        void listar_sinFiltros_retorna200() throws Exception {
            // given
            AusenciaResponse a1 = createMockResponse(1L, 1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), TipoAusencia.BAJA_MEDICA);
            AusenciaResponse a2 = createMockResponse(2L, 2L, LocalDate.of(2026, 4, 1), null, TipoAusencia.EMERGENCIA); // Indefinida
            when(service.listar(null, null)).thenReturn(List.of(a1, a2));

            // when & then
            mockMvc.perform(get("/api/v1/ausencias"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].fechaFin").value("2026-03-10"))
                    .andExpect(jsonPath("$[1].fechaFin").isEmpty()); // null en JSON

            verify(service).listar(null, null);
        }

        @Test
        @DisplayName("GET con filtro personaId retorna 200 filtrado")
        void listar_conFiltroPersonaId_retorna200() throws Exception {
            // given
            AusenciaResponse a1 = createMockResponse(1L, 1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), TipoAusencia.BAJA_MEDICA);
            when(service.listar(1L, null)).thenReturn(List.of(a1));

            // when & then
            mockMvc.perform(get("/api/v1/ausencias")
                            .param("personaId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].personaId").value(1));

            verify(service).listar(1L, null);
        }

        @Test
        @DisplayName("GET con filtro squadId retorna 200 filtrado")
        void listar_conFiltroSquadId_retorna200() throws Exception {
            // given
            AusenciaResponse a1 = createMockResponse(1L, 1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), TipoAusencia.BAJA_MEDICA);
            when(service.listar(null, 1L)).thenReturn(List.of(a1));

            // when & then
            mockMvc.perform(get("/api/v1/ausencias")
                            .param("squadId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(service).listar(null, 1L);
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/ausencias/{id} - Obtener
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/ausencias/{id} - Obtener")
    class ObtenerTests {

        @Test
        @DisplayName("GET por ID existente retorna 200")
        void obtener_idExistente_retorna200() throws Exception {
            // given
            AusenciaResponse response = createMockResponse(1L, 1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), TipoAusencia.BAJA_MEDICA);
            when(service.obtener(1L)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/ausencias/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.tipo").value("BAJA_MEDICA"));

            verify(service).obtener(1L);
        }

        @Test
        @DisplayName("GET por ID inexistente retorna 404")
        void obtener_idInexistente_retorna404() throws Exception {
            // given
            when(service.obtener(999L)).thenThrow(new IllegalArgumentException("Ausencia no encontrada: 999"));

            // when & then
            mockMvc.perform(get("/api/v1/ausencias/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // POST /api/v1/ausencias - Crear (CA-11: fechaFin nullable)
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/ausencias - Crear")
    class CrearTests {

        @Test
        @DisplayName("POST con fechaFin definida retorna 201")
        void crear_fechaFinDefinida_retorna201() throws Exception {
            // given
            AusenciaRequest request = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 10), // definida
                    TipoAusencia.BAJA_MEDICA,
                    "Gripe"
            );

            AusenciaResponse response = createMockResponse(1L, 1L, request.fechaInicio(), request.fechaFin(), request.tipo());
            when(service.crear(any(AusenciaRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/ausencias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fechaFin").value("2026-03-10"));

            verify(service).crear(any(AusenciaRequest.class));
        }

        @Test
        @DisplayName("POST con fechaFin null (indefinida) retorna 201 (CA-11)")
        void crear_fechaFinNull_retorna201() throws Exception {
            // given
            String requestJson = """
                    {
                        "personaId": 1,
                        "fechaInicio": "2026-03-01",
                        "fechaFin": null,
                        "tipo": "BAJA_MEDICA",
                        "motivo": "Baja indefinida"
                    }
                    """;

            AusenciaResponse response = createMockResponse(1L, 1L, LocalDate.of(2026, 3, 1), null, TipoAusencia.BAJA_MEDICA);
            when(service.crear(any(AusenciaRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/ausencias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fechaFin").isEmpty()); // null en respuesta

            verify(service).crear(any(AusenciaRequest.class));
        }

        @Test
        @DisplayName("POST con fechaFin < fechaInicio retorna 400")
        void crear_fechaFinAnterior_retorna400() throws Exception {
            // given
            AusenciaRequest request = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 10),
                    LocalDate.of(2026, 3, 1), // anterior
                    TipoAusencia.BAJA_MEDICA,
                    "Test"
            );

            when(service.crear(any())).thenThrow(new IllegalArgumentException("fechaFin debe ser posterior o igual a fechaInicio"));

            // when & then
            mockMvc.perform(post("/api/v1/ausencias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST con persona inexistente retorna 404")
        void crear_personaInexistente_retorna404() throws Exception {
            // given
            AusenciaRequest request = new AusenciaRequest(
                    999L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 10),
                    TipoAusencia.BAJA_MEDICA,
                    "Test"
            );

            when(service.crear(any())).thenThrow(new IllegalArgumentException("Persona no encontrada: 999"));

            // when & then
            mockMvc.perform(post("/api/v1/ausencias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // PUT /api/v1/ausencias/{id} - Actualizar (CA-11)
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/ausencias/{id} - Actualizar")
    class ActualizarTests {

        @Test
        @DisplayName("PUT con datos válidos retorna 200")
        void actualizar_datosValidos_retorna200() throws Exception {
            // given
            AusenciaRequest request = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 15), // extendida
                    TipoAusencia.BAJA_MEDICA,
                    "Extendida por complicaciones"
            );

            AusenciaResponse response = createMockResponse(1L, 1L, request.fechaInicio(), request.fechaFin(), request.tipo());
            when(service.actualizar(eq(1L), any(AusenciaRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/ausencias/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fechaFin").value("2026-03-15"));

            verify(service).actualizar(eq(1L), any(AusenciaRequest.class));
        }

        @Test
        @DisplayName("PUT agregar fechaFin (null → fecha) retorna 200 (CA-11)")
        void actualizar_agregarFechaFin_retorna200() throws Exception {
            // given
            AusenciaRequest request = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31), // antes null, ahora con fecha
                    TipoAusencia.BAJA_MEDICA,
                    "Fecha de alta definida"
            );

            AusenciaResponse response = createMockResponse(1L, 1L, request.fechaInicio(), request.fechaFin(), request.tipo());
            when(service.actualizar(eq(1L), any(AusenciaRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/ausencias/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fechaFin").value("2026-03-31"));
        }

        @Test
        @DisplayName("PUT quitar fechaFin (fecha → null) retorna 200 (CA-11)")
        void actualizar_quitarFechaFin_retorna200() throws Exception {
            // given
            String requestJson = """
                    {
                        "personaId": 1,
                        "fechaInicio": "2026-03-01",
                        "fechaFin": null,
                        "tipo": "BAJA_MEDICA",
                        "motivo": "Baja indefinida nuevamente"
                    }
                    """;

            AusenciaResponse response = createMockResponse(1L, 1L, LocalDate.of(2026, 3, 1), null, TipoAusencia.BAJA_MEDICA);
            when(service.actualizar(eq(1L), any(AusenciaRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/ausencias/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fechaFin").isEmpty()); // null
        }

        @Test
        @DisplayName("PUT con ID inexistente retorna 404")
        void actualizar_idInexistente_retorna404() throws Exception {
            // given
            AusenciaRequest request = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 10),
                    TipoAusencia.BAJA_MEDICA,
                    "Test"
            );

            when(service.actualizar(eq(999L), any())).thenThrow(new IllegalArgumentException("Ausencia no encontrada: 999"));

            // when & then
            mockMvc.perform(put("/api/v1/ausencias/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // DELETE /api/v1/ausencias/{id} - Eliminar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/ausencias/{id} - Eliminar")
    class EliminarTests {

        @Test
        @DisplayName("DELETE con ID existente retorna 204")
        void eliminar_idExistente_retorna204() throws Exception {
            // given
            doNothing().when(service).eliminar(1L);

            // when & then
            mockMvc.perform(delete("/api/v1/ausencias/1"))
                    .andExpect(status().isNoContent());

            verify(service).eliminar(1L);
        }

        @Test
        @DisplayName("DELETE con ID inexistente retorna 404")
        void eliminar_idInexistente_retorna404() throws Exception {
            // given
            doThrow(new IllegalArgumentException("Ausencia no encontrada: 999"))
                    .when(service).eliminar(999L);

            // when & then
            mockMvc.perform(delete("/api/v1/ausencias/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
