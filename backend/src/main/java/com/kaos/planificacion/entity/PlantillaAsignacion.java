package com.kaos.planificacion.entity;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Plantilla de distribución de horas para la planificación automática de issues Jira.
 *
 * <p>Cada plantilla aplica a un {@code tipoJira} (Story, Bug, Task…) y contiene
 * una o más líneas que describen qué porcentaje de la estimación total recibe
 * cada rol del equipo.</p>
 */
@Entity
@Table(
        name = "plantilla_asignacion",
        indexes = @Index(columnList = "tipo_jira, activo", name = "idx_plantilla_tipo_activo"),
        uniqueConstraints = @UniqueConstraint(columnNames = "nombre", name = "uk_plantilla_nombre")
)
@Comment("Plantillas de distribución de horas para planificación automática de issues Jira")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PlantillaAsignacion extends BaseEntity {

    @Comment("Nombre descriptivo único de la plantilla")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Comment("Tipo de issue Jira al que aplica: Story, Bug, Task, Sub-task, Spike")
    @Column(name = "tipo_jira", nullable = false, length = 50)
    private String tipoJira;

    @Comment("Si false la plantilla está desactivada y no se aplica automáticamente")
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Comment("Líneas de distribución por rol")
    @OneToMany(mappedBy = "plantilla", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<PlantillaAsignacionLinea> lineas = new ArrayList<>();
}
