package com.kaos.planificacion.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.persona.entity.Persona;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Tarea de larga duración que cruza múltiples sprints (tipo Gantt).
 * Usada para seguimiento, recordatorios, formación, reuniones recurrentes, etc.
 */
@Entity
@Table(name = "tarea_continua", indexes = {
    @Index(columnList = "squad_id",    name = "idx_tarea_continua_squad"),
    @Index(columnList = "persona_id",  name = "idx_tarea_continua_persona"),
    @Index(columnList = "fecha_inicio, fecha_fin", name = "idx_tarea_continua_fechas")
})
@Comment("Tareas de larga duración que cruzan múltiples sprints (seguimiento, recordatorios, formación, etc.)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TareaContinua extends BaseEntity {

    @Comment("Título descriptivo de la tarea continua")
    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Comment("Descripción opcional")
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Comment("Squad al que pertenece")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @Comment("Persona asignada (null = sin asignar)")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @Comment("Fecha absoluta de inicio (no vinculada a sprint)")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Comment("Fecha de fin, null si indefinida o recurrente")
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Comment("Horas dedicadas por día a esta tarea continua")
    @Column(name = "horas_por_dia", columnDefinition = "DECIMAL(5,2)")
    private BigDecimal horasPorDia;

    @Comment("Si true, la tarea es visual/recordatorio y no descuenta capacidad")
    @Column(name = "es_informativa", nullable = false)
    private boolean esInformativa = false;

    @Comment("Color hex de la barra en el timeline (#RRGGBB)")
    @Column(name = "color", nullable = false, length = 7)
    @Builder.Default
    private String color = "#6366f1";

    @Comment("Soft delete: si false la tarea está archivada")
    @Column(name = "activa", nullable = false)
    @Builder.Default
    private boolean activa = true;
}
