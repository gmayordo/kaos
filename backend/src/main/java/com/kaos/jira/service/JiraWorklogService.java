package com.kaos.jira.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.jira.dto.JiraWorklogResponse;
import com.kaos.jira.dto.WorklogDiaResponse;
import com.kaos.jira.dto.WorklogRequest;
import com.kaos.jira.dto.WorklogSemanaResponse;
import com.kaos.jira.entity.JiraIssue;
import com.kaos.jira.entity.JiraSyncQueue;
import com.kaos.jira.entity.JiraSyncQueue.EstadoOperacion;
import com.kaos.jira.entity.JiraSyncQueue.TipoOperacion;
import com.kaos.jira.entity.JiraWorklog;
import com.kaos.jira.entity.WorklogOrigen;
import com.kaos.jira.repository.JiraIssueRepository;
import com.kaos.jira.repository.JiraSyncQueueRepository;
import com.kaos.jira.repository.JiraWorklogRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Servicio para la gestión de imputaciones de tiempo (worklogs).
 *
 * <p>Permite registrar, editar y eliminar worklogs creados desde KAOS
 * (origen=KAOS, sincronizado=false), así como consultar la vista de Mi Día
 * y Mi Semana por persona.</p>
 *
 * <p>Al registrar o editar un worklog, si los datos son consistentes, se
 * encola una operación POST_WORKLOG en {@code jira_sync_queue} para que el
 * batch la envíe a Jira en diferido.</p>
 */
@Service
@Transactional(readOnly = true)
public class JiraWorklogService {

    private static final Logger log = LoggerFactory.getLogger(JiraWorklogService.class);

    private static final BigDecimal CINCO = new BigDecimal("5");
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JiraWorklogRepository worklogRepository;
    private final JiraIssueRepository issueRepository;
    private final PersonaRepository personaRepository;
    private final JiraSyncQueueRepository syncQueueRepository;

    public JiraWorklogService(
            JiraWorklogRepository worklogRepository,
            JiraIssueRepository issueRepository,
            PersonaRepository personaRepository,
            JiraSyncQueueRepository syncQueueRepository) {
        this.worklogRepository = worklogRepository;
        this.issueRepository = issueRepository;
        this.personaRepository = personaRepository;
        this.syncQueueRepository = syncQueueRepository;
    }

    // ─────────────────────────────────────────────
    // Consultas de vista
    // ─────────────────────────────────────────────

    /**
     * Vista "Mi Día": devuelve todas las imputaciones de una persona en una fecha,
     * junto con la capacidad diaria calculada desde su perfil horario.
     *
     * @param personaId ID de la persona
     * @param fecha     fecha a consultar (null = hoy)
     * @return resumen del día con líneas de worklog
     */
    public WorklogDiaResponse getMiDia(Long personaId, LocalDate fecha) {
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        Persona persona = resolverPersona(personaId);
        BigDecimal capacidad = calcularCapacidadDiaria(persona);

        List<JiraWorklog> worklogs = worklogRepository.findByPersonaIdAndFecha(personaId, fecha);

        BigDecimal horasImputadas = worklogs.stream()
                .map(JiraWorklog::getHoras)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean jornadaCompleta = horasImputadas.compareTo(capacidad) >= 0;

        List<WorklogDiaResponse.WorklogLineaResponse> lineas = worklogs.stream()
                .map(this::toLineaResponse)
                .toList();

        log.debug("getMiDia: persona={} fecha={} worklogs={} horas={}",
                personaId, fecha, worklogs.size(), horasImputadas);

        return new WorklogDiaResponse(
                fecha,
                personaId,
                persona.getNombre(),
                capacidad,
                horasImputadas,
                jornadaCompleta,
                lineas
        );
    }

    /**
     * Vista "Mi Semana": devuelve la grid de imputaciones de una persona para
     * la semana ISO que contiene la fecha indicada (lunes → viernes).
     *
     * @param personaId    ID de la persona
     * @param fechaSemana  cualquier día dentro de la semana; null = semana actual
     * @return grid semana×tarea con totales
     */
    public WorklogSemanaResponse getMiSemana(Long personaId, LocalDate fechaSemana) {
        if (fechaSemana == null) {
            fechaSemana = LocalDate.now();
        }
        LocalDate lunes = fechaSemana.with(DayOfWeek.MONDAY);
        LocalDate viernes = lunes.plusDays(4);

        Persona persona = resolverPersona(personaId);
        BigDecimal capacidadDia = calcularCapacidadDiaria(persona);

        List<JiraWorklog> worklogs = worklogRepository.findByPersonaAndFechaRange(personaId, lunes, viernes);

        // Agrupar por issue (linked map para preservar order de inserción)
        Map<String, List<JiraWorklog>> porIssue = new LinkedHashMap<>();
        for (JiraWorklog w : worklogs) {
            String key = w.getJiraIssue().getJiraKey();
            porIssue.computeIfAbsent(key, k -> new ArrayList<>()).add(w);
        }

        List<WorklogSemanaResponse.FilaTareaResponse> filas = new ArrayList<>();
        for (Map.Entry<String, List<JiraWorklog>> entry : porIssue.entrySet()) {
            String jiraKey = entry.getKey();
            List<JiraWorklog> issueWorklogs = entry.getValue();
            String summary = issueWorklogs.get(0).getJiraIssue().getSummary();

            List<WorklogSemanaResponse.CeldaDiaResponse> celdas = construirCeldas(lunes, issueWorklogs);

            BigDecimal totalTarea = issueWorklogs.stream()
                    .map(JiraWorklog::getHoras)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            filas.add(new WorklogSemanaResponse.FilaTareaResponse(jiraKey, summary, celdas, totalTarea));
        }

        BigDecimal totalHoras = worklogs.stream()
                .map(JiraWorklog::getHoras)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCapacidad = capacidadDia.multiply(new BigDecimal("5"));

        log.debug("getMiSemana: persona={} semana={}/{} tareas={} horas={}",
                personaId, lunes, viernes, filas.size(), totalHoras);

        return new WorklogSemanaResponse(
                lunes,
                viernes,
                personaId,
                persona.getNombre(),
                capacidadDia,
                totalHoras,
                totalCapacidad,
                filas
        );
    }

    /**
     * Devuelve todos los worklogs asociados a una issue Jira, ordenados por fecha.
     *
     * @param jiraKey clave de la issue (p.ej. KAOS-123)
     * @return lista de worklogs
     */
    public List<JiraWorklogResponse> getByIssue(String jiraKey) {
        return worklogRepository.findByJiraIssueJiraKey(jiraKey)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────

    /**
     * Registra un nuevo worklog desde KAOS y lo encola para sincronización diferida.
     *
     * @param request datos del worklog
     * @return worklog creado
     * @throws EntityNotFoundException  si la issue o persona no existen
     * @throws IllegalArgumentException si supera la capacidad diaria
     */
    @Transactional
    public JiraWorklogResponse registrar(WorklogRequest request) {
        JiraIssue issue = issueRepository.findByJiraKey(request.jiraKey())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Issue no encontrada: " + request.jiraKey()));

        Persona persona = resolverPersona(request.personaId());
        LocalDate fecha = LocalDate.parse(request.fecha(), FECHA_FMT);

        validarCapacidadDiaria(persona, fecha, BigDecimal.ZERO, request.horas());

        JiraWorklog worklog = JiraWorklog.builder()
                .jiraIssue(issue)
                .persona(persona)
                .fecha(fecha)
                .horas(request.horas())
                .comentario(request.comentario())
                .origen(WorklogOrigen.KAOS)
                .sincronizado(false)
                .build();

        worklog = worklogRepository.save(worklog);

        encolarPostWorklog(worklog);

        log.info("Worklog registrado: id={} issue={} persona={} fecha={} horas={}",
                worklog.getId(), request.jiraKey(), persona.getNombre(), fecha, request.horas());

        return toResponse(worklog);
    }

    /**
     * Edita un worklog ya existente. Solo es posible si aún no ha sido sincronizado.
     *
     * @param id      ID del worklog
     * @param request nuevos datos
     * @return worklog actualizado
     * @throws IllegalStateException si el worklog ya fue sincronizado con Jira
     */
    @Transactional
    public JiraWorklogResponse editar(Long id, WorklogRequest request) {
        JiraWorklog worklog = resolverWorklog(id);
        verificarNoSincronizado(worklog);

        JiraIssue issue = issueRepository.findByJiraKey(request.jiraKey())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Issue no encontrada: " + request.jiraKey()));

        Persona persona = resolverPersona(request.personaId());
        LocalDate fecha = LocalDate.parse(request.fecha(), FECHA_FMT);

        // Excluir las horas actuales del mismo worklog para no contar doble
        validarCapacidadDiaria(persona, fecha, worklog.getHoras(), request.horas());

        worklog.setJiraIssue(issue);
        worklog.setPersona(persona);
        worklog.setFecha(fecha);
        worklog.setHoras(request.horas());
        worklog.setComentario(request.comentario());

        worklog = worklogRepository.save(worklog);

        log.info("Worklog editado: id={} issue={} horas={}", id, request.jiraKey(), request.horas());

        return toResponse(worklog);
    }

    /**
     * Elimina un worklog. Solo es posible si aún no ha sido sincronizado.
     *
     * @param id ID del worklog
     * @throws IllegalStateException si el worklog ya fue sincronizado
     */
    @Transactional
    public void eliminar(Long id) {
        JiraWorklog worklog = resolverWorklog(id);
        verificarNoSincronizado(worklog);
        worklogRepository.delete(worklog);
        log.info("Worklog eliminado: id={}", id);
    }

    // ─────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────

    private Persona resolverPersona(Long personaId) {
        return personaRepository.findById(personaId)
                .orElseThrow(() -> new EntityNotFoundException("Persona no encontrada: " + personaId));
    }

    private JiraWorklog resolverWorklog(Long id) {
        return worklogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Worklog no encontrado: " + id));
    }

    private void verificarNoSincronizado(JiraWorklog worklog) {
        if (worklog.isSincronizado()) {
            throw new IllegalStateException(
                    "No se puede modificar el worklog " + worklog.getId()
                    + " porque ya fue sincronizado con Jira");
        }
    }

    /**
     * Capacidad diaria = totalSemanal del perfil / 5 días laborables.
     */
    private BigDecimal calcularCapacidadDiaria(Persona persona) {
        if (persona.getPerfilHorario() == null) {
            return new BigDecimal("8.00");
        }
        return persona.getPerfilHorario().getTotalSemanal()
                .divide(CINCO, 2, RoundingMode.HALF_UP);
    }

    /**
     * Valida que añadir {@code horasNuevas} no supere la capacidad diaria,
     * descontando {@code horasActuales} del mismo worklog (0 si es nuevo).
     */
    private void validarCapacidadDiaria(Persona persona, LocalDate fecha,
                                         BigDecimal horasActuales, BigDecimal horasNuevas) {
        BigDecimal capacidad = calcularCapacidadDiaria(persona);
        BigDecimal yaImputadas = worklogRepository.sumHorasByPersonaAndFecha(persona.getId(), fecha);
        BigDecimal totalTras = yaImputadas.subtract(horasActuales).add(horasNuevas);

        if (totalTras.compareTo(capacidad) > 0) {
            throw new IllegalArgumentException(
                    String.format("La imputación supera la capacidad diaria de la persona (%s h). " +
                                  "Ya imputadas: %s h, solicitadas: %s h, capacidad: %s h",
                            persona.getNombre(), yaImputadas.subtract(horasActuales), horasNuevas, capacidad));
        }
    }

    /**
     * Construye la cuadrícula de 5 días (lunes→viernes) para una issue.
     * Las celdas sin imputación tienen horas=0 y worklogId=null.
     */
    private List<WorklogSemanaResponse.CeldaDiaResponse> construirCeldas(
            LocalDate lunes, List<JiraWorklog> issueWorklogs) {
        List<WorklogSemanaResponse.CeldaDiaResponse> celdas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LocalDate dia = lunes.plusDays(i);
            JiraWorklog w = issueWorklogs.stream()
                    .filter(x -> x.getFecha().equals(dia))
                    .findFirst()
                    .orElse(null);
            if (w != null) {
                celdas.add(new WorklogSemanaResponse.CeldaDiaResponse(dia, w.getHoras(), w.getId()));
            } else {
                celdas.add(new WorklogSemanaResponse.CeldaDiaResponse(dia, BigDecimal.ZERO, null));
            }
        }
        return celdas;
    }

    /**
     * Encola una operación POST_WORKLOG para sincronización diferida.
     * Si ya existe una operación PENDIENTE del mismo tipo para esta issue, no duplica.
     */
    private void encolarPostWorklog(JiraWorklog worklog) {
        if (worklog.getJiraIssue().getSquad() == null) {
            log.warn("Worklog {} no tiene squad asociado; no se puede encolar", worklog.getId());
            return;
        }
        var squad = worklog.getJiraIssue().getSquad();
        boolean yaEncolado = syncQueueRepository
                .findBySquadIdAndTipoOperacionAndEstado(
                        squad.getId(),
                        TipoOperacion.POST_WORKLOG,
                        EstadoOperacion.PENDIENTE)
                .stream()
                .anyMatch(q -> String.valueOf(worklog.getId()).equals(q.getPayload()));

        if (!yaEncolado) {
            var op = JiraSyncQueue.builder()
                    .squad(squad)
                    .tipoOperacion(TipoOperacion.POST_WORKLOG)
                    .estado(EstadoOperacion.PENDIENTE)
                    .payload(String.valueOf(worklog.getId()))
                    .maxIntentos(3)
                    .programadaPara(LocalDateTime.now())
                    .build();
            syncQueueRepository.save(op);
            log.debug("POST_WORKLOG encolado para worklog {}", worklog.getId());
        }
    }

    // ─────────────────────────────────────────────
    // Mappers
    // ─────────────────────────────────────────────

    private JiraWorklogResponse toResponse(JiraWorklog w) {
        return new JiraWorklogResponse(
                w.getId(),
                w.getJiraIssue().getJiraKey(),
                w.getJiraIssue().getSummary(),
                w.getPersona().getId(),
                w.getPersona().getNombre(),
                w.getFecha(),
                w.getHoras(),
                w.getComentario(),
                w.getOrigen(),
                w.isSincronizado()
        );
    }

    private WorklogDiaResponse.WorklogLineaResponse toLineaResponse(JiraWorklog w) {
        return new WorklogDiaResponse.WorklogLineaResponse(
                w.getId(),
                w.getJiraIssue().getJiraKey(),
                w.getJiraIssue().getSummary(),
                w.getHoras(),
                w.getComentario(),
                w.isSincronizado()
        );
    }
}
