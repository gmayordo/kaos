package com.kaos.calendario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.kaos.calendario.dto.CapacidadDiaResponse;
import com.kaos.calendario.dto.CapacidadPersonaResponse;
import com.kaos.calendario.dto.CapacidadSquadResponse;
import com.kaos.calendario.entity.Ausencia;
import com.kaos.calendario.entity.EstadoVacacion;
import com.kaos.calendario.entity.Festivo;
import com.kaos.calendario.entity.MotivoReduccion;
import com.kaos.calendario.entity.TipoAusencia;
import com.kaos.calendario.entity.TipoFestivo;
import com.kaos.calendario.entity.TipoVacacion;
import com.kaos.calendario.entity.Vacacion;
import com.kaos.calendario.repository.AusenciaRepository;
import com.kaos.calendario.repository.FestivoRepository;
import com.kaos.calendario.repository.VacacionRepository;
import com.kaos.dedicacion.entity.SquadMember;
import com.kaos.dedicacion.repository.SquadMemberRepository;
import com.kaos.horario.entity.PerfilHorario;
import com.kaos.persona.entity.Persona;
import com.kaos.squad.entity.Squad;
import com.kaos.squad.repository.SquadRepository;

/**
 * Tests unitarios para {@link CapacidadService}.
 * Valida motor de cálculo día-a-día con todas las reglas de negocio (CA-12).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CapacidadService Tests - Motor de cálculo CA-12")
class CapacidadServiceTest {

    @Mock
    private SquadRepository squadRepository;

    @Mock
    private SquadMemberRepository squadMemberRepository;

    @Mock
    private FestivoRepository festivoRepository;

    @Mock
    private VacacionRepository vacacionRepository;

    @Mock
    private AusenciaRepository ausenciaRepository;

    @InjectMocks
    private CapacidadService service;

    private Squad squadMock;
    private Persona personaMock;
    private PerfilHorario perfilCompleto;
    private SquadMember miembroMock;

    @BeforeEach
    void setUp() {
        // Perfil horario estándar 40h/semana
        perfilCompleto = new PerfilHorario();
        perfilCompleto.setId(1L);
        perfilCompleto.setNombre("Completo 40h");
        perfilCompleto.setTotalSemanal(BigDecimal.valueOf(40));

        // Persona mock
        personaMock = new Persona();
        personaMock.setId(1L);
        personaMock.setNombre("Juan Pérez");
        personaMock.setEmail("juan.perez@kaos.com");
        personaMock.setPerfilHorario(perfilCompleto);

        // Squad mock
        squadMock = new Squad();
        squadMock.setId(1L);
        squadMock.setNombre("Squad Alpha");

        // Miembro del squad con 100% dedicación
        miembroMock = new SquadMember();
        miembroMock.setId(1L);
        miembroMock.setSquad(squadMock);
        miembroMock.setPersona(personaMock);
        miembroMock.setPorcentaje(100);
    }

    // ══════════════════════════════════════════════════════════
    // VALIDACIÓN ENTRADA
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validación de entrada")
    class ValidacionEntradaTests {

        @Test
        @DisplayName("calcularCapacidad() valida fechaFin >= fechaInicio")
        void calcularCapacidad_fechaFinAnterior_lanzaExcepcion() {
            // when & then
            assertThatThrownBy(() -> service.calcularCapacidad(
                    1L,
                    LocalDate.of(2026, 3, 10),
                    LocalDate.of(2026, 3, 5)
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha de fin debe ser posterior o igual");
        }

        @Test
        @DisplayName("calcularCapacidad() squad inexistente lanza excepción")
        void calcularCapacidad_squadInexistente_lanzaExcepcion() {
            // given
            when(squadRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.calcularCapacidad(
                    999L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31)
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Squad no encontrado: 999");
        }

        @Test
        @DisplayName("calcularCapacidad() squad sin miembros retorna 0 horas")
        void calcularCapacidad_squadSinMiembros_retornaCeroHoras() {
            // given
            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of()); // Sin miembros

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31)
            );

            // then
            assertThat(result.horasTotales()).isZero();
            assertThat(result.personas()).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════
    // DÍA NORMAL SIN REDUCCIONES
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Día normal sin reducciones")
    class DiaNormalTests {

        @Test
        @DisplayName("Lunes normal: persona 40h/sem 100% dedicación retorna 8h")
        void calcularCapacidad_lunesNormal_retorna8Horas() {
            // given
            LocalDate lunes = LocalDate.of(2026, 3, 2); // Lunes sin festivos/vacaciones

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(1L, lunes, lunes)).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(1L, lunes, lunes)).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, lunes, lunes);

            // then
            assertThat(result.horasTotales()).isEqualTo(8.0);
            assertThat(result.personas()).hasSize(1);

            CapacidadPersonaResponse persona = result.personas().get(0);
            assertThat(persona.detalles()).isEqualTo(8.0);
            assertThat(persona.detalles()).hasSize(1);

            CapacidadDiaResponse dia = persona.detalles().get(0);
            assertThat(dia.horasDisponibles()).isEqualTo(8.0);
            assertThat(dia.porcentajeCapacidad()).isEqualTo(100);
            assertThat(dia.motivoReduccion()).isNull(); // Sin reducción
        }

        @Test
        @DisplayName("Semana completa lun-vie: 5 días × 8h = 40h")
        void calcularCapacidad_semanaCompleta_retorna40Horas() {
            // given
            LocalDate lunes = LocalDate.of(2026, 3, 2);
            LocalDate viernes = LocalDate.of(2026, 3, 6);

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(1L, lunes, viernes)).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(1L, lunes, viernes)).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, lunes, viernes);

            // then
            assertThat(result.horasTotales()).isEqualTo(40.0); // 5 días × 8h
            CapacidadPersonaResponse persona = result.personas().get(0);
            assertThat(persona.detalles()).hasSize(5);
            assertThat(persona.detalles()).allMatch(d -> d.horasDisponibles() == 8.0);
        }
    }

    // ══════════════════════════════════════════════════════════
    // FIN DE SEMANA
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Fin de semana")
    class FinDeSemanaTests {

        @Test
        @DisplayName("Sábado retorna 0h con motivo FIN_SEMANA")
        void calcularCapacidad_sabado_retorna0Horas() {
            // given
            LocalDate sabado = LocalDate.of(2026, 3, 7);

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(1L, sabado, sabado)).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(1L, sabado, sabado)).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, sabado, sabado);

            // then
            assertThat(result.horasTotales()).isZero();
            CapacidadDiaResponse dia = result.personas().get(0).detalles().get(0);
            assertThat(dia.horasDisponibles()).isZero();
            assertThat(dia.porcentajeCapacidad()).isZero();
            assertThat(dia.motivoReduccion()).isEqualTo(MotivoReduccion.FIN_SEMANA);
        }

        @Test
        @DisplayName("Domingo retorna 0h con motivo FIN_SEMANA")
        void calcularCapacidad_domingo_retorna0Horas() {
            // given
            LocalDate domingo = LocalDate.of(2026, 3, 8);

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(1L, domingo, domingo)).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(1L, domingo, domingo)).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, domingo, domingo);

            // then
            CapacidadDiaResponse dia = result.personas().get(0).detalles().get(0);
            assertThat(dia.motivoReduccion()).isEqualTo(MotivoReduccion.FIN_SEMANA);
        }

        @Test
        @DisplayName("Semana lun-dom: solo lun-vie tienen capacidad")
        void calcularCapacidad_semanaConFinde_soloLaboralesTienenCapacidad() {
            // given
            LocalDate lunes = LocalDate.of(2026, 3, 2);
            LocalDate domingo = LocalDate.of(2026, 3, 8);

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(1L, lunes, domingo)).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(1L, lunes, domingo)).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, lunes, domingo);

            // then
            assertThat(result.horasTotales()).isEqualTo(40.0); // Solo 5 días laborables
            CapacidadPersonaResponse persona = result.personas().get(0);
            assertThat(persona.detalles()).hasSize(7);

            // Filtrar días de fin de semana
            long diasFinde = persona.detalles().stream()
                    .filter(d -> d.motivoReduccion() == MotivoReduccion.FIN_SEMANA)
                    .count();
            assertThat(diasFinde).isEqualTo(2); // Sábado y domingo
        }
    }

    // ══════════════════════════════════════════════════════════
    // FESTIVOS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Festivos")
    class FestivosTests {

        @Test
        @DisplayName("Festivo nacional retorna 0h con motivo FESTIVO")
        void calcularCapacidad_festivoNacional_retorna0Horas() {
            // given
            LocalDate anioNuevo = LocalDate.of(2026, 1, 1);

            Festivo festivo = Festivo.builder()
                    .id(1L)
                    .fecha(anioNuevo)
                    .descripcion("Año Nuevo")
                    .tipo(TipoFestivo.NACIONAL)
                    .ciudad("Zaragoza")
                    .build();

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of(festivo));
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, anioNuevo, anioNuevo);

            // then
            assertThat(result.horasTotales()).isZero();
            CapacidadDiaResponse dia = result.personas().get(0).detalles().get(0);
            assertThat(dia.horasDisponibles()).isZero();
            assertThat(dia.motivoReduccion()).isEqualTo(MotivoReduccion.FESTIVO);
        }

        @Test
        @DisplayName("Festivo no asignado a la persona NO reduce capacidad")
        void calcularCapacidad_festivoNoAsignado_capacidadNormal() {
            // given
            LocalDate fecha = LocalDate.of(2026, 1, 6); // Jueves

            Persona otraPersona = new Persona();
            otraPersona.setId(2L);

            Festivo festivo = Festivo.builder()
                    .id(1L)
                    .fecha(fecha)
                    .descripcion("Festivo regional")
                    .tipo(TipoFestivo.REGIONAL)
                    .ciudad("Valencia") // Ciudad diferente, no aplica
                    .build();

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of(festivo));
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, fecha, fecha);

            // then
            assertThat(result.horasTotales()).isEqualTo(8.0); // Día normal
            CapacidadDiaResponse dia = result.personas().get(0).detalles().get(0);
            assertThat(dia.motivoReduccion()).isNull(); // Sin reducción
        }
    }

    // ══════════════════════════════════════════════════════════
    // VACACIONES
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Vacaciones")
    class VacacionesTests {

        @Test
        @DisplayName("Día con vacación retorna 0h con motivo VACACION")
        void calcularCapacidad_diaConVacacion_retorna0Horas() {
            // given
            LocalDate fecha = LocalDate.of(2026, 3, 10);

            Vacacion vacacion = Vacacion.builder()
                    .id(1L)
                    .persona(personaMock)
                    .fechaInicio(LocalDate.of(2026, 3, 10))
                    .fechaFin(LocalDate.of(2026, 3, 14))
                    .diasLaborables(5)
                    .tipo(TipoVacacion.VACACIONES)
                    .estado(EstadoVacacion.REGISTRADA)
                    .build();

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of(vacacion));
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, fecha, fecha);

            // then
            assertThat(result.horasTotales()).isZero();
            CapacidadDiaResponse dia = result.personas().get(0).detalles().get(0);
            assertThat(dia.horasDisponibles()).isZero();
            assertThat(dia.motivoReduccion()).isEqualTo(MotivoReduccion.VACACION);
        }

        @Test
        @DisplayName("Rango con vacación parcial: antes OK, durante 0h, después OK")
        void calcularCapacidad_rangoConVacacionParcial_capacidadCorrecta() {
            // given
            LocalDate inicio = LocalDate.of(2026, 3, 9); // Lunes antes vacación
            LocalDate fin = LocalDate.of(2026, 3, 16); // Lunes después vacación

            Vacacion vacacion = Vacacion.builder()
                    .id(1L)
                    .persona(personaMock)
                    .fechaInicio(LocalDate.of(2026, 3, 10)) // Mar
                    .fechaFin(LocalDate.of(2026, 3, 14)) // Vie (5 días)
                    .diasLaborables(5)
                    .tipo(TipoVacacion.VACACIONES)
                    .estado(EstadoVacacion.REGISTRADA)
                    .build();

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of(vacacion));
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, inicio, fin);

            // then
            // Lun 9: 8h, Mar-Vie (10-14): 0h×5, Sab-Dom: 0h (finde), Lun 16: 8h = 16h total
            assertThat(result.horasTotales()).isEqualTo(16.0);
        }
    }

    // ══════════════════════════════════════════════════════════
    // AUSENCIAS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Ausencias")
    class AusenciasTests {

        @Test
        @DisplayName("Día con ausencia definida retorna 0h con motivo AUSENCIA")
        void calcularCapacidad_ausenciaDefinida_retorna0Horas() {
            // given
            LocalDate fecha = LocalDate.of(2026, 3, 10);

            Ausencia ausencia = Ausencia.builder()
                    .id(1L)
                    .persona(personaMock)
                    .fechaInicio(LocalDate.of(2026, 3, 1))
                    .fechaFin(LocalDate.of(2026, 3, 15))
                    .tipo(TipoAusencia.BAJA_MEDICA)
                    .build();

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of(ausencia));

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, fecha, fecha);

            // then
            assertThat(result.horasTotales()).isZero();
            CapacidadDiaResponse dia = result.personas().get(0).detalles().get(0);
            assertThat(dia.motivoReduccion()).isEqualTo(MotivoReduccion.AUSENCIA);
        }

        @Test
        @DisplayName("Ausencia indefinida (fechaFin=null) retorna 0h en cualquier fecha posterior")
        void calcularCapacidad_ausenciaIndefinida_retorna0HorasSinFin() {
            // given
            LocalDate fechaConsulta = LocalDate.of(2026, 3, 15);

            Ausencia ausenciaIndefinida = Ausencia.builder()
                    .id(1L)
                    .persona(personaMock)
                    .fechaInicio(LocalDate.of(2026, 3, 1))
                    .fechaFin(null) // Indefinida
                    .tipo(TipoAusencia.BAJA_MEDICA)
                    .build();

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any()))
                    .thenReturn(List.of(ausenciaIndefinida));

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, fechaConsulta, fechaConsulta);

            // then
            assertThat(result.horasTotales()).isZero();
            CapacidadDiaResponse dia = result.personas().get(0).detalles().get(0);
            assertThat(dia.motivoReduccion()).isEqualTo(MotivoReduccion.AUSENCIA);
        }
    }

    // ══════════════════════════════════════════════════════════
    // DEDICACIÓN PARCIAL
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dedicación parcial")
    class DedicacionParcialTests {

        @Test
        @DisplayName("Persona 50% dedicación retorna 4h/día")
        void calcularCapacidad_50Dedicacion_retorna4Horas() {
            // given
            LocalDate fecha = LocalDate.of(2026, 3, 2); // Lunes
            miembroMock.setPorcentaje(50);

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, fecha, fecha);

            // then
            assertThat(result.horasTotales()).isEqualTo(4.0);
            CapacidadDiaResponse dia = result.personas().get(0).detalles().get(0);
            assertThat(dia.horasDisponibles()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Persona 25% dedicación retorna 2h/día")
        void calcularCapacidad_25Dedicacion_retorna2Horas() {
            // given
            LocalDate fecha = LocalDate.of(2026, 3, 2);
            miembroMock.setPorcentaje(25);

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, fecha, fecha);

            // then
            assertThat(result.horasTotales()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Persona 30h/sem (reducción jornada) 100% dedicación retorna 6h/día")
        void calcularCapacidad_perfilReducido_retorna6Horas() {
            // given
            LocalDate fecha = LocalDate.of(2026, 3, 2);

            PerfilHorario perfilReducido = new PerfilHorario();
            perfilReducido.setId(2L);
            perfilReducido.setNombre("Reducido 30h");
            perfilReducido.setTotalSemanal(BigDecimal.valueOf(30));

            personaMock.setPerfilHorario(perfilReducido);

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L)).thenReturn(List.of(miembroMock));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, fecha, fecha);

            // then
            assertThat(result.horasTotales()).isEqualTo(6.0); // 30h/sem ÷ 5 días = 6h/día
        }
    }

    // ══════════════════════════════════════════════════════════
    // SQUAD CON MÚLTIPLES PERSONAS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Squad con múltiples personas")
    class SquadMultiPersonaTests {

        @Test
        @DisplayName("Squad con 3 personas normales suma capacidades (3×8h = 24h)")
        void calcularCapacidad_3PersonasNormales_suma24Horas() {
            // given
            LocalDate fecha = LocalDate.of(2026, 3, 2); // Lunes

            Persona p2 = crearPersona(2L, "Maria García", perfilCompleto);
            Persona p3 = crearPersona(3L, "Pedro López", perfilCompleto);

            SquadMember m2 = crearMiembro(2L, p2, 100);
            SquadMember m3 = crearMiembro(3L, p3, 100);

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L))
                    .thenReturn(List.of(miembroMock, m2, m3));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, fecha, fecha);

            // then
            assertThat(result.horasTotales()).isEqualTo(24.0); // 3 × 8h
            assertThat(result.personas()).hasSize(3);
        }

        @Test
        @DisplayName("Squad con 1 persona de vacaciones suma solo activos")
        void calcularCapacidad_1PersonaVacaciones_sumaSoloActivos() {
            // given
            LocalDate fecha = LocalDate.of(2026, 3, 10);

            Persona p2 = crearPersona(2L, "Maria García", perfilCompleto);
            Persona p3 = crearPersona(3L, "Pedro López", perfilCompleto);

            SquadMember m2 = crearMiembro(2L, p2, 100);
            SquadMember m3 = crearMiembro(3L, p3, 100);

            Vacacion vacacionP2 = Vacacion.builder()
                    .id(1L)
                    .persona(p2)
                    .fechaInicio(fecha)
                    .fechaFin(fecha.plusDays(5))
                    .diasLaborables(5)
                    .tipo(TipoVacacion.VACACIONES)
                    .estado(EstadoVacacion.REGISTRADA)
                    .build();

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L))
                    .thenReturn(List.of(miembroMock, m2, m3));
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any()))
                    .thenReturn(List.of(vacacionP2));
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, fecha, fecha);

            // then
            assertThat(result.horasTotales()).isEqualTo(16.0); // Solo P1 y P3 (2 × 8h)
        }

        @Test
        @DisplayName("Squad con mix dedicaciones suma correctamente")
        void calcularCapacidad_mixDedicaciones_sumaCorrecta() {
            // given
            LocalDate fecha = LocalDate.of(2026, 3, 2);

            Persona p2 = crearPersona(2L, "Maria García", perfilCompleto);
            Persona p3 = crearPersona(3L, "Pedro López", perfilCompleto);

            SquadMember m2 = crearMiembro(2L, p2, 50); // 50%
            SquadMember m3 = crearMiembro(3L, p3, 25); // 25%

            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(squadMemberRepository.findBySquadId(1L))
                    .thenReturn(List.of(miembroMock, m2, m3)); // 100%, 50%, 25%
            when(festivoRepository.findByAnio(2026)).thenReturn(List.of());
            when(vacacionRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());
            when(ausenciaRepository.findBySquadIdAndFechaRange(any(), any(), any())).thenReturn(List.of());

            // when
            CapacidadSquadResponse result = service.calcularCapacidad(1L, fecha, fecha);

            // then
            assertThat(result.horasTotales()).isEqualTo(14.0); // 8 + 4 + 2
        }
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════

    private Persona crearPersona(Long id, String nombre, PerfilHorario perfil) {
        Persona persona = new Persona();
        persona.setId(id);
        persona.setNombre(nombre);
        persona.setEmail(nombre.toLowerCase().replace(" ", ".") + "@kaos.com");
        persona.setPerfilHorario(perfil);
        return persona;
    }

    private SquadMember crearMiembro(Long id, Persona persona, Integer porcentaje) {
        SquadMember miembro = new SquadMember();
        miembro.setId(id);
        miembro.setSquad(squadMock);
        miembro.setPersona(persona);
        miembro.setPorcentaje(porcentaje);
        return miembro;
    }
}
