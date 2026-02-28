--liquibase formatted sql

--changeset maxwell:035 labels:planificacion
--comment: Crea tabla tarea_dependencia para modelar dependencias entre tareas (bloque 5)

CREATE TABLE tarea_dependencia (
    id          BIGSERIAL    PRIMARY KEY,
    -- Tarea de la que parte la dependencia (bloqueante)
    tarea_origen_id  BIGINT NOT NULL REFERENCES tarea(id) ON DELETE CASCADE,
    -- Tarea que depende de la origen (bloqueada hasta que origen esté COMPLETADA)
    tarea_destino_id BIGINT NOT NULL REFERENCES tarea(id) ON DELETE CASCADE,
    -- Tipo: ESTRICTA (no puede iniciar hasta completada) | SUAVE (informativa)
    tipo         VARCHAR(10)  NOT NULL,
    -- Audit
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(100),
    CONSTRAINT chk_dependencia_tipo     CHECK (tipo IN ('ESTRICTA', 'SUAVE')),
    CONSTRAINT chk_no_auto_referencia   CHECK (tarea_origen_id != tarea_destino_id),
    CONSTRAINT uk_tarea_dependencia_par UNIQUE (tarea_origen_id, tarea_destino_id)
);

COMMENT ON TABLE  tarea_dependencia                   IS 'Dependencias entre tareas de un sprint (origen bloquea a destino)';
COMMENT ON COLUMN tarea_dependencia.tarea_origen_id   IS 'Tarea bloqueante — debe completarse antes que la destino';
COMMENT ON COLUMN tarea_dependencia.tarea_destino_id  IS 'Tarea bloqueada — no puede iniciar hasta que origen esté COMPLETADA';
COMMENT ON COLUMN tarea_dependencia.tipo              IS 'ESTRICTA: bloqueo real | SUAVE: dependencia informativa';

CREATE INDEX idx_dependencia_origen  ON tarea_dependencia(tarea_origen_id);
CREATE INDEX idx_dependencia_destino ON tarea_dependencia(tarea_destino_id);

--rollback DROP TABLE tarea_dependencia;
