--liquibase formatted sql

--changeset gmayordo:008 labels:bloque-2-calendario
--comment: Tabla vacacion con relación a persona, control de períodos no solapados

CREATE TABLE vacacion (
    id BIGSERIAL PRIMARY KEY,
    -- Persona a la que pertenece la vacación
    persona_id BIGINT NOT NULL,
    -- Rango de fechas (ambas inclusive)
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    -- Días laborables calculados (excluyendo fines de semana)
    dias_laborables INTEGER NOT NULL,
    -- Tipo de vacación
    tipo VARCHAR(30) NOT NULL,
    -- Estado
    estado VARCHAR(20) NOT NULL,
    -- Comentario opcional
    comentario VARCHAR(500),
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    -- Constraints
    CONSTRAINT fk_vacacion_persona FOREIGN KEY (persona_id) REFERENCES persona(id) ON DELETE CASCADE,
    CONSTRAINT chk_vacacion_tipo CHECK (tipo IN ('VACACIONES', 'ASUNTOS_PROPIOS', 'LIBRE_DISPOSICION', 'PERMISO')),
    CONSTRAINT chk_vacacion_estado CHECK (estado IN ('SOLICITADA', 'REGISTRADA')),
    CONSTRAINT chk_vacacion_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT chk_vacacion_dias_positivo CHECK (dias_laborables > 0)
);

COMMENT ON TABLE vacacion IS 'Registro de vacaciones y permisos de personas';
COMMENT ON COLUMN vacacion.persona_id IS 'Persona a la que pertenece la vacación';
COMMENT ON COLUMN vacacion.fecha_inicio IS 'Fecha de inicio de la vacación (inclusive)';
COMMENT ON COLUMN vacacion.fecha_fin IS 'Fecha de fin de la vacación (inclusive)';
COMMENT ON COLUMN vacacion.dias_laborables IS 'Cantidad de días laborables (lun-vie) del período';
COMMENT ON COLUMN vacacion.tipo IS 'VACACIONES, ASUNTOS_PROPIOS, LIBRE_DISPOSICION, PERMISO';
COMMENT ON COLUMN vacacion.estado IS 'SOLICITADA (pendiente aprobación), REGISTRADA (aprobada)';
COMMENT ON COLUMN vacacion.comentario IS 'Comentario o justificación opcional';

-- Índices para optimizar búsquedas
CREATE INDEX idx_vacacion_persona ON vacacion(persona_id);
CREATE INDEX idx_vacacion_fechas ON vacacion(fecha_inicio, fecha_fin);
CREATE INDEX idx_vacacion_tipo ON vacacion(tipo);
CREATE INDEX idx_vacacion_estado ON vacacion(estado);

--rollback DROP INDEX idx_vacacion_estado;
--rollback DROP INDEX idx_vacacion_tipo;
--rollback DROP INDEX idx_vacacion_fechas;
--rollback DROP INDEX idx_vacacion_persona;
--rollback DROP TABLE vacacion;
