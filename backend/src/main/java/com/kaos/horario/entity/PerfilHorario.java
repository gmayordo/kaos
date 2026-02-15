package com.kaos.horario.entity;

import java.math.BigDecimal;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Formula;
import com.kaos.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Perfil de horario laboral por ubicación geográfica.
 * Define las horas laborables de cada día de la semana y la zona horaria.
 */
@Entity
@Table(name = "perfil_horario")
@Comment("Perfiles de horario laboral configurables por ubicación")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PerfilHorario extends BaseEntity {

    @Comment("Nombre del perfil (ej: España, Chile)")
    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Comment("Zona horaria IANA (ej: Europe/Madrid)")
    @Column(name = "zona_horaria", nullable = false, length = 50)
    private String zonaHoraria;

    @Comment("Horas laborables lunes")
    @Column(name = "horas_lunes", nullable = false, precision = 4, scale = 2)
    private BigDecimal horasLunes;

    @Comment("Horas laborables martes")
    @Column(name = "horas_martes", nullable = false, precision = 4, scale = 2)
    private BigDecimal horasMartes;

    @Comment("Horas laborables miércoles")
    @Column(name = "horas_miercoles", nullable = false, precision = 4, scale = 2)
    private BigDecimal horasMiercoles;

    @Comment("Horas laborables jueves")
    @Column(name = "horas_jueves", nullable = false, precision = 4, scale = 2)
    private BigDecimal horasJueves;

    @Comment("Horas laborables viernes")
    @Column(name = "horas_viernes", nullable = false, precision = 4, scale = 2)
    private BigDecimal horasViernes;

    /**
     * Total semanal calculado (suma L-V).
     * No es columna en BD — se calcula mediante @Formula.
     */
    @Formula("horas_lunes + horas_martes + horas_miercoles + horas_jueves + horas_viernes")
    private BigDecimal totalSemanal;
}
