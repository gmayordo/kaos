package com.kaos.calendario.entity;

import com.kaos.common.model.BaseEntity;
import com.kaos.persona.entity.Persona;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Entity Ausencia — ausencias inesperadas o bajas médicas.
 * A diferencia de Vacacion, fechaFin puede ser null (ausencias indefinidas).
 */
@Entity
@Table(name = "ausencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Ausencia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /**
     * Fecha de fin (puede ser null para ausencias indefinidas como baja médica).
     */
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoAusencia tipo;

    @Column(name = "comentario", length = 500)
    private String comentario;
}
