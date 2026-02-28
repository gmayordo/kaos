package com.kaos.planificacion.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.kaos.planificacion.dto.TareaRequest;
import com.kaos.planificacion.dto.TareaResponse;
import com.kaos.planificacion.entity.Tarea;

/**
 * Mapper para Tarea usando Map Struct.
 * Convierte entre Entity, Request y Response DTOs.
 */
@Mapper(componentModel = "spring")
public interface TareaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "persona", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "bloqueadores", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Tarea toEntity(TareaRequest request);

    @Mapping(target = "personaNombre", source = "persona.nombre")
    @Mapping(target = "estado", expression = "java(tarea.getEstado().toString())")
    @Mapping(target = "diaCapacidadDisponible", ignore = true)
    @Mapping(target = "bloqueada", expression = "java(tarea.getBloqueadores() != null && !tarea.getBloqueadores().isEmpty())")
    @Mapping(target = "tareaParentId", source = "tareaParent.id")
    @Mapping(target = "jiraIssueSummary", source = "jiraIssue.summary")
    @Mapping(target = "jiraEstimacionHoras", source = "jiraIssue.estimacionHoras")
    TareaResponse toResponse(Tarea tarea);

    List<TareaResponse> toResponseList(List<Tarea> tareas);

    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "persona", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "bloqueadores", ignore = true)
    void updateEntity(TareaRequest request, @MappingTarget Tarea tarea);
}
