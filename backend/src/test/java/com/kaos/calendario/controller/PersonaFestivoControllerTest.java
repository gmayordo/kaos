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
import com.kaos.calendario.dto.FestivoResponse;
import com.kaos.calendario.dto.PersonaBasicInfo;
import com.kaos.calendario.entity.TipoFestivo;
import com.kaos.calendario.service.FestivoService;

/**
 * Tests de integración para {@link PersonaFestivoController}.
 * Valida endpoints REST con @SpringBootTest y MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PersonaFestivoController Integration Tests")
class PersonaFestivoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FestivoService service;

    private FestivoResponse createMockResponse(Long id, LocalDate fecha, String descripcion,
            TipoFestivo tipo, List<PersonaBasicInfo> personas) {
        return new FestivoResponse(
                id,
                fecha,
                descripcion,
                tipo,
                personas,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private PersonaBasicInfo createPersonaBasicInfo(Long id, String nombre, String email) {
        return new PersonaBasicInfo(id, nombre, email);
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/personas/{personaId}/festivos - Listar por persona
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/personas/{personaId}/festivos - Listar por persona")
    class ListarPorPersonaTests {

        @Test
        @DisplayName("GET sin filtros retorna 200 con lista de festivos de la persona")
        void listar_sinFiltros_retorna200() throws Exception {
            // given
            Long personaId = 1L;
            PersonaBasicInfo persona = createPersonaBasicInfo(personaId, "Juan Pérez", "juan@email.com");
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2024, 12, 25), "Navidad",
                    TipoFestivo.NACIONAL, List.of(persona));
            FestivoResponse f2 = createMockResponse(2L, LocalDate.of(2024, 1, 1), "Año Nuevo",
                    TipoFestivo.NACIONAL, List.of(persona));
            when(service.listarPorPersona(personaId, null, null)).thenReturn(List.of(f1, f2));

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].descripcion").value("Navidad"))
                    .andExpect(jsonPath("$[1].descripcion").value("Año Nuevo"));

            verify(service).listarPorPersona(eq(personaId), eq(null), eq(null));
        }

        @Test
        @DisplayName("GET con filtro fechaInicio retorna 200 filtrado")
        void listar_conFiltroFechaInicio_retorna200() throws Exception {
            // given
            Long personaId = 1L;
            LocalDate fechaInicio = LocalDate.of(2024, 7, 1);
            PersonaBasicInfo persona = createPersonaBasicInfo(personaId, "Juan Pérez", "juan@email.com");
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2024, 12, 25), "Navidad",
                    TipoFestivo.NACIONAL, List.of(persona));
            when(service.listarPorPersona(personaId, fechaInicio, null)).thenReturn(List.of(f1));

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId)
                    .param("fechaInicio", fechaInicio.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].descripcion").value("Navidad"));

            verify(service).listarPorPersona(eq(personaId), eq(fechaInicio), eq(null));
        }

        @Test
        @DisplayName("GET con filtro fechaFin retorna 200 filtrado")
        void listar_conFiltroFechaFin_retorna200() throws Exception {
            // given
            Long personaId = 1L;
            LocalDate fechaFin = LocalDate.of(2024, 12, 31);
            PersonaBasicInfo persona = createPersonaBasicInfo(personaId, "Juan Pérez", "juan@email.com");
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2024, 12, 25), "Navidad",
                    TipoFestivo.NACIONAL, List.of(persona));
            when(service.listarPorPersona(personaId, null, fechaFin)).thenReturn(List.of(f1));

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId)
                    .param("fechaFin", fechaFin.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].descripcion").value("Navidad"));

            verify(service).listarPorPersona(eq(personaId), eq(null), eq(fechaFin));
        }

        @Test
        @DisplayName("GET con ambos filtros retorna 200 filtrado")
        void listar_conAmbosFiltros_retorna200() throws Exception {
            // given
            Long personaId = 1L;
            LocalDate fechaInicio = LocalDate.of(2024, 1, 1);
            LocalDate fechaFin = LocalDate.of(2024, 12, 31);
            PersonaBasicInfo persona = createPersonaBasicInfo(personaId, "Juan Pérez", "juan@email.com");
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2024, 12, 25), "Navidad",
                    TipoFestivo.NACIONAL, List.of(persona));
            when(service.listarPorPersona(personaId, fechaInicio, fechaFin)).thenReturn(List.of(f1));

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId)
                    .param("fechaInicio", fechaInicio.toString())
                    .param("fechaFin", fechaFin.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].descripcion").value("Navidad"));

            verify(service).listarPorPersona(eq(personaId), eq(fechaInicio), eq(fechaFin));
        }

        @Test
        @DisplayName("GET retorna lista vacía cuando no hay festivos asignados")
        void listar_sinFestivos_retornaListaVacia() throws Exception {
            // given
            Long personaId = 1L;
            when(service.listarPorPersona(personaId, null, null)).thenReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(service).listarPorPersona(eq(personaId), eq(null), eq(null));
        }
    }
}