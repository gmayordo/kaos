package com.kaos.planificacion.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.kaos.persona.entity.Persona;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Co-desarrolladores de una tarea (multi-dev).
 *
 * <p>Jira Solo admite un {@code assignee} por issue. Este modelo captura a los
 * desarrolladores adicionales que han imputado horas en la misma tarea, bien
 * detectados automáticamente ({@link DetectadoVia#WORKLOG}) o añadidos
 * manualmente por el LT ({@link DetectadoVia#MANUAL}).</p>
 *
 * <p>PK compuesta: {@code (tarea_id, persona_id)}.</p>
 */
@Entity
@Table(name = "tarea_colaborador")
@Comment("Co-desarrolladores de tareas KAOS, detectados por worklogs o añadidos manualmente")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TareaColaborador {

    @EmbeddedId
    private TareaColaboradorId id;

    // ── Relaciones (JPA necesita las referencias objeto además del EmbeddedId) ──

    @Comment("Tarea KAOS en la que colabora esta persona")
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tareaId")
    @JoinColumn(name = "tarea_id", nullable = false)
    private Tarea tarea;

    @Comment("Persona colaboradora")
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("personaId")
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    // ── Datos del rol ─────────────────────────────────────────────────────────

    @Comment("Rol de la persona en la tarea: DESARROLLADOR / REVISOR / APOYO")
    @Column(name = "rol", length = 30)
    private String rol;

    @Comment("Total de horas imputadas por esta persona en la tarea")
    @Column(name = "horas_imputadas", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal horasImputadas = BigDecimal.ZERO;

    @Comment("Cómo se detectó la colaboración: WORKLOG (automático) / MANUAL (asignado LT)")
    @Enumerated(EnumType.STRING)
    @Column(name = "detectado_via", nullable = false, length = 20)
    private DetectadoVia detectadoVia;

    // ── Auditoría ─────────────────────────────────────────────────────────────

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Enum interno ──────────────────────────────────────────────────────────

    /**
     * Cómo se detectó o añadió el co-desarrollador.
     */
    public enum DetectadoVia {
        /** Detectado automáticamente por worklogs al importar desde Jira. */
        WORKLOG,
        /** Añadido manualmente por el LT. */
        MANUAL
    }
}
