package com.kaos.calendario.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.kaos.calendario.dto.FestivoCsvError;
import com.kaos.calendario.dto.FestivoCsvRow;
import com.kaos.calendario.dto.FestivoCsvUploadResponse;
import com.kaos.calendario.dto.FestivoRequest;
import com.kaos.calendario.dto.FestivoResponse;
import com.kaos.calendario.entity.Festivo;
import com.kaos.calendario.entity.TipoFestivo;
import com.kaos.calendario.mapper.FestivoMapper;
import com.kaos.calendario.repository.FestivoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de negocio para gestión de {@link Festivo}.
 * Festivos están vinculados a ciudad (calendario laboral por ubicación).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FestivoService {

    private final FestivoRepository repository;
    private final FestivoMapper mapper;

    /**
     * Lista festivos con filtros opcionales.
     *
     * @param anio filtrar por año (nullable)
     * @param tipo filtrar por tipo (nullable)
     */
    public List<FestivoResponse> listar(Integer anio, TipoFestivo tipo) {
        log.debug("Listando festivos - anio: {}, tipo: {}", anio, tipo);

        List<Festivo> festivos;
        if (anio != null && tipo != null) {
            festivos = repository.findByAnioAndTipo(anio, tipo);
        } else if (anio != null) {
            festivos = repository.findByAnio(anio);
        } else if (tipo != null) {
            festivos = repository.findByTipo(tipo);
        } else {
            festivos = repository.findAll();
        }

        return mapper.toResponseList(festivos);
    }

    /**
     * Obtiene un festivo por su ID.
     *
     * @throws EntityNotFoundException si no existe
     */
    public FestivoResponse obtener(Long id) {
        log.debug("Obteniendo festivo con id: {}", id);
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Festivo no encontrado con id: " + id));
    }

    /**
     * Crea un nuevo festivo.
     * Valida que no haya duplicado (fecha + descripción + ciudad).
     */
    @Transactional
    public FestivoResponse crear(FestivoRequest request) {
        log.info("Creando festivo: {} ({}) en {}", request.descripcion(), request.fecha(), request.ciudad());

        // Validar duplicado
        if (repository.existsByFechaAndDescripcionAndCiudad(
                request.fecha(), request.descripcion(), request.ciudad())) {
            throw new IllegalArgumentException(
                    "Ya existe un festivo con fecha '" + request.fecha() + 
                    "', descripción '" + request.descripcion() + 
                    "' y ciudad '" + request.ciudad() + "'");
        }

        Festivo entity = mapper.toEntity(request);
        Festivo saved = repository.save(entity);
        log.info("Festivo creado con id: {}", saved.getId());
        return mapper.toResponse(saved);
    }

    /**
     * Actualiza un festivo existente.
     */
    @Transactional
    public FestivoResponse actualizar(Long id, FestivoRequest request) {
        log.info("Actualizando festivo con id: {}", id);

        Festivo entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Festivo no encontrado con id: " + id));

        // Validar duplicado solo si cambia fecha, descripción o ciudad
        if (!entity.getFecha().equals(request.fecha()) 
                || !entity.getDescripcion().equals(request.descripcion())
                || !entity.getCiudad().equals(request.ciudad())) {
            if (repository.existsByFechaAndDescripcionAndCiudad(
                    request.fecha(), request.descripcion(), request.ciudad())) {
                throw new IllegalArgumentException(
                        "Ya existe un festivo con fecha '" + request.fecha() + 
                        "', descripción '" + request.descripcion() + 
                        "' y ciudad '" + request.ciudad() + "'");
            }
        }

        // Actualizar campos
        mapper.updateEntity(request, entity);

        Festivo updated = repository.save(entity);
        log.info("Festivo actualizado: {}", id);
        return mapper.toResponse(updated);
    }

    /**
     * Elimina un festivo.
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando festivo con id: {}", id);
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Festivo no encontrado con id: " + id);
        }
        repository.deleteById(id);
        log.info("Festivo eliminado: {}", id);
    }

    /**
     * Lista festivos de una ciudad en un rango de fechas.
     *
     * @param ciudad      nombre de la ciudad
     * @param fechaInicio inicio del rango (nullable)
     * @param fechaFin    fin del rango (nullable)
     */
    public List<FestivoResponse> listarPorCiudad(String ciudad, LocalDate fechaInicio, LocalDate fechaFin) {
        log.debug("Listando festivos de ciudad: {} (desde {} hasta {})", ciudad, fechaInicio, fechaFin);
        List<Festivo> festivos = repository.findByCiudadAndFechaRange(ciudad, fechaInicio, fechaFin);
        return mapper.toResponseList(festivos);
    }

    /**
     * Carga masiva de festivos desde CSV.
     * Formato: fecha;descripcion;tipo;ciudad
     * Ejemplo: 2026-01-01;Año Nuevo;NACIONAL;Madrid
     * 
     * Procesamiento parcial: festivos duplicados se ignoran, errores no bloquean el resto.
     *
     * @param file archivo CSV
     * @return resumen de carga con detalles de errores
     */
    @Transactional
    public FestivoCsvUploadResponse cargarCsv(MultipartFile file) {
        log.info("Procesando carga masiva CSV: {}", file.getOriginalFilename());
        
        List<FestivoCsvError> errores = new ArrayList<>();
        int totalProcesados = 0;
        int exitosos = 0;
        int numeroFila = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String linea;
            while ((linea = reader.readLine()) != null) {
                numeroFila++;
                totalProcesados++;

                // Ignorar líneas vacías
                if (linea.isBlank()) {
                    continue;
                }

                try {
                    FestivoCsvRow row = parseCsvRow(linea);
                    procesarFilaCsv(row, numeroFila, errores);
                    exitosos++;
                } catch (Exception e) {
                    log.warn("Error en fila {}: {}", numeroFila, e.getMessage());
                    errores.add(new FestivoCsvError(numeroFila, e.getMessage()));
                }
            }
        } catch (IOException e) {
            log.error("Error leyendo archivo CSV", e);
            throw new IllegalArgumentException("Error leyendo archivo CSV: " + e.getMessage());
        }

        log.info("Carga masiva completada: {} procesados, {} exitosos, {} errores",
                totalProcesados, exitosos, errores.size());

        return new FestivoCsvUploadResponse(totalProcesados, exitosos, errores.size(), errores);
    }

    /**
     * Parsea una línea del CSV en FestivoCsvRow.
     * Formato: fecha;descripcion;tipo;ciudad
     */
    private FestivoCsvRow parseCsvRow(String linea) {
        String[] partes = linea.split(";");
        if (partes.length != 4) {
            throw new IllegalArgumentException("Formato inválido. Esperado: fecha;descripcion;tipo;ciudad");
        }

        try {
            LocalDate fecha = LocalDate.parse(partes[0].trim());
            String descripcion = partes[1].trim();
            TipoFestivo tipo = TipoFestivo.valueOf(partes[2].trim().toUpperCase());
            String ciudad = partes[3].trim();

            if (ciudad.isBlank()) {
                throw new IllegalArgumentException("Ciudad no puede estar vacía");
            }

            return new FestivoCsvRow(fecha, descripcion, tipo, ciudad);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida: " + partes[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo inválido: " + partes[2] + ". Debe ser: NACIONAL, REGIONAL o LOCAL");
        }
    }

    /**
     * Procesa una fila del CSV creando el festivo.
     * Si ya existe (duplicado), lo ignora sin lanzar error.
     */
    private void procesarFilaCsv(FestivoCsvRow row, int numeroFila, List<FestivoCsvError> errores) {
        // Verificar duplicado - ignorar sin error
        if (repository.existsByFechaAndDescripcionAndCiudad(row.fecha(), row.descripcion(), row.ciudad())) {
            log.debug("Fila {}: festivo duplicado, ignorando", numeroFila);
            return;
        }

        // Crear festivo
        Festivo festivo = Festivo.builder()
                .fecha(row.fecha())
                .descripcion(row.descripcion())
                .tipo(row.tipo())
                .ciudad(row.ciudad())
                .build();

        repository.save(festivo);
        log.debug("Fila {}: festivo creado para ciudad {}", numeroFila, row.ciudad());
    }
}
