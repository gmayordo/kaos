package com.kaos.calendario.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kaos.calendario.dto.CapacidadDiaResponse;
import com.kaos.calendario.dto.CapacidadPersonaResponse;
import com.kaos.calendario.dto.CapacidadSquadResponse;
import com.kaos.calendario.entity.Ausencia;
import com.kaos.calendario.entity.Festivo;
import com.kaos.calendario.entity.MotivoReduccion;
import com.kaos.calendario.entity.Vacacion;
import com.kaos.calendario.repository.AusenciaRepository;
import com.kaos.calendario.repository.FestivoRepository;
import com.kaos.calendario.repository.VacacionRepository;
import com.kaos.dedicacion.entity.SquadMember;
import com.kaos.dedicacion.repository.SquadMemberRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.squad.entity.Squad;
import com.kaos.squad.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para cálculo de capacidad de squads.
 * Motor de cálculo que considera: perfil horario, dedicación, festivos, vacaciones, ausencias.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CapacidadService {

    private final SquadRepository squadRepository;
    private final SquadMemberRepository squadMemberRepository;
    private final FestivoRepository festivoRepository;
    private final VacacionRepository vacacionRepository;
    private final AusenciaRepository ausenciaRepository;

    /**
     * Calcula la capacidad de un squad en un rango de fechas.
     * @param squadId ID del squad
     * @param fechaInicio Fecha inicio (inclusive)
     * @param fechaFin Fecha fin (inclusive)
     * @return Capacidad total del squad con detalle por persona y día
     */
    public CapacidadSquadResponse calcularCapacidad(Long squadId, LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Calculando capacidad del squad {} entre {} y {}", squadId, fechaInicio, fechaFin);

        // Validar fechas
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior o igual a la fecha de inicio");
        }

        // Validar squad existe
        Squad squad = squadRepository.findById(squadId)
                .orElseThrow(() -> new IllegalArgumentException("Squad no encontrado: " + squadId));

        // Obtener miembros activos del squad
        List<SquadMember> miembros = squadMemberRepository.findBySquadId(squadId);
        if (miembros.isEmpty()) {
            log.warn("Squad {} no tiene miembros activos", squadId);
            return new CapacidadSquadResponse(squadId, squad.getNombre(), fechaInicio, fechaFin, 0.0, List.of());
        }

        // Cargar festivos del rango
        List<Festivo> festivos = festivoRepository.findByAnio(fechaInicio.getYear());
        if (fechaInicio.getYear() != fechaFin.getYear()) {
            festivos.addAll(festivoRepository.findByAnio(fechaFin.getYear()));
        }

        // Cargar vacaciones del squad en rango
        List<Vacacion> vacaciones = vacacionRepository.findBySquadIdAndFechaRange(squadId, fechaInicio, fechaFin);

        // Cargar ausencias del squad en rango
        List<Ausencia> ausencias = ausenciaRepository.findBySquadIdAndFechaRange(squadId, fechaInicio, fechaFin);

        // Calcular capacidad por persona
        List<CapacidadPersonaResponse> capacidadesPersonas = new ArrayList<>();
        double horasTotalesSquad = 0.0;

        for (SquadMember miembro : miembros) {
            CapacidadPersonaResponse capacidadPersona = calcularCapacidadPersona(
                    miembro, fechaInicio, fechaFin, festivos, vacaciones, ausencias);
            capacidadesPersonas.add(capacidadPersona);
            horasTotalesSquad += capacidadPersona.horasTotales();
        }

        log.info("Capacidad total del squad {}: {} horas", squadId, horasTotalesSquad);
        return new CapacidadSquadResponse(
                squadId,
                squad.getNombre(),
                fechaInicio,
                fechaFin,
                horasTotalesSquad,
                capacidadesPersonas
        );
    }

    /**
     * Calcula capacidad de una persona en un rango de fechas.
     */
    private CapacidadPersonaResponse calcularCapacidadPersona(
            SquadMember miembro,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            List<Festivo> todosFestivos,
            List<Vacacion> todasVacaciones,
            List<Ausencia> todasAusencias) {

        Persona persona = miembro.getPersona();
        log.debug("Calculando capacidad para persona {}", persona.getNombre());

        // Filtrar festivos, vacaciones, ausencias de esta persona
        Set<LocalDate> festivosPersona = todosFestivos.stream()
                .filter(f -> f.getPersonas().contains(persona))
                .map(Festivo::getFecha)
                .collect(Collectors.toSet());

        List<Vacacion> vacacionesPersona = todasVacaciones.stream()
                .filter(v -> v.getPersona().getId().equals(persona.getId()))
                .toList();

        List<Ausencia> ausenciasPersona = todasAusencias.stream()
                .filter(a -> a.getPersona().getId().equals(persona.getId()))
                .toList();

        // Calcular capacidad día a día
        List<CapacidadDiaResponse> detallesDias = new ArrayList<>();
        double horasTotalesPersona = 0.0;

        LocalDate fecha = fechaInicio;
        while (!fecha.isAfter(fechaFin)) {
            CapacidadDiaResponse capacidadDia = calcularCapacidadDia(
                    fecha, miembro, festivosPersona, vacacionesPersona, ausenciasPersona);
            detallesDias.add(capacidadDia);
            horasTotalesPersona += capacidadDia.horasDisponibles();
            fecha = fecha.plusDays(1);
        }

        return new CapacidadPersonaResponse(
                persona.getId(),
                persona.getNombre(),
                horasTotalesPersona,
                detallesDias
        );
    }

    /**
     * Calcula capacidad de una persona en un día específico.
     */
    private CapacidadDiaResponse calcularCapacidadDia(
            LocalDate fecha,
            SquadMember miembro,
            Set<LocalDate> festivosPersona,
            List<Vacacion> vacacionesPersona,
            List<Ausencia> ausenciasPersona) {

        // Calcular horas teóricas diarias
        Double horasSemanales = miembro.getPersona().getPerfilHorario().getTotalSemanal().doubleValue();
        Integer dedicacion = miembro.getPorcentaje();
        double horasTeoricasDiarias = (horasSemanales / 5.0) * (dedicacion / 100.0);

        // Verificar si es fin de semana
        DayOfWeek diaSemana = fecha.getDayOfWeek();
        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            return new CapacidadDiaResponse(fecha, 0.0, horasTeoricasDiarias, 0, MotivoReduccion.FIN_SEMANA);
        }

        // Verificar si es festivo
        if (festivosPersona.contains(fecha)) {
            return new CapacidadDiaResponse(fecha, 0.0, horasTeoricasDiarias, 0, MotivoReduccion.FESTIVO);
        }

        // Verificar si hay vacación
        boolean tieneVacacion = vacacionesPersona.stream()
                .anyMatch(v -> !fecha.isBefore(v.getFechaInicio()) && !fecha.isAfter(v.getFechaFin()));
        if (tieneVacacion) {
            return new CapacidadDiaResponse(fecha, 0.0, horasTeoricasDiarias, 0, MotivoReduccion.VACACION);
        }

        // Verificar si hay ausencia
        boolean tieneAusencia = ausenciasPersona.stream()
                .anyMatch(a -> {
                    boolean despuesInicio = !fecha.isBefore(a.getFechaInicio());
                    boolean antesFin = a.getFechaFin() == null || !fecha.isAfter(a.getFechaFin());
                    return despuesInicio && antesFin;
                });
        if (tieneAusencia) {
            return new CapacidadDiaResponse(fecha, 0.0, horasTeoricasDiarias, 0, MotivoReduccion.AUSENCIA);
        }

        // Día laborable normal - capacidad completa
        int porcentaje = 100;
        return new CapacidadDiaResponse(fecha, horasTeoricasDiarias, horasTeoricasDiarias, porcentaje, null);
    }
}
