import { getRandomLogoPair } from "@/lib/logo-manager";
import { perfilHorarioService } from "@/services/perfilHorarioService";
import { personaService } from "@/services/personaService";
import { planificacionService } from "@/services/planificacionService";
import { sprintService } from "@/services/sprintService";
import { squadService } from "@/services/squadService";
import type { SprintResponse } from "@/types/api";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ChevronRight } from "lucide-react";
import { useEffect, useState } from "react";

export const Route = createFileRoute("/")({
  component: IndexPage,
});

function IndexPage() {
  // Generar logos aleatorios para las tarjetas
  const squadsLogo = getRandomLogoPair().kaos;
  const personasLogo = getRandomLogoPair().kaos;
  const calendarioLogo = getRandomLogoPair().kaos;
  const configuracionLogo = getRandomLogoPair().kaos;
  // Query para squads
  const { data: squadsData, isLoading: isLoadingSquads } = useQuery({
    queryKey: ["squads-dashboard"],
    queryFn: () => squadService.listar(0, 100),
  });

  const squads = Array.isArray(squadsData)
    ? squadsData
    : (squadsData?.content ?? []);

  const [selectedSquadId, setSelectedSquadId] = useState<number | "">("");

  useEffect(() => {
    if (selectedSquadId === "" && squads.length > 0) {
      setSelectedSquadId(squads[0].id);
    }
  }, [selectedSquadId, squads]);

  // Query para personas
  const { data: personasData, isLoading: isLoadingPersonas } = useQuery({
    queryKey: ["personas-dashboard"],
    queryFn: () => personaService.listar(0, 100),
  });

  // Query para perfiles horario
  const { data: perfilesData, isLoading: isLoadingPerfiles } = useQuery({
    queryKey: ["perfiles-horario-dashboard"],
    queryFn: () => perfilHorarioService.listar(0, 100),
  });

  const squadsCount = squads.length;
  const personasCount = personasData?.content?.length ?? 0;
  const perfilesCount = perfilesData?.content?.length ?? 0;

  const { data: sprintActivoData, isLoading: isLoadingSprintActivo } = useQuery(
    {
      queryKey: ["sprint-activo", selectedSquadId],
      queryFn: () =>
        sprintService.listar(0, 1, {
          squadId: Number(selectedSquadId),
          estado: "ACTIVO",
        }),
      enabled: selectedSquadId !== "",
    },
  );

  const sprintActivo = sprintActivoData?.content?.[0] ?? null;

  const { data: sprintsSquadData } = useQuery({
    queryKey: ["sprints-squad", selectedSquadId],
    queryFn: () =>
      sprintService.listar(0, 50, {
        squadId: Number(selectedSquadId),
      }),
    enabled: selectedSquadId !== "",
  });

  const sprintsSquad = sprintsSquadData?.content ?? [];
  const sprintReciente = sprintsSquad
    .slice()
    .sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio))[0];

  const { data: dashboardActivo, isLoading: isLoadingDashboardActivo } =
    useQuery({
      queryKey: ["dashboard-sprint-activo", sprintActivo?.id],
      queryFn: () => planificacionService.obtenerDashboard(sprintActivo!.id),
      enabled: !!sprintActivo,
    });

  const SprintCard = ({
    sprint,
    titulo,
    isLoading,
  }: {
    sprint: SprintResponse | null;
    titulo: string;
    isLoading: boolean;
  }) => {
    const dashboard = sprintActivo?.id === sprint?.id ? dashboardActivo : null;
    const isLoadingDashboard =
      sprintActivo?.id === sprint?.id ? isLoadingDashboardActivo : false;

    return (
      <div className="rounded-lg border border-border bg-card p-5 space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-semibold text-foreground">{titulo}</h3>
          {sprint && (
            <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground">
              {sprint.estado}
            </span>
          )}
        </div>

        {isLoading ? (
          <p className="text-sm text-muted-foreground">Cargando...</p>
        ) : sprint ? (
          <div className="space-y-2 text-sm text-muted-foreground">
            <div>
              <span className="text-foreground font-medium">
                {sprint.nombre}
              </span>
            </div>
            <div>
              {sprint.fechaInicio} → {sprint.fechaFin}
            </div>
            <div className="flex flex-wrap gap-3">
              <span>Capacidad: {sprint.capacidadTotal ?? 0}h</span>
              <span>
                Tareas: {sprint.tareasPendientes ?? 0}/
                {sprint.tareasEnProgreso ?? 0}/{sprint.tareasCompletadas ?? 0}
              </span>
            </div>
            {isLoadingDashboard ? (
              <p className="text-xs">Cargando avances...</p>
            ) : dashboard ? (
              <div className="flex flex-wrap gap-3">
                <span>Avance real: {dashboard.progresoReal}%</span>
                <span>Avance esperado: {dashboard.progresoEsperado}%</span>
                <span>Ocupacion: {dashboard.ocupacionPorcentaje}%</span>
              </div>
            ) : null}
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">
            No hay sprint en este estado.
          </p>
        )}
      </div>
    );
  };

  const DashboardCard = ({
    iconSrc,
    title,
    description,
    count,
    isLoading,
    href,
  }: {
    iconSrc: string;
    title: string;
    description: string;
    count: number;
    isLoading: boolean;
    href: string;
  }) => (
    <Link
      to={href}
      className="block group p-6 border border-border rounded-lg bg-card hover:bg-accent hover:border-accent transition-all hover:shadow-lg hover:scale-105"
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-4">
            <img src={iconSrc} alt={`${title} icon`} className="w-5 h-5" />
            <h3 className="font-semibold text-lg group-hover:text-primary transition-colors">
              {title}
            </h3>
          </div>
          <p className="text-sm text-muted-foreground mb-4">{description}</p>
          <div className="text-3xl font-bold text-primary">
            {isLoading ? (
              <span className="text-sm text-muted-foreground">Cargando...</span>
            ) : (
              count
            )}
          </div>
        </div>
        <ChevronRight className="w-5 h-5 text-muted-foreground group-hover:text-primary group-hover:translate-x-1 transition-all" />
      </div>
    </Link>
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Bienvenido a KAOS</h1>
        <p className="text-muted-foreground mt-2">
          Plataforma de Gestión de Equipos de Desarrollo
        </p>
      </div>

      <div className="rounded-xl border border-border bg-card p-6 space-y-4">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div>
            <h2 className="text-lg font-semibold text-foreground">
              Configuracion del sprint actual
            </h2>
            <p className="text-sm text-muted-foreground">
              Selecciona un squad para ver su sprint activo y planificado.
            </p>
          </div>
          <div className="flex items-center gap-2 text-sm">
            <span className="text-muted-foreground">Squad:</span>
            <select
              value={selectedSquadId}
              onChange={(e) =>
                setSelectedSquadId(e.target.value ? Number(e.target.value) : "")
              }
              className="px-3 py-1.5 rounded-md border border-border bg-background text-foreground"
            >
              {squads.map((squad) => (
                <option key={squad.id} value={squad.id}>
                  {squad.nombre}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <SprintCard
            sprint={sprintActivo}
            titulo="Sprint activo"
            isLoading={isLoadingSprintActivo}
          />
          <SprintCard
            sprint={sprintPlanificado}
            titulo="Sprint planificado"
            isLoading={isLoadingSprintPlan}
          />
        </div>

        {!sprintActivo && !sprintPlanificado && sprintReciente && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <SprintCard
              sprint={sprintReciente}
              titulo="Sprint mas reciente"
              isLoading={false}
            />
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mt-8">
        <DashboardCard
          iconSrc={squadsLogo}
          title="Squads"
          description="Gestiona los squads del proyecto y sus miembros"
          count={squadsCount}
          isLoading={isLoadingSquads}
          href="/squads"
        />

        <DashboardCard
          iconSrc={personasLogo}
          title="Personas"
          description="Administra el equipo, roles y dedicación"
          count={personasCount}
          isLoading={isLoadingPersonas}
          href="/personas"
        />

        <DashboardCard
          iconSrc={calendarioLogo}
          title="Calendario"
          description="Gestiona vacaciones y ausencias del equipo"
          count={0}
          isLoading={false}
          href="/calendario"
        />

        <DashboardCard
          iconSrc={configuracionLogo}
          title="Configuración"
          description="Configura perfiles de horario y preferencias"
          count={perfilesCount}
          isLoading={isLoadingPerfiles}
          href="/configuracion"
        />
      </div>
    </div>
  );
}
