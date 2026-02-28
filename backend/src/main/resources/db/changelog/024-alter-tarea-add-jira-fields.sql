--liquibase formatted sql

--changeset maxwell:024 labels:jira
--comment: Añade campos Jira a la tabla tarea para vincular issues importados desde Jira

ALTER TABLE tarea ADD COLUMN jira_key       VARCHAR(50);
ALTER TABLE tarea ADD COLUMN jira_issue_id  BIGINT;
ALTER TABLE tarea ADD COLUMN es_de_jira     BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tarea ADD CONSTRAINT fk_tarea_jira_issue
    FOREIGN KEY (jira_issue_id) REFERENCES jira_issue (id);

CREATE INDEX idx_tarea_jira_key ON tarea (jira_key);

COMMENT ON COLUMN tarea.jira_key       IS 'Clave del issue en Jira vinculado a esta tarea (ej: PROJ-123)';
COMMENT ON COLUMN tarea.jira_issue_id  IS 'FK al cache local del issue Jira del que procede esta tarea';
COMMENT ON COLUMN tarea.es_de_jira     IS 'TRUE si la tarea fue generada automáticamente por importación desde Jira';

--rollback ALTER TABLE tarea DROP COLUMN es_de_jira;
--rollback ALTER TABLE tarea DROP COLUMN jira_issue_id;
--rollback ALTER TABLE tarea DROP COLUMN jira_key;
