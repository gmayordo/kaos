--liquibase formatted sql

--changeset maxwell:016 labels:planificacion
--comment: Crear tabla bloqueo y relación N:M bloqueo_tarea

CREATE TABLE bloqueo (
    id                  BIGSERIAL       PRIMARY KEY,
    titulo              VARCHAR(255)    NOT NULL,
    descripcion         TEXT,
    tipo                VARCHAR(30)     NOT NULL,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'ABIERTO',
    responsable_id      BIGINT,
    fecha_resolucion    TIMESTAMP,
    notas               TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    CONSTRAINT fk_bloqueo_responsable FOREIGN KEY (responsable_id) REFERENCES persona(id) ON DELETE SET NULL
);

CREATE TABLE bloqueo_tarea (
    bloqueo_id          BIGINT          NOT NULL,
    tarea_id            BIGINT          NOT NULL,
    PRIMARY KEY (bloqueo_id, tarea_id),
    CONSTRAINT fk_bloqueo_tarea_bloqueo FOREIGN KEY (bloqueo_id) REFERENCES bloqueo(id) ON DELETE CASCADE,
    CONSTRAINT fk_bloqueo_tarea_tarea FOREIGN KEY (tarea_id) REFERENCES tarea(id) ON DELETE CASCADE
);

CREATE INDEX idx_bloqueo_estado ON bloqueo(estado);
CREATE INDEX idx_bloqueo_created_at ON bloqueo(created_at DESC);
CREATE INDEX idx_bloqueo_tarea ON bloqueo_tarea(tarea_id);

COMMENT ON TABLE bloqueo IS 'Bloqueos e impedimentos';
COMMENT ON COLUMN bloqueo.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN bloqueo.titulo IS 'Título del bloqueo';
COMMENT ON COLUMN bloqueo.descripcion IS 'Descripción detallada del impedimento';
COMMENT ON COLUMN bloqueo.tipo IS 'Tipo: DEPENDENCIA_EXTERNA, RECURSO, TECNICO, COMUNICACION, OTRO';
COMMENT ON COLUMN bloqueo.estado IS 'Estado: ABIERTO, EN_GESTION, RESUELTO';
COMMENT ON COLUMN bloqueo.responsable_id IS 'Persona responsable de resolver';
COMMENT ON COLUMN bloqueo.fecha_resolucion IS 'Fecha de resolución (null si aún abierto)';
COMMENT ON COLUMN bloqueo.notas IS 'Notas y historial de actualizaciones';
COMMENT ON COLUMN bloqueo.created_at IS 'Fecha de creación';
COMMENT ON COLUMN bloqueo.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN bloqueo.created_by IS 'Usuario que creó el registro';

COMMENT ON TABLE bloqueo_tarea IS 'Relación N:M entre bloqueos y tareas';

--rollback DROP TABLE IF EXISTS bloqueo_tarea CASCADE;
--rollback DROP TABLE IF EXISTS bloqueo CASCADE;
