--liquibase formatted sql

--changeset maxwell:022 labels:jira
--comment: Cola de operaciones Jira pendientes por cuota agotada o error transitorio

CREATE TABLE jira_sync_queue (
    id               BIGSERIAL    NOT NULL,
    -- Squad propietario
    squad_id         BIGINT       NOT NULL,
    -- Tipo y payload
    tipo_operacion   VARCHAR(50)  NOT NULL,
    payload          TEXT,
    -- Estado y reintentos
    estado           VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    intentos         INT          NOT NULL DEFAULT 0,
    max_intentos     INT          NOT NULL DEFAULT 3,
    -- Control temporal
    programada_para  TIMESTAMP,
    ejecutada_at     TIMESTAMP,
    error_mensaje    TEXT,
    -- Auditoría
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Constraints
    CONSTRAINT pk_jira_sync_queue     PRIMARY KEY (id),
    CONSTRAINT fk_sync_queue_squad    FOREIGN KEY (squad_id) REFERENCES squad (id),
    CONSTRAINT chk_sync_queue_tipo    CHECK (tipo_operacion IN ('SYNC_ISSUES', 'SYNC_WORKLOGS', 'POST_WORKLOG', 'SYNC_COMMENTS')),
    CONSTRAINT chk_sync_queue_estado  CHECK (estado IN ('PENDIENTE', 'EN_PROGRESO', 'COMPLETADA', 'ERROR'))
);

COMMENT ON TABLE jira_sync_queue IS 'Cola persistente de operaciones Jira pendientes: se encolan cuando la cuota API está agotada o hay errores transitorios';
COMMENT ON COLUMN jira_sync_queue.tipo_operacion IS 'SYNC_ISSUES: importar issues; SYNC_WORKLOGS: importar worklogs; POST_WORKLOG: enviar imputación a Jira; SYNC_COMMENTS: sincronizar comentarios';
COMMENT ON COLUMN jira_sync_queue.payload IS 'JSON con parámetros específicos de la operación (startAt de paginación, issueKey, etc.)';
COMMENT ON COLUMN jira_sync_queue.estado IS 'PENDIENTE: esperando ejecución; EN_PROGRESO: procesándose; COMPLETADA: OK; ERROR: fallida';
COMMENT ON COLUMN jira_sync_queue.intentos IS 'Número de intentos realizados';
COMMENT ON COLUMN jira_sync_queue.max_intentos IS 'Máximo de reintentos antes de marcar como ERROR definitivo (default 3)';
COMMENT ON COLUMN jira_sync_queue.programada_para IS 'No ejecutar antes de esta fecha — implementa retry delay exponencial';
COMMENT ON COLUMN jira_sync_queue.ejecutada_at IS 'Timestamp del último intento de ejecución';
COMMENT ON COLUMN jira_sync_queue.error_mensaje IS 'Último mensaje de error para diagnóstico';

CREATE INDEX idx_sync_queue_squad_estado   ON jira_sync_queue (squad_id, estado);
CREATE INDEX idx_sync_queue_programada     ON jira_sync_queue (programada_para, estado);

--rollback DROP TABLE jira_sync_queue;
