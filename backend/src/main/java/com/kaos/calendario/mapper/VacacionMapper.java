package com.kaos.calendario.mapper;

import java.util.List;
import org.mapstruct.*;
import com.kaos.calendario.dto.VacacionRequest;
import com.kaos.calendario.dto.VacacionResponse;
import com.kaos.calendario.entity.Vacacion;

/**
 * Mapper MapStruct para conversión entre {@link Vacacion} y sus DTOs.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface VacacionMapper {

    @Mapping(source = "persona.id", target = "personaId")
    @Mapping(source = "persona.nombre", target = "personaNombre")
    VacacionResponse toResponse(Vacacion entity);

    List<VacacionResponse> toResponseList(List<Vacacion> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "persona", ignore = true)
    @Mapping(target = "diasLaborables", ignore = true)
    Vacacion toEntity(VacacionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "persona", ignore = true)
    @Mapping(target = "diasLaborables", ignore = true)
    void updateEntity(VacacionRequest request, @MappingTarget Vacacion entity);
}
