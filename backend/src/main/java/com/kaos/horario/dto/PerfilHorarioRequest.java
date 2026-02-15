package com.kaos.horario.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request para crear o actualizar un perfil de horario.
 */
public record PerfilHorarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "La zona horaria es obligatoria")
        @Size(max = 50, message = "La zona horaria no puede superar 50 caracteres")
        String zonaHoraria,

        @NotNull(message = "Las horas del lunes son obligatorias")
        @DecimalMin(value = "0.0", message = "Las horas deben ser >= 0")
        BigDecimal horasLunes,

        @NotNull(message = "Las horas del martes son obligatorias")
        @DecimalMin(value = "0.0", message = "Las horas deben ser >= 0")
        BigDecimal horasMartes,

        @NotNull(message = "Las horas del miércoles son obligatorias")
        @DecimalMin(value = "0.0", message = "Las horas deben ser >= 0")
        BigDecimal horasMiercoles,

        @NotNull(message = "Las horas del jueves son obligatorias")
        @DecimalMin(value = "0.0", message = "Las horas deben ser >= 0")
        BigDecimal horasJueves,

        @NotNull(message = "Las horas del viernes son obligatorias")
        @DecimalMin(value = "0.0", message = "Las horas deben ser >= 0")
        BigDecimal horasViernes
) {}
