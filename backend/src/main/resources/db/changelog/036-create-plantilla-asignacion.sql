--liquibase formatted sql

--changeset maxwell:036 labels:planificacion
--comment: Crea tablas plantilla_asignacion y plantilla_asignacion_linea con seed inicial (bloque 5)

CREATE TABLE plantilla_asignacion (
    id          BIGSERIAL    PRIMARY KEY,
    -- Nombre descriptivo de la plantilla
    nombre      VARCHAR(100) NOT NULL,
    -- Tipo de issue Jira al que aplica (Story, Bug, Task, Sub-task, Spike)
    tipo_jira   VARCHAR(50)  NOT NULL,
    -- Si false, la plantilla no se ofrece en la UI pero permanece en BD
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Audit
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(100),
    CONSTRAINT uk_plantilla_nombre UNIQUE (nombre)
);

COMMENT ON TABLE  plantilla_asignacion         IS 'Plantillas de distribución de horas para planificación automática de issues Jira';
COMMENT ON COLUMN plantilla_asignacion.nombre  IS 'Nombre descriptivo único de la plantilla';
COMMENT ON COLUMN plantilla_asignacion.tipo_jira IS 'Tipo de issue Jira al que aplica: Story, Bug, Task, Sub-task, Spike';
COMMENT ON COLUMN plantilla_asignacion.activo  IS 'Si false la plantilla está desactivada y no se aplica automáticamente';

CREATE INDEX idx_plantilla_tipo_activo ON plantilla_asignacion(tipo_jira, activo);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE plantilla_asignacion_linea (
    id                  BIGSERIAL    PRIMARY KEY,
    -- Plantilla a la que pertenece esta línea
    plantilla_id        BIGINT       NOT NULL REFERENCES plantilla_asignacion(id) ON DELETE CASCADE,
    -- Rol de la persona que recibirá esta fracción de horas
    rol                 VARCHAR(20)  NOT NULL,
    -- Porcentaje de horas que le corresponde a este rol (>0, ≤100)
    porcentaje_horas    INT          NOT NULL,
    -- Orden de la línea dentro de la plantilla (para presentación)
    orden               INT          NOT NULL DEFAULT 0,
    -- Si este ítem depende de que se complete el ítem con ese orden primero (nullable)
    depende_de_orden    INT,
    -- Audit
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_plantilla_porcentaje CHECK (porcentaje_horas > 0 AND porcentaje_horas <= 100),
    CONSTRAINT chk_plantilla_rol CHECK (rol IN ('DESARROLLADOR','QA','TECH_LEAD','FUNCIONAL','OTRO'))
);

COMMENT ON TABLE  plantilla_asignacion_linea                  IS 'Líneas de una plantilla de asignación: cada una define un rol y su % de horas';
COMMENT ON COLUMN plantilla_asignacion_linea.plantilla_id     IS 'FK a plantilla_asignacion';
COMMENT ON COLUMN plantilla_asignacion_linea.rol              IS 'Rol KAOS: DESARROLLADOR, QA, TECH_LEAD, FUNCIONAL, OTRO';
COMMENT ON COLUMN plantilla_asignacion_linea.porcentaje_horas IS '% de la estimación total que se asigna a este rol (1..100)';
COMMENT ON COLUMN plantilla_asignacion_linea.orden            IS 'Posición dentro de la plantilla (para ordenación en UI)';
COMMENT ON COLUMN plantilla_asignacion_linea.depende_de_orden IS 'Orden del ítem del que depende este ítem (nullable)';

CREATE INDEX idx_plantilla_linea_plantilla ON plantilla_asignacion_linea(plantilla_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Seed: plantillas iniciales
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO plantilla_asignacion (nombre, tipo_jira, activo, created_by) VALUES
    ('Story estándar', 'Story',   TRUE, 'seed'),
    ('Bug estándar',   'Bug',     TRUE, 'seed'),
    ('Task estándar',  'Task',    TRUE, 'seed');

INSERT INTO plantilla_asignacion_linea (plantilla_id, rol, porcentaje_horas, orden) VALUES
    -- Story: 70% dev + 30% QA
    ((SELECT id FROM plantilla_asignacion WHERE nombre = 'Story estándar'), 'DESARROLLADOR', 70, 1),
    ((SELECT id FROM plantilla_asignacion WHERE nombre = 'Story estándar'), 'QA',            30, 2),
    -- Bug: 100% dev
    ((SELECT id FROM plantilla_asignacion WHERE nombre = 'Bug estándar'),   'DESARROLLADOR', 100, 1),
    -- Task: 100% dev
    ((SELECT id FROM plantilla_asignacion WHERE nombre = 'Task estándar'),  'DESARROLLADOR', 100, 1);

--rollback DELETE FROM plantilla_asignacion_linea WHERE plantilla_id IN (SELECT id FROM plantilla_asignacion WHERE created_by = 'seed');
--rollback DELETE FROM plantilla_asignacion WHERE created_by = 'seed';
--rollback DROP TABLE plantilla_asignacion_linea;
--rollback DROP TABLE plantilla_asignacion;
