--liquibase formatted sql

--changeset kaos:029 labels:seed,jira
--comment: Seed configuración Jira real — servidor umane.emeal.nttdata.com, usuario usr_virtualcare

-- ═══════════════════════════════════════════════════════════════════════
-- JIRA CONFIG por squad
--
-- URL:     https://umane.emeal.nttdata.com/jiraito
-- Usuario: usr_virtualcare
-- Token:   contraseña del usuario usr_virtualcare en Jira
--
-- board_evolutivo_id / board_correctivo_id → valor del campo custom cf[24140]
--   (campo "equipo" en Jira que identifica a qué squad pertenece cada issue)
--   red   → 97993 (id_squad_evol_jira)
--   green → 97992
--   blue  → 97991
--   Los valores id_squad_corr_jira (22517/22516/22515) NO son cf[24140].
--
-- mapeo_estados → estados reales del board en español (verificado Sprint 14-24)
-- ═══════════════════════════════════════════════════════════════════════

INSERT INTO jira_config (
    squad_id,
    url,
    usuario,
    token,
    board_correctivo_id,
    board_evolutivo_id,
    load_method,
    activa,
    mapeo_estados,
    created_at
)
SELECT
    s.id,
    'https://umane.emeal.nttdata.com/jiraito',
    'usr_virtualcare',
    'IvylVNnMpFES8xL4Q2wmLfUwq45USq8pDjwwFG',  -- ← sustituir por la contraseña real de usr_virtualcare en Jira
    CAST(s.id_squad_evol_jira AS BIGINT),  -- board_correctivo_id usa mismo cf[24140] que evolutivo
    CAST(s.id_squad_evol_jira AS BIGINT),  -- board_evolutivo_id → valor de cf[24140] del squad
    'API_REST',
    TRUE,
    '{"Abierta":"PENDIENTE","Under Study":"PENDIENTE","En curso":"EN_PROGRESO","Bloqueada":"BLOQUEADA","Entregada":"EN_REVISION","Cerrada":"COMPLETADA"}',
    CURRENT_TIMESTAMP
FROM squad s
WHERE s.nombre IN ('red', 'green', 'blue')
  AND NOT EXISTS (
      SELECT 1 FROM jira_config jc WHERE jc.squad_id = s.id
  );

--rollback DELETE FROM jira_config WHERE usuario = 'usr_virtualcare';
