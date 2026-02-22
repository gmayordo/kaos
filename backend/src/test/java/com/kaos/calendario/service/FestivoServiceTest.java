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
import com.kaos.calendario.entity.Festivo;
import com.kaos.calendario.entity.TipoFestivo;
import com.kaos.calendario.mapper.FestivoMapper;
import com.kaos.calendario.repository.FestivoRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para {@link FestivoService}.
 * Valida CRUD básico, carga masiva CSV y reglas de negocio (CA-06, CA-07).
 * Festivos están vinculados a ciudad (no a personas).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FestivoService Tests")
class FestivoServiceTest {

    @Mock
    private FestivoRepository repository;

    @Mock
    private FestivoMapper mapper;

    @InjectMocks
    private FestivoService service;

    private Festivo festivoMock;
    private FestivoRequest requestMock;
    private FestivoResponse responseMock;

    @BeforeEach
    void setUp() {
        // Festivo entity mock (vinculado a ciudad)
        festivoMock = Festivo.builder()
                .id(1L)
                .fecha(LocalDate.of(2026, 1, 1))
                .descripcion("Año Nuevo")
                .tipo(TipoFestivo.NACIONAL)
                .ciudad("Zaragoza")
                .build();

        // Request mock
        requestMock = new FestivoRequest(
                LocalDate.of(2026, 1, 1),
                "Año Nuevo",
                TipoFestivo.NACIONAL,
                "Zaragoza"
        );

        // Response mock
        responseMock = new FestivoResponse(
                1L,
                LocalDate.of(2026, 1, 1),
                "Año Nuevo",
                TipoFestivo.NACIONAL,
                "Zaragoza",
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
        @DisplayName("crear() festivo válido retorna response")
        void crear_festivoValido_retornaResponse() {
            // given
            when(repository.existsByFechaAndDescripcionAndCiudad(any(), any(), any())).thenReturn(false);
            when(mapper.toEntity(requestMock)).thenReturn(festivoMock);
            when(repository.save(any(Festivo.class))).thenReturn(festivoMock);
            when(mapper.toResponse(festivoMock)).thenReturn(responseMock);

            // when
            FestivoResponse result = service.crear(requestMock);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            verify(repository).existsByFechaAndDescripcionAndCiudad(
                    requestMock.fecha(), requestMock.descripcion(), requestMock.ciudad());
            verify(repository).save(any(Festivo.class));
        }

        @Test
        @DisplayName("crear() festivo duplicado (fecha+descripcion+ciudad) lanza IllegalArgumentException")
        void crear_festivoDuplicado_lanzaExcepcion() {
            // given
            when(repository.existsByFechaAndDescripcionAndCiudad(
                    requestMock.fecha(), 
                    requestMock.descripcion(),
                    requestMock.ciudad()
            )).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.crear(requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un festivo")
                    .hasMessageContaining(requestMock.fecha().toString())
                    .hasMessageContaining(requestMock.descripcion());

            verify(repository).existsByFechaAndDescripcionAndCiudad(
                    requestMock.fecha(), requestMock.descripcion(), requestMock.ciudad());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("crear() festivo en otra ciudad NO es duplicado")
        void crear_festivoEnOtraCiudad_creaCorrectamente() {
            // given
            FestivoRequest requestValencia = new FestivoRequest(
                    LocalDate.of(2026, 1, 1),
                    "Año Nuevo",
                    TipoFestivo.NACIONAL,
                    "Valencia"
            );

            when(repository.existsByFechaAndDescripcionAndCiudad(any(), any(), any())).thenReturn(false);
            when(mapper.toEntity(requestValencia)).thenReturn(festivoMock);
            when(repository.save(any(Festivo.class))).thenReturn(festivoMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // when
            service.crear(requestValencia);

            // then
            verify(repository).existsByFechaAndDescripcionAndCiudad(
                    LocalDate.of(2026, 1, 1), "Año Nuevo", "Valencia");
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
                    "Zaragoza"
            );

            when(repository.findById(1L)).thenReturn(Optional.of(festivoMock));
            when(repository.existsByFechaAndDescripcionAndCiudad(any(), any(), any())).thenReturn(false);
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
        @DisplayName("actualizar() valida duplicado si cambia fecha, descripción o ciudad")
        void actualizar_cambiaFechaODescripcion_validaDuplicado() {
            // given
            FestivoRequest updateRequest = new FestivoRequest(
                    LocalDate.of(2026, 1, 6),
                    "Reyes",
                    TipoFestivo.NACIONAL,
                    "Zaragoza"
            );

            when(repository.findById(1L)).thenReturn(Optional.of(festivoMock));
            when(repository.existsByFechaAndDescripcionAndCiudad(
                    updateRequest.fecha(),
                    updateRequest.descripcion(),
                    updateRequest.ciudad()
            )).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.actualizar(1L, updateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un festivo");

            verify(repository).existsByFechaAndDescripcionAndCiudad(
                    updateRequest.fecha(), updateRequest.descripcion(), updateRequest.ciudad());
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
    // LISTAR POR CIUDAD
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listarPorCiudad() - Tests de listado por ciudad")
    class ListarPorCiudadTests {

        @Test
        @DisplayName("listarPorCiudad() retorna festivos de la ciudad")
        void listarPorCiudad_ciudadExistente_retornaFestivos() {
            // given
            LocalDate inicio = LocalDate.of(2026, 1, 1);
            LocalDate fin = LocalDate.of(2026, 12, 31);
            List<Festivo> festivos = List.of(festivoMock);

            when(repository.findByCiudadAndFechaRange("Zaragoza", inicio, fin)).thenReturn(festivos);
            when(mapper.toResponseList(festivos)).thenReturn(List.of(responseMock));

            // when
            List<FestivoResponse> result = service.listarPorCiudad("Zaragoza", inicio, fin);

            // then
            assertThat(result).hasSize(1);
            verify(repository).findByCiudadAndFechaRange("Zaragoza", inicio, fin);
        }

        @Test
        @DisplayName("listarPorCiudad() sin festivos retorna lista vacía")
        void listarPorCiudad_sinFestivos_retornaVacia() {
            // given
            when(repository.findByCiudadAndFechaRange("Temuco", null, null)).thenReturn(List.of());
            when(mapper.toResponseList(List.of())).thenReturn(List.of());

            // when
            List<FestivoResponse> result = service.listarPorCiudad("Temuco", null, null);

            // then
            assertThat(result).isEmpty();
            verify(repository).findByCiudadAndFechaRange("Temuco", null, null);
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
                    2026-01-01;Año Nuevo;NACIONAL;Zaragoza
                    2026-04-18;Viernes Santo;NACIONAL;Valencia
                    2026-08-15;Asunción;REGIONAL;Temuco
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            when(repository.existsByFechaAndDescripcionAndCiudad(any(), any(), any())).thenReturn(false);
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
        @DisplayName("cargarCsv() CSV con duplicado se salta sin generar error")
        void cargarCsv_festivoDuplicado_saltaSinError() {
            // given
            String csvContent = """
                    2026-01-01;Año Nuevo;NACIONAL;Zaragoza
                    2026-01-01;Año Nuevo;NACIONAL;Zaragoza
                    2026-04-18;Viernes Santo;NACIONAL;Valencia
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            // Primera línea: no duplicado, segunda: duplicado, tercera: no duplicado
            when(repository.existsByFechaAndDescripcionAndCiudad(
                    LocalDate.of(2026, 1, 1), "Año Nuevo", "Zaragoza"))
                    .thenReturn(false, true); // Primera vez false, segunda true
            when(repository.existsByFechaAndDescripcionAndCiudad(
                    LocalDate.of(2026, 4, 18), "Viernes Santo", "Valencia"))
                    .thenReturn(false);

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
                    2026-01-01;Año Nuevo;NACIONAL;Zaragoza
                    2026-12-25-NAVIDAD-NACIONAL
                    2026-04-18;Viernes Santo;NACIONAL;Valencia
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            when(repository.existsByFechaAndDescripcionAndCiudad(any(), any(), any())).thenReturn(false);
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
                    2026-01-01;Año Nuevo;NACIONAL;Zaragoza
                    2026-01-01;Año Nuevo;NACIONAL;Zaragoza
                    2026-12-25-NAVIDAD
                    2026-04-18;Viernes Santo;NACIONAL;Valencia
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            when(repository.existsByFechaAndDescripcionAndCiudad(
                    LocalDate.of(2026, 1, 1), "Año Nuevo", "Zaragoza"))
                    .thenReturn(false, true); // Primera false, segunda true (duplicado)
            when(repository.existsByFechaAndDescripcionAndCiudad(
                    LocalDate.of(2026, 4, 18), "Viernes Santo", "Valencia"))
                    .thenReturn(false);

            when(repository.save(any(Festivo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            FestivoCsvUploadResponse result = service.cargarCsv(file);

            // then
            assertThat(result.totalProcesados()).isEqualTo(4);
            assertThat(result.exitosos()).isEqualTo(3); // Línea 1, 2 (duplicado, no error), 4
            assertThat(result.errores()).isEqualTo(1); // Línea 3 (formato)
            assertThat(result.detalleErrores()).hasSize(1);
            verify(repository, times(2)).save(any(Festivo.class)); // Solo guarda 2 (línea 1 y 4)
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
        @DisplayName("cargarCsv() CSV con ciudad vacía genera error")
        void cargarCsv_ciudadVacia_generaError() {
            // given
            String csvContent = "2026-01-01;Año Nuevo;NACIONAL;   \n";

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "festivos.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            // when
            FestivoCsvUploadResponse result = service.cargarCsv(file);

            // then
            assertThat(result.totalProcesados()).isEqualTo(1);
            assertThat(result.exitosos()).isZero();
            assertThat(result.errores()).isEqualTo(1);
            assertThat(result.detalleErrores().get(0).mensaje()).contains("Ciudad no puede estar vacía");
        }
    }
}
