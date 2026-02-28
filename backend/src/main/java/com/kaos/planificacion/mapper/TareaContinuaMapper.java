package com.kaos.planificacion.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.kaos.planificacion.dto.TareaContinuaResponse;
import com.kaos.planificacion.entity.TareaContinua;

/**
 * Mapper para TareaContinua usando MapStruct.
 */
@Mapper(componentModel = "spring")
public interface TareaContinuaMapper {

    @Mapping(target = "squadId",       source = "squad.id")
    @Mapping(target = "squadNombre",   source = "squad.nombre")
    @Mapping(target = "personaId",     source = "persona.id")
    @Mapping(target = "personaNombre", source = "persona.nombre")
    @Mapping(target = "horasPorDia",   expression = "java(e.getHorasPorDia() != null ? e.getHorasPorDia().doubleValue() : null)")
    TareaContinuaResponse toResponse(TareaContinua e);

    List<TareaContinuaResponse> toResponseList(List<TareaContinua> list);
}
