package com.kaos.dedicacion.service;

import com.kaos.dedicacion.dto.SquadMemberRequest;
import com.kaos.dedicacion.dto.SquadMemberResponse;
import com.kaos.dedicacion.entity.SquadMember;
import com.kaos.dedicacion.mapper.SquadMemberMapper;
import com.kaos.dedicacion.repository.SquadMemberRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.entity.Rol;
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.squad.entity.Squad;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link SquadMemberService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SquadMemberService Tests")
class SquadMemberServiceTest {

    @Mock
    private SquadMemberRepository repository;

    @Mock
    private SquadMemberMapper mapper;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private SquadRepository squadRepository;

    @InjectMocks
    private SquadMemberService service;

    private SquadMember squadMemberMock;
    private SquadMemberRequest requestMock;
    private SquadMemberResponse responseMock;
    private Persona personaMock;
    private Squad squadMock;

    @BeforeEach
    void setUp() {
        personaMock = new Persona();
        personaMock.setId(1L);
        personaMock.setNombre("Juan Pérez");

        squadMock = new Squad();
        squadMock.setId(1L);
        squadMock.setNombre("Squad Alpha");

        squadMemberMock = new SquadMember();
        squadMemberMock.setId(1L);
        squadMemberMock.setPersona(personaMock);
        squadMemberMock.setSquad(squadMock);
        squadMemberMock.setRol(Rol.BACKEND);
        squadMemberMock.setPorcentaje(50);
        squadMemberMock.setFechaInicio(LocalDate.now());
        squadMemberMock.setFechaFin(null);

        // SquadMemberRequest - 6 campos
        requestMock = new SquadMemberRequest(
                1L,  // personaId
                1L,  // squadId
                Rol.BACKEND,
                50,
                LocalDate.now(),
                null  // fechaFin
        );

        // SquadMemberResponse - 17 campos
        responseMock = new SquadMemberResponse(
                1L,
                1L,  // personaId
                "Juan Pérez",
                1L,  // squadId
                "Squad Alpha",
                Rol.BACKEND,
                50,
                LocalDate.now(),
                null,  // fechaFin
                new BigDecimal("4.00"),  // capacidadDiariaLunes
                new BigDecimal("4.00"),
                new BigDecimal("4.00"),
                new BigDecimal("4.00"),
                new BigDecimal("4.00"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("listarMiembrosSquad() - Listar miembros por squad")
    class ListarMiembrosSquadTests {

        @Test
        @DisplayName("Debe listar miembros de squad existente")
        void listarMiembrosSquad_squadExistente_retornaLista() {
            // Given
            when(squadRepository.existsById(1L)).thenReturn(true);
            when(repository.findBySquadId(1L)).thenReturn(List.of(squadMemberMock));
            when(mapper.toResponseList(anyList())).thenReturn(List.of(responseMock));

            // When
            List<SquadMemberResponse> result = service.listarMiembrosSquad(1L);

            // Then
            assertThat(result).isNotEmpty().hasSize(1);
            assertThat(result.get(0)).isEqualTo(responseMock);
            verify(squadRepository).existsById(1L);
            verify(repository).findBySquadId(1L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando squad no tiene miembros")
        void listarMiembrosSquad_sinMiembros_retornaVacio() {
            // Given
            when(squadRepository.existsById(1L)).thenReturn(true);
            when(repository.findBySquadId(1L)).thenReturn(List.of());
            when(mapper.toResponseList(anyList())).thenReturn(List.of());

            // When
            List<SquadMemberResponse> result = service.listarMiembrosSquad(1L);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findBySquadId(1L);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando squad no existe")
        void listarMiembrosSquad_squadInexistente_lanzaExcepcion() {
            // Given
            when(squadRepository.existsById(999L)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> service.listarMiembrosSquad(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Squad no encontrado con id: 999");
            verify(squadRepository).existsById(999L);
            verify(repository, never()).findBySquadId(any());
        }
    }

    @Nested
    @DisplayName("listarSquadsDePersona() - Listar squads por persona")
    class ListarSquadsDePersonaTests {

        @Test
        @DisplayName("Debe listar squads de persona existente")
        void listarSquadsDePersona_personaExistente_retornaLista() {
            // Given
            when(personaRepository.existsById(1L)).thenReturn(true);
            when(repository.findByPersonaId(1L)).thenReturn(List.of(squadMemberMock));
            when(mapper.toResponseList(anyList())).thenReturn(List.of(responseMock));

            // When
            List<SquadMemberResponse> result = service.listarSquadsDePersona(1L);

            // Then
            assertThat(result).isNotEmpty().hasSize(1);
            verify(personaRepository).existsById(1L);
            verify(repository).findByPersonaId(1L);
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando persona no tiene asignaciones")
        void listarSquadsDePersona_sinAsignaciones_retornaVacio() {
            // Given
            when(personaRepository.existsById(1L)).thenReturn(true);
            when(repository.findByPersonaId(1L)).thenReturn(List.of());
            when(mapper.toResponseList(anyList())).thenReturn(List.of());

            // When
            List<SquadMemberResponse> result = service.listarSquadsDePersona(1L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando persona no existe")
        void listarSquadsDePersona_personaInexistente_lanzaExcepcion() {
            // Given
            when(personaRepository.existsById(999L)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> service.listarSquadsDePersona(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Persona no encontrada con id: 999");
            verify(personaRepository).existsById(999L);
            verify(repository, never()).findByPersonaId(any());
        }
    }

    @Nested
    @DisplayName("asignar() - Crear asignación")
    class AsignarTests {

        @Test
        @DisplayName("Debe asignar persona a squad con datos válidos")
        void asignar_conDatosValidos_creaAsignacion() {
            // Given
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.existsByPersonaIdAndSquadId(1L, 1L)).thenReturn(false);
            when(repository.sumPorcentajeByPersonaId(1L, null)).thenReturn(0);
            when(mapper.toEntity(requestMock)).thenReturn(squadMemberMock);
            when(repository.save(any(SquadMember.class))).thenReturn(squadMemberMock);
            when(mapper.toResponse(squadMemberMock)).thenReturn(responseMock);

            // When
            SquadMemberResponse result = service.asignar(requestMock);

            // Then
            assertThat(result).isNotNull().isEqualTo(responseMock);
            verify(personaRepository).findById(1L);
            verify(squadRepository).findById(1L);
            verify(repository).existsByPersonaIdAndSquadId(1L, 1L);
            verify(repository).sumPorcentajeByPersonaId(1L, null);
            verify(repository).save(any(SquadMember.class));
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando persona no existe")
        void asignar_personaInexistente_lanzaExcepcion() {
            // Given
            when(personaRepository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            SquadMemberRequest request = new SquadMemberRequest(
                    999L, 1L, Rol.BACKEND, 50, LocalDate.now(), null
            );
            assertThatThrownBy(() -> service.asignar(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Persona no encontrada con id: 999");
            verify(personaRepository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando squad no existe")
        void asignar_squadInexistente_lanzaExcepcion() {
            // Given
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(squadRepository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            SquadMemberRequest request = new SquadMemberRequest(
                    1L, 999L, Rol.BACKEND, 50, LocalDate.now(), null
            );
            assertThatThrownBy(() -> service.asignar(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Squad no encontrado con id: 999");
            verify(squadRepository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException cuando ya existe asignación")
        void asignar_asignacionDuplicada_lanzaExcepcion() {
            // Given
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.existsByPersonaIdAndSquadId(1L, 1L)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.asignar(requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("La persona ya está asignada a este squad");
            verify(repository).existsByPersonaIdAndSquadId(1L, 1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException cuando se supera 100% de dedicación")
        void asignar_porcentajeSuperado_lanzaExcepcion() {
            // Given
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.existsByPersonaIdAndSquadId(1L, 1L)).thenReturn(false);
            when(repository.sumPorcentajeByPersonaId(1L, null)).thenReturn(60);

            // When / Then
            SquadMemberRequest requestExcesivo = new SquadMemberRequest(
                    1L, 1L, Rol.BACKEND, 50, LocalDate.now(), null
            );
            assertThatThrownBy(() -> service.asignar(requestExcesivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("El porcentaje total de dedicación sería 110%")
                    .hasMessageContaining("superando el 100%");
            verify(repository).sumPorcentajeByPersonaId(1L, null);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe permitir asignación hasta exactamente 100%")
        void asignar_hasta100Porciento_creaAsignacion() {
            // Given
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.existsByPersonaIdAndSquadId(1L, 1L)).thenReturn(false);
            when(repository.sumPorcentajeByPersonaId(1L, null)).thenReturn(50);
            when(mapper.toEntity(any())).thenReturn(squadMemberMock);
            when(repository.save(any())).thenReturn(squadMemberMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            SquadMemberRequest request100 = new SquadMemberRequest(
                    1L, 1L, Rol.BACKEND, 50, LocalDate.now(), null  // 50% actual + 50% nuevo = 100%
            );
            SquadMemberResponse result = service.asignar(request100);

            // Then
            assertThat(result).isNotNull();
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Debe permitir asignación con porcentaje 0%")
        void asignar_conPorcentajeCero_creaAsignacion() {
            // Given
            when(personaRepository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(squadRepository.findById(1L)).thenReturn(Optional.of(squadMock));
            when(repository.existsByPersonaIdAndSquadId(1L, 1L)).thenReturn(false);
            when(repository.sumPorcentajeByPersonaId(1L, null)).thenReturn(0);
            when(mapper.toEntity(any())).thenReturn(squadMemberMock);
            when(repository.save(any())).thenReturn(squadMemberMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            SquadMemberRequest requestCero = new SquadMemberRequest(
                    1L, 1L, Rol.QA, 0, LocalDate.now(), null
            );
            SquadMemberResponse result = service.asignar(requestCero);

            // Then
            assertThat(result).isNotNull();
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar() - Actualizar asignación")
    class ActualizarTests {

        @Test
        @DisplayName("Debe actualizar asignación con datos válidos")
        void actualizar_conDatosValidos_actualizaAsignacion() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(squadMemberMock));
            when(repository.sumPorcentajeByPersonaId(1L, 1L)).thenReturn(0);
            doNothing().when(mapper).updateEntity(requestMock, squadMemberMock);
            when(repository.save(squadMemberMock)).thenReturn(squadMemberMock);
            when(mapper.toResponse(squadMemberMock)).thenReturn(responseMock);

            // When
            SquadMemberResponse result = service.actualizar(1L, requestMock);

            // Then
            assertThat(result).isNotNull().isEqualTo(responseMock);
            verify(repository).findById(1L);
            verify(mapper).updateEntity(requestMock, squadMemberMock);
            verify(repository).save(squadMemberMock);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando asignación no existe")
        void actualizar_asignacionInexistente_lanzaExcepcion() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.actualizar(999L, requestMock))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Asignación no encontrada con id: 999");
            verify(repository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe validar porcentaje al actualizar")
        void actualizar_validaPorcentaje() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(squadMemberMock));
            when(repository.sumPorcentajeByPersonaId(1L, 1L)).thenReturn(60);

            // When / Then
            SquadMemberRequest requestExcesivo = new SquadMemberRequest(
                    1L, 1L, Rol.BACKEND, 50, LocalDate.now(), null
            );
            assertThatThrownBy(() -> service.actualizar(1L, requestExcesivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("superando el 100%");
            verify(repository).sumPorcentajeByPersonaId(1L, 1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException cuando cambia a persona+squad duplicado")
        void actualizar_duplicaPersonaSquad_lanzaExcepcion() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(squadMemberMock));
            when(repository.existsByPersonaIdAndSquadIdAndIdNot(2L, 2L, 1L)).thenReturn(true);

            // When / Then
            SquadMemberRequest requestCambioPersonaSquad = new SquadMemberRequest(
                    2L, 2L, Rol.FRONTEND, 30, LocalDate.now(), null
            );
            assertThatThrownBy(() -> service.actualizar(1L, requestCambioPersonaSquad))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe una asignación para esta persona en este squad");
            verify(repository).existsByPersonaIdAndSquadIdAndIdNot(2L, 2L, 1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe resolver nueva persona cuando cambia personaId")
        void actualizar_cambiaPersona_resuelveEntidad() {
            // Given
            Persona nuevaPersona = new Persona();
            nuevaPersona.setId(2L);
            squadMemberMock.getPersona().setId(1L);  // persona actual
            when(repository.findById(1L)).thenReturn(Optional.of(squadMemberMock));
            when(personaRepository.findById(2L)).thenReturn(Optional.of(nuevaPersona));
            when(repository.existsByPersonaIdAndSquadIdAndIdNot(2L, 1L, 1L)).thenReturn(false);
            when(repository.sumPorcentajeByPersonaId(2L, 1L)).thenReturn(0);
            doNothing().when(mapper).updateEntity(any(), any());
            when(repository.save(any())).thenReturn(squadMemberMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            SquadMemberRequest requestCambioPersona = new SquadMemberRequest(
                    2L, 1L, Rol.BACKEND, 30, LocalDate.now(), null
            );
            service.actualizar(1L, requestCambioPersona);

            // Then
            verify(personaRepository).findById(2L);
        }

        @Test
        @DisplayName("Debe resolver nuevo squad cuando cambia squadId")
        void actualizar_cambiaSquad_resuelveEntidad() {
            // Given
            Squad nuevoSquad = new Squad();
            nuevoSquad.setId(2L);
            squadMemberMock.getSquad().setId(1L);
            when(repository.findById(1L)).thenReturn(Optional.of(squadMemberMock));
            when(squadRepository.findById(2L)).thenReturn(Optional.of(nuevoSquad));
            when(repository.existsByPersonaIdAndSquadIdAndIdNot(1L, 2L, 1L)).thenReturn(false);
            when(repository.sumPorcentajeByPersonaId(1L, 1L)).thenReturn(0);
            doNothing().when(mapper).updateEntity(any(), any());
            when(repository.save(any())).thenReturn(squadMemberMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            SquadMemberRequest requestCambioSquad = new SquadMemberRequest(
                    1L, 2L, Rol.BACKEND, 30, LocalDate.now(), null
            );
            service.actualizar(1L, requestCambioSquad);

            // Then
            verify(squadRepository).findById(2L);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando nueva persona no existe")
        void actualizar_cambiaPersonaInexistente_lanzaExcepcion() {
            // Given
            squadMemberMock.getPersona().setId(1L);  // persona actual
            when(repository.findById(1L)).thenReturn(Optional.of(squadMemberMock));
            when(personaRepository.findById(999L)).thenReturn(Optional.empty());
            when(repository.existsByPersonaIdAndSquadIdAndIdNot(999L, 1L, 1L)).thenReturn(false);
            when(repository.sumPorcentajeByPersonaId(999L, 1L)).thenReturn(0);

            // When / Then
            SquadMemberRequest requestPersonaInexistente = new SquadMemberRequest(
                    999L, 1L, Rol.BACKEND, 30, LocalDate.now(), null
            );
            assertThatThrownBy(() -> service.actualizar(1L, requestPersonaInexistente))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Persona no encontrada con id: 999");
            verify(personaRepository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando nuevo squad no existe")
        void actualizar_cambiaSquadInexistente_lanzaExcepcion() {
            // Given
            squadMemberMock.getSquad().setId(1L);  // squad actual
            when(repository.findById(1L)).thenReturn(Optional.of(squadMemberMock));
            when(squadRepository.findById(999L)).thenReturn(Optional.empty());
            when(repository.existsByPersonaIdAndSquadIdAndIdNot(1L, 999L, 1L)).thenReturn(false);
            when(repository.sumPorcentajeByPersonaId(1L, 1L)).thenReturn(0);

            // When / Then
            SquadMemberRequest requestSquadInexistente = new SquadMemberRequest(
                    1L, 999L, Rol.BACKEND, 30, LocalDate.now(), null
            );
            assertThatThrownBy(() -> service.actualizar(1L, requestSquadInexistente))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Squad no encontrado con id: 999");
            verify(squadRepository).findById(999L);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("eliminar() - Eliminar asignación")
    class EliminarTests {

        @Test
        @DisplayName("Debe eliminar asignación existente")
        void eliminar_asignacionExistente_elimina() {
            // Given
            when(repository.existsById(1L)).thenReturn(true);
            doNothing().when(repository).deleteById(1L);

            // When
            service.eliminar(1L);

            // Then
            verify(repository).existsById(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando asignación no existe")
        void eliminar_asignacionInexistente_lanzaExcepcion() {
            // Given
            when(repository.existsById(999L)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> service.eliminar(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Asignación no encontrada con id: 999");
            verify(repository).existsById(999L);
            verify(repository, never()).deleteById(any());
        }
    }
}
