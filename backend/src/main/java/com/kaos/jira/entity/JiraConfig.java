package com.kaos.jira.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.squad.entity.Squad;
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
 * Configuración de integración con Jira para un squad.
 * Almacena las credenciales y parámetros de conexión al board de Jira.
 */
@Entity
@Table(name = "jira_config")
@Comment("Configuración de integración Jira por squad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraConfig extends BaseEntity {

    @Comment("Squad al que pertenece esta configuración")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @Comment("URL base de la instancia Jira (ej: https://myorg.atlassian.net)")
    @Column(name = "jira_url", nullable = false, length = 255)
    private String jiraUrl;

    @Comment("Token de API para autenticación en Jira")
    @Column(name = "api_token", nullable = false, length = 500)
    private String apiToken;

    @Comment("Correo del usuario Jira asociado al token")
    @Column(name = "usuario_email", length = 255)
    private String usuarioEmail;

    @Comment("ID del board de evolutivos en Jira")
    @Column(name = "board_evolutivo_id")
    private Long boardEvolutivoId;

    @Comment("ID del board de correctivos en Jira")
    @Column(name = "board_correctivo_id")
    private Long boardCorrectivoId;

    @Comment("Clave del proyecto Jira del squad (ej: RED, GREEN)")
    @Column(name = "project_key", length = 20)
    private String projectKey;
}
