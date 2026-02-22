package com.kaos.planificacion.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.kaos.planificacion.dto.BloqueoRequest;
import com.kaos.planificacion.dto.BloqueoResponse;
import com.kaos.planificacion.entity.Bloqueo;

/**
 * Mapper para Bloqueo usando MapStruct.
 * Convierte entre Entity, Request y Response DTOs.
 */
@Mapper(componentModel = "spring")
public interface BloqueoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaResolucion", ignore = true)
    @Mapping(target = "tareas", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Bloqueo toEntity(BloqueoRequest request);

    @Mapping(target = "responsableNombre", source = "responsable.nombre")
    @Mapping(target = "tareasAfectadas", expression = "java(bloqueo.getTareas() != null ? (long) bloqueo.getTareas().size() : 0L)")
    BloqueoResponse toResponse(Bloqueo bloqueo);

    List<BloqueoResponse> toResponseList(List<Bloqueo> bloqueos);

    @Mapping(target = "responsable", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaResolucion", ignore = true)
    @Mapping(target = "tareas", ignore = true)
    void updateEntity(BloqueoRequest request, @MappingTarget Bloqueo bloqueo);
}
