--liquibase formatted sql

--changeset autor:011-update-vacacion-constraint
--comment: Actualizar constraint de dias_laborables para permitir 0 días (vacaciones en fin de semana)

-- Drop existing constraint
ALTER TABLE vacacion DROP CONSTRAINT chk_vacacion_dias_positivo;

-- Add new constraint allowing 0 or more days
ALTER TABLE vacacion ADD CONSTRAINT chk_vacacion_dias_positivo CHECK (dias_laborables >= 0);

--rollback ALTER TABLE vacacion DROP CONSTRAINT chk_vacacion_dias_positivo;
--rollback ALTER TABLE vacacion ADD CONSTRAINT chk_vacacion_dias_positivo CHECK (dias_laborables > 0);