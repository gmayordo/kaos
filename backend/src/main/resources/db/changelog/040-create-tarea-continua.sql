--liquibase formatted sql

--changeset maxwell:040 labels:planificacion
--comment: Crea tabla tarea_continua para tareas de larga duración que cruzan múltiples sprints (tipo Gantt)

CREATE TABLE tarea_continua (
    id              BIGSERIAL       PRIMARY KEY,
    -- Título de la tarea continua
    titulo          VARCHAR(255)    NOT NULL,
    -- Descripción opcional
    descripcion     TEXT,
    -- Squad al que pertenece
    squad_id        BIGINT          NOT NULL REFERENCES squad(id),
    -- Persona asignada (puede ser null = sin asignar)
    persona_id      BIGINT          REFERENCES persona(id),
    -- Fecha absoluta de inicio (no vinculada a sprint)
    fecha_inicio    DATE            NOT NULL,
    -- Fecha de fin, null si indefinida/recurrente
    fecha_fin       DATE,
    -- Horas dedicadas por día
    horas_por_dia   DECIMAL(5,2),
    -- Si true, no descuenta capacidad
    es_informativa  BOOLEAN         NOT NULL DEFAULT FALSE,
    -- Color hex de la barra en el timeline (#RRGGBB)
    color           VARCHAR(7)      NOT NULL DEFAULT '#6366f1',
    -- Soft delete
    activa          BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Audit
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100)
);

COMMENT ON TABLE  tarea_continua                    IS 'Tareas de larga duración que cruzan múltiples sprints (seguimiento, recordatorios, formación, etc.)';
COMMENT ON COLUMN tarea_continua.titulo             IS 'Título descriptivo de la tarea continua';
COMMENT ON COLUMN tarea_continua.descripcion        IS 'Descripción opcional';
COMMENT ON COLUMN tarea_continua.squad_id           IS 'FK al squad al que pertenece';
COMMENT ON COLUMN tarea_continua.persona_id         IS 'FK a la persona asignada (null = sin asignar)';
COMMENT ON COLUMN tarea_continua.fecha_inicio       IS 'Fecha absoluta de inicio (no vinculada a sprint)';
COMMENT ON COLUMN tarea_continua.fecha_fin          IS 'Fecha de fin, null si indefinida o recurrente';
COMMENT ON COLUMN tarea_continua.horas_por_dia      IS 'Horas dedicadas por día a esta tarea continua';
COMMENT ON COLUMN tarea_continua.es_informativa     IS 'Si true, la tarea es visual/recordatorio y no descuenta capacidad';
COMMENT ON COLUMN tarea_continua.color              IS 'Color hex de la barra en el timeline (#RRGGBB)';
COMMENT ON COLUMN tarea_continua.activa             IS 'Soft delete: si false la tarea está archivada';

CREATE INDEX idx_tarea_continua_squad   ON tarea_continua(squad_id);
CREATE INDEX idx_tarea_continua_persona ON tarea_continua(persona_id);
CREATE INDEX idx_tarea_continua_fechas  ON tarea_continua(fecha_inicio, fecha_fin);

--rollback DROP TABLE tarea_continua;
