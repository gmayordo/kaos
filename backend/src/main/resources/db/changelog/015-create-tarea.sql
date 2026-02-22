--liquibase formatted sql

--changeset maxwell:015 labels:planificacion
--comment: Crear tabla tarea para tareas dentro de sprints

CREATE TABLE tarea (
    id                  BIGSERIAL       PRIMARY KEY,
    sprint_id           BIGINT          NOT NULL,
    titulo              VARCHAR(255)    NOT NULL,
    tipo                VARCHAR(20)     NOT NULL,
    categoria           VARCHAR(20)     NOT NULL,
    estimacion          DECIMAL(10,2)   NOT NULL,
    prioridad           VARCHAR(20)     NOT NULL DEFAULT 'NORMAL',
    estado              VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE',
    persona_id          BIGINT,
    dia_asignado        INTEGER,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    CONSTRAINT fk_tarea_sprint FOREIGN KEY (sprint_id) REFERENCES sprint(id) ON DELETE CASCADE,
    CONSTRAINT fk_tarea_persona FOREIGN KEY (persona_id) REFERENCES persona(id) ON DELETE SET NULL,
    CONSTRAINT chk_dia_asignado CHECK (dia_asignado IS NULL OR (dia_asignado >= 1 AND dia_asignado <= 10)),
    CONSTRAINT chk_estimacion CHECK (estimacion > 0)
);

CREATE INDEX idx_tarea_sprint ON tarea(sprint_id);
CREATE INDEX idx_tarea_persona ON tarea(persona_id);
CREATE INDEX idx_tarea_estado ON tarea(estado);
CREATE INDEX idx_tarea_sprint_persona_estado ON tarea(sprint_id, persona_id, estado);

COMMENT ON TABLE tarea IS 'Tareas dentro de sprints';
COMMENT ON COLUMN tarea.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN tarea.sprint_id IS 'Sprint al que pertenece la tarea';
COMMENT ON COLUMN tarea.titulo IS 'Título descriptivo de la tarea';
COMMENT ON COLUMN tarea.tipo IS 'Tipo: HISTORIA, TAREA, BUG, SPIKE';
COMMENT ON COLUMN tarea.categoria IS 'Categoría: CORRECTIVO o EVOLUTIVO';
COMMENT ON COLUMN tarea.estimacion IS 'Estimación de horas (0.5 a 40)';
COMMENT ON COLUMN tarea.prioridad IS 'Prioridad: BAJA, NORMAL, ALTA, BLOQUEANTE';
COMMENT ON COLUMN tarea.estado IS 'Estado: PENDIENTE, EN_PROGRESO, BLOQUEADO, COMPLETADA';
COMMENT ON COLUMN tarea.persona_id IS 'Persona asignada a la tarea';
COMMENT ON COLUMN tarea.dia_asignado IS 'Día del sprint (1=L, 2=M, ..., 10=V semana 2)';
COMMENT ON COLUMN tarea.created_at IS 'Fecha de creación';
COMMENT ON COLUMN tarea.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN tarea.created_by IS 'Usuario que creó el registro';

--rollback DROP TABLE IF EXISTS tarea CASCADE;
