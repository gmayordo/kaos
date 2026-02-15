package com.kaos.calendario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.kaos.calendario.dto.VacacionRequest;
import com.kaos.calendario.dto.VacacionResponse;
import com.kaos.calendario.entity.EstadoVacacion;
import com.kaos.calendario.entity.TipoVacacion;
import com.kaos.calendario.entity.Vacacion;
import com.kaos.calendario.mapper.VacacionMapper;
import com.kaos.calendario.repository.VacacionRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;

/**
 * Tests unitarios para {@link VacacionService}.
 * Valida cálculo de días laborables (CA-10), validación solapamiento (CA-09) y CRUD básico.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VacacionService Tests")
class VacacionServiceTest {

    @Mock
    private VacacionRepository repository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private VacacionMapper mapper;

    @InjectMocks
    private VacacionService service;

    private Vacacion vacacionMock;
    private VacacionRequest requestMock;
    private VacacionResponse responseMock;
    private Persona personaMock;

    @BeforeEach
    void setUp() {
        // Persona mock
        personaMock = new Persona();
        personaMock.setId(1L);
        personaMock.setNombre("Juan Pérez");
        personaMock.setEmail("juan.perez@kaos.com");

        // Request mock para vacación de 1 semana
        requestMock = new VacacionRequest(
                1L, // personaId
                LocalDate.of(2026, 3, 2), // lunes
                LocalDate.of(2026, 3, 8), // domingo
                TipoVacacion.VACACIONES,
                EstadoVacacion.SOLICITADA,
                "Vacaciones de marzo"
        );

        // Entity mock
        vacacionMock = Vacacion.builder()
                .id(1L)
                .persona(personaMock)
                .fechaInicio(LocalDate.of(2026, 3, 2))
                .fechaFin(LocalDate.of(2026, 3, 8))
                .diasLaborables(5)
                .tipo(TipoVacacion.VACACIONES)
                .estado(EstadoVacacion.REGISTRADA)
                .comentario("Vacaciones de marzo")
                .build();

        // Response mock
        responseMock = new VacacionResponse(
                1L,
                1L,
                "Juan Pérez",
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 8),
                5,
                TipoVacacion.VACACIONES,
                EstadoVacacion.REGISTRADA,
                "Vacaciones de marzo",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ══════════════════════════════════════════════════════════
    // CALCULAR DÍAS LABORABLES - CA-10
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calcularDiasLaborables() - Tests de cálculo (CA-10)")
    class CalcularDiasLaborablesTests {

        @Test
        @DisplayName("Mismo día lunes retorna 1 día laborable")
        void crear_mismoDiaLunes_retorna1DiaLaborable() {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 3, 2), // lunes
                    LocalDate.of(2026, 3, 2), // mismo día
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "Mismo día"
            );

            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsSolapamiento(anyLong(), isNull(), any(), any())).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(vacacionMock);
            when(repository.save(any(Vacacion.class))).thenAnswer(inv -> {
                Vacacion v = inv.getArgument(0);
                v.setId(1L);
                return v;
            });
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.crear(request);

            // then
            verify(repository).save(argThat(v -> v.getDiasLaborables() == 1));
        }

        @Test
        @DisplayName("Mismo día sábado retorna 0 días laborables")
        void crear_mismoDiaSabado_retorna0DiasLaborables() {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 3, 7), // sábado
                    LocalDate.of(2026, 3, 7),
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "Sábado"
            );

            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsSolapamiento(anyLong(), isNull(), any(), any())).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(vacacionMock);
            when(repository.save(any(Vacacion.class))).thenAnswer(inv -> {
                Vacacion v = inv.getArgument(0);
                v.setId(1L);
                return v;
            });
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.crear(request);

            // then
            verify(repository).save(argThat(v -> v.getDiasLaborables() == 0));
        }

        @Test
        @DisplayName("Semana completa lun-dom retorna 5 días laborables")
        void crear_semanaCompleta_retorna5DiasLaborables() {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 3, 2), // lunes
                    LocalDate.of(2026, 3, 8), // domingo
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "Semana completa"
            );

            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsSolapamiento(anyLong(), isNull(), any(), any())).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(vacacionMock);
            when(repository.save(any(Vacacion.class))).thenAnswer(inv -> {
                Vacacion v = inv.getArgument(0);
                v.setId(1L);
                return v;
            });
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.crear(request);

            // then
            verify(repository).save(argThat(v -> v.getDiasLaborables() == 5));
        }

        @Test
        @DisplayName("Viernes a lunes retorna 2 días laborables")
        void crear_viernesALunes_retorna2DiasLaborables() {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 3, 6), // viernes
                    LocalDate.of(2026, 3, 9), // lunes
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "Viernes a lunes"
            );

            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsSolapamiento(anyLong(), isNull(), any(), any())).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(vacacionMock);
            when(repository.save(any(Vacacion.class))).thenAnswer(inv -> {
                Vacacion v = inv.getArgument(0);
                v.setId(1L);
                return v;
            });
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.crear(request);

            // then
            verify(repository).save(argThat(v -> v.getDiasLaborables() == 2));
        }

        @Test
        @DisplayName("Solo fin de semana retorna 0 días laborables")
        void crear_soloFinDeSemana_retorna0DiasLaborables() {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 3, 7), // sábado
                    LocalDate.of(2026, 3, 8), // domingo
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "Fin de semana"
            );

            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsSolapamiento(anyLong(), isNull(), any(), any())).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(vacacionMock);
            when(repository.save(any(Vacacion.class))).thenAnswer(inv -> {
                Vacacion v = inv.getArgument(0);
                v.setId(1L);
                return v;
            });
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.crear(request);

            // then
            verify(repository).save(argThat(v -> v.getDiasLaborables() == 0));
        }

        @Test
        @DisplayName("2 semanas completas retorna 10 días laborables")
        void crear_dosSemanasCompletas_retorna10DiasLaborables() {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 3, 2), // lunes
                    LocalDate.of(2026, 3, 15), // domingo (2 semanas)
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "2 semanas"
            );

            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsSolapamiento(anyLong(), isNull(), any(), any())).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(vacacionMock);
            when(repository.save(any(Vacacion.class))).thenAnswer(inv -> {
                Vacacion v = inv.getArgument(0);
                v.setId(1L);
                return v;
            });
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.crear(request);

            // then
            verify(repository).save(argThat(v -> v.getDiasLaborables() == 10));
        }

        @Test
        @DisplayName("Mes completo marzo 2026 retorna 23 días laborables")
        void crear_mesCompleto_retorna23DiasLaborables() {
            // given
            VacacionRequest request = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 3, 1), // domingo
                    LocalDate.of(2026, 3, 31), // martes
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "Mes completo"
            );

            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsSolapamiento(anyLong(), isNull(), any(), any())).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(vacacionMock);
            when(repository.save(any(Vacacion.class))).thenAnswer(inv -> {
                Vacacion v = inv.getArgument(0);
                v.setId(1L);
                return v;
            });
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.crear(request);

            // then
            // Marzo 2026: 31 días - 8 fines de semana = 23 días laborables
            verify(repository).save(argThat(v -> v.getDiasLaborables() == 23));
        }
    }

    // ══════════════════════════════════════════════════════════
    // VALIDACIÓN SOLAPAMIENTO - CA-09
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validación Solapamiento - CA-09")
    class ValidacionSolapamientoTests {

        @Test
        @DisplayName("crear() sin solapamiento crea vacación correctamente")
        void crear_sinSolapamiento_creaCorrectamente() {
            // given
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsSolapamiento(1L, null, requestMock.fechaInicio(), requestMock.fechaFin()))
                    .thenReturn(false);
            when(mapper.toEntity(requestMock)).thenReturn(vacacionMock);
            when(repository.save(any(Vacacion.class))).thenReturn(vacacionMock);
            when(mapper.toResponse(vacacionMock)).thenReturn(responseMock);

            // when
            VacacionResponse result = service.crear(requestMock);

            // then
            assertThat(result).isNotNull();
            verify(repository).existsSolapamiento(1L, null, requestMock.fechaInicio(), requestMock.fechaFin());
            verify(repository).save(any(Vacacion.class));
        }

        @Test
        @DisplayName("crear() con solapamiento lanza IllegalArgumentException")
        void crear_conSolapamiento_lanzaExcepcion() {
            // given
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsSolapamiento(1L, null, requestMock.fechaInicio(), requestMock.fechaFin()))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.crear(requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe una vacación para esta persona");

            verify(repository).existsSolapamiento(1L, null, requestMock.fechaInicio(), requestMock.fechaFin());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("actualizar() con self-exclusion permite actualizar fechas sin solapamiento")
        void actualizar_sinSolapamientoPropio_actualizaCorrectamente() {
            // given
            when(repository.findById(1L)).thenReturn(Optional.of(vacacionMock));
            when(repository.existsSolapamiento(1L, 1L, requestMock.fechaInicio(), requestMock.fechaFin()))
                    .thenReturn(false); // No hay solapamiento con otras vacaciones
            when(repository.save(any(Vacacion.class))).thenReturn(vacacionMock);
            when(mapper.toResponse(vacacionMock)).thenReturn(responseMock);

            // when
            VacacionResponse result = service.actualizar(1L, requestMock);

            // then
            assertThat(result).isNotNull();
            verify(repository).existsSolapamiento(1L, 1L, requestMock.fechaInicio(), requestMock.fechaFin());
            verify(repository).save(any(Vacacion.class));
        }

        @Test
        @DisplayName("actualizar() con solapamiento (no self) lanza excepción")
        void actualizar_conSolapamientoOtraVacacion_lanzaExcepcion() {
            // given
            when(repository.findById(1L)).thenReturn(Optional.of(vacacionMock));
            when(repository.existsSolapamiento(1L, 1L, requestMock.fechaInicio(), requestMock.fechaFin()))
                    .thenReturn(true); // Hay solapamiento con otra vacación

            // when & then
            assertThatThrownBy(() -> service.actualizar(1L, requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe una vacación para esta persona");

            verify(repository).existsSolapamiento(1L, 1L, requestMock.fechaInicio(), requestMock.fechaFin());
            verify(repository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════
    // CRUD BÁSICO
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CRUD Básico")
    class CrudBasicoTests {

        @Test
        @DisplayName("crear() valida fechaFin >= fechaInicio")
        void crear_fechaFinAnterior_lanzaExcepcion() {
            // given
            VacacionRequest invalidRequest = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 3, 10),
                    LocalDate.of(2026, 3, 5), // fin anterior a inicio
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "Fechas inválidas"
            );

            // when & then
            assertThatThrownBy(() -> service.crear(invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fecha de fin debe ser posterior o igual");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("crear() con persona inexistente lanza excepción")
        void crear_personaInexistente_lanzaExcepcion() {
            // given
            when(personaRepository.findById(999L)).thenReturn(Optional.empty());

            VacacionRequest request = new VacacionRequest(
                    999L,
                    LocalDate.of(2026, 3, 2),
                    LocalDate.of(2026, 3, 8),
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "Persona inexistente"
            );

            // when & then
            assertThatThrownBy(() -> service.crear(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Persona no encontrada: 999");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("obtener(id) retorna vacación existente")
        void obtener_vacacionExistente_retornaResponse() {
            // given
            when(repository.findById(1L)).thenReturn(Optional.of(vacacionMock));
            when(mapper.toResponse(vacacionMock)).thenReturn(responseMock);

            // when
            VacacionResponse result = service.obtener(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("obtener(id) vacación inexistente lanza excepción")
        void obtener_vacacionNoExiste_lanzaExcepcion() {
            // given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.obtener(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Vacación no encontrada: 999");
        }

        @Test
        @DisplayName("actualizar() recalcula días laborables al cambiar fechas")
        void actualizar_cambiaFechas_recalculaDiasLaborables() {
            // given
            VacacionRequest updateRequest = new VacacionRequest(
                    1L,
                    LocalDate.of(2026, 3, 2),
                    LocalDate.of(2026, 3, 15), // Cambia fin (2 semanas ahora)
                    TipoVacacion.VACACIONES,
                    EstadoVacacion.REGISTRADA,
                    "Extendida"
            );

            when(repository.findById(1L)).thenReturn(Optional.of(vacacionMock));
            when(repository.existsSolapamiento(anyLong(), anyLong(), any(), any())).thenReturn(false);
            when(repository.save(any(Vacacion.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.actualizar(1L, updateRequest);

            // then
            verify(repository).save(argThat(v -> v.getDiasLaborables() == 10)); // 2 semanas = 10 días
        }

        @Test
        @DisplayName("eliminar() vacación existente elimina correctamente")
        void eliminar_vacacionExistente_eliminaCorrectamente() {
            // given
            when(repository.existsById(1L)).thenReturn(true);
            doNothing().when(repository).deleteById(1L);

            // when
            service.eliminar(1L);

            // then
            verify(repository).existsById(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("eliminar() vacación inexistente lanza excepción")
        void eliminar_vacacionNoExiste_lanzaExcepcion() {
            // given
            when(repository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> service.eliminar(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Vacación no encontrada: 999");

            verify(repository, never()).deleteById(any());
        }
    }

    // ══════════════════════════════════════════════════════════
    // LISTAR Y FILTROS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Listar y filtros")
    class ListarTests {

        @Test
        @DisplayName("listar() sin filtros retorna todas las vacaciones")
        void listar_sinFiltros_retornaTodas() {
            // given
            List<Vacacion> vacaciones = List.of(vacacionMock);
            when(repository.findAll()).thenReturn(vacaciones);
            when(mapper.toResponseList(vacaciones)).thenReturn(List.of(responseMock));

            // when
            List<VacacionResponse> result = service.listar(null, null, null, null);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findAll();
        }

        @Test
        @DisplayName("listar(personaId) filtra por persona")
        void listar_conPersonaId_filtraPorPersona() {
            // given
            List<Vacacion> vacaciones = List.of(vacacionMock);
            when(repository.findByPersonaId(1L)).thenReturn(vacaciones);
            when(mapper.toResponseList(vacaciones)).thenReturn(List.of(responseMock));

            // when
            List<VacacionResponse> result = service.listar(1L, null, null, null);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findByPersonaId(1L);
            verify(repository, never()).findAll();
        }

        @Test
        @DisplayName("listar(squadId) filtra por squad")
        void listar_conSquadId_filtraPorSquad() {
            // given
            List<Vacacion> vacaciones = List.of(vacacionMock);
            when(repository.findBySquadIdAndFechaRange(5L, null, null)).thenReturn(vacaciones);
            when(mapper.toResponseList(vacaciones)).thenReturn(List.of(responseMock));

            // when
            List<VacacionResponse> result = service.listar(null, 5L, null, null);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findBySquadIdAndFechaRange(5L, null, null);
        }

        @Test
        @DisplayName("listarPorSquad(id, rango) retorna vacaciones del squad en rango")
        void listarPorSquad_conRango_retornaVacacionesEnRango() {
            // given
            LocalDate inicio = LocalDate.of(2026, 3, 1);
            LocalDate fin = LocalDate.of(2026, 3, 31);
            List<Vacacion> vacaciones = List.of(vacacionMock);

            when(repository.findBySquadIdAndFechaRange(5L, inicio, fin)).thenReturn(vacaciones);
            when(mapper.toResponseList(vacaciones)).thenReturn(List.of(responseMock));

            // when
            List<VacacionResponse> result = service.listarPorSquad(5L, inicio, fin);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findBySquadIdAndFechaRange(5L, inicio, fin);
        }
    }
}
