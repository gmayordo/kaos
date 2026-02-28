--liquibase formatted sql

--changeset kaos:038 labels:fix,jira
--comment: Corrige mapeoEstados — los estados reales de Jira llegan en inglés (Open/In Progress/Delivered/Closed)

UPDATE jira_config
SET mapeo_estados = '{"Open":"PENDIENTE","Under Study":"PENDIENTE","In Progress":"EN_PROGRESO","In Review":"EN_PROGRESO","Delivered":"COMPLETADA","Closed":"COMPLETADA"}'
WHERE activa = TRUE;

--rollback UPDATE jira_config SET mapeo_estados = '{"Abierta":"PENDIENTE","Under Study":"PENDIENTE","En curso":"EN_PROGRESO","Bloqueada":"BLOQUEADA","Entregada":"EN_REVISION","Cerrada":"COMPLETADA"}' WHERE activa = TRUE;
