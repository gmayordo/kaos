package com.kaos.calendario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.kaos.calendario.dto.AusenciaRequest;
import com.kaos.calendario.dto.AusenciaResponse;
import com.kaos.calendario.entity.Ausencia;
import com.kaos.calendario.entity.TipoAusencia;
import com.kaos.calendario.mapper.AusenciaMapper;
import com.kaos.calendario.repository.AusenciaRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;

/**
 * Tests unitarios para {@link AusenciaService}.
 * Valida CRUD con fechaFin nullable (CA-11).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AusenciaService Tests")
class AusenciaServiceTest {

    @Mock
    private AusenciaRepository repository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private AusenciaMapper mapper;

    @InjectMocks
    private AusenciaService service;

    private Ausencia ausenciaMock;
    private AusenciaRequest requestMock;
    private AusenciaResponse responseMock;
    private Persona personaMock;

    @BeforeEach
    void setUp() {
        // Persona mock
        personaMock = new Persona();
        personaMock.setId(1L);
        personaMock.setNombre("Juan Pérez");
        personaMock.setEmail("juan.perez@kaos.com");

        // Request mock con fechaFin definida
        requestMock = new AusenciaRequest(
                1L, // personaId
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 15), // fechaFin definida
                TipoAusencia.BAJA_MEDICA,
                "Baja médica"
        );

        // Entity mock
        ausenciaMock = Ausencia.builder()
                .id(1L)
                .persona(personaMock)
                .fechaInicio(LocalDate.of(2026, 3, 1))
                .fechaFin(LocalDate.of(2026, 3, 15))
                .tipo(TipoAusencia.BAJA_MEDICA)
                .comentario("Baja médica")
                .build();

        // Response mock
        responseMock = new AusenciaResponse(
                1L,
                1L,
                "Juan Pérez",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 15),
                TipoAusencia.BAJA_MEDICA,
                "Baja médica",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ══════════════════════════════════════════════════════════
    // FECHAFIN NULLABLE - CA-11
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("fechaFin nullable - CA-11")
    class FechaFinNullableTests {

        @Test
        @DisplayName("crear() ausencia indefinida (fechaFin=null) crea correctamente")
        void crear_ausenciaIndefinida_creaCorrectamente() {
            // given
            AusenciaRequest request = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    null, // Ausencia indefinida
                    TipoAusencia.BAJA_MEDICA,
                    "Baja sin fecha fin"
            );

            Ausencia ausenciaIndefinida = Ausencia.builder()
                    .id(1L)
                    .persona(personaMock)
                    .fechaInicio(LocalDate.of(2026, 3, 1))
                    .fechaFin(null)
                    .tipo(TipoAusencia.BAJA_MEDICA)
                    .comentario("Baja sin fecha fin")
                    .build();

            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(mapper.toEntity(request)).thenReturn(ausenciaIndefinida);
            when(repository.save(any(Ausencia.class))).thenReturn(ausenciaIndefinida);
            when(mapper.toResponse(ausenciaIndefinida)).thenReturn(
                    new AusenciaResponse(1L, 1L, "Juan Pérez", 
                            LocalDate.of(2026, 3, 1), null, 
                            TipoAusencia.BAJA_MEDICA, "Baja sin fecha fin",
                            LocalDateTime.now(), LocalDateTime.now())
            );

            // when
            AusenciaResponse result = service.crear(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.fechaFin()).isNull(); // Validar que fechaFin es null
            verify(repository).save(argThat(a -> a.getFechaFin() == null));
        }

        @Test
        @DisplayName("crear() ausencia con fechaFin definida crea correctamente")
        void crear_ausenciaConFechaFin_creaCorrectamente() {
            // given
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(mapper.toEntity(requestMock)).thenReturn(ausenciaMock);
            when(repository.save(any(Ausencia.class))).thenReturn(ausenciaMock);
            when(mapper.toResponse(ausenciaMock)).thenReturn(responseMock);

            // when
            AusenciaResponse result = service.crear(requestMock);

            // then
            assertThat(result).isNotNull();
            assertThat(result.fechaFin()).isEqualTo(LocalDate.of(2026, 3, 15));
            verify(repository).save(any(Ausencia.class));
        }

        @Test
        @DisplayName("actualizar() agregar fechaFin (null → fecha) actualiza correctamente")
        void actualizar_agregarFechaFin_actualizaCorrectamente() {
            // given
            Ausencia ausenciaIndefinida = Ausencia.builder()
                    .id(1L)
                    .persona(personaMock)
                    .fechaInicio(LocalDate.of(2026, 3, 1))
                    .fechaFin(null) // Actualmente null
                    .tipo(TipoAusencia.BAJA_MEDICA)
                    .comentario("Baja médica")
                    .build();

            AusenciaRequest updateRequest = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 15), // Ahora tiene fechaFin
                    TipoAusencia.BAJA_MEDICA,
                    "Baja médica con fecha fin"
            );

            when(repository.findById(1L)).thenReturn(Optional.of(ausenciaIndefinida));
            when(repository.save(any(Ausencia.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.actualizar(1L, updateRequest);

            // then
            verify(mapper).updateEntity(updateRequest, ausenciaIndefinida);
            verify(repository).save(argThat(a -> a.getFechaFin() != null));
        }

        @Test
        @DisplayName("actualizar() quitar fechaFin (fecha → null) vuelve indefinida")
        void actualizar_quitarFechaFin_vuelveIndefinida() {
            // given
            AusenciaRequest updateRequest = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    null, // Quitamos fechaFin
                    TipoAusencia.BAJA_MEDICA,
                    "Baja indefinida otra vez"
            );

            when(repository.findById(1L)).thenReturn(Optional.of(ausenciaMock));
            when(repository.save(any(Ausencia.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(
                    new AusenciaResponse(1L, 1L, "Juan Pérez", 
                            LocalDate.of(2026, 3, 1), null, 
                            TipoAusencia.BAJA_MEDICA, "Baja indefinida otra vez",
                            LocalDateTime.now(), LocalDateTime.now())
            );

            // when
            service.actualizar(1L, updateRequest);

            // then
            verify(mapper).updateEntity(updateRequest, ausenciaMock);
            verify(repository).save(any(Ausencia.class));
        }

        @Test
        @DisplayName("actualizar() cambiar fechaFin (fecha → otra fecha) actualiza correctamente")
        void actualizar_cambiarFechaFin_actualizaCorrectamente() {
            // given
            AusenciaRequest updateRequest = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 20), // Cambio de fecha fin
                    TipoAusencia.BAJA_MEDICA,
                    "Extensión de baja"
            );

            when(repository.findById(1L)).thenReturn(Optional.of(ausenciaMock));
            when(repository.save(any(Ausencia.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(
                    new AusenciaResponse(1L, 1L, "Juan Pérez", 
                            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 20), 
                            TipoAusencia.BAJA_MEDICA, "Extensión de baja",
                            LocalDateTime.now(), LocalDateTime.now())
            );

            // when
            service.actualizar(1L, updateRequest);

            // then
            verify(repository).save(any(Ausencia.class));
        }
    }

    // ══════════════════════════════════════════════════════════
    // VALIDACIÓN FECHAS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validación de fechas")
    class ValidacionFechasTests {

        @Test
        @DisplayName("crear() valida fechaFin >= fechaInicio")
        void crear_fechaFinAnterior_lanzaExcepcion() {
            // given
            AusenciaRequest invalidRequest = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 10),
                    LocalDate.of(2026, 3, 5), // fin anterior a inicio
                    TipoAusencia.BAJA_MEDICA,
                    "Fechas inválidas"
            );

            // when & then
            assertThatThrownBy(() -> service.crear(invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fecha de fin debe ser posterior o igual");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("actualizar() valida fechaFin >= fechaInicio")
        void actualizar_fechaFinAnterior_lanzaExcepcion() {
            // given
            AusenciaRequest invalidRequest = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 10),
                    LocalDate.of(2026, 3, 5), // fin anterior a inicio
                    TipoAusencia.BAJA_MEDICA,
                    "Fechas inválidas"
            );

            when(repository.findById(1L)).thenReturn(Optional.of(ausenciaMock));

            // when & then
            assertThatThrownBy(() -> service.actualizar(1L, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fecha de fin debe ser posterior o igual");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("crear() con fechaFin=null no lanza excepción (ausencia indefinida)")
        void crear_fechaFinNull_noLanzaExcepcion() {
            // given
            AusenciaRequest request = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    null, // No hay fechaFin, no debe validar
                    TipoAusencia.BAJA_MEDICA,
                    "Ausencia indefinida"
            );

            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(mapper.toEntity(request)).thenReturn(ausenciaMock);
            when(repository.save(any(Ausencia.class))).thenReturn(ausenciaMock);
            when(mapper.toResponse(ausenciaMock)).thenReturn(responseMock);

            // when & then
            assertThatCode(() -> service.crear(request)).doesNotThrowAnyException();
        }
    }

    // ══════════════════════════════════════════════════════════
    // CRUD BÁSICO
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CRUD Básico")
    class CrudBasicoTests {

        @Test
        @DisplayName("crear() con persona inexistente lanza excepción")
        void crear_personaInexistente_lanzaExcepcion() {
            // given
            when(personaRepository.findById(999L)).thenReturn(Optional.empty());

            AusenciaRequest request = new AusenciaRequest(
                    999L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 15),
                    TipoAusencia.BAJA_MEDICA,
                    "Persona inexistente"
            );

            // when & then
            assertThatThrownBy(() -> service.crear(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Persona no encontrada: 999");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("obtener(id) retorna ausencia existente")
        void obtener_ausenciaExistente_retornaResponse() {
            // given
            when(repository.findById(1L)).thenReturn(Optional.of(ausenciaMock));
            when(mapper.toResponse(ausenciaMock)).thenReturn(responseMock);

            // when
            AusenciaResponse result = service.obtener(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("obtener(id) ausencia inexistente lanza excepción")
        void obtener_ausenciaNoExiste_lanzaExcepcion() {
            // given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.obtener(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ausencia no encontrada: 999");
        }

        @Test
        @DisplayName("actualizar() ausencia inexistente lanza excepción")
        void actualizar_ausenciaNoExiste_lanzaExcepcion() {
            // given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.actualizar(999L, requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ausencia no encontrada: 999");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("actualizar() permite cambiar tipo y motivo")
        void actualizar_cambiaTipoYMotivo_actualizaCorrectamente() {
            // given
            AusenciaRequest updateRequest = new AusenciaRequest(
                    1L,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 15),
                    TipoAusencia.EMERGENCIA, // Cambio de tipo
                    "Permiso personal" // Cambio de motivo
            );

            when(repository.findById(1L)).thenReturn(Optional.of(ausenciaMock));
            when(repository.save(any(Ausencia.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(
                    new AusenciaResponse(1L, 1L, "Juan Pérez", 
                            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15), 
                            TipoAusencia.EMERGENCIA, "Permiso personal",
                            LocalDateTime.now(), LocalDateTime.now())
            );

            // when
            service.actualizar(1L, updateRequest);

            // then
            verify(mapper).updateEntity(updateRequest, ausenciaMock);
            verify(repository).save(any(Ausencia.class));
        }

        @Test
        @DisplayName("eliminar() ausencia existente elimina correctamente")
        void eliminar_ausenciaExistente_eliminaCorrectamente() {
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
        @DisplayName("eliminar() ausencia inexistente lanza excepción")
        void eliminar_ausenciaNoExiste_lanzaExcepcion() {
            // given
            when(repository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> service.eliminar(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ausencia no encontrada: 999");

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
        @DisplayName("listar() sin filtros retorna todas las ausencias")
        void listar_sinFiltros_retornaTodas() {
            // given
            List<Ausencia> ausencias = List.of(ausenciaMock);
            when(repository.findAll()).thenReturn(ausencias);
            when(mapper.toResponseList(ausencias)).thenReturn(List.of(responseMock));

            // when
            List<AusenciaResponse> result = service.listar(null, null, null, null);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findAll();
        }

        @Test
        @DisplayName("listar(personaId) filtra por persona")
        void listar_conPersonaId_filtraPorPersona() {
            // given
            List<Ausencia> ausencias = List.of(ausenciaMock);
            when(repository.findByPersonaId(1L)).thenReturn(ausencias);
            when(mapper.toResponseList(ausencias)).thenReturn(List.of(responseMock));

            // when
            List<AusenciaResponse> result = service.listar(1L, null, null, null);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findByPersonaId(1L);
            verify(repository, never()).findAll();
        }

        @Test
        @DisplayName("listar(squadId) filtra por squad")
        void listar_conSquadId_filtraPorSquad() {
            // given
            List<Ausencia> ausencias = List.of(ausenciaMock);
            when(repository.findBySquadIdAndFechaRange(5L, null, null)).thenReturn(ausencias);
            when(mapper.toResponseList(ausencias)).thenReturn(List.of(responseMock));

            // when
            List<AusenciaResponse> result = service.listar(null, 5L, null, null);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findBySquadIdAndFechaRange(5L, null, null);
        }
    }
}
