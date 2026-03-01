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
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
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
import com.kaos.planificacion.entity.TareaAsignacionTimeline;
import com.kaos.planificacion.entity.TareaContinua;
import com.kaos.planificacion.repository.BloqueoRepository;
import com.kaos.planificacion.repository.SprintRepository;
import com.kaos.planificacion.repository.TareaAsignacionTimelineRepository;
import com.kaos.planificacion.repository.TareaContinuaRepository;
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
    private final TareaAsignacionTimelineRepository asignacionTimelineRepository;
    private final TareaContinuaRepository tareaContinuaRepository;

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
                        .tipo(tarea.getTipo() != null ? tarea.getTipo().toString() : null)
                        .estado(tarea.getEstado().toString())
                        .prioridad(tarea.getPrioridad().toString())
                        .bloqueada(tarea.getBloqueadores() != null && !tarea.getBloqueadores().isEmpty())
                        .origen("SPRINT")
                        .jiraIssueKey(tarea.getJiraKey())
                        .build();

                if (timeline.containsKey(tarea.getPersona().getId())) {
                    timeline.get(tarea.getPersona().getId())
                            .get(tarea.getDiaAsignado())
                            .add(tareaEnLinea);
                }
            }
        }

        // Llenar con asignaciones de tareas padre Jira (HISTORIA) en el timeline
        List<TareaAsignacionTimeline> asignaciones = asignacionTimelineRepository.findBySprintId(sprintId);
        for (TareaAsignacionTimeline asignacion : asignaciones) {
            if (asignacion.getPersona() == null) continue;
            Long personaId = asignacion.getPersona().getId();
            if (!timeline.containsKey(personaId)) continue;

            TimelineSprintResponse.TareaEnLinea tareaEnLinea = TimelineSprintResponse.TareaEnLinea.builder()
                    .tareaId(asignacion.getTarea().getId())
                    .titulo(asignacion.getTarea().getTitulo())
                    .estimacion(asignacion.getTarea().getEstimacion() != null
                            ? asignacion.getTarea().getEstimacion().doubleValue() : null)
                    .tipo(asignacion.getTarea().getTipo() != null ? asignacion.getTarea().getTipo().toString() : null)
                    .estado(asignacion.getTarea().getEstado() != null ? asignacion.getTarea().getEstado().toString() : null)
                    .prioridad(asignacion.getTarea().getPrioridad() != null ? asignacion.getTarea().getPrioridad().toString() : null)
                    .bloqueada(false)
                    .origen("JIRA_PADRE")
                    .diaInicio(asignacion.getDiaInicio())
                    .diaFin(asignacion.getDiaFin())
                    .horasPorDia(asignacion.getHorasPorDia() != null ? asignacion.getHorasPorDia().doubleValue() : null)
                    .esInformativa(asignacion.isEsInformativa())
                    .jiraIssueKey(asignacion.getTarea().getJiraKey())
                    .build();

            // Las barras multi-día se añaden al día de inicio (el frontend renderiza el span)
            timeline.get(personaId)
                    .get(asignacion.getDiaInicio())
                    .add(tareaEnLinea);
        }

        // Llenar con tareas continuas que se solapan con el sprint
        List<TareaContinua> tareasContinuas = tareaContinuaRepository.findActivasEnRango(
                sprint.getSquad().getId(), sprint.getFechaInicio(), sprint.getFechaFin());
        for (TareaContinua tc : tareasContinuas) {
            if (tc.getPersona() == null) continue;
            Long personaId = tc.getPersona().getId();
            if (!timeline.containsKey(personaId)) continue;

            // Calcular qué días del sprint (1..10) abarca la tarea continua
            int diaInicioSprint = calcularDiaEnSprint(sprint.getFechaInicio(), tc.getFechaInicio());
            int diaFinSprint = calcularDiaFinEnSprint(sprint.getFechaInicio(), sprint.getFechaFin(), tc.getFechaFin());

            TimelineSprintResponse.TareaEnLinea tareaEnLinea = TimelineSprintResponse.TareaEnLinea.builder()
                    .tareaId(tc.getId())
                    .titulo(tc.getTitulo())
                    .estimacion(null)
                    .tipo(null)
                    .estado(null)
                    .prioridad(null)
                    .bloqueada(false)
                    .origen("CONTINUA")
                    .diaInicio(diaInicioSprint)
                    .diaFin(diaFinSprint)
                    .horasPorDia(tc.getHorasPorDia() != null ? tc.getHorasPorDia().doubleValue() : null)
                    .esInformativa(tc.isEsInformativa())
                    .color(tc.getColor())
                    .build();

            timeline.get(personaId)
                    .get(diaInicioSprint)
                    .add(tareaEnLinea);
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
     * Exporta la timeline de un sprint a Excel en formato grid visual.
     * Filas = personas, columnas = días del sprint (máx 10).
     * Cada celda muestra el nombre de la tarea y las horas, con color según tipo.
     *
     * @param sprintId ID del sprint
     * @return bytes del archivo .xlsx
     */
    public byte[] exportarTimelineExcel(Long sprintId) {
        TimelineSprintResponse timeline = obtenerTimeline(sprintId);
        int totalDias = 10;
        DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("dd/MM");

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Timeline");
            sheet.setDefaultColumnWidth(18);
            sheet.setColumnWidth(0, 28 * 256); // columna Persona más ancha

            // ── Estilos ───────────────────────────────────────────────────────
            XSSFCellStyle titleStyle = crearEstiloTitulo(workbook);
            XSSFCellStyle headerStyle = crearEstiloHeader(workbook);
            XSSFCellStyle personaStyle = crearEstiloPersona(workbook);
            Map<String, XSSFCellStyle> tipoStyles = crearEstilosTipo(workbook);
            XSSFCellStyle emptyStyle = crearEstiloVacio(workbook);

            int rowIdx = 0;

            // ── Fila 0: Título del sprint ─────────────────────────────────────
            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.setHeightInPoints(28);
            var titleCell = titleRow.createCell(0);
            titleCell.setCellValue(timeline.sprintNombre() + "  ·  "
                    + timeline.fechaInicio().format(shortDate)
                    + " – " + timeline.fechaFin().format(shortDate));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalDias));

            // ── Fila 1: Cabecera de días ──────────────────────────────────────
            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(36);
            var personaHeader = headerRow.createCell(0);
            personaHeader.setCellValue("Persona");
            personaHeader.setCellStyle(headerStyle);

            for (int dia = 1; dia <= totalDias; dia++) {
                LocalDate fecha = timeline.fechaInicio().plusDays(dia - 1L);
                String label = mapDayOfWeekShort(fecha.getDayOfWeek()) + "\n" + fecha.format(shortDate);
                var cell = headerRow.createCell(dia);
                cell.setCellValue(label);
                cell.setCellStyle(headerStyle);
            }

            // ── Filas de personas ─────────────────────────────────────────────
            for (TimelineSprintResponse.PersonaEnLinea persona : timeline.personas()) {
                // Calcular cuántas filas necesita esta persona (máx tareas en un día)
                int maxTareasPorDia = 1;
                if (persona.dias() != null) {
                    for (var dia : persona.dias()) {
                        if (dia.tareas() != null && dia.tareas().size() > maxTareasPorDia) {
                            maxTareasPorDia = dia.tareas().size();
                        }
                    }
                }

                // Crear una fila Excel por cada slot de tarea
                for (int slot = 0; slot < maxTareasPorDia; slot++) {
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(40);

                    // Columna A: nombre de persona (solo en primer slot)
                    var personaCell = row.createCell(0);
                    if (slot == 0) {
                        personaCell.setCellValue(persona.personaNombre());
                    }
                    personaCell.setCellStyle(personaStyle);

                    // Columnas 1-10: tarea del día en este slot
                    for (int dia = 1; dia <= totalDias; dia++) {
                        var cell = row.createCell(dia);
                        TimelineSprintResponse.TareaEnLinea tarea = null;
                        if (persona.dias() != null) {
                            for (var diaConTareas : persona.dias()) {
                                if (diaConTareas.dia() == dia) {
                                    if (diaConTareas.tareas() != null && diaConTareas.tareas().size() > slot) {
                                        tarea = diaConTareas.tareas().get(slot);
                                    }
                                    break;
                                }
                            }
                        }
                        if (tarea != null) {
                            String horas = tarea.estimacion() != null
                                    ? String.format("%.0fh", tarea.estimacion()) : "";
                            cell.setCellValue(tarea.titulo() + (horas.isEmpty() ? "" : "  (" + horas + ")"));
                            String tipo = tarea.tipo() != null ? tarea.tipo().toUpperCase() : "";
                            cell.setCellStyle(tipoStyles.getOrDefault(tipo, tipoStyles.get("DEFAULT")));
                        } else {
                            cell.setCellStyle(emptyStyle);
                        }
                    }
                }

                // Fusionar columna A si hay más de un slot para esta persona
                if (maxTareasPorDia > 1) {
                    int startRow = rowIdx - maxTareasPorDia;
                    sheet.addMergedRegion(new CellRangeAddress(startRow, rowIdx - 1, 0, 0));
                }
            }

            // Fijar primera columna (persona) y primeras 2 filas (título + header)
            sheet.createFreezePane(1, 2);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Error generando Excel de timeline", ex);
        }
    }

    // ── Helpers de estilos Excel ──────────────────────────────────────────────

    private XSSFCellStyle crearEstiloTitulo(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)15, (byte)23, (byte)42}, null)); // slate-900
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        aplicarBorde(style);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short)14);
        font.setColor(new XSSFColor(new byte[]{(byte)248, (byte)250, (byte)252}, null)); // slate-50
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle crearEstiloHeader(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)30, (byte)41, (byte)59}, null)); // slate-800
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        aplicarBorde(style);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short)10);
        font.setColor(new XSSFColor(new byte[]{(byte)148, (byte)163, (byte)184}, null)); // slate-400
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle crearEstiloPersona(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)30, (byte)41, (byte)59}, null)); // slate-800
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(false);
        aplicarBorde(style);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short)10);
        font.setColor(new XSSFColor(new byte[]{(byte)248, (byte)250, (byte)252}, null));
        style.setFont(font);
        return style;
    }

    private Map<String, XSSFCellStyle> crearEstilosTipo(XSSFWorkbook wb) {
        Map<String, XSSFCellStyle> map = new HashMap<>();
        // HISTORIA → violet-600  #7C3AED
        map.put("HISTORIA", crearEstiloTarea(wb, new byte[]{(byte)124,(byte)58,(byte)237}));
        // TAREA → sky-600  #0284C7
        map.put("TAREA",    crearEstiloTarea(wb, new byte[]{(byte)2,  (byte)132,(byte)199}));
        // BUG → red-600  #DC2626
        map.put("BUG",      crearEstiloTarea(wb, new byte[]{(byte)220,(byte)38, (byte)38 }));
        // SPIKE → amber-600  #D97706
        map.put("SPIKE",    crearEstiloTarea(wb, new byte[]{(byte)217,(byte)119,(byte)6  }));
        // DEFAULT → slate-600  #475569
        map.put("DEFAULT",  crearEstiloTarea(wb, new byte[]{(byte)71, (byte)85, (byte)105}));
        return map;
    }

    private XSSFCellStyle crearEstiloTarea(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        aplicarBorde(style);
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short)9);
        font.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle crearEstiloVacio(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)15,(byte)23,(byte)42}, null)); // slate-900
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        aplicarBorde(style);
        return style;
    }

    private void aplicarBorde(XSSFCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    }

    private String mapDayOfWeekShort(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Lun";
            case TUESDAY -> "Mar";
            case WEDNESDAY -> "Mié";
            case THURSDAY -> "Jue";
            case FRIDAY -> "Vie";
            case SATURDAY -> "Sáb";
            case SUNDAY -> "Dom";
        };
    }

    /**
     * Calcula el día del sprint (1..10) correspondiente a una fecha.
     * Si la fecha es anterior al inicio del sprint, devuelve 1.
     * Solo cuenta días laborables (L-V).
     */
    private int calcularDiaEnSprint(LocalDate fechaInicioSprint, LocalDate fecha) {
        if (!fecha.isAfter(fechaInicioSprint)) return 1;
        int dia = 1;
        LocalDate cursor = fechaInicioSprint;
        while (cursor.isBefore(fecha) && dia < 10) {
            cursor = cursor.plusDays(1);
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                dia++;
            }
        }
        return Math.min(dia, 10);
    }

    /**
     * Calcula el día de fin del sprint (1..10) para una tarea continua.
     * Si fechaFinTarea es null (indefinida), devuelve 10.
     * Si es posterior al fin del sprint, devuelve 10.
     */
    private int calcularDiaFinEnSprint(LocalDate fechaInicioSprint, LocalDate fechaFinSprint, LocalDate fechaFinTarea) {
        if (fechaFinTarea == null || !fechaFinTarea.isBefore(fechaFinSprint)) return 10;
        return calcularDiaEnSprint(fechaInicioSprint, fechaFinTarea);
    }

}
