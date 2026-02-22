package com.kaos.persona.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.kaos.horario.entity.PerfilHorario;
import com.kaos.persona.dto.PersonaRequest;
import com.kaos.persona.dto.PersonaResponse;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.entity.Seniority;

/**
 * Tests para PersonaMapper.
 * Verifica conversiones entre Entity ↔ DTO de Persona.
 */
@SpringBootTest
@DisplayName("PersonaMapper Tests")
class PersonaMapperTest {

    @Autowired
    private PersonaMapper mapper;

    @Nested
    @DisplayName("toEntity Tests")
    class ToEntityTests {

        @Test
        @DisplayName("Convierte request a entity correctamente")
        void toEntity_conRequestValido_creaEntity() {
            PersonaRequest request = new PersonaRequest(
                    "Juan Pérez",
                    "juan.perez@example.com",
                    "jperez",
                    1L,
                    "Zaragoza",
                    Seniority.SENIOR,
                    "Java, Spring Boot",
                    new BigDecimal("45.50"),
                    LocalDate.of(2023, 1, 15),
                    true
            );

            Persona entity = mapper.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNull(); // No mapea ID
            assertThat(entity.getNombre()).isEqualTo("Juan Pérez");
            assertThat(entity.getEmail()).isEqualTo("juan.perez@example.com");
            assertThat(entity.getIdJira()).isEqualTo("jperez");
            assertThat(entity.getSeniority()).isEqualTo(Seniority.SENIOR);
            assertThat(entity.getSkills()).isEqualTo("Java, Spring Boot");
            assertThat(entity.getCosteHora()).isEqualByComparingTo("45.50");
            assertThat(entity.getFechaIncorporacion()).isEqualTo(LocalDate.of(2023, 1, 15));
            assertThat(entity.getSendNotifications()).isTrue();
            assertThat(entity.getPerfilHorario()).isNull(); // Se asigna en service layer
            assertThat(entity.getActivo()).isTrue(); // @Builder.Default en Persona entity
        }

        @Test
        @DisplayName("Ignora campos nulos opcionales")
        void toEntity_conCamposNulos_ignoraNulos() {
            PersonaRequest request = new PersonaRequest(
                    "Ana García",
                    "ana.garcia@example.com",
                    null, // idJira opcional
                    2L,
                    "Zaragoza",
                    Seniority.MID,
                    null, // skills opcional
                    new BigDecimal("40.00"),
                    LocalDate.of(2024, 6, 1),
                    false
            );

            Persona entity = mapper.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getIdJira()).isNull();
            assertThat(entity.getSkills()).isNull();
            assertThat(entity.getNombre()).isEqualTo("Ana García");
            assertThat(entity.getEmail()).isEqualTo("ana.garcia@example.com");
        }
    }

    @Nested
    @DisplayName("toResponse Tests")
    class ToResponseTests {

        @Test
        @DisplayName("Convierte entity a response correctamente")
        void toResponse_conEntityCompleto_creaResponse() {
            PerfilHorario perfil = PerfilHorario.builder()
                    .id(10L)
                    .nombre("Madrid UTC+1")
                    .build();

            Persona entity = Persona.builder()
                    .id(5L)
                    .nombre("Carlos López")
                    .email("carlos.lopez@example.com")
                    .idJira("clopez")
                    .perfilHorario(perfil)
                    .seniority(Seniority.JUNIOR)
                    .skills("Python, Django")
                    .costeHora(new BigDecimal("35.00"))
                    .activo(true)
                    .fechaIncorporacion(LocalDate.of(2024, 3, 10))
                    .sendNotifications(true)
                    .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                    .updatedAt(LocalDateTime.of(2024, 2, 1, 12, 30))
                    .build();

            PersonaResponse response = mapper.toResponse(entity);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.nombre()).isEqualTo("Carlos López");
            assertThat(response.email()).isEqualTo("carlos.lopez@example.com");
            assertThat(response.idJira()).isEqualTo("clopez");
            assertThat(response.perfilHorarioId()).isEqualTo(10L);
            assertThat(response.perfilHorarioNombre()).isEqualTo("Madrid UTC+1");
            assertThat(response.seniority()).isEqualTo(Seniority.JUNIOR);
            assertThat(response.skills()).isEqualTo("Python, Django");
            assertThat(response.costeHora()).isEqualByComparingTo("35.00");
            assertThat(response.activo()).isTrue();
            assertThat(response.fechaIncorporacion()).isEqualTo(LocalDate.of(2024, 3, 10));
            assertThat(response.sendNotifications()).isTrue();
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
            assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2024, 2, 1, 12, 30));
        }

        @Test
        @DisplayName("Maneja correctamente perfilHorario null")
        void toResponse_conPerfilNull_returnaNulosEnPerfil() {
            Persona entity = Persona.builder()
                    .id(1L)
                    .nombre("Test User")
                    .email("test@example.com")
                    .perfilHorario(null) // Sin perfil asignado
                    .seniority(Seniority.SENIOR)
                    .costeHora(new BigDecimal("50.00"))
                    .activo(true)
                    .fechaIncorporacion(LocalDate.now())
                    .sendNotifications(false)
                    .build();

            PersonaResponse response = mapper.toResponse(entity);

            assertThat(response).isNotNull();
            assertThat(response.perfilHorarioId()).isNull();
            assertThat(response.perfilHorarioNombre()).isNull();
            assertThat(response.nombre()).isEqualTo("Test User");
        }
    }

    @Nested
    @DisplayName("toResponseList Tests")
    class ToResponseListTests {

        @Test
        @DisplayName("Convierte lista de entities a responses")
        void toResponseList_conVariasEntidades_creaListaResponses() {
            PerfilHorario perfil1 = PerfilHorario.builder().id(1L).nombre("Madrid").build();
            PerfilHorario perfil2 = PerfilHorario.builder().id(2L).nombre("Londres").build();

            List<Persona> entities = List.of(
                    Persona.builder()
                            .id(1L)
                            .nombre("Persona 1")
                            .email("p1@example.com")
                            .perfilHorario(perfil1)
                            .seniority(Seniority.SENIOR)
                            .costeHora(new BigDecimal("45.00"))
                            .activo(true)
                            .fechaIncorporacion(LocalDate.of(2023, 1, 1))
                            .sendNotifications(true)
                            .build(),
                    Persona.builder()
                            .id(2L)
                            .nombre("Persona 2")
                            .email("p2@example.com")
                            .perfilHorario(perfil2)
                            .seniority(Seniority.MID)
                            .costeHora(new BigDecimal("40.00"))
                            .activo(false)
                            .fechaIncorporacion(LocalDate.of(2024, 6, 1))
                            .sendNotifications(false)
                            .build()
            );

            List<PersonaResponse> responses = mapper.toResponseList(entities);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).id()).isEqualTo(1L);
            assertThat(responses.get(0).nombre()).isEqualTo("Persona 1");
            assertThat(responses.get(0).perfilHorarioId()).isEqualTo(1L);
            assertThat(responses.get(1).id()).isEqualTo(2L);
            assertThat(responses.get(1).nombre()).isEqualTo("Persona 2");
            assertThat(responses.get(1).perfilHorarioId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Retorna lista vacía cuando input vacío")
        void toResponseList_conListaVacia_retornaListaVacia() {
            List<Persona> emptyList = List.of();

            List<PersonaResponse> responses = mapper.toResponseList(emptyList);

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
            Persona existing = Persona.builder()
                    .id(100L)
                    .nombre("Nombre Viejo")
                    .email("viejo@example.com")
                    .idJira("viejo")
                    .seniority(Seniority.JUNIOR)
                    .skills("Java")
                    .costeHora(new BigDecimal("30.00"))
                    .activo(true)
                    .fechaIncorporacion(LocalDate.of(2020, 1, 1))
                    .sendNotifications(false)
                    .createdAt(LocalDateTime.of(2020, 1, 1, 10, 0))
                    .build();

            // Request con nuevos datos
            PersonaRequest request = new PersonaRequest(
                    "Nombre Nuevo",
                    "nuevo@example.com",
                    "nuevo",
                    5L,
                    "Zaragoza",
                    Seniority.SENIOR,
                    "Java, Spring Boot, Docker",
                    new BigDecimal("50.00"),
                    LocalDate.of(2023, 5, 15),
                    true
            );

            mapper.updateEntity(request, existing);

            // Verificar que se actualizaron los campos del request
            assertThat(existing.getNombre()).isEqualTo("Nombre Nuevo");
            assertThat(existing.getEmail()).isEqualTo("nuevo@example.com");
            assertThat(existing.getIdJira()).isEqualTo("nuevo");
            assertThat(existing.getSeniority()).isEqualTo(Seniority.SENIOR);
            assertThat(existing.getSkills()).isEqualTo("Java, Spring Boot, Docker");
            assertThat(existing.getCosteHora()).isEqualByComparingTo("50.00");
            assertThat(existing.getFechaIncorporacion()).isEqualTo(LocalDate.of(2023, 5, 15));
            assertThat(existing.getSendNotifications()).isTrue();

            // Verificar que NO se alteraron campos gestionados
            assertThat(existing.getId()).isEqualTo(100L); // ID no cambia
            assertThat(existing.getActivo()).isTrue(); // No lo toca updateEntity
            assertThat(existing.getCreatedAt()).isEqualTo(LocalDateTime.of(2020, 1, 1, 10, 0)); // Timestamp no cambia
        }

        @Test
        @DisplayName("Ignora valores null del request (IGNORE strategy)")
        void updateEntity_conValoresNullEnRequest_ignoraNulos() {
            Persona existing = Persona.builder()
                    .id(200L)
                    .nombre("Juan Pérez")
                    .email("juan@example.com")
                    .idJira("jperez")
                    .seniority(Seniority.SENIOR)
                    .skills("Java, Spring")
                    .costeHora(new BigDecimal("45.00"))
                    .activo(true)
                    .fechaIncorporacion(LocalDate.of(2022, 3, 1))
                    .sendNotifications(true)
                    .build();

            PersonaRequest request = new PersonaRequest(
                    "Juan Pérez Actualizado",
                    "juan@example.com",
                    null, // idJira null → no debe sobrescribir
                    3L,
                    "Zaragoza",
                    Seniority.SENIOR,
                    null, // skills null → no debe sobrescribir
                    new BigDecimal("47.00"),
                    LocalDate.of(2022, 3, 1),
                    true
            );

            mapper.updateEntity(request, existing);

            // Valores presentes en request se actualizan
            assertThat(existing.getNombre()).isEqualTo("Juan Pérez Actualizado");
            assertThat(existing.getCosteHora()).isEqualByComparingTo("47.00");

            // Valores null en request NO sobrescriben (IGNORE strategy)
            assertThat(existing.getIdJira()).isEqualTo("jperez"); // Mantiene valor original
            assertThat(existing.getSkills()).isEqualTo("Java, Spring"); // Mantiene valor original
        }
    }
}
