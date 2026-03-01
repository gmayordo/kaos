package com.kaos.planificacion.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.kaos.planificacion.dto.TareaAsignacionTimelineResponse;
import com.kaos.planificacion.entity.TareaAsignacionTimeline;

/**
 * Mapper para TareaAsignacionTimeline usando MapStruct.
 */
@Mapper(componentModel = "spring")
public interface TareaAsignacionTimelineMapper {

    @Mapping(target = "tareaId",       source = "tarea.id")
    @Mapping(target = "tareaTitulo",   source = "tarea.titulo")
    @Mapping(target = "tareaJiraKey",  source = "tarea.jiraKey")
    @Mapping(target = "personaId",     source = "persona.id")
    @Mapping(target = "personaNombre", source = "persona.nombre")
    @Mapping(target = "sprintId",      source = "sprint.id")
    @Mapping(target = "horasPorDia",   expression = "java(e.getHorasPorDia() != null ? e.getHorasPorDia().doubleValue() : null)")
    TareaAsignacionTimelineResponse toResponse(TareaAsignacionTimeline e);

    List<TareaAsignacionTimelineResponse> toResponseList(List<TareaAsignacionTimeline> list);
}
