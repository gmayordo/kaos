package com.kaos.planificacion.exception;

/**
 * Excepción lanzada cuando la capacidad disponible es insuficiente para asignar una tarea.
 */
public class CapacidadInsuficienteException extends RuntimeException {

    private final Long personaId;
    private final Integer dia;
    private final Double capacidadDisponible;
    private final Double horasRequeridas;

    public CapacidadInsuficienteException(
            String message,
            Long personaId,
            Integer dia,
            Double capacidadDisponible,
            Double horasRequeridas) {
        super(message);
        this.personaId = personaId;
        this.dia = dia;
        this.capacidadDisponible = capacidadDisponible;
        this.horasRequeridas = horasRequeridas;
    }

    public Long getPersonaId() {
        return personaId;
    }

    public Integer getDia() {
        return dia;
    }

    public Double getCapacidadDisponible() {
        return capacidadDisponible;
    }

    public Double getHorasRequeridas() {
        return horasRequeridas;
    }
}
