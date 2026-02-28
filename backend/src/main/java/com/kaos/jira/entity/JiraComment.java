package com.kaos.jira.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.persona.entity.Persona;
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
 * Cache local de comentarios importados desde Jira.
 *
 * <p>Cada comentario se vincula a un {@link JiraIssue} y se identifica
 * de forma única por {@code jiraCommentId} para upsert idempotente.</p>
 */
@Entity
@Table(
        name = "jira_comment",
        uniqueConstraints = @UniqueConstraint(columnNames = "jira_comment_id", name = "uk_jira_comment_id")
)
@Comment("Cache de comentarios importados desde Jira Server / Data Center")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraComment extends BaseEntity {

    @Comment("ID del comentario en Jira (comment.id). Único para upsert.")
    @Column(name = "jira_comment_id", length = 50)
    private String jiraCommentId;

    @Comment("Issue KAOS a la que pertenece este comentario")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_issue_id", nullable = false)
    private JiraIssue jiraIssue;

    @Comment("author.key del comentario en Jira")
    @Column(name = "autor_jira", nullable = false, length = 100)
    private String autorJira;

    @Comment("Persona KAOS mapeada por author.key")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @Comment("Cuerpo/texto del comentario")
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Comment("Fecha de creación del comentario en Jira")
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Comment("Fecha de última actualización del comentario en Jira")
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
