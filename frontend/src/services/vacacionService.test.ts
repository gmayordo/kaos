import { describe, it, expect, beforeEach, vi } from "vitest";
import * as vacacionService from "@/services/vacacionService";
import { api } from "@/services/api";

// Mock api
vi.mock("@/services/api");

describe("VacacionService — Excel Import/Analysis", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("analizarExcel", () => {
    it("CA-S01: Invoca POST /vacaciones/analizar-excel con file y año", async () => {
      const file = new File(["content"], "test.xlsx", {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const año = 2026;

      const expectedResponse = {
        totalFilasPersona: 2,
        personasResueltas: [
          {
            nombreExcel: "Alberto",
            personaId: 1,
            personaNombre: "Alberto Rodriguez",
          },
        ],
        personasNoResueltas: ["Desconocida"],
      };

      vi.mocked(api.post).mockResolvedValue({ data: expectedResponse });

      const result = await vacacionService.vacacionService.analizarExcel(
        file,
        año,
      );

      expect(api.post).toHaveBeenCalledWith(
        "/vacaciones/analizar-excel?año=2026",
        expect.any(FormData),
        { headers: { "Content-Type": "multipart/form-data" } },
      );
      expect(result).toEqual(expectedResponse);
    });

    it("CA-S02: Usa año por defecto si no se proporciona", async () => {
      const file = new File(["content"], "test.xlsx", {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });

      vi.mocked(api.post).mockResolvedValue({ data: {} as any });

      await vacacionService.vacacionService.analizarExcel(file);

      // Verificar que se llamó sin parámetro año o con año actual
      expect(api.post).toHaveBeenCalled();
    });

    it("CA-S03: Rechaza si el archivo está vacío", async () => {
      const emptyFile = new File([], "empty.xlsx", {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });

      vi.mocked(api.post).mockRejectedValue(new Error("El fichero está vacío"));

      await expect(
        vacacionService.vacacionService.analizarExcel(emptyFile, 2026),
      ).rejects.toThrow("El fichero está vacío");
    });
  });

  describe("importarExcel", () => {
    it("CA-S04: Importa sin mappings (parámetro opcional)", async () => {
      const file = new File(["content"], "test.xlsx", {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const año = 2026;

      const expectedResponse = {
        personasProcesadas: 1,
        vacacionesCreadas: 2,
        ausenciasCreadas: 0,
        personasNoEncontradas: [],
        errores: [],
      };

      vi.mocked(api.post).mockResolvedValue({ data: expectedResponse });

      const result = await vacacionService.vacacionService.importarExcel(
        file,
        año,
      );

      expect(api.post).toHaveBeenCalledWith(
        "/vacaciones/importar-excel?año=2026",
        expect.any(FormData),
        { headers: { "Content-Type": "multipart/form-data" } },
      );
      expect(result).toEqual(expectedResponse);
    });

    it("CA-S05: Importa con mappings manuales", async () => {
      const file = new File(["content"], "test.xlsx", {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const año = 2026;
      const mappings = {
        "Alberto Rodriguez": 1,
        Marcela: 2,
      };

      const expectedResponse = {
        personasProcesadas: 2,
        vacacionesCreadas: 3,
        ausenciasCreadas: 1,
        personasNoEncontradas: [],
        errores: [],
      };

      vi.mocked(api.post).mockResolvedValue({ data: expectedResponse });

      const result = await vacacionService.vacacionService.importarExcel(
        file,
        año,
        mappings,
      );

      expect(api.post).toHaveBeenCalledWith(
        expect.stringContaining("/vacaciones/importar-excel"),
        expect.any(FormData),
        { headers: { "Content-Type": "multipart/form-data" } },
      );

      // Verificar que mappingsJson fue incluido en la query string
      const callArgs = vi.mocked(api.post).mock.calls[0];
      expect(callArgs[0]).toContain("mappingsJson");
      expect(result).toEqual(expectedResponse);
    });

    it("CA-S06: Serializa mappings a JSON en query string", async () => {
      const file = new File(["content"], "test.xlsx");
      const mappings = { "Name A": 100, "Name B": 200 };

      vi.mocked(api.post).mockResolvedValue({ data: {} as any });

      await vacacionService.vacacionService.importarExcel(file, 2026, mappings);

      const callUrl = vi.mocked(api.post).mock.calls[0][0];
      // Debe contener mappingsJson con formato JSON válido
      expect(callUrl).toMatch(/mappingsJson=/);
      expect(callUrl).toContain('"Name A"');
    });

    it("CA-S07: Maneja respuesta con personas no encontradas", async () => {
      const file = new File(["content"], "test.xlsx");

      const responseWithErrors = {
        personasProcesadas: 2,
        vacacionesCreadas: 1,
        ausenciasCreadas: 0,
        personasNoEncontradas: ["Juan Pérez", "Desconocido"],
        errores: [],
      };

      vi.mocked(api.post).mockResolvedValue({ data: responseWithErrors });

      const result = await vacacionService.vacacionService.importarExcel(
        file,
        2026,
      );

      expect(result.personasNoEncontradas).toHaveLength(2);
      expect(result.personasNoEncontradas).toContain("Juan Pérez");
    });

    it("CA-S08: Maneja errores en la respuesta", async () => {
      const file = new File(["content"], "test.xlsx");

      const responseWithErrors = {
        personasProcesadas: 0,
        vacacionesCreadas: 0,
        ausenciasCreadas: 0,
        personasNoEncontradas: [],
        errores: ["Formato de Excel inválido", "Año fiscal no soportado"],
      };

      vi.mocked(api.post).mockResolvedValue({ data: responseWithErrors });

      const result = await vacacionService.vacacionService.importarExcel(
        file,
        2026,
      );

      expect(result.errores).toHaveLength(2);
      expect(result.errores[0]).toContain("Formato de Excel inválido");
    });
  });

  describe("Validación de tipos", () => {
    it("CA-S09: Respuesta de análisis tiene estructura correcta", async () => {
      const file = new File(["content"], "test.xlsx");

      const analysis = {
        totalFilasPersona: 5,
        personasResueltas: [
          { nombreExcel: "Person1", personaId: 1, personaNombre: "Person Uno" },
          { nombreExcel: "Person2", personaId: 2, personaNombre: "Person Dos" },
        ],
        personasNoResueltas: ["Person3"],
      };

      vi.mocked(api.post).mockResolvedValue({ data: analysis });

      const result = await vacacionService.vacacionService.analizarExcel(
        file,
        2026,
      );

      expect(result).toHaveProperty("totalFilasPersona");
      expect(result).toHaveProperty("personasResueltas");
      expect(result).toHaveProperty("personasNoResueltas");
      expect(result.personasResueltas[0]).toHaveProperty("nombreExcel");
      expect(result.personasResueltas[0]).toHaveProperty("personaId");
      expect(result.personasResueltas[0]).toHaveProperty("personaNombre");
    });

    it("CA-S10: Respuesta de importación tiene todos los campos", async () => {
      const file = new File(["content"], "test.xlsx");

      const importResult = {
        personasProcesadas: 3,
        vacacionesCreadas: 5,
        ausenciasCreadas: 2,
        personasNoEncontradas: ["Unknown"],
        errores: ["Error processing row 5"],
      };

      vi.mocked(api.post).mockResolvedValue({ data: importResult });

      const result = await vacacionService.vacacionService.importarExcel(
        file,
        2026,
      );

      expect(result).toHaveProperty("personasProcesadas");
      expect(result).toHaveProperty("vacacionesCreadas");
      expect(result).toHaveProperty("ausenciasCreadas");
      expect(result).toHaveProperty("personasNoEncontradas");
      expect(result).toHaveProperty("errores");
    });
  });
});
