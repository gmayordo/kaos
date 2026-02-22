--liquibase formatted sql

--changeset gmayordo:010 labels:bloque-2-calendario
--comment: Seed festivos nacionales España 2026

-- ═══════════════════════════════════════════════
-- FESTIVOS NACIONALES ESPAÑA 2026
-- Solo se asignan a personas con perfil_horario España (id=1)
-- ═══════════════════════════════════════════════

-- Insertar festivos
INSERT INTO festivo (fecha, descripcion, tipo, created_at) VALUES
    ('2026-01-01', 'Año Nuevo',         'NACIONAL', CURRENT_TIMESTAMP),
    ('2026-01-06', 'Reyes Magos',       'NACIONAL', CURRENT_TIMESTAMP),
    ('2026-10-12', 'Fiesta Nacional',   'NACIONAL', CURRENT_TIMESTAMP);

-- Asignar festivos a todas las personas de España (perfil_horario_id = 1)
-- Subquery para resolver IDs dinámicamente

-- Año Nuevo 2026
INSERT INTO persona_festivo (festivo_id, persona_id)
SELECT f.id, p.id
FROM festivo f, persona p
WHERE f.fecha = '2026-01-01' 
  AND f.descripcion = 'Año Nuevo'
  AND p.perfil_horario_id = 1;

-- Reyes Magos 2026
INSERT INTO persona_festivo (festivo_id, persona_id)
SELECT f.id, p.id
FROM festivo f, persona p
WHERE f.fecha = '2026-01-06' 
  AND f.descripcion = 'Reyes Magos'
  AND p.perfil_horario_id = 1;

-- Fiesta Nacional 2026
INSERT INTO persona_festivo (festivo_id, persona_id)
SELECT f.id, p.id
FROM festivo f, persona p
WHERE f.fecha = '2026-10-12' 
  AND f.descripcion = 'Fiesta Nacional'
  AND p.perfil_horario_id = 1;

--rollback DELETE FROM persona_festivo WHERE festivo_id IN (SELECT id FROM festivo WHERE fecha IN ('2026-01-01', '2026-01-06', '2026-10-12'));
--rollback DELETE FROM festivo WHERE fecha IN ('2026-01-01', '2026-01-06', '2026-10-12');
