package com.kaos.jira.alert.entity;

import java.math.BigDecimal;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Regla de alerta de coherencia configurable en BD (DT-45).
 *
 * <p>Cada regla define una condición SpEL que el motor evalúa tras cada
 * sincronización con Jira. Si la condición se cumple, se genera una
 * {@link JiraAlerta} con el mensaje resuelto.</p>
 *
 * <p>Las reglas con {@code squad} nulo se aplican a todos los squads;
 * con squad específico solo al squad indicado.</p>
 */
@Entity
@Table(name = "jira_alert_rule")
@Comment("Reglas de coherencia configurables evaluadas con SpEL tras cada sync Jira")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JiraAlertRule extends BaseEntity {

    // ── Ámbito ─────────────────────────────────────

    @Comment("Squad al que aplica la regla (null = todas los squads)")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id")
    private Squad squad;

    // ── Identificación ──────────────────────────────

    @Comment("Identificador corto legible por humanos, ej: tarea-done-jira-activa-kaos")
    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Comment("Descripción detallada de la regla para documentación interna")
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    // ── Clasificación ────────────────────────────────

    @Comment("Tipo lógico de la regla: ESTADO_INCOHERENTE, IMPUTACION_FALTANTE, etc.")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoAlerta tipo;

    @Comment("Severidad de la alerta generada: CRITICO, AVISO o INFO")
    @Enumerated(EnumType.STRING)
    @Column(name = "severidad", nullable = false, length = 20)
    private Severidad severidad;

    // ── Evaluación ───────────────────────────────────

    @Comment("Expresión SpEL evaluada sobre el contexto del sprint: #issue, #tarea, #persona, #regla")
    @Column(name = "condicion_spel", nullable = false, columnDefinition = "TEXT")
    private String condicionSpel;

    @Comment("Plantilla del mensaje con placeholders: {jiraKey}, {estadoKaos}, {persona}, {valor}")
    @Column(name = "mensaje_template", nullable = false, length = 500)
    private String mensajeTemplate;

    // ── Configuración ────────────────────────────────

    @Comment("Valor umbral configurable sin tocar código; referenciable como #regla.umbralValor en SpEL")
    @Column(name = "umbral_valor", precision = 10, scale = 2)
    private BigDecimal umbralValor;

    @Comment("Permite desactivar la regla sin eliminarla de la BD")
    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    // ── Enums internos ───────────────────────────────

    /** Tipo lógico de alerta. Determina sobre qué colección itera el motor SpEL. */
    public enum TipoAlerta {
        /** Estado Jira y estado KAOS no coinciden. Iteración por issue. */
        ESTADO_INCOHERENTE,
        /** Persona/equipo sin imputar horas durante N días. Iteración por persona. */
        IMPUTACION_FALTANTE,
        /** Horas consumidas superan el umbral de desviación sobre la estimación. */
        DESVIACION_HORAS,
        /** Sprint con desfase tiempo/completitud superior al umbral. Nivel sprint. */
        SPRINT_EN_RIESGO,
        /** Issue sin estimación de horas (null o 0). */
        ESTIMACION_CERO,
        /** Issue en In Progress durante más días que el umbral. */
        TAREA_ESTANCADA,
        /** Regla ad-hoc totalmente controlada por condicion_spel. */
        CUSTOM
    }

    /** Severidad de la alerta generada. */
    public enum Severidad {
        /** Requiere atención inmediata. */
        CRITICO,
        /** Situación anómala que conviene revisar. */
        AVISO,
        /** Información relevante sin urgencia. */
        INFO
    }
}
