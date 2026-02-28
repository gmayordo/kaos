--liquibase formatted sql

--changeset maxwell:034 labels:planificacion
--comment: Añade jerarquía padre-hijo a la tabla tarea para vincular subtareas Jira con su tarea padre KAOS

ALTER TABLE tarea ADD COLUMN tarea_parent_id BIGINT;

ALTER TABLE tarea ADD CONSTRAINT fk_tarea_parent
    FOREIGN KEY (tarea_parent_id) REFERENCES tarea (id) ON DELETE SET NULL;

CREATE INDEX idx_tarea_parent_id ON tarea (tarea_parent_id);

COMMENT ON COLUMN tarea.tarea_parent_id IS 'FK a la tarea padre KAOS (nullable). Si existe, esta tarea es subtarea de la referenciada.';

--rollback DROP INDEX idx_tarea_parent_id;
--rollback ALTER TABLE tarea DROP CONSTRAINT fk_tarea_parent;
--rollback ALTER TABLE tarea DROP COLUMN tarea_parent_id;
