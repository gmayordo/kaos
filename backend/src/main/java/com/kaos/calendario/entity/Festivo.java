package com.kaos.calendario.entity;

import java.time.LocalDate;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Festivo por ciudad/ubicación.
 * Un festivo implica 0 horas de capacidad para las personas de esa ciudad.
 */
@Entity
@Table(
    name = "festivo",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_festivo_fecha_descripcion_ciudad",
        columnNames = {"fecha", "descripcion", "ciudad"}
    )
)
@Comment("Festivos por ciudad/ubicación")
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

    @Comment("Ciudad del festivo (Madrid, Barcelona, Santiago, etc.)")
    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;
}
