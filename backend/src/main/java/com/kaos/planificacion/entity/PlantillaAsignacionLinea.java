package com.kaos.planificacion.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Línea de una {@link PlantillaAsignacion}.
 * Define un rol y el porcentaje de la estimación total que recibe.
 */
@Entity
@Table(
        name = "plantilla_asignacion_linea",
        indexes = @Index(columnList = "plantilla_id", name = "idx_plantilla_linea_plantilla")
)
@Comment("Líneas de una plantilla de asignación: cada una define un rol y su % de horas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PlantillaAsignacionLinea extends BaseEntity {

    @Comment("FK a plantilla_asignacion")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plantilla_id", nullable = false)
    private PlantillaAsignacion plantilla;

    @Comment("Rol KAOS: DESARROLLADOR, QA, TECH_LEAD, FUNCIONAL, OTRO")
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private RolPlantilla rol;

    @Comment("% de la estimación total que se asigna a este rol (1..100)")
    @Column(name = "porcentaje_horas", nullable = false)
    private Integer porcentajeHoras;

    @Comment("Posición dentro de la plantilla (para ordenación en UI)")
    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Comment("Orden del ítem del que depende este ítem (nullable)")
    @Column(name = "depende_de_orden")
    private Integer dependeDeOrden;
}
