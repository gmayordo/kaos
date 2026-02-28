--liquibase formatted sql

--changeset maxwell:031 labels:jira
--comment: Crea tabla jira_comment para almacenar comentarios importados desde Jira

CREATE TABLE jira_comment (
    id BIGSERIAL PRIMARY KEY,
    -- ID del comentario en Jira (unique para upsert idempotente)
    jira_comment_id VARCHAR(50),
    -- Issue KAOS a la que pertenece este comentario
    jira_issue_id BIGINT NOT NULL REFERENCES jira_issue(id),
    -- author.key del comentario en Jira
    autor_jira VARCHAR(100) NOT NULL,
    -- Persona KAOS mapeada por author.key
    persona_id BIGINT REFERENCES persona(id),
    -- Cuerpo del comentario
    body TEXT,
    -- Fecha de creación del comentario en Jira
    fecha_creacion TIMESTAMP,
    -- Fecha de última actualización del comentario en Jira
    fecha_actualizacion TIMESTAMP,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    CONSTRAINT uk_jira_comment_id UNIQUE (jira_comment_id)
);

COMMENT ON TABLE jira_comment IS 'Cache de comentarios importados desde Jira Server / Data Center';
COMMENT ON COLUMN jira_comment.jira_comment_id IS 'ID del comentario en Jira (comment.id). Único para upsert.';
COMMENT ON COLUMN jira_comment.jira_issue_id IS 'Issue KAOS a la que pertenece este comentario';
COMMENT ON COLUMN jira_comment.autor_jira IS 'author.key del comentario en Jira';
COMMENT ON COLUMN jira_comment.persona_id IS 'Persona KAOS mapeada por author.key';
COMMENT ON COLUMN jira_comment.body IS 'Cuerpo/texto del comentario';
COMMENT ON COLUMN jira_comment.fecha_creacion IS 'Fecha de creación del comentario en Jira';
COMMENT ON COLUMN jira_comment.fecha_actualizacion IS 'Fecha de última actualización del comentario en Jira';

CREATE INDEX idx_jira_comment_issue ON jira_comment(jira_issue_id);

--rollback DROP TABLE jira_comment;
