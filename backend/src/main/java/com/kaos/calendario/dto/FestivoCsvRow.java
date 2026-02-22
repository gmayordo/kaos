package com.kaos.calendario.dto;

import java.time.LocalDate;
import com.kaos.calendario.entity.TipoFestivo;

/**
 * Representa una fila del CSV de festivos por ciudad.
 * Formato: fecha;descripcion;tipo;ciudad
 * Ejemplo: 2026-01-01;Año Nuevo;NACIONAL;Madrid
 */
public record FestivoCsvRow(
        LocalDate fecha,
        String descripcion,
        TipoFestivo tipo,
        String ciudad
) {}
