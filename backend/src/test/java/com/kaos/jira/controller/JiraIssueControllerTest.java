package com.kaos.jira.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaos.jira.dto.JiraIssueResponse;
import com.kaos.jira.service.PlanificarIssueService;
import com.kaos.planificacion.dto.TareaResponse;
import com.kaos.planificacion.exception.SprintNoEnPlanificacionException;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests de integración para {@link JiraIssueController}.
 * Cubre los endpoints REST de planificación de issues Jira.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("JiraIssueController Integration Tests")
class JiraIssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlanificarIssueService planificarIssueService;

    // ── Builders ──────────────────────────────────────────────────────────────

    private JiraIssueResponse buildIssue(String key, String tipo) {
        return new JiraIssueResponse(
                1L, key, "Implementar " + key, tipo,
                "To Do", null, "dev@jira", null, null,
                BigDecimal.valueOf(8), BigDecimal.ZERO,
                null, false, null, new ArrayList<>()
        );
    }

    private TareaResponse buildTareaResponse(Long id, String jiraKey) {
        return new TareaResponse(
                id, "Tarea " + jiraKey, 10L, 5L, "Ana García",
                "HISTORIA", "EVOLUTIVO",
                BigDecimal.valueOf(8), "NORMAL",
                "PENDIENTE", 2, null, false,
                jiraKey,
                LocalDateTime.now(),
                null, null, null,
                null
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET /api/v1/jira/issues
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/jira/issues - Listar")
    class ListarTests {

        @Test
        @DisplayName("devuelve 200 con lista de issues cuando hay resultados")
        void listar_conIssues_retorna200() throws Exception {
            var issues = List.of(
                    buildIssue("RED-42", "Story"),
                    buildIssue("RED-43", "Bug")
            );
            when(planificarIssueService.listarIssuesPlanificables(anyLong(), anyLong(), anyBoolean()))
                    .thenReturn(issues);

            mockMvc.perform(get("/api/v1/jira/issues")
                            .param("squadId", "1")
                            .param("sprintId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].jiraKey").value("RED-42"))
                    .andExpect(jsonPath("$[1].tipoJira").value("Bug"));
        }

        @Test
        @DisplayName("devuelve 200 con lista vacía cuando no hay resultados")
        void listar_sinIssues_retorna200YListaVacia() throws Exception {
            when(planificarIssueService.listarIssuesPlanificables(anyLong(), anyLong(), anyBoolean()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/jira/issues")
                            .param("squadId", "1")
                            .param("sprintId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("soloSinTarea=false pasa correctamente al servicio")
        void listar_soloSinTareaFalse_propaga() throws Exception {
            when(planificarIssueService.listarIssuesPlanificables(1L, 10L, false))
                    .thenReturn(List.of(buildIssue("RED-44", "Task")));

            mockMvc.perform(get("/api/v1/jira/issues")
                            .param("squadId", "1")
                            .param("sprintId", "10")
                            .param("soloSinTarea", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].jiraKey").value("RED-44"));
        }

        @Test
        @DisplayName("falta parámetro squadId → 400")
        void listar_sinSquadId_retorna400() throws Exception {
            mockMvc.perform(get("/api/v1/jira/issues")
                            .param("sprintId", "10"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("incluye subtareas anidadas en el response")
        void listar_conSubtareas_incluyeSubtareas() throws Exception {
            var subtarea = new JiraIssueResponse(
                    2L, "RED-99", "Subtarea login", "Sub-task",
                    "To Do", null, null, null, null,
                    BigDecimal.valueOf(2), BigDecimal.ZERO,
                    "RED-42", false, null, new ArrayList<>()
            );
            var padre = new JiraIssueResponse(
                    1L, "RED-42", "Historia login", "Story",
                    "To Do", null, null, null, null,
                    BigDecimal.valueOf(8), BigDecimal.ZERO,
                    null, false, null, List.of(subtarea)
            );

            when(planificarIssueService.listarIssuesPlanificables(anyLong(), anyLong(), anyBoolean()))
                    .thenReturn(List.of(padre));

            mockMvc.perform(get("/api/v1/jira/issues")
                            .param("squadId", "1")
                            .param("sprintId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].subtareas.length()").value(1))
                    .andExpect(jsonPath("$[0].subtareas[0].jiraKey").value("RED-99"));
        }
    }

    // ══════════════════════════════════════════════════════════
    // POST /api/v1/jira/issues/planificar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/jira/issues/planificar - Planificar")
    class PlanificarTests {

        private String validRequestJson() {
            return """
                    {
                      "sprintId": 10,
                      "asignaciones": [
                        {
                          "jiraKey": "RED-42",
                          "personaId": 5,
                          "estimacion": 8.0,
                          "diaAsignado": 2
                        }
                      ]
                    }
                    """;
        }

        @Test
        @DisplayName("planificación exitosa retorna 201 con tareas creadas")
        void planificar_valido_retorna201() throws Exception {
            var tareas = List.of(buildTareaResponse(200L, "RED-42"));
            when(planificarIssueService.planificar(any())).thenReturn(tareas);

            mockMvc.perform(post("/api/v1/jira/issues/planificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(200))
                    .andExpect(jsonPath("$[0].referenciaJira").value("RED-42"));
        }

        @Test
        @DisplayName("body vacío → 400")
        void planificar_bodyVacio_retorna400() throws Exception {
            mockMvc.perform(post("/api/v1/jira/issues/planificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("asignaciones vacías → 400 (validación @NotEmpty)")
        void planificar_asignacionesVacias_retorna400() throws Exception {
            String body = """
                    { "sprintId": 10, "asignaciones": [] }
                    """;

            mockMvc.perform(post("/api/v1/jira/issues/planificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("sprint no encontrado → 404")
        void planificar_sprintNoEncontrado_retorna404() throws Exception {
            when(planificarIssueService.planificar(any()))
                    .thenThrow(new EntityNotFoundException("Sprint no encontrado con id: 10"));

            mockMvc.perform(post("/api/v1/jira/issues/planificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("sprint no planificable → 422 (UNPROCESSABLE_ENTITY)")
        void planificar_sprintNoPlanificable_retorna422() throws Exception {
            when(planificarIssueService.planificar(any()))
                    .thenThrow(new SprintNoEnPlanificacionException(10L, "CERRADO"));

            mockMvc.perform(post("/api/v1/jira/issues/planificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("plantilla no encontrada sin estimación → 422")
        void planificar_issueYaTieneTarea_retorna409() throws Exception {
            when(planificarIssueService.planificar(any()))
                    .thenThrow(new IllegalStateException("El issue RED-42 ya tiene una tarea KAOS asignada"));

            mockMvc.perform(post("/api/v1/jira/issues/planificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("múltiples issues planificados retorna todos en 201")
        void planificar_multipleIssues_retornaTodos() throws Exception {
            var tareas = List.of(
                    buildTareaResponse(200L, "RED-42"),
                    buildTareaResponse(201L, "RED-43")
            );
            when(planificarIssueService.planificar(any())).thenReturn(tareas);

            String body = """
                    {
                      "sprintId": 10,
                      "asignaciones": [
                        { "jiraKey": "RED-42", "personaId": 5, "estimacion": 8.0, "diaAsignado": 2 },
                        { "jiraKey": "RED-43", "personaId": 5, "estimacion": 4.0, "diaAsignado": 3 }
                      ]
                    }
                    """;

            mockMvc.perform(post("/api/v1/jira/issues/planificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].referenciaJira").value("RED-42"))
                    .andExpect(jsonPath("$[1].referenciaJira").value("RED-43"));
        }
    }
}
