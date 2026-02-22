--liquibase formatted sql

--changeset maxwell:014 labels:planificacion
--comment: Crear tabla sprint para ciclos de planificación

CREATE TABLE sprint (
    id                  BIGSERIAL       PRIMARY KEY,
    nombre              VARCHAR(100)    NOT NULL,
    squad_id            BIGINT          NOT NULL,
    fecha_inicio        DATE            NOT NULL,
    fecha_fin           DATE            NOT NULL,
    objetivo            TEXT,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'PLANIFICACION',
    capacidad_total     DECIMAL(10,2),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    CONSTRAINT fk_sprint_squad FOREIGN KEY (squad_id) REFERENCES squad(id) ON DELETE CASCADE,
    CONSTRAINT uk_sprint_squad_fecha UNIQUE(squad_id, fecha_inicio)
);

CREATE INDEX idx_sprint_squad ON sprint(squad_id);
CREATE INDEX idx_sprint_estado ON sprint(estado);

COMMENT ON TABLE sprint IS 'Sprints de planificación (ciclos de 2 semanas)';
COMMENT ON COLUMN sprint.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN sprint.nombre IS 'Nombre del sprint (ej: RED-Sprint-1)';
COMMENT ON COLUMN sprint.squad_id IS 'Squad asignado al sprint';
COMMENT ON COLUMN sprint.fecha_inicio IS 'Fecha de inicio (lunes)';
COMMENT ON COLUMN sprint.fecha_fin IS 'Fecha de fin (viernes, 14 días después)';
COMMENT ON COLUMN sprint.objetivo IS 'Objetivo del sprint';
COMMENT ON COLUMN sprint.estado IS 'Estado: PLANIFICACION, ACTIVO, CERRADO';
COMMENT ON COLUMN sprint.capacidad_total IS 'Capacidad total calculada del squad (horas)';
COMMENT ON COLUMN sprint.created_at IS 'Fecha de creación';
COMMENT ON COLUMN sprint.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN sprint.created_by IS 'Usuario que creó el registro';

--rollback DROP TABLE IF EXISTS sprint CASCADE;
