package com.kaos.planificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.kaos.calendario.service.CapacidadService;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.dto.TareaRequest;
import com.kaos.planificacion.dto.TareaResponse;
import com.kaos.planificacion.entity.EstadoTarea;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.SprintEstado;
import com.kaos.planificacion.entity.Tarea;
import com.kaos.planificacion.mapper.TareaMapper;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.planificacion.repository.TareaRepository;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para TareaService.
 * Cubre CA-15, CA-16, CA-17, CA-18, CA-19.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TareaService")
class TareaServiceTest {

    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private TareaMapper tareaMapper;
    @Mock
    private CapacidadService capacidadService;

    @InjectMocks
    private TareaService tareaService;

    private Tarea tarea;
    private Sprint sprint;
    private Squad squad;
    private Persona persona;
    private TareaResponse tareaResponse;

    // TareaRequest(titulo, sprintId, descripcion, tipo, categoria, estimacion, prioridad,
    //              personaId, diaAsignado, referenciaJira, estado)
    private TareaRequest requestValido;

    @BeforeEach
    void setUp() {
        squad = new Squad();
        squad.setId(1L);
        squad.setNombre("Squad Backend");

        sprint = new Sprint();
        sprint.setId(1L);
        sprint.setNombre("Sprint 1");
        sprint.setEstado(SprintEstado.ACTIVO);
        sprint.setSquad(squad);

        tarea = new Tarea();
        tarea.setId(1L);
        tarea.setTitulo("Implementar API");
        tarea.setSprint(sprint);
        tarea.setEstado(EstadoTarea.PENDIENTE);
        tarea.setCreatedAt(LocalDateTime.now());
        // Persona ya asignada al mismo día → actualizar() no activa validación de capacidad
        persona = new Persona();
        persona.setId(1L);
        tarea.setPersona(persona);
        tarea.setDiaAsignado(1);

        tareaResponse = mock(TareaResponse.class);
        when(tareaResponse.id()).thenReturn(1L);

        requestValido = new TareaRequest(
            "Implementar API", 1L, "Descripción",
            "FEATURE", "BACKEND", BigDecimal.valueOf(8),
            "ALTA", null, null, "JIRA-100", null
        );
    }

    @Nested
    @DisplayName("obtener()")
    class ObtenerTests {

        @Test
        @DisplayName("CA-18: Tarea existente retorna TareaResponse")
        void testObtenerExistente() {
            when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));
            when(tareaMapper.toResponse(tarea)).thenReturn(tareaResponse);

            TareaResponse result = tareaService.obtener(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("CA-18: Tarea inexistente lanza EntityNotFoundException")
        void testObtenerNoExistente() {
            when(tareaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tareaService.obtener(99L))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("crear()")
    class CrearTests {

        @Test
        @DisplayName("CA-15: Crear tarea válida persiste y retorna respuesta")
        void testCrearValida() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(tareaMapper.toEntity(requestValido)).thenReturn(tarea);
            when(tareaRepository.save(any())).thenReturn(tarea);
            when(tareaMapper.toResponse(tarea)).thenReturn(tareaResponse);

            TareaResponse result = tareaService.crear(requestValido);

            assertThat(result).isNotNull();
            verify(tareaRepository).save(any());
        }

        @Test
        @DisplayName("CA-15: Crear tarea con sprint inexistente lanza EntityNotFoundException")
        void testCrearSprintInvalido() {
            when(sprintRepository.findById(99L)).thenReturn(Optional.empty());
            TareaRequest req = new TareaRequest(
                "Tarea X", 99L, null, "FEATURE", "BACKEND",
                BigDecimal.valueOf(4), "MEDIA", null, null, null, null
            );

            assertThatThrownBy(() -> tareaService.crear(req))
                .isInstanceOf(EntityNotFoundException.class);
            verify(tareaRepository, never()).save(any());
        }

        @Test
        @DisplayName("CA-15: Crear tarea con estimación = 0 lanza IllegalArgumentException")
        void testCrearEstimacionCero() {
            sprint.setEstado(SprintEstado.PLANIFICACION);
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            TareaRequest req = new TareaRequest(
                "Tarea cero", 1L, null, "FEATURE", "BACKEND",
                BigDecimal.ZERO, "MEDIA", null, null, null, null
            );

            assertThatThrownBy(() -> tareaService.crear(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimación");
            verify(tareaRepository, never()).save(any());
        }

        @Test
        @DisplayName("CA-15: Crear tarea con sprint CERRADO lanza excepción")
        void testCrearSprintCerrado() {
            sprint.setEstado(SprintEstado.CERRADO);
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));

            assertThatThrownBy(() -> tareaService.crear(requestValido))
                .isInstanceOf(Exception.class);
            verify(tareaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar()")
    class ActualizarTests {

        @Test
        @DisplayName("CA-15: Actualizar tarea válida persiste cambios")
        void testActualizarValido() {
            TareaRequest request = new TareaRequest("Tarea Actualizada", 1L, "Descripción", "HISTORIA", 
                "EVOLUTIVO", BigDecimal.valueOf(3), "NORMAL", 1L, 1, "JIRA-123", "EN_PROGRESO");
            when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));
            when(personaRepository.existsById(1L)).thenReturn(true);
            when(tareaRepository.save(any())).thenReturn(tarea);
            when(tareaMapper.toResponse(tarea)).thenReturn(tareaResponse);

            TareaResponse result = tareaService.actualizar(1L, request);

            assertThat(result).isNotNull();
            verify(tareaRepository).save(any());
        }

        @Test
        @DisplayName("CA-15: Actualizar tarea inexistente lanza EntityNotFoundException")
        void testActualizarInexistente() {
            TareaRequest request = new TareaRequest("Tarea X", 1L, "Descripción", "HISTORIA", 
                "CORRECTIVO", BigDecimal.valueOf(3), "NORMAL", 1L, 1, "JIRA-123", "PENDIENTE");
            when(tareaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tareaService.actualizar(99L, request))
                .isInstanceOf(EntityNotFoundException.class);
            verify(tareaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cambiarEstado()")
    class CambiarEstadoTests {

        @Test
        @DisplayName("CA-17: PENDIENTE → EN_PROGRESO es válido")
        void testCambiarPendienteAEnProgreso() {
            tarea.setEstado(EstadoTarea.PENDIENTE);
            when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));
            when(tareaRepository.save(any())).thenReturn(tarea);
            when(tareaMapper.toResponse(tarea)).thenReturn(tareaResponse);

            TareaResponse result = tareaService.cambiarEstado(1L, EstadoTarea.EN_PROGRESO);

            assertThat(result).isNotNull();
            verify(tareaRepository).save(any());
        }

        @Test
        @DisplayName("CA-17: EN_PROGRESO → COMPLETADA es válido")
        void testCambiarEnProgresoACompletada() {
            tarea.setEstado(EstadoTarea.EN_PROGRESO);
            when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));
            when(tareaRepository.save(any())).thenReturn(tarea);
            when(tareaMapper.toResponse(tarea)).thenReturn(tareaResponse);

            TareaResponse result = tareaService.cambiarEstado(1L, EstadoTarea.COMPLETADA);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("CA-17: Transición inválida desde COMPLETADA lanza excepción")
        void testCambiarEstadoInvalido() {
            tarea.setEstado(EstadoTarea.COMPLETADA);
            when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));

            assertThatThrownBy(() -> tareaService.cambiarEstado(1L, EstadoTarea.PENDIENTE))
                .isInstanceOf(Exception.class);
            verify(tareaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listar()")
    class ListarTests {

        @Test
        @DisplayName("CA-18: Listar por sprintId con paginación")
        void testListarPorSprint() {
            var pageable = PageRequest.of(0, 10);
            when(tareaRepository.findBySprintId(1L, pageable)).thenReturn(new PageImpl<>(List.of(tarea)));
            when(tareaMapper.toResponse(tarea)).thenReturn(tareaResponse);

            var result = tareaService.listar(1L, null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("CA-18: Listar por sprintId + personaId aplica filtro")
        void testListarPorSprintYPersona() {
            var pageable = PageRequest.of(0, 10);
            when(tareaRepository.findBySprintIdAndPersonaId(1L, 2L, pageable))
                .thenReturn(new PageImpl<>(List.of(tarea)));
            when(tareaMapper.toResponse(tarea)).thenReturn(tareaResponse);

            var result = tareaService.listar(1L, 2L, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("eliminar()")
    class EliminarTests {

        @Test
        @DisplayName("CA-19: Eliminar tarea PENDIENTE ejecuta deleteById")
        void testEliminarPendiente() {
            tarea.setEstado(EstadoTarea.PENDIENTE);
            when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));

            tareaService.eliminar(1L);

            verify(tareaRepository).deleteById(1L);
        }

        @Test
        @DisplayName("CA-19: Eliminar tarea EN_PROGRESO lanza excepción")
        void testEliminarEnProgreso() {
            tarea.setEstado(EstadoTarea.EN_PROGRESO);
            when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));

            assertThatThrownBy(() -> tareaService.eliminar(1L))
                .isInstanceOf(Exception.class);
            verify(tareaRepository, never()).deleteById(anyLong());
        }
    }
}
