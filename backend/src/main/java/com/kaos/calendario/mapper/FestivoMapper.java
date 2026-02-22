package com.kaos.calendario.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import com.kaos.calendario.dto.FestivoRequest;
import com.kaos.calendario.dto.FestivoResponse;
import com.kaos.calendario.entity.Festivo;

/**
 * Mapper MapStruct para conversión entre {@link Festivo} y sus DTOs.
 * Festivos están vinculados a ciudad, no a personas individuales.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {})
public interface FestivoMapper {

    /**
     * Convierte Festivo entity a Response DTO.
     */
    FestivoResponse toResponse(Festivo entity);

    List<FestivoResponse> toResponseList(List<Festivo> entities);

    /**
     * Convierte Request DTO a Festivo entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Festivo toEntity(FestivoRequest request);

    /**
     * Actualiza entity existente desde Request.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(FestivoRequest request, @MappingTarget Festivo entity);
}
