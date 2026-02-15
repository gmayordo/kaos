package com.kaos.dedicacion.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.mapstruct.*;
import com.kaos.dedicacion.dto.SquadMemberRequest;
import com.kaos.dedicacion.dto.SquadMemberResponse;
import com.kaos.dedicacion.entity.SquadMember;

/**
 * Mapper MapStruct para conversión entre {@link SquadMember} y sus DTOs.
 * Calcula la capacidad diaria: horas_perfil_dia × porcentaje / 100.
 */
@Mapper(componentModel = "spring")
public interface SquadMemberMapper {

    @Mapping(source = "persona.id", target = "personaId")
    @Mapping(source = "persona.nombre", target = "personaNombre")
    @Mapping(source = "squad.id", target = "squadId")
    @Mapping(source = "squad.nombre", target = "squadNombre")
    @Mapping(target = "capacidadDiariaLunes", expression = "java(calcularCapacidad(entity.getPersona().getPerfilHorario().getHorasLunes(), entity.getPorcentaje()))")
    @Mapping(target = "capacidadDiariaMartes", expression = "java(calcularCapacidad(entity.getPersona().getPerfilHorario().getHorasMartes(), entity.getPorcentaje()))")
    @Mapping(target = "capacidadDiariaMiercoles", expression = "java(calcularCapacidad(entity.getPersona().getPerfilHorario().getHorasMiercoles(), entity.getPorcentaje()))")
    @Mapping(target = "capacidadDiariaJueves", expression = "java(calcularCapacidad(entity.getPersona().getPerfilHorario().getHorasJueves(), entity.getPorcentaje()))")
    @Mapping(target = "capacidadDiariaViernes", expression = "java(calcularCapacidad(entity.getPersona().getPerfilHorario().getHorasViernes(), entity.getPorcentaje()))")
    SquadMemberResponse toResponse(SquadMember entity);

    List<SquadMemberResponse> toResponseList(List<SquadMember> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "persona", ignore = true)
    @Mapping(target = "squad", ignore = true)
    SquadMember toEntity(SquadMemberRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "persona", ignore = true)
    @Mapping(target = "squad", ignore = true)
    void updateEntity(SquadMemberRequest request, @MappingTarget SquadMember entity);

    /**
     * Calcula la capacidad diaria: horas × porcentaje / 100.
     */
    default BigDecimal calcularCapacidad(BigDecimal horasDia, Integer porcentaje) {
        if (horasDia == null || porcentaje == null) return BigDecimal.ZERO;
        return horasDia.multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
