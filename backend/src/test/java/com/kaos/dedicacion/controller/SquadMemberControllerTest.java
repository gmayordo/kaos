package com.kaos.dedicacion.controller;

import com.kaos.dedicacion.dto.SquadMemberRequest;
import com.kaos.dedicacion.dto.SquadMemberResponse;
import com.kaos.dedicacion.service.SquadMemberService;
import com.kaos.persona.entity.Rol;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para {@link SquadMemberController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SquadMemberController Integration Tests")
class SquadMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SquadMemberService service;

    @Nested
    @DisplayName("GET /api/v1/squads/{squadId}/miembros")
    class ListarMiembrosSquadTests {

        @Test
        @DisplayName("Retorna miembros del squad")
        void listar_miembrosSquad_retorna200() throws Exception {
            List<SquadMemberResponse> miembros = List.of(
                    new SquadMemberResponse(1L, 1L, "Juan", 100L, "Squad Alpha",
                            Rol.BACKEND, 50, LocalDate.of(2024, 1, 1), null,
                            BigDecimal.valueOf(4), BigDecimal.valueOf(4), BigDecimal.valueOf(4),
                            BigDecimal.valueOf(4), BigDecimal.valueOf(4),
                            LocalDateTime.now(), LocalDateTime.now())
            );
            when(service.listarMiembrosSquad(100L)).thenReturn(miembros);

            mockMvc.perform(get("/api/v1/squads/100/miembros"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].personaNombre").value("Juan"));
        }

        @Test
        @DisplayName("Squad sin miembros retorna lista vacía")
        void listar_squadSinMiembros_retorna200Vacio() throws Exception {
            when(service.listarMiembrosSquad(100L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/squads/100/miembros"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/personas/{personaId}/squads")
    class ListarSquadsDePersonaTests {

        @Test
        @DisplayName("Retorna squads de la persona")
        void listar_squadsPersona_retorna200() throws Exception {
            List<SquadMemberResponse> squads = List.of(
                    new SquadMemberResponse(1L, 1L, "Juan", 100L, "Squad Alpha",
                            Rol.BACKEND, 50, LocalDate.of(2024, 1, 1), null,
                            BigDecimal.valueOf(4), BigDecimal.valueOf(4), BigDecimal.valueOf(4),
                            BigDecimal.valueOf(4), BigDecimal.valueOf(4),
                            LocalDateTime.now(), LocalDateTime.now()),
                    new SquadMemberResponse(2L, 1L, "Juan", 101L, "Squad Beta",
                            Rol.FRONTEND, 50, LocalDate.of(2024, 1, 1), null,
                            BigDecimal.valueOf(4), BigDecimal.valueOf(4), BigDecimal.valueOf(4),
                            BigDecimal.valueOf(4), BigDecimal.valueOf(4),
                            LocalDateTime.now(), LocalDateTime.now())
            );
            when(service.listarSquadsDePersona(1L)).thenReturn(squads);

            mockMvc.perform(get("/api/v1/personas/1/squads"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].squadNombre").value("Squad Alpha"))
                    .andExpect(jsonPath("$[1].squadNombre").value("Squad Beta"));
        }

        @Test
        @DisplayName("Persona sin squads retorna lista vacía")
        void listar_personaSinSquads_retorna200Vacio() throws Exception {
            when(service.listarSquadsDePersona(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/personas/1/squads"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/squad-members")
    class AsignarTests {

        @Test
        @DisplayName("Con datos válidos retorna 201 CREATED")
        void asignar_conDatosValidos_retorna201() throws Exception {
            SquadMemberRequest request = new SquadMemberRequest(1L, 100L, Rol.BACKEND,
                    50, LocalDate.of(2024, 1, 1), null);
            SquadMemberResponse response = new SquadMemberResponse(1L, 1L, "Juan",
                    100L, "Squad Alpha", Rol.BACKEND, 50, LocalDate.of(2024, 1, 1), null,
                    BigDecimal.valueOf(4), BigDecimal.valueOf(4), BigDecimal.valueOf(4),
                    BigDecimal.valueOf(4), BigDecimal.valueOf(4),
                    LocalDateTime.now(), LocalDateTime.now());
            when(service.asignar(any(SquadMemberRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/squad-members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.personaNombre").value("Juan"))
                    .andExpect(jsonPath("$.porcentaje").value(50));
        }

        @Test
        @DisplayName("Con porcentaje > 100 retorna 400")
        void asignar_porcentajeSuperior100_retorna400() throws Exception {
            SquadMemberRequest request = new SquadMemberRequest(1L, 100L, Rol.BACKEND,
                    150, LocalDate.of(2024, 1, 1), null);
            when(service.asignar(any(SquadMemberRequest.class)))
                    .thenThrow(new IllegalArgumentException("El porcentaje total supera el 100%"));

            mockMvc.perform(post("/api/v1/squad-members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/squad-members/{id}")
    class ActualizarTests {

        @Test
        @DisplayName("Con datos válidos retorna 200 OK")
        void actualizar_conDatosValidos_retorna200() throws Exception {
            SquadMemberRequest request = new SquadMemberRequest(1L, 100L, Rol.LIDER_TECNICO,
                    75, LocalDate.of(2024, 1, 1), null);
            SquadMemberResponse response = new SquadMemberResponse(1L, 1L, "Juan",
                    100L, "Squad Alpha", Rol.LIDER_TECNICO, 75, LocalDate.of(2024, 1, 1), null,
                    BigDecimal.valueOf(6), BigDecimal.valueOf(6), BigDecimal.valueOf(6),
                    BigDecimal.valueOf(6), BigDecimal.valueOf(6),
                    LocalDateTime.now(), LocalDateTime.now());
            when(service.actualizar(eq(1L), any(SquadMemberRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/v1/squad-members/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("LIDER_TECNICO"))
                    .andExpect(jsonPath("$.porcentaje").value(75));
        }

        @Test
        @DisplayName("Con ID inexistente retorna 404")
        void actualizar_conIdInexistente_retorna404() throws Exception {
            SquadMemberRequest request = new SquadMemberRequest(1L, 100L, Rol.BACKEND,
                    50, LocalDate.of(2024, 1, 1), null);
            when(service.actualizar(eq(999L), any(SquadMemberRequest.class)))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Asignación no encontrada"));

            mockMvc.perform(put("/api/v1/squad-members/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/squad-members/{id}")
    class EliminarTests {

        @Test
        @DisplayName("Con ID existente retorna 204 NO CONTENT")
        void eliminar_conIdExistente_retorna204() throws Exception {
            doNothing().when(service).eliminar(1L);

            mockMvc.perform(delete("/api/v1/squad-members/1"))
                    .andExpect(status().isNoContent());

            verify(service).eliminar(1L);
        }

        @Test
        @DisplayName("Con ID inexistente retorna 404")
        void eliminar_conIdInexistente_retorna404() throws Exception {
            doThrow(new jakarta.persistence.EntityNotFoundException("Asignación no encontrada"))
                    .when(service).eliminar(999L);

            mockMvc.perform(delete("/api/v1/squad-members/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
