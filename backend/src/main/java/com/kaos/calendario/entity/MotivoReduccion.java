package com.kaos.calendario.entity;

/**
 * Motivo de reducción de capacidad en un día específico.
 */
public enum MotivoReduccion {
    /** Fin de semana (sábado o domingo) */
    FIN_SEMANA,
    
    /** Festivo nacional/regional/local */
    FESTIVO,
    
    /** Vacación registrada */
    VACACION,
    
    /** Ausencia (baja médica, emergencia) */
    AUSENCIA
}
