package com.kaos.jira.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
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
 * Cache local de enlaces remotos (remote links) importados desde Jira.
 *
 * <p>Cada remote link se vincula a un {@link JiraIssue} y se identifica
 * de forma única por {@code jiraLinkId} para upsert idempotente.</p>
 */
@Entity
@Table(
        name = "jira_remote_link",
        uniqueConstraints = @UniqueConstraint(columnNames = "jira_link_id", name = "uk_jira_remote_link_id")
)
@Comment("Cache de enlaces remotos (remote links) importados desde Jira")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraRemoteLink extends BaseEntity {

    @Comment("ID del remote link en Jira. Único para upsert.")
    @Column(name = "jira_link_id", length = 50)
    private String jiraLinkId;

    @Comment("Issue KAOS a la que pertenece este enlace")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_issue_id", nullable = false)
    private JiraIssue jiraIssue;

    @Comment("URL del enlace remoto")
    @Column(name = "url", nullable = false, length = 2000)
    private String url;

    @Comment("Título/etiqueta del enlace mostrado en Jira")
    @Column(name = "titulo", length = 500)
    private String titulo;

    @Comment("Resumen/descripción del enlace")
    @Column(name = "resumen", length = 1000)
    private String resumen;

    @Comment("URL del icono del enlace (si lo proporciona Jira)")
    @Column(name = "icono_url", length = 2000)
    private String iconoUrl;

    @Comment("Tipo de relación (mentioned in, is caused by, etc.)")
    @Column(name = "relacion", length = 100)
    private String relacion;
}
