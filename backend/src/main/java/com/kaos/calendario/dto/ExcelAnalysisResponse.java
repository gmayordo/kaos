package com.kaos.calendario.dto;

import java.util.List;

/**
 * Resultado del análisis previo de un fichero Excel (dry-run, sin guardar).
 * Permite al usuario verificar qué nombres se han resuelto y cuáles
 * necesitan mapeo manual antes de iniciar la importación real.
 */
public record ExcelAnalysisResponse(
        /** Total de filas de persona detectadas en el Excel (excluye cabeceras/secciones). */
        int totalFilasPersona,

        /** Nombres del Excel que se han resuelto automáticamente contra la BD. */
        List<PersonaMatch> personasResueltas,

        /**
         * Nombres del Excel para los que NO se encontró ninguna persona en la BD.
         * El usuario deberá asignarles un personaId antes de importar.
         */
        List<String> personasNoResueltas
) {

    /**
     * Mapeo automático de un nombre del Excel a una persona de la BD.
     *
     * @param nombreExcel  nombre tal como aparece en el Excel
     * @param personaId    ID de la persona encontrada en BD
     * @param personaNombre nombre completo de la persona en BD
     */
    public record PersonaMatch(String nombreExcel, Long personaId, String personaNombre) {}
}
