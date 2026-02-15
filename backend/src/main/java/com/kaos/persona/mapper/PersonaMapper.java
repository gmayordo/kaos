package com.kaos.persona.mapper;

import java.util.List;
import org.mapstruct.*;
import com.kaos.persona.dto.PersonaRequest;
import com.kaos.persona.dto.PersonaResponse;
import com.kaos.persona.entity.Persona;

/**
 * Mapper MapStruct para conversión entre {@link Persona} y sus DTOs.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PersonaMapper {

    @Mapping(source = "perfilHorario.id", target = "perfilHorarioId")
    @Mapping(source = "perfilHorario.nombre", target = "perfilHorarioNombre")
    PersonaResponse toResponse(Persona entity);

    List<PersonaResponse> toResponseList(List<Persona> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "perfilHorario", ignore = true)
    Persona toEntity(PersonaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "perfilHorario", ignore = true)
    void updateEntity(PersonaRequest request, @MappingTarget Persona entity);
}
