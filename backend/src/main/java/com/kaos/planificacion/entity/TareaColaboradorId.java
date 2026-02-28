package com.kaos.planificacion.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clave primaria compuesta para {@link TareaColaborador}.
 *
 * <p>Identifica unívocamente el par (tarea, persona colaboradora).</p>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TareaColaboradorId implements Serializable {

    @Column(name = "tarea_id")
    private Long tareaId;

    @Column(name = "persona_id")
    private Long personaId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TareaColaboradorId that = (TareaColaboradorId) o;
        return Objects.equals(tareaId, that.tareaId)
                && Objects.equals(personaId, that.personaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tareaId, personaId);
    }
}
