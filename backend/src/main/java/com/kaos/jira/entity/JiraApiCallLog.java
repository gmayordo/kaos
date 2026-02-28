package com.kaos.jira.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de cada llamada realizada a la API REST de Jira.
 * Usado por {@link com.kaos.jira.service.JiraRateLimiter} para calcular
 * el consumo de la ventana de 200 llamadas cada 2 horas.
 *
 * <p>No extiende {@link com.kaos.common.model.BaseEntity} para mantener
 * la tabla lo más ligera posible (solo los campos imprescindibles).
 */
@Entity
@Table(name = "jira_api_call_log")
@Comment("Registro de llamadas a la API REST de Jira para control de rate limiting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("Endpoint llamado (ej: /rest/api/2/search)")
    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Comment("Método HTTP utilizado (GET, POST, PUT, etc.)")
    @Column(name = "metodo", nullable = false, length = 10)
    private String metodo;

    @Comment("Código de respuesta HTTP recibido")
    @Column(name = "status_code")
    private Integer statusCode;

    @Comment("Squad que generó la llamada (opcional, para trazabilidad)")
    @Column(name = "squad_id")
    private Long squadId;

    @Comment("Timestamp exacto de la llamada, usado para calcular la ventana de 2 horas")
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
}
