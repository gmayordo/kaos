package com.kaos.persona.controller;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaos.persona.dto.PersonaRequest;
import com.kaos.persona.dto.PersonaResponse;
import com.kaos.persona.entity.Rol;
import com.kaos.persona.entity.Seniority;
import com.kaos.persona.service.PersonaService;import com.kaos.persona.dto.PersonaRequest;
import com.kaos.persona.dto.PersonaResponse;
import com.kaos.persona.entity.Rol;
import com.kaos.persona.entity.Seniority;
import com.kaos.persona.service.PersonaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
 * Tests de integración para {@link PersonaController}.
 * Usa @SpringBootTest con @AutoConfigureMockMvc para testing completo con MockMvc.
 * MockBean del service para aislar la capa REST.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PersonaController Integration Tests")
class PersonaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonaService service;

    private PersonaResponse createMockResponse(Long id, String nombre) {
        return new PersonaResponse(
                id,
                nombre,
                "test@example.com",
                "JIRA-001",
                1L,
                "Zona Europa",
                "Zaragoza",
                Seniority.MID,
                "Java",
                new BigDecimal("50.00"),
                true,
                LocalDate.now(),
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /api/v1/personas - Listar")
    class ListarTests {

        @Test
        @DisplayName("Debe listar personas sin filtros con paginación por defecto")
        void listar_sinFiltros_retorna200ConPage() throws Exception {
            // Given
            PersonaResponse persona1 = createMockResponse(1L, "Juan Pérez");
            PersonaResponse persona2 = createMockResponse(2L, "Ana López");
            Page<PersonaResponse> page = new PageImpl<>(List.of(persona1, persona2));
            when(service.listar(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/v1/personas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].nombre").value("Juan Pérez"))
                    .andExpect(jsonPath("$.content[1].nombre").value("Ana López"));

            verify(service).listar(isNull(), isNull(), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("Debe listar personas filtrando por squadId")
        void listar_conFiltroSquadId_retorna200() throws Exception {
            // Given
            PersonaResponse persona = createMockResponse(1L, "Juan Pérez");
            Page<PersonaResponse> page = new PageImpl<>(List.of(persona));
            when(service.listar(eq(5L), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/v1/personas?squadId=5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(service).listar(eq(5L), isNull(), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("Debe listar personas filtrando por rol")
        void listar_conFiltroRol_retorna200() throws Exception {
            // Given
            PersonaResponse persona = createMockResponse(1L, "Developer");
            Page<PersonaResponse> page = new PageImpl<>(List.of(persona));
            when(service.listar(isNull(), eq(Rol.BACKEND), isNull(), isNull(), isNull(), any()))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/v1/personas?rol=BACKEND"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(service).listar(isNull(), eq(Rol.BACKEND), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("Debe listar personas con paginación personalizada")
        void listar_conPaginacionCustom_retorna200() throws Exception {
            // Given
            PersonaResponse persona = createMockResponse(1L, "Test");
            Page<PersonaResponse> page = new PageImpl<>(List.of(persona), PageRequest.of(1, 5), 10);
            when(service.listar(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/v1/personas?page=1&size=5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageable.pageNumber").value(1))
                    .andExpect(jsonPath("$.pageable.pageSize").value(5));

            verify(service).listar(isNull(), isNull(), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("Debe listar personas con ordenamiento personalizado ASC")
        void listar_conSortAsc_retorna200() throws Exception {
            // Given
            PersonaResponse persona = createMockResponse(1L, "Ana");
            Page<PersonaResponse> page = new PageImpl<>(List.of(persona), PageRequest.of(0, 20), 1);
            when(service.listar(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/v1/personas?sort=nombre,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(service).listar(isNull(), isNull(), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("Debe listar personas con ordenamiento personalizado DESC")
        void listar_conSortDesc_retorna200() throws Exception {
            // Given
            PersonaResponse persona = createMockResponse(1L, "Zoe");
            Page<PersonaResponse> page = new PageImpl<>(List.of(persona), PageRequest.of(0, 20), 1);
            when(service.listar(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/v1/personas?sort=email,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(service).listar(isNull(), isNull(), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("Debe retornar página vacía cuando no hay resultados")
        void listar_sinResultados_retorna200Vacio() throws Exception {
            // Given
            Page<PersonaResponse> emptyPage = Page.empty();
            when(service.listar(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(emptyPage);

            // When / Then
            mockMvc.perform(get("/api/v1/personas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/personas/{id} - Obtener")
    class ObtenerTests {

        @Test
        @DisplayName("Debe obtener persona existente por ID")
        void obtener_conIdExistente_retorna200() throws Exception {
            // Given
            PersonaResponse response = createMockResponse(1L, "Juan Pérez");
            when(service.obtener(1L)).thenReturn(response);

            // When / Then
            mockMvc.perform(get("/api/v1/personas/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Juan Pérez"))
                    .andExpect(jsonPath("$.email").value("test@example.com"));

            verify(service).obtener(1L);
        }

        @Test
        @DisplayName("Debe retornar 404 cuando persona no existe")
        void obtener_conIdInexistente_retorna404() throws Exception {
            // Given
            when(service.obtener(999L)).thenThrow(new jakarta.persistence.EntityNotFoundException("Persona no encontrada"));

            // When / Then
            mockMvc.perform(get("/api/v1/personas/999"))
                    .andExpect(status().isNotFound());

            verify(service).obtener(999L);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/personas - Crear")
    class CrearTests {

        @Test
        @DisplayName("Debe crear persona con datos válidos")
        void crear_conDatosValidos_retorna201() throws Exception {
            // Given
            PersonaRequest request = new PersonaRequest(
                    "Juan Pérez",
                    "juan@example.com",
                    "JIRA-001",
                    1L,
                    "Zaragoza",
                    Seniority.MID,
                    "Java",
                    new BigDecimal("50.00"),
                    LocalDate.now(),
                    true
            );
            PersonaResponse response = createMockResponse(1L, "Juan Pérez");
            when(service.crear(any(PersonaRequest.class))).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/v1/personas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Juan Pérez"));

            verify(service).crear(any(PersonaRequest.class));
        }

        @Test
        @DisplayName("Debe retornar 400 cuando request tiene campos inválidos")
        void crear_conCamposInvalidos_retorna400() throws Exception {
            // Given - request sin nombre (campo obligatorio)
            PersonaRequest requestInvalido = new PersonaRequest(
                    "",  // nombre vacío
                    "invalid-email",  // email inválido
                    "JIRA-001",
                    1L,
                    "Zaragoza",
                    Seniority.MID,
                    "Java",
                    new BigDecimal("50.00"),
                    LocalDate.now(),
                    true
            );

            // When / Then
            mockMvc.perform(post("/api/v1/personas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvalido)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).crear(any());
        }

        @Test
        @DisplayName("Debe retornar 400 cuando email ya existe")
        void crear_conEmailDuplicado_retorna400() throws Exception {
            // Given
            PersonaRequest request = new PersonaRequest(
                    "Juan Pérez",
                    "duplicado@example.com",
                    "JIRA-001",
                    1L,
                    "Zaragoza",
                    Seniority.MID,
                    "Java",
                    new BigDecimal("50.00"),
                    LocalDate.now(),
                    true
            );
            when(service.crear(any())).thenThrow(new IllegalArgumentException("Email ya registrado"));

            // When / Then
            mockMvc.perform(post("/api/v1/personas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(service).crear(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/personas/{id} - Actualizar")
    class ActualizarTests {

        @Test
        @DisplayName("Debe actualizar persona con datos válidos")
        void actualizar_conDatosValidos_retorna200() throws Exception {
            // Given
            PersonaRequest request = new PersonaRequest(
                    "Juan Pérez Updated",
                    "juan.updated@example.com",
                    "JIRA-001",
                    1L,
                    "Zaragoza",
                    Seniority.SENIOR,
                    "Java, Spring Boot",
                    new BigDecimal("60.00"),
                    LocalDate.now(),
                    true
            );
            PersonaResponse response = createMockResponse(1L, "Juan Pérez Updated");
            when(service.actualizar(eq(1L), any(PersonaRequest.class))).thenReturn(response);

            // When / Then
            mockMvc.perform(put("/api/v1/personas/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).actualizar(eq(1L), any(PersonaRequest.class));
        }

        @Test
        @DisplayName("Debe retornar 404 cuando persona a actualizar no existe")
        void actualizar_conIdInexistente_retorna404() throws Exception {
            // Given
            PersonaRequest request = new PersonaRequest(
                    "Test",
                    "test@example.com",
                    "JIRA-001",
                    1L,
                    "Zaragoza",
                    Seniority.MID,
                    "Java",
                    new BigDecimal("50.00"),
                    LocalDate.now(),
                    true
            );
            when(service.actualizar(eq(999L), any()))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Persona no encontrada"));

            // When / Then
            mockMvc.perform(put("/api/v1/personas/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            verify(service).actualizar(eq(999L), any());
        }

        @Test
        @DisplayName("Debe retornar 400 cuando request tiene campos inválidos")
        void actualizar_conCamposInvalidos_retorna400() throws Exception {
            // Given
            PersonaRequest requestInvalido = new PersonaRequest(
                    "",  // nombre vacío
                    "invalid",
                    "JIRA-001",
                    1L,
                    "Zaragoza",
                    Seniority.MID,
                    "Java",
                    new BigDecimal("50.00"),
                    LocalDate.now(),
                    true
            );

            // When / Then
            mockMvc.perform(put("/api/v1/personas/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvalido)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).actualizar(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/personas/{id}/desactivar - Desactivar")
    class DesactivarTests {

        @Test
        @DisplayName("Debe desactivar persona existente")
        void desactivar_conIdExistente_retorna200() throws Exception {
            // Given
            PersonaResponse response = createMockResponse(1L, "Juan Pérez");
            when(service.desactivar(1L)).thenReturn(response);

            // When / Then
            mockMvc.perform(patch("/api/v1/personas/1/desactivar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).desactivar(1L);
        }

        @Test
        @DisplayName("Debe retornar 404 cuando persona no existe")
        void desactivar_conIdInexistente_retorna404() throws Exception {
            // Given
            when(service.desactivar(999L))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Persona no encontrada"));

            // When / Then
            mockMvc.perform(patch("/api/v1/personas/999/desactivar"))
                    .andExpect(status().isNotFound());

            verify(service).desactivar(999L);
        }
    }
}
