--liquibase formatted sql

--changeset maxwell:018 labels:jira
--comment: Tabla cache de issues importadas desde Jira Server / Data Center

CREATE TABLE jira_issue (
    id                BIGSERIAL PRIMARY KEY,
    -- Identificación Jira
    jira_key          VARCHAR(50)   NOT NULL,
    jira_id           VARCHAR(50),
    -- Relaciones KAOS
    squad_id          BIGINT        NOT NULL,
    sprint_id         BIGINT,
    tarea_id          BIGINT,
    -- Jerarquía
    parent_key        VARCHAR(50),
    -- Contenido
    summary           VARCHAR(500)  NOT NULL,
    descripcion       TEXT,
    tipo_jira         VARCHAR(50)   NOT NULL,
    -- Estado
    estado_jira       VARCHAR(50)   NOT NULL,
    estado_kaos       VARCHAR(20),
    -- Asignación
    asignado_jira     VARCHAR(100),
    persona_id        BIGINT,
    -- Estimación y tiempo
    estimacion_horas  DECIMAL(6, 2),
    horas_consumidas  DECIMAL(6, 2) DEFAULT 0,
    -- Clasificación
    categoria         VARCHAR(20),
    prioridad_jira    VARCHAR(20),
    -- Control de sincronización
    ultima_sync       TIMESTAMP,
    -- Campos de auditoría (BaseEntity)
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(100),
    -- Constraints
    CONSTRAINT uk_jira_issue_key    UNIQUE (jira_key),
    CONSTRAINT fk_jira_issue_squad  FOREIGN KEY (squad_id)  REFERENCES squad (id),
    CONSTRAINT fk_jira_issue_sprint FOREIGN KEY (sprint_id) REFERENCES sprint (id),
    CONSTRAINT fk_jira_issue_tarea  FOREIGN KEY (tarea_id)  REFERENCES tarea (id),
    CONSTRAINT fk_jira_issue_persona FOREIGN KEY (persona_id) REFERENCES persona (id)
);

COMMENT ON TABLE jira_issue IS 'Cache local de issues importadas desde Jira Server / Data Center';
COMMENT ON COLUMN jira_issue.jira_key IS 'Clave única del issue en Jira (ej: PROJ-123)';
COMMENT ON COLUMN jira_issue.jira_id IS 'ID numérico interno del issue en Jira';
COMMENT ON COLUMN jira_issue.squad_id IS 'Squad propietario de este issue';
COMMENT ON COLUMN jira_issue.sprint_id IS 'Sprint KAOS al que está vinculada (nullable)';
COMMENT ON COLUMN jira_issue.tarea_id IS 'Tarea KAOS generada a partir de este issue (nullable)';
COMMENT ON COLUMN jira_issue.parent_key IS 'Clave del issue padre en Jira — relleno si es sub-task';
COMMENT ON COLUMN jira_issue.summary IS 'Título del issue (campo summary de Jira)';
COMMENT ON COLUMN jira_issue.tipo_jira IS 'Tipo de issue: Story / Task / Bug / Sub-task / Spike';
COMMENT ON COLUMN jira_issue.estado_jira IS 'Estado del issue en Jira (ej: In Progress, Done)';
COMMENT ON COLUMN jira_issue.estado_kaos IS 'Estado mapeado en KAOS mediante JiraConfig.mapeoEstados';
COMMENT ON COLUMN jira_issue.asignado_jira IS 'assignee.key del issue en Jira';
COMMENT ON COLUMN jira_issue.persona_id IS 'Persona KAOS mapeada por idJira del assignee';
COMMENT ON COLUMN jira_issue.estimacion_horas IS 'Estimación original en horas (timeoriginalestimate / 3600)';
COMMENT ON COLUMN jira_issue.horas_consumidas IS 'Suma de horas de worklogs importados';
COMMENT ON COLUMN jira_issue.categoria IS 'Categoría de la tarea: CORRECTIVO / EVOLUTIVO';
COMMENT ON COLUMN jira_issue.prioridad_jira IS 'Prioridad en Jira: Highest / High / Medium / Low / Lowest';
COMMENT ON COLUMN jira_issue.ultima_sync IS 'Timestamp de la última sincronización de este issue';

CREATE UNIQUE INDEX idx_jira_issue_key    ON jira_issue (jira_key);
CREATE INDEX        idx_jira_issue_sprint ON jira_issue (sprint_id);
CREATE INDEX        idx_jira_issue_squad  ON jira_issue (squad_id);
CREATE INDEX        idx_jira_issue_parent ON jira_issue (parent_key);

--rollback DROP TABLE jira_issue;
