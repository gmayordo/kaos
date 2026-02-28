--liquibase formatted sql

--changeset agente13:028 labels:jira,alertas,fix
--comment: Agrega columnas de auditoría BaseEntity (created_by, updated_by) que faltaban en jira_alert_rule

ALTER TABLE jira_alert_rule ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE jira_alert_rule ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

COMMENT ON COLUMN jira_alert_rule.created_by IS 'Usuario que creó la regla (BaseEntity audit)';
COMMENT ON COLUMN jira_alert_rule.updated_by IS 'Usuario que modificó por última vez la regla (BaseEntity audit)';

--rollback ALTER TABLE jira_alert_rule DROP COLUMN IF EXISTS created_by, DROP COLUMN IF EXISTS updated_by;
