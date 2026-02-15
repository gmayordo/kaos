import React from "react";

/**
 * Logo Manager - Gestiona los logos de KAOS y CONTROL con selección aleatoria del tipo
 * Garantiza que ambos logos sean del mismo estilo
 */

export type LogoType =
  | "classic"
  | "modern"
  | "neon"
  | "geometric"
  | "vintage"
  | "icon";

export interface LogoPair {
  kaos: string;
  control: string;
  type: LogoType;
}

const LOGO_TYPES: LogoType[] = [
  "classic",
  "modern",
  "neon",
  "geometric",
  "vintage",
  "icon",
];

export { LOGO_TYPES };

const LOGOS: Record<LogoType, LogoPair> = {
  classic: {
    kaos: "/logo-kaos-classic.svg",
    control: "/logo-control-classic.svg",
    type: "classic",
  },
  modern: {
    kaos: "/logo-kaos-modern.svg",
    control: "/logo-control-modern.svg",
    type: "modern",
  },
  neon: {
    kaos: "/logo-kaos-neon.svg",
    control: "/logo-control-neon.svg",
    type: "neon",
  },
  geometric: {
    kaos: "/logo-kaos-geometric.svg",
    control: "/logo-control-geometric.svg",
    type: "geometric",
  },
  vintage: {
    kaos: "/logo-kaos-vintage.svg",
    control: "/logo-control-vintage.svg",
    type: "vintage",
  },
  icon: {
    kaos: "/logo-kaos-icon.svg",
    control: "/logo-control-icon.svg",
    type: "icon",
  },
};

/**
 * Retorna un par de logos del mismo tipo de forma aleatoria
 */
export function getRandomLogoPair(): LogoPair {
  const randomType = LOGO_TYPES[Math.floor(Math.random() * LOGO_TYPES.length)];
  return LOGOS[randomType];
}

/**
 * Retorna un par de logos de un tipo específico
 */
export function getLogoPair(type: LogoType): LogoPair {
  return LOGOS[type];
}

/**
 * Retorna solo el logo de KAOS para un tipo específico
 */
export function getKaosLogo(type: LogoType): string {
  return LOGOS[type].kaos;
}

/**
 * Retorna solo el logo de CONTROL para un tipo específico
 */
export function getControlLogo(type: LogoType): string {
  return LOGOS[type].control;
}

/**
 * Hook React para usar logos aleatorios
 */
export function useRandomLogo() {
  const [logoPair, setLogoPair] = React.useState<LogoPair | null>(null);

  React.useEffect(() => {
    setLogoPair(getRandomLogoPair());
  }, []);

  return logoPair;
}
