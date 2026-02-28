--liquibase formatted sql

--changeset kaos:030 labels:fix,jira
--comment: Corrige board_correctivo_id en jira_config — usa cf[24140] (id_squad_evol_jira), no id_squad_corr_jira

-- Los valores id_squad_corr_jira (22517/22516/22515) NO son el campo custom cf[24140] de Jira.
-- El campo cf[24140] usa los mismos valores que id_squad_evol_jira (97993/97992/97991).
-- Ambos board_evolutivo_id y board_correctivo_id deben contener el valor cf[24140] del squad.

UPDATE jira_config jc
SET board_correctivo_id = CAST(s.id_squad_evol_jira AS BIGINT)
FROM squad s
WHERE jc.squad_id = s.id
  AND s.nombre IN ('red', 'green', 'blue');

--rollback UPDATE jira_config jc SET board_correctivo_id = CAST(s.id_squad_corr_jira AS BIGINT) FROM squad s WHERE jc.squad_id = s.id AND s.nombre IN ('red', 'green', 'blue');
