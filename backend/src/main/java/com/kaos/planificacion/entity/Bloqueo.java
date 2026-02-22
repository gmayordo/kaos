package com.kaos.planificacion.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.persona.entity.Persona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Bloqueo: Impedimento que puede afectar a una o más tareas.
 */
@Entity
@Table(name = "bloqueo", indexes = {
    @Index(columnList = "estado", name = "idx_bloqueo_estado"),
    @Index(columnList = "created_at DESC", name = "idx_bloqueo_createdAt")
})
@Comment("Bloqueos e impedimentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Bloqueo extends BaseEntity {

    @Comment("Título del bloqueo")
    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Comment("Descripción detallada del impedimento")
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Comment("Tipo: DEPENDENCIA_EXTERNA, RECURSO, TECNICO, COMUNICACION, OTRO")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoBloqueo tipo;

    @Comment("Estado: ABIERTO, EN_GESTION, RESUELTO")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoBloqueo estado;

    @Comment("Persona responsable de resolver el bloqueo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Persona responsable;

    @Comment("Fecha de resolución (null si aún abierto)")
    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Comment("Notas y historial de actualizaciones")
    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Comment("Tareas afectadas por este bloqueo")
    @ManyToMany(mappedBy = "bloqueadores", fetch = FetchType.LAZY)
    private Set<Tarea> tareas = new HashSet<>();
}
