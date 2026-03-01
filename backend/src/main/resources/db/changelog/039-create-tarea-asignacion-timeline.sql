--liquibase formatted sql

--changeset maxwell:039 labels:planificacion
--comment: Crea tabla tarea_asignacion_timeline para vincular tareas padre Jira con personas y rangos de días en el timeline

CREATE TABLE tarea_asignacion_timeline (
    id              BIGSERIAL       PRIMARY KEY,
    -- Tarea padre (HISTORIA) vinculada
    tarea_id        BIGINT          NOT NULL REFERENCES tarea(id) ON DELETE CASCADE,
    -- Persona asignada a este rango
    persona_id      BIGINT          NOT NULL REFERENCES persona(id),
    -- Sprint donde aplica la asignación
    sprint_id       BIGINT          NOT NULL REFERENCES sprint(id),
    -- Día de inicio en el sprint (1..10)
    dia_inicio      INTEGER         NOT NULL,
    -- Día de fin en el sprint (1..10)
    dia_fin         INTEGER         NOT NULL,
    -- Horas diarias dedicadas (null = usa toda la disponibilidad)
    horas_por_dia   DECIMAL(5,2),
    -- Si true, no descuenta capacidad del sprint
    es_informativa  BOOLEAN         NOT NULL DEFAULT FALSE,
    -- Audit
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    CONSTRAINT chk_tat_dia_rango CHECK (dia_inicio >= 1 AND dia_fin <= 10 AND dia_inicio <= dia_fin)
);

COMMENT ON TABLE  tarea_asignacion_timeline                    IS 'Asignaciones de tareas padre a personas en el timeline con rango de días';
COMMENT ON COLUMN tarea_asignacion_timeline.tarea_id           IS 'FK a tarea padre (HISTORIA) vinculada al timeline';
COMMENT ON COLUMN tarea_asignacion_timeline.persona_id         IS 'FK a la persona asignada para este rango';
COMMENT ON COLUMN tarea_asignacion_timeline.sprint_id          IS 'FK al sprint donde aplica la asignación';
COMMENT ON COLUMN tarea_asignacion_timeline.dia_inicio         IS 'Día de inicio en el sprint (1=lunes semana 1 ... 10=viernes semana 2)';
COMMENT ON COLUMN tarea_asignacion_timeline.dia_fin            IS 'Día de fin en el sprint (1..10), debe ser >= dia_inicio';
COMMENT ON COLUMN tarea_asignacion_timeline.horas_por_dia      IS 'Horas dedicadas por día a esta tarea. null = toda la disponibilidad';
COMMENT ON COLUMN tarea_asignacion_timeline.es_informativa     IS 'Si true, la asignación es visual/recordatorio y no descuenta capacidad';

CREATE INDEX idx_tat_sprint ON tarea_asignacion_timeline(sprint_id);
CREATE INDEX idx_tat_tarea  ON tarea_asignacion_timeline(tarea_id);
CREATE INDEX idx_tat_persona ON tarea_asignacion_timeline(persona_id);

--rollback DROP TABLE tarea_asignacion_timeline;
