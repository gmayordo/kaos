package com.kaos.planificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.kaos.persona.repository.PersonaRepository;
import com.kaos.planificacion.dto.BloqueoRequest;
import com.kaos.planificacion.dto.BloqueoResponse;
import com.kaos.planificacion.entity.Bloqueo;
import com.kaos.planificacion.entity.EstadoBloqueo;
import com.kaos.planificacion.mapper.BloqueoMapper;
import com.kaos.planificacion.repository.BloqueoRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para BloqueoService.
 * Cubre CA-20, CA-21, CA-22.
 *
 * BloqueoRequest(titulo, descripcion, tipo, estado, responsableId, notas)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BloqueoService")
class BloqueoServiceTest {

    @Mock
    private BloqueoRepository bloqueoRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private BloqueoMapper bloqueoMapper;

    @InjectMocks
    private BloqueoService bloqueoService;

    private Bloqueo bloqueo;
    private BloqueoResponse bloqueoResponse;

    @BeforeEach
    void setUp() {
        bloqueo = new Bloqueo();
        bloqueo.setId(1L);
        bloqueo.setTitulo("Bloqueo técnico");
        bloqueo.setEstado(EstadoBloqueo.ABIERTO);
        bloqueo.setCreatedAt(LocalDateTime.now());

        bloqueoResponse = mock(BloqueoResponse.class);
        when(bloqueoResponse.id()).thenReturn(1L);
        when(bloqueoResponse.estado()).thenReturn("ABIERTO");
    }

    @Nested
    @DisplayName("obtener()")
    class ObtenerTests {

        @Test
        @DisplayName("CA-20: Bloqueo existente retorna BloqueoResponse")
        void testObtenerExistente() {
            when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));
            when(bloqueoMapper.toResponse(bloqueo)).thenReturn(bloqueoResponse);

            BloqueoResponse result = bloqueoService.obtener(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("CA-20: Bloqueo inexistente lanza EntityNotFoundException")
        void testObtenerNoExistente() {
            when(bloqueoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bloqueoService.obtener(99L))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("crear()")
    class CrearTests {

        @Test
        @DisplayName("CA-20: Crear bloqueo válido → estado ABIERTO")
        void testCrearValido() {
            BloqueoRequest request = new BloqueoRequest(
                "Bloqueo A", "Descripción", "TECNICO", null, null, "Sin notas"
            );
            when(bloqueoMapper.toEntity(request)).thenReturn(bloqueo);
            when(bloqueoRepository.save(any())).thenReturn(bloqueo);
            when(bloqueoMapper.toResponse(bloqueo)).thenReturn(bloqueoResponse);

            BloqueoResponse result = bloqueoService.crear(request);

            assertThat(result).isNotNull();
            assertThat(result.estado()).isEqualTo("ABIERTO");
            verify(bloqueoRepository).save(any());
        }

        @Test
        @DisplayName("CA-20: Crear bloqueo con responsable inválido lanza EntityNotFoundException")
        void testCrearResponsableInvalido() {
            BloqueoRequest request = new BloqueoRequest(
                "Bloqueo B", "Desc", "TECNICO", null, 99L, "Notas"
            );
            when(personaRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> bloqueoService.crear(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Responsable");
            verify(bloqueoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("actualizar()")
    class ActualizarTests {

        @Test
        @DisplayName("CA-20: Actualizar bloqueo válido persiste cambios")
        void testActualizarValido() {
            BloqueoRequest request = new BloqueoRequest("Bloqueo Actualizado", "Descripción", "TECNICO", 
                "EN_GESTION", 1L, "Notas");
            when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));
            when(personaRepository.existsById(1L)).thenReturn(true);
            when(bloqueoRepository.save(any())).thenReturn(bloqueo);
            when(bloqueoMapper.toResponse(bloqueo)).thenReturn(bloqueoResponse);

            BloqueoResponse result = bloqueoService.actualizar(1L, request);

            assertThat(result).isNotNull();
            verify(bloqueoRepository).save(any());
        }

        @Test
        @DisplayName("CA-20: Actualizar bloqueo inexistente lanza EntityNotFoundException")
        void testActualizarInexistente() {
            BloqueoRequest request = new BloqueoRequest("Bloqueo X", "Descripción", "TECNICO", 
                "ABIERTO", 1L, "Notas");
            when(bloqueoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bloqueoService.actualizar(99L, request))
                .isInstanceOf(EntityNotFoundException.class);
            verify(bloqueoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cambiarEstado()")
    class CambiarEstadoTests {

        @Test
        @DisplayName("CA-21: ABIERTO → EN_GESTION es válido")
        void testAbiertaAEnGestion() {
            bloqueo.setEstado(EstadoBloqueo.ABIERTO);
            when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));
            when(bloqueoRepository.save(any())).thenReturn(bloqueo);
            when(bloqueoMapper.toResponse(bloqueo)).thenReturn(bloqueoResponse);

            BloqueoResponse result = bloqueoService.cambiarEstado(1L, EstadoBloqueo.EN_GESTION);

            assertThat(result).isNotNull();
            verify(bloqueoRepository).save(any());
        }

        @Test
        @DisplayName("CA-21: EN_GESTION → RESUELTO setea fechaResolucion")
        void testEnGestionAResuelto() {
            bloqueo.setEstado(EstadoBloqueo.EN_GESTION);
            when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));
            when(bloqueoRepository.save(any())).thenReturn(bloqueo);
            when(bloqueoMapper.toResponse(bloqueo)).thenReturn(bloqueoResponse);

            bloqueoService.cambiarEstado(1L, EstadoBloqueo.RESUELTO);

            assertThat(bloqueo.getFechaResolucion()).isNotNull();
        }

        @Test
        @DisplayName("CA-21: ABIERTO → RESUELTO directamente es válido")
        void testAbiertoAResueltoDirecto() {
            bloqueo.setEstado(EstadoBloqueo.ABIERTO);
            when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));
            when(bloqueoRepository.save(any())).thenReturn(bloqueo);
            when(bloqueoMapper.toResponse(bloqueo)).thenReturn(bloqueoResponse);

            bloqueoService.cambiarEstado(1L, EstadoBloqueo.RESUELTO);

            assertThat(bloqueo.getFechaResolucion()).isNotNull();
        }

        @Test
        @DisplayName("CA-21: EN_GESTION → ABIERTO (retroceder) lanza IllegalStateException")
        void testTransicionInvalida() {
            bloqueo.setEstado(EstadoBloqueo.EN_GESTION);
            when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));

            assertThatThrownBy(() -> bloqueoService.cambiarEstado(1L, EstadoBloqueo.ABIERTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transición inválida");
            verify(bloqueoRepository, never()).save(any());
        }

        @Test
        @DisplayName("CA-21: RESUELTO → EN_GESTION (reapertura) lanza IllegalStateException")
        void testTransicionDesdeResuelto() {
            bloqueo.setEstado(EstadoBloqueo.RESUELTO);
            when(bloqueoRepository.findById(1L)).thenReturn(Optional.of(bloqueo));

            assertThatThrownBy(() -> bloqueoService.cambiarEstado(1L, EstadoBloqueo.EN_GESTION))
                .isInstanceOf(IllegalStateException.class);
            verify(bloqueoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listar()")
    class ListarTests {

        @Test
        @DisplayName("CA-22: Listar por estado retorna page")
        void testListarPorEstado() {
            var pageable = PageRequest.of(0, 10);
            when(bloqueoRepository.findByEstado(EstadoBloqueo.ABIERTO, pageable))
                .thenReturn(new PageImpl<>(List.of(bloqueo)));
            when(bloqueoMapper.toResponse(bloqueo)).thenReturn(bloqueoResponse);

            var result = bloqueoService.listar(EstadoBloqueo.ABIERTO, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("CA-22: Listar sin filtro retorna todos")
        void testListarSinFiltro() {
            var pageable = PageRequest.of(0, 10);
            when(bloqueoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(bloqueo)));
            when(bloqueoMapper.toResponse(bloqueo)).thenReturn(bloqueoResponse);

            var result = bloqueoService.listar(null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("contarActivos()")
    class ContarActivosTests {

        @Test
        @DisplayName("CA-22: Retorna conteo de ABIERTO + EN_GESTION")
        void testContarActivos() {
            when(bloqueoRepository.countByEstadoAbiertosOEnGestion()).thenReturn(3L);

            Long result = bloqueoService.contarActivos();

            assertThat(result).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("eliminar()")
    class EliminarTests {

        @Test
        @DisplayName("CA-22: Eliminar bloqueo existente sin restricciones")
        void testEliminarExistente() {
            when(bloqueoRepository.existsById(1L)).thenReturn(true);

            bloqueoService.eliminar(1L);

            verify(bloqueoRepository).deleteById(1L);
        }

        @Test
        @DisplayName("CA-22: Eliminar bloqueo inexistente lanza EntityNotFoundException")
        void testEliminarInexistente() {
            when(bloqueoRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> bloqueoService.eliminar(99L))
                .isInstanceOf(EntityNotFoundException.class);
            verify(bloqueoRepository, never()).deleteById(anyLong());
        }
    }
}
