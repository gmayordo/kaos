--liquibase formatted sql

--changeset maxwell:027 labels:jira,alertas,seed
--comment: Seed de las 7 reglas predefinidas de coherencia Jira←→KAOS

-- ──────────────────────────────────────────────────────────
-- Regla 1: Tarea Done en Jira pero no Completada en KAOS
-- Severidad CRITICO — puede indicar cierre manual en Jira sin sincronizar KAOS
-- ──────────────────────────────────────────────────────────
INSERT INTO jira_alert_rule (nombre, tipo, condicion_spel, mensaje_template, severidad, umbral_valor, activa)
VALUES (
    'tarea-done-jira-activa-kaos',
    'ESTADO_INCOHERENTE',
    '#issue.estadoJira == ''Done'' && #tarea.estado != ''COMPLETADA''',
    'PROJ-{jiraKey}: Estado Done en Jira pero {estadoKaos} en KAOS',
    'CRITICO',
    NULL,
    TRUE
);

-- ──────────────────────────────────────────────────────────
-- Regla 2: Tarea Completada en KAOS pero aún abierta en Jira
-- Severidad AVISO — el dev cerró en KAOS pero olvidó actualizar Jira
-- ──────────────────────────────────────────────────────────
INSERT INTO jira_alert_rule (nombre, tipo, condicion_spel, mensaje_template, severidad, umbral_valor, activa)
VALUES (
    'tarea-completada-kaos-abierta-jira',
    'ESTADO_INCOHERENTE',
    '#tarea.estado == ''COMPLETADA'' && #issue.estadoJira != ''Done''',
    'PROJ-{jiraKey}: Completada en KAOS pero {estadoJira} en Jira',
    'AVISO',
    NULL,
    TRUE
);

-- ──────────────────────────────────────────────────────────
-- Regla 3: Estimación 0h o nula
-- Severidad AVISO — impide calcular capacidad y detectar desviaciones
-- ──────────────────────────────────────────────────────────
INSERT INTO jira_alert_rule (nombre, tipo, condicion_spel, mensaje_template, severidad, umbral_valor, activa)
VALUES (
    'estimacion-cero',
    'ESTIMACION_CERO',
    '#issue.estimacionHoras == null || #issue.estimacionHoras == 0',
    'PROJ-{jiraKey}: Sin estimación de horas — revisar en Jira',
    'AVISO',
    NULL,
    TRUE
);

-- ──────────────────────────────────────────────────────────
-- Regla 4: Persona sin imputar ≥ umbral días durante sprint activo
-- Severidad AVISO — umbral por defecto: 2 días (configurable)
-- ──────────────────────────────────────────────────────────
INSERT INTO jira_alert_rule (nombre, tipo, condicion_spel, mensaje_template, severidad, umbral_valor, activa)
VALUES (
    'persona-sin-imputar',
    'IMPUTACION_FALTANTE',
    '#diasSinImputar >= #regla.umbralValor',
    '{persona}: sin imputar {diasSinImputar} días (sprint activo)',
    'AVISO',
    2,
    TRUE
);

-- ──────────────────────────────────────────────────────────
-- Regla 5: Sprint en riesgo — desfase tiempo/completitud ≥ umbral
-- Severidad CRITICO — umbral por defecto: 30% (configurable)
-- ──────────────────────────────────────────────────────────
INSERT INTO jira_alert_rule (nombre, tipo, condicion_spel, mensaje_template, severidad, umbral_valor, activa)
VALUES (
    'sprint-en-riesgo',
    'SPRINT_EN_RIESGO',
    '#pctTiempo - #pctCompletitud >= #regla.umbralValor',
    'Sprint al {pctTiempo}% del tiempo pero solo {pctCompletitud}% completitud (desfase {delta}%)',
    'CRITICO',
    30,
    TRUE
);

-- ──────────────────────────────────────────────────────────
-- Regla 6: Tarea estancada en "En Progreso" ≥ umbral días
-- Severidad AVISO — umbral por defecto: 3 días (configurable)
-- ──────────────────────────────────────────────────────────
INSERT INTO jira_alert_rule (nombre, tipo, condicion_spel, mensaje_template, severidad, umbral_valor, activa)
VALUES (
    'tarea-estancada',
    'TAREA_ESTANCADA',
    '#diasEnProgreso >= #regla.umbralValor',
    'PROJ-{jiraKey}: {diasEnProgreso} días en "En Progreso" sin avance',
    'AVISO',
    3,
    TRUE
);

-- ──────────────────────────────────────────────────────────
-- Regla 7: Horas consumidas > estimadas × (umbral / 100)
-- Severidad AVISO — umbral por defecto: 120% = 1.2× la estimación
-- ──────────────────────────────────────────────────────────
INSERT INTO jira_alert_rule (nombre, tipo, condicion_spel, mensaje_template, severidad, umbral_valor, activa)
VALUES (
    'desviacion-horas',
    'DESVIACION_HORAS',
    '#issue.horasConsumidas > #issue.estimacionHoras * (#regla.umbralValor / 100)',
    'PROJ-{jiraKey}: {horasConsumidas}h consumidas vs {estimacion}h estimadas ({pct}% desviación)',
    'AVISO',
    120,
    TRUE
);

--rollback DELETE FROM jira_alert_rule WHERE nombre IN (
--rollback   'tarea-done-jira-activa-kaos','tarea-completada-kaos-abierta-jira',
--rollback   'estimacion-cero','persona-sin-imputar','sprint-en-riesgo',
--rollback   'tarea-estancada','desviacion-horas'
--rollback );
