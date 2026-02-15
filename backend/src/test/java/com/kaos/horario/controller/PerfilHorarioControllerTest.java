package com.kaos.horario.controller;

import com.kaos.horario.dto.PerfilHorarioRequest;
import com.kaos.horario.dto.PerfilHorarioResponse;
import com.kaos.horario.service.PerfilHorarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para {@link PerfilHorarioController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PerfilHorarioController Integration Tests")
class PerfilHorarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PerfilHorarioService service;

    @Nested
    @DisplayName("GET /api/v1/perfiles-horario")
    class ListarTests {

        @Test
        @DisplayName("Retorna todos los perfiles de horario")
        void listar_retorna200ConPerfiles() throws Exception {
            List<PerfilHorarioResponse> perfiles = List.of(
                    new PerfilHorarioResponse(1L, "Jornada Completa ES", "Europe/Madrid",
                            BigDecimal.valueOf(8), BigDecimal.valueOf(8), BigDecimal.valueOf(8),
                            BigDecimal.valueOf(8), BigDecimal.valueOf(8),
                            BigDecimal.valueOf(40), LocalDateTime.now(), LocalDateTime.now()),
                    new PerfilHorarioResponse(2L, "Jornada Reducida", "Europe/Madrid",
                            BigDecimal.valueOf(6), BigDecimal.valueOf(6), BigDecimal.valueOf(6),
                            BigDecimal.valueOf(6), BigDecimal.valueOf(6),
                            BigDecimal.valueOf(30), LocalDateTime.now(), LocalDateTime.now())
            );
            when(service.listar()).thenReturn(perfiles);

            mockMvc.perform(get("/api/v1/perfiles-horario"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].nombre").value("Jornada Completa ES"))
                    .andExpect(jsonPath("$[1].totalSemanal").value(30));
        }

        @Test
        @DisplayName("Sin perfiles retorna lista vacía")
        void listar_sinPerfiles_retorna200Vacio() throws Exception {
            when(service.listar()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/perfiles-horario"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/perfiles-horario/{id}")
    class ObtenerTests {

        @Test
        @DisplayName("Con ID existente retorna perfil")
        void obtener_conIdExistente_retorna200() throws Exception {
            PerfilHorarioResponse perfil = new PerfilHorarioResponse(1L, "Jornada Completa ES",
                    "Europe/Madrid", BigDecimal.valueOf(8), BigDecimal.valueOf(8),
                    BigDecimal.valueOf(8), BigDecimal.valueOf(8), BigDecimal.valueOf(8),
                    BigDecimal.valueOf(40), LocalDateTime.now(), LocalDateTime.now());
            when(service.obtener(1L)).thenReturn(perfil);

            mockMvc.perform(get("/api/v1/perfiles-horario/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Jornada Completa ES"));
        }

        @Test
        @DisplayName("Con ID inexistente retorna 404")
        void obtener_conIdInexistente_retorna404() throws Exception {
            when(service.obtener(999L))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Perfil no encontrado"));

            mockMvc.perform(get("/api/v1/perfiles-horario/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/perfiles-horario")
    class CrearTests {

        @Test
        @DisplayName("Con datos válidos retorna 201 CREATED")
        void crear_conDatosValidos_retorna201() throws Exception {
            PerfilHorarioRequest request = new PerfilHorarioRequest("Jornada 4 días",
                    "Europe/Madrid", BigDecimal.valueOf(10), BigDecimal.valueOf(10),
                    BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.ZERO);
            PerfilHorarioResponse response = new PerfilHorarioResponse(3L, "Jornada 4 días",
                    "Europe/Madrid", BigDecimal.valueOf(10), BigDecimal.valueOf(10),
                    BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.ZERO,
                    BigDecimal.valueOf(40), LocalDateTime.now(), LocalDateTime.now());
            when(service.crear(any(PerfilHorarioRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/perfiles-horario")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.nombre").value("Jornada 4 días"));
        }

        @Test
        @DisplayName("Con nombre duplicado retorna 400")
        void crear_conNombreDuplicado_retorna400() throws Exception {
            PerfilHorarioRequest request = new PerfilHorarioRequest("Jornada Completa ES",
                    "Europe/Madrid", BigDecimal.valueOf(8), BigDecimal.valueOf(8),
                    BigDecimal.valueOf(8), BigDecimal.valueOf(8), BigDecimal.valueOf(8));
            when(service.crear(any(PerfilHorarioRequest.class)))
                    .thenThrow(new IllegalArgumentException("Perfil con nombre 'Jornada Completa ES' ya existe"));

            mockMvc.perform(post("/api/v1/perfiles-horario")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/perfiles-horario/{id}")
    class ActualizarTests {

        @Test
        @DisplayName("Con datos válidos retorna 200 OK")
        void actualizar_conDatosValidos_retorna200() throws Exception {
            PerfilHorarioRequest request = new PerfilHorarioRequest("Jornada Actualizada",
                    "Europe/Madrid", BigDecimal.valueOf(7), BigDecimal.valueOf(7),
                    BigDecimal.valueOf(7), BigDecimal.valueOf(7), BigDecimal.valueOf(7));
            PerfilHorarioResponse response = new PerfilHorarioResponse(1L, "Jornada Actualizada",
                    "Europe/Madrid", BigDecimal.valueOf(7), BigDecimal.valueOf(7),
                    BigDecimal.valueOf(7), BigDecimal.valueOf(7), BigDecimal.valueOf(7),
                    BigDecimal.valueOf(35), LocalDateTime.now(), LocalDateTime.now());
            when(service.actualizar(eq(1L), any(PerfilHorarioRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/v1/perfiles-horario/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Jornada Actualizada"));
        }

        @Test
        @DisplayName("Con ID inexistente retorna 404")
        void actualizar_conIdInexistente_retorna404() throws Exception {
            PerfilHorarioRequest request = new PerfilHorarioRequest("Perfil",
                    "Europe/Madrid", BigDecimal.valueOf(8), BigDecimal.valueOf(8),
                    BigDecimal.valueOf(8), BigDecimal.valueOf(8), BigDecimal.valueOf(8));
            when(service.actualizar(eq(999L), any(PerfilHorarioRequest.class)))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Perfil no encontrado"));

            mockMvc.perform(put("/api/v1/perfiles-horario/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/perfiles-horario/{id}")
    class EliminarTests {

        @Test
        @DisplayName("Con ID existente y sin personas asignadas retorna 204 NO CONTENT")
        void eliminar_conIdExistenteSinPersonas_retorna204() throws Exception {
            doNothing().when(service).eliminar(1L);

            mockMvc.perform(delete("/api/v1/perfiles-horario/1"))
                    .andExpect(status().isNoContent());

            verify(service).eliminar(1L);
        }

        @Test
        @DisplayName("Con ID inexistente retorna 404")
        void eliminar_conIdInexistente_retorna404() throws Exception {
            doThrow(new jakarta.persistence.EntityNotFoundException("Perfil no encontrado"))
                    .when(service).eliminar(999L);

            mockMvc.perform(delete("/api/v1/perfiles-horario/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Con personas asignadas retorna 409 CONFLICT")
        void eliminar_conPersonasAsignadas_retorna409() throws Exception {
            doThrow(new IllegalStateException("No se puede eliminar perfil con personas asignadas"))
                    .when(service).eliminar(1L);

            mockMvc.perform(delete("/api/v1/perfiles-horario/1"))
                    .andExpect(status().isConflict());
        }
    }
}
