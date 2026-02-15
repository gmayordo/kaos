package com.kaos.horario.mapper;

import java.util.List;
import org.mapstruct.*;
import com.kaos.horario.dto.PerfilHorarioRequest;
import com.kaos.horario.dto.PerfilHorarioResponse;
import com.kaos.horario.entity.PerfilHorario;

/**
 * Mapper MapStruct para conversión entre {@link PerfilHorario} y sus DTOs.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PerfilHorarioMapper {

    /** Entity → Response DTO. */
    PerfilHorarioResponse toResponse(PerfilHorario entity);

    /** Lista de Entity → Lista de Response DTO. */
    List<PerfilHorarioResponse> toResponseList(List<PerfilHorario> entities);

    /** Request DTO → Entity (para creación). */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "totalSemanal", ignore = true)
    PerfilHorario toEntity(PerfilHorarioRequest request);

    /** Actualiza entity existente con datos del request. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "totalSemanal", ignore = true)
    void updateEntity(PerfilHorarioRequest request, @MappingTarget PerfilHorario entity);
}
