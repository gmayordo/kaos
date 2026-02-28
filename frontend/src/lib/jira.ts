/**
 * Utilidades Jira — URL base y helpers de enlaces.
 * Para cambiar el servidor Jira, editar JIRA_BASE_URL.
 */

export const JIRA_BASE_URL = "https://umane.emeal.nttdata.com/jiraito";

/**
 * Construye la URL de la issue en Jira.
 * @example jiraIssueUrl("EHCOSPROD-78181")
 * // → "https://umane.emeal.nttdata.com/jiraito/browse/EHCOSPROD-78181"
 */
export function jiraIssueUrl(jiraKey: string): string {
  return `${JIRA_BASE_URL}/browse/${jiraKey}`;
}
