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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaos.calendario.dto.FestivoCsvError;
import com.kaos.calendario.dto.FestivoCsvUploadResponse;
import com.kaos.calendario.dto.FestivoRequest;
import com.kaos.calendario.dto.FestivoResponse;
import com.kaos.calendario.entity.TipoFestivo;
import com.kaos.calendario.service.FestivoService;

/**
 * Tests de integración para {@link FestivoController}.
 * Valida endpoints REST con @SpringBootTest y MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("FestivoController Integration Tests")
class FestivoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FestivoService service;

    private FestivoResponse createMockResponse(Long id, LocalDate fecha, String descripcion, TipoFestivo tipo) {
        return new FestivoResponse(
                id,
                fecha,
                descripcion,
                tipo,
                List.of(), // personas vacías para simplificar
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/festivos - Listar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/festivos - Listar")
    class ListarTests {

        @Test
        @DisplayName("GET sin filtros retorna 200 con lista completa")
        void listar_sinFiltros_retorna200() throws Exception {
            // given
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2026, 1, 1), "Año Nuevo", TipoFestivo.NACIONAL);
            FestivoResponse f2 = createMockResponse(2L, LocalDate.of(2026, 12, 25), "Navidad", TipoFestivo.NACIONAL);
            when(service.listar(null, null)).thenReturn(List.of(f1, f2));

            // when & then
            mockMvc.perform(get("/api/v1/festivos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].descripcion").value("Año Nuevo"))
                    .andExpect(jsonPath("$[1].descripcion").value("Navidad"));

            verify(service).listar(null, null);
        }

        @Test
        @DisplayName("GET con filtro anio retorna 200 filtrado")
        void listar_conFiltroAnio_retorna200() throws Exception {
            // given
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2026, 1, 1), "Año Nuevo", TipoFestivo.NACIONAL);
            when(service.listar(2026, null)).thenReturn(List.of(f1));

            // when & then
            mockMvc.perform(get("/api/v1/festivos")
                            .param("anio", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].fecha").value("2026-01-01"));

            verify(service).listar(2026, null);
        }

        @Test
        @DisplayName("GET con filtro tipo retorna 200 filtrado")
        void listar_conFiltroTipo_retorna200() throws Exception {
            // given
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2026, 1, 6), "Día Regional", TipoFestivo.REGIONAL);
            when(service.listar(null, TipoFestivo.REGIONAL)).thenReturn(List.of(f1));

            // when & then
            mockMvc.perform(get("/api/v1/festivos")
                            .param("tipo", "REGIONAL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].tipo").value("REGIONAL"));

            verify(service).listar(null, TipoFestivo.REGIONAL);
        }

        @Test
        @DisplayName("GET con ambos filtros retorna 200 filtrado")
        void listar_conAmbosFiltros_retorna200() throws Exception {
            // given
            when(service.listar(2026, TipoFestivo.LOCAL)).thenReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/festivos")
                            .param("anio", "2026")
                            .param("tipo", "LOCAL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(service).listar(2026, TipoFestivo.LOCAL);
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/festivos/{id} - Obtener
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/festivos/{id} - Obtener")
    class ObtenerTests {

        @Test
        @DisplayName("GET por ID existente retorna 200")
        void obtener_idExistente_retorna200() throws Exception {
            // given
            FestivoResponse response = createMockResponse(1L, LocalDate.of(2026, 1, 1), "Año Nuevo", TipoFestivo.NACIONAL);
            when(service.obtener(1L)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/festivos/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.descripcion").value("Año Nuevo"));

            verify(service).obtener(1L);
        }

        @Test
        @DisplayName("GET por ID inexistente retorna 404")
        void obtener_idInexistente_retorna404() throws Exception {
            // given
            when(service.obtener(999L)).thenThrow(new IllegalArgumentException("Festivo no encontrado: 999"));

            // when & then
            mockMvc.perform(get("/api/v1/festivos/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // POST /api/v1/festivos - Crear
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/festivos - Crear")
    class CrearTests {

        @Test
        @DisplayName("POST con datos válidos retorna 201")
        void crear_datosValidos_retorna201() throws Exception {
            // given
            FestivoRequest request = new FestivoRequest(
                    LocalDate.of(2026, 5, 1),
                    "Día del Trabajo",
                    TipoFestivo.NACIONAL,
                    List.of(1L, 2L)
            );

            FestivoResponse response = createMockResponse(1L, request.fecha(), request.descripcion(), request.tipo());
            when(service.crear(any(FestivoRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/festivos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.descripcion").value("Día del Trabajo"));

            verify(service).crear(any(FestivoRequest.class));
        }

        @Test
        @DisplayName("POST con fecha null retorna 400")
        void crear_fechaNull_retorna400() throws Exception {
            // given
            String requestJson = """
                    {
                        "fecha": null,
                        "descripcion": "Test",
                        "tipo": "NACIONAL",
                        "personaIds": []
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/festivos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST con descripcion blank retorna 400")
        void crear_descripcionBlank_retorna400() throws Exception {
            // given
            String requestJson = """
                    {
                        "fecha": "2026-01-01",
                        "descripcion": "",
                        "tipo": "NACIONAL",
                        "personaIds": []
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/festivos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST duplicado retorna 409")
        void crear_duplicado_retorna409() throws Exception {
            // given
            FestivoRequest request = new FestivoRequest(
                    LocalDate.of(2026, 1, 1),
                    "Año Nuevo",
                    TipoFestivo.NACIONAL,
                    List.of()
            );

            when(service.crear(any())).thenThrow(new IllegalArgumentException("Ya existe un festivo con la misma fecha y descripción"));

            // when & then
            mockMvc.perform(post("/api/v1/festivos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // ══════════════════════════════════════════════════════════
    // PUT /api/v1/festivos/{id} - Actualizar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/festivos/{id} - Actualizar")
    class ActualizarTests {

        @Test
        @DisplayName("PUT con datos válidos retorna 200")
        void actualizar_datosValidos_retorna200() throws Exception {
            // given
            FestivoRequest request = new FestivoRequest(
                    LocalDate.of(2026, 1, 1),
                    "Año Nuevo Actualizado",
                    TipoFestivo.NACIONAL,
                    List.of(1L)
            );

            FestivoResponse response = createMockResponse(1L, request.fecha(), request.descripcion(), request.tipo());
            when(service.actualizar(eq(1L), any(FestivoRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(put("/api/v1/festivos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.descripcion").value("Año Nuevo Actualizado"));

            verify(service).actualizar(eq(1L), any(FestivoRequest.class));
        }

        @Test
        @DisplayName("PUT con ID inexistente retorna 404")
        void actualizar_idInexistente_retorna404() throws Exception {
            // given
            FestivoRequest request = new FestivoRequest(
                    LocalDate.of(2026, 1, 1),
                    "Test",
                    TipoFestivo.NACIONAL,
                    List.of()
            );

            when(service.actualizar(eq(999L), any())).thenThrow(new IllegalArgumentException("Festivo no encontrado: 999"));

            // when & then
            mockMvc.perform(put("/api/v1/festivos/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // DELETE /api/v1/festivos/{id} - Eliminar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/festivos/{id} - Eliminar")
    class EliminarTests {

        @Test
        @DisplayName("DELETE con ID existente retorna 204")
        void eliminar_idExistente_retorna204() throws Exception {
            // given
            doNothing().when(service).eliminar(1L);

            // when & then
            mockMvc.perform(delete("/api/v1/festivos/1"))
                    .andExpect(status().isNoContent());

            verify(service).eliminar(1L);
        }

        @Test
        @DisplayName("DELETE con ID inexistente retorna 404")
        void eliminar_idInexistente_retorna404() throws Exception {
            // given
            doThrow(new IllegalArgumentException("Festivo no encontrado: 999"))
                    .when(service).eliminar(999L);

            // when & then
            mockMvc.perform(delete("/api/v1/festivos/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // POST /api/v1/festivos/carga-masiva - CSV Upload
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/festivos/carga-masiva - CSV Upload")
    class CargaMasivaTests {

        @Test
        @DisplayName("POST CSV válido retorna 200 con resumen")
        void cargarCsv_archivoValido_retorna200() throws Exception {
            // given
            String csvContent = """
                    2026-01-01,Año Nuevo,NACIONAL,juan.perez@kaos.com
                    2026-05-01,Día del Trabajo,NACIONAL,maria.garcia@kaos.com
                    2026-12-25,Navidad,NACIONAL,pedro.lopez@kaos.com
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            FestivoCsvUploadResponse response = new FestivoCsvUploadResponse(3, 3, 0, List.of());
            when(service.cargarCsv(any())).thenReturn(response);

            // when & then
            mockMvc.perform(multipart("/api/v1/festivos/carga-masiva")
                            .file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalProcesados").value(3))
                    .andExpect(jsonPath("$.exitosos").value(3))
                    .andExpect(jsonPath("$.errores").value(0));

            verify(service).cargarCsv(any());
        }

        @Test
        @DisplayName("POST CSV con errores retorna 200 con resumen de errores")
        void cargarCsv_archivoConErrores_retorna200ConErrores() throws Exception {
            // given
            String csvContent = """
                    2026-01-01,Año Nuevo,NACIONAL,juan.perez@kaos.com
                    2026-05-01,Día del Trabajo,NACIONAL,noexiste@kaos.com
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            FestivoCsvUploadResponse response = new FestivoCsvUploadResponse(
                    2, 1, 1,
                    List.of(new FestivoCsvError(2, "Persona no encontrada: noexiste@kaos.com"))
            );
            when(service.cargarCsv(any())).thenReturn(response);

            // when & then
            mockMvc.perform(multipart("/api/v1/festivos/carga-masiva")
                            .file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalProcesados").value(2))
                    .andExpect(jsonPath("$.exitosos").value(1))
                    .andExpect(jsonPath("$.errores").value(1))
                    .andExpect(jsonPath("$.mensajesError[0]").value("Línea 2: Persona no encontrada: noexiste@kaos.com"));
        }

        @Test
        @DisplayName("POST archivo vacío retorna 400")
        void cargarCsv_archivoVacio_retorna400() throws Exception {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    new byte[0]
            );

            // when & then
            mockMvc.perform(multipart("/api/v1/festivos/carga-masiva")
                            .file(file))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST archivo no CSV retorna 400")
        void cargarCsv_archivoNoCsv_retorna400() throws Exception {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.txt",
                    "text/plain",
                    "contenido".getBytes()
            );

            // when & then
            mockMvc.perform(multipart("/api/v1/festivos/carga-masiva")
                            .file(file))
                    .andExpect(status().isBadRequest());
        }
    }
}
