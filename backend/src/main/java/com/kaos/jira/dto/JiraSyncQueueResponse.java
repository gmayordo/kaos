package com.kaos.jira.dto;

import java.time.LocalDateTime;
import com.kaos.jira.entity.JiraSyncQueue.EstadoOperacion;
import com.kaos.jira.entity.JiraSyncQueue.TipoOperacion;

/**
 * DTO de respuesta con el detalle de una operación encolada en la cola Jira.
 */
public record JiraSyncQueueResponse(

        Long id,
        Long squadId,
        String squadNombre,

        TipoOperacion tipoOperacion,
        EstadoOperacion estado,

        int intentos,
        int maxIntentos,

        LocalDateTime programadaPara,
        LocalDateTime ejecutadaAt,
        String errorMensaje,
        LocalDateTime createdAt
) {}
