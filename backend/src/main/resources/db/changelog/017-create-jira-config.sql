--liquibase formatted sql

--changeset kaos:017 labels:jira
--comment: Crear tabla jira_config para configuración de proyectos Jira a sincronizar

CREATE TABLE jira_config (
    id          BIGSERIAL       PRIMARY KEY,
    project_key VARCHAR(50)     NOT NULL,
    tipo        VARCHAR(20)     NOT NULL,
    activo      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(100),
    CONSTRAINT uq_jira_config_project_tipo UNIQUE (project_key, tipo),
    CONSTRAINT chk_jira_config_tipo CHECK (tipo IN ('EVOLUTIVO', 'CORRECTIVO'))
);

CREATE INDEX idx_jira_config_project_key ON jira_config(project_key);
CREATE INDEX idx_jira_config_activo ON jira_config(activo);

COMMENT ON TABLE jira_config IS 'Configuración de proyectos Jira a sincronizar';
COMMENT ON COLUMN jira_config.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN jira_config.project_key IS 'Clave del proyecto Jira (ej: BACK, FRONT)';
COMMENT ON COLUMN jira_config.tipo IS 'Tipo de sincronización: EVOLUTIVO o CORRECTIVO';
COMMENT ON COLUMN jira_config.activo IS 'Indica si esta configuración está activa';
COMMENT ON COLUMN jira_config.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN jira_config.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN jira_config.created_by IS 'Usuario que creó el registro';

--rollback DROP TABLE IF EXISTS jira_config;
