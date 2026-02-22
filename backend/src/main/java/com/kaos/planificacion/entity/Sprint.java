package com.kaos.planificacion.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.squad.entity.Squad;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Sprint: Ciclo de planificación de 2 semanas.
 * Cada sprint pertenece a un squad y contiene tareas planificadas.
 */
@Entity
@Table(name = "sprint", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"squad_id", "fecha_inicio"}, name = "uk_sprint_squad_fecha")
})
@Comment("Sprints de planificación (ciclos de 2 semanas)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Sprint extends BaseEntity {

    @Comment("Nombre del sprint (ej: RED-Sprint-1)")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Comment("Squad asignado al sprint")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @Comment("Fecha de inicio (lunes)")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Comment("Fecha de fin (domingo, 13 días después del inicio)")
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Comment("Objetivo del sprint")
    @Column(name = "objetivo", columnDefinition = "TEXT", length = 1000)
    private String objetivo;

    @Comment("Estado: PLANIFICACION, ACTIVO, CERRADO")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private SprintEstado estado;

    @Comment("Capacidad total calculada del squad (horas)")
    @Column(name = "capacidad_total", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal capacidadTotal;

    @Comment("Tareas asignadas al sprint")
    @OneToMany(mappedBy = "sprint", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<Tarea> tareas = new ArrayList<>();
}
