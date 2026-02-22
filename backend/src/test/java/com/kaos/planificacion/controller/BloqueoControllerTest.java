package com.kaos.planificacion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import com.kaos.planificacion.dto.BloqueoRequest;
import com.kaos.planificacion.dto.BloqueoResponse;
import com.kaos.planificacion.entity.EstadoBloqueo;
import com.kaos.planificacion.service.BloqueoService;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests de integración para {@link BloqueoController}.
 * Cubre CA-20 (bloqueos e impedimentos).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BloqueoController Integration Tests")
class BloqueoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BloqueoService bloqueoService;

    private BloqueoResponse buildBloqueo(Long id, String estado) {
        return new BloqueoResponse(
                id, "Bloqueo " + id,
                "Descripción del impedimento",
                "TECNICO",
                estado,
                1L, "Juan Pérez",
                estado.equals("RESUELTO") ? LocalDateTime.now() : null,
                null, 2L,
                LocalDateTime.now()
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/bloqueos
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/bloqueos - Listar")
    class ListarTests {

        @Test
        @DisplayName("Listar sin filtros retorna 200")
        void listar_sinFiltros_retorna200() throws Exception {
            var bloqueos = List.of(buildBloqueo(1L, "ABIERTO"), buildBloqueo(2L, "EN_GESTION"));
            when(bloqueoService.listar(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(bloqueos));

            mockMvc.perform(get("/api/v1/bloqueos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].tipo").value("TECNICO"))
                    .andExpect(jsonPath("$.content[0].estado").value("ABIERTO"));
        }

        @Test
        @DisplayName("Listar con filtro ACTIVO retorna 200")
        void listar_conFiltroActivo_retorna200() throws Exception {
            var bloqueos = List.of(buildBloqueo(1L, "ABIERTO"));
            when(bloqueoService.listar(EstadoBloqueo.ABIERTO, org.springframework.data.domain.PageRequest.of(0, 20)))
                    .thenReturn(new PageImpl<>(bloqueos));

            mockMvc.perform(get("/api/v1/bloqueos?estado=ABIERTO"))
                    .andExpect(status().isOk());
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/bloqueos/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/bloqueos/{id} - Obtener")
    class ObtenerTests {

        @Test
        @DisplayName("Obtener bloqueo existente retorna 200")
        void obtener_bloqueoExistente_retorna200() throws Exception {
            when(bloqueoService.obtener(1L)).thenReturn(buildBloqueo(1L, "ABIERTO"));

            mockMvc.perform(get("/api/v1/bloqueos/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.estado").value("ABIERTO"))
                    .andExpect(jsonPath("$.tipo").value("TECNICO"));
        }

        @Test
        @DisplayName("Obtener bloqueo inexistente retorna 404")
        void obtener_bloqueoInexistente_retorna404() throws Exception {
            when(bloqueoService.obtener(99L))
                    .thenThrow(new EntityNotFoundException("Bloqueo no encontrado con id: 99"));

            mockMvc.perform(get("/api/v1/bloqueos/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ══════════════════════════════════════════════════════════
    // POST /api/v1/bloqueos
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/bloqueos - Crear")
    class CrearTests {

        @Test
        @DisplayName("CA-20: Crear bloqueo válido retorna 201")
        void crear_bloqueoValido_retorna201() throws Exception {
            var request = new BloqueoRequest("Dependencia BD Oracle", "Falta acceso", "TECNICO", null, 1L, null);
            when(bloqueoService.crear(any())).thenReturn(buildBloqueo(1L, "ABIERTO"));

            mockMvc.perform(post("/api/v1/bloqueos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.estado").value("ABIERTO"));

            verify(bloqueoService).crear(any());
        }

        @Test
        @DisplayName("Crear bloqueo sin título retorna 400")
        void crear_sinTitulo_retorna400() throws Exception {
            var bodyInvalido = "{\"tipo\":\"TECNICO\"}";

            mockMvc.perform(post("/api/v1/bloqueos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyInvalido))
                    .andExpect(status().isBadRequest());
        }
    }

    // ══════════════════════════════════════════════════════════
    // PATCH /api/v1/bloqueos/{id}/estado
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/v1/bloqueos/{id}/estado - Cambiar Estado")
    class CambiarEstadoTests {

        @Test
        @DisplayName("Resolver bloqueo ACTIVO → RESUELTO retorna 200")
        void cambiarEstado_activoAResuelto_retorna200() throws Exception {
            when(bloqueoService.cambiarEstado(1L, EstadoBloqueo.RESUELTO))
                    .thenReturn(buildBloqueo(1L, "RESUELTO"));

            mockMvc.perform(patch("/api/v1/bloqueos/1/estado?estado=RESUELTO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("RESUELTO"))
                    .andExpect(jsonPath("$.fechaResolucion").isNotEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/bloqueos/activos/count
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/bloqueos/activos/count - Contar Activos")
    class ContarActivosTests {

        @Test
        @DisplayName("CA-20: Contar bloqueos activos retorna 200 con número")
        void contarActivos_retorna200() throws Exception {
            when(bloqueoService.contarActivos()).thenReturn(3L);

            mockMvc.perform(get("/api/v1/bloqueos/activos/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(3));
        }
    }

    // ══════════════════════════════════════════════════════════
    // DELETE /api/v1/bloqueos/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/bloqueos/{id} - Eliminar")
    class EliminarTests {

        @Test
        @DisplayName("Eliminar bloqueo existente retorna 204")
        void eliminar_bloqueoExistente_retorna204() throws Exception {
            doNothing().when(bloqueoService).eliminar(1L);

            mockMvc.perform(delete("/api/v1/bloqueos/1"))
                    .andExpect(status().isNoContent());

            verify(bloqueoService).eliminar(1L);
        }

        @Test
        @DisplayName("Eliminar bloqueo inexistente retorna 404")
        void eliminar_bloqueoInexistente_retorna404() throws Exception {
            org.mockito.Mockito.doThrow(new EntityNotFoundException("Bloqueo no encontrado con id: 99"))
                    .when(bloqueoService).eliminar(99L);

            mockMvc.perform(delete("/api/v1/bloqueos/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
