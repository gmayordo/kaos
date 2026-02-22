--liquibase formatted sql

--changeset kaos:007 labels:calendario
--comment: Crear tabla festivo y relación M:N con persona

CREATE TABLE festivo (
    id          BIGSERIAL       PRIMARY KEY,
    fecha       DATE            NOT NULL,
    descripcion VARCHAR(200)    NOT NULL,
    tipo        VARCHAR(20)     NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(100),
    CONSTRAINT uk_festivo_fecha_descripcion UNIQUE (fecha, descripcion),
    CONSTRAINT chk_festivo_tipo CHECK (tipo IN ('NACIONAL', 'REGIONAL', 'LOCAL'))
);

COMMENT ON TABLE festivo IS 'Festivos asignados a personas específicas';
COMMENT ON COLUMN festivo.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN festivo.fecha IS 'Fecha del festivo';
COMMENT ON COLUMN festivo.descripcion IS 'Descripción del festivo (ej: Año Nuevo)';
COMMENT ON COLUMN festivo.tipo IS 'Tipo: NACIONAL / REGIONAL / LOCAL';
COMMENT ON COLUMN festivo.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN festivo.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN festivo.created_by IS 'Usuario que creó el registro';

CREATE INDEX idx_festivo_fecha ON festivo(fecha);
CREATE INDEX idx_festivo_tipo ON festivo(tipo);

-- Tabla de unión M:N entre persona y festivo
CREATE TABLE persona_festivo (
    festivo_id BIGINT NOT NULL REFERENCES festivo(id) ON DELETE CASCADE,
    persona_id BIGINT NOT NULL REFERENCES persona(id) ON DELETE CASCADE,
    PRIMARY KEY (festivo_id, persona_id)
);

COMMENT ON TABLE persona_festivo IS 'Relación M:N entre festivos y personas';
COMMENT ON COLUMN persona_festivo.festivo_id IS 'FK al festivo';
COMMENT ON COLUMN persona_festivo.persona_id IS 'FK a la persona';

CREATE INDEX idx_persona_festivo_persona ON persona_festivo(persona_id);

--rollback DROP TABLE IF EXISTS persona_festivo;
--rollback DROP TABLE IF EXISTS festivo;
