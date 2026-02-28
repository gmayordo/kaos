--liquibase formatted sql

--changeset maxwell:021 labels:jira
--comment: Tabla de registro de llamadas a la API REST de Jira para control de rate limiting

CREATE TABLE jira_api_call_log (
    id          BIGSERIAL PRIMARY KEY,
    -- Endpoint llamado (ej: /rest/api/2/search)
    endpoint    VARCHAR(500) NOT NULL,
    -- Método HTTP: GET, POST, PUT, DELETE
    metodo      VARCHAR(10) NOT NULL,
    -- Código de respuesta HTTP recibido
    status_code INTEGER,
    -- Squad origen de la llamada (para trazabilidad)
    squad_id    BIGINT,
    -- Timestamp exacto, clave para el cálculo de la ventana de 2 horas
    executed_at TIMESTAMP NOT NULL
);

COMMENT ON TABLE jira_api_call_log IS 'Registro de llamadas a la API REST de Jira para control de rate limiting (200 calls/2h)';
COMMENT ON COLUMN jira_api_call_log.endpoint IS 'Endpoint de la API Jira llamado (ej: /rest/api/2/search)';
COMMENT ON COLUMN jira_api_call_log.metodo IS 'Método HTTP utilizado (GET, POST, etc.)';
COMMENT ON COLUMN jira_api_call_log.status_code IS 'Código de respuesta HTTP recibido (200, 401, 429, etc.)';
COMMENT ON COLUMN jira_api_call_log.squad_id IS 'Squad que originó la llamada (sin FK para no bloquear purgas)';
COMMENT ON COLUMN jira_api_call_log.executed_at IS 'Timestamp de la llamada; se usa en consulta COUNT WHERE executed_at >= NOW() - 2h';

-- Índice crítico para la query de rate limiting
CREATE INDEX idx_jira_call_log_executed_at ON jira_api_call_log (executed_at DESC);
CREATE INDEX idx_jira_call_log_squad_executed ON jira_api_call_log (squad_id, executed_at DESC);

--rollback DROP TABLE jira_api_call_log;
