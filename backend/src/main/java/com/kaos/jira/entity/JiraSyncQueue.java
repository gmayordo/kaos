package com.kaos.jira.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.kaos.squad.entity.Squad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cola persistente de operaciones Jira pendientes de ejecutar.
 *
 * <p>Cuando el rate limiter detecta que la cuota de 200 calls/2h está agotada,
 * las operaciones se encolan aquí. El {@code JiraBatchScheduler} las retoma
 * cada 30 minutos cuando se libera cuota disponible.</p>
 *
 * <p>Soporta reintentos automáticos hasta {@code maxIntentos} con delay
 * exponencial gestionado por {@code programadaPara}.</p>
 */
@Entity
@Table(name = "jira_sync_queue")
@Comment("Cola de operaciones Jira pendientes por cuota agotada o error transitorio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class JiraSyncQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Squad propietario ────────────────────────────────────────────────────

    @Comment("Squad al que pertenece esta operación pendiente")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    // ── Tipo y payload ───────────────────────────────────────────────────────

    @Comment("Tipo de operación: SYNC_ISSUES / SYNC_WORKLOGS / POST_WORKLOG / SYNC_COMMENTS")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacion", nullable = false, length = 50)
    private TipoOperacion tipoOperacion;

    @Comment("JSON con los parámetros específicos de la operación (startAt, issueKey, etc.)")
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    // ── Estado y reintentos ──────────────────────────────────────────────────

    @Comment("Estado actual de la operación en la cola")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoOperacion estado = EstadoOperacion.PENDIENTE;

    @Comment("Número de intentos realizados hasta el momento")
    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private int intentos = 0;

    @Comment("Máximo de reintentos permitidos antes de marcar como ERROR definitivo")
    @Column(name = "max_intentos", nullable = false)
    @Builder.Default
    private int maxIntentos = 3;

    // ── Control temporal ─────────────────────────────────────────────────────

    @Comment("No ejecutar esta operación antes de esta fecha (implementa retry delay)")
    @Column(name = "programada_para")
    private LocalDateTime programadaPara;

    @Comment("Timestamp de la última ejecución intentada")
    @Column(name = "ejecutada_at")
    private LocalDateTime ejecutadaAt;

    @Comment("Último mensaje de error registrado (para diagnóstico)")
    @Column(name = "error_mensaje", columnDefinition = "TEXT")
    private String errorMensaje;

    // ── Auditoría ────────────────────────────────────────────────────────────

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Enums ────────────────────────────────────────────────────────────────

    /** Tipo de operación encolada. */
    public enum TipoOperacion {
        SYNC_ISSUES,
        SYNC_WORKLOGS,
        POST_WORKLOG,
        SYNC_COMMENTS
    }

    /** Estado del ciclo de vida de la operación en la cola. */
    public enum EstadoOperacion {
        PENDIENTE,
        EN_PROGRESO,
        COMPLETADA,
        ERROR
    }

    // ── Métodos de negocio ───────────────────────────────────────────────────

    /** Devuelve {@code true} si la operación puede reintentarse. */
    public boolean puedeReintentar() {
        return intentos < maxIntentos && estado != EstadoOperacion.COMPLETADA;
    }

    /** Incrementa el contador de intentos. */
    public void registrarIntento() {
        this.intentos++;
        this.ejecutadaAt = LocalDateTime.now();
    }
}
