package com.kaos.calendario.dto;

/**
 * Detalle de error en una fila del CSV.
 */
public record FestivoCsvError(
        int fila,
        String mensaje
) {}
