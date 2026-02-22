--liquibase formatted sql

--changeset maxwell:017 labels:jira
--comment: Crear tabla jira_issue para issues importados desde Jira

CREATE TABLE jira_issue (
    id              BIGSERIAL       PRIMARY KEY,
    squad_id        BIGINT          NOT NULL,
    issue_key       VARCHAR(50)     NOT NULL,
    summary         VARCHAR(500)    NOT NULL,
    tipo_jira       VARCHAR(50),
    categoria       VARCHAR(30),
    estado          VARCHAR(100),
    parent_key      VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    CONSTRAINT fk_jira_issue_squad FOREIGN KEY (squad_id) REFERENCES squad(id) ON DELETE CASCADE,
    CONSTRAINT uk_jira_issue_key   UNIQUE (squad_id, issue_key)
);

CREATE INDEX idx_jira_issue_squad    ON jira_issue(squad_id);
CREATE INDEX idx_jira_issue_parent   ON jira_issue(parent_key);
CREATE INDEX idx_jira_issue_estado   ON jira_issue(estado);

COMMENT ON TABLE jira_issue IS 'Issues importados desde Jira por squad';
COMMENT ON COLUMN jira_issue.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN jira_issue.squad_id IS 'Squad al que pertenece el issue';
COMMENT ON COLUMN jira_issue.issue_key IS 'Clave del issue en Jira (ej: KAOS-123)';
COMMENT ON COLUMN jira_issue.summary IS 'Título/resumen del issue en Jira';
COMMENT ON COLUMN jira_issue.tipo_jira IS 'Tipo en Jira: Sub-task, Story, Bug, Task...';
COMMENT ON COLUMN jira_issue.categoria IS 'Categoría kaos: CORRECTIVO o EVOLUTIVO';
COMMENT ON COLUMN jira_issue.estado IS 'Estado actual en Jira (ej: In Progress, Done)';
COMMENT ON COLUMN jira_issue.parent_key IS 'Clave del issue padre (null si no es sub-task)';
COMMENT ON COLUMN jira_issue.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN jira_issue.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN jira_issue.created_by IS 'Usuario que creó el registro';

--rollback DROP TABLE IF EXISTS jira_issue CASCADE;
