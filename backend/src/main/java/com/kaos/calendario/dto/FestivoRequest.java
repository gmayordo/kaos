package com.kaos.calendario.dto;

import java.time.LocalDate;
import com.kaos.calendario.entity.TipoFestivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request para crear o actualizar un festivo.
 */
public record FestivoRequest(
        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La descripción es obligatoria")
        @Size(max = 200, message = "La descripción no puede superar 200 caracteres")
        String descripcion,

        @NotNull(message = "El tipo es obligatorio")
        TipoFestivo tipo,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 100, message = "La ciudad no puede superar 100 caracteres")
        String ciudad
) {}
