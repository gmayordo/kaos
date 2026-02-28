package com.kaos.planificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.kaos.planificacion.entity.Tarea;
import com.kaos.planificacion.entity.TareaDependencia;
import com.kaos.planificacion.entity.TipoDependencia;
import com.kaos.planificacion.exception.DependenciaCiclicaException;
import com.kaos.planificacion.exception.DependenciaDuplicadaException;
import com.kaos.planificacion.repository.TareaDependenciaRepository;
import com.kaos.planificacion.repository.TareaRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para {@link TareaDependenciaService}.
 * Cubre gestión de dependencias y detección de ciclos BFS.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TareaDependenciaService")
class TareaDependenciaServiceTest {

    @Mock private TareaDependenciaRepository dependenciaRepository;
    @Mock private TareaRepository tareaRepository;

    @InjectMocks
    private TareaDependenciaService service;

    private Tarea tareaA;
    private Tarea tareaB;
    private Tarea tareaC;

    @BeforeEach
    void setUp() {
        tareaA = new Tarea();
        tareaA.setId(1L);
        tareaA.setTitulo("Tarea A");

        tareaB = new Tarea();
        tareaB.setId(2L);
        tareaB.setTitulo("Tarea B");

        tareaC = new Tarea();
        tareaC.setId(3L);
        tareaC.setTitulo("Tarea C");
    }

    // ══════════════════════════════════════════════════════════
    // listarDependencias
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listarDependencias")
    class ListarTests {

        @Test
        @DisplayName("combina dependencias salientes y entrantes")
        void listar_combinaSalientesYEntrantes() {
            TareaDependencia saliente = buildDep(10L, tareaA, tareaB, TipoDependencia.ESTRICTA);
            TareaDependencia entrante = buildDep(11L, tareaC, tareaA, TipoDependencia.SUAVE);

            when(dependenciaRepository.findByTareaOrigenId(1L)).thenReturn(List.of(saliente));
            when(dependenciaRepository.findByTareaDestinoId(1L)).thenReturn(List.of(entrante));

            var result = service.listarDependencias(1L);

            assertThat(result).hasSize(2);
            assertThat(result.stream().map(d -> d.id()).toList()).containsExactlyInAnyOrder(10L, 11L);
        }

        @Test
        @DisplayName("tarea sin dependencias devuelve lista vacía")
        void listar_sinDependencias_devuelveLista() {
            when(dependenciaRepository.findByTareaOrigenId(1L)).thenReturn(List.of());
            when(dependenciaRepository.findByTareaDestinoId(1L)).thenReturn(List.of());

            var result = service.listarDependencias(1L);

            assertThat(result).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════
    // crearDependencia
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("crearDependencia")
    class CrearTests {

        @BeforeEach
        void setUp() {
            when(tareaRepository.findById(1L)).thenReturn(Optional.of(tareaA));
            when(tareaRepository.findById(2L)).thenReturn(Optional.of(tareaB));
            when(dependenciaRepository.existsByTareaOrigenIdAndTareaDestinoId(any(), any()))
                    .thenReturn(false);
            when(dependenciaRepository.findByTareaOrigenId(2L)).thenReturn(List.of());
        }

        @Test
        @DisplayName("creación exitosa retorna DTO correcto")
        void crear_valida_creaYDevuelveDTO() {
            TareaDependencia guardada = buildDep(50L, tareaA, tareaB, TipoDependencia.ESTRICTA);
            when(dependenciaRepository.save(any())).thenReturn(guardada);

            var result = service.crearDependencia(1L, 2L, TipoDependencia.ESTRICTA);

            assertThat(result.id()).isEqualTo(50L);
            assertThat(result.tareaOrigenId()).isEqualTo(1L);
            assertThat(result.tareaDestinoId()).isEqualTo(2L);
            assertThat(result.tipo()).isEqualTo(TipoDependencia.ESTRICTA);
            verify(dependenciaRepository).save(any(TareaDependencia.class));
        }

        @Test
        @DisplayName("auto-dependencia → IllegalArgumentException")
        void crear_mismoOrigenDestino_lanzaIlegalArgument() {
            assertThatThrownBy(() -> service.crearDependencia(1L, 1L, TipoDependencia.SUAVE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sí misma");

            verify(dependenciaRepository, never()).save(any());
        }

        @Test
        @DisplayName("tarea origen no encontrada → EntityNotFoundException")
        void crear_origenNoEncontrado_lanzaEntityNotFound() {
            when(tareaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crearDependencia(99L, 2L, TipoDependencia.ESTRICTA))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Tarea origen");
        }

        @Test
        @DisplayName("tarea destino no encontrada → EntityNotFoundException")
        void crear_destinoNoEncontrado_lanzaEntityNotFound() {
            when(tareaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crearDependencia(1L, 99L, TipoDependencia.ESTRICTA))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Tarea destino");
        }

        @Test
        @DisplayName("dependencia duplicada → DependenciaDuplicadaException")
        void crear_dependenciaDuplicada_lanzaDuplicada() {
            when(dependenciaRepository.existsByTareaOrigenIdAndTareaDestinoId(1L, 2L))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.crearDependencia(1L, 2L, TipoDependencia.SUAVE))
                    .isInstanceOf(DependenciaDuplicadaException.class);

            verify(dependenciaRepository, never()).save(any());
        }

        @Test
        @DisplayName("ciclo directo A→B + B→A → DependenciaCiclicaException")
        void crear_cicloDirecto_lanzaCiclica() {
            // Ya existe: 2(B) → 1(A)
            TareaDependencia depBA = buildDep(20L, tareaB, tareaA, TipoDependencia.ESTRICTA);
            when(dependenciaRepository.findByTareaOrigenId(2L)).thenReturn(List.of(depBA));

            // Intentamos crear: 1(A) → 2(B) → ciclo!
            assertThatThrownBy(() -> service.crearDependencia(1L, 2L, TipoDependencia.ESTRICTA))
                    .isInstanceOf(DependenciaCiclicaException.class);

            verify(dependenciaRepository, never()).save(any());
        }

        @Test
        @DisplayName("ciclo transitivo A→B→C + C→A → DependenciaCiclicaException")
        void crear_cicloTransitivo_lanzaCiclica() {
            // Grafo existente: 2(B) → 3(C), 3(C) → 1(A)
            TareaDependencia depBC = buildDep(21L, tareaB, tareaC, TipoDependencia.ESTRICTA);
            TareaDependencia depCA = buildDep(22L, tareaC, tareaA, TipoDependencia.ESTRICTA);

            when(dependenciaRepository.findByTareaOrigenId(2L)).thenReturn(List.of(depBC));
            when(dependenciaRepository.findByTareaOrigenId(3L)).thenReturn(List.of(depCA));

            // Intentamos 1(A) → 2(B) → ciclo transitivo A→B→C→A
            assertThatThrownBy(() -> service.crearDependencia(1L, 2L, TipoDependencia.ESTRICTA))
                    .isInstanceOf(DependenciaCiclicaException.class);
        }

        @Test
        @DisplayName("grafo sin ciclo: A→B→C, crear C→D no lanza excepción")
        void crear_grafoDirigidoSinCiclo_noLanzaExcepcion() {
            when(tareaRepository.findById(3L)).thenReturn(Optional.of(tareaC));
            Tarea tareaD = new Tarea();
            tareaD.setId(4L);
            tareaD.setTitulo("Tarea D");
            when(tareaRepository.findById(4L)).thenReturn(Optional.of(tareaD));

            // BFS desde D: no hay dependencias salientes → no se llega a C
            when(dependenciaRepository.findByTareaOrigenId(4L)).thenReturn(List.of());

            TareaDependencia guardada = buildDep(60L, tareaC, tareaD, TipoDependencia.SUAVE);
            when(dependenciaRepository.save(any())).thenReturn(guardada);

            // No debe lanzar
            var result = service.crearDependencia(3L, 4L, TipoDependencia.SUAVE);

            assertThat(result.id()).isEqualTo(60L);
        }
    }

    // ══════════════════════════════════════════════════════════
    // eliminarDependencia
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("eliminarDependencia")
    class EliminarTests {

        @Test
        @DisplayName("eliminar dependencia existente llama deleteById")
        void eliminar_existente_invocaDeleteById() {
            when(dependenciaRepository.existsById(50L)).thenReturn(true);

            service.eliminarDependencia(50L);

            verify(dependenciaRepository).deleteById(50L);
        }

        @Test
        @DisplayName("eliminar dependencia inexistente → EntityNotFoundException")
        void eliminar_inexistente_lanzaEntityNotFound() {
            when(dependenciaRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> service.eliminarDependencia(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Dependencia no encontrada");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private TareaDependencia buildDep(Long id, Tarea origen, Tarea destino, TipoDependencia tipo) {
        TareaDependencia dep = new TareaDependencia();
        dep.setId(id);
        dep.setTareaOrigen(origen);
        dep.setTareaDestino(destino);
        dep.setTipo(tipo);
        return dep;
    }
}
