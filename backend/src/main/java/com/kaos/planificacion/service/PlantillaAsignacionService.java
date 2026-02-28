package com.kaos.planificacion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.dto.PlanificarAsignacionItem;
import com.kaos.planificacion.dto.PlantillaAsignacionRequest;
import com.kaos.planificacion.dto.PlantillaAsignacionResponse;
import com.kaos.planificacion.entity.PlantillaAsignacion;
import com.kaos.planificacion.entity.PlantillaAsignacionLinea;
import com.kaos.planificacion.entity.RolPlantilla;
import com.kaos.planificacion.repository.PlantillaAsignacionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio CRUD para {@link PlantillaAsignacion} y lógica de aplicación automática.
 *
 * <p>El método {@link #aplicar} toma un tipo Jira y una estimación total y devuelve
 * una lista de {@link PlanificarAsignacionItem} pre-rellenados con las horas
 * correspondientes a cada rol según los porcentajes de la plantilla activa.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlantillaAsignacionService {

    private final PlantillaAsignacionRepository repository;

    // ── Consulta ─────────────────────────────────────────────────────────────

    public List<PlantillaAsignacionResponse> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public PlantillaAsignacionResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    // ── Crear / Actualizar / Eliminar ─────────────────────────────────────────

    @Transactional
    public PlantillaAsignacionResponse crear(PlantillaAsignacionRequest request) {
        log.info("Creando plantilla: {}", request.nombre());
        PlantillaAsignacion plantilla = buildFromRequest(new PlantillaAsignacion(), request);
        return toResponse(repository.save(plantilla));
    }

    @Transactional
    public PlantillaAsignacionResponse actualizar(Long id, PlantillaAsignacionRequest request) {
        log.info("Actualizando plantilla {}", id);
        PlantillaAsignacion plantilla = findById(id);
        plantilla.getLineas().clear();
        buildFromRequest(plantilla, request);
        return toResponse(repository.save(plantilla));
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando plantilla {}", id);
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Plantilla no encontrada con id: " + id);
        }
        repository.deleteById(id);
    }

    // ── Aplicar plantilla ────────────────────────────────────────────────────

    /**
     * Busca la plantilla activa para {@code tipoJira} y genera una lista de
     * {@link PlanificarAsignacionItem} con las horas distribuidas por porcentaje.
     *
     * <p>Si no existe plantilla activa para el tipo dado, devuelve lista vacía
     * (el caller decide si lanzar excepción o dejar que el usuario asigne manualmente).</p>
     *
     * @param tipoJira         Tipo Jira del issue (Story, Bug, Task…)
     * @param estimacionTotal  Estimación total en horas
     * @return Lista de ítems pre-rellenados con horas por rol (sin personaId ni diaAsignado)
     */
    public List<PlanificarAsignacionItem> aplicar(String tipoJira, BigDecimal estimacionTotal) {
        log.debug("Aplicando plantilla para tipoJira={} estimacion={}", tipoJira, estimacionTotal);

        return repository.findFirstByTipoJiraIgnoreCaseAndActivoTrue(tipoJira)
                .map(plantilla -> plantilla.getLineas().stream()
                        .map(linea -> {
                            BigDecimal horas = estimacionTotal
                                    .multiply(BigDecimal.valueOf(linea.getPorcentajeHoras()))
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            return new PlanificarAsignacionItem(
                                    null,          // jiraKey — el caller lo rellena
                                    null,          // personaId — el usuario elige
                                    horas,
                                    null,          // diaAsignado — el usuario elige
                                    null,          // tipo — derivado del issue
                                    null,          // categoria
                                    null           // prioridad
                            );
                        })
                        .toList())
                .orElse(List.of());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PlantillaAsignacion findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plantilla no encontrada con id: " + id));
    }

    private PlantillaAsignacion buildFromRequest(PlantillaAsignacion plantilla, PlantillaAsignacionRequest req) {
        plantilla.setNombre(req.nombre());
        plantilla.setTipoJira(req.tipoJira());
        plantilla.setActivo(req.activo() != null ? req.activo() : true);

        req.lineas().forEach(lineaReq -> {
            PlantillaAsignacionLinea linea = new PlantillaAsignacionLinea();
            linea.setPlantilla(plantilla);
            linea.setRol(RolPlantilla.valueOf(lineaReq.rol().toUpperCase()));
            linea.setPorcentajeHoras(lineaReq.porcentajeHoras());
            linea.setOrden(lineaReq.orden());
            linea.setDependeDeOrden(lineaReq.dependeDeOrden());
            plantilla.getLineas().add(linea);
        });

        return plantilla;
    }

    private PlantillaAsignacionResponse toResponse(PlantillaAsignacion p) {
        List<PlantillaAsignacionResponse.LineaResponse> lineas = p.getLineas().stream()
                .map(l -> new PlantillaAsignacionResponse.LineaResponse(
                        l.getId(),
                        l.getRol().name(),
                        l.getPorcentajeHoras(),
                        l.getOrden(),
                        l.getDependeDeOrden()))
                .toList();
        return new PlantillaAsignacionResponse(p.getId(), p.getNombre(), p.getTipoJira(), p.getActivo(), lineas);
    }
}
