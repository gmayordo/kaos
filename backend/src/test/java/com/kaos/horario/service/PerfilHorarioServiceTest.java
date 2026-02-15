package com.kaos.horario.service;

import com.kaos.horario.dto.PerfilHorarioRequest;
import com.kaos.horario.dto.PerfilHorarioResponse;
import com.kaos.horario.entity.PerfilHorario;
import com.kaos.horario.mapper.PerfilHorarioMapper;
import com.kaos.horario.repository.PerfilHorarioRepository;
import com.kaos.persona.repository.PersonaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link PerfilHorarioService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PerfilHorarioService Tests")
class PerfilHorarioServiceTest {

    @Mock
    private PerfilHorarioRepository repository;

    @Mock
    private PerfilHorarioMapper mapper;

    @Mock
    private PersonaRepository personaRepository;

    @InjectMocks
    private PerfilHorarioService service;

    private PerfilHorario perfilMock;
    private PerfilHorarioRequest requestMock;
    private PerfilHorarioResponse responseMock;

    @BeforeEach
    void setUp() {
        perfilMock = new PerfilHorario();
        perfilMock.setId(1L);
        perfilMock.setNombre("Zona Europa");
        perfilMock.setZonaHoraria("Europe/Madrid");
        perfilMock.setHorasLunes(new BigDecimal("8.00"));
        perfilMock.setHorasMartes(new BigDecimal("8.00"));
        perfilMock.setHorasMiercoles(new BigDecimal("8.00"));
        perfilMock.setHorasJueves(new BigDecimal("8.00"));
        perfilMock.setHorasViernes(new BigDecimal("8.00"));

        // PerfilHorarioRequest - 7 campos
        requestMock = new PerfilHorarioRequest(
                "Zona Europa",
                "Europe/Madrid",
                new BigDecimal("8.00"),
                new BigDecimal("8.00"),
                new BigDecimal("8.00"),
                new BigDecimal("8.00"),
                new BigDecimal("8.00")
        );

        // PerfilHorarioResponse - 11 campos
        responseMock = new PerfilHorarioResponse(
                1L,
                "Zona Europa",
                "Europe/Madrid",
                new BigDecimal("8.00"),
                new BigDecimal("8.00"),
                new BigDecimal("8.00"),
                new BigDecimal("8.00"),
                new BigDecimal("8.00"),
                new BigDecimal("40.00"),  // totalSemanal
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("listar() - Listado completo")
    class ListarTests {

        @Test
        @DisplayName("Debe listar todos los perfiles de horario")
        void listar_retornaTodos() {
            // Given
            when(repository.findAll()).thenReturn(List.of(perfilMock));
            when(mapper.toResponseList(anyList())).thenReturn(List.of(responseMock));

            // When
            List<PerfilHorarioResponse> result = service.listar();

            // Then
            assertThat(result).isNotEmpty().hasSize(1);
            assertThat(result.get(0)).isEqualTo(responseMock);
            verify(repository).findAll();
            verify(mapper).toResponseList(anyList());
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay perfiles")
        void listar_sinPerfiles_retornaVacio() {
            // Given
            when(repository.findAll()).thenReturn(List.of());
            when(mapper.toResponseList(anyList())).thenReturn(List.of());

            // When
            List<PerfilHorarioResponse> result = service.listar();

            // Then
            assertThat(result).isEmpty();
            verify(repository).findAll();
        }

        @Test
        @DisplayName("Debe listar múltiples perfiles correctamente")
        void listar_variosPerfiles_retornaTodos() {
            // Given
            PerfilHorario perfil2 = new PerfilHorario();
            perfil2.setId(2L);
            perfil2.setNombre("Zona América");
            when(repository.findAll()).thenReturn(List.of(perfilMock, perfil2));
            PerfilHorarioResponse response2 = new PerfilHorarioResponse(
                    2L, "Zona América", "America/New_York",
                    new BigDecimal("8"), new BigDecimal("8"), new BigDecimal("8"),
                    new BigDecimal("8"), new BigDecimal("8"), new BigDecimal("40"),
                    LocalDateTime.now(), LocalDateTime.now()
            );
            when(mapper.toResponseList(anyList())).thenReturn(List.of(responseMock, response2));

            // When
            List<PerfilHorarioResponse> result = service.listar();

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("obtener() - Obtener por ID")
    class ObtenerTests {

        @Test
        @DisplayName("Debe obtener perfil existente por ID")
        void obtener_conIdExistente_retornaPerfil() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));
            when(mapper.toResponse(perfilMock)).thenReturn(responseMock);

            // When
            PerfilHorarioResponse result = service.obtener(1L);

            // Then
            assertThat(result).isNotNull().isEqualTo(responseMock);
            verify(repository).findById(1L);
            verify(mapper).toResponse(perfilMock);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando ID no existe")
        void obtener_conIdInexistente_lanzaExcepcion() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.obtener(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Perfil de horario no encontrado con id: 999");
            verify(repository).findById(999L);
        }
    }

    @Nested
    @DisplayName("crear() - Creación de perfil")
    class CrearTests {

        @Test
        @DisplayName("Debe crear perfil con datos válidos")
        void crear_conDatosValidos_creaPerfil() {
            // Given
            when(repository.existsByNombre(requestMock.nombre())).thenReturn(false);
            when(mapper.toEntity(requestMock)).thenReturn(perfilMock);
            when(repository.save(any(PerfilHorario.class))).thenReturn(perfilMock);
            when(mapper.toResponse(perfilMock)).thenReturn(responseMock);

            // When
            PerfilHorarioResponse result = service.crear(requestMock);

            // Then
            assertThat(result).isNotNull().isEqualTo(responseMock);
            verify(repository).existsByNombre(requestMock.nombre());
            verify(mapper).toEntity(requestMock);
            verify(repository).save(any(PerfilHorario.class));
            verify(mapper).toResponse(perfilMock);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando nombre ya existe")
        void crear_conNombreDuplicado_lanzaExcepcion() {
            // Given
            when(repository.existsByNombre(requestMock.nombre())).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.crear(requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un perfil de horario con el nombre");
            verify(repository).existsByNombre(requestMock.nombre());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe crear perfil con horario parcial (0 horas algunos días)")
        void crear_conHorarioParcial_creaPerfil() {
            // Given
            PerfilHorarioRequest requestParcial = new PerfilHorarioRequest(
                    "Part-time",
                    "Europe/Madrid",
                    new BigDecimal("4.00"),
                    new BigDecimal("4.00"),
                    new BigDecimal("0.00"),  // miércoles no trabaja
                    new BigDecimal("4.00"),
                    new BigDecimal("0.00")   // viernes no trabaja
            );
            when(repository.existsByNombre(anyString())).thenReturn(false);
            when(mapper.toEntity(any())).thenReturn(perfilMock);
            when(repository.save(any())).thenReturn(perfilMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            PerfilHorarioResponse result = service.crear(requestParcial);

            // Then
            assertThat(result).isNotNull();
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar() - Actualización de perfil")
    class ActualizarTests {

        @Test
        @DisplayName("Debe actualizar perfil con datos válidos")
        void actualizar_conDatosValidos_actualizaPerfil() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));
            when(repository.existsByNombreAndIdNot(requestMock.nombre(), 1L)).thenReturn(false);
            doNothing().when(mapper).updateEntity(requestMock, perfilMock);
            when(repository.save(perfilMock)).thenReturn(perfilMock);
            when(mapper.toResponse(perfilMock)).thenReturn(responseMock);

            // When
            PerfilHorarioResponse result = service.actualizar(1L, requestMock);

            // Then
            assertThat(result).isNotNull().isEqualTo(responseMock);
            verify(repository).findById(1L);
            verify(repository).existsByNombreAndIdNot(requestMock.nombre(), 1L);
            verify(mapper).updateEntity(requestMock, perfilMock);
            verify(repository).save(perfilMock);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando ID no existe")
        void actualizar_conIdInexistente_lanzaExcepcion() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.actualizar(999L, requestMock))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Perfil de horario no encontrado con id: 999");
            verify(repository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando nombre ya existe en otro perfil")
        void actualizar_conNombreDuplicado_lanzaExcepcion() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));
            when(repository.existsByNombreAndIdNot(requestMock.nombre(), 1L)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.actualizar(1L, requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un perfil de horario con el nombre");
            verify(repository).existsByNombreAndIdNot(requestMock.nombre(), 1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe permitir actualizar sin cambiar nombre (mismo nombre)")
        void actualizar_mantieneNombre_noValidaDuplicado() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));
            when(repository.existsByNombreAndIdNot(requestMock.nombre(), 1L)).thenReturn(false);
            doNothing().when(mapper).updateEntity(any(), any());
            when(repository.save(any())).thenReturn(perfilMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            PerfilHorarioResponse result = service.actualizar(1L, requestMock);

            // Then
            assertThat(result).isNotNull();
            verify(repository).existsByNombreAndIdNot(requestMock.nombre(), 1L);
        }
    }

    @Nested
    @DisplayName("eliminar() - Eliminación de perfil")
    class EliminarTests {

        @Test
        @DisplayName("Debe eliminar perfil sin personas asignadas")
        void eliminar_sinPersonas_eliminaPerfil() {
            // Given
            when(repository.existsById(1L)).thenReturn(true);
            when(personaRepository.existsByPerfilHorarioId(1L)).thenReturn(false);
            doNothing().when(repository).deleteById(1L);

            // When
            service.eliminar(1L);

            // Then
            verify(repository).existsById(1L);
            verify(personaRepository).existsByPerfilHorarioId(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando ID no existe")
        void eliminar_conIdInexistente_lanzaExcepcion() {
            // Given
            when(repository.existsById(999L)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> service.eliminar(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Perfil de horario no encontrado con id: 999");
            verify(repository).existsById(999L);
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Debe lanzar IllegalStateException cuando tiene personas asignadas")
        void eliminar_conPersonasAsignadas_lanzaExcepcion() {
            // Given
            when(repository.existsById(1L)).thenReturn(true);
            when(personaRepository.existsByPerfilHorarioId(1L)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.eliminar(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No se puede eliminar: hay personas asignadas");
            verify(repository).existsById(1L);
            verify(personaRepository).existsByPerfilHorarioId(1L);
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Debe validar existencia antes de verificar personas")
        void eliminar_validaExistenciaAntes() {
            // Given
            when(repository.existsById(1L)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> service.eliminar(1L))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(repository).existsById(1L);
            verify(personaRepository, never()).existsByPerfilHorarioId(any());
        }
    }
}
