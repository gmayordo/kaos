package com.kaos.calendario.dto;

import java.util.List;

/**
 * Resultado de la importación masiva desde Excel.
 */
public record ExcelImportResponse(
        /** Personas procesadas correctamente. */
        int personasProcesadas,
        /** Registros de vacación creados. */
        int vacacionesCreadas,
        /** Registros de ausencia creados. */
        int ausenciasCreadas,
        /** Nombres del Excel que no se encontraron en la BD. */
        List<String> personasNoEncontradas,
        /** Errores puntuales (persona + detalle). */
        List<String> errores
) {}
