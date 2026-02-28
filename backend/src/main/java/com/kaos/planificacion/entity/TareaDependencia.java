package com.kaos.planificacion.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
 * Dependencia entre dos tareas del sprint.
 *
 * <p>{@code tareaOrigen} debe estar COMPLETADA antes de que {@code tareaDestino}
 * pueda iniciar (si tipo == ESTRICTA). Con tipo SUAVE la relación es solo informativa.</p>
 */
@Entity
@Table(
        name = "tarea_dependencia",
        indexes = {
                @Index(columnList = "tarea_origen_id",  name = "idx_dependencia_origen"),
                @Index(columnList = "tarea_destino_id", name = "idx_dependencia_destino")
        },
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"tarea_origen_id", "tarea_destino_id"},
                name = "uk_tarea_dependencia_par"
        )
)
@Comment("Dependencias entre tareas de un sprint (origen bloquea a destino)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TareaDependencia extends BaseEntity {

    @Comment("Tarea bloqueante — debe completarse antes que la destino")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_origen_id", nullable = false)
    private Tarea tareaOrigen;

    @Comment("Tarea bloqueada — no puede iniciar hasta que origen esté COMPLETADA")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_destino_id", nullable = false)
    private Tarea tareaDestino;

    @Comment("ESTRICTA: bloqueo real | SUAVE: dependencia informativa")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoDependencia tipo;
}
