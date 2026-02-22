package com.kaos.persona.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.Comment;
import com.kaos.common.model.BaseEntity;
import com.kaos.horario.entity.PerfilHorario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Miembro del equipo de desarrollo.
 */
@Entity
@Table(name = "persona")
@Comment("Miembros del equipo de desarrollo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Persona extends BaseEntity {

    @Comment("Nombre completo de la persona")
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Comment("Email corporativo (único)")
    @Column(name = "email", nullable = false, unique = true, length = 200)
    private String email;

    @Comment("Usuario Jira (ej: gmayordo)")
    @Column(name = "id_jira", unique = true, length = 100)
    private String idJira;

    @Comment("Perfil de horario asignado")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_horario_id", nullable = false)
    private PerfilHorario perfilHorario;

    @Comment("Ciudad de la persona para calendario laboral")
    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;

    @Comment("Nivel de seniority: JUNIOR, MID, SENIOR, LEAD")
    @Enumerated(EnumType.STRING)
    @Column(name = "seniority", length = 20)
    private Seniority seniority;

    @Comment("Skills separadas por coma")
    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Comment("Coste por hora (opcional)")
    @Column(name = "coste_hora", precision = 8, scale = 2)
    private BigDecimal costeHora;

    @Comment("Estado activo/inactivo de la persona")
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Comment("Fecha de incorporación al equipo")
    @Column(name = "fecha_incorporacion")
    private LocalDate fechaIncorporacion;

    @Comment("Flag para envío de notificaciones por email")
    @Column(name = "send_notifications", nullable = false)
    @Builder.Default
    private Boolean sendNotifications = true;
}
