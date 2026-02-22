--liquibase formatted sql

--changeset kaos:012 labels:calendario
--comment: Cambiar festivos de estar vinculados a personas a estar vinculados a ciudad

-- 1. Eliminar tabla de unión persona_festivo
DROP TABLE IF EXISTS persona_festivo;

-- 2. Agregar columna ciudad a festivo
ALTER TABLE festivo ADD COLUMN ciudad VARCHAR(100);

-- 3. Poblar ciudad con valor por defecto para datos existentes
UPDATE festivo SET ciudad = 'Madrid' WHERE ciudad IS NULL;

-- 4. Hacer ciudad NOT NULL
ALTER TABLE festivo ALTER COLUMN ciudad SET NOT NULL;

-- 5. Crear índice en ciudad
CREATE INDEX idx_festivo_ciudad ON festivo(ciudad);

-- 6. Actualizar comentarios
COMMENT ON COLUMN festivo.ciudad IS 'Ciudad del festivo (Madrid, Barcelona, Santiago, etc.)';

-- 7. Actualizar constraint unique para incluir ciudad
ALTER TABLE festivo DROP CONSTRAINT uk_festivo_fecha_descripcion;
ALTER TABLE festivo ADD CONSTRAINT uk_festivo_fecha_descripcion_ciudad 
    UNIQUE (fecha, descripcion, ciudad);

--rollback ALTER TABLE festivo DROP CONSTRAINT uk_festivo_fecha_descripcion_ciudad;
--rollback ALTER TABLE festivo ADD CONSTRAINT uk_festivo_fecha_descripcion UNIQUE (fecha, descripcion);
--rollback DROP INDEX IF EXISTS idx_festivo_ciudad;
--rollback ALTER TABLE festivo DROP COLUMN ciudad;
--rollback CREATE TABLE persona_festivo (festivo_id BIGINT NOT NULL REFERENCES festivo(id) ON DELETE CASCADE, persona_id BIGINT NOT NULL REFERENCES persona(id) ON DELETE CASCADE, PRIMARY KEY (festivo_id, persona_id));
