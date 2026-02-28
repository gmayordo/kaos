package com.kaos.jira.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Cache local de imputaciones (worklogs) importadas desde Jira.
 *
 * <p>Cada worklog representa el tiempo imputado por un desarrollador
 * en una issue concreta en una fecha determinada.</p>
 *
 * <p>El campo {@code origen} diferencia los worklogs importados de Jira
 * ({@link WorklogOrigen#JIRA}) de los registrados directamente en KAOS
 * ({@link WorklogOrigen#KAOS}) que aún no se han enviado a Jira.</p>
 *
 * <p>{@code jiraWorklogId} es el identificador externo único del worklog en Jira.</p>
 */
@Entity
@Table(
        name = "jira_worklog",
        uniqueConstraints = @UniqueConstraint(columnNames = "jira_worklog_id", name = "uk_jira_worklog_id")
)
@Comment("Cache de imputaciones de horas importadas desde Jira o registradas en KAOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraWorklog extends BaseEntity {

    // ── Identificación Jira ───────────────────────────────────────────────────

    @Comment("ID del worklog en Jira (worklog.id). Único. Nullable para origen=KAOS antes de sync.")
    @Column(name = "jira_worklog_id", length = 50)
    private String jiraWorklogId;

    // ── Relaciones ────────────────────────────────────────────────────────────

    @Comment("Issue KAOS a la que pertenece este worklog")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_issue_id", nullable = false)
    private JiraIssue jiraIssue;

    @Comment("Persona KAOS mapeada por author.key de Jira. Nullable si no hay mapeo.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    // ── Autoría ───────────────────────────────────────────────────────────────

    @Comment("author.key del worklog en Jira (siempre guardado para trazabilidad)")
    @Column(name = "autor_jira", nullable = false, length = 100)
    private String autorJira;

    // ── Tiempo ────────────────────────────────────────────────────────────────

    @Comment("Fecha del worklog (campo started de Jira, solo la parte de fecha)")
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Comment("Horas imputadas (timeSpentSeconds / 3600)")
    @Column(name = "horas", nullable = false, precision = 6, scale = 2)
    private BigDecimal horas;

    // ── Contenido ─────────────────────────────────────────────────────────────

    @Comment("Comentario del worklog (campo comment de Jira)")
    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    // ── Control de origen ─────────────────────────────────────────────────────

    @Comment("Origen del worklog: JIRA (importado) / KAOS (registrado aquí)")
    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 20)
    private WorklogOrigen origen;

    @Comment("Si el worklog ya fue enviado a Jira (solo aplica a origen=KAOS)")
    @Column(name = "sincronizado", nullable = false)
    private boolean sincronizado = false;
}
