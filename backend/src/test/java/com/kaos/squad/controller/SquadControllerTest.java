package com.kaos.squad.controller;

import com.kaos.squad.dto.SquadRequest;
import com.kaos.squad.dto.SquadResponse;
import com.kaos.squad.entity.EstadoSquad;
import com.kaos.squad.service.SquadService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para {@link SquadController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SquadController Integration Tests")
class SquadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SquadService service;

    @Nested
    @DisplayName("GET /api/v1/squads")
    class ListarTests {

        @Test
        @DisplayName("Sin filtro retorna todos los squads")
        void listar_sinFiltro_retorna200() throws Exception {
            List<SquadResponse> squads = List.of(
                    new SquadResponse(1L, "Squad Alpha", "Desarrollo core", EstadoSquad.ACTIVO,
                            "SA-1", "SE-1", LocalDateTime.now(), LocalDateTime.now()),
                    new SquadResponse(2L, "Squad Beta", "Nuevas features", EstadoSquad.ACTIVO,
                            "SB-1", "SE-2", LocalDateTime.now(), LocalDateTime.now())
            );
            when(service.listar(null)).thenReturn(squads);

            mockMvc.perform(get("/api/v1/squads"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].nombre").value("Squad Alpha"))
                    .andExpect(jsonPath("$[1].nombre").value("Squad Beta"));
        }

        @Test
        @DisplayName("Con filtro estado ACTIVO retorna solo activos")
        void listar_conFiltroActivo_retorna200() throws Exception {
            List<SquadResponse> squads = List.of(
                    new SquadResponse(1L, "Squad Alpha", "Desarrollo core", EstadoSquad.ACTIVO,
                            "SA-1", "SE-1", LocalDateTime.now(), LocalDateTime.now())
            );
            when(service.listar(EstadoSquad.ACTIVO)).thenReturn(squads);

            mockMvc.perform(get("/api/v1/squads").param("estado", "ACTIVO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("Sin resultados retorna lista vacía")
        void listar_sinResultados_retorna200Vacio() throws Exception {
            when(service.listar(null)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/squads"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/squads/{id}")
    class ObtenerTests {

        @Test
        @DisplayName("Con ID existente retorna squad")
        void obtener_conIdExistente_retorna200() throws Exception {
            SquadResponse squad = new SquadResponse(1L, "Squad Alpha", "Desarrollo core",
                    EstadoSquad.ACTIVO, "SA-1", "SE-1", LocalDateTime.now(), LocalDateTime.now());
            when(service.obtener(1L)).thenReturn(squad);

            mockMvc.perform(get("/api/v1/squads/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Squad Alpha"));
        }

        @Test
        @DisplayName("Con ID inexistente retorna 404")
        void obtener_conIdInexistente_retorna404() throws Exception {
            when(service.obtener(999L))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Squad no encontrado"));

            mockMvc.perform(get("/api/v1/squads/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/squads")
    class CrearTests {

        @Test
        @DisplayName("Con datos válidos retorna 201 CREATED")
        void crear_conDatosValidos_retorna201() throws Exception {
            SquadRequest request = new SquadRequest("Squad Gamma", "Nuevas features", "SG-1", "SE-3");
            SquadResponse response = new SquadResponse(3L, "Squad Gamma", "Nuevas features",
                    EstadoSquad.ACTIVO, "SG-1", "SE-3", LocalDateTime.now(), LocalDateTime.now());
            when(service.crear(any(SquadRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/squads")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.nombre").value("Squad Gamma"));
        }

        @Test
        @DisplayName("Con nombre duplicado retorna 400")
        void crear_conNombreDuplicado_retorna400() throws Exception {
            SquadRequest request = new SquadRequest("Squad Alpha", "Desc", "SA-10", "SE-10");
            when(service.crear(any(SquadRequest.class)))
                    .thenThrow(new IllegalArgumentException("Squad con nombre Alpha ya existe"));

            mockMvc.perform(post("/api/v1/squads")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/squads/{id}")
    class ActualizarTests {

        @Test
        @DisplayName("Con datos válidos retorna 200 OK")
        void actualizar_conDatosValidos_retorna200() throws Exception {
            SquadRequest request = new SquadRequest("Squad Alpha Updated", "Nueva desc", "SA-1", "SE-1");
            SquadResponse response = new SquadResponse(1L, "Squad Alpha Updated", "Nueva desc",
                    EstadoSquad.ACTIVO, "SA-1", "SE-1", LocalDateTime.now(), LocalDateTime.now());
            when(service.actualizar(eq(1L), any(SquadRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/v1/squads/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Squad Alpha Updated"));
        }

        @Test
        @DisplayName("Con ID inexistente retorna 404")
        void actualizar_conIdInexistente_retorna404() throws Exception {
            SquadRequest request = new SquadRequest("Squad", "Desc", "S-1", "SE-1");
            when(service.actualizar(eq(999L), any(SquadRequest.class)))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Squad no encontrado"));

            mockMvc.perform(put("/api/v1/squads/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/squads/{id}/desactivar")
    class DesactivarTests {

        @Test
        @DisplayName("Con ID existente retorna 200 OK")
        void desactivar_conIdExistente_retorna200() throws Exception {
            SquadResponse response = new SquadResponse(1L, "Squad Alpha", "Desarrollo core",
                    EstadoSquad.INACTIVO, "SA-1", "SE-1", LocalDateTime.now(), LocalDateTime.now());
            when(service.desactivar(1L)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/squads/1/desactivar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("INACTIVO"));
        }

        @Test
        @DisplayName("Con ID inexistente retorna 404")
        void desactivar_conIdInexistente_retorna404() throws Exception {
            when(service.desactivar(999L))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Squad no encontrado"));

            mockMvc.perform(patch("/api/v1/squads/999/desactivar"))
                    .andExpect(status().isNotFound());
        }
    }
}
