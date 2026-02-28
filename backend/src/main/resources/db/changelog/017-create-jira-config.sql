--liquibase formatted sql

--changeset maxwell:017 labels:jira
--comment: Tabla de configuración de conexión a Jira Server/DC por squad

CREATE TABLE jira_config (
    id                  BIGSERIAL PRIMARY KEY,
    squad_id            BIGINT NOT NULL,
    -- URL base del servidor Jira
    url                 VARCHAR(500) NOT NULL,
    -- Nombre de usuario Jira con permisos de lectura
    usuario             VARCHAR(200) NOT NULL,
    -- Token de acceso cifrado con AES-256/GCM
    token               VARCHAR(500) NOT NULL,
    -- IDs de boards Jira por tipo de tarea
    board_correctivo_id BIGINT,
    board_evolutivo_id  BIGINT,
    -- Mapeo de estados Jira → KAOS en JSON (ej: {"Done":"COMPLETADA","In Progress":"EN_PROGRESO"})
    mapeo_estados       TEXT,
    -- Método de carga: API_REST | SELENIUM | LOCAL
    load_method         VARCHAR(20) NOT NULL DEFAULT 'API_REST',
    -- Permite desactivar sin borrar
    activa              BOOLEAN NOT NULL DEFAULT TRUE,
    -- Campos de auditoría (BaseEntity)
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    -- Constraints
    CONSTRAINT uk_jira_config_squad UNIQUE (squad_id),
    CONSTRAINT fk_jira_config_squad FOREIGN KEY (squad_id) REFERENCES squad (id)
);

COMMENT ON TABLE jira_config IS 'Configuración de conexión a Jira Server/DC por squad';
COMMENT ON COLUMN jira_config.url IS 'URL base del servidor Jira (ej: https://jira.empresa.com)';
COMMENT ON COLUMN jira_config.usuario IS 'Nombre de usuario de Jira con permisos de lectura';
COMMENT ON COLUMN jira_config.token IS 'Token de acceso Jira, almacenado cifrado con AES-256/GCM';
COMMENT ON COLUMN jira_config.board_correctivo_id IS 'ID del board Jira para tareas correctivas';
COMMENT ON COLUMN jira_config.board_evolutivo_id IS 'ID del board Jira para tareas evolutivas';
COMMENT ON COLUMN jira_config.mapeo_estados IS 'Mapeo de estados Jira a estados KAOS en formato JSON';
COMMENT ON COLUMN jira_config.load_method IS 'Método de carga activo: API_REST | SELENIUM | LOCAL';
COMMENT ON COLUMN jira_config.activa IS 'Indica si la configuración está activa';

CREATE INDEX idx_jira_config_squad ON jira_config (squad_id);

--rollback DROP TABLE jira_config;
