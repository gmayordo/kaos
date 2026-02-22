package com.kaos.calendario.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import com.kaos.calendario.dto.VacacionRequest;
import com.kaos.calendario.dto.VacacionResponse;
import com.kaos.calendario.entity.EstadoVacacion;
import com.kaos.calendario.entity.TipoVacacion;
import com.kaos.calendario.service.ExcelImportService;
import com.kaos.calendario.service.VacacionService;

/**
 * Tests de integración para {@link VacacionController}.
 * Valida endpoints REST con @SpringBootTest y MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("VacacionController Integration Tests")
class VacacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VacacionService service;

    @MockBean
    private ExcelImportService excelImportService;

    private VacacionResponse createMockResponse(Long id, Long personaId, LocalDate inicio, LocalDate fin, Integer dias) {
        return new VacacionResponse(
                id,
                personaId,
                "Juan Pérez",
                inicio,
                fin,
                dias,
                TipoVacacion.VACACIONES,
                EstadoVacacion.REGISTRADA,
                null, // observaciones
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/vacaciones - Listar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/vacaciones - Listar")
    class ListarTests {

        @Test
        @DisplayName("GET sin filtros retorna 200 con lista completa")
        void listar_sinFiltros_retorna200() throws Exception {
            // given
            VacacionResponse v1 = createMockResponse(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), 8);
            VacacionResponse v2 = createMockResponse(2L, 2L, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 31), 13);
            when(service.listar(null, null, null, null)).thenReturn(List.of(v1, v2));

            // when & then
            mockMvc.perform(get("/api/v1/vacaciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].diasLaborables").value(8))
                    .andExpect(jsonPath("$[1].diasLaborables").value(13));

            verify(service).listar(null, null, null, null);
        }

        @Test
        @DisplayName("GET con filtro personaId retorna 200 filtrado")
        void listar_conFiltroPersonaId_retorna200() throws Exception {
            // given
            VacacionResponse v1 = createMockResponse(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), 8);
            when(service.listar(1L, null, null, null)).thenReturn(List.of(v1));

            // when & then
            mockMvc.perform(get("/api/v1/vacaciones")
                            .param("personaId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].personaId").value(1));

            verify(service).listar(1L, null, null, null);
        }

        @Test
        @DisplayName("GET con filtro squadId retorna 200 filtrado")
        void listar_conFiltroSquadId_retorna200() throws Exception {
            // given
            VacacionResponse v1 = createMockResponse(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), 8);
            when(service.listar(null, 1L, null, null)).thenReturn(List.of(v1));

            // when & then
            mockMvc.perform(get("/api/v1/vacaciones")
                            .param("squadId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(service).listar(null, 1L, null, null);
        }

        @Test
        @DisplayName("GET con ambos filtros retorna 200 filtrado")
        void listar_conAmbosFiltros_retorna200() throws Exception {
            // given
            when(service.listar(1L, 2L, null, null)).thenReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/vacaciones")
                            .param("personaId", "1")
                            .param("squadId", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(service).listar(1L, 2L, null, null);
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/vacaciones/{id} - Obtener
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/vacaciones/{id} - Obtener")
    class ObtenerTests {

        @Test
        @DisplayName("GET por ID existente retorna 200")
        void obtener_idExistente_retorna200() throws Exception {
            // given
            VacacionResponse response = createMockResponse(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), 8);
            when(service.obtener(1L)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/vacaciones/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.diasLaborables").value(8));

            verify(service).obtener(1L);
        }

        @Test
        @DisplayName("GET por ID inexistente retorna 400")
        void obtener_idInexistente_retorna400() throws Exception {
            // given
            when(service.obtener(999L)).thenThrow(new IllegalArgumentException("Vacacion no encontrada: 999"));

            // when & then
            mockMvc.perform(get("/api/v1/vacaciones/999"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════
    // POST /api/v1/vacaciones - Crear
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/vacaciones - Crear")
    class CrearTests {

        @Test
        @DisplayName("POST con datos válidos retorna 201")
        void crear_datosValidos_retorna201() throws Exception {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 10),
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    null
            );

            VacacionResponse response = createMockResponse(1L, 1L, request.fechaInicio(), request.fechaFin(), 8);
            when(service.crear(any(VacacionRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/vacaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.diasLaborables").value(8));

            verify(service).crear(any(VacacionRequest.class));
        }

        @Test
        @DisplayName("POST con fechaFin < fechaInicio retorna 400")
        void crear_fechaFinAnterior_retorna400() throws Exception {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 7, 10),
                    LocalDate.of(2026, 7, 1), // anterior
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    null
            );

            when(service.crear(any())).thenThrow(new IllegalArgumentException("fechaFin debe ser posterior o igual a fechaInicio"));

            // when & then
            mockMvc.perform(post("/api/v1/vacaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST con solapamiento retorna 400")
        void crear_solapamiento_retorna400() throws Exception {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 15),
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    null
            );

            when(service.crear(any())).thenThrow(new IllegalArgumentException("La persona ya tiene vacaciones en ese rango"));

            // when & then
            mockMvc.perform(post("/api/v1/vacaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST con persona inexistente retorna 404")
        void crear_personaInexistente_retorna404() throws Exception {
            // given
            VacacionRequest request = new VacacionRequest(
                    999L,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 10),
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    null
            );

            when(service.crear(any())).thenThrow(new IllegalArgumentException("Persona no encontrada: 999"));

            // when & then
            mockMvc.perform(post("/api/v1/vacaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════
    // PUT /api/v1/vacaciones/{id} - Actualizar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/vacaciones/{id} - Actualizar")
    class ActualizarTests {

        @Test
        @DisplayName("PUT con datos válidos retorna 200")
        void actualizar_datosValidos_retorna200() throws Exception {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 7, 5),
                    LocalDate.of(2026, 7, 15),
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "Ajustado"
            );

            VacacionResponse response = createMockResponse(1L, 1L, request.fechaInicio(), request.fechaFin(), 9);
            when(service.actualizar(eq(1L), any(VacacionRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/vacaciones/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.diasLaborables").value(9));

            verify(service).actualizar(eq(1L), any(VacacionRequest.class));
        }

        @Test
        @DisplayName("PUT con ID inexistente retorna 404")
        void actualizar_idInexistente_retorna404() throws Exception {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 10),
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    null
            );

            when(service.actualizar(eq(999L), any())).thenThrow(new IllegalArgumentException("Vacacion no encontrada: 999"));

            // when & then
            mockMvc.perform(put("/api/v1/vacaciones/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT con solapamiento de otra vacación retorna 400")
        void actualizar_solapamientoOtra_retorna400() throws Exception {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 20), // solapa con otra
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    null
            );

            when(service.actualizar(eq(1L), any())).thenThrow(new IllegalArgumentException("Solapamiento con otra vacación"));

            // when & then
            mockMvc.perform(put("/api/v1/vacaciones/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════
    // DELETE /api/v1/vacaciones/{id} - Eliminar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/vacaciones/{id} - Eliminar")
    class EliminarTests {

        @Test
        @DisplayName("DELETE con ID existente retorna 204")
        void eliminar_idExistente_retorna204() throws Exception {
            // given
            doNothing().when(service).eliminar(1L);

            // when & then
            mockMvc.perform(delete("/api/v1/vacaciones/1"))
                    .andExpect(status().isNoContent());

            verify(service).eliminar(1L);
        }

        @Test
        @DisplayName("DELETE con ID inexistente retorna 400")
        void eliminar_idInexistente_retorna400() throws Exception {
            // given
            doThrow(new IllegalArgumentException("Vacacion no encontrada: 999"))
                    .when(service).eliminar(999L);

            // when & then
            mockMvc.perform(delete("/api/v1/vacaciones/999"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Endpoints Excel Import")
    class ExcelImportTests {

        @Test
        @DisplayName("POST /analizar-excel — análisis dry-run exitoso")
        void analizarExcel_archivoValido() throws Exception {
            // given
            byte[] excelContent = "fichero-mock".getBytes();
            var analysisResponse = new com.kaos.calendario.dto.ExcelAnalysisResponse(
                    2,
                    List.of(
                            new com.kaos.calendario.dto.ExcelAnalysisResponse.PersonaMatch(
                                    "Alberto Rodriguez González", 1L, "Alberto Rodriguez González"
                            )
                    ),
                    List.of("Persona Desconocida")
            );

            when(excelImportService.analizarExcel(any(), eq(2026)))
                    .thenReturn(analysisResponse);

            // when & then
            mockMvc.perform(multipart("/api/v1/vacaciones/analizar-excel")
                    .file(new org.springframework.mock.web.MockMultipartFile(
                            "file", "datos.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelContent
                    ))
                    .param("año", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalFilasPersona").value(2))
                    .andExpect(jsonPath("$.personasResueltas.length()").value(1))
                    .andExpect(jsonPath("$.personasNoResueltas.length()").value(1));
        }

        @Test
        @DisplayName("POST /analizar-excel — archivo vacío retorna 400")
        void analizarExcel_archivoVacio() throws Exception {
            // when & then
            mockMvc.perform(multipart("/api/v1/vacaciones/analizar-excel")
                    .file(new org.springframework.mock.web.MockMultipartFile(
                            "file", "vacio.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]
                    ))
                    .param("año", "2026"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("POST /importar-excel — importación sin mappings")
        void importarExcel_sinMappings() throws Exception {
            // given
            byte[] excelContent = "fichero-mock".getBytes();
            var importResponse = new com.kaos.calendario.dto.ExcelImportResponse(
                    1, 2, 0, List.of(), List.of()
            );

            when(excelImportService.importarExcel(any(), eq(2026), any()))
                    .thenReturn(importResponse);

            // when & then
            mockMvc.perform(multipart("/api/v1/vacaciones/importar-excel")
                    .file(new org.springframework.mock.web.MockMultipartFile(
                            "file", "datos.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelContent
                    ))
                    .param("año", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personasProcesadas").value(1))
                    .andExpect(jsonPath("$.vacacionesCreadas").value(2));
        }

        @Test
        @DisplayName("POST /importar-excel — con mappings JSON")
        void importarExcel_conMappingsJSON() throws Exception {
            // given
            byte[] excelContent = "fichero-mock".getBytes();
            String mappingsJson = "{\"Alberto Rodriguez González\": 1}";
            var importResponse = new com.kaos.calendario.dto.ExcelImportResponse(
                    1, 3, 1, List.of(), List.of()
            );

            when(excelImportService.importarExcel(any(), eq(2026), any()))
                    .thenReturn(importResponse);

            // when & then
            mockMvc.perform(multipart("/api/v1/vacaciones/importar-excel")
                    .file(new org.springframework.mock.web.MockMultipartFile(
                            "file", "datos.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelContent
                    ))
                    .param("año", "2026")
                    .param("mappingsJson", mappingsJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personasProcesadas").value(1))
                    .andExpect(jsonPath("$.vacacionesCreadas").value(3))
                    .andExpect(jsonPath("$.ausenciasCreadas").value(1));
        }

        @Test
        @DisplayName("POST /importar-excel — mappings JSON inválido retorna 400")
        void importarExcel_mappingsJSONInvalido() throws Exception {
            // when & then
            mockMvc.perform(multipart("/api/v1/vacaciones/importar-excel")
                    .file(new org.springframework.mock.web.MockMultipartFile(
                            "file", "datos.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "mock".getBytes()
                    ))
                    .param("año", "2026")
                    .param("mappingsJson", "{ INVALID JSON }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /importar-excel — personas no encontradas en response")
        void importarExcel_personasNoEncontradas() throws Exception {
            // given
            byte[] excelContent = "fichero-mock".getBytes();
            var importResponse = new com.kaos.calendario.dto.ExcelImportResponse(
                    2, 3, 1, List.of("Persona Desconocida"), List.of()
            );

            when(excelImportService.importarExcel(any(), eq(2026), any()))
                    .thenReturn(importResponse);

            // when & then
            mockMvc.perform(multipart("/api/v1/vacaciones/importar-excel")
                    .file(new org.springframework.mock.web.MockMultipartFile(
                            "file", "datos.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelContent
                    ))
                    .param("año", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personasNoEncontradas.length()").value(1))
                    .andExpect(jsonPath("$.personasNoEncontradas[0]").value("Persona Desconocida"));
        }
    }
}
