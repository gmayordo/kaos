--liquibase formatted sql

--changeset maxwell:020 labels:jira
--comment: Tabla de co-desarrolladores de tareas KAOS detectados por worklogs o asignados manualmente

CREATE TABLE tarea_colaborador (
    -- PK compuesta
    tarea_id         BIGINT        NOT NULL,
    persona_id       BIGINT        NOT NULL,
    -- Datos de la colaboración
    rol              VARCHAR(30),
    horas_imputadas  DECIMAL(6, 2) NOT NULL DEFAULT 0,
    -- Cómo fue detectado: WORKLOG (automático) / MANUAL (asignado LT)
    detectado_via    VARCHAR(20)   NOT NULL DEFAULT 'WORKLOG',
    -- Auditoría
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Constraints
    CONSTRAINT pk_tarea_colaborador         PRIMARY KEY (tarea_id, persona_id),
    CONSTRAINT fk_tarea_col_tarea           FOREIGN KEY (tarea_id)   REFERENCES tarea (id) ON DELETE CASCADE,
    CONSTRAINT fk_tarea_col_persona         FOREIGN KEY (persona_id) REFERENCES persona (id),
    CONSTRAINT chk_tarea_col_detectado_via  CHECK (detectado_via IN ('WORKLOG', 'MANUAL'))
);

COMMENT ON TABLE tarea_colaborador IS 'Co-desarrolladores de tareas KAOS: detectados por worklogs de Jira o asignados manualmente por el LT';
COMMENT ON COLUMN tarea_colaborador.tarea_id IS 'Tarea KAOS en la que colabora esta persona';
COMMENT ON COLUMN tarea_colaborador.persona_id IS 'Persona colaboradora';
COMMENT ON COLUMN tarea_colaborador.rol IS 'Rol en la tarea: DESARROLLADOR / REVISOR / APOYO';
COMMENT ON COLUMN tarea_colaborador.horas_imputadas IS 'Total de horas imputadas por esta persona en la tarea';
COMMENT ON COLUMN tarea_colaborador.detectado_via IS 'WORKLOG: detectado automáticamente al importar worklogs de Jira; MANUAL: asignado por el LT';

CREATE INDEX idx_tarea_col_tarea   ON tarea_colaborador (tarea_id);
CREATE INDEX idx_tarea_col_persona ON tarea_colaborador (persona_id);

--rollback DROP TABLE tarea_colaborador;
