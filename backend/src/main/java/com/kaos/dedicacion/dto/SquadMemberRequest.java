package com.kaos.dedicacion.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.kaos.persona.entity.Rol;

/**
 * Request para asignar o actualizar dedicación de persona en squad.
 */
public record SquadMemberRequest(
        @NotNull(message = "El ID de persona es obligatorio")
        Long personaId,

        @NotNull(message = "El ID de squad es obligatorio")
        Long squadId,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        @NotNull(message = "El porcentaje es obligatorio")
        @Min(value = 0, message = "El porcentaje mínimo es 0")
        @Max(value = 100, message = "El porcentaje máximo es 100")
        Integer porcentaje,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        LocalDate fechaFin
) {}
