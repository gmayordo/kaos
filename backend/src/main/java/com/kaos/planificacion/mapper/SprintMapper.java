package com.kaos.planificacion.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.kaos.planificacion.dto.SprintRequest;
import com.kaos.planificacion.dto.SprintResponse;
import com.kaos.planificacion.entity.Sprint;

/**
 * Mapper para Sprint usando MapStruct.
 * Convierte entre Entity, Request y Response DTOs.
 */
@Mapper(componentModel = "spring")
public interface SprintMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "squad", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaFin", ignore = true)
    @Mapping(target = "tareas", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Sprint toEntity(SprintRequest request);

    @Mapping(target = "squadId", source = "squad.id")
    @Mapping(target = "squadNombre", source = "squad.nombre")
    @Mapping(target = "estado", expression = "java(sprint.getEstado().toString())")
    @Mapping(target = "tareasPendientes", ignore = true)
    @Mapping(target = "tareasEnProgreso", ignore = true)
    @Mapping(target = "tareasCompletadas", ignore = true)
    SprintResponse toResponse(Sprint sprint);

    List<SprintResponse> toResponseList(List<Sprint> sprints);

    @Mapping(target = "squad", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaFin", ignore = true)
    @Mapping(target = "tareas", ignore = true)
    void updateEntity(SprintRequest request, @MappingTarget Sprint sprint);
}
