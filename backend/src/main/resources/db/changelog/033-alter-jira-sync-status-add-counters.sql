--liquibase formatted sql

--changeset maxwell:033 labels:jira
--comment: Añade contadores de comentarios y remote links importados al estado de sync

ALTER TABLE jira_sync_status ADD COLUMN comments_importados INT NOT NULL DEFAULT 0;
ALTER TABLE jira_sync_status ADD COLUMN remote_links_importados INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN jira_sync_status.comments_importados IS 'Número de comentarios importados en el último ciclo de sync';
COMMENT ON COLUMN jira_sync_status.remote_links_importados IS 'Número de remote links importados en el último ciclo de sync';

--rollback ALTER TABLE jira_sync_status DROP COLUMN comments_importados, DROP COLUMN remote_links_importados;
