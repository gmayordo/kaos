package com.kaos.jira.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Estado de sincronización de un proyecto Jira.
 * Guarda la fecha de la última sincronización exitosa para permitir cargas incrementales.
 */
@Entity
@Table(name = "jira_sync_status", uniqueConstraints = {
    @UniqueConstraint(columnNames = "project_key", name = "uq_jira_sync_status_project_key")
})
@Comment("Estado de la última sincronización exitosa por proyecto Jira")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraSyncStatus extends BaseEntity {

    @Comment("Clave del proyecto Jira (ej: BACK, FRONT)")
    @Column(name = "project_key", nullable = false, length = 50)
    private String projectKey;

    @Comment("Fecha y hora de la última sincronización exitosa; null si nunca se ha sincronizado")
    @Column(name = "ultima_sync")
    private LocalDateTime ultimaSync;
}
