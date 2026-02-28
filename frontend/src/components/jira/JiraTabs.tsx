/**
 * JiraTabs — Sub-navegación accesible para las secciones Jira.
 * Componente presentacional extraído de mi-dia.tsx y mi-semana.tsx.
 */

import { Link } from "@tanstack/react-router";

export type JiraTabKey = "config" | "mi-dia" | "mi-semana" | "issues";

interface JiraTabsProps {
  /** Pestaña actualmente activa */
  active: JiraTabKey;
}

const tabs: { to: string; label: string; key: JiraTabKey }[] = [
  { to: "/jira", label: "Configuración", key: "config" },
  { to: "/jira/issues", label: "Issues", key: "issues" },
  { to: "/jira/mi-dia", label: "Mi Día", key: "mi-dia" },
  { to: "/jira/mi-semana", label: "Mi Semana", key: "mi-semana" },
];

export function JiraTabs({ active }: JiraTabsProps) {
  return (
    <nav aria-label="Secciones Jira">
      <div
        className="flex gap-1 border-b border-border pb-0 mb-6"
        role="tablist"
      >
        {tabs.map((tab) => (
          <Link
            key={tab.key}
            to={tab.to}
            role="tab"
            aria-selected={active === tab.key}
            aria-current={active === tab.key ? "page" : undefined}
            className={`px-4 py-2 text-sm rounded-t-md transition-colors focus:outline-none focus:ring-2 focus:ring-primary ${
              active === tab.key
                ? "bg-primary text-primary-foreground font-medium"
                : "text-muted-foreground hover:text-foreground hover:bg-accent"
            }`}
          >
            {tab.label}
          </Link>
        ))}
      </div>
    </nav>
  );
}
