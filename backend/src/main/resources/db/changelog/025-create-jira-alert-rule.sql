--liquibase formatted sql

--changeset maxwell:025 labels:jira,alertas
--comment: Tabla de reglas de coherencia configurables evaluadas con SpEL tras sync Jira

CREATE TABLE jira_alert_rule (
    id               BIGSERIAL     PRIMARY KEY,
    -- Squad al que aplica la regla (null = aplica a todos los squads)
    squad_id         BIGINT        REFERENCES squad(id) ON DELETE SET NULL,
    -- Identificador corto legible: tarea-done-jira-activa-kaos
    nombre           VARCHAR(100)  NOT NULL,
    -- Descripción detallada para documentación interna
    descripcion      VARCHAR(500),
    -- Tipo lógico: ESTADO_INCOHERENTE | IMPUTACION_FALTANTE | DESVIACION_HORAS | etc.
    tipo             VARCHAR(50)   NOT NULL,
    -- Expresión SpEL evaluada sobre el contexto del sprint
    condicion_spel   TEXT          NOT NULL,
    -- Plantilla del mensaje con placeholders {jiraKey}, {persona}, {valor}
    mensaje_template VARCHAR(500)  NOT NULL,
    -- Severidad: CRITICO | AVISO | INFO
    severidad        VARCHAR(20)   NOT NULL,
    -- Umbral configurable sin tocar código; accesible como #regla.umbralValor en SpEL
    umbral_valor     DECIMAL(10,2),
    -- Permite desactivar la regla sin eliminarla
    activa           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,

    CONSTRAINT uq_alert_rule_nombre UNIQUE (nombre),
    CONSTRAINT chk_alert_rule_severidad CHECK (severidad IN ('CRITICO','AVISO','INFO')),
    CONSTRAINT chk_alert_rule_tipo CHECK (tipo IN (
        'ESTADO_INCOHERENTE','IMPUTACION_FALTANTE','DESVIACION_HORAS',
        'SPRINT_EN_RIESGO','ESTIMACION_CERO','TAREA_ESTANCADA','CUSTOM'
    ))
);

COMMENT ON TABLE  jira_alert_rule IS 'Reglas de coherencia Jira←→KAOS configuradas en BD y evaluadas con SpEL';
COMMENT ON COLUMN jira_alert_rule.nombre IS 'Identificador corto único: tarea-done-jira-activa-kaos';
COMMENT ON COLUMN jira_alert_rule.condicion_spel IS 'Expresión SpEL: contexto incluye #issue, #tarea, #persona, #regla, #pctTiempo, etc.';
COMMENT ON COLUMN jira_alert_rule.mensaje_template IS 'Template con placeholders: {jiraKey}, {estadoKaos}, {persona}, {valor}';
COMMENT ON COLUMN jira_alert_rule.umbral_valor IS 'Referenciable como #regla.umbralValor desde condicion_spel';
COMMENT ON COLUMN jira_alert_rule.activa IS 'false desactiva la regla sin borrarla';

CREATE INDEX idx_alert_rule_squad     ON jira_alert_rule(squad_id);
CREATE INDEX idx_alert_rule_activa    ON jira_alert_rule(activa);
CREATE INDEX idx_alert_rule_tipo      ON jira_alert_rule(tipo);

--rollback DROP TABLE IF EXISTS jira_alert_rule;
