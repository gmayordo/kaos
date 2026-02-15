# Handoff - Testing Dashboard & Logos

**Fecha:** 15 de febrero de 2026  
**Agente Origen:** Maxwell Smart (Frontend)  
**Agente Destino:** Agente 13 (Testing)  
**Proyecto:** KAOS

---

## Resumen

Generar tests unitarios e integración para los cambios de dashboard dinámico y sistema de logos con selección aleatoria.

---

## Cambios Implementados (Requeridos para Test)

### 1. Logo Manager System

**Archivo:** `src/lib/logo-manager.ts`

**Funcionalidad:**

- Exporta 6 tipos de logos: classic, modern, neon, geometric, vintage, icon
- Selección **aleatoria garantizada del mismo tipo** para KAOS + CONTROL
- Helpers para obtener logos por tipo específico

**Funciones Exportadas:**

```typescript
getRandomLogoPair(): LogoPair
getLogoPair(type: LogoType): LogoPair
getKaosLogo(type: LogoType): string
getControlLogo(type: LogoType): string
useRandomLogo(): LogoPair | null
LOGO_TYPES: LogoType[]
```

### 2. LogoDisplay Component

**Archivo:** `src/components/LogoDisplay.tsx`

**Props:**

- `type?: LogoType` - Tipo específico o aleatorio
- `size?: number` - Tamaño en píxeles (default 100)
- `display?: 'kaos' | 'control' | 'both'` (default: 'both')
- `className?: string` - CSS adicional
- `gap?: string` - Espaciado entre logos (default: '1rem')

**Funcionalidad:**

- Renderiza logo(s) como `<img>`
- Loading state mientras carga datos
- Responsive a cambios de `type` prop

### 3. Dashboard Index

**Archivo:** `src/routes/index.tsx`

**Cambios:**

- Integración React Query para traer datos de `/api/v1/squads` y `/api/v1/personas`
- Tarjetas dinámicas con contadores actualizados
- Tarjetas son `<Link>` que navegan a `/squads`, `/personas`, `/configuracion`
- Estados: loading, error, success
- Hover effects y animaciones

**Datos Esperados:**

```typescript
squads: { content: SquadResponse[] }
personas: { content: PersonaResponse[] }
```

---

## Criterios de Aceptación

### CA-01: LogoDisplay Unit Tests

**Caso 1.1:** Renderizar con props por defecto

- [ ] Renderiza 2 imágenes (kaos + control)
- [ ] Tienen atributo `alt` correcto
- [ ] Tamaño por defecto = 100px

**Caso 1.2:** Renderizar solo KAOS

- [ ] `display="kaos"` → solo muestra logo KAOS
- [ ] `display="control"` → solo muestra logo CONTROL

**Caso 1.3:** Tipo específico

- [ ] `type="neon"` → ambos logos son neon
- [ ] `type="vintage"` → ambos logos son vintage

**Caso 1.4:** Loading state

- [ ] Mientras carga → muestra div gris con animación
- [ ] Después carga → muestra imágenes

**Caso 1.5:** Customización

- [ ] `size={200}` → width y height = 200px
- [ ] `gap="2rem"` → flex gap = 2rem
- [ ] `className="shadow-lg"` → aplica clase CSS

---

### CA-02: Logo Manager Unit Tests

**Caso 2.1:** getRandomLogoPair()

- [ ] Retorna `LogoPair` válido
- [ ] `logoPair.kaos` y `logoPair.control` existen
- [ ] Ambas URLs pertenecen al mismo tipo
- [ ] `type` coincide con los URLs

**Caso 2.2:** Selección Aleatoria

- [ ] Ejecutada 100 veces → genera mínimo 2 tipos diferentes
- [ ] Nunca KAOS y CONTROL de tipos diferentes

**Caso 2.3:** getLogoPair(type)

- [ ] `getLogoPair('neon')` → retorna neon pair
- [ ] Todo tipo en LOGO_TYPES → funciona correctamente

**Caso 2.4:** Logos Individual

- [ ] `getKaosLogo('classic')` → `/logo-kaos-classic.svg`
- [ ] `getControlLogo('modern')` → `/logo-control-modern.svg`

**Caso 2.5:** LOGO_TYPES export

- [ ] Array contiene 6 elementos
- [ ] Incluye: classic, modern, neon, geometric, vintage, icon

---

### CA-03: Dashboard Integration Tests

**Caso 3.1:** Carga inicial

- [ ] Renderiza "Bienvenido a KAOS"
- [ ] Renderiza 3 tarjetas
- [ ] Muestra "Cargando..." mientras carga datos

**Caso 3.2:** Datos Cargados

- [ ] Tarjeta Squads → muestra números > 0
- [ ] Tarjeta Personas → muestra números
- [ ] Tarjeta Config → siempre muestra 0

**Caso 3.3:** Estados de Carga y Error

- [ ] Si React Query data = undefined → muestra "Cargando..."
- [ ] Si error → muestra texto de error (no crash)

**Caso 3.4:** Navegación

- [ ] Click en Squads Card → navega a `/squads`
- [ ] Click en Personas Card → navega a `/personas`
- [ ] Click en Config Card → navega a `/configuracion`

**Caso 3.5:** Hover Effects

- [ ] Tarjeta en hover → cambia color (bg-accent)
- [ ] Flecha se mueve (translate-x)
- [ ] Sombra aumenta (shadow-lg)

**Caso 3.6:** Query Keys

- [ ] queryKey = `["squads-dashboard"]` para squads
- [ ] queryKey = `["personas-dashboard"]` para personas
- [ ] Se reutilizan en caché correctamente

---

## Archivos de Test a Crear

| Archivo                               | Tipo        | Framework                            |
| ------------------------------------- | ----------- | ------------------------------------ |
| `src/lib/logo-manager.test.ts`        | Unit        | Vitest                               |
| `src/components/LogoDisplay.test.tsx` | Unit        | Vitest + React Testing Library       |
| `src/routes/index.test.tsx`           | Integration | Vitest + React Testing Library + MSW |

---

## Estrategia de Testing

### Unit Tests (Logo Manager)

```typescript
describe("logo-manager", () => {
  describe("getRandomLogoPair", () => {
    // Random logic, type consistency
  });

  describe("getLogoPair", () => {
    // Type-specific retrieval
  });
});
```

### Component Tests (LogoDisplay)

```typescript
describe("LogoDisplay", () => {
  // Rendering with different props
  // Loading state
  // Customization (size, gap, className)
  // Display variants (kaos, control, both)
});
```

### Integration Tests (Dashboard)

```typescript
describe("Dashboard Index", () => {
  // Mock React Query
  // Mock MSW for API calls
  // Test data loading
  // Test navigation links
  // Test error states
});
```

---

## Mocking Strategy

### React Query

```typescript
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
```

### API Calls (MSW)

```typescript
import { setupServer } from "msw/node";
import { http, HttpResponse } from "msw";

const server = setupServer(
  http.get("/api/v1/squads", () =>
    HttpResponse.json({ content: [{ id: 1, nombre: "Test Squad" }] }),
  ),
  http.get("/api/v1/personas", () =>
    HttpResponse.json({ content: [{ id: 1, nombre: "Test Person" }] }),
  ),
);
```

### Router

```typescript
import { RouterProvider } from "@tanstack/react-router";
import { createMemoryHistory } from "@tanstack/react-router";

const memoryHistory = createMemoryHistory({ initialEntries: ["/"] });
```

---

## Orden de Implementación

1. **TASK-T01:** Tests unitarios de `logo-manager.ts`
2. **TASK-T02:** Tests de `LogoDisplay.tsx` (rendering + props)
3. **TASK-T03:** Tests de integración de Dashboard (data loading)
4. **TASK-T04:** Tests de navegación (links clickeables)
5. **TASK-T05:** Cobertura mínima 80% en archivos nuevos

---

## Criterios de Éxito

✅ Todos los tests pasan localmente (`npm test`)  
✅ Cobertura ≥ 80% en archivos nuevos  
✅ Cero warnings en test output  
✅ Tests en CI/CD (si aplica)  
✅ Documentación de cómo ejecutar tests actual

---

## Notas

- No testear getters/setters triviales
- Enfocarse en lógica de negocio
- Tests deben ser determinísticos (no flaky)
- Edge cases: null, undefined, array vacío
- Usar palabras clave Given/When/Then en describe blocks
