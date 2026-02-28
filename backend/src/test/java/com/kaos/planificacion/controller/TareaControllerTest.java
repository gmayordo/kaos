package com.kaos.planificacion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaos.planificacion.dto.TareaRequest;
import com.kaos.planificacion.dto.TareaResponse;
import com.kaos.planificacion.entity.EstadoTarea;
import com.kaos.planificacion.exception.CapacidadInsuficienteException;
import com.kaos.planificacion.exception.TareaNoEnPendienteException;
import com.kaos.planificacion.service.TareaService;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests de integración para {@link TareaController}.
 * Cubre CA-15, CA-16 (endpoints REST).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TareaController Integration Tests")
class TareaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TareaService tareaService;

    private TareaResponse buildTarea(Long id, String estado) {
        return new TareaResponse(
                id, "Tarea " + id, 1L, 1L, "Juan Pérez",
                "HISTORIA", "EVOLUTIVO",
                BigDecimal.valueOf(4), "NORMAL",
                estado, 3, 6.0, false,
                "KAOS-" + id,
                LocalDateTime.now(),
                null, null, null
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/tareas
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/tareas - Listar")
    class ListarTests {

        @Test
        @DisplayName("Listar sin filtros retorna 200 con page")
        void listar_sinFiltros_retorna200() throws Exception {
            var tareas = List.of(buildTarea(1L, "PENDIENTE"), buildTarea(2L, "EN_PROGRESO"));
            when(tareaService.listar(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(tareas));

            mockMvc.perform(get("/api/v1/tareas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].titulo").value("Tarea 1"))
                    .andExpect(jsonPath("$.content[1].estado").value("EN_PROGRESO"));
        }

        @Test
        @DisplayName("Listar con filtro sprintId retorna 200")
        void listar_conSprintId_retorna200() throws Exception {
            var tareas = List.of(buildTarea(1L, "PENDIENTE"));
            when(tareaService.listar(eq(1L), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(tareas));

            mockMvc.perform(get("/api/v1/tareas?sprintId=1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/tareas/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/tareas/{id} - Obtener")
    class ObtenerTests {

        @Test
        @DisplayName("Obtener tarea existente retorna 200")
        void obtener_tareaExistente_retorna200() throws Exception {
            when(tareaService.obtener(1L)).thenReturn(buildTarea(1L, "PENDIENTE"));

            mockMvc.perform(get("/api/v1/tareas/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                    .andExpect(jsonPath("$.tipo").value("HISTORIA"));
        }

        @Test
        @DisplayName("Obtener tarea inexistente retorna 404")
        void obtener_tareaInexistente_retorna404() throws Exception {
            when(tareaService.obtener(99L))
                    .thenThrow(new EntityNotFoundException("Tarea no encontrada con id: 99"));

            mockMvc.perform(get("/api/v1/tareas/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    // ══════════════════════════════════════════════════════════
    // POST /api/v1/tareas
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/tareas - Crear")
    class CrearTests {

        @Test
        @DisplayName("CA-15: Crear tarea válida retorna 201")
        void crear_tareaValida_retorna201() throws Exception {
            var request = new TareaRequest("Autenticación OAuth", 1L, null, "HISTORIA", "EVOLUTIVO",
                    BigDecimal.valueOf(4), "NORMAL", 1L, 3, "KAOS-42", null);
            when(tareaService.crear(any())).thenReturn(buildTarea(1L, "PENDIENTE"));

            mockMvc.perform(post("/api/v1/tareas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE"));

            verify(tareaService).crear(any());
        }

        @Test
        @DisplayName("Crear tarea sin título retorna 400")
        void crear_sinTitulo_retorna400() throws Exception {
            var bodyInvalido = "{\"sprintId\":1,\"tipo\":\"HISTORIA\",\"categoria\":\"EVOLUTIVO\",\"estimacion\":4,\"prioridad\":\"NORMAL\"}";

            mockMvc.perform(post("/api/v1/tareas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyInvalido))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("CA-16: Crear tarea con capacidad excedida retorna 409")
        void crear_capacidadExcedida_retorna409() throws Exception {
            var request = new TareaRequest("Tarea pesada", 1L, null, "TAREA", "CORRECTIVO",
                    BigDecimal.valueOf(12), "ALTA", 1L, 3, null, null);
            when(tareaService.crear(any()))
                    .thenThrow(new CapacidadInsuficienteException(
                            "Capacidad insuficiente", 1L, 3, 2.0, 12.0));

            mockMvc.perform(post("/api/v1/tareas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CAPACIDAD_INSUFICIENTE"));
        }
    }

    // ══════════════════════════════════════════════════════════
    // PATCH /api/v1/tareas/{id}/estado
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/v1/tareas/{id}/estado - Cambiar Estado")
    class CambiarEstadoTests {

        @Test
        @DisplayName("Iniciar tarea PENDIENTE → EN_PROGRESO retorna 200")
        void cambiarEstado_pendienteAEnProgreso_retorna200() throws Exception {
            when(tareaService.cambiarEstado(1L, EstadoTarea.EN_PROGRESO))
                    .thenReturn(buildTarea(1L, "EN_PROGRESO"));

            mockMvc.perform(patch("/api/v1/tareas/1/estado?estado=EN_PROGRESO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("EN_PROGRESO"));
        }

        @Test
        @DisplayName("Cambiar estado de tarea inexistente retorna 404")
        void cambiarEstado_tareaInexistente_retorna404() throws Exception {
            when(tareaService.cambiarEstado(99L, EstadoTarea.EN_PROGRESO))
                    .thenThrow(new EntityNotFoundException("Tarea no encontrada con id: 99"));

            mockMvc.perform(patch("/api/v1/tareas/99/estado?estado=EN_PROGRESO"))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // DELETE /api/v1/tareas/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/tareas/{id} - Eliminar")
    class EliminarTests {

        @Test
        @DisplayName("Eliminar tarea en PENDIENTE retorna 204")
        void eliminar_enPendiente_retorna204() throws Exception {
            doNothing().when(tareaService).eliminar(1L);

            mockMvc.perform(delete("/api/v1/tareas/1"))
                    .andExpect(status().isNoContent());

            verify(tareaService).eliminar(1L);
        }

        @Test
        @DisplayName("Eliminar tarea no en PENDIENTE retorna 422")
        void eliminar_noEnPendiente_retorna422() throws Exception {
            doThrow(new TareaNoEnPendienteException(1L, "EN_PROGRESO"))
                    .when(tareaService).eliminar(1L);

            mockMvc.perform(delete("/api/v1/tareas/1"))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
