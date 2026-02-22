package com.kaos.calendario.entity;

/**
 * Tipo de festivo según su ámbito de aplicación.
 */
public enum TipoFestivo {
    /** Festivo nacional (todo el país) */
    NACIONAL,
    
    /** Festivo regional (comunidad autónoma, provincia) */
    REGIONAL,
    
    /** Festivo local (ciudad, municipio) */
    LOCAL
}
