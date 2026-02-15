package com.kaos.calendario.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaos.calendario.dto.FestivoRequest;
import com.kaos.calendario.dto.FestivoResponse;
import com.kaos.calendario.dto.FestivoCsvUploadResponse;
import com.kaos.calendario.entity.TipoFestivo;
import com.kaos.calendario.service.FestivoService;

/**
 * Controller REST para Festivos.
 */
@RestController
@RequestMapping("/api/v1/festivos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Festivo", description = "Gestión de festivos con asignación a personas")
public class FestivoController {

    private final FestivoService service;

    @GetMapping
    @Operation(summary = "Lista festivos con filtros opcionales")
    public ResponseEntity<List<FestivoResponse>> listarFestivos(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) TipoFestivo tipo) {
        log.debug("GET /api/v1/festivos - anio: {}, tipo: {}", anio, tipo);
        return ResponseEntity.ok(service.listar(anio, tipo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un festivo por ID")
    public ResponseEntity<FestivoResponse> obtenerFestivo(@PathVariable Long id) {
        log.debug("GET /api/v1/festivos/{}", id);
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea un nuevo festivo")
    public ResponseEntity<FestivoResponse> crearFestivo(@Valid @RequestBody FestivoRequest request) {
        log.info("POST /api/v1/festivos");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un festivo")
    public ResponseEntity<FestivoResponse> actualizarFestivo(
            @PathVariable Long id,
            @Valid @RequestBody FestivoRequest request) {
        log.info("PUT /api/v1/festivos/{}", id);
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un festivo")
    public ResponseEntity<Void> eliminarFestivo(@PathVariable Long id) {
        log.info("DELETE /api/v1/festivos/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/carga-masiva", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Carga masiva de festivos desde CSV")
    public ResponseEntity<FestivoCsvUploadResponse> cargarFestivosCsv(
            @RequestParam("file") MultipartFile file) {
        log.info("POST /api/v1/festivos/carga-masiva - archivo: {}", file.getOriginalFilename());
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        
        if (!file.getOriginalFilename().endsWith(".csv")) {
            throw new IllegalArgumentException("El archivo debe ser formato CSV");
        }
        
        return ResponseEntity.ok(service.cargarCsv(file));
    }
}
