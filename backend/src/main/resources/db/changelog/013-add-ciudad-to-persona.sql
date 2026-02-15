--liquibase formatted sql

--changeset maxwell:013-add-ciudad-to-persona labels:persona
--comment: Agregar campo ciudad a Persona para vincular con calendario laboral

ALTER TABLE persona ADD COLUMN ciudad VARCHAR(100);

COMMENT ON COLUMN persona.ciudad IS 'Ciudad de la persona para calendario laboral (Zaragoza, Valencia, Temuco)';

-- Actualizar personas existentes con ciudad por defecto según zona horaria
UPDATE persona SET ciudad = 'Zaragoza' 
WHERE perfil_horario_id IN (
    SELECT id FROM perfil_horario WHERE zona_horaria LIKE 'Europe/Madrid%'
);

UPDATE persona SET ciudad = 'Temuco' 
WHERE perfil_horario_id IN (
    SELECT id FROM perfil_horario WHERE zona_horaria LIKE 'America/Santiago%'
);

-- Hacer NOT NULL después de asignar valores
ALTER TABLE persona ALTER COLUMN ciudad SET NOT NULL;

--rollback ALTER TABLE persona DROP COLUMN ciudad;
