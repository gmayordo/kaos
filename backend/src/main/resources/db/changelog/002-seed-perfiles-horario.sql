--liquibase formatted sql

--changeset kaos:002 labels:horario,seed
--comment: Seed de perfiles de horario España y Chile

INSERT INTO perfil_horario (nombre, zona_horaria, horas_lunes, horas_martes, horas_miercoles, horas_jueves, horas_viernes, created_at)
VALUES
    ('España', 'Europe/Madrid', 8.50, 8.50, 8.50, 8.50, 7.00, CURRENT_TIMESTAMP),
    ('Chile', 'America/Santiago', 9.00, 9.00, 9.00, 9.00, 8.00, CURRENT_TIMESTAMP);

--rollback DELETE FROM perfil_horario WHERE nombre IN ('España', 'Chile');
