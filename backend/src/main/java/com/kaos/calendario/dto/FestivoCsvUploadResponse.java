package com.kaos.calendario.dto;

/**
 * Resumen de carga masiva de festivos desde CSV.
 */
public record FestivoCsvUploadResponse(
        int totalProcesados,
        int exitosos,
        int errores,
        java.util.List<FestivoCsvError> detalleErrores
) {}
