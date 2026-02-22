package com.kaos.calendario.service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.kaos.calendario.dto.ExcelAnalysisResponse;
import com.kaos.calendario.dto.ExcelImportResponse;
import com.kaos.calendario.entity.Ausencia;
import com.kaos.calendario.entity.EstadoVacacion;
import com.kaos.calendario.entity.TipoAusencia;
import com.kaos.calendario.entity.TipoVacacion;
import com.kaos.calendario.entity.Vacacion;
import com.kaos.calendario.repository.AusenciaRepository;
import com.kaos.calendario.repository.VacacionRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de importación masiva desde ficheros Excel de vacaciones.
 *
 * <p>Soporta dos formatos:</p>
 * <ul>
 *   <li><b>España</b>: año fiscal empieza en ENERO. Columna 1 = nombre,
 *       columnas 2–5 = totales (ignoradas), columnas 6+ = calendario.</li>
 *   <li><b>Chile</b>: año fiscal empieza en un mes distinto (normalmente ABRIL).
 *       Columna 1 = nombre, columna 2 = localización, columna 3 = equipo,
 *       columnas 4–10 = totales (ignoradas), columnas 11+ = calendario.</li>
 * </ul>
 *
 * <p>Códigos de tipo importados:</p>
 * <ul>
 *   <li>V → VACACIONES</li>
 *   <li>LD → LIBRE_DISPOSICION</li>
 *   <li>AP → ASUNTOS_PROPIOS</li>
 *   <li>LC → PERMISO</li>
 *   <li>B → Ausencia BAJA_MEDICA</li>
 *   <li>O → Ausencia OTRO (solo Chile)</li>
 *   <li>Resto (C, T, Z, M, VAL, COR, COR, etc.) → ignorados</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    // ──────────────────────────────────────────────────────────────────────
    // Constantes
    // ──────────────────────────────────────────────────────────────────────

    private static final Map<String, Integer> MONTH_MAP = Map.ofEntries(
            Map.entry("ENERO", 1),       Map.entry("FEBRERO", 2),
            Map.entry("MARZO", 3),       Map.entry("ABRIL", 4),
            Map.entry("MAYO", 5),        Map.entry("JUNIO", 6),
            Map.entry("JULIO", 7),       Map.entry("AGOSTO", 8),
            Map.entry("SEPTIEMBRE", 9),  Map.entry("OCTUBRE", 10),
            Map.entry("NOVIEMBRE", 11),  Map.entry("DICIEMBRE", 12)
    );

    /** Códigos de Excel → TipoVacacion */
    private static final Map<String, TipoVacacion> VACACION_CODES = Map.of(
            "V",  TipoVacacion.VACACIONES,
            "LD", TipoVacacion.LIBRE_DISPOSICION,
            "AP", TipoVacacion.ASUNTOS_PROPIOS,
            "LC", TipoVacacion.PERMISO
    );

    /** Códigos de Excel → TipoAusencia */
    private static final Map<String, TipoAusencia> AUSENCIA_CODES = Map.of(
            "B", TipoAusencia.BAJA_MEDICA,
            "O", TipoAusencia.OTRO
    );

    /**
     * Máxima brecha en días entre dos celdas del mismo tipo para considerarlos
     * el mismo período (permite saltar fines de semana de hasta 3 días, e.g.
     * viernes → lunes = 3 días de diferencia).
     */
    private static final long MAX_GAP_DAYS = 3;

    // ──────────────────────────────────────────────────────────────────────
    // Dependencias
    // ──────────────────────────────────────────────────────────────────────

    private final PersonaRepository personaRepository;
    private final VacacionRepository vacacionRepository;
    private final AusenciaRepository ausenciaRepository;

    // ──────────────────────────────────────────────────────────────────────
    // Punto de entrada
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Analiza un fichero Excel sin guardar nada en BD (dry-run).
     * Devuelve qué nombres se han auto-resuelto y cuáles no.
     *
     * @param file fichero .xlsx
     * @param año  año fiscal
     */
    public ExcelAnalysisResponse analizarExcel(MultipartFile file, int año) throws IOException {
        log.info("Analizando Excel (dry-run): {}, año fiscal {}", file.getOriginalFilename(), año);

        // LinkedHashSet para mantener orden de aparición y evitar duplicados
        Set<String> nombresEncontrados = new LinkedHashSet<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                Sheet sheet = workbook.getSheetAt(si);
                int monthRowIdx = findMonthRow(sheet);
                if (monthRowIdx < 0) continue;

                for (int r = monthRowIdx + 2; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    String nombre = getStringValue(row.getCell(1));
                    if (nombre == null || nombre.isBlank()) continue;
                    Cell col2Cell = row.getCell(2);
                    if (col2Cell == null || col2Cell.getCellType() == CellType.BLANK) continue;
                    String col2 = getStringValue(col2Cell);
                    if (col2 != null && (col2.equalsIgnoreCase("Dias rest. FY25")
                            || col2.equalsIgnoreCase("Localización")
                            || col2.equalsIgnoreCase("Coordinación"))) {
                        continue;
                    }
                    nombresEncontrados.add(nombre.trim());
                }
            }
        }

        List<ExcelAnalysisResponse.PersonaMatch> resueltas = new ArrayList<>();
        List<String> noResueltas = new ArrayList<>();

        for (String nombre : nombresEncontrados) {
            Optional<Persona> personaOpt = resolvePersona(nombre, Collections.emptyMap());
            if (personaOpt.isPresent()) {
                Persona p = personaOpt.get();
                resueltas.add(new ExcelAnalysisResponse.PersonaMatch(nombre, p.getId(), p.getNombre()));
            } else {
                noResueltas.add(nombre);
            }
        }

        log.info("Análisis completado: {} resueltas, {} no resueltas", resueltas.size(), noResueltas.size());
        return new ExcelAnalysisResponse(nombresEncontrados.size(), resueltas, noResueltas);
    }

    /**
     * Importa vacaciones/ausencias desde un fichero Excel.
     *
     * @param file     fichero .xlsx subido por el usuario
     * @param año      año fiscal (e.g. 2026 para España FY26; 2025 para Chile FY2025)
     * @param mappings mapeo manual nombre-excel → personaId para nombres no auto-resueltos.
     *                 Puede ser null o vacío si no hay mapeos manuales.
     */
    @Transactional
    public ExcelImportResponse importarExcel(MultipartFile file, int año,
                                              Map<String, Long> mappings) throws IOException {
        Map<String, Long> effectiveMappings = (mappings != null) ? mappings : Collections.emptyMap();
        log.info("Iniciando importación Excel: {}, año fiscal {}, {} mapeos manuales",
                file.getOriginalFilename(), año, effectiveMappings.size());

        int personasProcesadas = 0;
        int vacacionesCreadas = 0;
        int ausenciasCreadas = 0;
        List<String> personasNoEncontradas = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                Sheet sheet = workbook.getSheetAt(si);
                String sheetName = sheet.getSheetName();
                log.info("Procesando hoja: {}", sheetName);

                // 1. Buscar la fila con los nombres de mes
                int monthRowIdx = findMonthRow(sheet);
                if (monthRowIdx < 0) {
                    log.warn("Hoja '{}' no tiene fila de meses — se omite", sheetName);
                    continue;
                }

                // 2. Construir mapa columna → LocalDate
                Map<Integer, LocalDate> colDateMap = buildColumnDateMap(sheet, monthRowIdx, año);
                if (colDateMap.isEmpty()) {
                    log.warn("Hoja '{}' no generó mapa de fechas — se omite", sheetName);
                    continue;
                }

                // 3. Detectar la primera columna de datos de calendario
                int firstCalendarCol = colDateMap.keySet().stream().mapToInt(i -> i).min().orElse(6);

                // 4. Procesar filas de personas (a partir de monthRowIdx + 2)
                for (int r = monthRowIdx + 2; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    String nombre = getStringValue(row.getCell(1));
                    if (nombre == null || nombre.isBlank()) continue;

                    // Distinguir fila de persona de fila de sección/cabecera:
                    // col[2] debe ser numérica o un nombre de ciudad (no nulo)
                    Cell col2Cell = row.getCell(2);
                    if (col2Cell == null || col2Cell.getCellType() == CellType.BLANK) continue;
                    // Ignorar fila si col[2] es texto genérico de cabecera
                    String col2 = getStringValue(col2Cell);
                    if (col2 != null && (col2.equalsIgnoreCase("Dias rest. FY25")
                            || col2.equalsIgnoreCase("Localización")
                            || col2.equalsIgnoreCase("Coordinación"))) {
                        continue;
                    }

                    // Buscar persona en BD (auto-resolución + mapeo manual)
                    Optional<Persona> personaOpt = resolvePersona(nombre, effectiveMappings);
                    if (personaOpt.isEmpty()) {
                        if (!personasNoEncontradas.contains(nombre)) {
                            personasNoEncontradas.add(nombre);
                        }
                        continue;
                    }
                    Persona persona = personaOpt.get();

                    // Recopilar (fecha → código)
                    TreeMap<LocalDate, String> dayCodes = new TreeMap<>();
                    for (Map.Entry<Integer, LocalDate> entry : colDateMap.entrySet()) {
                        int col = entry.getKey();
                        if (col < firstCalendarCol) continue;
                        Cell cell = row.getCell(col);
                        String code = getStringValue(cell);
                        if (code != null && !code.isBlank()) {
                            // Normalizar código (quitar variantes como "C-1h")
                            String normalizedCode = normalizeCode(code);
                            if (normalizedCode != null) {
                                dayCodes.put(entry.getValue(), normalizedCode);
                            }
                        }
                    }

                    if (dayCodes.isEmpty()) {
                        personasProcesadas++;
                        continue;
                    }

                    // Agrupar días consecutivos del mismo tipo en rangos
                    List<DayRange> ranges = groupConsecutiveDays(dayCodes);

                    for (DayRange range : ranges) {
                        try {
                            TipoVacacion tipoVac = VACACION_CODES.get(range.code());
                            TipoAusencia tipoAus = AUSENCIA_CODES.get(range.code());

                            if (tipoVac != null) {
                                // Verificar que no existe solapamiento
                                boolean solapa = vacacionRepository.existsSolapamiento(
                                        persona.getId(), null, range.inicio(), range.fin());
                                if (solapa) {
                                    log.warn("Solapamiento para {} [{} - {}] — se omite",
                                            nombre, range.inicio(), range.fin());
                                    continue;
                                }
                                Vacacion v = Vacacion.builder()
                                        .persona(persona)
                                        .fechaInicio(range.inicio())
                                        .fechaFin(range.fin())
                                        .diasLaborables(calcularDiasLaborables(range.inicio(), range.fin()))
                                        .tipo(tipoVac)
                                        .estado(EstadoVacacion.REGISTRADA)
                                        .build();
                                vacacionRepository.save(v);
                                vacacionesCreadas++;

                            } else if (tipoAus != null) {
                                boolean solapaAus = ausenciaRepository.existsSolapamiento(
                                        persona.getId(), null, range.inicio(), range.fin());
                                if (solapaAus) {
                                    log.warn("Ausencia duplicada para {} [{} - {}] — se omite",
                                            nombre, range.inicio(), range.fin());
                                    continue;
                                }
                                Ausencia a = Ausencia.builder()
                                        .persona(persona)
                                        .fechaInicio(range.inicio())
                                        .fechaFin(range.fin())
                                        .tipo(tipoAus)
                                        .comentario("Importado desde Excel")
                                        .build();
                                ausenciaRepository.save(a);
                                ausenciasCreadas++;
                            }
                        } catch (Exception ex) {
                            String msg = String.format("%s [%s – %s (%s)]: %s",
                                    nombre, range.inicio(), range.fin(), range.code(), ex.getMessage());
                            errores.add(msg);
                            log.error("Error importando rango: {}", msg, ex);
                        }
                    }

                    personasProcesadas++;
                }
            }
        }

        log.info("Importación completada: {} personas, {} vacaciones, {} ausencias, {} no encontradas, {} errores",
                personasProcesadas, vacacionesCreadas, ausenciasCreadas,
                personasNoEncontradas.size(), errores.size());

        return new ExcelImportResponse(
                personasProcesadas,
                vacacionesCreadas,
                ausenciasCreadas,
                personasNoEncontradas,
                errores);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Métodos auxiliares: estructura del Excel
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Encuentra el índice de la fila que contiene los nombres de mes (ENERO, FEBRERO...).
     * Devuelve -1 si no se encuentra.
     */
    private int findMonthRow(Sheet sheet) {
        for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 30); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell cell : row) {
                String val = getStringValue(cell);
                if (val != null && MONTH_MAP.containsKey(val.trim().toUpperCase())) {
                    return r;
                }
            }
        }
        return -1;
    }

    /**
     * Construye el mapa columna → LocalDate a partir de la fila de meses y la siguiente
     * (que contiene los números de día).
     *
     * <p>Lógica del año fiscal:</p>
     * <ul>
     *   <li>Si el primer mes es Enero → todos los meses usan {@code año}.</li>
     *   <li>Si el primer mes no es Enero → los meses desde el primer mes hasta
     *       Diciembre usan {@code año - 1}; Enero en adelante usa {@code año}.</li>
     * </ul>
     */
    private Map<Integer, LocalDate> buildColumnDateMap(Sheet sheet, int monthRowIdx, int año) {
        Row monthRow = sheet.getRow(monthRowIdx);
        Row dayRow   = sheet.getRow(monthRowIdx + 1);
        if (monthRow == null || dayRow == null) return Map.of();

        // Paso 1: detectar la columna de inicio de cada mes
        // TreeMap para mantener el orden de columna
        TreeMap<Integer, Integer> colMonth = new TreeMap<>(); // col → month number
        for (Cell cell : monthRow) {
            String val = getStringValue(cell);
            if (val != null) {
                Integer m = MONTH_MAP.get(val.trim().toUpperCase());
                if (m != null) {
                    colMonth.put(cell.getColumnIndex(), m);
                }
            }
        }
        if (colMonth.isEmpty()) return Map.of();

        // Detectar primer mes para lógica de año fiscal
        int firstMonth = colMonth.firstEntry().getValue();
        boolean wrapsYear = firstMonth != 1; // Chile: año fiscal que cruza el año

        // Paso 2: para cada columna que tiene un número de día, determinar la fecha
        Map<Integer, LocalDate> colDateMap = new HashMap<>();

        // Iterar la fila de días
        for (Cell cell : dayRow) {
            if (cell == null || cell.getCellType() == CellType.BLANK) continue;
            int day;
            if (cell.getCellType() == CellType.NUMERIC) {
                day = (int) cell.getNumericCellValue();
            } else {
                String s = getStringValue(cell);
                if (s == null) continue;
                try { day = Integer.parseInt(s.trim()); } catch (NumberFormatException e) { continue; }
            }
            if (day < 1 || day > 31) continue;

            int col = cell.getColumnIndex();

            // Encontrar el mes correspondiente: el mes cuya columna de inicio es <= col
            Map.Entry<Integer, Integer> monthEntry = colMonth.floorEntry(col);
            if (monthEntry == null) continue;
            int month = monthEntry.getValue();

            // Calcular el año correcto según lógica fiscal
            int yearForDate;
            if (!wrapsYear) {
                yearForDate = año;
            } else {
                // El año fiscal empieza antes de Enero: meses ≥ firstMonth → año-1
                yearForDate = (month >= firstMonth) ? (año - 1) : año;
            }

            try {
                LocalDate date = LocalDate.of(yearForDate, month, day);
                colDateMap.put(col, date);
            } catch (Exception e) {
                // Fecha inválida (ej. 31 de febrero) — ignorar
            }
        }

        return colDateMap;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Métodos auxiliares: resolución de persona y agrupación de días
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Resuelve una Persona a partir del nombre extraído del Excel.
     * Orden de búsqueda:
     * <ol>
     *   <li>Mapeo manual ({@code mappings}) — el usuario asignó explícitamente este nombre</li>
     *   <li>Coincidencia exacta en BD (case-insensitive)</li>
     *   <li>Coincidencia parcial única en BD</li>
     * </ol>
     *
     * @param nombre   nombre tal como aparece en el Excel
     * @param mappings mapa nombre-excel → personaId proporcionado por el usuario
     */
    private Optional<Persona> resolvePersona(String nombre, Map<String, Long> mappings) {
        // 1. Mapeo manual explícito
        Long manualId = mappings.get(nombre.trim());
        if (manualId == null) manualId = mappings.get(nombre); // fallback sin trim
        if (manualId != null) {
            return personaRepository.findById(manualId);
        }

        // 2. Coincidencia exacta BD
        Optional<Persona> exact = personaRepository.findByNombreIgnoreCase(nombre.trim());
        if (exact.isPresent()) return exact;

        // 3. Coincidencia parcial única
        String[] parts = nombre.trim().split("\\s+");
        if (parts.length == 0) return Optional.empty();

        List<Persona> candidates = personaRepository.findByNombreContainingIgnoreCase(nombre.trim());
        if (candidates.size() == 1) return Optional.of(candidates.get(0));

        // Intentar con las dos primeras palabras si no hubo resultado
        if (parts.length >= 2 && candidates.isEmpty()) {
            String partial = parts[0] + " " + parts[1];
            candidates = personaRepository.findByNombreContainingIgnoreCase(partial);
            if (candidates.size() == 1) return Optional.of(candidates.get(0));
        }

        return Optional.empty();
    }

    /**
     * Agrupa un mapa de (fecha, código) en rangos de días consecutivos del mismo tipo.
     * Dos fechas se consideran parte del mismo rango si tienen el mismo código y la diferencia
     * entre ellas no supera {@link #MAX_GAP_DAYS} (para puentes de fin de semana).
     */
    private List<DayRange> groupConsecutiveDays(TreeMap<LocalDate, String> dayCodes) {
        List<DayRange> ranges = new ArrayList<>();
        if (dayCodes.isEmpty()) return ranges;

        // Iterate sorted dates
        LocalDate rangeStart = null;
        LocalDate rangeEnd   = null;
        String    rangeCode  = null;

        for (Map.Entry<LocalDate, String> entry : dayCodes.entrySet()) {
            LocalDate date = entry.getKey();
            String code    = entry.getValue();

            if (rangeStart == null) {
                // Iniciar primer rango
                rangeStart = date;
                rangeEnd   = date;
                rangeCode  = code;
            } else if (code.equals(rangeCode) && (date.toEpochDay() - rangeEnd.toEpochDay()) <= MAX_GAP_DAYS) {
                // Extender rango actual
                rangeEnd = date;
            } else {
                // Cerrar rango anterior y abrir uno nuevo
                ranges.add(new DayRange(rangeStart, rangeEnd, rangeCode));
                rangeStart = date;
                rangeEnd   = date;
                rangeCode  = code;
            }
        }
        // Cerrar último rango
        if (rangeStart != null) {
            ranges.add(new DayRange(rangeStart, rangeEnd, rangeCode));
        }

        return ranges;
    }

    /**
     * Normaliza un código de tipo del Excel.
     * Devuelve null para códigos que NO deben importarse.
     */
    private String normalizeCode(String raw) {
        if (raw == null) return null;
        String upper = raw.trim().toUpperCase();

        // Códigos de vacación o ausencia conocidos → devolver en mayúsculas
        if (VACACION_CODES.containsKey(upper) || AUSENCIA_CODES.containsKey(upper)) {
            return upper;
        }

        // Ignorar el resto (C, C-1H, T, Z, M, VAL, COR, D, S, espacio, etc.)
        return null;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Métodos auxiliares: cálculos de fecha / lectura de celdas
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Calcula días laborables entre dos fechas inclusive, excluyendo sábados y domingos.
     */
    private int calcularDiasLaborables(LocalDate inicio, LocalDate fin) {
        int count = 0;
        LocalDate d = inicio;
        while (!d.isAfter(fin)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            d = d.plusDays(1);
        }
        return count;
    }

    /**
     * Extrae el valor de texto de una celda (STRING o NUMERIC convertido a String),
     * o devuelve null si la celda está en blanco.
     */
    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim().isEmpty() ? null
                                                                        : cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                // Si es entero, devolver como entero (evitar "1.0")
                yield (d == Math.floor(d)) ? String.valueOf((int) d) : String.valueOf(d);
            }
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf((int) cell.getNumericCellValue()); }
            }
            default -> null;
        };
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tipos internos
    // ──────────────────────────────────────────────────────────────────────

    /** Rango contíguo de días con el mismo código de tipo. */
    private record DayRange(LocalDate inicio, LocalDate fin, String code) {}
}
