package com.kaos.dedicacion.mapper;

import com.kaos.dedicacion.dto.SquadMemberRequest;
import com.kaos.dedicacion.dto.SquadMemberResponse;
import com.kaos.dedicacion.entity.SquadMember;
import com.kaos.horario.entity.PerfilHorario;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.entity.Rol;
import com.kaos.persona.entity.Seniority;
import com.kaos.squad.entity.EstadoSquad;
import com.kaos.squad.entity.Squad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests para SquadMemberMapper.
 * Verifica conversiones Entity ↔ DTO y cálculo de capacidadDiaria.
 */
@SpringBootTest
@DisplayName("SquadMemberMapper Tests")
class SquadMemberMapperTest {

    @Autowired
    private SquadMemberMapper mapper;

    @Nested
    @DisplayName("toEntity Tests")
    class ToEntityTests {

        @Test
        @DisplayName("Convierte request a entity correctamente")
        void toEntity_conRequestValido_creaEntity() {
            SquadMemberRequest request = new SquadMemberRequest(
                    10L,
                    20L,
                    Rol.BACKEND,
                    75,
                    LocalDate.of(2024, 1, 15),
                    LocalDate.of(2024, 12, 31)
            );

            SquadMember entity = mapper.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNull(); // No mapea ID
            assertThat(entity.getRol()).isEqualTo(Rol.BACKEND);
            assertThat(entity.getPorcentaje()).isEqualTo(75);
            assertThat(entity.getFechaInicio()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(entity.getFechaFin()).isEqualTo(LocalDate.of(2024, 12, 31));
            assertThat(entity.getPersona()).isNull(); // Se asigna en service layer
            assertThat(entity.getSquad()).isNull();   // Se asigna en service layer
        }

        @Test
        @DisplayName("Crea entity sin fecha fin (asignación indefinida)")
        void toEntity_sinFechaFin_creaEntityValido() {
            SquadMemberRequest request = new SquadMemberRequest(
                    5L,
                    15L,
                    Rol.FRONTEND,
                    100,
                    LocalDate.of(2024, 6, 1),
                    null // Sin fecha fin
            );

            SquadMember entity = mapper.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getRol()).isEqualTo(Rol.FRONTEND);
            assertThat(entity.getPorcentaje()).isEqualTo(100);
            assertThat(entity.getFechaInicio()).isEqualTo(LocalDate.of(2024, 6, 1));
            assertThat(entity.getFechaFin()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponse Tests")
    class ToResponseTests {

        @Test
        @DisplayName("Convierte entity a response con capacidad diaria calculada")
        void toResponse_conEntityCompleto_calculaCapacidadDiaria() {
            PerfilHorario perfil = PerfilHorario.builder()
                    .id(1L)
                    .nombre("Madrid 8h")
                    .horasLunes(new BigDecimal("8.00"))
                    .horasMartes(new BigDecimal("8.00"))
                    .horasMiercoles(new BigDecimal("8.00"))
                    .horasJueves(new BigDecimal("8.00"))
                    .horasViernes(new BigDecimal("6.00"))
                    .build();

            Persona persona = Persona.builder()
                    .id(10L)
                    .nombre("Juan Pérez")
                    .perfilHorario(perfil)
                    .seniority(Seniority.SENIOR)
                    .build();

            Squad squad = Squad.builder()
                    .id(20L)
                    .nombre("Squad Alpha")
                    .estado(EstadoSquad.ACTIVO)
                    .build();

            SquadMember entity = SquadMember.builder()
                    .id(100L)
                    .persona(persona)
                    .squad(squad)
                    .rol(Rol.BACKEND)
                    .porcentaje(50) // 50% de dedicación
                    .fechaInicio(LocalDate.of(2024, 1, 1))
                    .fechaFin(LocalDate.of(2024, 12, 31))
                    .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                    .updatedAt(LocalDateTime.of(2024, 2, 15, 12, 30))
                    .build();

            SquadMemberResponse response = mapper.toResponse(entity);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.personaId()).isEqualTo(10L);
            assertThat(response.personaNombre()).isEqualTo("Juan Pérez");
            assertThat(response.squadId()).isEqualTo(20L);
            assertThat(response.squadNombre()).isEqualTo("Squad Alpha");
            assertThat(response.rol()).isEqualTo(Rol.BACKEND);
            assertThat(response.porcentaje()).isEqualTo(50);
            assertThat(response.fechaInicio()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(response.fechaFin()).isEqualTo(LocalDate.of(2024, 12, 31));

            // Capacidad diaria = horasDia × 50 / 100
            assertThat(response.capacidadDiariaLunes()).isEqualByComparingTo("4.00");    // 8 × 50% = 4
            assertThat(response.capacidadDiariaMartes()).isEqualByComparingTo("4.00");   // 8 × 50% = 4
            assertThat(response.capacidadDiariaMiercoles()).isEqualByComparingTo("4.00");// 8 × 50% = 4
            assertThat(response.capacidadDiariaJueves()).isEqualByComparingTo("4.00");   // 8 × 50% = 4
            assertThat(response.capacidadDiariaViernes()).isEqualByComparingTo("3.00");  // 6 × 50% = 3

            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
            assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2024, 2, 15, 12, 30));
        }

        @Test
        @DisplayName("Calcula capacidad diaria con porcentaje 100%")
        void toResponse_conPorcentaje100_retornaHorasCompletas() {
            PerfilHorario perfil = PerfilHorario.builder()
                    .horasLunes(new BigDecimal("7.50"))
                    .horasMartes(new BigDecimal("7.50"))
                    .horasMiercoles(new BigDecimal("7.50"))
                    .horasJueves(new BigDecimal("7.50"))
                    .horasViernes(new BigDecimal("5.00"))
                    .build();

            Persona persona = Persona.builder()
                    .id(1L)
                    .nombre("Test User")
                    .perfilHorario(perfil)
                    .build();

            Squad squad = Squad.builder()
                    .id(2L)
                    .nombre("Squad Test")
                    .build();

            SquadMember entity = SquadMember.builder()
                    .id(1L)
                    .persona(persona)
                    .squad(squad)
                    .rol(Rol.QA)
                    .porcentaje(100) // 100% de dedicación
                    .fechaInicio(LocalDate.of(2024, 1, 1))
                    .build();

            SquadMemberResponse response = mapper.toResponse(entity);

            // Con 100%, capacidad = horas completas
            assertThat(response.capacidadDiariaLunes()).isEqualByComparingTo("7.50");
            assertThat(response.capacidadDiariaMartes()).isEqualByComparingTo("7.50");
            assertThat(response.capacidadDiariaMiercoles()).isEqualByComparingTo("7.50");
            assertThat(response.capacidadDiariaJueves()).isEqualByComparingTo("7.50");
            assertThat(response.capacidadDiariaViernes()).isEqualByComparingTo("5.00");
        }

        @Test
        @DisplayName("Calcula capacidad diaria con porcentaje bajo (25%)")
        void toResponse_conPorcentaje25_calculaCorrectamente() {
            PerfilHorario perfil = PerfilHorario.builder()
                    .horasLunes(new BigDecimal("8.00"))
                    .horasMartes(new BigDecimal("8.00"))
                    .horasMiercoles(new BigDecimal("8.00"))
                    .horasJueves(new BigDecimal("8.00"))
                    .horasViernes(new BigDecimal("8.00"))
                    .build();

            Persona persona = Persona.builder()
                    .id(5L)
                    .nombre("Ana García")
                    .perfilHorario(perfil)
                    .build();

            Squad squad = Squad.builder()
                    .id(10L)
                    .nombre("Squad Beta")
                    .build();

            SquadMember entity = SquadMember.builder()
                    .id(50L)
                    .persona(persona)
                    .squad(squad)
                    .rol(Rol.LIDER_FUNCIONAL)
                    .porcentaje(25) // 25% de dedicación
                    .fechaInicio(LocalDate.of(2024, 3, 1))
                    .build();

            SquadMemberResponse response = mapper.toResponse(entity);

            // 8 × 25% = 2.00
            assertThat(response.capacidadDiariaLunes()).isEqualByComparingTo("2.00");
            assertThat(response.capacidadDiariaMartes()).isEqualByComparingTo("2.00");
            assertThat(response.capacidadDiariaMiercoles()).isEqualByComparingTo("2.00");
            assertThat(response.capacidadDiariaJueves()).isEqualByComparingTo("2.00");
            assertThat(response.capacidadDiariaViernes()).isEqualByComparingTo("2.00");
        }
    }

    @Nested
    @DisplayName("toResponseList Tests")
    class ToResponseListTests {

        @Test
        @DisplayName("Convierte lista de entities a responses")
        void toResponseList_conVariasAsignaciones_creaListaResponses() {
            PerfilHorario perfil = PerfilHorario.builder()
                    .horasLunes(new BigDecimal("8.00"))
                    .horasMartes(new BigDecimal("8.00"))
                    .horasMiercoles(new BigDecimal("8.00"))
                    .horasJueves(new BigDecimal("8.00"))
                    .horasViernes(new BigDecimal("6.00"))
                    .build();

            Persona persona1 = Persona.builder().id(1L).nombre("Persona 1").perfilHorario(perfil).build();
            Persona persona2 = Persona.builder().id(2L).nombre("Persona 2").perfilHorario(perfil).build();

            Squad squad = Squad.builder().id(10L).nombre("Squad Delta").build();

            List<SquadMember> entities = List.of(
                    SquadMember.builder()
                            .id(1L)
                            .persona(persona1)
                            .squad(squad)
                            .rol(Rol.BACKEND)
                            .porcentaje(100)
                            .fechaInicio(LocalDate.of(2024, 1, 1))
                            .build(),
                    SquadMember.builder()
                            .id(2L)
                            .persona(persona2)
                            .squad(squad)
                            .rol(Rol.FRONTEND)
                            .porcentaje(50)
                            .fechaInicio(LocalDate.of(2024, 2, 1))
                            .build()
            );

            List<SquadMemberResponse> responses = mapper.toResponseList(entities);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).id()).isEqualTo(1L);
            assertThat(responses.get(0).personaNombre()).isEqualTo("Persona 1");
            assertThat(responses.get(0).porcentaje()).isEqualTo(100);
            assertThat(responses.get(0).capacidadDiariaLunes()).isEqualByComparingTo("8.00");

            assertThat(responses.get(1).id()).isEqualTo(2L);
            assertThat(responses.get(1).personaNombre()).isEqualTo("Persona 2");
            assertThat(responses.get(1).porcentaje()).isEqualTo(50);
            assertThat(responses.get(1).capacidadDiariaLunes()).isEqualByComparingTo("4.00");
        }

        @Test
        @DisplayName("Retorna lista vacía cuando input vacío")
        void toResponseList_conListaVacia_retornaListaVacia() {
            List<SquadMember> emptyList = List.of();

            List<SquadMemberResponse> responses = mapper.toResponseList(emptyList);

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
            SquadMember existing = SquadMember.builder()
                    .id(100L)
                    .rol(Rol.BACKEND)
                    .porcentaje(50)
                    .fechaInicio(LocalDate.of(2023, 1, 1))
                    .fechaFin(LocalDate.of(2023, 12, 31))
                    .createdAt(LocalDateTime.of(2023, 1, 1, 10, 0))
                    .build();

            // Request con nuevos datos
            SquadMemberRequest request = new SquadMemberRequest(
                    10L,
                    20L,
                    Rol.LIDER_TECNICO, // Cambio de rol
                    75, // Más porcentaje
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 6, 30)
            );

            mapper.updateEntity(request, existing);

            // Verificar que se actualizaron los campos del request
            assertThat(existing.getRol()).isEqualTo(Rol.LIDER_TECNICO);
            assertThat(existing.getPorcentaje()).isEqualTo(75);
            assertThat(existing.getFechaInicio()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(existing.getFechaFin()).isEqualTo(LocalDate.of(2024, 6, 30));

            // Verificar que NO se alteraron campos gestionados
            assertThat(existing.getId()).isEqualTo(100L); // ID no cambia
            assertThat(existing.getPersona()).isNull();   // No lo toca updateEntity
            assertThat(existing.getSquad()).isNull();     // No lo toca updateEntity
            assertThat(existing.getCreatedAt()).isEqualTo(LocalDateTime.of(2023, 1, 1, 10, 0)); // Timestamp no cambia
        }

        @Test
        @DisplayName("Elimina fecha fin al actualizar con null")
        void updateEntity_conFechaFinNull_actualizaAIndefinido() {
            SquadMember existing = SquadMember.builder()
                    .id(200L)
                    .rol(Rol.QA)
                    .porcentaje(100)
                    .fechaInicio(LocalDate.of(2023, 6, 1))
                    .fechaFin(LocalDate.of(2024, 12, 31)) // Tenía fecha fin
                    .build();

            SquadMemberRequest request = new SquadMemberRequest(
                    5L,
                    15L,
                    Rol.QA,
                    100,
                    LocalDate.of(2023, 6, 1),
                    null // Sin fecha fin → asignación indefinida
            );

            mapper.updateEntity(request, existing);

            assertThat(existing.getFechaFin()).isNull(); // Se eliminó la fecha fin
            assertThat(existing.getPorcentaje()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("calcularCapacidad Tests (método default)")
    class CalcularCapacidadTests {

        @Test
        @DisplayName("Calcula capacidad correctamente con valores válidos")
        void calcularCapacidad_conValoresValidos_calculaCorrectamente() {
            BigDecimal horas = new BigDecimal("8.00");
            Integer porcentaje = 50;

            BigDecimal capacidad = mapper.calcularCapacidad(horas, porcentaje);

            assertThat(capacidad).isEqualByComparingTo("4.00"); // 8 × 50 / 100 = 4
        }

        @Test
        @DisplayName("Retorna 0 cuando horas es null")
        void calcularCapacidad_conHorasNull_retornaCero() {
            BigDecimal capacidad = mapper.calcularCapacidad(null, 50);

            assertThat(capacidad).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Retorna 0 cuando porcentaje es null")
        void calcularCapacidad_conPorcentajeNull_retornaCero() {
            BigDecimal horas = new BigDecimal("8.00");

            BigDecimal capacidad = mapper.calcularCapacidad(horas, null);

            assertThat(capacidad).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Retorna 0 cuando ambos son null")
        void calcularCapacidad_conAmbosNull_retornaCero() {
            BigDecimal capacidad = mapper.calcularCapacidad(null, null);

            assertThat(capacidad).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Calcula correctamente con porcentaje 100")
        void calcularCapacidad_conPorcentaje100_retornaHorasCompletas() {
            BigDecimal horas = new BigDecimal("7.50");

            BigDecimal capacidad = mapper.calcularCapacidad(horas, 100);

            assertThat(capacidad).isEqualByComparingTo("7.50"); // 7.5 × 100 / 100 = 7.5
        }

        @Test
        @DisplayName("Redondea correctamente a 2 decimales (HALF_UP)")
        void calcularCapacidad_redondea_aDosdecimales() {
            BigDecimal horas = new BigDecimal("8.00");
            Integer porcentaje = 33; // 8 × 33 / 100 = 2.64

            BigDecimal capacidad = mapper.calcularCapacidad(horas, porcentaje);

            assertThat(capacidad).isEqualByComparingTo("2.64");
        }

        @Test
        @DisplayName("Calcula correctamente con porcentaje 0")
        void calcularCapacidad_conPorcentaje0_retornaCero() {
            BigDecimal horas = new BigDecimal("8.00");

            BigDecimal capacidad = mapper.calcularCapacidad(horas, 0);

            assertThat(capacidad).isEqualByComparingTo("0.00"); // 8 × 0 / 100 = 0
        }
    }
}
