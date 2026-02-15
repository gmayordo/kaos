package com.kaos.persona.dto;

import com.kaos.persona.entity.Seniority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request para crear o actualizar una persona.
 */
public record PersonaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener formato válido")
        @Size(max = 200, message = "El email no puede superar 200 caracteres")
        String email,

        @Size(max = 100, message = "El ID Jira no puede superar 100 caracteres")
        String idJira,

        @NotNull(message = "El perfil de horario es obligatorio")
        Long perfilHorarioId,

        Seniority seniority,

        String skills,

        BigDecimal costeHora,

        LocalDate fechaIncorporacion,

        Boolean sendNotifications
) {}
