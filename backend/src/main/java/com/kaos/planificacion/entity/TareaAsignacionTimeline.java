package com.kaos.planificacion.entity;

import java.math.BigDecimal;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.persona.entity.Persona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Asignación de una tarea padre (HISTORIA) a una persona en el timeline con rango de días.
 * Permite vincular issues Jira padre al grid de planificación sin necesitar {@code diaAsignado}.
 */
@Entity
@Table(name = "tarea_asignacion_timeline", indexes = {
    @Index(columnList = "sprint_id",  name = "idx_tat_sprint"),
    @Index(columnList = "tarea_id",   name = "idx_tat_tarea"),
    @Index(columnList = "persona_id", name = "idx_tat_persona")
})
@Comment("Asignaciones de tareas padre a personas en el timeline con rango de días")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TareaAsignacionTimeline extends BaseEntity {

    @Comment("Tarea padre (HISTORIA) vinculada al timeline")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_id", nullable = false)
    private Tarea tarea;

    @Comment("Persona asignada para este rango de días")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Comment("Sprint donde aplica la asignación")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @Comment("Día de inicio en el sprint (1=lunes semana 1 ... 10=viernes semana 2)")
    @Column(name = "dia_inicio", nullable = false)
    private Integer diaInicio;

    @Comment("Día de fin en el sprint (1..10), debe ser >= dia_inicio")
    @Column(name = "dia_fin", nullable = false)
    private Integer diaFin;

    @Comment("Horas dedicadas por día a esta tarea. null = toda la disponibilidad")
    @Column(name = "horas_por_dia", columnDefinition = "DECIMAL(5,2)")
    private BigDecimal horasPorDia;

    @Comment("Si true, la asignación es visual/recordatorio y no descuenta capacidad")
    @Column(name = "es_informativa", nullable = false)
    private boolean esInformativa = false;
}
