import {
  getLogoPair,
  getRandomLogoPair,
  type LogoPair,
  type LogoType,
} from "@/lib/logo-manager";
import React, { useEffect, useState } from "react";

export interface LogoDisplayProps {
  /**
   * Tipo específico de logo a mostrar. Si no se proporciona, se selecciona aleatoriamente
   */
  type?: LogoType;
  /**
   * Tamaño del logo en píxeles
   * @default 100
   */
  size?: number;
  /**
   * Mostrar solo KAOS o solo CONTROL, o ambos
   * @default 'both'
   */
  display?: "kaos" | "control" | "both";
  /**
   * CSS class adicional
   */
  className?: string;
  /**
   * Espaciado entre logos cuando display='both'
   * @default '1rem'
   */
  gap?: string;
}

/**
 * Componente que muestra los logos de KAOS y CONTROL del mismo tipo
 * Puede mostrar ambos aleatoriamente o un tipo específico
 */
export const LogoDisplay: React.FC<LogoDisplayProps> = ({
  type,
  size = 100,
  display = "both",
  className = "",
  gap = "1rem",
}) => {
  const [logoPair, setLogoPair] = useState<LogoPair | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Simular pequeño delay para evitar hydration mismatch
    const timer = setTimeout(() => {
      if (type) {
        setLogoPair(getLogoPair(type));
      } else {
        setLogoPair(getRandomLogoPair());
      }
      setIsLoading(false);
    }, 0);

    return () => clearTimeout(timer);
  }, [type]);

  if (isLoading || !logoPair) {
    return (
      <div
        style={{ width: size, height: size }}
        className={`bg-gray-200 rounded animate-pulse ${className}`}
      />
    );
  }

  const logoStyle = {
    width: size,
    height: size,
    objectFit: "contain",
  } as React.CSSProperties;

  const containerStyle =
    display === "both"
      ? ({
          display: "flex",
          gap,
          alignItems: "center",
          justifyContent: "center",
        } as React.CSSProperties)
      : undefined;

  return (
    <div style={containerStyle} className={className}>
      {(display === "kaos" || display === "both") && (
        <img
          src={logoPair.kaos}
          alt="KAOS Logo"
          style={logoStyle}
          title={`KAOS - ${logoPair.type}`}
        />
      )}

      {(display === "control" || display === "both") && (
        <img
          src={logoPair.control}
          alt="CONTROL Logo"
          style={logoStyle}
          title={`CONTROL - ${logoPair.type}`}
        />
      )}
    </div>
  );
};

export default LogoDisplay;
