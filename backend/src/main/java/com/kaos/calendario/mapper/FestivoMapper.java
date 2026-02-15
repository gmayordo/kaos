package com.kaos.calendario.mapper;

import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.*;
import com.kaos.calendario.dto.FestivoRequest;
import com.kaos.calendario.dto.FestivoResponse;
import com.kaos.calendario.dto.PersonaBasicInfo;
import com.kaos.calendario.entity.Festivo;
import com.kaos.persona.entity.Persona;

/**
 * Mapper MapStruct para conversión entre {@link Festivo} y sus DTOs.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FestivoMapper {

    /**
     * Convierte Festivo entity a Response DTO.
     * Las personas se mapean a PersonaBasicInfo
     */
    @Mapping(target = "personas", expression = "java(mapPersonasToBasicInfo(entity.getPersonas()))")
    FestivoResponse toResponse(Festivo entity);

    List<FestivoResponse> toResponseList(List<Festivo> entities);

    /**
     * Convierte Request DTO a Festivo entity (sin asignar personas).
     * Las personas se asignan en el servicio.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "personas", ignore = true)
    Festivo toEntity(FestivoRequest request);

    /**
     * Actualiza entity existente desde Request (sin actualizar personas).
     * Las personas se actualizan en el servicio.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "personas", ignore = true)
    void updateEntity(FestivoRequest request, @MappingTarget Festivo entity);

    /**
     * Mapea Set de Persona a List de PersonaBasicInfo.
     */
    default List<PersonaBasicInfo> mapPersonasToBasicInfo(java.util.Set<Persona> personas) {
        if (personas == null) {
            return List.of();
        }
        return personas.stream()
                .map(p -> new PersonaBasicInfo(p.getId(), p.getNombre(), p.getEmail()))
                .collect(Collectors.toList());
    }
}
