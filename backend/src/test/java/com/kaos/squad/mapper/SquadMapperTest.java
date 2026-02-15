package com.kaos.squad.mapper;

import com.kaos.squad.dto.SquadRequest;
import com.kaos.squad.dto.SquadResponse;
import com.kaos.squad.entity.EstadoSquad;
import com.kaos.squad.entity.Squad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests para SquadMapper.
 * Verifica conversiones entre Entity ↔ DTO de Squad.
 */
@SpringBootTest
@DisplayName("SquadMapper Tests")
class SquadMapperTest {

    @Autowired
    private SquadMapper mapper;

    @Nested
    @DisplayName("toEntity Tests")
    class ToEntityTests {

        @Test
        @DisplayName("Convierte request a entity correctamente")
        void toEntity_conRequestValido_creaEntity() {
            SquadRequest request = new SquadRequest(
                    "Squad Phoenix",
                    "Squad de desarrollo backend",
                    "BOARD-123",
                    "BOARD-456"
            );

            Squad entity = mapper.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNull(); // No mapea ID
            assertThat(entity.getNombre()).isEqualTo("Squad Phoenix");
            assertThat(entity.getDescripcion()).isEqualTo("Squad de desarrollo backend");
            assertThat(entity.getIdSquadCorrJira()).isEqualTo("BOARD-123");
            assertThat(entity.getIdSquadEvolJira()).isEqualTo("BOARD-456");
            assertThat(entity.getEstado()).isEqualTo(EstadoSquad.ACTIVO); // @Builder.Default en Squad entity
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("Crea entity con descripción y boards null")
        void toEntity_conDescripcionNull_creaEntityValido() {
            SquadRequest request = new SquadRequest(
                    "Squad Dragon",
                    null,
                    null,
                    null
            );

            Squad entity = mapper.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getNombre()).isEqualTo("Squad Dragon");
            assertThat(entity.getDescripcion()).isNull();
            assertThat(entity.getIdSquadCorrJira()).isNull();
            assertThat(entity.getIdSquadEvolJira()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponse Tests")
    class ToResponseTests {

        @Test
        @DisplayName("Convierte entity a response correctamente")
        void toResponse_conEntityCompleto_creaResponse() {
            Squad entity = Squad.builder()
                    .id(10L)
                    .nombre("Squad Alpha")
                    .descripcion("Squad de innovación")
                    .estado(EstadoSquad.ACTIVO)
                    .createdAt(LocalDateTime.of(2024, 1, 15, 10, 0))
                    .updatedAt(LocalDateTime.of(2024, 2, 20, 14, 30))
                    .build();

            SquadResponse response = mapper.toResponse(entity);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.nombre()).isEqualTo("Squad Alpha");
            assertThat(response.descripcion()).isEqualTo("Squad de innovación");
            assertThat(response.estado()).isEqualTo(EstadoSquad.ACTIVO);
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 0));
            assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2024, 2, 20, 14, 30));
        }

        @Test
        @DisplayName("Convierte entity inactivo correctamente")
        void toResponse_conSquadInactivo_mapeaEstado() {
            Squad entity = Squad.builder()
                    .id(5L)
                    .nombre("Squad Legacy")
                    .descripcion("Squad obsoleto")
                    .estado(EstadoSquad.INACTIVO)
                    .createdAt(LocalDateTime.of(2020, 5, 1, 9, 0))
                    .build();

            SquadResponse response = mapper.toResponse(entity);

            assertThat(response).isNotNull();
            assertThat(response.estado()).isEqualTo(EstadoSquad.INACTIVO);
            assertThat(response.nombre()).isEqualTo("Squad Legacy");
            assertThat(response.updatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponseList Tests")
    class ToResponseListTests {

        @Test
        @DisplayName("Convierte lista de entities a responses")
        void toResponseList_conVariosSquads_creaListaResponses() {
            List<Squad> entities = List.of(
                    Squad.builder()
                            .id(1L)
                            .nombre("Squad A")
                            .descripcion("Frontend")
                            .estado(EstadoSquad.ACTIVO)
                            .createdAt(LocalDateTime.of(2023, 1, 1, 10, 0))
                            .build(),
                    Squad.builder()
                            .id(2L)
                            .nombre("Squad B")
                            .descripcion("Backend")
                            .estado(EstadoSquad.ACTIVO)
                            .createdAt(LocalDateTime.of(2023, 6, 15, 14, 30))
                            .build(),
                    Squad.builder()
                            .id(3L)
                            .nombre("Squad C")
                            .descripcion("Infraestructura")
                            .estado(EstadoSquad.INACTIVO)
                            .createdAt(LocalDateTime.of(2022, 3, 10, 8, 0))
                            .build()
            );

            List<SquadResponse> responses = mapper.toResponseList(entities);

            assertThat(responses).hasSize(3);
            assertThat(responses.get(0).id()).isEqualTo(1L);
            assertThat(responses.get(0).nombre()).isEqualTo("Squad A");
            assertThat(responses.get(0).estado()).isEqualTo(EstadoSquad.ACTIVO);
            assertThat(responses.get(1).id()).isEqualTo(2L);
            assertThat(responses.get(1).nombre()).isEqualTo("Squad B");
            assertThat(responses.get(2).id()).isEqualTo(3L);
            assertThat(responses.get(2).estado()).isEqualTo(EstadoSquad.INACTIVO);
        }

        @Test
        @DisplayName("Retorna lista vacía cuando input vacío")
        void toResponseList_conListaVacia_retornaListaVacia() {
            List<Squad> emptyList = List.of();

            List<SquadResponse> responses = mapper.toResponseList(emptyList);

            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateEntity Tests")
    class UpdateEntityTests {

        @Test
        @DisplayName("Actualiza entity existente con datos de request")
        void updateEntity_conRequestValido_actualizaEntity() {
            // Entity existente
            Squad existing = Squad.builder()
                    .id(100L)
                    .nombre("Nombre Viejo")
                    .descripcion("Descripción vieja")
                    .estado(EstadoSquad.ACTIVO)
                    .createdAt(LocalDateTime.of(2020, 1, 1, 10, 0))
                    .updatedAt(LocalDateTime.of(2021, 5, 10, 12, 0))
                    .build();

            // Request con nuevos datos
            SquadRequest request = new SquadRequest(
                    "Nombre Nuevo",
                    "Descripción actualizada",
                    "BOARD-NEW-CORR",
                    "BOARD-NEW-EVOL"
            );

            mapper.updateEntity(request, existing);

            // Verificar que se actualizaron los campos del request
            assertThat(existing.getNombre()).isEqualTo("Nombre Nuevo");
            assertThat(existing.getDescripcion()).isEqualTo("Descripción actualizada");
            assertThat(existing.getIdSquadCorrJira()).isEqualTo("BOARD-NEW-CORR");
            assertThat(existing.getIdSquadEvolJira()).isEqualTo("BOARD-NEW-EVOL");

            // Verificar que NO se alteraron campos gestionados
            assertThat(existing.getId()).isEqualTo(100L); // ID no cambia
            assertThat(existing.getEstado()).isEqualTo(EstadoSquad.ACTIVO); // Estado no lo toca updateEntity
            assertThat(existing.getCreatedAt()).isEqualTo(LocalDateTime.of(2020, 1, 1, 10, 0)); // Timestamp no cambia
        }

        @Test
        @DisplayName("Ignora valores null del request (IGNORE strategy)")
        void updateEntity_conDescripcionNull_ignoraNulos() {
            Squad existing = Squad.builder()
                    .id(200L)
                    .nombre("Squad Omega")
                    .descripcion("Descripción original")
                    .estado(EstadoSquad.ACTIVO)
                    .createdAt(LocalDateTime.of(2022, 3, 1, 9, 0))
                    .build();

            SquadRequest request = new SquadRequest(
                    "Squad Omega Updated",
                    null, // Descripción null → no debe sobrescribir
                    "BOARD-OMEGA-CORR",
                    "BOARD-OMEGA-EVOL"
            );

            mapper.updateEntity(request, existing);

            // Nombre presente en request se actualiza
            assertThat(existing.getNombre()).isEqualTo("Squad Omega Updated");
            assertThat(existing.getIdSquadCorrJira()).isEqualTo("BOARD-OMEGA-CORR");
            assertThat(existing.getIdSquadEvolJira()).isEqualTo("BOARD-OMEGA-EVOL");

            // Descripción null en request NO sobrescribe (IGNORE strategy)
            assertThat(existing.getDescripcion()).isEqualTo("Descripción original");
        }

        @Test
        @DisplayName("Actualiza solo el nombre si descripción es null")
        void updateEntity_soloNombre_mantineDescripcionOriginal() {
            Squad existing = Squad.builder()
                    .id(300L)
                    .nombre("Squad Antiguo")
                    .descripcion("Descripción importante")
                    .estado(EstadoSquad.INACTIVO)
                    .createdAt(LocalDateTime.of(2019, 11, 20, 11, 0))
                    .build();

            SquadRequest request = new SquadRequest(
                    "Squad Renovado",
                    null,
                    null,
                    null
            );

            mapper.updateEntity(request, existing);

            assertThat(existing.getNombre()).isEqualTo("Squad Renovado");
            assertThat(existing.getDescripcion()).isEqualTo("Descripción importante");
            assertThat(existing.getEstado()).isEqualTo(EstadoSquad.INACTIVO);
        }
    }
}
