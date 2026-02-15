package com.kaos.squad.mapper;

import java.util.List;
import org.mapstruct.*;
import com.kaos.squad.dto.SquadRequest;
import com.kaos.squad.dto.SquadResponse;
import com.kaos.squad.entity.Squad;

/**
 * Mapper MapStruct para conversión entre {@link Squad} y sus DTOs.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SquadMapper {

    SquadResponse toResponse(Squad entity);

    List<SquadResponse> toResponseList(List<Squad> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Squad toEntity(SquadRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "estado", ignore = true)
    void updateEntity(SquadRequest request, @MappingTarget Squad entity);
}
