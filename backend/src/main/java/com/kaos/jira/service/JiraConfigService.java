package com.kaos.jira.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.client.JiraApiClient;
import com.kaos.jira.config.JiraLoadConfig;
import com.kaos.jira.config.JiraLoadMethod;
import com.kaos.jira.dto.JiraConfigRequest;
import com.kaos.jira.dto.JiraConfigResponse;
import com.kaos.jira.entity.JiraConfig;
import com.kaos.jira.repository.JiraConfigRepository;
import com.kaos.squad.entity.Squad;
import com.kaos.squad.repository.SquadRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para gestión y consulta de la configuración Jira.
 *
 * <p>Proporciona operaciones de lectura de configuración, cambio de método
 * de carga en caliente y prueba de conectividad.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JiraConfigService {

    private final JiraConfigRepository jiraConfigRepository;
    private final JiraLoadConfig jiraLoadConfig;
    private final JiraApiClient jiraApiClient;
    private final SquadRepository squadRepository;

    // ── Consulta de configuración ─────────────────────────────────────────────

    /**
     * Obtiene la configuración activa de Jira para un squad.
     * El token se oculta en la respuesta.
     *
     * @param squadId ID del squad
     * @return DTO con la configuración (token ofuscado)
     * @throws EntityNotFoundException si no existe configuración activa
     */
    @Transactional(readOnly = true)
    public JiraConfigResponse obtenerConfig(Long squadId) {
        JiraConfig config = jiraConfigRepository.findBySquadIdAndActivaTrue(squadId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe configuración Jira activa para el squad " + squadId));

        return toResponse(config);
    }

    // ── Cambio de método en caliente ─────────────────────────────────────────

    /**
     * Cambia el método de carga de Jira en caliente (sin reiniciar el servidor).
     *
     * <p>El cambio afecta globalmente a todos los squads.</p>
     *
     * @param nuevoMetodo nombre del método (API_REST / SELENIUM / LOCAL)
     * @return método activo tras el cambio
     * @throws IllegalArgumentException si el valor es inválido
     */
    public JiraLoadMethod cambiarMetodo(String nuevoMetodo) {
        JiraLoadMethod metodo = JiraLoadMethod.fromString(nuevoMetodo);
        JiraLoadMethod anterior = jiraLoadConfig.getCurrentMethod();
        jiraLoadConfig.setCurrentMethod(metodo);
        log.info("[JiraConfigService] Método cambiado: {} → {}", anterior, metodo);
        return metodo;
    }

    // ── Prueba de conectividad ────────────────────────────────────────────────

    /**
     * Prueba la conectividad con el servidor Jira de un squad.
     *
     * <p>Solo funciona con el método API_REST (el método SELENIUM requiere
     * navegador y no es adecuado para health-checks).</p>
     *
     * @param squadId ID del squad a probar
     * @return {@code true} si la conexión es exitosa
     * @throws EntityNotFoundException si no hay configuración activa
     */
    @Transactional(readOnly = true)
    public boolean probarConexion(Long squadId) {
        JiraConfig config = jiraConfigRepository.findBySquadIdAndActivaTrue(squadId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe configuración Jira activa para el squad " + squadId));

        log.info("[JiraConfigService] Probando conexión Jira para squad {}", squadId);
        return jiraApiClient.probarConexion(config);
    }

    // ── Crear / Actualizar configuración ─────────────────────────────────────

    /**
     * Crea o actualiza la configuración Jira de un squad.
     *
     * <p>Si ya existe una configuración para el squad, la actualiza.
     * Si no existe, crea una nueva.</p>
     *
     * <p>Si el campo {@code token} llega vacío o null, el token existente
     * en base de datos se mantiene intacto (útil al editar sin re-introducir).</p>
     *
     * @param squadId ID del squad
     * @param request datos de la configuración
     * @return configuración guardada con token ofuscado
     * @throws EntityNotFoundException  si el squad no existe
     * @throws IllegalArgumentException si es una config nueva y el token está vacío
     */
    @Transactional
    public JiraConfigResponse guardarConfig(Long squadId, JiraConfigRequest request) {
        Squad squad = squadRepository.findById(squadId)
                .orElseThrow(() -> new EntityNotFoundException("Squad no encontrado: " + squadId));

        JiraConfig config = jiraConfigRepository.findBySquadId(squadId)
                .orElse(JiraConfig.builder().squad(squad).build());

        config.setUrl(request.url().trim());
        config.setUsuario(request.usuario().trim());
        if (request.token() != null && !request.token().isBlank()) {
            config.setToken(request.token().trim());
        } else if (config.getToken() == null) {
            throw new IllegalArgumentException("El token es obligatorio al crear una configuración nueva");
        }
        config.setBoardCorrectivoId(request.boardCorrectivoId());
        config.setBoardEvolutivoId(request.boardEvolutivoId());
        config.setLoadMethod(request.loadMethod());
        config.setActiva(request.activa());
        config.setMapeoEstados(request.mapeoEstados());

        JiraConfig saved = jiraConfigRepository.save(config);
        log.info("[JiraConfigService] Configuración Jira guardada para squad {}", squadId);
        return toResponse(saved);
    }

    // ── Mapper privado ────────────────────────────────────────────────────────

    private JiraConfigResponse toResponse(JiraConfig config) {
        return new JiraConfigResponse(
                config.getSquad().getId(),
                config.getSquad().getNombre(),
                config.getUrl(),
                config.getUsuario(),
                config.getToken() != null ? "****" : "no configurado",
                config.getLoadMethod(),
                config.isActiva(),
                config.getMapeoEstados()
        );
    }
}
