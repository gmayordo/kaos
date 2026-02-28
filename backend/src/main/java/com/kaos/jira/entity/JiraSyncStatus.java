package com.kaos.jira.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estado de la última sincronización Jira para un squad.
 *
 * <p>Un registro por squad. El frontend lo consulta para mostrar:
 * cuándo fue la última sync, cuota disponible, operaciones pendientes en cola
 * y si hay errores.</p>
 *
 * <p>Se actualiza al inicio y al final de cada ciclo de sincronización
 * (manual o batch).</p>
 */
@Entity
@Table(
        name = "jira_sync_status",
        uniqueConstraints = @UniqueConstraint(columnNames = "squad_id", name = "uk_jira_sync_status_squad")
)
@Comment("Estado de la última sincronización Jira por squad: última ejecución, cuota, errores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraSyncStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Squad ────────────────────────────────────────────────────────────────

    @Comment("Squad propietario — un registro de estado por squad (UNIQUE)")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    // ── Resultado de la última sync ──────────────────────────────────────────

    @Comment("Timestamp de la última sincronización completada con éxito")
    @Column(name = "ultima_sync")
    private LocalDateTime ultimaSync;

    @Comment("Número de issues importadas en el último ciclo de sync")
    @Column(name = "issues_importadas", nullable = false)
    @Builder.Default
    private int issuesImportadas = 0;

    @Comment("Número de worklogs importados en el último ciclo de sync")
    @Column(name = "worklogs_importados", nullable = false)
    @Builder.Default
    private int worklogsImportados = 0;

    @Comment("Número de comentarios importados en el último ciclo de sync")
    @Column(name = "comments_importados", nullable = false)
    @Builder.Default
    private int commentsImportados = 0;

    @Comment("Número de remote links importados en el último ciclo de sync")
    @Column(name = "remote_links_importados", nullable = false)
    @Builder.Default
    private int remoteLinksImportados = 0;

    // ── Cuota API ────────────────────────────────────────────────────────────

    @Comment("Calls consumidas en la ventana rodante de las últimas 2 horas")
    @Column(name = "calls_consumidas_2h", nullable = false)
    @Builder.Default
    private int callsConsumidas2h = 0;

    @Comment("Calls disponibles hasta agotar el límite de 200/2h (calculado: 200 - consumidas)")
    @Column(name = "calls_restantes_2h", nullable = false)
    @Builder.Default
    private int callsRestantes2h = 200;

    // ── Estado y errores ─────────────────────────────────────────────────────

    @Comment("Estado actual del proceso de sync para este squad")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoSync estado = EstadoSync.IDLE;

    @Comment("Último mensaje de error de sincronización (null si OK)")
    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    @Comment("Número de operaciones PENDIENTE en jira_sync_queue para este squad")
    @Column(name = "operaciones_pendientes", nullable = false)
    @Builder.Default
    private int operacionesPendientes = 0;

    // ── Auditoría ────────────────────────────────────────────────────────────

    @Comment("Timestamp de la última actualización de este registro de estado")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Enum ─────────────────────────────────────────────────────────────────

    /** Estados del ciclo de vida de una sincronización. */
    public enum EstadoSync {
        IDLE,
        SINCRONIZANDO,
        ERROR,
        CUOTA_AGOTADA
    }

    // ── Métodos de negocio ───────────────────────────────────────────────────

    /** Actualiza los contadores de cuota a partir del valor actual de calls consumidas. */
    public void actualizarCuota(int consumidas) {
        this.callsConsumidas2h = consumidas;
        this.callsRestantes2h = Math.max(0, 200 - consumidas);
        if (consumidas >= 195) {
            this.estado = EstadoSync.CUOTA_AGOTADA;
        }
    }

    /** Marca la sync como en curso. */
    public void iniciarSync() {
        this.estado = EstadoSync.SINCRONIZANDO;
        this.ultimoError = null;
        this.updatedAt = LocalDateTime.now();
    }

    /** Cierra el ciclo de sync con éxito. */
    public void completarSync(int issues, int worklogs, int comments, int remoteLinks) {
        this.estado = EstadoSync.IDLE;
        this.ultimaSync = LocalDateTime.now();
        this.issuesImportadas = issues;
        this.worklogsImportados = worklogs;
        this.commentsImportados = comments;
        this.remoteLinksImportados = remoteLinks;
        this.ultimoError = null;
        this.updatedAt = LocalDateTime.now();
    }

    /** Registra un error en la sync. */
    public void registrarError(String mensajeError) {
        this.estado = EstadoSync.ERROR;
        this.ultimoError = mensajeError;
        this.updatedAt = LocalDateTime.now();
    }
}
