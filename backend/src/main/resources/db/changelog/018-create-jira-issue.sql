--liquibase formatted sql

--changeset maxwell:018 labels:jira
--comment: Crear tabla jira_issue para almacenar issues importadas desde Jira

CREATE TABLE jira_issue (
    id                  BIGSERIAL       PRIMARY KEY,
    jira_key            VARCHAR(50)     NOT NULL UNIQUE,
    titulo              VARCHAR(500)    NOT NULL,
    tipo                VARCHAR(50),
    estado              VARCHAR(50),
    config_id           BIGINT          NOT NULL,
    sprint_id           BIGINT,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    CONSTRAINT fk_jira_issue_config FOREIGN KEY (config_id) REFERENCES jira_config(id) ON DELETE CASCADE,
    CONSTRAINT fk_jira_issue_sprint FOREIGN KEY (sprint_id) REFERENCES sprint(id) ON DELETE SET NULL
);

CREATE INDEX idx_jira_issue_config ON jira_issue(config_id);
CREATE INDEX idx_jira_issue_sprint ON jira_issue(sprint_id);
CREATE INDEX idx_jira_issue_jira_key ON jira_issue(jira_key);

COMMENT ON TABLE jira_issue IS 'Issues importadas desde Jira';
COMMENT ON COLUMN jira_issue.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN jira_issue.jira_key IS 'Clave única del issue en Jira (ej: RED-123)';
COMMENT ON COLUMN jira_issue.titulo IS 'Título o resumen del issue';
COMMENT ON COLUMN jira_issue.tipo IS 'Tipo de issue (ej: Story, Bug, Task)';
COMMENT ON COLUMN jira_issue.estado IS 'Estado actual del issue en Jira';
COMMENT ON COLUMN jira_issue.config_id IS 'Configuración Jira con la que fue importado';
COMMENT ON COLUMN jira_issue.sprint_id IS 'Sprint KAOS al que está vinculado (null si no hay sprint activo)';
COMMENT ON COLUMN jira_issue.created_at IS 'Fecha de creación';
COMMENT ON COLUMN jira_issue.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN jira_issue.created_by IS 'Usuario que creó el registro';

--rollback DROP TABLE IF EXISTS jira_issue CASCADE;
