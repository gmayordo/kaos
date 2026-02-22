import { createFileRoute } from "@tanstack/react-router";
import {
  Bug,
  CheckCircle2,
  Code2,
  Database,
  GitCommit,
  Info,
  Package,
  Rocket,
  Wrench,
} from "lucide-react";

export const Route = createFileRoute("/about")({
  component: AboutPage,
});

// ─── Changelog ───────────────────────────────────────────────────────────────

interface ChangeEntry {
  id: string;
  tipo: "bug" | "mejora" | "feature";
  titulo: string;
  descripcion: string;
}

interface Release {
  version: string;
  fecha: string;
  etiqueta?: string;
  cambios: ChangeEntry[];
}

const CHANGELOG: Release[] = [
  {
    version: "1.2.1",
    fecha: "22/02/2026",
    etiqueta: "Hotfix — Exportacion y Dashboard",
    cambios: [
      {
        id: "FIX-001",
        tipo: "bug",
        titulo: "Home — Sprint planificado eliminado del dashboard",
        descripcion:
          "El dashboard de inicio mostraba tanto el sprint ACTIVO como el PLANIFICACION. Corregido para mostrar unicamente el sprint activo del squad seleccionado.",
      },
      {
        id: "FEATURE-006",
        tipo: "feature",
        titulo: "Timeline — Boton de exportacion a Excel",
        descripcion:
          "Nuevo boton 'Descargar' en la cabecera de la pagina Timeline que exporta la planificacion del sprint en formato XLSX. Incluye nombre de fichero dinamico con nombre del sprint y fecha actual.",
      },
    ],
  },
  {
    version: "1.2.0",
    fecha: "22/02/2026",
    etiqueta: "Liberacion Planificacion",
    cambios: [
      {
        id: "FEATURE-003",
        tipo: "feature",
        titulo: "Planificacion integral de sprints",
        descripcion:
          "Nueva pagina de Planificacion con selector global de sprints, vistas Timeline/Kanban/Dashboard y acciones para crear, activar, inactivar y eliminar sprints. Incluye gestion de tareas y acceso rapido a squads.",
      },
      {
        id: "FEATURE-004",
        tipo: "feature",
        titulo: "Sprints globales por squad",
        descripcion:
          "La creacion de sprint se replica a todos los squads y el cambio de estado aplica al grupo por nombre y fechas.",
      },
      {
        id: "MEJORA-002",
        tipo: "mejora",
        titulo: "Fin de sprint en domingo",
        descripcion:
          "El calculo de fecha fin ahora termina en domingo (inicio lunes + 13 dias) y se recalcula en actualizacion.",
      },
      {
        id: "FEATURE-005",
        tipo: "feature",
        titulo: "Exportacion timeline a Excel (API)",
        descripcion:
          "Nuevo endpoint para exportar la timeline del sprint en formato XLSX.",
      },
    ],
  },
  {
    version: "1.1.0",
    fecha: "21/02/2026",
    etiqueta: "Bloque 2 Completado — Importación Excel",
    cambios: [
      {
        id: "FEATURE-001",
        tipo: "feature",
        titulo: "Importación masiva de vacaciones desde Excel",
        descripcion:
          "Nuevo asistente en Configuración → Importar Vacaciones para cargar calendarios de vacaciones desde ficheros Excel (España FY y Chile CAR). Incluye análisis previo (dry-run), detección automática de personas y mapeo manual para nombres no encontrados. Soporta códigos de ausencia: V (Vacaciones), LD (Libre Disposición), AP (Asuntos Propios), LC (Permiso), B (Baja Médica), O (Otro). Agrupa automáticamente días consecutivos permitiendo gaps de fin de semana.",
      },
      {
        id: "FEATURE-002",
        tipo: "feature",
        titulo: "Tests unitarios — Suite completa de Excel Import",
        descripcion:
          "Agregados 45 casos de prueba: 9 tests unitarios ExcelImportService (parsing, detección personas, agrupación), 6 tests integración VacacionController (endpoints /analizar-excel y /importar-excel), 20 tests UI (wizard 3-pasos), 10 tests servicios frontend (serialización, validación tipos).",
      },
    ],
  },
  {
    version: "0.1.3",
    fecha: "21/02/2026",
    etiqueta: "Navegación Configuración",
    cambios: [
      {
        id: "BUG-008",
        tipo: "bug",
        titulo: "Festivos inaccesible desde el menú lateral",
        descripcion:
          "El sidebar sólo tenía un enlace genérico a Configuración que siempre abría Perfiles Horario. No había forma de llegar a Festivos ni al botón Importar CSV. Se reestructuró el sidebar con dos ítems directos: Perfiles Horario y Festivos.",
      },
    ],
  },
  {
    version: "0.1.2",
    fecha: "21/02/2026",
    etiqueta: "Filtro por persona",
    cambios: [
      {
        id: "MEJORA-001",
        tipo: "mejora",
        titulo: "Calendario — Filtrar vacaciones/ausencias por persona",
        descripcion:
          "Al seleccionar un squad aparece un nuevo selector 'Persona' que filtra las vacaciones y ausencias mostradas. Al cambiar de squad el filtro se resetea automáticamente.",
      },
    ],
  },
  {
    version: "0.1.1",
    fecha: "21/02/2026",
    etiqueta: "Correcciones validación",
    cambios: [
      {
        id: "BUG-006",
        tipo: "bug",
        titulo: "Calendario — Vacaciones/ausencias no se mostraban",
        descripcion:
          "porSquad tipaba la respuesta del backend como PageResponse<T> y accedía a .content, pero el endpoint devuelve List<T> directamente. El array resultaba siempre vacío. Corregido para usar los endpoints dedicados /squads/{id}/vacaciones y /squads/{id}/ausencias.",
      },
      {
        id: "BUG-007",
        tipo: "bug",
        titulo: "Festivos — Botón 'Importar CSV' invisible en tema oscuro",
        descripcion:
          "El botón de carga CSV usaba clases hardcodeadas (text-zinc-700, border-zinc-300, hover:bg-zinc-50) que resultan invisibles en el tema oscuro. Reemplazadas por clases del sistema de diseño (text-foreground, border-border, hover:bg-accent).",
      },
    ],
  },
  {
    version: "0.1.0",
    fecha: "21/02/2026",
    etiqueta: "Bloque 2 — Correcciones y Festivos",
    cambios: [
      {
        id: "BUG-001",
        tipo: "bug",
        titulo: "Squads — IDs Jira no se guardaban",
        descripcion:
          "El formulario de creación/edición de Squads no tenía campos para idSquadCorrJira ni idSquadEvolJira. Ahora se incluyen con validación.",
      },
      {
        id: "BUG-002",
        tipo: "bug",
        titulo: "Personas — Campo ciudad obligatorio ausente",
        descripcion:
          "El backend requería ciudad como campo @NotBlank, pero el formulario y el tipo TypeScript no lo incluían. Resultado: error 400 al guardar.",
      },
      {
        id: "BUG-003",
        tipo: "bug",
        titulo: "Calendario — Días laborables mostraba 'undefined'",
        descripcion:
          "VacacionResponse tenía el campo diasLaborables en backend pero el tipo frontend usaba dias. Ahora sincronizado.",
      },
      {
        id: "BUG-004",
        tipo: "bug",
        titulo: "Calendario — Botón Registrar no abría el selector",
        descripcion:
          "handleRegistrar activaba simultáneamente tipoRegistroSeleccionado y isVacacionFormOpen, haciendo que la condición del selector nunca se cumpliera.",
      },
      {
        id: "BUG-005",
        tipo: "bug",
        titulo: "Configuración Festivos — Página sin implementar",
        descripcion:
          "La ruta /configuracion/festivos mostraba un placeholder. Ahora tiene tabla con filtros por año/tipo, creación, edición, eliminación y carga CSV.",
      },
    ],
  },
  {
    version: "0.0.1",
    fecha: "31/01/2026",
    etiqueta: "Bloque 1 — MVP Inicial",
    cambios: [
      {
        id: "FEAT-001",
        tipo: "feature",
        titulo: "Gestión de Squads y Personas",
        descripcion: "CRUD completo de Squads y Personas con asociaciones.",
      },
      {
        id: "FEAT-002",
        tipo: "feature",
        titulo: "Calendario de vacaciones y ausencias",
        descripcion:
          "Vista de calendario mensual con registro de vacaciones, ausencias y festivos.",
      },
      {
        id: "FEAT-003",
        tipo: "feature",
        titulo: "Configuración de festivos",
        descripcion:
          "Gestión de festivos nacionales y locales con importación CSV.",
      },
    ],
  },
];

const STACK = [
  { label: "Frontend", valor: "React 19 + TypeScript + Vite", icon: Code2 },
  {
    label: "Router / Estado",
    valor: "TanStack Router + TanStack Query",
    icon: GitCommit,
  },
  { label: "Estilos", valor: "Tailwind CSS 3", icon: Package },
  { label: "Backend", valor: "Spring Boot 3 · Java 21", icon: Database },
  { label: "Base de datos", valor: "PostgreSQL + Liquibase", icon: Database },
  {
    label: "Despliegue",
    valor: "Docker + Docker Compose",
    icon: Rocket,
  },
];

// ─── Componente ───────────────────────────────────────────────────────────────

const TIPO_CONFIG = {
  bug: {
    color: "text-red-400",
    bg: "bg-red-500/10 border-red-500/20",
    badge: "bg-red-500/20 text-red-400",
    icon: Bug,
    label: "Bug",
  },
  mejora: {
    color: "text-yellow-400",
    bg: "bg-yellow-500/10 border-yellow-500/20",
    badge: "bg-yellow-500/20 text-yellow-400",
    icon: Wrench,
    label: "Mejora",
  },
  feature: {
    color: "text-emerald-400",
    bg: "bg-emerald-500/10 border-emerald-500/20",
    badge: "bg-emerald-500/20 text-emerald-400",
    icon: CheckCircle2,
    label: "Feature",
  },
};

function AboutPage() {
  const latest = CHANGELOG[0];

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex items-start gap-4">
        <div className="p-3 bg-primary/10 rounded-xl">
          <Info className="h-7 w-7 text-primary" />
        </div>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            Bienvenido a KAOS
          </h1>
          <p className="text-muted-foreground mt-1">
            Plataforma de Gestion de Equipos de Desarrollo con planificacion de
            sprints, tareas y capacidad
          </p>
        </div>
      </div>

      {/* Versión actual destacada */}
      <div className="rounded-xl border border-primary/30 bg-primary/5 p-6 space-y-2">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="flex items-center gap-3">
            <span className="text-2xl font-bold text-primary">
              v{latest.version}
            </span>
            {latest.etiqueta && (
              <span className="text-sm px-2.5 py-0.5 rounded-full bg-primary/20 text-primary font-medium">
                {latest.etiqueta}
              </span>
            )}
          </div>
          <span className="text-sm text-muted-foreground">
            Desplegado el {latest.fecha}
          </span>
        </div>
        <p className="text-sm text-muted-foreground">
          {latest.cambios.length} cambio
          {latest.cambios.length !== 1 ? "s" : ""} incluidos en esta versión
        </p>
      </div>

      {/* Changelog */}
      <div className="space-y-10">
        {CHANGELOG.map((release) => (
          <section key={release.version} className="space-y-4">
            {/* Release header */}
            <div className="flex items-center gap-3">
              <span className="font-bold text-lg">v{release.version}</span>
              {release.etiqueta && (
                <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground">
                  {release.etiqueta}
                </span>
              )}
              <span className="ml-auto text-xs text-muted-foreground">
                {release.fecha}
              </span>
            </div>
            <div className="border-l-2 border-border pl-4 space-y-3">
              {release.cambios.map((cambio) => {
                const cfg = TIPO_CONFIG[cambio.tipo];
                const Icon = cfg.icon;
                return (
                  <div
                    key={cambio.id}
                    className={`rounded-lg border p-4 space-y-1 ${cfg.bg}`}
                  >
                    <div className="flex items-center gap-2 flex-wrap">
                      <Icon className={`h-4 w-4 ${cfg.color}`} />
                      <span className="font-medium text-sm">
                        {cambio.titulo}
                      </span>
                      <span
                        className={`ml-auto text-xs px-2 py-0.5 rounded-full font-mono ${cfg.badge}`}
                      >
                        {cambio.id}
                      </span>
                    </div>
                    <p className="text-xs text-muted-foreground leading-relaxed pl-6">
                      {cambio.descripcion}
                    </p>
                  </div>
                );
              })}
            </div>
          </section>
        ))}
      </div>

      {/* Stack técnico */}
      <section className="space-y-4">
        <h2 className="text-lg font-semibold flex items-center gap-2">
          <Code2 className="h-5 w-5 text-muted-foreground" />
          Stack técnico
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {STACK.map(({ label, valor, icon: Icon }) => (
            <div
              key={label}
              className="flex items-center gap-3 rounded-lg border border-border bg-card p-3"
            >
              <Icon className="h-4 w-4 text-muted-foreground shrink-0" />
              <div>
                <p className="text-xs text-muted-foreground">{label}</p>
                <p className="text-sm font-medium">{valor}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Footer */}
      <div className="text-center text-xs text-muted-foreground pb-4">
        KAOS — Gestión de Equipos &nbsp;·&nbsp; Powered by CONTROL
      </div>
    </div>
  );
}
