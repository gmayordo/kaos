package com.kaos.squad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request para crear o actualizar un squad.
 */
public record SquadRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        String descripcion,

        @Size(max = 100, message = "El ID Jira correctivos no puede superar 100 caracteres")
        String idSquadCorrJira,

        @Size(max = 100, message = "El ID Jira evolutivos no puede superar 100 caracteres")
        String idSquadEvolJira
) {}
