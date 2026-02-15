package com.kaos.squad.service;

import com.kaos.squad.dto.SquadRequest;
import com.kaos.squad.dto.SquadResponse;
import com.kaos.squad.entity.EstadoSquad;
import com.kaos.squad.entity.Squad;
import com.kaos.squad.mapper.SquadMapper;
import com.kaos.squad.repository.SquadRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link SquadService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SquadService Tests")
class SquadServiceTest {

    @Mock
    private SquadRepository repository;

    @Mock
    private SquadMapper mapper;

    @InjectMocks
    private SquadService service;

    private Squad squadMock;
    private SquadRequest requestMock;
    private SquadResponse responseMock;

    @BeforeEach
    void setUp() {
        squadMock = new Squad();
        squadMock.setId(1L);
        squadMock.setNombre("Squad Alpha");
        squadMock.setDescripcion("Squad de desarrollo");
        squadMock.setEstado(EstadoSquad.ACTIVO);
        squadMock.setIdSquadCorrJira("CORR-123");
        squadMock.setIdSquadEvolJira("EVOL-456");

        // SquadRequest - 4 campos
        requestMock = new SquadRequest(
                "Squad Alpha",
                "Squad de desarrollo",
                "CORR-123",
                "EVOL-456"
        );

        // SquadResponse - 8 campos
        responseMock = new SquadResponse(
                1L,
                "Squad Alpha",
                "Squad de desarrollo",
                EstadoSquad.ACTIVO,
                "CORR-123",
                "EVOL-456",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("listar() - Listado con filtros")
    class ListarTests {

        @Test
        @DisplayName("Debe listar todos los squads sin filtro")
        void listar_sinFiltro_retornaTodos() {
            // Given
            when(repository.findAll()).thenReturn(List.of(squadMock));
            when(mapper.toResponseList(anyList())).thenReturn(List.of(responseMock));

            // When
            List<SquadResponse> result = service.listar(null);

            // Then
            assertThat(result).isNotEmpty().hasSize(1);
            assertThat(result.get(0)).isEqualTo(responseMock);
            verify(repository).findAll();
            verify(repository, never()).findByEstado(any());
        }

        @Test
        @DisplayName("Debe listar squads filtrando por estado ACTIVO")
        void listar_conFiltroActivo_retornaActivos() {
            // Given
            when(repository.findByEstado(EstadoSquad.ACTIVO)).thenReturn(List.of(squadMock));
            when(mapper.toResponseList(anyList())).thenReturn(List.of(responseMock));

            // When
            List<SquadResponse> result = service.listar(EstadoSquad.ACTIVO);

            // Then
            assertThat(result).isNotEmpty();
            verify(repository).findByEstado(EstadoSquad.ACTIVO);
            verify(repository, never()).findAll();
        }

        @Test
        @DisplayName("Debe listar squads filtrando por estado INACTIVO")
        void listar_conFiltroInactivo_retornaInactivos() {
            // Given
            when(repository.findByEstado(EstadoSquad.INACTIVO)).thenReturn(List.of());
            when(mapper.toResponseList(anyList())).thenReturn(List.of());

            // When
            List<SquadResponse> result = service.listar(EstadoSquad.INACTIVO);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findByEstado(EstadoSquad.INACTIVO);
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay squads")
        void listar_sinSquads_retornaVacio() {
            // Given
            when(repository.findAll()).thenReturn(List.of());
            when(mapper.toResponseList(anyList())).thenReturn(List.of());

            // When
            List<SquadResponse> result = service.listar(null);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findAll();
        }
    }

    @Nested
    @DisplayName("obtener() - Obtener por ID")
    class ObtenerTests {

        @Test
        @DisplayName("Debe obtener squad existente por ID")
        void obtener_conIdExistente_retornaSquad() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(mapper.toResponse(squadMock)).thenReturn(responseMock);

            // When
            SquadResponse result = service.obtener(1L);

            // Then
            assertThat(result).isNotNull().isEqualTo(responseMock);
            verify(repository).findById(1L);
            verify(mapper).toResponse(squadMock);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando ID no existe")
        void obtener_conIdInexistente_lanzaExcepcion() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.obtener(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Squad no encontrado con id: 999");
            verify(repository).findById(999L);
        }
    }

    @Nested
    @DisplayName("crear() - Creación de squad")
    class CrearTests {

        @Test
        @DisplayName("Debe crear squad con datos válidos")
        void crear_conDatosValidos_creaSquad() {
            // Given
            when(repository.existsByNombre(requestMock.nombre())).thenReturn(false);
            when(mapper.toEntity(requestMock)).thenReturn(squadMock);
            when(repository.save(any(Squad.class))).thenReturn(squadMock);
            when(mapper.toResponse(squadMock)).thenReturn(responseMock);

            // When
            SquadResponse result = service.crear(requestMock);

            // Then
            assertThat(result).isNotNull().isEqualTo(responseMock);
            verify(repository).existsByNombre(requestMock.nombre());
            verify(mapper).toEntity(requestMock);
            verify(repository).save(argThat(s -> s.getEstado() == EstadoSquad.ACTIVO));
            verify(mapper).toResponse(squadMock);
        }

        @Test
        @DisplayName("Debe establecer estado ACTIVO por defecto al crear")
        void crear_estableceEstadoActivo() {
            // Given
            when(repository.existsByNombre(anyString())).thenReturn(false);
            when(mapper.toEntity(any())).thenReturn(squadMock);
            when(repository.save(any())).thenReturn(squadMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            service.crear(requestMock);

            // Then
            verify(repository).save(argThat(s -> s.getEstado() == EstadoSquad.ACTIVO));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando nombre ya existe")
        void crear_conNombreDuplicado_lanzaExcepcion() {
            // Given
            when(repository.existsByNombre(requestMock.nombre())).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.crear(requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un squad con el nombre");
            verify(repository).existsByNombre(requestMock.nombre());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar() - Actualización de squad")
    class ActualizarTests {

        @Test
        @DisplayName("Debe actualizar squad con datos válidos")
        void actualizar_conDatosValidos_actualizaSquad() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.existsByNombreAndIdNot(requestMock.nombre(), 1L)).thenReturn(false);
            doNothing().when(mapper).updateEntity(requestMock, squadMock);
            when(repository.save(squadMock)).thenReturn(squadMock);
            when(mapper.toResponse(squadMock)).thenReturn(responseMock);

            // When
            SquadResponse result = service.actualizar(1L, requestMock);

            // Then
            assertThat(result).isNotNull().isEqualTo(responseMock);
            verify(repository).findById(1L);
            verify(repository).existsByNombreAndIdNot(requestMock.nombre(), 1L);
            verify(mapper).updateEntity(requestMock, squadMock);
            verify(repository).save(squadMock);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando ID no existe")
        void actualizar_conIdInexistente_lanzaExcepcion() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.actualizar(999L, requestMock))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Squad no encontrado con id: 999");
            verify(repository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando nombre ya existe en otro squad")
        void actualizar_conNombreDuplicado_lanzaExcepcion() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.existsByNombreAndIdNot(requestMock.nombre(), 1L)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.actualizar(1L, requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un squad con el nombre");
            verify(repository).existsByNombreAndIdNot(requestMock.nombre(), 1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe permitir actualizar sin cambiar nombre (mismo nombre)")
        void actualizar_mantieneNombre_noValidaDuplicado() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.existsByNombreAndIdNot(requestMock.nombre(), 1L)).thenReturn(false);
            doNothing().when(mapper).updateEntity(any(), any());
            when(repository.save(any())).thenReturn(squadMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            SquadResponse result = service.actualizar(1L, requestMock);

            // Then
            assertThat(result).isNotNull();
            verify(repository).existsByNombreAndIdNot(requestMock.nombre(), 1L);
        }
    }

    @Nested
    @DisplayName("desactivar() - Desactivación de squad")
    class DesactivarTests {

        @Test
        @DisplayName("Debe desactivar squad sin sprints activos")
        void desactivar_sinSprintsActivos_desactivaSquad() {
            // Given
            squadMock.setEstado(EstadoSquad.ACTIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.save(squadMock)).thenReturn(squadMock);
            when(mapper.toResponse(squadMock)).thenReturn(responseMock);

            // When
            SquadResponse result = service.desactivar(1L);

            // Then
            assertThat(result).isNotNull();
            verify(repository).findById(1L);
            verify(repository).save(argThat(s -> s.getEstado() == EstadoSquad.INACTIVO));
            verify(mapper).toResponse(squadMock);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando ID no existe")
        void desactivar_conIdInexistente_lanzaExcepcion() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.desactivar(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Squad no encontrado con id: 999");
            verify(repository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe desactivar squad ya desactivado sin error")
        void desactivar_squadYaDesactivado_ejecutaSinError() {
            // Given
            squadMock.setEstado(EstadoSquad.INACTIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.save(squadMock)).thenReturn(squadMock);
            when(mapper.toResponse(squadMock)).thenReturn(responseMock);

            // When
            SquadResponse result = service.desactivar(1L);

            // Then
            assertThat(result).isNotNull();
            verify(repository).save(argThat(s -> s.getEstado() == EstadoSquad.INACTIVO));
        }

        @Test
        @DisplayName("Debe cambia estado de ACTIVO a INACTIVO")
        void desactivar_cambiaEstado() {
            // Given
            squadMock.setEstado(EstadoSquad.ACTIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.save(any())).thenReturn(squadMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            service.desactivar(1L);

            // Then
            assertThat(squadMock.getEstado()).isEqualTo(EstadoSquad.INACTIVO);
        }

        /**
         * TODO [Bloque 3]: Implementar test cuando exista SprintRepository.
         * Este test debe verificar que se lanza IllegalStateException cuando
         * un squad tiene sprints activos.
         *
         * Ejemplo futuro:
         * <pre>
         * @Test
         * @DisplayName("Debe lanzar IllegalStateException cuando tiene sprints activos")
         * void desactivar_conSprintsActivos_lanzaExcepcion() {
         *     // Given
         *     when(repository.findById(1L)).thenReturn(Optional.of(squadMock));
         *     when(sprintRepository.existsBySquadIdAndEstado(1L, EstadoSprint.ACTIVO))
         *         .thenReturn(true);
         *
         *     // When / Then
         *     assertThatThrownBy(() -> service.desactivar(1L))
         *         .isInstanceOf(IllegalStateException.class)
         *         .hasMessageContaining("tiene sprints activos");
         *     verify(repository, never()).save(any());
         * }
         * </pre>
         */
    }
}
