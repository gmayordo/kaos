package com.kaos.persona.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.kaos.horario.entity.PerfilHorario;
import com.kaos.horario.repository.PerfilHorarioRepository;
import com.kaos.persona.dto.PersonaRequest;
import com.kaos.persona.dto.PersonaResponse;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.entity.Rol;
import com.kaos.persona.entity.Seniority;
import com.kaos.persona.mapper.PersonaMapper;
import com.kaos.persona.repository.PersonaRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para {@link PersonaService}.
 * Valida lógica de negocio, validaciones y manejo de errores.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonaService Tests")
class PersonaServiceTest {

    @Mock
    private PersonaRepository repository;

    @Mock
    private PersonaMapper mapper;

    @Mock
    private PerfilHorarioRepository perfilHorarioRepository;

    @InjectMocks
    private PersonaService service;

    private Persona personaMock;
    private PersonaRequest requestMock;
    private PersonaResponse responseMock;
    private PerfilHorario perfilHorarioMock;

    @BeforeEach
    void setUp() {
        // PerfilHorario mock
        perfilHorarioMock = new PerfilHorario();
        perfilHorarioMock.setId(1L);
        perfilHorarioMock.setNombre("Zona Europa");

        // Persona entity mock
        personaMock = new Persona();
        personaMock.setId(1L);
        personaMock.setNombre("Juan Pérez");
        personaMock.setEmail("juan@example.com");
        personaMock.setIdJira("JIRA-001");
        personaMock.setPerfilHorario(perfilHorarioMock);
        personaMock.setSeniority(Seniority.MID);
        personaMock.setSkills("Java, Spring Boot");
        personaMock.setCosteHora(new BigDecimal("50.00"));
        personaMock.setActivo(true);
        personaMock.setFechaIncorporacion(LocalDate.now());
        personaMock.setSendNotifications(true);

        // PersonaRequest mock - 10 campos
        requestMock = new PersonaRequest(
                "Juan Pérez",
                "juan@example.com",
                "JIRA-001",
                1L,  // perfilHorarioId
                "Zaragoza",  // ciudad
                Seniority.MID,
                "Java, Spring Boot",
                new BigDecimal("50.00"),
                LocalDate.now(),
                true  // sendNotifications
        );

        // PersonaResponse mock - 15 campos
        responseMock = new PersonaResponse(
                1L,
                "Juan Pérez",
                "juan@example.com",
                "JIRA-001",
                1L,  // perfilHorarioId
                "Zona Europa",  // perfilHorarioNombre
                "Zaragoza",  // ciudad
                Seniority.MID,
                "Java, Spring Boot",
                new BigDecimal("50.00"),
                true,  // activo
                LocalDate.now(),
                true,  // sendNotifications
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("listar() - Listado con filtros")
    class ListarTests {

        @Test
        @DisplayName("Debe listar personas sin filtros")
        void listar_sinFiltros_retornaPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Persona> personaPage = new PageImpl<>(List.of(personaMock));
            when(repository.findWithFilters(null, null, null, null, null, pageable))
                    .thenReturn(personaPage);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            Page<PersonaResponse> result = service.listar(null, null, null, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(responseMock);
            verify(repository).findWithFilters(null, null, null, null, null, pageable);
            verify(mapper).toResponse(personaMock);
        }

        @Test
        @DisplayName("Debe listar personas filtrando por squad")
        void listar_conFiltroSquad_retornaPageFiltrada() {
            // Given
            Long squadId = 5L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Persona> personaPage = new PageImpl<>(List.of(personaMock));
            when(repository.findWithFilters(squadId, null, null, null, null, pageable))
                    .thenReturn(personaPage);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            Page<PersonaResponse> result = service.listar(squadId, null, null, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(repository).findWithFilters(squadId, null, null, null, null, pageable);
        }

        @Test
        @DisplayName("Debe listar personas filtrando por rol")
        void listar_conFiltroRol_retornaPageFiltrada() {
            // Given
            Rol rol = Rol.BACKEND;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Persona> personaPage = new PageImpl<>(List.of(personaMock));
            when(repository.findWithFilters(null, rol, null, null, null, pageable))
                    .thenReturn(personaPage);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            Page<PersonaResponse> result = service.listar(null, rol, null, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            verify(repository).findWithFilters(null, rol, null, null, null, pageable);
        }

        @Test
        @DisplayName("Debe listar personas filtrando por seniority")
        void listar_conFiltroSeniority_retornaPageFiltrada() {
            // Given
            Seniority seniority = Seniority.SENIOR;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Persona> personaPage = new PageImpl<>(List.of(personaMock));
            when(repository.findWithFilters(null, null, seniority, null, null, pageable))
                    .thenReturn(personaPage);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            Page<PersonaResponse> result = service.listar(null, null, seniority, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            verify(repository).findWithFilters(null, null, seniority, null, null, pageable);
        }

        @Test
        @DisplayName("Debe listar personas filtrando por ubicacion")
        void listar_conFiltroUbicacion_retornaPageFiltrada() {
            // Given
            String ubicacion = "Europa";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Persona> personaPage = new PageImpl<>(List.of(personaMock));
            when(repository.findWithFilters(null, null, null, ubicacion, null, pageable))
                    .thenReturn(personaPage);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            Page<PersonaResponse> result = service.listar(null, null, null, ubicacion, null, pageable);

            // Then
            assertThat(result).isNotNull();
            verify(repository).findWithFilters(null, null, null, ubicacion, null, pageable);
        }

        @Test
        @DisplayName("Debe listar solo personas activas")
        void listar_conFiltroActivo_retornaPageFiltrada() {
            // Given
            Boolean activo = true;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Persona> personaPage = new PageImpl<>(List.of(personaMock));
            when(repository.findWithFilters(null, null, null, null, activo, pageable))
                    .thenReturn(personaPage);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            Page<PersonaResponse> result = service.listar(null, null, null, null, activo, pageable);

            // Then
            assertThat(result).isNotNull();
            verify(repository).findWithFilters(null, null, null, null, activo, pageable);
        }

        @Test
        @DisplayName("Debe retornar página vacía cuando no hay resultados")
        void listar_sinResultados_retornaPageVacia() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Persona> emptyPage = Page.empty();
            when(repository.findWithFilters(null, null, null, null, null, pageable))
                    .thenReturn(emptyPage);

            // When
            Page<PersonaResponse> result = service.listar(null, null, null, null, null, pageable);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findWithFilters(null, null, null, null, null, pageable);
            verify(mapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("obtener() - Obtener por ID")
    class ObtenerTests {

        @Test
        @DisplayName("Debe obtener persona existente por ID")
        void obtener_conIdExistente_retornaPersona() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            PersonaResponse result = service.obtener(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(responseMock);
            verify(repository).findById(1L);
            verify(mapper).toResponse(personaMock);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando ID no existe")
        void obtener_conIdInexistente_lanzaExcepcion() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.obtener(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Persona no encontrada con id: 999");
            verify(repository).findById(999L);
            verify(mapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("crear() - Creación de persona")
    class CrearTests {

        @Test
        @DisplayName("Debe crear persona con datos válidos")
        void crear_conDatosValidos_creaPersona() {
            // Given
            when(repository.existsByEmail(requestMock.email())).thenReturn(false);
            when(repository.existsByIdJira(requestMock.idJira())).thenReturn(false);
            when(perfilHorarioRepository.findById(1L)).thenReturn(Optional.of(perfilHorarioMock));
            when(mapper.toEntity(requestMock)).thenReturn(personaMock);
            when(repository.save(any(Persona.class))).thenReturn(personaMock);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            PersonaResponse result = service.crear(requestMock);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(responseMock);
            verify(repository).existsByEmail(requestMock.email());
            verify(repository).existsByIdJira(requestMock.idJira());
            verify(perfilHorarioRepository).findById(1L);
            verify(mapper).toEntity(requestMock);
            verify(repository).save(any(Persona.class));
            verify(mapper).toResponse(personaMock);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando email ya existe")
        void crear_conEmailDuplicado_lanzaExcepcion() {
            // Given
            when(repository.existsByEmail(requestMock.email())).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.crear(requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email ya registrado");
            verify(repository).existsByEmail(requestMock.email());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando idJira ya existe")
        void crear_conIdJiraDuplicado_lanzaExcepcion() {
            // Given
            when(repository.existsByEmail(requestMock.email())).thenReturn(false);
            when(repository.existsByIdJira(requestMock.idJira())).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.crear(requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID Jira ya registrado");
            verify(repository).existsByIdJira(requestMock.idJira());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando perfil de horario no existe")
        void crear_conPerfilHorarioInexistente_lanzaExcepcion() {
            // Given
            when(repository.existsByEmail(requestMock.email())).thenReturn(false);
            when(repository.existsByIdJira(requestMock.idJira())).thenReturn(false);
            when(perfilHorarioRepository.findById(1L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.crear(requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Configure perfil de horario primero");
            verify(perfilHorarioRepository).findById(1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe crear persona con sendNotifications true por defecto cuando es null")
        void crear_conSendNotificationsNull_estableceTrue() {
            // Given
            PersonaRequest requestSinNotifications = new PersonaRequest(
                    "Test User",
                    "test@example.com",
                    "JIRA-002",
                    1L,
                    "Valencia",  // ciudad
                    Seniority.JUNIOR,
                    "Java",
                    new BigDecimal("30.00"),
                    LocalDate.now(),
                    null  // sendNotifications null
            );
            Persona persona = new Persona();
            when(repository.existsByEmail(anyString())).thenReturn(false);
            when(repository.existsByIdJira(anyString())).thenReturn(false);
            when(perfilHorarioRepository.findById(1L)).thenReturn(Optional.of(perfilHorarioMock));
            when(mapper.toEntity(any())).thenReturn(persona);
            when(repository.save(any())).thenReturn(persona);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            service.crear(requestSinNotifications);

            // Then
            verify(repository).save(argThat(p -> p.getSendNotifications().equals(true)));
        }

        @Test
        @DisplayName("Debe crear persona activa por defecto")
        void crear_estableceActivoTrue() {
            // Given
            when(repository.existsByEmail(anyString())).thenReturn(false);
            when(repository.existsByIdJira(anyString())).thenReturn(false);
            when(perfilHorarioRepository.findById(1L)).thenReturn(Optional.of(perfilHorarioMock));
            when(mapper.toEntity(requestMock)).thenReturn(personaMock);
            when(repository.save(any(Persona.class))).thenReturn(personaMock);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            service.crear(requestMock);

            // Then
            verify(repository).save(argThat(p -> p.getActivo().equals(true)));
        }

        @Test
        @DisplayName("Debe permitir crear persona sin idJira (null)")
        void crear_sinIdJira_creaPersona() {
            // Given
            PersonaRequest requestSinJira = new PersonaRequest(
                    "Test User",
                    "test@example.com",
                    null,  // idJira null
                    1L,
                    "Temuco",  // ciudad
                    Seniority.JUNIOR,
                    "Python",
                    new BigDecimal("40.00"),
                    LocalDate.now(),
                    true
            );
            when(repository.existsByEmail(anyString())).thenReturn(false);
            when(perfilHorarioRepository.findById(1L)).thenReturn(Optional.of(perfilHorarioMock));
            when(mapper.toEntity(any())).thenReturn(personaMock);
            when(repository.save(any())).thenReturn(personaMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            PersonaResponse result = service.crear(requestSinJira);

            // Then
            assertThat(result).isNotNull();
            verify(repository, never()).existsByIdJira(anyString());
            verify(repository).save(any());
        }

        @Test
        @DisplayName("Debe permitir crear persona con idJira vacío")
        void crear_conIdJiraVacio_creaPersona() {
            // Given
            PersonaRequest requestJiraVacio = new PersonaRequest(
                    "Test User",
                    "test@example.com",
                    "   ",  // idJira blank
                    1L,
                    "Zaragoza",  // ciudad
                    Seniority.JUNIOR,
                    "Go",
                    new BigDecimal("45.00"),
                    LocalDate.now(),
                    true
            );
            when(repository.existsByEmail(anyString())).thenReturn(false);
            when(perfilHorarioRepository.findById(1L)).thenReturn(Optional.of(perfilHorarioMock));
            when(mapper.toEntity(any())).thenReturn(personaMock);
            when(repository.save(any())).thenReturn(personaMock);
            when(mapper.toResponse(any())).thenReturn(responseMock);

            // When
            PersonaResponse result = service.crear(requestJiraVacio);

            // Then
            assertThat(result).isNotNull();
            verify(repository, never()).existsByIdJira(anyString());
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar() - Actualización de persona")
    class ActualizarTests {

        @Test
        @DisplayName("Debe actualizar persona con datos válidos")
        void actualizar_conDatosValidos_actualizaPersona() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsByEmailAndIdNot(requestMock.email(), 1L)).thenReturn(false);
            when(repository.existsByIdJiraAndIdNot(requestMock.idJira(), 1L)).thenReturn(false);
            when(perfilHorarioRepository.findById(1L)).thenReturn(Optional.of(perfilHorarioMock));
            doNothing().when(mapper).updateEntity(requestMock, personaMock);
            when(repository.save(personaMock)).thenReturn(personaMock);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            PersonaResponse result = service.actualizar(1L, requestMock);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(responseMock);
            verify(repository).findById(1L);
            verify(repository).existsByEmailAndIdNot(requestMock.email(), 1L);
            verify(repository).existsByIdJiraAndIdNot(requestMock.idJira(), 1L);
            verify(mapper).updateEntity(requestMock, personaMock);
            verify(repository).save(personaMock);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando ID no existe")
        void actualizar_conIdInexistente_lanzaExcepcion() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.actualizar(999L, requestMock))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Persona no encontrada con id: 999");
            verify(repository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando email ya existe en otra persona")
        void actualizar_conEmailDuplicado_lanzaExcepcion() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsByEmailAndIdNot(requestMock.email(), 1L)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.actualizar(1L, requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email ya registrado");
            verify(repository).existsByEmailAndIdNot(requestMock.email(), 1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando idJira ya existe en otra persona")
        void actualizar_conIdJiraDuplicado_lanzaExcepcion() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsByEmailAndIdNot(requestMock.email(), 1L)).thenReturn(false);
            when(repository.existsByIdJiraAndIdNot(requestMock.idJira(), 1L)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.actualizar(1L, requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID Jira ya registrado");
            verify(repository).existsByIdJiraAndIdNot(requestMock.idJira(), 1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando perfil de horario no existe")
        void actualizar_conPerfilHorarioInexistente_lanzaExcepcion() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsByEmailAndIdNot(requestMock.email(), 1L)).thenReturn(false);
            when(repository.existsByIdJiraAndIdNot(requestMock.idJira(), 1L)).thenReturn(false);
            when(perfilHorarioRepository.findById(1L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.actualizar(1L, requestMock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Configure perfil de horario primero");
            verify(perfilHorarioRepository).findById(1L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe permitir actualizar sin cambiar email (mismo email)")
        void actualizar_mantieneEmail_noValidaDuplicado() {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.existsByEmailAndIdNot(requestMock.email(), 1L)).thenReturn(false);
            when(repository.existsByIdJiraAndIdNot(requestMock.idJira(), 1L)).thenReturn(false);
            when(perfilHorarioRepository.findById(1L)).thenReturn(Optional.of(perfilHorarioMock));
            doNothing().when(mapper).updateEntity(requestMock, personaMock);
            when(repository.save(personaMock)).thenReturn(personaMock);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            PersonaResponse result = service.actualizar(1L, requestMock);

            // Then
            assertThat(result).isNotNull();
            verify(repository).existsByEmailAndIdNot(requestMock.email(), 1L);
        }
    }

    @Nested
    @DisplayName("desactivar() - Desactivación de persona")
    class DesactivarTests {

        @Test
        @DisplayName("Debe desactivar persona existente")
        void desactivar_conIdExistente_desactivaPersona() {
            // Given
            personaMock.setActivo(true);
            when(repository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.save(personaMock)).thenReturn(personaMock);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            PersonaResponse result = service.desactivar(1L);

            // Then
            assertThat(result).isNotNull();
            verify(repository).findById(1L);
            verify(repository).save(argThat(p -> p.getActivo().equals(false)));
            verify(mapper).toResponse(personaMock);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException cuando ID no existe")
        void desactivar_conIdInexistente_lanzaExcepcion() {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.desactivar(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Persona no encontrada con id: 999");
            verify(repository).findById(999L);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Debe desactivar persona ya desactivada sin error")
        void desactivar_personaYaDesactivada_ejecutaSinError() {
            // Given
            personaMock.setActivo(false);
            when(repository.findById(1L)).thenReturn(Optional.of(personaMock));
            when(repository.save(personaMock)).thenReturn(personaMock);
            when(mapper.toResponse(personaMock)).thenReturn(responseMock);

            // When
            PersonaResponse result = service.desactivar(1L);

            // Then
            assertThat(result).isNotNull();
            verify(repository).save(argThat(p -> p.getActivo().equals(false)));
        }
    }
}
