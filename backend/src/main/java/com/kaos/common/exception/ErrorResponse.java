package com.kaos.common.exception;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Respuesta estándar de error.
 * Usada por {@link GlobalExceptionHandler} para todas las respuestas de error.
 *
 * @param code      código de error (e.g., "NOT_FOUND", "VALIDATION_ERROR")
 * @param message   mensaje descriptivo del error
 * @param details   detalles adicionales (errores de validación, etc.)
 * @param timestamp momento del error
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        List<String> details,
        LocalDateTime timestamp
) {
    /** Crea ErrorResponse sin detalles. */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, LocalDateTime.now());
    }

    /** Crea ErrorResponse con detalles. */
    public static ErrorResponse of(String code, String message, List<String> details) {
        return new ErrorResponse(code, message, details, LocalDateTime.now());
    }
}
