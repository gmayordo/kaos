--liquibase formatted sql

--changeset kaos:018 labels:jira
--comment: Crear tabla jira_sync_status para rastrear la última sincronización exitosa por proyecto

CREATE TABLE jira_sync_status (
    id          BIGSERIAL       PRIMARY KEY,
    project_key VARCHAR(50)     NOT NULL,
    ultima_sync TIMESTAMP,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(100),
    CONSTRAINT uq_jira_sync_status_project_key UNIQUE (project_key)
);

CREATE INDEX idx_jira_sync_status_project_key ON jira_sync_status(project_key);

COMMENT ON TABLE jira_sync_status IS 'Estado de la última sincronización exitosa por proyecto Jira';
COMMENT ON COLUMN jira_sync_status.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN jira_sync_status.project_key IS 'Clave del proyecto Jira (ej: BACK, FRONT)';
COMMENT ON COLUMN jira_sync_status.ultima_sync IS 'Fecha y hora de la última sincronización exitosa; null si nunca se ha sincronizado';
COMMENT ON COLUMN jira_sync_status.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN jira_sync_status.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN jira_sync_status.created_by IS 'Usuario que creó el registro';

--rollback DROP TABLE IF EXISTS jira_sync_status;
