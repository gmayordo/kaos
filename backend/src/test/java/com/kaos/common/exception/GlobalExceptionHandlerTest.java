package com.kaos.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests para GlobalExceptionHandler.
 * Valida el manejo centralizado de excepciones en todos los endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("EntityNotFoundException - 404 NOT_FOUND")
    class EntityNotFoundTests {

        @Test
        @DisplayName("Debe retornar 404 cuando recurso no existe")
        void handleNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/personas/99999"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value(containsString("Persona no encontrada")))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException - 400 VALIDATION_ERROR")
    class ValidationTests {

        @Test
        @DisplayName("Debe retornar 400 cuando request tiene campos inválidos")
        void handleValidation() throws Exception {
            String invalidRequest = """
                {
                    "nombre": "",
                    "email": "invalid-email",
                    "rol": "DEVELOPER",
                    "seniority": "JUNIOR",
                    "costeHora": -10.5,
                    "fechaIncorporacion": "2024-01-15"
                }
                """;

            mockMvc.perform(post("/api/v1/personas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value("Error de validación"))
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException - 400 BAD_REQUEST")
    class IllegalArgumentTests {

        @Test
        @DisplayName("Debe retornar 400 con parámetros de paginación inválidos")
        void handleIllegalArgument() throws Exception {
            // Provocar IllegalArgumentException con page/size negativos
            mockMvc.perform(get("/api/v1/personas")
                            .param("page", "-1")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}
