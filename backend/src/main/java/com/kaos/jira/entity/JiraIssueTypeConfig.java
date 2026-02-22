package com.kaos.jira.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Configuración de tipos/sub-tipos de issue Jira por squad.
 * Permite definir, para cada tipo de issue Jira, qué sub-tipo kaos corresponde
 * según el patrón detectado en el summary, los estados válidos y el estado final.
 */
@Entity
@Table(name = "jira_issue_type_config", indexes = {
    @Index(columnList = "squad_id", name = "idx_jira_type_config_squad"),
    @Index(columnList = "activa",   name = "idx_jira_type_config_activa")
})
@Comment("Configuración de tipos/sub-tipos de issue Jira por squad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraIssueTypeConfig extends BaseEntity {

    @Comment("Squad al que aplica esta configuración")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @Comment("Tipo de issue en Jira: Sub-task, Story, Bug...")
    @Column(name = "tipo_jira", nullable = false, length = 50)
    private String tipoJira;

    @Comment("Subtipo kaos: DESARROLLO, JUNIT, DOCUMENTACION, OTROS")
    @Column(name = "subtipo_kaos", length = 30)
    private String subtipoKaos;

    @Comment("Regex o prefijo para detectar subtipo en el summary del issue")
    @Column(name = "patron_nombre", length = 200)
    private String patronNombre;

    @Comment("JSON con estados Jira válidos para este tipo (ej: [\"In Progress\",\"Done\"])")
    @Column(name = "estados_validos", columnDefinition = "TEXT")
    private String estadosValidos;

    @Comment("Estado Jira que indica que el issue está completado")
    @Column(name = "estado_final", length = 50)
    private String estadoFinal;

    @Comment("Si este tipo cuenta para la capacidad del sprint")
    @Column(name = "contabilizar_cap", nullable = false)
    @Builder.Default
    private Boolean contabilizarCap = true;

    @Comment("Si esta configuración está activa")
    @Column(name = "activa", nullable = false)
    @Builder.Default
    private Boolean activa = true;
}
