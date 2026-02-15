--liquibase formatted sql

--changeset kaos:003 labels:squad
--comment: Crear tabla squad para equipos de desarrollo

CREATE TABLE squad (
    id                  BIGSERIAL       PRIMARY KEY,
    nombre              VARCHAR(100)    NOT NULL UNIQUE,
    descripcion         TEXT,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVO',
    id_squad_corr_jira  VARCHAR(100),
    id_squad_evol_jira  VARCHAR(100),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100)
);

COMMENT ON TABLE squad IS 'Equipos de desarrollo (squads)';
COMMENT ON COLUMN squad.id IS 'Identificador único autogenerado';
COMMENT ON COLUMN squad.nombre IS 'Nombre del squad (ej: red, green, blue)';
COMMENT ON COLUMN squad.descripcion IS 'Descripción del squad';
COMMENT ON COLUMN squad.estado IS 'Estado del squad: ACTIVO o INACTIVO';
COMMENT ON COLUMN squad.id_squad_corr_jira IS 'ID del board de correctivos en Jira';
COMMENT ON COLUMN squad.id_squad_evol_jira IS 'ID del board de evolutivos en Jira';
COMMENT ON COLUMN squad.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN squad.updated_at IS 'Fecha de última modificación';
COMMENT ON COLUMN squad.created_by IS 'Usuario que creó el registro';

--rollback DROP TABLE IF EXISTS squad;
