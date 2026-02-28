--liquibase formatted sql

--changeset maxwell:023 labels:jira
--comment: Estado de sincronización Jira por squad: una fila por squad con cuota, contadores y último error

CREATE TABLE jira_sync_status (
    id                      BIGSERIAL   NOT NULL,
    -- Squad propietario (UNIQUE: 1 fila por squad)
    squad_id                BIGINT      NOT NULL,
    -- Resultado de la última sync
    ultima_sync             TIMESTAMP,
    issues_importadas       INT         NOT NULL DEFAULT 0,
    worklogs_importados     INT         NOT NULL DEFAULT 0,
    -- Cuota API (ventana de 2 horas)
    calls_consumidas_2h     INT         NOT NULL DEFAULT 0,
    calls_restantes_2h      INT         NOT NULL DEFAULT 200,
    -- Estado y errores
    estado                  VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    ultimo_error            TEXT,
    operaciones_pendientes  INT         NOT NULL DEFAULT 0,
    -- Timestamp última actualización
    updated_at              TIMESTAMP,
    -- Constraints
    CONSTRAINT pk_jira_sync_status       PRIMARY KEY (id),
    CONSTRAINT uk_jira_sync_status_squad UNIQUE (squad_id),
    CONSTRAINT fk_sync_status_squad      FOREIGN KEY (squad_id) REFERENCES squad (id),
    CONSTRAINT chk_sync_status_estado    CHECK (estado IN ('IDLE', 'SINCRONIZANDO', 'ERROR', 'CUOTA_AGOTADA'))
);

COMMENT ON TABLE jira_sync_status IS 'Estado de la última sincronización Jira por squad: la UI consulta este registro para mostrar el widget de estado';
COMMENT ON COLUMN jira_sync_status.squad_id IS 'Squad propietario — un registro por squad (UNIQUE)';
COMMENT ON COLUMN jira_sync_status.ultima_sync IS 'Timestamp de la última sincronización completada con éxito';
COMMENT ON COLUMN jira_sync_status.issues_importadas IS 'Issues importadas en el último ciclo de sync';
COMMENT ON COLUMN jira_sync_status.worklogs_importados IS 'Worklogs importados en el último ciclo de sync';
COMMENT ON COLUMN jira_sync_status.calls_consumidas_2h IS 'Calls API consumidas en la ventana rodante de las últimas 2 horas';
COMMENT ON COLUMN jira_sync_status.calls_restantes_2h IS 'Calls disponibles calculadas como 200 - consumidas (para mostrar en UI)';
COMMENT ON COLUMN jira_sync_status.estado IS 'IDLE: sin sync activa; SINCRONIZANDO: en curso; ERROR: falló; CUOTA_AGOTADA: límite alcanzado';
COMMENT ON COLUMN jira_sync_status.operaciones_pendientes IS 'Número de operaciones PENDIENTE en jira_sync_queue para este squad';

--rollback DROP TABLE jira_sync_status;
