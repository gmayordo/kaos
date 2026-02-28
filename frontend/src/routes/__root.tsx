import { createRootRoute, Link, Outlet } from "@tanstack/react-router";
import {
  Briefcase,
  Calendar,
  CalendarDays,
  Clock,
  GitBranch,
  Home,
  Info,
  Layers,
  LayoutDashboard,
  ListChecks,
  Users,
} from "lucide-react";

export const Route = createRootRoute({
  component: RootLayout,
});

function RootLayout() {
  return (
    <div className="flex min-h-screen">
      {/* Skip to content — accesibilidad */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-0 focus:left-0 focus:z-[100] focus:bg-primary focus:text-primary-foreground focus:px-4 focus:py-2 focus:rounded-br-md focus:shadow-lg"
      >
        Saltar al contenido principal
      </a>

      {/* Sidebar */}
      <aside className="w-64 bg-card border-r border-border flex flex-col">
        <div className="p-6">
          <Link
            to="/"
            className="flex items-center gap-3 hover:opacity-80 transition-opacity"
          >
            <img src="/kaos-logo.svg" alt="KAOS" className="h-10 w-10" />
            <div>
              <h1 className="text-2xl font-bold text-primary">KAOS</h1>
              <p className="text-xs text-muted-foreground">
                Gestión de Equipos
              </p>
            </div>
          </Link>
        </div>
        <nav className="px-4 space-y-1" aria-label="Navegación principal">
          <NavLink to="/" icon={Home}>
            Inicio
          </NavLink>
          <NavLink to="/squads" icon={Briefcase}>
            Squads
          </NavLink>
          <NavLink to="/personas" icon={Users}>
            Personas
          </NavLink>
          <NavLink to="/calendario" icon={Calendar}>
            Calendario
          </NavLink>
          <NavLink to="/planificacion" icon={LayoutDashboard}>
            Planificación
          </NavLink>
          <NavLink to="/jira" icon={GitBranch}>
            Jira
          </NavLink>
          <NavLink to="/jira/issues" icon={ListChecks}>
            Issues Jira
          </NavLink>

          {/* Sección Configuración */}
          <div className="pt-2 pb-1 px-3">
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/60">
              Configuración
            </p>
          </div>
          <NavLink to="/configuracion" icon={Clock}>
            Perfiles Horario
          </NavLink>
          <NavLink to="/configuracion/festivos" icon={CalendarDays}>
            Festivos
          </NavLink>
          <NavLink to="/configuracion/plantillas" icon={Layers}>
            Plantillas
          </NavLink>

          <NavLink to="/about" icon={Info}>
            Acerca de
          </NavLink>
        </nav>

        {/* Footer con logo CONTROL */}
        <div className="mt-auto p-4 border-t border-border">
          <Link
            to="/"
            className="flex items-center gap-2 opacity-60 hover:opacity-100 transition-opacity"
          >
            <img src="/control-logo.svg" alt="CONTROL" className="h-8 w-8" />
            <div>
              <p className="text-xs font-semibold text-muted-foreground">
                Powered by
              </p>
              <p className="text-xs text-muted-foreground">CONTROL</p>
            </div>
          </Link>
        </div>
      </aside>

      {/* Main content */}
      <main
        id="main-content"
        className="flex-1 p-8 bg-background"
        tabIndex={-1}
      >
        <Outlet />
      </main>
    </div>
  );
}

interface NavLinkProps {
  to: string;
  icon: React.ComponentType<{ className?: string }>;
  children: React.ReactNode;
}

function NavLink({ to, icon: Icon, children }: NavLinkProps) {
  return (
    <Link
      to={to}
      className="flex items-center gap-3 px-3 py-2 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors border-l-[3px] border-transparent"
      activeProps={{
        className:
          "bg-accent text-foreground font-medium border-l-[3px] border-primary",
      }}
    >
      <Icon className="h-5 w-5" aria-hidden="true" />
      <span>{children}</span>
    </Link>
  );
}
