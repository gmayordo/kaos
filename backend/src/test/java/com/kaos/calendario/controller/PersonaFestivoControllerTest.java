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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.kaos.calendario.dto.FestivoResponse;
import com.kaos.calendario.entity.TipoFestivo;
import com.kaos.calendario.service.FestivoService;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;

/**
 * Tests de integración para {@link PersonaFestivoController}.
 * Valida endpoints REST que listan festivos por ciudad de la persona.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PersonaFestivoController Integration Tests")
class PersonaFestivoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FestivoService service;

    @MockBean
    private PersonaRepository personaRepository;

    private FestivoResponse createMockResponse(Long id, LocalDate fecha, String descripcion,
            TipoFestivo tipo, String ciudad) {
        return new FestivoResponse(id, fecha, descripcion, tipo, ciudad,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private Persona createMockPersona(Long id, String nombre, String email, String ciudad) {
        Persona persona = new Persona();
        persona.setId(id);
        persona.setNombre(nombre);
        persona.setEmail(email);
        persona.setCiudad(ciudad);
        return persona;
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/personas/{personaId}/festivos - Listar festivos por ciudad
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/personas/{personaId}/festivos - Listar festivos por ciudad de persona")
    class ListarPorPersonaTests {

        @Test
        @DisplayName("GET sin filtros retorna 200 con festivos de la ciudad de la persona")
        void listar_sinFiltros_retorna200() throws Exception {
            // given
            Long personaId = 1L;
            Persona persona = createMockPersona(personaId, "Juan Pérez", "juan@email.com", "Zaragoza");
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2024, 12, 25), "Navidad",
                    TipoFestivo.NACIONAL, "Zaragoza");
            FestivoResponse f2 = createMockResponse(2L, LocalDate.of(2024, 1, 1), "Año Nuevo",
                    TipoFestivo.NACIONAL, "Zaragoza");

            when(personaRepository.findById(personaId)).thenReturn(Optional.of(persona));
            when(service.listarPorCiudad("Zaragoza", null, null)).thenReturn(List.of(f1, f2));

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].descripcion").value("Navidad"))
                    .andExpect(jsonPath("$[1].descripcion").value("Año Nuevo"));

            verify(personaRepository).findById(eq(personaId));
            verify(service).listarPorCiudad(eq("Zaragoza"), eq(null), eq(null));
        }

        @Test
        @DisplayName("GET con filtro fechaInicio retorna 200 filtrado")
        void listar_conFiltroFechaInicio_retorna200() throws Exception {
            // given
            Long personaId = 1L;
            LocalDate fechaInicio = LocalDate.of(2024, 7, 1);
            Persona persona = createMockPersona(personaId, "Juan Pérez", "juan@email.com", "Valencia");
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2024, 12, 25), "Navidad",
                    TipoFestivo.NACIONAL, "Valencia");

            when(personaRepository.findById(personaId)).thenReturn(Optional.of(persona));
            when(service.listarPorCiudad("Valencia", fechaInicio, null)).thenReturn(List.of(f1));

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId)
                    .param("fechaInicio", fechaInicio.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].descripcion").value("Navidad"));

            verify(personaRepository).findById(eq(personaId));
            verify(service).listarPorCiudad(eq("Valencia"), eq(fechaInicio), eq(null));
        }

        @Test
        @DisplayName("GET con filtro fechaFin retorna 200 filtrado")
        void listar_conFiltroFechaFin_retorna200() throws Exception {
            // given
            Long personaId = 1L;
            LocalDate fechaFin = LocalDate.of(2024, 12, 31);
            Persona persona = createMockPersona(personaId, "Juan Pérez", "juan@email.com", "Temuco");
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2024, 12, 25), "Navidad",
                    TipoFestivo.NACIONAL, "Temuco");

            when(personaRepository.findById(personaId)).thenReturn(Optional.of(persona));
            when(service.listarPorCiudad("Temuco", null, fechaFin)).thenReturn(List.of(f1));

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId)
                    .param("fechaFin", fechaFin.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].descripcion").value("Navidad"));

            verify(personaRepository).findById(eq(personaId));
            verify(service).listarPorCiudad(eq("Temuco"), eq(null), eq(fechaFin));
        }

        @Test
        @DisplayName("GET con ambos filtros retorna 200 filtrado")
        void listar_conAmbosFiltros_retorna200() throws Exception {
            // given
            Long personaId = 1L;
            LocalDate fechaInicio = LocalDate.of(2024, 1, 1);
            LocalDate fechaFin = LocalDate.of(2024, 12, 31);
            Persona persona = createMockPersona(personaId, "Juan Pérez", "juan@email.com", "Zaragoza");
            FestivoResponse f1 = createMockResponse(1L, LocalDate.of(2024, 12, 25), "Navidad",
                    TipoFestivo.NACIONAL, "Zaragoza");

            when(personaRepository.findById(personaId)).thenReturn(Optional.of(persona));
            when(service.listarPorCiudad("Zaragoza", fechaInicio, fechaFin)).thenReturn(List.of(f1));

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId)
                    .param("fechaInicio", fechaInicio.toString())
                    .param("fechaFin", fechaFin.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].descripcion").value("Navidad"));

            verify(personaRepository).findById(eq(personaId));
            verify(service).listarPorCiudad(eq("Zaragoza"), eq(fechaInicio), eq(fechaFin));
        }

        @Test
        @DisplayName("GET retorna lista vacía cuando no hay festivos en la ciudad")
        void listar_sinFestivos_retornaListaVacia() throws Exception {
            // given
            Long personaId = 1L;
            Persona persona = createMockPersona(personaId, "Juan Pérez", "juan@email.com", "Zaragoza");

            when(personaRepository.findById(personaId)).thenReturn(Optional.of(persona));
            when(service.listarPorCiudad("Zaragoza", null, null)).thenReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(personaRepository).findById(eq(personaId));
            verify(service).listarPorCiudad(eq("Zaragoza"), eq(null), eq(null));
        }

        @Test
        @DisplayName("GET retorna 404 cuando persona no existe")
        void listar_personaInexistente_retorna404() throws Exception {
            // given
            Long personaId = 999L;
            when(personaRepository.findById(personaId)).thenReturn(Optional.empty());

            // when & then
            mockMvc.perform(get("/api/v1/personas/{personaId}/festivos", personaId))
                    .andExpect(status().isNotFound());

            verify(personaRepository).findById(eq(personaId));
        }
    }
}