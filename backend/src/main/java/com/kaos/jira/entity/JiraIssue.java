package com.kaos.jira.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.planificacion.entity.Sprint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
}
