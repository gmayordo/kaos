--liquibase formatted sql

--changeset kaos:004 labels:persona
--comment: Crear tabla persona para miembros del equipo

CREATE TABLE persona (
    id                  BIGSERIAL       PRIMARY KEY,
    nombre              VARCHAR(200)    NOT NULL,
    email               VARCHAR(200)    NOT NULL UNIQUE,
    id_jira             VARCHAR(100)    UNIQUE,
    perfil_horario_id   BIGINT          NOT NULL REFERENCES perfil_horario(id),
    seniority           VARCHAR(20),
    skills              TEXT,
    coste_hora          DECIMAL(8,2),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_incorporacion DATE,
    send_notifications  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100)
);

COMMENT ON TABLE persona IS 'Miembros del equipo de desarrollo';
COMMENT ON COLUMN persona.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN persona.nombre IS 'Nombre completo de la persona';
COMMENT ON COLUMN persona.email IS 'Email corporativo (único)';
COMMENT ON COLUMN persona.id_jira IS 'Usuario Jira (ej: gmayordo)';
COMMENT ON COLUMN persona.perfil_horario_id IS 'FK al perfil de horario asignado';
COMMENT ON COLUMN persona.seniority IS 'Nivel de seniority: JUNIOR, MID, SENIOR, LEAD';
COMMENT ON COLUMN persona.skills IS 'Skills separadas por coma';
COMMENT ON COLUMN persona.coste_hora IS 'Coste por hora (opcional)';
COMMENT ON COLUMN persona.activo IS 'Estado activo/inactivo de la persona';
COMMENT ON COLUMN persona.fecha_incorporacion IS 'Fecha de incorporación al equipo';
COMMENT ON COLUMN persona.send_notifications IS 'Flag para envío de notificaciones por email';
COMMENT ON COLUMN persona.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN persona.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN persona.created_by IS 'Usuario que creó el registro';

CREATE INDEX idx_persona_perfil_horario ON persona(perfil_horario_id);
CREATE INDEX idx_persona_activo ON persona(activo);

--rollback DROP TABLE IF EXISTS persona;
