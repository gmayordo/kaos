package com.kaos.jira.dto;

import java.time.LocalDateTime;
import com.kaos.jira.entity.JiraSyncStatus.EstadoSync;

/**
 * DTO de respuesta con el estado de sincronización Jira de un squad.
 *
 * <p>Incluye cuota de API, resultado de la última sync y operaciones pendientes
 * en cola. Utilizado por el widget de estado del frontend.</p>
 */
public record JiraSyncStatusResponse(

        Long squadId,
        String squadNombre,

        /** Estado actual: IDLE / SINCRONIZANDO / ERROR / CUOTA_AGOTADA */
        EstadoSync estado,

        LocalDateTime ultimaSync,
        int issuesImportadas,
        int worklogsImportados,
        int commentsImportados,
        int remoteLinksImportados,

        /** Calls consumidas en la ventana rodante de 2 horas. */
        int callsConsumidas2h,
        /** Calls disponibles hasta el límite de 200/2h. */
        int callsRestantes2h,

        String ultimoError,

        /** Operaciones PENDIENTE en la cola de este squad. */
        int operacionesPendientes,

        LocalDateTime updatedAt
) {}
