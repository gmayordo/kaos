package com.kaos.planificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.kaos.calendario.service.CapacidadService;
import com.kaos.planificacion.dto.SprintRequest;
import com.kaos.planificacion.dto.SprintResponse;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.mapper.SprintMapper;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.squad.entity.Squad;
import com.kaos.squad.repository.SquadRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para SprintService.
 * Cubre CA-12, CA-13, CA-14.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SprintService")
@SuppressWarnings("null")
class SprintServiceTest {

    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private SquadRepository squadRepository;
    @Mock
    private SprintMapper sprintMapper;
    @Mock
    private CapacidadService capacidadService;

    @InjectMocks
    private SprintService sprintService;

    private Sprint sprint;
    private Squad squad;
    private SprintResponse sprintResponse;
    private LocalDate lunes;
    private LocalDate domingo;

    @BeforeEach
    void setUp() {
        squad = new Squad();
        squad.setId(1L);
        squad.setNombre("Squad Backend");

        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setNombre("Sprint 1");
        sprint.setSquad(squad);
        sprint.setEstado(SprintEstado.PLANIFICACION);
        sprint.setCapacidadTotal(BigDecimal.valueOf(80));
        sprint.setCreatedAt(LocalDateTime.now());

        sprintResponse = mock(SprintResponse.class);
        when(sprintResponse.id()).thenReturn(1L);

        // Fecha que sea lunes para la validación del servicio
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        domingo = lunes.plusDays(13);
    }

    @Nested
    @DisplayName("obtener()")
    class ObtenerTests {

        @Test
        @DisplayName("CA-12: Sprint existente retorna SprintResponse")
        void testObtenerExistente() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(sprintMapper.toResponse(sprint)).thenReturn(sprintResponse);

            SprintResponse result = sprintService.obtener(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("CA-12: Sprint inexistente lanza EntityNotFoundException")
        void testObtenerNoExistente() {
            when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sprintService.obtener(99L))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("crear()")
    class CrearTests {

        @Test
        @DisplayName("CA-12: Crear sprint válido persiste y retorna respuesta")
        void testCrearValido() {
            var capacidadMock = mock(com.kaos.calendario.dto.CapacidadSquadResponse.class);
            when(capacidadMock.horasTotales()).thenReturn(80.0);

            var squad2 = new Squad();
            squad2.setId(2L);
            squad2.setNombre("Squad Frontend");

            var sprint2 = new Sprint();
            sprint2.setId(2L);
            sprint2.setNombre("Sprint 1");
            sprint2.setSquad(squad2);
            sprint2.setEstado(SprintEstado.PLANIFICACION);
            sprint2.setCapacidadTotal(BigDecimal.valueOf(80));
            sprint2.setCreatedAt(LocalDateTime.now());

            SprintRequest request = new SprintRequest("Sprint 1", 1L, lunes, "Objetivo");
            when(squadRepository.findAll()).thenReturn(List.of(squad, squad2));
            when(sprintRepository.existsSolapamiento(anyLong(), any(), any(), anyLong())).thenReturn(false);
            when(capacidadService.calcularCapacidad(anyLong(), any(), any())).thenReturn(capacidadMock);
            when(sprintMapper.toEntity(request)).thenReturn(sprint, sprint2);
            when(sprintRepository.saveAll(anyList())).thenReturn(List.of(sprint, sprint2));
            when(sprintMapper.toResponse(sprint)).thenReturn(sprintResponse);

            SprintResponse result = sprintService.crear(request);

            assertThat(result).isNotNull();
            verify(sprintRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("CA-12: Crear sprint con squad inexistente lanza EntityNotFoundException")
        void testCrearSquadInvalido() {
            SprintRequest request = new SprintRequest("Sprint X", 99L, lunes, "Objetivo");
            when(squadRepository.findAll()).thenReturn(List.of(squad));

            assertThatThrownBy(() -> sprintService.crear(request))
                .isInstanceOf(EntityNotFoundException.class);
            verify(sprintRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("CA-12: Crear sprint con fecha que no es lunes lanza IllegalArgumentException")
        void testCrearFechaNoLunes() {
            LocalDate martes = lunes.plusDays(1);
            SprintRequest request = new SprintRequest("Sprint X", 1L, martes, "Objetivo");
            when(squadRepository.findAll()).thenReturn(List.of(squad));

            assertThatThrownBy(() -> sprintService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lunes");
            verify(sprintRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("CA-12: Solapamiento de fechas lanza excepción")
        void testCrearConSolapamiento() {
            SprintRequest request = new SprintRequest("Sprint X", 1L, lunes, "Objetivo");
            when(squadRepository.findAll()).thenReturn(List.of(squad));
            when(sprintRepository.existsSolapamiento(anyLong(), any(), any(), anyLong())).thenReturn(true);

            assertThatThrownBy(() -> sprintService.crear(request))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("solap");
            verify(sprintRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("actualizar()")
    class ActualizarTests {

        @Test
        @DisplayName("CA-12: Actualizar sprint válido persiste cambios")
        void testActualizarValido() {
            SprintRequest request = new SprintRequest("Sprint 1 Actualizado", 1L, lunes, "Objetivo Actualizado");
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(sprintRepository.existsSolapamiento(anyLong(), any(), any(), anyLong())).thenReturn(false);
            when(sprintRepository.save(any(Sprint.class))).thenReturn(sprint);
            when(sprintMapper.toResponse(sprint)).thenReturn(sprintResponse);

            SprintResponse result = sprintService.actualizar(1L, request);

            assertThat(result).isNotNull();
            verify(sprintRepository).save(any(Sprint.class));
        }

        @Test
        @DisplayName("CA-12: Actualizar sprint inexistente lanza EntityNotFoundException")
        void testActualizarInexistente() {
            SprintRequest request = new SprintRequest("Sprint X", 1L, lunes, "Objetivo");
            when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sprintService.actualizar(99L, request))
                .isInstanceOf(EntityNotFoundException.class);
            verify(sprintRepository, never()).save(any(Sprint.class));
        }
    }

    @Nested
    @DisplayName("cambiarEstado()")
    class CambiarEstadoTests {

        @Test
        @DisplayName("CA-13: PLANIFICACION → ACTIVO es válido")
        void testCambiarEstadoPlanificacionAActivo() {
            sprint.setEstado(SprintEstado.PLANIFICACION);
            sprint.setFechaInicio(lunes);
            sprint.setFechaFin(domingo);
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(sprintRepository.findByNombreAndFechaInicioAndFechaFin(
                sprint.getNombre(), lunes, domingo)).thenReturn(List.of(sprint));
            when(sprintRepository.saveAll(anyList())).thenReturn(List.of(sprint));
            when(sprintMapper.toResponseList(List.of(sprint))).thenReturn(List.of(sprintResponse));

            List<SprintResponse> result = sprintService.cambiarEstado(1L, SprintEstado.ACTIVO);

            assertThat(result).isNotNull();
            verify(sprintRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("CA-13: ACTIVO → CERRADO es válido")
        void testCambiarEstadoActivoACerrado() {
            sprint.setEstado(SprintEstado.ACTIVO);
            sprint.setFechaInicio(lunes);
            sprint.setFechaFin(domingo);
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(sprintRepository.findByNombreAndFechaInicioAndFechaFin(
                sprint.getNombre(), lunes, domingo)).thenReturn(List.of(sprint));
            when(sprintRepository.saveAll(anyList())).thenReturn(List.of(sprint));
            when(sprintMapper.toResponseList(List.of(sprint))).thenReturn(List.of(sprintResponse));

            List<SprintResponse> result = sprintService.cambiarEstado(1L, SprintEstado.CERRADO);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("CA-13: Transición inválida CERRADO → PLANIFICACION lanza excepción")
        void testCambiarEstadoCerradoAInvalido() {
            sprint.setEstado(SprintEstado.CERRADO);
            sprint.setFechaInicio(lunes);
            sprint.setFechaFin(domingo);
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(sprintRepository.findByNombreAndFechaInicioAndFechaFin(
                sprint.getNombre(), lunes, domingo)).thenReturn(List.of(sprint));

            assertThatThrownBy(() -> sprintService.cambiarEstado(1L, SprintEstado.PLANIFICACION))
                .isInstanceOf(Exception.class);
            verify(sprintRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("listar()")
    class ListarTests {

        @Test
        @DisplayName("CA-14: Listar por squadId con paginación retorna page")
        void testListarPorSquad() {
            var pageable = PageRequest.of(0, 10);
            @SuppressWarnings("null")
            var page = new PageImpl<>(List.of(sprint));
            when(sprintRepository.findBySquadId(1L, pageable)).thenReturn(page);
            when(sprintMapper.toResponse(sprint)).thenReturn(sprintResponse);

            var result = sprintService.listar(1L, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("CA-14: Listar por squadId + estado aplica filtro combinado")
        void testListarPorSquadYEstado() {
            var pageable = PageRequest.of(0, 10);
            @SuppressWarnings("null")
            var page = new PageImpl<>(List.of(sprint));
            when(sprintRepository.findBySquadIdAndEstado(1L, SprintEstado.ACTIVO, pageable))
                .thenReturn(page);
            when(sprintMapper.toResponse(sprint)).thenReturn(sprintResponse);

            var result = sprintService.listar(1L, SprintEstado.ACTIVO, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("eliminar()")
    class EliminarTests {

        @Test
        @DisplayName("CA-14: Eliminar sprint en PLANIFICACION ejecuta deleteById")
        void testEliminarEnPlanificacion() {
            sprint.setEstado(SprintEstado.PLANIFICACION);
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));

            sprintService.eliminar(1L);

            verify(sprintRepository).deleteById(1L);
        }

        @Test
        @DisplayName("CA-14: Eliminar sprint ACTIVO lanza excepción")
        void testEliminarInvalido() {
            sprint.setEstado(SprintEstado.ACTIVO);
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));

            assertThatThrownBy(() -> sprintService.eliminar(1L))
                .isInstanceOf(Exception.class);
            verify(sprintRepository, never()).deleteById(anyLong());
        }
    }
}
