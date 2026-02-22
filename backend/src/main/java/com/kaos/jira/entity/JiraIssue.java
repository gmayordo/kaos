package com.kaos.jira.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.planificacion.entity.Sprint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import com.kaos.squad.entity.Squad;
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
 * Issue importada desde Jira.
 * Almacena la información básica de cada issue sincronizada.
 */
@Entity
@Table(name = "jira_issue")
@Comment("Issues importadas desde Jira")
 * Issue importado desde Jira.
 * El campo {@code subtipoJira} se calcula al importar: para sub-tasks
 * (parentKey != null) se detecta comparando el summary contra los
 * patrones definidos en {@link JiraIssueTypeConfig}.
 */
@Entity
@Table(name = "jira_issue", indexes = {
    @Index(columnList = "squad_id",   name = "idx_jira_issue_squad"),
    @Index(columnList = "parent_key", name = "idx_jira_issue_parent"),
    @Index(columnList = "estado",     name = "idx_jira_issue_estado")
})
@Comment("Issues importados desde Jira por squad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraIssue extends BaseEntity {

    @Comment("Clave única del issue en Jira (ej: RED-123)")
    @Column(name = "jira_key", nullable = false, unique = true, length = 50)
    private String jiraKey;

    @Comment("Título o resumen del issue")
    @Column(name = "titulo", nullable = false, length = 500)
    private String titulo;

    @Comment("Tipo de issue (ej: Story, Bug, Task)")
    @Column(name = "tipo", length = 50)
    private String tipo;

    @Comment("Estado actual del issue en Jira")
    @Column(name = "estado", length = 50)
    private String estado;

    @Comment("Configuración Jira con la que fue importado")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private JiraConfig config;

    @Comment("Sprint KAOS al que está vinculado (null si no hay sprint activo)")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;
    @Comment("Squad al que pertenece el issue")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @Comment("Clave del issue en Jira (ej: KAOS-123)")
    @Column(name = "issue_key", nullable = false, length = 50)
    private String issueKey;

    @Comment("Título/resumen del issue en Jira")
    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Comment("Tipo en Jira: Sub-task, Story, Bug, Task...")
    @Column(name = "tipo_jira", length = 50)
    private String tipoJira;

    @Comment("Categoría kaos: CORRECTIVO o EVOLUTIVO")
    @Column(name = "categoria", length = 30)
    private String categoria;

    @Comment("Estado actual en Jira (ej: In Progress, Done)")
    @Column(name = "estado", length = 100)
    private String estado;

    @Comment("Clave del issue padre (null si no es sub-task)")
    @Column(name = "parent_key", length = 50)
    private String parentKey;

    @Comment("Subtipo detectado: DESARROLLO | JUNIT | DOCUMENTACION | OTROS (solo para sub-tasks)")
    @Column(name = "subtipo_jira", length = 30)
    private String subtipoJira;
}
