package com.kaos.calendario.dto;

import com.kaos.calendario.entity.TipoFestivo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

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

        @NotEmpty(message = "Debe asignar al menos una persona")
        List<Long> personaIds
) {}
