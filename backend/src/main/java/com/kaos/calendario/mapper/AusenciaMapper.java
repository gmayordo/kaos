package com.kaos.calendario.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import com.kaos.calendario.dto.AusenciaRequest;
import com.kaos.calendario.dto.AusenciaResponse;
import com.kaos.calendario.entity.Ausencia;

/**
 * Mapper MapStruct para conversión entre {@link Ausencia} y sus DTOs.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AusenciaMapper {

    @Mapping(source = "persona.id", target = "personaId")
    @Mapping(source = "persona.nombre", target = "personaNombre")
    AusenciaResponse toResponse(Ausencia entity);

    List<AusenciaResponse> toResponseList(List<Ausencia> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "persona", ignore = true)
    Ausencia toEntity(AusenciaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "persona", ignore = true)
    void updateEntity(AusenciaRequest request, @MappingTarget Ausencia entity);
}
