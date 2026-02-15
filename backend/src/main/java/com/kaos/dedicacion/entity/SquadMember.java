package com.kaos.dedicacion.entity;

import java.time.LocalDate;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.entity.Rol;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Asignación de una persona a un squad con rol y porcentaje de dedicación.
 */
@Entity
@Table(name = "squad_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_squad_member_persona_squad",
                columnNames = {"persona_id", "squad_id"}))
@Comment("Asignaciones de personas a squads con dedicación")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SquadMember extends BaseEntity {

    @Comment("Persona asignada")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Comment("Squad al que se asigna")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @Comment("Rol de la persona en el squad")
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 30)
    private Rol rol;

    @Comment("Porcentaje de dedicación (0-100)")
    @Column(name = "porcentaje", nullable = false)
    private Integer porcentaje;

    @Comment("Fecha inicio de la asignación")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Comment("Fecha fin de la asignación (null = indefinido)")
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
}
