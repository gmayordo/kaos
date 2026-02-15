import { getRandomLogoPair } from "@/lib/logo-manager";
import { perfilHorarioService } from "@/services/perfilHorarioService";
import { personaService } from "@/services/personaService";
import { squadService } from "@/services/squadService";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ChevronRight } from "lucide-react";

export const Route = createFileRoute("/")({
  component: IndexPage,
});

function IndexPage() {
  // Generar logos aleatorios para las tarjetas
  const squadsLogo = getRandomLogoPair().kaos;
  const personasLogo = getRandomLogoPair().kaos;
  const configuracionLogo = getRandomLogoPair().kaos;
  // Query para squads
  const { data: squadsData, isLoading: isLoadingSquads } = useQuery({
    queryKey: ["squads-dashboard"],
    queryFn: () => squadService.listar(0, 100),
  });

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

  const squadsCount = Array.isArray(squadsData)
    ? squadsData.length
    : (squadsData?.content?.length ?? 0);
  const personasCount = personasData?.content?.length ?? 0;
  const perfilesCount = perfilesData?.content?.length ?? 0;

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

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-8">
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
