--liquibase formatted sql

--changeset kaos:005 labels:dedicacion
--comment: Crear tabla squad_member para asignaciones persona-squad con dedicación

CREATE TABLE squad_member (
    id              BIGSERIAL       PRIMARY KEY,
    persona_id      BIGINT          NOT NULL REFERENCES persona(id),
    squad_id        BIGINT          NOT NULL REFERENCES squad(id),
    rol             VARCHAR(30)     NOT NULL,
    porcentaje      INTEGER         NOT NULL CHECK (porcentaje >= 0 AND porcentaje <= 100),
    fecha_inicio    DATE            NOT NULL,
    fecha_fin       DATE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    CONSTRAINT uk_squad_member_persona_squad UNIQUE (persona_id, squad_id)
);

COMMENT ON TABLE squad_member IS 'Asignaciones de personas a squads con dedicación';
COMMENT ON COLUMN squad_member.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN squad_member.persona_id IS 'FK a la persona asignada';
COMMENT ON COLUMN squad_member.squad_id IS 'FK al squad';
COMMENT ON COLUMN squad_member.rol IS 'Rol en el squad: LIDER_TECNICO, LIDER_FUNCIONAL, FRONTEND, BACKEND, QA, SCRUM_MASTER';
COMMENT ON COLUMN squad_member.porcentaje IS 'Porcentaje de dedicación (0-100)';
COMMENT ON COLUMN squad_member.fecha_inicio IS 'Fecha inicio de la asignación';
COMMENT ON COLUMN squad_member.fecha_fin IS 'Fecha fin de la asignación (null = indefinido)';
COMMENT ON COLUMN squad_member.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN squad_member.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN squad_member.created_by IS 'Usuario que creó el registro';

CREATE INDEX idx_squad_member_persona ON squad_member(persona_id);
CREATE INDEX idx_squad_member_squad ON squad_member(squad_id);
CREATE INDEX idx_squad_member_fecha_fin ON squad_member(fecha_fin);

--rollback DROP TABLE IF EXISTS squad_member;
