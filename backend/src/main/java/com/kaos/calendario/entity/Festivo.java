package com.kaos.calendario.entity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.persona.entity.Persona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Festivo asignado a una o más personas.
 * Un festivo implica 0 horas de capacidad para las personas asignadas.
 */
@Entity
@Table(
    name = "festivo",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_festivo_fecha_descripcion",
        columnNames = {"fecha", "descripcion"}
    )
)
@Comment("Festivos asignados a personas específicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Festivo extends BaseEntity {

    @Comment("Fecha del festivo")
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Comment("Descripción del festivo")
    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Comment("Tipo de festivo (NACIONAL, REGIONAL, LOCAL)")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoFestivo tipo;

    @Comment("Personas a las que aplica este festivo")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "persona_festivo",
        joinColumns = @JoinColumn(name = "festivo_id"),
        inverseJoinColumns = @JoinColumn(name = "persona_id")
    )
    @lombok.Builder.Default
    private Set<Persona> personas = new HashSet<>();
}
