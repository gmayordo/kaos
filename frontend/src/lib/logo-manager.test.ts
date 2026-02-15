import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  LOGO_TYPES,
  getRandomLogoPair,
  getLogoPair,
  getKaosLogo,
  getControlLogo,
  type LogoPair,
  type LogoType,
} from "./logo-manager";

describe("logo-manager", () => {
  describe("LOGO_TYPES export", () => {
    it("debe exportar array de 6 tipos", () => {
      expect(LOGO_TYPES).toHaveLength(6);
    });

    it("debe incluir todos los tipos esperados", () => {
      expect(LOGO_TYPES).toContain("classic");
      expect(LOGO_TYPES).toContain("modern");
      expect(LOGO_TYPES).toContain("neon");
      expect(LOGO_TYPES).toContain("geometric");
      expect(LOGO_TYPES).toContain("vintage");
      expect(LOGO_TYPES).toContain("icon");
    });
  });

  describe("getRandomLogoPair", () => {
    it("debe retornar un LogoPair válido", () => {
      const pair = getRandomLogoPair();

      expect(pair).toBeDefined();
      expect(pair.kaos).toBeDefined();
      expect(pair.control).toBeDefined();
      expect(pair.type).toBeDefined();
    });

    it("debe retornar URLs válidas para kaos y control", () => {
      const pair = getRandomLogoPair();

      expect(pair.kaos).toMatch(/logo-kaos-\w+\.svg$/);
      expect(pair.control).toMatch(/logo-control-\w+\.svg$/);
    });

    it("debe garantizar que kaos y control sean del mismo tipo", () => {
      const pair = getRandomLogoPair();
      const type = pair.type;

      expect(pair.kaos).toContain(`kaos-${type}`);
      expect(pair.control).toContain(`control-${type}`);
    });

    it("debe retornar un tipo válido", () => {
      const pair = getRandomLogoPair();

      expect(LOGO_TYPES).toContain(pair.type);
    });

    it("CAN_01: debe generar al menos 2 tipos diferentes en 100 iteraciones", () => {
      const types = new Set<LogoType>();

      for (let i = 0; i < 100; i++) {
        types.add(getRandomLogoPair().type);
      }

      expect(types.size).toBeGreaterThanOrEqual(2);
    });

    it("NEVER: nunca debe mezclar tipos entre kaos y control", () => {
      for (let i = 0; i < 20; i++) {
        const pair = getRandomLogoPair();
        const type = pair.type;

        const kaosType = pair.kaos.match(/kaos-(\w+)\.svg/)?.[1];
        const controlType = pair.control.match(/control-(\w+)\.svg/)?.[1];

        expect(kaosType).toBe(type);
        expect(controlType).toBe(type);
        expect(kaosType).toBe(controlType);
      }
    });
  });

  describe("getLogoPair(type)", () => {
    it("must return correct pair for each type", () => {
      const typesToTest: LogoType[] = [
        "classic",
        "modern",
        "neon",
        "geometric",
        "vintage",
        "icon",
      ];

      typesToTest.forEach((type) => {
        const pair = getLogoPair(type);

        expect(pair.type).toBe(type);
        expect(pair.kaos).toContain(`kaos-${type}`);
        expect(pair.control).toContain(`control-${type}`);
      });
    });

    it("must return same pair for same type on multiple calls", () => {
      const pair1 = getLogoPair("neon");
      const pair2 = getLogoPair("neon");

      expect(pair1.kaos).toBe(pair2.kaos);
      expect(pair1.control).toBe(pair2.control);
    });

    it("must return different pairs for different types", () => {
      const classic = getLogoPair("classic");
      const modern = getLogoPair("modern");

      expect(classic.kaos).not.toBe(modern.kaos);
      expect(classic.control).not.toBe(modern.control);
    });
  });

  describe("getKaosLogo(type)", () => {
    it("must return correct kaos logo URL for given type", () => {
      expect(getKaosLogo("classic")).toBe("/logo-kaos-classic.svg");
      expect(getKaosLogo("modern")).toBe("/logo-kaos-modern.svg");
      expect(getKaosLogo("neon")).toBe("/logo-kaos-neon.svg");
      expect(getKaosLogo("geometric")).toBe("/logo-kaos-geometric.svg");
      expect(getKaosLogo("vintage")).toBe("/logo-kaos-vintage.svg");
      expect(getKaosLogo("icon")).toBe("/logo-kaos-icon.svg");
    });

    it("must always start with /logo-kaos- and end with .svg", () => {
      const types: LogoType[] = [
        "classic",
        "modern",
        "neon",
        "geometric",
        "vintage",
        "icon",
      ];

      types.forEach((type) => {
        const logo = getKaosLogo(type);

        expect(logo).toMatch(/^\/logo-kaos-\w+\.svg$/);
      });
    });
  });

  describe("getControlLogo(type)", () => {
    it("must return correct control logo URL for given type", () => {
      expect(getControlLogo("classic")).toBe("/logo-control-classic.svg");
      expect(getControlLogo("modern")).toBe("/logo-control-modern.svg");
      expect(getControlLogo("neon")).toBe("/logo-control-neon.svg");
      expect(getControlLogo("geometric")).toBe("/logo-control-geometric.svg");
      expect(getControlLogo("vintage")).toBe("/logo-control-vintage.svg");
      expect(getControlLogo("icon")).toBe("/logo-control-icon.svg");
    });

    it("must always start with /logo-control- and end with .svg", () => {
      const types: LogoType[] = [
        "classic",
        "modern",
        "neon",
        "geometric",
        "vintage",
        "icon",
      ];

      types.forEach((type) => {
        const logo = getControlLogo(type);

        expect(logo).toMatch(/^\/logo-control-\w+\.svg$/);
      });
    });
  });

  describe("Type Safety", () => {
    it("must have matching kaos and control URLs for same type", () => {
      const types: LogoType[] = [
        "classic",
        "modern",
        "neon",
        "geometric",
        "vintage",
        "icon",
      ];

      types.forEach((type) => {
        const kaosLogo = getKaosLogo(type);
        const controlLogo = getControlLogo(type);

        // Extract type from URLs
        const kaosType = kaosLogo.match(/logo-kaos-(\w+)\.svg/)?.[1];
        const controlType = controlLogo.match(/logo-control-(\w+)\.svg/)?.[1];

        expect(kaosType).toBe(type);
        expect(controlType).toBe(type);
        expect(kaosType).toBe(controlType);
      });
    });
  });
});
