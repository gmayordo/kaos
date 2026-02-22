--liquibase formatted sql

--changeset maxwell:029 labels:jira
--comment: Crear tabla jira_issue_type_config para configuración de tipos de issue por squad

CREATE TABLE jira_issue_type_config (
    id               BIGSERIAL       PRIMARY KEY,
    squad_id         BIGINT          NOT NULL,
    tipo_jira        VARCHAR(50)     NOT NULL,
    subtipo_kaos     VARCHAR(30),
    patron_nombre    VARCHAR(200),
    estados_validos  TEXT,
    estado_final     VARCHAR(50),
    contabilizar_cap BOOLEAN         NOT NULL DEFAULT true,
    activa           BOOLEAN         NOT NULL DEFAULT true,
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(100),
    CONSTRAINT fk_jira_type_config_squad FOREIGN KEY (squad_id) REFERENCES squad(id) ON DELETE CASCADE,
    CONSTRAINT uk_jira_type_squad        UNIQUE (squad_id, tipo_jira, subtipo_kaos)
);

CREATE INDEX idx_jira_type_config_squad  ON jira_issue_type_config(squad_id);
CREATE INDEX idx_jira_type_config_activa ON jira_issue_type_config(activa);

COMMENT ON TABLE jira_issue_type_config IS 'Configuración de tipos/sub-tipos de issue Jira por squad';
COMMENT ON COLUMN jira_issue_type_config.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN jira_issue_type_config.squad_id IS 'Squad al que aplica esta configuración';
COMMENT ON COLUMN jira_issue_type_config.tipo_jira IS 'Tipo de issue en Jira: Sub-task, Story, Bug...';
COMMENT ON COLUMN jira_issue_type_config.subtipo_kaos IS 'Subtipo kaos: DESARROLLO, JUNIT, DOCUMENTACION, OTROS';
COMMENT ON COLUMN jira_issue_type_config.patron_nombre IS 'Regex o prefijo para detectar subtipo en el summary del issue';
COMMENT ON COLUMN jira_issue_type_config.estados_validos IS 'JSON con estados Jira válidos para este tipo (ej: ["In Progress","Done"])';
COMMENT ON COLUMN jira_issue_type_config.estado_final IS 'Estado Jira que indica que el issue está completado';
COMMENT ON COLUMN jira_issue_type_config.contabilizar_cap IS 'Si este tipo cuenta para la capacidad del sprint';
COMMENT ON COLUMN jira_issue_type_config.activa IS 'Si esta configuración está activa';
COMMENT ON COLUMN jira_issue_type_config.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN jira_issue_type_config.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN jira_issue_type_config.created_by IS 'Usuario que creó el registro';

--rollback DROP TABLE IF EXISTS jira_issue_type_config CASCADE;
