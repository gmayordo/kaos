--liquibase formatted sql

--changeset kaos:006 labels:seed
--comment: Seed datos equipo real desde equipos.yaml — 3 squads, 17 personas, 19 squad_members

-- ═══════════════════════════════════════════════
-- SQUADS (datos reales de equipos.yaml)
-- ═══════════════════════════════════════════════

INSERT INTO squad (nombre, descripcion, estado, id_squad_corr_jira, id_squad_evol_jira, created_at) VALUES
    ('red',   'Squad Red',   'ACTIVO', '22517', '97993', CURRENT_TIMESTAMP),
    ('green', 'Squad Green', 'ACTIVO', '22516', '97992', CURRENT_TIMESTAMP),
    ('blue',  'Squad Blue',  'ACTIVO', '22515', '97991', CURRENT_TIMESTAMP);

-- ═══════════════════════════════════════════════
-- PERSONAS (17 únicas — datos reales)
-- perfil_horario_id: 1 = España (Europe/Madrid), 2 = Chile (America/Santiago)
-- ═══════════════════════════════════════════════

INSERT INTO persona (nombre, email, id_jira, perfil_horario_id, seniority, skills, activo, fecha_incorporacion, send_notifications, created_at) VALUES
    -- Squad Red
    ('Gerardo Mayordomo Pérez',           'gmayordo@emeal.nttdata.com', 'gmayordo', 1, 'LEAD',   'Java,Spring Boot,Arquitectura,DevOps',      TRUE, '2025-01-01', TRUE,  CURRENT_TIMESTAMP),
    ('David Izquierdo Escuer',            'dizquiee@emeal.nttdata.com', 'dizquiee', 1, 'SENIOR', 'React,TypeScript,CSS,Frontend',              TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Leslie Aguayo Duran',               'laguayod@emeal.nttdata.com', 'laguayod', 2, 'SENIOR', 'Análisis funcional,Jira,Documentación',      TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Ricardo Alejandro Villagran',       'rvillagr@emeal.nttdata.com', 'rvillagr', 2, 'SENIOR', 'Java,Spring Boot,PostgreSQL',                TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Rodrigo Garces Altamirano',         'rgarcesa@emeal.nttdata.com', 'rgarcesa', 2, 'SENIOR', 'Testing,QA,Selenium',                        TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    -- Scrum Master (compartido en 3 squads)
    ('Luis Galvan Moreno',                'lgalvanm@emeal.nttdata.com', 'lgalvanm', 1, 'LEAD',   'Scrum,Agile,Gestión de proyectos',           TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    -- Squad Green
    ('Javier Paredes Martinez',           'jparedma@emeal.nttdata.com', 'jparedma', 2, 'SENIOR', 'Java,Spring Boot,PostgreSQL',                TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Victoria Martinez Paredez',         'vmartipa@emeal.nttdata.com', 'vmartipa', 2, 'SENIOR', 'React,TypeScript,CSS,Frontend',              TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Javier Hurtado Tabasco',            'jhurtado@emeal.nttdata.com', 'jhurtado', 1, 'LEAD',   'Java,Spring Boot,Microservicios',            TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Andres Ulloa Vargas',               'aulloava@emeal.nttdata.com', 'aulloava', 2, 'SENIOR', 'Testing,QA,Automatización',                  TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Carlos Paños Huertas',              'cpanoshu@emeal.nttdata.com', 'cpanoshu', 1, 'SENIOR', 'Análisis funcional,UX,Documentación',        TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Ethan Mortes Esquer',               'emortese@emeal.nttdata.com', 'emortese', 1, 'SENIOR', 'Análisis funcional,QA,Testing',              TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    -- Squad Blue
    ('Edgardo Jesus Fuentealba Gonzalez', 'efuentea@emeal.nttdata.com', 'efuentea', 2, 'SENIOR', 'React,TypeScript,CSS,Frontend',              TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Angel Anda Enguidanos',             'aandaeng@emeal.nttdata.com', 'aandaeng', 1, 'LEAD',   'Java,PostgreSQL,Infraestructura',            TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Angela Lemonao Candia',             'alemonao@emeal.nttdata.com', 'alemonao', 2, 'SENIOR', 'Testing,QA,Automatización',                  TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('German Flores Fernandez',           'gfloresf@emeal.nttdata.com', 'gfloresf', 2, 'SENIOR', 'Java,Spring Boot,PostgreSQL',                TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP),
    ('Marta Ruiz Server',                 'mruizser@emeal.nttdata.com', 'mruizser', 1, 'SENIOR', 'Gestión funcional,Scrum,Documentación',      TRUE, '2025-01-01', FALSE, CURRENT_TIMESTAMP);

-- ═══════════════════════════════════════════════
-- SQUAD MEMBERS (19 asignaciones)
-- Todos a 100% excepto Luis Galván SM: 33%+33%+34% = 100%
-- Subqueries para resolver IDs dinámicamente
-- ═══════════════════════════════════════════════

-- ── Squad Red (6 miembros) ──

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'LIDER_TECNICO', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'gmayordo@emeal.nttdata.com' AND s.nombre = 'red';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'FRONTEND', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'dizquiee@emeal.nttdata.com' AND s.nombre = 'red';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'LIDER_FUNCIONAL', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'laguayod@emeal.nttdata.com' AND s.nombre = 'red';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'BACKEND', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'rvillagr@emeal.nttdata.com' AND s.nombre = 'red';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'QA', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'rgarcesa@emeal.nttdata.com' AND s.nombre = 'red';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'SCRUM_MASTER', 33, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'lgalvanm@emeal.nttdata.com' AND s.nombre = 'red';

-- ── Squad Green (7 miembros) ──

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'LIDER_TECNICO', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'jhurtado@emeal.nttdata.com' AND s.nombre = 'green';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'FRONTEND', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'vmartipa@emeal.nttdata.com' AND s.nombre = 'green';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'BACKEND', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'jparedma@emeal.nttdata.com' AND s.nombre = 'green';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'QA', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'aulloava@emeal.nttdata.com' AND s.nombre = 'green';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'LIDER_FUNCIONAL', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'cpanoshu@emeal.nttdata.com' AND s.nombre = 'green';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'LIDER_FUNCIONAL', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'emortese@emeal.nttdata.com' AND s.nombre = 'green';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'SCRUM_MASTER', 33, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'lgalvanm@emeal.nttdata.com' AND s.nombre = 'green';

-- ── Squad Blue (6 miembros) ──

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'LIDER_TECNICO', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'aandaeng@emeal.nttdata.com' AND s.nombre = 'blue';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'FRONTEND', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'efuentea@emeal.nttdata.com' AND s.nombre = 'blue';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'BACKEND', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'gfloresf@emeal.nttdata.com' AND s.nombre = 'blue';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'QA', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'alemonao@emeal.nttdata.com' AND s.nombre = 'blue';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'LIDER_FUNCIONAL', 100, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'mruizser@emeal.nttdata.com' AND s.nombre = 'blue';

INSERT INTO squad_member (persona_id, squad_id, rol, porcentaje, fecha_inicio, created_at)
SELECT p.id, s.id, 'SCRUM_MASTER', 34, '2025-01-01', CURRENT_TIMESTAMP
FROM persona p, squad s WHERE p.email = 'lgalvanm@emeal.nttdata.com' AND s.nombre = 'blue';

--rollback DELETE FROM squad_member; DELETE FROM persona WHERE email LIKE '%@emeal.nttdata.com'; DELETE FROM squad WHERE nombre IN ('red', 'green', 'blue');
