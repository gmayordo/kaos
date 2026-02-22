package com.kaos.calendario.dto;

/**
 * Información básica de una persona (para referencias).
 */
public record PersonaBasicInfo(
        Long id,
        String nombre,
        String email
) {}
