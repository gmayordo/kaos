package com.kaos.calendario.entity;

import java.time.LocalDate;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.persona.entity.Persona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Vacación de una persona en un rango de fechas.
 */
@Entity
@Table(name = "vacacion")
@Comment("Vacaciones de personas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Vacacion extends BaseEntity {

    @Comment("Persona a la que pertenece la vacación")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Comment("Fecha de inicio de vacaciones")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Comment("Fecha de fin de vacaciones (inclusive)")
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Comment("Días laborables (excluye fines de semana)")
    @Column(name = "dias_laborables")
    private Integer diasLaborables;

    @Comment("Tipo de vacación")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoVacacion tipo;

    @Comment("Estado de la vacación")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoVacacion estado;

    @Comment("Comentario adicional")
    @Column(name = "comentario", length = 500)
    private String comentario;
}
