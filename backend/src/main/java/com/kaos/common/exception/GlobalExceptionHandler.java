package com.kaos.common.exception;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.kaos.planificacion.exception.AsignacionRequeridaException;
import com.kaos.planificacion.exception.CapacidadInsuficienteException;
import com.kaos.planificacion.exception.DependenciaCiclicaException;
import com.kaos.planificacion.exception.DependenciaDuplicadaException;
import com.kaos.planificacion.exception.SolapamientoSprintException;
import com.kaos.planificacion.exception.SprintNoEnPlanificacionException;
import com.kaos.planificacion.exception.TareaNoEnPendienteException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Manejador global de excepciones.
 * Centraliza el mapeo de excepciones a respuestas HTTP estándar.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Parámetro requerido ausente: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("MISSING_PARAMETER",
                        "Parámetro requerido: " + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        log.warn("Error de validación: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", "Error de validación", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .toList();
        log.warn("Violación de constraint: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("CONSTRAINT_VIOLATION", "Violación de restricción", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        log.warn("Argumento inválido: {}", message);

        // Detectar conflictos por mensaje
        if (message != null && (message.contains("Ya existe") || message.contains("duplicado") || message.contains("ya registrado"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of("CONFLICT", message));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("BAD_REQUEST", message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Estado inválido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(CapacidadInsuficienteException.class)
    public ResponseEntity<ErrorResponse> handleCapacidadInsuficiente(CapacidadInsuficienteException ex) {
        List<String> details = List.of(
                "personaId: " + ex.getPersonaId(),
                "dia: " + ex.getDia(),
                "disponibles: " + ex.getCapacidadDisponible(),
                "requeridas: " + ex.getHorasRequeridas()
        );
        log.warn("Capacidad insuficiente: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CAPACIDAD_INSUFICIENTE", ex.getMessage(), details));
    }

    @ExceptionHandler(SolapamientoSprintException.class)
    public ResponseEntity<ErrorResponse> handleSolapamientoSprint(SolapamientoSprintException ex) {
        log.warn("Solapamiento de sprint: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("SOLAPAMIENTO_SPRINT", ex.getMessage()));
    }

    @ExceptionHandler(SprintNoEnPlanificacionException.class)
    public ResponseEntity<ErrorResponse> handleSprintNoEnPlanificacion(SprintNoEnPlanificacionException ex) {
        log.warn("Operacion no permitida sobre sprint {}: estado actual {}", ex.getSprintId(), ex.getEstadoActual());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of("SPRINT_ESTADO_INVALIDO", ex.getMessage()));
    }

    @ExceptionHandler(TareaNoEnPendienteException.class)
    public ResponseEntity<ErrorResponse> handleTareaNoEnPendiente(TareaNoEnPendienteException ex) {
        log.warn("Operacion no permitida sobre tarea {}: estado actual {}", ex.getTareaId(), ex.getEstadoActual());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of("TAREA_ESTADO_INVALIDO", ex.getMessage()));
    }

    @ExceptionHandler(DependenciaCiclicaException.class)
    public ResponseEntity<ErrorResponse> handleDependenciaCiclica(DependenciaCiclicaException ex) {
        log.warn("Ciclo detectado en dependencias: {} → {}", ex.getTareaOrigenId(), ex.getTareaDestinoId());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("DEPENDENCIA_CICLICA", ex.getMessage()));
    }

    @ExceptionHandler(DependenciaDuplicadaException.class)
    public ResponseEntity<ErrorResponse> handleDependenciaDuplicada(DependenciaDuplicadaException ex) {
        log.warn("Dependencia duplicada: {} → {}", ex.getTareaOrigenId(), ex.getTareaDestinoId());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("DEPENDENCIA_DUPLICADA", ex.getMessage()));
    }

    @ExceptionHandler(AsignacionRequeridaException.class)
    public ResponseEntity<ErrorResponse> handleAsignacionRequerida(AsignacionRequeridaException ex) {
        log.warn("Asignación requerida para issue {}: {}", ex.getJiraKey(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("ASIGNACION_REQUERIDA", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("DATA_INTEGRITY_ERROR", "Violación de integridad de datos"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Error interno no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Error interno del servidor"));
    }
}
