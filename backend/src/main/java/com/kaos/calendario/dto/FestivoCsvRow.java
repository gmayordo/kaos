package com.kaos.calendario.dto;

import com.kaos.calendario.entity.TipoFestivo;

import java.time.LocalDate;
import java.util.List;

/**
 * Representa una fila del CSV de festivos.
 * Formato: fecha;descripcion;tipo;emails_personas
 * Ejemplo: 2026-01-01;Año Nuevo;NACIONAL;persona1@ehcos.com|persona2@ehcos.com
 */
public record FestivoCsvRow(
        LocalDate fecha,
        String descripcion,
        TipoFestivo tipo,
        List<String> emails
) {}
