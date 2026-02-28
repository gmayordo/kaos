--liquibase formatted sql

--changeset maxwell:032 labels:jira
--comment: Crea tabla jira_remote_link para almacenar enlaces remotos importados desde Jira

CREATE TABLE jira_remote_link (
    id BIGSERIAL PRIMARY KEY,
    -- ID del remote link en Jira (unique para upsert idempotente)
    jira_link_id VARCHAR(50),
    -- Issue KAOS a la que pertenece este enlace
    jira_issue_id BIGINT NOT NULL REFERENCES jira_issue(id),
    -- URL del enlace remoto
    url VARCHAR(2000) NOT NULL,
    -- Título/etiqueta del enlace
    titulo VARCHAR(500),
    -- Resumen/descripción del enlace
    resumen VARCHAR(1000),
    -- Icono URL (si lo proporciona Jira)
    icono_url VARCHAR(2000),
    -- Relación del enlace (ej: "mentioned in", "is caused by")
    relacion VARCHAR(100),
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    CONSTRAINT uk_jira_remote_link_id UNIQUE (jira_link_id)
);

COMMENT ON TABLE jira_remote_link IS 'Cache de enlaces remotos (remote links) importados desde Jira';
COMMENT ON COLUMN jira_remote_link.jira_link_id IS 'ID del remote link en Jira. Único para upsert.';
COMMENT ON COLUMN jira_remote_link.jira_issue_id IS 'Issue KAOS a la que pertenece este enlace';
COMMENT ON COLUMN jira_remote_link.url IS 'URL del enlace remoto';
COMMENT ON COLUMN jira_remote_link.titulo IS 'Título/etiqueta del enlace mostrado en Jira';
COMMENT ON COLUMN jira_remote_link.resumen IS 'Resumen/descripción del enlace';
COMMENT ON COLUMN jira_remote_link.relacion IS 'Tipo de relación (mentioned in, is caused by, etc.)';

CREATE INDEX idx_jira_remote_link_issue ON jira_remote_link(jira_issue_id);

--rollback DROP TABLE jira_remote_link;
