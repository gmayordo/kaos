# KAOS & CONTROL Logos

5 estilos diferentes de logos para KAOS y CONTROL, con selección aleatoria pero garantizando que ambos sean del mismo tipo.

## Tipos Disponibles

1. **Classic** - Estilo tradicional con detalles clásicos
2. **Modern** - Diseño minimalista y contemporáneo
3. **Neon** - Estilo cyberpunk con colores fluorescentes
4. **Geometric** - Formas geométricas abstractas
5. **Vintage** - Diseño retro con degradados
6. **Icon** - Versión pequeña para favicon

## Archivos SVG

```
frontend/public/
├── logo-kaos-classic.svg
├── logo-kaos-modern.svg
├── logo-kaos-neon.svg
├── logo-kaos-geometric.svg
├── logo-kaos-vintage.svg
├── logo-kaos-icon.svg
├── logo-control-classic.svg
├── logo-control-modern.svg
├── logo-control-neon.svg
├── logo-control-geometric.svg
├── logo-control-vintage.svg
└── logo-control-icon.svg
```

## Componente React

### Uso Básico

```tsx
import { LogoDisplay } from "@/components";

// Logos aleatorios del mismo tipo
<LogoDisplay size={100} />;
```

### Props

- **`type?`** - Tipo específico: `'classic'`, `'modern'`, `'neon'`, `'geometric'`, `'vintage'`, `'icon'`
- **`size?`** - Tamaño en píxeles (default: 100)
- **`display?`** - Qué mostrar: `'kaos'`, `'control'`, `'both'` (default: 'both')
- **`gap?`** - Espaciado entre logos (default: '1rem')
- **`className?`** - CSS class adicional

### Ejemplos

```tsx
// Tipo específico
<LogoDisplay type="neon" size={150} />

// Solo un logo
<LogoDisplay display="kaos" size={100} />

// Con gap personalizado
<LogoDisplay size={120} gap="2rem" />

// Con styling
<LogoDisplay size={100} className="shadow-lg rounded" />
```

## Hook de Uso

```tsx
import { getRandomLogoPair, getLogoPair, LOGO_TYPES } from "@/lib/logo-manager";

// Obtener par aleatorio
const logoPair = getRandomLogoPair();
console.log(logoPair.kaos); // '/logo-kaos-*.svg'
console.log(logoPair.control); // '/logo-control-*.svg'
console.log(logoPair.type); // 'classic', 'modern', etc.

// Obtener tipo específico
const modernLogos = getLogoPair("modern");

// Iterar sobre tipos
LOGO_TYPES.forEach((type) => {
  console.log(type);
});
```

## Showcase

Para ver todos los logos disponibles, navega a:

- `/dev/logo-showcase` (si existe la ruta)

O importa el componente:

```tsx
import LogoShowcase from "@/components/LogoShowcase";
```

## Estructura de Diseño

### Elemento KAOS

- Logo: Escudo con águila, alas, globo terrestre
- Paleta: Roja/Negra (clásico), Verde Neón (moderno)

### Elemento CONTROL

- Logo: Escudo con símbolos de control (engranajes, redes, jerarquías)
- Paleta: Gold/Azul (clásico), Verde Neón (moderno)

## Garantías

✅ Ambos logos siempre del mismo tipo visual
✅ Selección verdaderamente aleatoria
✅ Escalables sin pérdida de calidad (SVG)
✅ Optimizados para web
✅ Accesibles (alt text incluido)
