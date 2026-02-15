# KAOS Frontend

Frontend de la plataforma KAOS — Gestión de Equipos de Desarrollo.

## Stack

- **React 19** — Framework UI
- **Vite** — Build tool
- **TypeScript** — Type safety
- **Tailwind CSS** — Styling
- **Shadcn/ui** — Component library base
- **TanStack Router** — Client-side routing
- **TanStack Query** — Server state management
- **Axios** — HTTP client

## Requisitos

- Node.js 18+
- Backend corriendo en `localhost:8080`

## Instalación

```bash
npm install
```

## Desarrollo

```bash
npm run dev
```

La aplicación estará disponible en `http://localhost:5173`.

El proxy está configurado para redirigir `/api` a `http://localhost:8080`.

## Build

```bash
npm run build
```

Los archivos compilados estarán en `dist/`.

## Estructura

```
src/
├── routes/           # Páginas con TanStack Router
│   ├── __root.tsx    # Layout raíz con sidebar
│   └── index.tsx     # Página inicio
├── features/         # Features por dominio
│   ├── horario/
│   ├── squad/
│   ├── persona/
│   └── dedicacion/
├── services/         # Servicios API
│   └── api.ts        # Axios instance
├── types/            # TypeScript types
│   └── api.ts        # API response/request types
├── lib/              # Utilities
│   └── utils.ts      # cn() helper
└── main.tsx          # Entry point
```

## Shadcn/ui Components

Los componentes de Shadcn/ui se añaden manualmente según se necesitan.

Para agregar un componente nuevo:

```bash
npx shadcn@latest add button
npx shadcn@latest add card
npx shadcn@latest add input
# etc.
```

## API

La API se consume desde `src/services/api.ts` con Axios.

Los tipos están definidos en `src/types/api.ts` (generados manualmente desde el backend).

## Convenciones

- **Componentes presentacionales**: Sin lógica de negocio, props + callbacks
- **Loading/Error states**: Siempre implementados en componentes que hacen fetch
- **TanStack Query**: Para todo server state (GET/POST/PUT/DELETE)
- **Route-based code splitting**: Componentes lazy por ruta
