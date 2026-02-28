--liquibase formatted sql

--changeset kaos:037 labels:planificacion
--comment: Añade columnas audit (updated_at, created_by) a plantilla_asignacion_linea que faltaban en el changeset 036

ALTER TABLE plantilla_asignacion_linea ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE plantilla_asignacion_linea ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);

COMMENT ON COLUMN plantilla_asignacion_linea.updated_at  IS 'Fecha de última modificación (audit)';
COMMENT ON COLUMN plantilla_asignacion_linea.created_by  IS 'Usuario que creó el registro (audit)';

--rollback ALTER TABLE plantilla_asignacion_linea DROP COLUMN IF EXISTS updated_at, DROP COLUMN IF EXISTS created_by;
