package com.kaos.planificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import com.kaos.planificacion.dto.PlantillaAsignacionRequest;
import com.kaos.planificacion.entity.PlantillaAsignacion;
import com.kaos.planificacion.entity.PlantillaAsignacionLinea;
import com.kaos.planificacion.entity.RolPlantilla;
import com.kaos.planificacion.repository.PlantillaAsignacionRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para {@link PlantillaAsignacionService}.
 * Cubre CRUD y lógica de aplicación de porcentajes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlantillaAsignacionService")
class PlantillaAsignacionServiceTest {

    @Mock private PlantillaAsignacionRepository repository;

    @InjectMocks
    private PlantillaAsignacionService service;

    private PlantillaAsignacion plantillaStory;

    @BeforeEach
    void setUp() {
        PlantillaAsignacionLinea lineaDev = new PlantillaAsignacionLinea();
        lineaDev.setId(11L);
        lineaDev.setRol(RolPlantilla.DESARROLLADOR);
        lineaDev.setPorcentajeHoras(70);
        lineaDev.setOrden(1);

        PlantillaAsignacionLinea lineaQa = new PlantillaAsignacionLinea();
        lineaQa.setId(12L);
        lineaQa.setRol(RolPlantilla.QA);
        lineaQa.setPorcentajeHoras(30);
        lineaQa.setOrden(2);

        plantillaStory = new PlantillaAsignacion();
        plantillaStory.setId(1L);
        plantillaStory.setNombre("Story estándar");
        plantillaStory.setTipoJira("Story");
        plantillaStory.setActivo(true);
        plantillaStory.setLineas(new ArrayList<>(List.of(lineaDev, lineaQa)));
    }

    // ══════════════════════════════════════════════════════════
    // listar
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("listar devuelve todas las plantillas mapeadas")
    void listar_devuelveTodas() {
        when(repository.findAll()).thenReturn(List.of(plantillaStory));

        var result = service.listar();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nombre()).isEqualTo("Story estándar");
        assertThat(result.get(0).tipoJira()).isEqualTo("Story");
        assertThat(result.get(0).lineas()).hasSize(2);
    }

    // ══════════════════════════════════════════════════════════
    // obtener
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("obtener")
    class ObtenerTests {

        @Test
        @DisplayName("id existente devuelve response correcto")
        void obtener_existente_devuelveResponse() {
            when(repository.findById(1L)).thenReturn(Optional.of(plantillaStory));

            var result = service.obtener(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.nombre()).isEqualTo("Story estándar");
        }

        @Test
        @DisplayName("id inexistente → EntityNotFoundException")
        void obtener_inexistente_lanzaEntityNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtener(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ══════════════════════════════════════════════════════════
    // crear
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("crear plantilla retorna response con datos del request")
    void crear_valido_retornaResponse() {
        var lineaReq = new PlantillaAsignacionRequest.LineaRequest("DESARROLLADOR", 100, 1, null);
        var request = new PlantillaAsignacionRequest("Bug fix", "Bug", true, List.of(lineaReq));

        PlantillaAsignacion guardada = new PlantillaAsignacion();
        guardada.setId(2L);
        guardada.setNombre("Bug fix");
        guardada.setTipoJira("Bug");
        guardada.setActivo(true);

        PlantillaAsignacionLinea linea = new PlantillaAsignacionLinea();
        linea.setId(20L);
        linea.setRol(RolPlantilla.DESARROLLADOR);
        linea.setPorcentajeHoras(100);
        linea.setOrden(1);
        guardada.setLineas(List.of(linea));

        when(repository.save(any())).thenReturn(guardada);

        var result = service.crear(request);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.nombre()).isEqualTo("Bug fix");
        assertThat(result.tipoJira()).isEqualTo("Bug");
        assertThat(result.lineas()).hasSize(1);
    }

    // ══════════════════════════════════════════════════════════
    // actualizar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("actualizar")
    class ActualizarTests {

        @Test
        @DisplayName("actualizar existente modifica nombre y tipoJira")
        void actualizar_existente_modificaDatos() {
            when(repository.findById(1L)).thenReturn(Optional.of(plantillaStory));

            PlantillaAsignacion actualizada = new PlantillaAsignacion();
            actualizada.setId(1L);
            actualizada.setNombre("Story senior");
            actualizada.setTipoJira("Story");
            actualizada.setActivo(false);
            actualizada.setLineas(List.of());
            when(repository.save(any())).thenReturn(actualizada);

            var lineaReq = new PlantillaAsignacionRequest.LineaRequest("TECH_LEAD", 100, 1, null);
            var request = new PlantillaAsignacionRequest("Story senior", "Story", false, List.of(lineaReq));

            var result = service.actualizar(1L, request);

            assertThat(result.nombre()).isEqualTo("Story senior");
            assertThat(result.activo()).isFalse();
        }

        @Test
        @DisplayName("actualizar inexistente → EntityNotFoundException")
        void actualizar_inexistente_lanzaEntityNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            var lineaReq = new PlantillaAsignacionRequest.LineaRequest("QA", 100, 1, null);
            var request = new PlantillaAsignacionRequest("X", "Bug", true, List.of(lineaReq));

            assertThatThrownBy(() -> service.actualizar(99L, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════
    // eliminar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("eliminar")
    class EliminarTests {

        @Test
        @DisplayName("eliminar existente invoca deleteById")
        void eliminar_existente_invocaDelete() {
            when(repository.existsById(1L)).thenReturn(true);

            service.eliminar(1L);

            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("eliminar inexistente → EntityNotFoundException")
        void eliminar_inexistente_lanzaEntityNotFound() {
            when(repository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> service.eliminar(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ══════════════════════════════════════════════════════════
    // aplicar
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("aplicar")
    class AplicarTests {

        @Test
        @DisplayName("plantilla activa existe: distribuye horas por porcentaje")
        void aplicar_plantillaActiva_distribuyeHoras() {
            when(repository.findFirstByTipoJiraIgnoreCaseAndActivoTrue("Story"))
                    .thenReturn(Optional.of(plantillaStory));

            // 10 horas: 70% → 7h, 30% → 3h
            var result = service.aplicar("Story", BigDecimal.TEN);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).estimacion())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(new BigDecimal("7.00"));
            assertThat(result.get(1).estimacion())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(new BigDecimal("3.00"));
        }

        @Test
        @DisplayName("sin plantilla activa devuelve lista vacía")
        void aplicar_sinPlantilla_devuelveVacia() {
            when(repository.findFirstByTipoJiraIgnoreCaseAndActivoTrue("Spike"))
                    .thenReturn(Optional.empty());

            var result = service.aplicar("Spike", BigDecimal.valueOf(16));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("búsqueda de tipo es case-insensitive (STORY → Story)")
        void aplicar_tipoCaseInsensitive_encuentraPlantilla() {
            when(repository.findFirstByTipoJiraIgnoreCaseAndActivoTrue("STORY"))
                    .thenReturn(Optional.of(plantillaStory));

            var result = service.aplicar("STORY", BigDecimal.ONE);

            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("estimación 8h con 70/30 produce horas correctas con redondeo")
        void aplicar_estimacion8h_proProduceRedondeo() {
            when(repository.findFirstByTipoJiraIgnoreCaseAndActivoTrue("Story"))
                    .thenReturn(Optional.of(plantillaStory));

            // 8h: 70% → 5.60, 30% → 2.40
            var result = service.aplicar("Story", BigDecimal.valueOf(8));

            assertThat(result.get(0).estimacion())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(new BigDecimal("5.60"));
            assertThat(result.get(1).estimacion())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(new BigDecimal("2.40"));
        }
    }
}
