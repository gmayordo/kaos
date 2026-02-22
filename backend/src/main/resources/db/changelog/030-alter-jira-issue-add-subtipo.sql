--liquibase formatted sql

--changeset maxwell:030 labels:jira
--comment: Añadir columna subtipo_jira a jira_issue para almacenar el subtipo detectado

ALTER TABLE jira_issue ADD COLUMN subtipo_jira VARCHAR(30);

COMMENT ON COLUMN jira_issue.subtipo_jira IS 'Subtipo detectado: DESARROLLO | JUNIT | DOCUMENTACION | OTROS (solo para sub-tasks)';

--rollback ALTER TABLE jira_issue DROP COLUMN IF EXISTS subtipo_jira;
