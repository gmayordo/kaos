--liquibase formatted sql

--changeset maxwell:026 labels:jira,alertas
--comment: Tabla de alertas generadas por el motor SpEL tras cada sync Jira

CREATE TABLE jira_alerta (
    id               BIGSERIAL    PRIMARY KEY,
    -- Sprint sobre el que se evaluó la alerta
    sprint_id        BIGINT       NOT NULL REFERENCES sprint(id) ON DELETE CASCADE,
    -- Squad propietario de la alerta
    squad_id         BIGINT       NOT NULL REFERENCES squad(id),
    -- Regla que disparó la alerta
    regla_id         BIGINT       NOT NULL REFERENCES jira_alert_rule(id),
    -- Copia desnormalizada de la severidad para queries sin JOIN
    severidad        VARCHAR(20)  NOT NULL,
    -- Mensaje con valores reales ya resueltos
    mensaje          TEXT         NOT NULL,
    -- Issue afectada (nullable: alertas de nivel sprint/persona)
    jira_key         VARCHAR(50),
    -- Persona afectada (nullable: alertas de nivel issue/sprint)
    persona_id       BIGINT       REFERENCES persona(id) ON DELETE SET NULL,
    -- Estado de resolución
    resuelta         BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Si ya fue incluida en el último resumen por email
    notificada_email BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_alerta_severidad CHECK (severidad IN ('CRITICO','AVISO','INFO'))
);

COMMENT ON TABLE  jira_alerta IS 'Alertas de coherencia generadas por el motor SpEL tras cada sync Jira';
COMMENT ON COLUMN jira_alerta.regla_id IS 'FK a jira_alert_rule — identifica qué regla disparó la alerta';
COMMENT ON COLUMN jira_alerta.mensaje IS 'Mensaje final con los valores reales ya interpolados';
COMMENT ON COLUMN jira_alerta.jira_key IS 'Issue afectada; NULL en alertas de nivel sprint o persona';
COMMENT ON COLUMN jira_alerta.resuelta IS 'El Lead Tech marca la alerta como atendida';
COMMENT ON COLUMN jira_alerta.notificada_email IS 'True cuando el resumen email ya incluyó esta alerta';

CREATE INDEX idx_alerta_sprint        ON jira_alerta(sprint_id);
CREATE INDEX idx_alerta_squad         ON jira_alerta(squad_id);
CREATE INDEX idx_alerta_resuelta      ON jira_alerta(resuelta);
CREATE INDEX idx_alerta_severidad     ON jira_alerta(severidad, sprint_id);
CREATE INDEX idx_alerta_email_pending ON jira_alerta(notificada_email);

--rollback DROP TABLE IF EXISTS jira_alerta;
