--liquibase formatted sql

--changeset kaos:001 labels:horario
--comment: Crear tabla perfil_horario para perfiles de horario laboral

CREATE TABLE perfil_horario (
    id              BIGSERIAL       PRIMARY KEY,
    nombre          VARCHAR(100)    NOT NULL UNIQUE,
    zona_horaria    VARCHAR(50)     NOT NULL,
    horas_lunes     DECIMAL(4,2)    NOT NULL,
    horas_martes    DECIMAL(4,2)    NOT NULL,
    horas_miercoles DECIMAL(4,2)    NOT NULL,
    horas_jueves    DECIMAL(4,2)    NOT NULL,
    horas_viernes   DECIMAL(4,2)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100)
);

COMMENT ON TABLE perfil_horario IS 'Perfiles de horario laboral configurables por ubicación';
COMMENT ON COLUMN perfil_horario.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN perfil_horario.nombre IS 'Nombre del perfil (ej: España, Chile)';
COMMENT ON COLUMN perfil_horario.zona_horaria IS 'Zona horaria IANA (ej: Europe/Madrid)';
COMMENT ON COLUMN perfil_horario.horas_lunes IS 'Horas laborables lunes';
COMMENT ON COLUMN perfil_horario.horas_martes IS 'Horas laborables martes';
COMMENT ON COLUMN perfil_horario.horas_miercoles IS 'Horas laborables miércoles';
COMMENT ON COLUMN perfil_horario.horas_jueves IS 'Horas laborables jueves';
COMMENT ON COLUMN perfil_horario.horas_viernes IS 'Horas laborables viernes';
COMMENT ON COLUMN perfil_horario.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN perfil_horario.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN perfil_horario.created_by IS 'Usuario que creó el registro';

--rollback DROP TABLE IF EXISTS perfil_horario;
