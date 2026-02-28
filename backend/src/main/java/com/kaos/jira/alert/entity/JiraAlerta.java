package com.kaos.jira.alert.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.kaos.jira.alert.entity.JiraAlertRule.Severidad;
import com.kaos.persona.entity.Persona;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Alerta generada por el motor SpEL al evaluar una regla {@link JiraAlertRule}.
 *
 * <p>Se crea una instancia por cada regla que se dispara sobre un issue,
 * persona o sprint. Persiste hasta que el Lead Tech la marca como resuelta.</p>
 *
 * <p>No extiende {@code BaseEntity} porque no necesita {@code createdBy}
 * (la crea el sistema, no un usuario) y su ciclo de vida es solo inserción.</p>
 */
@Entity
@Table(name = "jira_alerta")
@Comment("Alertas generadas por el motor de coherencia SpEL tras cada sync Jira")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Contexto ──────────────────────────────────

    @Comment("Sprint sobre el que se evaluó la alerta")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @Comment("Squad al que pertenece la alerta")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @Comment("Regla que generó esta alerta")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_id", nullable = false)
    private JiraAlertRule regla;

    // ── Contenido ────────────────────────────────

    @Comment("Copia de la severidad para consultas rápidas sin JOIN")
    @Enumerated(EnumType.STRING)
    @Column(name = "severidad", nullable = false, length = 20)
    private Severidad severidad;

    @Comment("Mensaje con los valores reales ya resueltos (sin placeholders)")
    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    // ── Issue / persona afectadas (nullable) ──────

    @Comment("Clave Jira de la issue que disparó la alerta (nullable: alertas de nivel sprint o persona)")
    @Column(name = "jira_key", length = 50)
    private String jiraKey;

    @Comment("Persona afectada por la alerta (nullable: alertas de nivel issue o sprint)")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    // ── Estado ───────────────────────────────────

    @Comment("true cuando el Lead Tech marca la alerta como atendida")
    @Column(name = "resuelta", nullable = false)
    @Builder.Default
    private boolean resuelta = false;

    @Comment("true cuando el resumen por email ya incluyó esta alerta")
    @Column(name = "notificada_email", nullable = false)
    @Builder.Default
    private boolean notificadaEmail = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Métodos de negocio ────────────────────────

    /** Marca la alerta como resuelta. */
    public void resolver() {
        this.resuelta = true;
    }

    /** Marca la alerta como incluida en el último email de resumen. */
    public void marcarNotificada() {
        this.notificadaEmail = true;
    }
}
