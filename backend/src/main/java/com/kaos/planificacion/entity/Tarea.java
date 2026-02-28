package com.kaos.planificacion.entity;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.persona.entity.Persona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Tarea: Unidad de trabajo dentro de un sprint.
 * Cada tarea puede ser asignada a una persona y tiene un estado en la máquina de estados.
 */
@Entity
@Table(name = "tarea", indexes = {
    @Index(columnList = "sprint_id", name = "idx_tarea_sprint"),
    @Index(columnList = "persona_id", name = "idx_tarea_persona"),
    @Index(columnList = "estado", name = "idx_tarea_estado"),
    @Index(columnList = "sprint_id,persona_id,estado", name = "idx_tarea_sprint_persona_estado")
})
@Comment("Tareas dentro de sprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Tarea extends BaseEntity {

    @Comment("Sprint al que pertenece la tarea")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @Comment("Título descriptivo de la tarea")
    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Comment("Tipo de tarea: HISTORIA, TAREA, BUG, SPIKE")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoTarea tipo;

    @Comment("Categoría: CORRECTIVO o EVOLUTIVO")
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 20)
    private Categoria categoria;

    @Comment("Estimación de horas para completar (0.5 a 40)")
    @Column(name = "estimacion", columnDefinition = "DECIMAL(10,2)", nullable = false)
    private BigDecimal estimacion;

    @Comment("Prioridad: BAJA, NORMAL, ALTA, BLOQUEANTE")
    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 20)
    private Prioridad prioridad;

    @Comment("Estado: PENDIENTE, EN_PROGRESO, BLOQUEADO, COMPLETADA")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTarea estado;

    @Comment("Persona asignada a la tarea")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @Comment("Día del sprint (1=L, 2=M, ..., 10=V semana 2), null si sin asignar")
    @Column(name = "dia_asignado")
    private Integer diaAsignado;

    @Comment("Bloqueos que afectan esta tarea")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "bloqueo_tarea",
        joinColumns = @JoinColumn(name = "tarea_id"),
        inverseJoinColumns = @JoinColumn(name = "bloqueo_id")
    )
    private Set<Bloqueo> bloqueadores = new HashSet<>();

    // ── Integración Jira (añadido en Bloque 4) ───────────────────────────────────

    @Comment("Clave del issue en Jira vinculado a esta tarea (ej: PROJ-123)")
    @Column(name = "jira_key", length = 50)
    private String jiraKey;

    @Comment("Cache del issue Jira del que procede esta tarea")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jira_issue_id")
    private com.kaos.jira.entity.JiraIssue jiraIssue;

    @Comment("Indica si la tarea fue generada por importación desde Jira")
    @Column(name = "es_de_jira", nullable = false)
    private boolean esDeJira = false;

    @Comment("Tarea padre KAOS — si existe, esta tarea es subtarea derivada de un issue hijo en Jira")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_parent_id")
    private Tarea tareaParent;
}
