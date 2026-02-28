package com.kaos.jira.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.jira.entity.converter.AesEncryptConverter;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Configuración de conexión a Jira por squad.
 * Almacena las credenciales y parámetros de sincronización.
 *
 * <p>El token se almacena cifrado en base de datos usando AES-256/GCM
 * vía {@link AesEncryptConverter}. La clave de cifrado se configura
 * mediante la variable de entorno {@code AES_SECRET_KEY}.
 */
@Entity
@Table(name = "jira_config",
        uniqueConstraints = @UniqueConstraint(columnNames = "squad_id", name = "uk_jira_config_squad"))
@Comment("Configuración de conexión a Jira Server/DC por squad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraConfig extends BaseEntity {

    @Comment("Squad al que pertenece esta configuración de Jira")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @Comment("URL base del servidor Jira (ej: https://jira.empresa.com)")
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Comment("Nombre de usuario de Jira con permisos de lectura")
    @Column(name = "usuario", nullable = false, length = 200)
    private String usuario;

    @Comment("Token de acceso Jira, almacenado cifrado con AES-256/GCM")
    @Convert(converter = AesEncryptConverter.class)
    @Column(name = "token", nullable = false, length = 500)
    private String token;

    @Comment("ID del board Jira para tareas correctivas")
    @Column(name = "board_correctivo_id")
    private Long boardCorrectivoId;

    @Comment("ID del board Jira para tareas evolutivas")
    @Column(name = "board_evolutivo_id")
    private Long boardEvolutivoId;

    @Comment("Mapeo de estados Jira → estados KAOS en formato JSON (ej: {\"Done\":\"COMPLETADA\",\"In Progress\":\"EN_PROGRESO\"})")
    @Column(name = "mapeo_estados", columnDefinition = "TEXT")
    private String mapeoEstados;

    @Comment("Método de carga activo: API_REST | SELENIUM | LOCAL")
    @Column(name = "load_method", nullable = false, length = 20)
    private String loadMethod;

    @Comment("Indica si la configuración está activa")
    @Column(name = "activa", nullable = false)
    private boolean activa = true;
}
