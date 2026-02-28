package com.kaos.jira.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.kaos.common.model.BaseEntity;
import com.kaos.persona.entity.Persona;
import com.kaos.planificacion.entity.Sprint;
import com.kaos.planificacion.entity.Tarea;
import com.kaos.squad.entity.Squad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Cache local de issues importadas desde Jira.
 *
 * <p>Actúa como fuente de verdad local para evitar re-fetchear datos que no
 * cambian entre sincronizaciones. El campo {@code ultimaSync} permite conocer
 * la frescura del dato.</p>
 *
 * <p>La clave {@code jiraKey} (ej: PROJ-123) es única y es el identificador
 * natural usado para la importación idempotente.</p>
 */
@Entity
@Table(
        name = "jira_issue",
        uniqueConstraints = @UniqueConstraint(columnNames = "jira_key", name = "uk_jira_issue_key")
)
@Comment("Cache de issues importadas desde Jira Server / Data Center")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraIssue extends BaseEntity {

    // ── Identificación Jira ───────────────────────────────────────────────────

    @Comment("Clave única del issue en Jira (ej: PROJ-123)")
    @Column(name = "jira_key", nullable = false, length = 50)
    private String jiraKey;

    @Comment("ID del issue en el sistema Jira (numérico interno)")
    @Column(name = "jira_id", length = 50)
    private String jiraId;

    // ── Relaciones KAOS ───────────────────────────────────────────────────────

    @Comment("Squad propietario de este issue")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @Comment("Sprint KAOS al que está vinculada este issue (nullable si no asignada)")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @Comment("Tarea KAOS generada a partir de este issue (nullable si aún no importada)")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_id")
    private Tarea tarea;

    // ── Jerarquía ─────────────────────────────────────────────────────────────

    @Comment("Clave del issue padre en Jira — relleno si es sub-task o sub-issue")
    @Column(name = "parent_key", length = 50)
    private String parentKey;

    // ── Contenido del issue ───────────────────────────────────────────────────

    @Comment("Título del issue (campo summary en Jira)")
    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Comment("Descripción completa del issue")
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Comment("Tipo de issue en Jira: Story / Task / Bug / Sub-task / Spike")
    @Column(name = "tipo_jira", nullable = false, length = 50)
    private String tipoJira;

    // ── Estado ────────────────────────────────────────────────────────────────

    @Comment("Estado del issue en Jira (ej: In Progress, Done)")
    @Column(name = "estado_jira", nullable = false, length = 50)
    private String estadoJira;

    @Comment("Estado mapeado en KAOS (mediante JiraConfig.mapeoEstados)")
    @Column(name = "estado_kaos", length = 20)
    private String estadoKaos;

    // ── Asignación ────────────────────────────────────────────────────────────

    @Comment("assignee.key del issue en Jira (siempre guardado)")
    @Column(name = "asignado_jira", length = 100)
    private String asignadoJira;

    @Comment("Persona KAOS mapeada por idJira del assignee")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    // ── Estimación y tiempo ───────────────────────────────────────────────────

    @Comment("Estimación original en horas (timeoriginalestimate / 3600)")
    @Column(name = "estimacion_horas", precision = 6, scale = 2)
    private BigDecimal estimacionHoras;

    @Comment("Suma de horas de worklogs importados")
    @Column(name = "horas_consumidas", precision = 6, scale = 2)
    private BigDecimal horasConsumidas;

    // ── Clasificación ─────────────────────────────────────────────────────────

    @Comment("Categoría derivada del board de origen: CORRECTIVO / EVOLUTIVO")
    @Column(name = "categoria", length = 20)
    private String categoria;

    @Comment("Prioridad en Jira: Highest / High / Medium / Low / Lowest")
    @Column(name = "prioridad_jira", length = 20)
    private String prioridadJira;

    // ── Control de sync ───────────────────────────────────────────────────────

    @Comment("Timestamp de la última sincronización de este issue")
    @Column(name = "ultima_sync")
    private LocalDateTime ultimaSync;
}
