package com.kaos.planificacion.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.planificacion.dto.DependenciaDTOResponse;
import com.kaos.planificacion.entity.TareaDependencia;
import com.kaos.planificacion.entity.TipoDependencia;
import com.kaos.planificacion.exception.DependenciaCiclicaException;
import com.kaos.planificacion.exception.DependenciaDuplicadaException;
import com.kaos.planificacion.repository.TareaDependenciaRepository;
import com.kaos.planificacion.repository.TareaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para gestión de dependencias entre tareas.
 *
 * <p>Antes de persistir cualquier dependencia se ejecuta una detección de ciclos
 * mediante BFS sobre el grafo de dependencias existente para garantizar que
 * el grafo permanece acíclico.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TareaDependenciaService {

    private final TareaDependenciaRepository dependenciaRepository;
    private final TareaRepository tareaRepository;

    // ── Consulta ─────────────────────────────────────────────────────────────

    /**
     * Lista todas las dependencias en las que participa una tarea
     * (tanto las que salen de ella como las que llegan a ella).
     *
     * @param tareaId ID de la tarea
     * @return Lista combinada de dependencias (salientes + entrantes)
     */
    public List<DependenciaDTOResponse> listarDependencias(Long tareaId) {
        log.debug("Listando dependencias de tarea {}", tareaId);

        List<TareaDependencia> salientes  = dependenciaRepository.findByTareaOrigenId(tareaId);
        List<TareaDependencia> entrantes  = dependenciaRepository.findByTareaDestinoId(tareaId);

        List<DependenciaDTOResponse> resultado = new ArrayList<>();
        salientes.forEach(d -> resultado.add(toDTO(d)));
        entrantes.forEach(d -> resultado.add(toDTO(d)));

        return resultado;
    }

    // ── Crear ────────────────────────────────────────────────────────────────

    /**
     * Crea una nueva dependencia entre dos tareas, previa validación de:
     * <ol>
     *   <li>Ambas tareas existen</li>
     *   <li>No son la misma tarea</li>
     *   <li>La dependencia no existe ya (evitar duplicados)</li>
     *   <li>No introduce un ciclo en el grafo de dependencias (BFS)</li>
     * </ol>
     *
     * @param origenId  ID de la tarea bloqueante
     * @param destinoId ID de la tarea bloqueada
     * @param tipo      Tipo de dependencia (ESTRICTA / SUAVE)
     * @return DependenciaDTOResponse de la dependencia creada
     * @throws EntityNotFoundException       si alguna tarea no existe
     * @throws IllegalArgumentException      si origen == destino
     * @throws DependenciaDuplicadaException si la dependencia ya existe
     * @throws DependenciaCiclicaException   si introduciría un ciclo
     */
    @Transactional
    public DependenciaDTOResponse crearDependencia(Long origenId, Long destinoId, TipoDependencia tipo) {
        log.info("Creando dependencia {} → {} tipo {}", origenId, destinoId, tipo);

        if (origenId.equals(destinoId)) {
            throw new IllegalArgumentException("Una tarea no puede depender de sí misma");
        }

        var tareaOrigen  = tareaRepository.findById(origenId)
                .orElseThrow(() -> new EntityNotFoundException("Tarea origen no encontrada: " + origenId));
        var tareaDestino = tareaRepository.findById(destinoId)
                .orElseThrow(() -> new EntityNotFoundException("Tarea destino no encontrada: " + destinoId));

        if (dependenciaRepository.existsByTareaOrigenIdAndTareaDestinoId(origenId, destinoId)) {
            throw new DependenciaDuplicadaException(origenId, destinoId);
        }

        if (detectarCiclo(origenId, destinoId)) {
            throw new DependenciaCiclicaException(origenId, destinoId);
        }

        TareaDependencia dependencia = new TareaDependencia();
        dependencia.setTareaOrigen(tareaOrigen);
        dependencia.setTareaDestino(tareaDestino);
        dependencia.setTipo(tipo);

        TareaDependencia saved = dependenciaRepository.save(dependencia);
        log.info("Dependencia creada con id {}", saved.getId());

        return toDTO(saved);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    /**
     * Elimina una dependencia por su ID.
     *
     * @param dependenciaId ID de la dependencia a eliminar
     * @throws EntityNotFoundException si no existe
     */
    @Transactional
    public void eliminarDependencia(Long dependenciaId) {
        log.info("Eliminando dependencia {}", dependenciaId);
        if (!dependenciaRepository.existsById(dependenciaId)) {
            throw new EntityNotFoundException("Dependencia no encontrada con id: " + dependenciaId);
        }
        dependenciaRepository.deleteById(dependenciaId);
    }

    // ── BFS — Detección de ciclos ─────────────────────────────────────────────

    /**
     * Detecta si añadir la dependencia {@code origenId → destinoId} crearía un ciclo.
     *
     * <p>Algoritmo: BFS desde {@code destinoId} siguiendo las aristas salientes.
     * Si durante el recorrido alcanzamos {@code origenId}, significa que ya existe
     * un camino {@code destino → ... → origen}, y por tanto crear {@code origen → destino}
     * cerraría el ciclo.</p>
     *
     * @param origenId  ID del nodo origen de la nueva arista
     * @param destinoId ID del nodo destino de la nueva arista
     * @return {@code true} si se detectaría un ciclo
     */
    private boolean detectarCiclo(Long origenId, Long destinoId) {
        Set<Long> visitados = new HashSet<>();
        Queue<Long> cola    = new LinkedList<>();
        cola.add(destinoId);

        while (!cola.isEmpty()) {
            Long actual = cola.poll();

            if (actual.equals(origenId)) {
                return true; // ciclo detectado
            }

            if (!visitados.add(actual)) {
                continue; // ya visitado en esta iteración
            }

            dependenciaRepository.findByTareaOrigenId(actual).forEach(dep -> {
                Long siguiente = dep.getTareaDestino().getId();
                if (!visitados.contains(siguiente)) {
                    cola.add(siguiente);
                }
            });
        }

        return false;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private DependenciaDTOResponse toDTO(TareaDependencia d) {
        return new DependenciaDTOResponse(
                d.getId(),
                d.getTareaOrigen().getId(),
                d.getTareaOrigen().getTitulo(),
                d.getTareaDestino().getId(),
                d.getTareaDestino().getTitulo(),
                d.getTipo()
        );
    }
}
