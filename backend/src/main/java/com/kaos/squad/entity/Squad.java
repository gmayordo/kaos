package com.kaos.squad.entity;

import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Equipo de desarrollo (squad).
 * Cada squad tiene un nombre, estado, y referencias a boards de Jira.
 */
@Entity
@Table(name = "squad")
@Comment("Equipos de desarrollo (squads)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Squad extends BaseEntity {

    @Comment("Nombre del squad (ej: red, green, blue)")
    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Comment("Descripción del squad")
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Comment("Estado del squad: ACTIVO o INACTIVO")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoSquad estado = EstadoSquad.ACTIVO;

    @Comment("ID del board de correctivos en Jira")
    @Column(name = "id_squad_corr_jira", length = 100)
    private String idSquadCorrJira;

    @Comment("ID del board de evolutivos en Jira")
    @Column(name = "id_squad_evol_jira", length = 100)
    private String idSquadEvolJira;
}
