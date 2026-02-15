package com.kaos.horario.mapper;

import com.kaos.horario.dto.PerfilHorarioRequest;
import com.kaos.horario.dto.PerfilHorarioResponse;
import com.kaos.horario.entity.PerfilHorario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests para PerfilHorarioMapper.
 * Verifica conversiones entre Entity ↔ DTO de PerfilHorario.
 */
@SpringBootTest
@DisplayName("PerfilHorarioMapper Tests")
class PerfilHorarioMapperTest {

    @Autowired
    private PerfilHorarioMapper mapper;

    @Nested
    @DisplayName("toEntity Tests")
    class ToEntityTests {

        @Test
        @DisplayName("Convierte request a entity correctamente")
        void toEntity_conRequestValido_creaEntity() {
            PerfilHorarioRequest request = new PerfilHorarioRequest(
                    "Madrid UTC+1",
                    "Europe/Madrid",
                    new BigDecimal("8.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("6.00")
            );

            PerfilHorario entity = mapper.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNull(); // No mapea ID
            assertThat(entity.getNombre()).isEqualTo("Madrid UTC+1");
            assertThat(entity.getZonaHoraria()).isEqualTo("Europe/Madrid");
            assertThat(entity.getHorasLunes()).isEqualByComparingTo("8.00");
            assertThat(entity.getHorasMartes()).isEqualByComparingTo("8.00");
            assertThat(entity.getHorasMiercoles()).isEqualByComparingTo("8.00");
            assertThat(entity.getHorasJueves()).isEqualByComparingTo("8.00");
            assertThat(entity.getHorasViernes()).isEqualByComparingTo("6.00");
            assertThat(entity.getTotalSemanal()).isNull(); // Se calcula en service layer
            assertThat(entity.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("Crea entity con horas variables por día")
        void toEntity_conHorasVariables_creaEntityValido() {
            PerfilHorarioRequest request = new PerfilHorarioRequest(
                    "Perfil Personalizado",
                    "America/New_York",
                    new BigDecimal("7.50"),
                    new BigDecimal("8.00"),
                    new BigDecimal("6.50"),
                    new BigDecimal("8.00"),
                    new BigDecimal("5.00")
            );

            PerfilHorario entity = mapper.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getHorasLunes()).isEqualByComparingTo("7.50");
            assertThat(entity.getHorasMartes()).isEqualByComparingTo("8.00");
            assertThat(entity.getHorasMiercoles()).isEqualByComparingTo("6.50");
            assertThat(entity.getHorasJueves()).isEqualByComparingTo("8.00");
            assertThat(entity.getHorasViernes()).isEqualByComparingTo("5.00");
        }
    }

    @Nested
    @DisplayName("toResponse Tests")
    class ToResponseTests {

        @Test
        @DisplayName("Convierte entity a response correctamente")
        void toResponse_conEntityCompleto_creaResponse() {
            PerfilHorario entity = PerfilHorario.builder()
                    .id(15L)
                    .nombre("Londres UTC")
                    .zonaHoraria("Europe/London")
                    .horasLunes(new BigDecimal("8.00"))
                    .horasMartes(new BigDecimal("8.00"))
                    .horasMiercoles(new BigDecimal("8.00"))
                    .horasJueves(new BigDecimal("8.00"))
                    .horasViernes(new BigDecimal("8.00"))
                    .totalSemanal(new BigDecimal("40.00"))
                    .createdAt(LocalDateTime.of(2024, 1, 10, 9, 0))
                    .updatedAt(LocalDateTime.of(2024, 3, 15, 10, 30))
                    .build();

            PerfilHorarioResponse response = mapper.toResponse(entity);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(15L);
            assertThat(response.nombre()).isEqualTo("Londres UTC");
            assertThat(response.zonaHoraria()).isEqualTo("Europe/London");
            assertThat(response.horasLunes()).isEqualByComparingTo("8.00");
            assertThat(response.horasMartes()).isEqualByComparingTo("8.00");
            assertThat(response.horasMiercoles()).isEqualByComparingTo("8.00");
            assertThat(response.horasJueves()).isEqualByComparingTo("8.00");
            assertThat(response.horasViernes()).isEqualByComparingTo("8.00");
            assertThat(response.totalSemanal()).isEqualByComparingTo("40.00");
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 10, 9, 0));
            assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2024, 3, 15, 10, 30));
        }

        @Test
        @DisplayName("Convierte entity con jornada reducida viernes")
        void toResponse_conJornadaReducida_mapeaCorrectamente() {
            PerfilHorario entity = PerfilHorario.builder()
                    .id(20L)
                    .nombre("Jornada Intensiva Verano")
                    .zonaHoraria("Europe/Madrid")
                    .horasLunes(new BigDecimal("8.00"))
                    .horasMartes(new BigDecimal("8.00"))
                    .horasMiercoles(new BigDecimal("8.00"))
                    .horasJueves(new BigDecimal("8.00"))
                    .horasViernes(new BigDecimal("5.00"))
                    .totalSemanal(new BigDecimal("37.00"))
                    .createdAt(LocalDateTime.of(2024, 6, 1, 8, 0))
                    .build();

            PerfilHorarioResponse response = mapper.toResponse(entity);

            assertThat(response).isNotNull();
            assertThat(response.horasViernes()).isEqualByComparingTo("5.00");
            assertThat(response.totalSemanal()).isEqualByComparingTo("37.00");
            assertThat(response.nombre()).isEqualTo("Jornada Intensiva Verano");
        }
    }

    @Nested
    @DisplayName("toResponseList Tests")
    class ToResponseListTests {

        @Test
        @DisplayName("Convierte lista de entities a responses")
        void toResponseList_conVariosPerfiles_creaListaResponses() {
            List<PerfilHorario> entities = List.of(
                    PerfilHorario.builder()
                            .id(1L)
                            .nombre("Perfil 1")
                            .zonaHoraria("Europe/Madrid")
                            .horasLunes(new BigDecimal("8.00"))
                            .horasMartes(new BigDecimal("8.00"))
                            .horasMiercoles(new BigDecimal("8.00"))
                            .horasJueves(new BigDecimal("8.00"))
                            .horasViernes(new BigDecimal("6.00"))
                            .totalSemanal(new BigDecimal("38.00"))
                            .createdAt(LocalDateTime.of(2023, 1, 1, 10, 0))
                            .build(),
                    PerfilHorario.builder()
                            .id(2L)
                            .nombre("Perfil 2")
                            .zonaHoraria("America/New_York")
                            .horasLunes(new BigDecimal("7.00"))
                            .horasMartes(new BigDecimal("7.00"))
                            .horasMiercoles(new BigDecimal("7.00"))
                            .horasJueves(new BigDecimal("7.00"))
                            .horasViernes(new BigDecimal("7.00"))
                            .totalSemanal(new BigDecimal("35.00"))
                            .createdAt(LocalDateTime.of(2023, 6, 15, 14, 30))
                            .build()
            );

            List<PerfilHorarioResponse> responses = mapper.toResponseList(entities);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).id()).isEqualTo(1L);
            assertThat(responses.get(0).nombre()).isEqualTo("Perfil 1");
            assertThat(responses.get(0).totalSemanal()).isEqualByComparingTo("38.00");
            assertThat(responses.get(1).id()).isEqualTo(2L);
            assertThat(responses.get(1).nombre()).isEqualTo("Perfil 2");
            assertThat(responses.get(1).totalSemanal()).isEqualByComparingTo("35.00");
        }

        @Test
        @DisplayName("Retorna lista vacía cuando input vacío")
        void toResponseList_conListaVacia_retornaListaVacia() {
            List<PerfilHorario> emptyList = List.of();

            List<PerfilHorarioResponse> responses = mapper.toResponseList(emptyList);

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
            PerfilHorario existing = PerfilHorario.builder()
                    .id(100L)
                    .nombre("Perfil Viejo")
                    .zonaHoraria("Europe/Paris")
                    .horasLunes(new BigDecimal("7.00"))
                    .horasMartes(new BigDecimal("7.00"))
                    .horasMiercoles(new BigDecimal("7.00"))
                    .horasJueves(new BigDecimal("7.00"))
                    .horasViernes(new BigDecimal("7.00"))
                    .totalSemanal(new BigDecimal("35.00"))
                    .createdAt(LocalDateTime.of(2020, 1, 1, 10, 0))
                    .build();

            // Request con nuevos datos
            PerfilHorarioRequest request = new PerfilHorarioRequest(
                    "Perfil Actualizado",
                    "Europe/Madrid",
                    new BigDecimal("8.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("6.00")
            );

            mapper.updateEntity(request, existing);

            // Verificar que se actualizaron los campos del request
            assertThat(existing.getNombre()).isEqualTo("Perfil Actualizado");
            assertThat(existing.getZonaHoraria()).isEqualTo("Europe/Madrid");
            assertThat(existing.getHorasLunes()).isEqualByComparingTo("8.00");
            assertThat(existing.getHorasMartes()).isEqualByComparingTo("8.00");
            assertThat(existing.getHorasMiercoles()).isEqualByComparingTo("8.00");
            assertThat(existing.getHorasJueves()).isEqualByComparingTo("8.00");
            assertThat(existing.getHorasViernes()).isEqualByComparingTo("6.00");

            // Verificar que NO se alteraron campos gestionados
            assertThat(existing.getId()).isEqualTo(100L); // ID no cambia
            assertThat(existing.getTotalSemanal()).isEqualByComparingTo("35.00"); // Se recalcula en service, no aquí
            assertThat(existing.getCreatedAt()).isEqualTo(LocalDateTime.of(2020, 1, 1, 10, 0)); // Timestamp no cambia
        }

        @Test
        @DisplayName("Actualiza solo algunos días de la semana")
        void updateEntity_conNuevosHorarios_actualizaTodosCampos() {
            PerfilHorario existing = PerfilHorario.builder()
                    .id(200L)
                    .nombre("Perfil Original")
                    .zonaHoraria("Asia/Tokyo")
                    .horasLunes(new BigDecimal("6.00"))
                    .horasMartes(new BigDecimal("6.00"))
                    .horasMiercoles(new BigDecimal("6.00"))
                    .horasJueves(new BigDecimal("6.00"))
                    .horasViernes(new BigDecimal("6.00"))
                    .totalSemanal(new BigDecimal("30.00"))
                    .createdAt(LocalDateTime.of(2022, 3, 1, 9, 0))
                    .build();

            PerfilHorarioRequest request = new PerfilHorarioRequest(
                    "Perfil Original", // Mismo nombre
                    "Asia/Tokyo",      // Misma zona
                    new BigDecimal("7.50"), // Más horas
                    new BigDecimal("7.50"),
                    new BigDecimal("7.50"),
                    new BigDecimal("7.50"),
                    new BigDecimal("5.00")  // Viernes reducido
            );

            mapper.updateEntity(request, existing);

            // Verificar actualización de horarios
            assertThat(existing.getHorasLunes()).isEqualByComparingTo("7.50");
            assertThat(existing.getHorasMartes()).isEqualByComparingTo("7.50");
            assertThat(existing.getHorasMiercoles()).isEqualByComparingTo("7.50");
            assertThat(existing.getHorasJueves()).isEqualByComparingTo("7.50");
            assertThat(existing.getHorasViernes()).isEqualByComparingTo("5.00");

            // totalSemanal no se recalcula en mapper (se hace en service)
            assertThat(existing.getTotalSemanal()).isEqualByComparingTo("30.00");
        }
    }
}
