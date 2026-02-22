--liquibase formatted sql

--changeset gmayordo:009 labels:bloque-2-calendario
--comment: Tabla ausencia (bajas médicas, emergencias) con fecha fin opcional

CREATE TABLE ausencia (
    id BIGSERIAL PRIMARY KEY,
    -- Persona afectada
    persona_id BIGINT NOT NULL,
    -- Rango de fechas (fecha_fin puede ser NULL para ausencias indefinidas)
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    -- Tipo de ausencia
    tipo VARCHAR(20) NOT NULL,
    -- Comentario o justificación
    comentario VARCHAR(500),
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    -- Constraints
    CONSTRAINT fk_ausencia_persona FOREIGN KEY (persona_id) REFERENCES persona(id) ON DELETE CASCADE,
    CONSTRAINT chk_ausencia_tipo CHECK (tipo IN ('BAJA_MEDICA', 'EMERGENCIA', 'OTRO')),
    CONSTRAINT chk_ausencia_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

COMMENT ON TABLE ausencia IS 'Registro de ausencias inesperadas (bajas médicas, emergencias)';
COMMENT ON COLUMN ausencia.persona_id IS 'Persona afectada por la ausencia';
COMMENT ON COLUMN ausencia.fecha_inicio IS 'Fecha de inicio de la ausencia';
COMMENT ON COLUMN ausencia.fecha_fin IS 'Fecha de fin (NULL = ausencia indefinida o sin fecha cierre)';
COMMENT ON COLUMN ausencia.tipo IS 'BAJA_MEDICA, EMERGENCIA, OTRO';
COMMENT ON COLUMN ausencia.comentario IS 'Comentario o justificación de la ausencia';

-- Índices para optimizar búsquedas
CREATE INDEX idx_ausencia_persona ON ausencia(persona_id);
CREATE INDEX idx_ausencia_fechas ON ausencia(fecha_inicio, fecha_fin);
CREATE INDEX idx_ausencia_tipo ON ausencia(tipo);

--rollback DROP INDEX idx_ausencia_tipo;
--rollback DROP INDEX idx_ausencia_fechas;
--rollback DROP INDEX idx_ausencia_persona;
--rollback DROP TABLE ausencia;
