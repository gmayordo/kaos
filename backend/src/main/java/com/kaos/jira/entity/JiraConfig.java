package com.kaos.jira.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Configuración de un proyecto Jira a sincronizar.
 * Define la clave de proyecto y el tipo de sincronización.
 */
@Entity
@Table(name = "jira_config", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_key", "tipo"}, name = "uq_jira_config_project_tipo")
})
@Comment("Configuración de proyectos Jira a sincronizar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraConfig extends BaseEntity {

    @Comment("Clave del proyecto Jira (ej: BACK, FRONT)")
    @Column(name = "project_key", nullable = false, length = 50)
    private String projectKey;

    @Comment("Tipo de sincronización: EVOLUTIVO (sprints abiertos) o CORRECTIVO (backlog/bugs)")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoSincronizacion tipo;

    @Comment("Indica si esta configuración está activa")
    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
