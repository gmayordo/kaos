package com.kaos.calendario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import com.kaos.calendario.dto.FestivoCsvUploadResponse;
import com.kaos.calendario.dto.FestivoRequest;
import com.kaos.calendario.dto.FestivoResponse;
import com.kaos.calendario.dto.PersonaBasicInfo;
import com.kaos.calendario.entity.Festivo;
import com.kaos.calendario.entity.TipoFestivo;
import com.kaos.calendario.mapper.FestivoMapper;
import com.kaos.calendario.repository.FestivoRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para {@link FestivoService}.
 * Valida CRUD básico, carga masiva CSV y reglas de negocio (CA-06, CA-07).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FestivoService Tests")
class FestivoServiceTest {

    @Mock
    private FestivoRepository repository;

    @Mock
    private FestivoMapper mapper;

    @Mock
    private PersonaRepository personaRepository;

    @InjectMocks
    private FestivoService service;

    private Festivo festivoMock;
    private FestivoRequest requestMock;
    private FestivoResponse responseMock;
    private Persona personaMock;

    @BeforeEach
    void setUp() {
        // Persona mock
        personaMock = new Persona();
        personaMock.setId(1L);
        personaMock.setNombre("Juan Pérez");
        personaMock.setEmail("juan.perez@kaos.com");

        // Festivo entity mock
        festivoMock = Festivo.builder()
                .id(1L)
                .fecha(LocalDate.of(2026, 1, 1))
                .descripcion("Año Nuevo")
                .tipo(TipoFestivo.NACIONAL)
                .personas(Set.of(personaMock))
                .build();

        // Request mock
        requestMock = new FestivoRequest(
                LocalDate.of(2026, 1, 1),
                "Año Nuevo",
                TipoFestivo.NACIONAL,
                List.of(1L)
        );

        // Response mock
        responseMock = new FestivoResponse(
                1L,
                LocalDate.of(2026, 1, 1),
                "Año Nuevo",
                TipoFestivo.NACIONAL,
                List.of(new PersonaBasicInfo(1L, "Juan Pérez", "juan.perez@kaos.com")),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ══════════════════════════════════════════════════════════
    // LISTAR - Sin filtros y con filtros
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listar() - Tests de listado con filtros")
    class ListarTests {

        @Test
        @DisplayName("listar() sin filtros retorna todos los festivos")
        void listar_sinFiltros_retornaTodos() {
            // given
            List<Festivo> festivos = List.of(festivoMock);
            when(repository.findAll()).thenReturn(festivos);
            when(mapper.toResponseList(festivos)).thenReturn(List.of(responseMock));

            // when
            List<FestivoResponse> result = service.listar(null, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).descripcion()).isEqualTo("Año Nuevo");
            verify(repository).findAll();
            verify(mapper).toResponseList(festivos);
        }

        @Test
        @DisplayName("listar(anio) filtra por año correctamente")
        void listar_conAnio_filtraPorAnio() {
            // given
            List<Festivo> festivos = List.of(festivoMock);
            when(repository.findByAnio(2026)).thenReturn(festivos);
            when(mapper.toResponseList(festivos)).thenReturn(List.of(responseMock));

            // when
            List<FestivoResponse> result = service.listar(2026, null);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findByAnio(2026);
            verify(repository, never()).findAll();
        }

        @Test
        @DisplayName("listar(tipo) filtra por tipo correctamente")
        void listar_conTipo_filtraPorTipo() {
            // given
            List<Festivo> festivos = List.of(festivoMock);
            when(repository.findByTipo(TipoFestivo.NACIONAL)).thenReturn(festivos);
            when(mapper.toResponseList(festivos)).thenReturn(List.of(responseMock));

            // when
            List<FestivoResponse> result = service.listar(null, TipoFestivo.NACIONAL);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findByTipo(TipoFestivo.NACIONAL);
        }

        @Test
        @DisplayName("listar(anio, tipo) filtra por ambos criterios")
        void listar_conAnioYTipo_filtraPorAmbos() {
            // given
            List<Festivo> festivos = List.of(festivoMock);
            when(repository.findByAnioAndTipo(2026, TipoFestivo.NACIONAL)).thenReturn(festivos);
            when(mapper.toResponseList(festivos)).thenReturn(List.of(responseMock));

            // when
            List<FestivoResponse> result = service.listar(2026, TipoFestivo.NACIONAL);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findByAnioAndTipo(2026, TipoFestivo.NACIONAL);
        }
    }

    // ══════════════════════════════════════════════════════════
    // OBTENER - Por ID
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("obtener() - Tests de obtención por ID")
    class ObtenerTests {

        @Test
        @DisplayName("obtener(id) retorna festivo existente")
        void obtener_festivoExistente_retornaResponse() {
            // given
            when(repository.findById(1L)).thenReturn(Optional.of(festivoMock));
            when(mapper.toResponse(festivoMock)).thenReturn(responseMock);

            // when
            FestivoResponse result = service.obtener(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.descripcion()).isEqualTo("Año Nuevo");
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("obtener(id) lanza excepción si no existe")
        void obtener_festivoNoExiste_lanzaExcepcion() {
            // given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.obtener(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Festivo no encontrado con id: 999");
            verify(repository).findById(999L);
        }
    }

    // ══════════════════════════════════════════════════════════
    // CREAR - Validaciones CA-06
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("crear() - Tests de creación con validaciones (CA-06)")
    class CrearTests {

        @Test
        @DisplayName("crear() festivo válido con personas retorna response")
        void crear_festivoValido_retornaResponse() {
            // given
            when(repository.existsByFechaAndDescripcion(any(), any())).thenReturn(false);
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(mapper.toEntity(requestMock)).thenReturn(festivoMock);
            when(repository.save(any(Festivo.class))).thenReturn(festivoMock);
            when(mapper.toResponse(festivoMock)).thenReturn(responseMock);

            // when
            FestivoResponse result = service.crear(requestMock);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            verify(repository).existsByFechaAndDescripcion(requestMock.fecha(), requestMock.descripcion());
            verify(personaRepository).findById(1L);
            verify(repository).save(any(Festivo.class));
        }

        @Test
        @DisplayName("crear() festivo duplicado (fecha+descripcion) lanza IllegalArgumentException")
        void crear_festivoDuplicado_lanzaExcepcion() {
            // given
            when(repository.existsByFechaAndDescripcion(
                    requestMock.fecha(), 
                    requestMock.descripcion()
            )).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.crear(requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un festivo")
                    .hasMessageContaining(requestMock.fecha().toString())
                    .hasMessageContaining(requestMock.descripcion());

            verify(repository).existsByFechaAndDescripcion(requestMock.fecha(), requestMock.descripcion());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("crear() con persona inexistente lanza IllegalArgumentException")
        void crear_personaInexistente_lanzaExcepcion() {
            // given
            when(repository.existsByFechaAndDescripcion(any(), any())).thenReturn(false);
            when(personaRepository.findById(999L)).thenReturn(Optional.empty());

            FestivoRequest request = new FestivoRequest(
                    LocalDate.of(2026, 1, 6),
                    "Reyes",
                    TipoFestivo.NACIONAL,
                    List.of(999L)
            );

            // when & then
            assertThatThrownBy(() -> service.crear(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Persona no encontrada con id: 999");

            verify(personaRepository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("crear() asigna múltiples personas correctamente")
        void crear_multiplePersonas_asignaTodas() {
            // given
            Persona p1 = new Persona();
            p1.setId(1L);
            Persona p2 = new Persona();
            p2.setId(2L);

            FestivoRequest request = new FestivoRequest(
                    LocalDate.of(2026, 1, 1),
                    "Año Nuevo",
                    TipoFestivo.NACIONAL,
                    List.of(1L, 2L)
            );

            when(repository.existsByFechaAndDescripcion(any(), any())).thenReturn(false);
            when(personaRepository.findById(1L)).thenReturn(Optional.of(p1));
            when(personaRepository.findById(2L)).thenReturn(Optional.of(p2));
            when(mapper.toEntity(request)).thenReturn(festivoMock);
            when(repository.save(any(Festivo.class))).thenReturn(festivoMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.crear(request);

            // then
            verify(personaRepository).findById(1L);
            verify(personaRepository).findById(2L);
            verify(repository).save(any(Festivo.class));
        }
    }

    // ══════════════════════════════════════════════════════════
    // ACTUALIZAR
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("actualizar() - Tests de actualización")
    class ActualizarTests {

        @Test
        @DisplayName("actualizar() festivo existente cambia datos")
        void actualizar_festivoExistente_actualizaDatos() {
            // given
            FestivoRequest updateRequest = new FestivoRequest(
                    LocalDate.of(2026, 1, 1),
                    "Año Nuevo 2026", // Descripción cambiada
                    TipoFestivo.NACIONAL,
                    List.of(1L)
            );

            when(repository.findById(1L)).thenReturn(Optional.of(festivoMock));
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsByFechaAndDescripcion(any(), any())).thenReturn(false);
            when(repository.save(any(Festivo.class))).thenReturn(festivoMock);
            when(mapper.toResponse(festivoMock)).thenReturn(responseMock);

            // when
            FestivoResponse result = service.actualizar(1L, updateRequest);

            // then
            assertThat(result).isNotNull();
            verify(repository).findById(1L);
            verify(mapper).updateEntity(updateRequest, festivoMock);
            verify(repository).save(festivoMock);
        }

        @Test
        @DisplayName("actualizar() festivo inexistente lanza excepción")
        void actualizar_festivoNoExiste_lanzaExcepcion() {
            // given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.actualizar(999L, requestMock))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Festivo no encontrado con id: 999");

            verify(repository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("actualizar() valida duplicado si cambia fecha o descripción")
        void actualizar_cambiaFechaODescripcion_validaDuplicado() {
            // given
            FestivoRequest updateRequest = new FestivoRequest(
                    LocalDate.of(2026, 1, 6),
                    "Reyes",
                    TipoFestivo.NACIONAL,
                    List.of(1L)
            );

            when(repository.findById(1L)).thenReturn(Optional.of(festivoMock));
            when(repository.existsByFechaAndDescripcion(
                    updateRequest.fecha(), 
                    updateRequest.descripcion()
            )).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.actualizar(1L, updateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un festivo");

            verify(repository).existsByFechaAndDescripcion(updateRequest.fecha(), updateRequest.descripcion());
        }
    }

    // ══════════════════════════════════════════════════════════
    // ELIMINAR
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("eliminar() - Tests de eliminación")
    class EliminarTests {

        @Test
        @DisplayName("eliminar() festivo existente borra correctamente")
        void eliminar_festivoExistente_eliminaCorrectamente() {
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
        @DisplayName("eliminar() festivo inexistente lanza excepción")
        void eliminar_festivoNoExiste_lanzaExcepcion() {
            // given
            when(repository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> service.eliminar(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Festivo no encontrado con id: 999");

            verify(repository).existsById(999L);
            verify(repository, never()).deleteById(any());
        }
    }

    // ══════════════════════════════════════════════════════════
    // LISTAR POR PERSONA
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listarPorPersona() - Tests de listado por persona")
    class ListarPorPersonaTests {

        @Test
        @DisplayName("listarPorPersona() retorna festivos de la persona")
        void listarPorPersona_personaExistente_retornaFestivos() {
            // given
            LocalDate inicio = LocalDate.of(2026, 1, 1);
            LocalDate fin = LocalDate.of(2026, 12, 31);
            List<Festivo> festivos = List.of(festivoMock);

            when(personaRepository.existsById(1L)).thenReturn(true);
            when(repository.findByPersonaIdAndFechaRange(1L, inicio, fin)).thenReturn(festivos);
            when(mapper.toResponseList(festivos)).thenReturn(List.of(responseMock));

            // when
            List<FestivoResponse> result = service.listarPorPersona(1L, inicio, fin);

            // then
            assertThat(result).hasSize(1);
            verify(personaRepository).existsById(1L);
            verify(repository).findByPersonaIdAndFechaRange(1L, inicio, fin);
        }

        @Test
        @DisplayName("listarPorPersona() persona inexistente lanza excepción")
        void listarPorPersona_personaNoExiste_lanzaExcepcion() {
            // given
            when(personaRepository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> service.listarPorPersona(999L, null, null))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Persona no encontrada con id: 999");

            verify(personaRepository).existsById(999L);
        }
    }

    // ══════════════════════════════════════════════════════════
    // CARGA MASIVA CSV - CA-07
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cargarCsv() - Tests de carga masiva CSV (CA-07)")
    class CargarCsvTests {

        @Test
        @DisplayName("cargarCsv() CSV perfectamente válido procesa todo")
        void cargarCsv_csvValido_procesaTodo() {
            // given
            String csvContent = """
                    2026-01-01;Año Nuevo;NACIONAL;juan.perez@kaos.com
                    2026-04-18;Viernes Santo;NACIONAL;maria.garcia@kaos.com
                    2026-08-15;Asunción;REGIONAL;pedro.lopez@kaos.com
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            Persona p1 = crearPersona(1L, "juan.perez@kaos.com");
            Persona p2 = crearPersona(2L, "maria.garcia@kaos.com");
            Persona p3 = crearPersona(3L, "pedro.lopez@kaos.com");

            when(repository.existsByFechaAndDescripcion(any(), any())).thenReturn(false);
            when(personaRepository.findByEmail("juan.perez@kaos.com")).thenReturn(Optional.of(p1));
            when(personaRepository.findByEmail("maria.garcia@kaos.com")).thenReturn(Optional.of(p2));
            when(personaRepository.findByEmail("pedro.lopez@kaos.com")).thenReturn(Optional.of(p3));
            when(repository.save(any(Festivo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            FestivoCsvUploadResponse result = service.cargarCsv(file);

            // then
            assertThat(result.totalProcesados()).isEqualTo(3);
            assertThat(result.exitosos()).isEqualTo(3);
            assertThat(result.errores()).isZero();
            assertThat(result.detalleErrores()).isEmpty();
            verify(repository, times(3)).save(any(Festivo.class));
        }

        @Test
        @DisplayName("cargarCsv() CSV con email inexistente genera error pero continúa")
        void cargarCsv_emailInexistente_generaErrorPeroSigueProcesando() {
            // given
            String csvContent = """
                    2026-01-01;Año Nuevo;NACIONAL;juan.perez@kaos.com
                    2026-04-18;Viernes Santo;NACIONAL;noexiste@kaos.com
                    2026-08-15;Asunción;REGIONAL;pedro.lopez@kaos.com
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            Persona p1 = crearPersona(1L, "juan.perez@kaos.com");
            Persona p3 = crearPersona(3L, "pedro.lopez@kaos.com");

            when(repository.existsByFechaAndDescripcion(any(), any())).thenReturn(false);
            when(personaRepository.findByEmail("juan.perez@kaos.com")).thenReturn(Optional.of(p1));
            when(personaRepository.findByEmail("noexiste@kaos.com")).thenReturn(Optional.empty());
            when(personaRepository.findByEmail("pedro.lopez@kaos.com")).thenReturn(Optional.of(p3));
            when(repository.save(any(Festivo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            FestivoCsvUploadResponse result = service.cargarCsv(file);

            // then
            assertThat(result.totalProcesados()).isEqualTo(3);
            assertThat(result.exitosos()).isEqualTo(2); // Solo 2 exitosos (fila 2 falló)
            assertThat(result.errores()).isEqualTo(1);
            assertThat(result.detalleErrores()).hasSize(1);
            assertThat(result.detalleErrores().get(0).mensaje())
                    .contains("Persona no encontrada con email: noexiste@kaos.com");
            verify(repository, times(2)).save(any(Festivo.class)); // Solo guarda 2
        }

        @Test
        @DisplayName("cargarCsv() CSV con duplicado se salta sin generar error")
        void cargarCsv_festivoDuplicado_saltaSinError() {
            // given
            String csvContent = """
                    2026-01-01;Año Nuevo;NACIONAL;juan.perez@kaos.com
                    2026-01-01;Año Nuevo;NACIONAL;maria.garcia@kaos.com
                    2026-04-18;Viernes Santo;NACIONAL;pedro.lopez@kaos.com
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            Persona p1 = crearPersona(1L, "juan.perez@kaos.com");
            Persona p3 = crearPersona(3L, "pedro.lopez@kaos.com");

            // Primera línea: no duplicado, segunda: duplicado, tercera: no duplicado
            when(repository.existsByFechaAndDescripcion(LocalDate.of(2026, 1, 1), "Año Nuevo"))
                    .thenReturn(false, true); // Primera vez false, segunda true
            when(repository.existsByFechaAndDescripcion(LocalDate.of(2026, 4, 18), "Viernes Santo"))
                    .thenReturn(false);

            when(personaRepository.findByEmail("juan.perez@kaos.com")).thenReturn(Optional.of(p1));
            when(personaRepository.findByEmail("pedro.lopez@kaos.com")).thenReturn(Optional.of(p3));
            when(repository.save(any(Festivo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            FestivoCsvUploadResponse result = service.cargarCsv(file);

            // then
            assertThat(result.totalProcesados()).isEqualTo(3);
            assertThat(result.exitosos()).isEqualTo(3); // Todas se procesaron (duplicado se ignora)
            assertThat(result.errores()).isZero(); // Duplicados NO son errores
            verify(repository, times(2)).save(any(Festivo.class)); // Solo guarda 2 (salta duplicado)
        }

        @Test
        @DisplayName("cargarCsv() CSV con formato incorrecto genera error")
        void cargarCsv_formatoIncorrecto_generaError() {
            // given
            String csvContent = """
                    2026-01-01;Año Nuevo;NACIONAL;juan.perez@kaos.com
                    2026-12-25-NAVIDAD-NACIONAL
                    2026-04-18;Viernes Santo;NACIONAL;pedro.lopez@kaos.com
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            Persona p1 = crearPersona(1L, "juan.perez@kaos.com");
            Persona p3 = crearPersona(3L, "pedro.lopez@kaos.com");

            when(repository.existsByFechaAndDescripcion(any(), any())).thenReturn(false);
            when(personaRepository.findByEmail("juan.perez@kaos.com")).thenReturn(Optional.of(p1));
            when(personaRepository.findByEmail("pedro.lopez@kaos.com")).thenReturn(Optional.of(p3));
            when(repository.save(any(Festivo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            FestivoCsvUploadResponse result = service.cargarCsv(file);

            // then
            assertThat(result.totalProcesados()).isEqualTo(3);
            assertThat(result.exitosos()).isEqualTo(2);
            assertThat(result.errores()).isEqualTo(1);
            assertThat(result.detalleErrores()).hasSize(1);
            assertThat(result.detalleErrores().get(0).fila()).isEqualTo(2);
            assertThat(result.detalleErrores().get(0).mensaje()).contains("Formato inválido");
        }

        @Test
        @DisplayName("cargarCsv() CSV mix de errores procesa correctamente")
        void cargarCsv_mixErrores_procesaParcialmente() {
            // given
            String csvContent = """
                    2026-01-01;Año Nuevo;NACIONAL;juan.perez@kaos.com
                    2026-01-06;Reyes;NACIONAL;noexiste@kaos.com
                    2026-01-01;Año Nuevo;NACIONAL;otro@kaos.com
                    2026-12-25-NAVIDAD
                    2026-04-18;Viernes Santo;NACIONAL;pedro.lopez@kaos.com
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            Persona p1 = crearPersona(1L, "juan.perez@kaos.com");
            Persona p3 = crearPersona(3L, "pedro.lopez@kaos.com");

            when(repository.existsByFechaAndDescripcion(LocalDate.of(2026, 1, 1), "Año Nuevo"))
                    .thenReturn(false, true); // Primera false, otra true (duplicado)
            when(repository.existsByFechaAndDescripcion(LocalDate.of(2026, 4, 18), "Viernes Santo"))
                    .thenReturn(false);

            when(personaRepository.findByEmail("juan.perez@kaos.com")).thenReturn(Optional.of(p1));
            when(personaRepository.findByEmail("noexiste@kaos.com")).thenReturn(Optional.empty());
            when(personaRepository.findByEmail("pedro.lopez@kaos.com")).thenReturn(Optional.of(p3));
            when(repository.save(any(Festivo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            FestivoCsvUploadResponse result = service.cargarCsv(file);

            // then
            assertThat(result.totalProcesados()).isEqualTo(5);
            assertThat(result.exitosos()).isEqualTo(3); // Línea 1, 3 (duplicado, no error), 5
            assertThat(result.errores()).isEqualTo(2); // Línea 2 (email) y 4 (formato)
            assertThat(result.detalleErrores()).hasSize(2);
            verify(repository, times(2)).save(any(Festivo.class)); // Solo guarda 2 (línea 1 y 5)
        }

        @Test
        @DisplayName("cargarCsv() CSV vacío procesa sin errores")
        void cargarCsv_csvVacio_procesaSinErrores() {
            // given
            String csvContent = "";

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            // when
            FestivoCsvUploadResponse result = service.cargarCsv(file);

            // then
            assertThat(result.totalProcesados()).isZero();
            assertThat(result.exitosos()).isZero();
            assertThat(result.errores()).isZero();
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("cargarCsv() CSV con múltiples emails asigna todas las personas")
        void cargarCsv_multipleEmails_asignaTodasLasPersonas() {
            // given
            String csvContent = "2026-01-01;Año Nuevo;NACIONAL;juan@kaos.com|maria@kaos.com|pedro@kaos.com\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            Persona p1 = crearPersona(1L, "juan@kaos.com");
            Persona p2 = crearPersona(2L, "maria@kaos.com");
            Persona p3 = crearPersona(3L, "pedro@kaos.com");

            when(repository.existsByFechaAndDescripcion(any(), any())).thenReturn(false);
            when(personaRepository.findByEmail("juan@kaos.com")).thenReturn(Optional.of(p1));
            when(personaRepository.findByEmail("maria@kaos.com")).thenReturn(Optional.of(p2));
            when(personaRepository.findByEmail("pedro@kaos.com")).thenReturn(Optional.of(p3));
            when(repository.save(any(Festivo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            FestivoCsvUploadResponse result = service.cargarCsv(file);

            // then
            assertThat(result.exitosos()).isEqualTo(1);
            verify(personaRepository).findByEmail("juan@kaos.com");
            verify(personaRepository).findByEmail("maria@kaos.com");
            verify(personaRepository).findByEmail("pedro@kaos.com");
            verify(repository).save(any(Festivo.class));
        }
    }

    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════

    private Persona crearPersona(Long id, String email) {
        Persona persona = new Persona();
        persona.setId(id);
        persona.setEmail(email);
        persona.setNombre("Persona " + id);
        return persona;
    }
}
