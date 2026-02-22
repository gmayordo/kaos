package com.kaos.planificacion.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.calendario.dto.CapacidadDiaResponse;
import com.kaos.calendario.dto.CapacidadPersonaResponse;
import com.kaos.calendario.dto.CapacidadSquadResponse;
import com.kaos.calendario.service.CapacidadService;
import com.kaos.planificacion.dto.DashboardSprintResponse;
import com.kaos.planificacion.dto.TimelineSprintResponse;
import com.kaos.planificacion.entity.EstadoTarea;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.repository.BloqueoRepository;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.planificacion.repository.TareaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de análisis y reportes para sprints.
 * Proporciona dashboard y timeline para visualización de planificación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanificacionService {

    private final SprintRepository sprintRepository;
    private final TareaRepository tareaRepository;
    private final BloqueoRepository bloqueoRepository;
    private final CapacidadService capacidadService;

    /**
     * Obtiene el dashboard de un sprint.
     * Incluye:
     * - Métricas de progreso (tareas pendientes, en progreso, completadas, bloqueadas)
     * - Ocupación de capacidad del squad (%)
     * - Alertas (bloqueos activos, sobrecarga)
     * - Tareas prioritarias
     *
     * @param sprintId ID del sprint
     * @return DashboardSprintResponse con métricas del sprint
     * @throws EntityNotFoundException si el sprint no existe
     */
    public DashboardSprintResponse obtenerDashboard(Long sprintId) {
        log.debug("Obteniendo dashboard para sprint: {}", sprintId);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado con id: " + sprintId));

        // Contar tareas por estado
        Long tareasTotal = tareaRepository.countBySprintId(sprintId);
        Long tareasPendientes = tareaRepository.countBySprintIdAndEstado(sprintId, EstadoTarea.PENDIENTE);
        Long tareasEnProgreso = tareaRepository.countBySprintIdAndEstado(sprintId, EstadoTarea.EN_PROGRESO);
        Long tareasCompletadas = tareaRepository.countBySprintIdAndEstado(sprintId, EstadoTarea.COMPLETADA);
        Long tareasBloqueadas = tareaRepository.countBySprintIdAndEstado(sprintId, EstadoTarea.BLOQUEADO);

        // Calcular capacidad
        double progresoEsperado = 50.0; // Media esperada en mitad de sprint
        double progresoReal = (tareasCompletadas.doubleValue() / tareasTotal) * 100.0;

        // Contar bloqueos activos
        Long bloqueosActivos = bloqueoRepository.countByEstadoAbiertosOEnGestion();

        // Calcular ocupación de capacidad
        CapacidadSquadResponse capacidadSquad = capacidadService.calcularCapacidad(
                sprint.getSquad().getId(),
                sprint.getFechaInicio(),
                sprint.getFechaFin());

        double horasTotales = capacidadSquad.horasTotales();
        double horasAsignadas = tareaRepository.sumEstimacionPorSprint(sprintId) != null 
            ? tareaRepository.sumEstimacionPorSprint(sprintId).doubleValue() 
            : 0.0;
        double ocupacionPorcentaje = (horasAsignadas / horasTotales) * 100.0;

        // Generar alertas
        List<String> alertas = new ArrayList<>();
        if (ocupacionPorcentaje > 90) {
            alertas.add("ALERTA: Sprint con ocupación al " + Math.round(ocupacionPorcentaje) + "%");
        }
        if (bloqueosActivos > 0) {
            alertas.add("ALERTA: " + bloqueosActivos + " bloqueo(s) activo(s)");
        }
        if (tareasBloqueadas > 0) {
            alertas.add("ALERTA: " + tareasBloqueadas + " tarea(s) bloqueada(s)");
        }
        if (progresoReal < progresoEsperado * 0.7) {
            alertas.add("ALERTA: Progreso por debajo de lo esperado");
        }

        // Construir respuesta
        return DashboardSprintResponse.builder()
                .sprintId(sprintId)
                .sprintNombre(sprint.getNombre())
                .estado(sprint.getEstado().toString())
                .tareasTotal(tareasTotal)
                .tareasPendientes(tareasPendientes)
                .tareasEnProgreso(tareasEnProgreso)
                .tareasCompletadas(tareasCompletadas)
                .tareasBloqueadas(tareasBloqueadas)
                .progresoEsperado(progresoEsperado)
                .progresoReal(progresoReal)
                .capacidadTotalHoras(horasTotales)
                .capacidadAsignadaHoras(horasAsignadas)
                .ocupacionPorcentaje(ocupacionPorcentaje)
                .bloqueosActivos(bloqueosActivos)
                .alertas(alertas)
                .fechaInicio(sprint.getFechaInicio())
                .fechaFin(sprint.getFechaFin())
                .build();
    }

    /**
     * Obtiene la timeline (grid) de un sprint.
     * Matriz [personas] x [días 1-10] con tareas asignadas.
     *
     * @param sprintId ID del sprint
     * @return TimelineSprintResponse con grid de asignaciones
     * @throws EntityNotFoundException si el sprint no existe
     */
    public TimelineSprintResponse obtenerTimeline(Long sprintId) {
        log.debug("Obteniendo timeline para sprint: {}", sprintId);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado con id: " + sprintId));

        // Obtener capacidad del squad (para tener la matriz personas x días)
        CapacidadSquadResponse capacidad = capacidadService.calcularCapacidad(
                sprint.getSquad().getId(),
                sprint.getFechaInicio(),
                sprint.getFechaFin());

        // Construir matriz: para cada persona, para cada día (1-10), listar tareas
        Map<Long, Map<Integer, List<TimelineSprintResponse.TareaEnLinea>>> timeline = new HashMap<>();

        // Inicializar estructura
        for (CapacidadPersonaResponse persona : capacidad.personas()) {
            Map<Integer, List<TimelineSprintResponse.TareaEnLinea>> diasPersona = new HashMap<>();
            for (int dia = 1; dia <= 10; dia++) {
                diasPersona.put(dia, new ArrayList<>());
            }
            timeline.put(persona.personaId(), diasPersona);
        }

        // Llenar con tareas asignadas
        var tareasPage = tareaRepository.findBySprintId(sprintId, org.springframework.data.domain.Pageable.unpaged());
        for (var tarea : tareasPage.getContent()) {
            if (tarea.getPersona() != null && tarea.getDiaAsignado() != null) {
                TimelineSprintResponse.TareaEnLinea tareaEnLinea = TimelineSprintResponse.TareaEnLinea.builder()
                        .tareaId(tarea.getId())
                        .titulo(tarea.getTitulo())
                        .estimacion(tarea.getEstimacion().doubleValue())
                        .estado(tarea.getEstado().toString())
                        .prioridad(tarea.getPrioridad().toString())
                        .bloqueada(tarea.getBloqueadores() != null && !tarea.getBloqueadores().isEmpty())
                        .build();

                timeline.get(tarea.getPersona().getId())
                        .get(tarea.getDiaAsignado())
                        .add(tareaEnLinea);
            }
        }

        // Construir lista de personas con su grid de días
        List<TimelineSprintResponse.PersonaEnLinea> personasEnLinea = timeline.entrySet().stream()
                .map(entry -> {
                    Long personaId = entry.getKey();
                    Map<Integer, List<TimelineSprintResponse.TareaEnLinea>> diasMap = entry.getValue();

                    // Buscar nombre de persona desde capacidad
                    String personaNombre = capacidad.personas().stream()
                            .filter(cp -> cp.personaId().equals(personaId))
                            .map(CapacidadPersonaResponse::personaNombre)
                            .findFirst()
                            .orElse("Persona " + personaId);

                    // Construir grid de días
                    List<TimelineSprintResponse.DiaConTareas> dias = new ArrayList<>();
                    for (int dia = 1; dia <= 10; dia++) {
                        CapacidadDiaResponse capacidadDia = capacidad.personas().stream()
                                .filter(cp -> cp.personaId().equals(personaId))
                                .flatMap(cp -> cp.detalles().stream())
                                .skip(dia - 1)
                                .findFirst()
                                .orElse(null);

                        dias.add(TimelineSprintResponse.DiaConTareas.builder()
                                .dia(dia)
                                .horasDisponibles(capacidadDia != null ? capacidadDia.horasDisponibles() : 0.0)
                                .tareas(diasMap.getOrDefault(dia, new ArrayList<>()))
                                .build());
                    }

                    return TimelineSprintResponse.PersonaEnLinea.builder()
                            .personaId(personaId)
                            .personaNombre(personaNombre)
                            .dias(dias)
                            .build();
                })
                .collect(Collectors.toList());

        return TimelineSprintResponse.builder()
                .sprintId(sprintId)
                .sprintNombre(sprint.getNombre())
                .fechaInicio(sprint.getFechaInicio())
                .fechaFin(sprint.getFechaFin())
                .personas(personasEnLinea)
                .build();
    }

    /**
     * Exporta la timeline de un sprint a Excel.
     * Columnas: Persona, DiaSemana, Fecha, Tarea.
     *
     * @param sprintId ID del sprint
     * @return bytes del archivo .xlsx
     */
    public byte[] exportarTimelineExcel(Long sprintId) {
        TimelineSprintResponse timeline = obtenerTimeline(sprintId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Timeline");
            int rowIdx = 0;

            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("Persona");
            header.createCell(1).setCellValue("DiaSemana");
            header.createCell(2).setCellValue("Fecha");
            header.createCell(3).setCellValue("Tarea");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

            for (TimelineSprintResponse.PersonaEnLinea persona : timeline.personas()) {
                if (persona.dias() == null) {
                    continue;
                }

                for (TimelineSprintResponse.DiaConTareas dia : persona.dias()) {
                    if (dia.tareas() == null || dia.tareas().isEmpty()) {
                        continue;
                    }

                    LocalDate fecha = timeline.fechaInicio().plusDays(dia.dia() - 1L);
                    String diaSemana = mapDayOfWeek(fecha.getDayOfWeek());

                    for (TimelineSprintResponse.TareaEnLinea tarea : dia.tareas()) {
                        Row row = sheet.createRow(rowIdx++);
                        row.createCell(0).setCellValue(persona.personaNombre());
                        row.createCell(1).setCellValue(diaSemana);
                        row.createCell(2).setCellValue(fecha.format(dateFormatter));
                        row.createCell(3).setCellValue(tarea.titulo());
                    }
                }
            }

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Error generando Excel de timeline", ex);
        }
    }

    private String mapDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miercoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sabado";
            case SUNDAY -> "Domingo";
        };
    }
}
