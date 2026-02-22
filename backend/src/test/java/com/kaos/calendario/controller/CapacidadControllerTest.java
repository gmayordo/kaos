package com.kaos.calendario.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.kaos.calendario.dto.CapacidadDiaResponse;
import com.kaos.calendario.dto.CapacidadPersonaResponse;
import com.kaos.calendario.dto.CapacidadSquadResponse;
import com.kaos.calendario.entity.MotivoReduccion;
import com.kaos.calendario.service.CapacidadService;

/**
 * Tests de integración para {@link CapacidadController}.
 * Valida endpoint de cálculo de capacidad con MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CapacidadController Integration Tests")
class CapacidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CapacidadService service;

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/capacidad/squad/{id}
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/capacidad/squad/{id} - Calcular")
    class CalcularTests {

        @Test
        @DisplayName("GET con parámetros válidos retorna 200 con estructura completa")
        void calcularCapacidad_parametrosValidos_retorna200() throws Exception {
            // given
            LocalDate inicio = LocalDate.of(2026, 3, 2); // Lunes
            LocalDate fin = LocalDate.of(2026, 3, 6);    // Viernes

            CapacidadDiaResponse dia1 = new CapacidadDiaResponse(inicio, 8.0, 8.0, 100, null);
            CapacidadDiaResponse dia2 = new CapacidadDiaResponse(inicio.plusDays(1), 8.0, 8.0, 100, null);

            CapacidadPersonaResponse persona1 = new CapacidadPersonaResponse(
                    1L,
                    "Juan Pérez",
                    40.0, // 5 días × 8h
                    List.of(dia1, dia2)
            );

            CapacidadSquadResponse response = new CapacidadSquadResponse(
                    1L,
                    "Squad Alpha",
                    inicio,
                    fin,
                    40.0,
                    List.of(persona1)
            );

            when(service.calcularCapacidad(1L, inicio, fin)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaInicio", "2026-03-02")
                            .param("fechaFin", "2026-03-06"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.squadId").value(1))
                    .andExpect(jsonPath("$.squadNombre").value("Squad Alpha"))
                    .andExpect(jsonPath("$.horasTotales").value(40.0))
                    .andExpect(jsonPath("$.personas").isArray())
                    .andExpect(jsonPath("$.personas.length()").value(1))
                    .andExpect(jsonPath("$.personas[0].horasTotales").value(40.0))
                    .andExpect(jsonPath("$.personas[0].dias").isArray());

            verify(service).calcularCapacidad(1L, inicio, fin);
        }

        @Test
        @DisplayName("GET sin fechaInicio retorna 400")
        void calcularCapacidad_sinFechaInicio_retorna400() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaFin", "2026-03-31"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET sin fechaFin retorna 400")
        void calcularCapacidad_sinFechaFin_retorna400() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaInicio", "2026-03-01"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET con fechaFin anterior a fechaInicio retorna 400")
        void calcularCapacidad_fechaFinAnterior_retorna400() throws Exception {
            // given
            when(service.calcularCapacidad(eq(1L), any(), any()))
                    .thenThrow(new IllegalArgumentException("fecha de fin debe ser posterior o igual a fecha de inicio"));

            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaInicio", "2026-03-10")
                            .param("fechaFin", "2026-03-01"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET con squad inexistente retorna 404")
        void calcularCapacidad_squadInexistente_retorna404() throws Exception {
            // given
            when(service.calcularCapacidad(eq(999L), any(), any()))
                    .thenThrow(new IllegalArgumentException("Squad no encontrado: 999"));

            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/999")
                            .param("fechaInicio", "2026-03-01")
                            .param("fechaFin", "2026-03-31"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET día normal retorna horas completas sin reducción")
        void calcularCapacidad_diaNormal_horasCompletas() throws Exception {
            // given
            LocalDate lunes = LocalDate.of(2026, 3, 2);

            CapacidadDiaResponse dia = new CapacidadDiaResponse(lunes, 8.0, 8.0, 100, null);
            CapacidadPersonaResponse persona = new CapacidadPersonaResponse(1L, "Juan", 8.0, List.of(dia));
            CapacidadSquadResponse response = new CapacidadSquadResponse(1L, "Squad", lunes, lunes, 8.0, List.of(persona));

            when(service.calcularCapacidad(1L, lunes, lunes)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaInicio", "2026-03-02")
                            .param("fechaFin", "2026-03-02"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horasTotales").value(8.0))
                    .andExpect(jsonPath("$.personas[0].dias[0].horasDisponibles").value(8.0))
                    .andExpect(jsonPath("$.personas[0].dias[0].porcentaje").value(100))
                    .andExpect(jsonPath("$.personas[0].dias[0].motivoReduccion").isEmpty()); // null
        }

        @Test
        @DisplayName("GET fin de semana retorna 0 horas con motivo FIN_SEMANA")
        void calcularCapacidad_finDeSemana_retorna0Horas() throws Exception {
            // given
            LocalDate sabado = LocalDate.of(2026, 3, 7);

            CapacidadDiaResponse dia = new CapacidadDiaResponse(sabado, 0.0, 8.0, 0, MotivoReduccion.FIN_SEMANA);
            CapacidadPersonaResponse persona = new CapacidadPersonaResponse(1L, "Juan", 0.0, List.of(dia));
            CapacidadSquadResponse response = new CapacidadSquadResponse(1L, "Squad", sabado, sabado, 0.0, List.of(persona));

            when(service.calcularCapacidad(1L, sabado, sabado)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaInicio", "2026-03-07")
                            .param("fechaFin", "2026-03-07"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horasTotales").value(0.0))
                    .andExpect(jsonPath("$.personas[0].dias[0].horasDisponibles").value(0.0))
                    .andExpect(jsonPath("$.personas[0].dias[0].motivoReduccion").value("FIN_SEMANA"));
        }

        @Test
        @DisplayName("GET día con festivo retorna 0 horas con motivo FESTIVO")
        void calcularCapacidad_diaFestivo_retorna0Horas() throws Exception {
            // given
            LocalDate anioNuevo = LocalDate.of(2026, 1, 1);

            CapacidadDiaResponse dia = new CapacidadDiaResponse(anioNuevo, 0.0, 8.0, 0, MotivoReduccion.FESTIVO);
            CapacidadPersonaResponse persona = new CapacidadPersonaResponse(1L, "Juan", 0.0, List.of(dia));
            CapacidadSquadResponse response = new CapacidadSquadResponse(1L, "Squad", anioNuevo, anioNuevo, 0.0, List.of(persona));

            when(service.calcularCapacidad(1L, anioNuevo, anioNuevo)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaInicio", "2026-01-01")
                            .param("fechaFin", "2026-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personas[0].dias[0].motivoReduccion").value("FESTIVO"));
        }

        @Test
        @DisplayName("GET día con vacación retorna 0 horas con motivo VACACION")
        void calcularCapacidad_diaVacacion_retorna0Horas() throws Exception {
            // given
            LocalDate fecha = LocalDate.of(2026, 7, 15);

            CapacidadDiaResponse dia = new CapacidadDiaResponse(fecha, 0.0, 8.0, 0, MotivoReduccion.VACACION);
            CapacidadPersonaResponse persona = new CapacidadPersonaResponse(1L, "Juan", 0.0, List.of(dia));
            CapacidadSquadResponse response = new CapacidadSquadResponse(1L, "Squad", fecha, fecha, 0.0, List.of(persona));

            when(service.calcularCapacidad(1L, fecha, fecha)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaInicio", "2026-07-15")
                            .param("fechaFin", "2026-07-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personas[0].dias[0].motivoReduccion").value("VACACION"));
        }

        @Test
        @DisplayName("GET día con ausencia retorna 0 horas con motivo AUSENCIA")
        void calcularCapacidad_diaAusencia_retorna0Horas() throws Exception {
            // given
            LocalDate fecha = LocalDate.of(2026, 3, 10);

            CapacidadDiaResponse dia = new CapacidadDiaResponse(fecha, 0.0, 8.0, 0, MotivoReduccion.AUSENCIA);
            CapacidadPersonaResponse persona = new CapacidadPersonaResponse(1L, "Juan", 0.0, List.of(dia));
            CapacidadSquadResponse response = new CapacidadSquadResponse(1L, "Squad", fecha, fecha, 0.0, List.of(persona));

            when(service.calcularCapacidad(1L, fecha, fecha)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaInicio", "2026-03-10")
                            .param("fechaFin", "2026-03-10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personas[0].dias[0].motivoReduccion").value("AUSENCIA"));
        }

        @Test
        @DisplayName("GET rango 1 semana retorna 5 días laborables")
        void calcularCapacidad_rangoSemana_retorna5Dias() throws Exception {
            // given
            LocalDate lunes = LocalDate.of(2026, 3, 2);
            LocalDate domingo = LocalDate.of(2026, 3, 8);

            // 5 días laborables + 2 finde
            List<CapacidadDiaResponse> dias = List.of(
                    new CapacidadDiaResponse(lunes, 8.0, 8.0, 100, null),
                    new CapacidadDiaResponse(lunes.plusDays(1), 8.0, 8.0, 100, null),
                    new CapacidadDiaResponse(lunes.plusDays(2), 8.0, 8.0, 100, null),
                    new CapacidadDiaResponse(lunes.plusDays(3), 8.0, 8.0, 100, null),
                    new CapacidadDiaResponse(lunes.plusDays(4), 8.0, 8.0, 100, null),
                    new CapacidadDiaResponse(lunes.plusDays(5), 0.0, 8.0, 0, MotivoReduccion.FIN_SEMANA),
                    new CapacidadDiaResponse(domingo, 0.0, 8.0, 0, MotivoReduccion.FIN_SEMANA)
            );

            CapacidadPersonaResponse persona = new CapacidadPersonaResponse(1L, "Juan", 40.0, dias);
            CapacidadSquadResponse response = new CapacidadSquadResponse(1L, "Squad", lunes, domingo, 40.0, List.of(persona));

            when(service.calcularCapacidad(1L, lunes, domingo)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaInicio", "2026-03-02")
                            .param("fechaFin", "2026-03-08"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horasTotales").value(40.0))
                    .andExpect(jsonPath("$.personas[0].dias.length()").value(7))
                    .andExpect(jsonPath("$.personas[0].horasTotales").value(40.0));
        }

        @Test
        @DisplayName("GET squad con 3 personas suma capacidades")
        void calcularCapacidad_multiPersona_sumaCapacidades() throws Exception {
            // given
            LocalDate fecha = LocalDate.of(2026, 3, 2);

            CapacidadDiaResponse dia1 = new CapacidadDiaResponse(fecha, 8.0, 8.0, 100, null);
            CapacidadDiaResponse dia2 = new CapacidadDiaResponse(fecha, 8.0, 8.0, 100, null);
            CapacidadDiaResponse dia3 = new CapacidadDiaResponse(fecha, 8.0, 8.0, 100, null);

            List<CapacidadPersonaResponse> personas = List.of(
                    new CapacidadPersonaResponse(1L, "Juan", 8.0, List.of(dia1)),
                    new CapacidadPersonaResponse(2L, "María", 8.0, List.of(dia2)),
                    new CapacidadPersonaResponse(3L, "Pedro", 8.0, List.of(dia3))
            );

            CapacidadSquadResponse response = new CapacidadSquadResponse(1L, "Squad", fecha, fecha, 24.0, personas);

            when(service.calcularCapacidad(1L, fecha, fecha)).thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/capacidad/squad/1")
                            .param("fechaInicio", "2026-03-02")
                            .param("fechaFin", "2026-03-02"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horasTotales").value(24.0))
                    .andExpect(jsonPath("$.personas.length()").value(3));
        }
    }
}
