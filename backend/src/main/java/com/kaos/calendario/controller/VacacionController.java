package com.kaos.calendario.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaos.calendario.dto.ExcelAnalysisResponse;
import com.kaos.calendario.dto.ExcelImportResponse;
import com.kaos.calendario.dto.VacacionRequest;
import com.kaos.calendario.dto.VacacionResponse;
import com.kaos.calendario.service.ExcelImportService;
import com.kaos.calendario.service.VacacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para operaciones CRUD de Vacacion.
 * Endpoints: /api/v1/vacaciones
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vacaciones")
@RequiredArgsConstructor
@Tag(name = "Vacacion", description = "Gestión de vacaciones y permisos")
public class VacacionController {

    private final VacacionService service;
    private final ExcelImportService excelImportService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "Lista vacaciones con filtros opcionales")
    public ResponseEntity<List<VacacionResponse>> listarVacaciones(
            @RequestParam(required = false) Long personaId,
            @RequestParam(required = false) Long squadId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        log.info("GET /api/v1/vacaciones?personaId={}&squadId={}&fechaInicio={}&fechaFin={}", 
                personaId, squadId, fechaInicio, fechaFin);
        return ResponseEntity.ok(service.listar(personaId, squadId, fechaInicio, fechaFin));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una vacación por ID")
    public ResponseEntity<VacacionResponse> obtenerVacacion(@PathVariable Long id) {
        log.info("GET /api/v1/vacaciones/{}", id);
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva vacación")
    public ResponseEntity<VacacionResponse> crearVacacion(@Valid @RequestBody VacacionRequest request) {
        log.info("POST /api/v1/vacaciones");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una vacación existente")
    public ResponseEntity<VacacionResponse> actualizarVacacion(
            @PathVariable Long id,
            @Valid @RequestBody VacacionRequest request) {
        log.info("PUT /api/v1/vacaciones/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una vacación")
    public ResponseEntity<Void> eliminarVacacion(@PathVariable Long id) {
        log.info("DELETE /api/v1/vacaciones/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Analiza un fichero Excel sin importar nada (dry-run).
     * Devuelve qué nombres se resolvieron automáticamente y cuáles necesitan
     * mapeo manual por parte del usuario.
     */
    @PostMapping(value = "/analizar-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analiza un Excel de vacaciones sin importar (dry-run)")
    public ResponseEntity<ExcelAnalysisResponse> analizarExcel(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int año) {
        log.info("POST /api/v1/vacaciones/analizar-excel — fichero: {}, año: {}",
                file.getOriginalFilename(), año);
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El fichero está vacío");
        }
        String fname = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!fname.endsWith(".xlsx") && !fname.endsWith(".xls")) {
            throw new IllegalArgumentException("Solo se aceptan ficheros .xlsx o .xls");
        }
        try {
            return ResponseEntity.ok(excelImportService.analizarExcel(file, año));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error leyendo el fichero Excel: " + e.getMessage());
        }
    }

    /**
     * Importa vacaciones y ausencias desde un fichero Excel (.xlsx).
     *
     * @param file     fichero Excel con el calendario de vacaciones
     * @param año      año fiscal (por defecto el año en curso)
     * @param mappingsJson JSON con mapeos manuales nombre-excel → personaId,
     *                     e.g. {"Marcela":"12","Gino":"7"}.
     *                     Opcional; si no se envía se asume {}
     */
    @PostMapping(value = "/importar-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa vacaciones/ausencias desde un fichero Excel")
    public ResponseEntity<ExcelImportResponse> importarExcel(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int año,
            @RequestParam(required = false) String mappingsJson) {
        log.info("POST /api/v1/vacaciones/importar-excel — fichero: {}, año: {}, mappings: {}",
                file.getOriginalFilename(), año, mappingsJson);
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El fichero está vacío");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new IllegalArgumentException("Solo se aceptan ficheros .xlsx o .xls");
        }

        Map<String, Long> mappings = Collections.emptyMap();
        if (mappingsJson != null && !mappingsJson.isBlank()) {
            try {
                mappings = objectMapper.readValue(mappingsJson, new TypeReference<Map<String, Long>>() {});
            } catch (IOException e) {
                throw new IllegalArgumentException("El parámetro 'mappingsJson' no tiene formato JSON válido: " + e.getMessage());
            }
        }

        try {
            ExcelImportResponse result = excelImportService.importarExcel(file, año, mappings);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error leyendo el fichero Excel: " + e.getMessage());
        }
    }
}
