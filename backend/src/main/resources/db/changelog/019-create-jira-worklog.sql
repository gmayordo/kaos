--liquibase formatted sql

--changeset maxwell:019 labels:jira
--comment: Tabla cache de imputaciones (worklogs) importadas de Jira o registradas en KAOS

CREATE TABLE jira_worklog (
    id               BIGSERIAL     PRIMARY KEY,
    -- Identificación externa
    jira_worklog_id  VARCHAR(50),
    -- Relaciones
    jira_issue_id    BIGINT        NOT NULL,
    persona_id       BIGINT,
    -- Autoría
    autor_jira       VARCHAR(100)  NOT NULL,
    -- Tiempo
    fecha            DATE          NOT NULL,
    horas            DECIMAL(6, 2) NOT NULL,
    comentario       TEXT,
    -- Control de origen
    origen           VARCHAR(20)   NOT NULL DEFAULT 'JIRA',
    sincronizado     BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Campos de auditoría (BaseEntity)
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(100),
    -- Constraints
    CONSTRAINT uk_jira_worklog_id       UNIQUE (jira_worklog_id),
    CONSTRAINT fk_jira_worklog_issue    FOREIGN KEY (jira_issue_id) REFERENCES jira_issue (id),
    CONSTRAINT fk_jira_worklog_persona  FOREIGN KEY (persona_id)    REFERENCES persona (id),
    CONSTRAINT chk_jira_worklog_origen  CHECK (origen IN ('JIRA', 'KAOS'))
);

COMMENT ON TABLE jira_worklog IS 'Cache de imputaciones de horas importadas de Jira o registradas en KAOS';
COMMENT ON COLUMN jira_worklog.jira_worklog_id IS 'ID del worklog en Jira (worklog.id). Nullable para worklogs KAOS no sincronizados.';
COMMENT ON COLUMN jira_worklog.jira_issue_id IS 'Issue KAOS a la que pertenece este worklog';
COMMENT ON COLUMN jira_worklog.persona_id IS 'Persona KAOS mapeada por author.key de Jira';
COMMENT ON COLUMN jira_worklog.autor_jira IS 'author.key del worklog en Jira (siempre guardado)';
COMMENT ON COLUMN jira_worklog.fecha IS 'Fecha del worklog (campo started de Jira, solo parte de fecha)';
COMMENT ON COLUMN jira_worklog.horas IS 'Horas imputadas (timeSpentSeconds / 3600)';
COMMENT ON COLUMN jira_worklog.comentario IS 'Comentario del worklog';
COMMENT ON COLUMN jira_worklog.origen IS 'Origen del worklog: JIRA (importado) / KAOS (registrado aquí)';
COMMENT ON COLUMN jira_worklog.sincronizado IS 'Si el worklog ya fue enviado a Jira (aplica solo a origen=KAOS)';

CREATE INDEX idx_worklog_issue_fecha   ON jira_worklog (jira_issue_id, fecha);
CREATE INDEX idx_worklog_persona_fecha ON jira_worklog (persona_id, fecha);
CREATE INDEX idx_worklog_origen_sync   ON jira_worklog (origen, sincronizado);

--rollback DROP TABLE jira_worklog;
