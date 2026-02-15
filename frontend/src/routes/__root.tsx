import { createRootRoute, Link, Outlet } from "@tanstack/react-router";
import { Briefcase, Calendar, Home, Settings, Users } from "lucide-react";

export const Route = createRootRoute({
  component: RootLayout,
});

function RootLayout() {
  return (
    <div className="flex min-h-screen">
      {/* Sidebar */}
      <aside className="w-64 bg-card border-r border-border flex flex-col">
        <div className="p-6">
          <div className="flex items-center gap-3">
            <img src="/kaos-logo.svg" alt="KAOS" className="h-10 w-10" />
            <div>
              <h1 className="text-2xl font-bold text-primary">KAOS</h1>
              <p className="text-xs text-muted-foreground">
                Gestión de Equipos
              </p>
            </div>
          </div>
        </div>
        <nav className="px-4 space-y-1">
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
          <NavLink to="/configuracion" icon={Settings}>
            Configuración
          </NavLink>
        </nav>

        {/* Footer con logo CONTROL */}
        <div className="mt-auto p-4 border-t border-border">
          <div className="flex items-center gap-2 opacity-60 hover:opacity-100 transition-opacity">
            <img src="/control-logo.svg" alt="CONTROL" className="h-8 w-8" />
            <div>
              <p className="text-xs font-semibold text-muted-foreground">
                Powered by
              </p>
              <p className="text-xs text-muted-foreground">CONTROL</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 p-8 bg-background">
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
      className="flex items-center gap-3 px-3 py-2 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
      activeProps={{
        className: "bg-accent text-foreground font-medium",
      }}
    >
      <Icon className="h-5 w-5" />
      <span>{children}</span>
    </Link>
  );
}
