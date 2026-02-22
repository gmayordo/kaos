package com.kaos.calendario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import com.kaos.calendario.dto.ExcelAnalysisResponse;
import com.kaos.calendario.dto.ExcelImportResponse;
import com.kaos.calendario.entity.Ausencia;
import com.kaos.calendario.entity.Vacacion;
import com.kaos.calendario.repository.AusenciaRepository;
import com.kaos.calendario.repository.VacacionRepository;
import com.kaos.persona.entity.Persona;
import com.kaos.persona.repository.PersonaRepository;

/**
 * Tests unitarios para {@link ExcelImportService}.
 * Valida parsing de Excel (España FY26 + Chile CAR), detección de personas,
 * agrupación de días y mapeo de códigos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExcelImportService Tests")
class ExcelImportServiceTest {

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private VacacionRepository vacacionRepository;

    @Mock
    private AusenciaRepository ausenciaRepository;

    @InjectMocks
    private ExcelImportService service;

    private Persona personaAlberto;
    private Persona personaMarcela;

    @BeforeEach
    void setUp() {
        personaAlberto = new Persona();
        personaAlberto.setId(1L);
        personaAlberto.setNombre("Alberto Rodriguez González");

        personaMarcela = new Persona();
        personaMarcela.setId(2L);
        personaMarcela.setNombre("Marcela");
    }

    @Nested
    @DisplayName("Análisis de Excel (dry-run)")
    class AnalisisExcelTests {

        @Test
        @DisplayName("CA-T01: Analiza Excel España FY26 — detecta personas resueltas")
        void testAnalizarExcelEspañaFY26() throws IOException {
            // Given: Archivo Excel España con personas exactas
            MultipartFile file = createExcelEspañaFY26();
            when(personaRepository.findByNombreIgnoreCase("Alberto Rodriguez González"))
                    .thenReturn(Optional.of(personaAlberto));
            when(personaRepository.findByNombreIgnoreCase("Persona Desconocida"))
                    .thenReturn(Optional.empty());

            // When: Análisis dry-run
            ExcelAnalysisResponse analysis = service.analizarExcel(file, 2026);

            // Then: Detecta 1 resuelta, 1 sin resolver
            assertThat(analysis.totalFilasPersona()).isEqualTo(2);
            assertThat(analysis.personasResueltas()).hasSize(1);
            assertThat(analysis.personasResueltas().get(0).nombreExcel())
                    .isEqualTo("Alberto Rodriguez González");
            assertThat(analysis.personasNoResueltas()).hasSize(1);
            assertThat(analysis.personasNoResueltas()).contains("Persona Desconocida");
        }

        @Test
        @DisplayName("CA-T02: Detección parcial — nombre similar")
        void testAnalizarExcelConCoincidenciaParcia() throws IOException {
            // Given: Archivo con "Alberto" (sin apellido) — debe encontrar por LIKE
            MultipartFile file = createExcelConNombreIncompleto();
            when(personaRepository.findByNombreIgnoreCase("Alberto"))
                    .thenReturn(Optional.empty());
            when(personaRepository.findByNombreContainingIgnoreCase("Alberto"))
                    .thenReturn(List.of(personaAlberto));

            // When
            ExcelAnalysisResponse analysis = service.analizarExcel(file, 2026);

            // Then: Debe encontrarlo por partial match
            assertThat(analysis.personasResueltas()).hasSize(1);
            assertThat(analysis.personasResueltas().get(0).personaId())
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("CA-T03: Archivo vacío — lanza excepción")
        void testAnalizarExcelVacio() throws IOException {
            // Given: Archivo Excel sin filas de personas
            MultipartFile file = createExcelVacio();

            // When/Then
            assertThatThrownBy(() -> service.analizarExcel(file, 2026))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Importación de Excel con mappeos")
    class ImportacionExcelTests {

        @Test
        @DisplayName("CA-T04: Importa vacaciones — agrupa días consecutivos")
        void testImportarExcelConVacaciones() throws IOException {
            // Given
            MultipartFile file = createExcelConVacaciones();
            var mappings = java.util.Map.of("Alberto Rodriguez González", 1L);

            when(personaRepository.findByNombreIgnoreCase("Alberto Rodriguez González"))
                    .thenReturn(Optional.of(personaAlberto));
            when(vacacionRepository.save(any(Vacacion.class)))
                    .thenAnswer(inv -> {
                        Vacacion v = inv.getArgument(0);
                        v.setId(100L);
                        return v;
                    });

            // When
            ExcelImportResponse result = service.importarExcel(file, 2026, mappings);

            // Then: Debe crear al menos 1 vacación (días agrupados)
            assertThat(result.vacacionesCreadas()).isGreaterThanOrEqualTo(1);
            assertThat(result.personasProcesadas()).isEqualTo(1);
        }

        @Test
        @DisplayName("CA-T05: Mapeo manual — resuelve nombres no identificados")
        void testImportarExcelConMapeoManual() throws IOException {
            // Given: Persona con nombre inusual, se proporciona mapeo manual
            MultipartFile file = createExcelConNombreRaro();
            var mappings = java.util.Map.of("Persona Rara", 2L);

            when(personaRepository.findByNombreIgnoreCase("Persona Rara"))
                    .thenReturn(Optional.empty());
            when(personaRepository.findById(2L))
                    .thenReturn(Optional.of(personaMarcela));
            when(vacacionRepository.save(any(Vacacion.class)))
                    .thenAnswer(inv -> {
                        Vacacion v = inv.getArgument(0);
                        v.setId(101L);
                        return v;
                    });

            // When
            ExcelImportResponse result = service.importarExcel(file, 2026, mappings);

            // Then: Usa el mapeo manual
            assertThat(result.personasProcesadas()).isEqualTo(1);
            assertThat(result.personasNoEncontradas()).isEmpty();
        }

        @Test
        @DisplayName("CA-T06: Sin mapeo para persona no encontrada — la omite")
        void testImportarExcelOmiteSinMapeo() throws IOException {
            // Given: Persona no encontrada, sin mapeo manual
            MultipartFile file = createExcelConPersonaDesconocida();
            Map<String, Long> mappings = java.util.Map.of();

            when(personaRepository.findByNombreIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());

            // When
            ExcelImportResponse result = service.importarExcel(file, 2026, mappings);

            // Then: La añade a personasNoEncontradas
            assertThat(result.personasNoEncontradas()).isNotEmpty();
            assertThat(result.vacacionesCreadas()).isEqualTo(0);
        }

        @Test
        @DisplayName("CA-T07: Códigos de ausencia — mapea V, LD, AP, etc.")
        void testMapeoCodigosAusencia() throws IOException {
            // Given: Excel con diferentes códigos (V, LD, AP)
            MultipartFile file = createExcelConCodigosVariados();
            var mappings = java.util.Map.of("Alberto Rodriguez González", 1L);

            when(personaRepository.findByNombreIgnoreCase("Alberto Rodriguez González"))
                    .thenReturn(Optional.of(personaAlberto));
            when(vacacionRepository.save(any(Vacacion.class)))
                    .thenAnswer(inv -> {
                        Vacacion v = inv.getArgument(0);
                        v.setId(100L);
                        return v;
                    });
            when(ausenciaRepository.save(any(Ausencia.class)))
                    .thenAnswer(inv -> {
                        Ausencia a = inv.getArgument(0);
                        a.setId(200L);
                        return a;
                    });

            // When
            ExcelImportResponse result = service.importarExcel(file, 2026, mappings);

            // Then: Debe crear vacaciones (V) y ausencias (LD, AP, etc.)
            assertThat(result.vacacionesCreadas() + result.ausenciasCreadas())
                    .isGreaterThan(1);
        }
    }

    @Nested
    @DisplayName("Edge cases y errores")
    class EdgeCasesTests {

        @Test
        @DisplayName("CA-T08: Archivo no-Excel — lanza excepción")
        void testArchivoNoExcel() throws IOException {
            // Given: Archivo CSV (no .xlsx)
            MultipartFile file = createNonExcelFile();

            // When/Then
            assertThatThrownBy(() -> service.analizarExcel(file, 2026))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("CA-T09: Días no consecutivos con gap < 3 — agrupa")
        void testAgrupacionConGapWeekendsActual() throws IOException {
            // Given: Días V, V, V, [sab/dom], V, V
            // (gap de fin de semana < 3 días laborables)
            MultipartFile file = createExcelWithWeekendGap();
            var mappings = java.util.Map.of("Alberto Rodriguez González", 1L);

            when(personaRepository.findByNombreIgnoreCase("Alberto Rodriguez González"))
                    .thenReturn(Optional.of(personaAlberto));
            when(vacacionRepository.save(any(Vacacion.class)))
                    .thenAnswer(inv -> {
                        Vacacion v = inv.getArgument(0);
                        v.setId(100L);
                        return v;
                    });

            // When
            ExcelImportResponse result = service.importarExcel(file, 2026, mappings);

            // Then: Debe agrupar como 1 vacación (no 2)
            assertThat(result.vacacionesCreadas()).isEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Métodos auxiliares para crear archivos Excel de prueba
    // ─────────────────────────────────────────────────────────

    private MultipartFile createExcelEspañaFY26() throws IOException {
        Workbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet();

        // Row 10: mes header (ENERO, FEBRERO, ...)
        var row10 = sheet.createRow(10);
        row10.createCell(7).setCellValue("ENERO");
        row10.createCell(12).setCellValue("FEBRERO");

        // Row 11: day numbers
        var row11 = sheet.createRow(11);
        row11.createCell(7).setCellValue(1); // 1-ene
        row11.createCell(8).setCellValue(2); // 2-ene
        row11.createCell(12).setCellValue(1); // 1-feb

        // Row 12: header
        var row12 = sheet.createRow(12);
        row12.createCell(1).setCellValue("Nombre");

        // Row 13: Alberto (con V en ENERO)
        var row13 = sheet.createRow(13);
        row13.createCell(1).setCellValue("Alberto Rodriguez González");
        row13.createCell(7).setCellValue("V"); // 1-ene
        row13.createCell(8).setCellValue("V"); // 2-ene

        // Row 14: Persona Desconocida
        var row14 = sheet.createRow(14);
        row14.createCell(1).setCellValue("Persona Desconocida");
        row14.createCell(7).setCellValue("V"); // 1-ene

        return mockMultipartFile(wb, "españa_fy26.xlsx");
    }

    private MultipartFile createExcelConNombreIncompleto() throws IOException {
        Workbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet();
        
        // Setup básico
        var row10 = sheet.createRow(10);
        row10.createCell(7).setCellValue("ENERO");
        var row11 = sheet.createRow(11);
        row11.createCell(7).setCellValue(1);
        var row12 = sheet.createRow(12);
        row12.createCell(1).setCellValue("Nombre");

        // Persona con nombre incompleto
        var row13 = sheet.createRow(13);
        row13.createCell(1).setCellValue("Alberto");
        row13.createCell(7).setCellValue("V");

        return mockMultipartFile(wb, "incompleto.xlsx");
    }

    private MultipartFile createExcelVacio() throws IOException {
        Workbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet();
        
        var row10 = sheet.createRow(10);
        row10.createCell(7).setCellValue("ENERO");
        var row11 = sheet.createRow(11);
        row11.createCell(7).setCellValue(1);
        // Sin filas de personas
        
        return mockMultipartFile(wb, "vacio.xlsx");
    }

    private MultipartFile createExcelConVacaciones() throws IOException {
        Workbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet();
        
        var row10 = sheet.createRow(10);
        row10.createCell(7).setCellValue("ENERO");
        row10.createCell(11).setCellValue("FEBRERO");
        
        var row11 = sheet.createRow(11);
        row11.createCell(7).setCellValue(6); // lunes
        row11.createCell(8).setCellValue(7); // martes
        row11.createCell(9).setCellValue(8); // miél
        row11.createCell(11).setCellValue(3); // lunes feb
        
        var row12 = sheet.createRow(12);
        row12.createCell(1).setCellValue("Nombre");
        
        var row13 = sheet.createRow(13);
        row13.createCell(1).setCellValue("Alberto Rodriguez González");
        row13.createCell(7).setCellValue("V");
        row13.createCell(8).setCellValue("V");
        row13.createCell(9).setCellValue("V");
        row13.createCell(11).setCellValue("V");
        
        return mockMultipartFile(wb, "conVacaciones.xlsx");
    }

    private MultipartFile createExcelConNombreRaro() throws IOException {
        Workbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet();
        
        var row10 = sheet.createRow(10);
        row10.createCell(7).setCellValue("ENERO");
        var row11 = sheet.createRow(11);
        row11.createCell(7).setCellValue(1);
        var row12 = sheet.createRow(12);
        row12.createCell(1).setCellValue("Nombre");
        
        var row13 = sheet.createRow(13);
        row13.createCell(1).setCellValue("Persona Rara");
        row13.createCell(7).setCellValue("V");
        
        return mockMultipartFile(wb, "nombreRaro.xlsx");
    }

    private MultipartFile createExcelConPersonaDesconocida() throws IOException {
        Workbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet();
        
        var row10 = sheet.createRow(10);
        row10.createCell(7).setCellValue("ENERO");
        var row11 = sheet.createRow(11);
        row11.createCell(7).setCellValue(1);
        var row12 = sheet.createRow(12);
        row12.createCell(1).setCellValue("Nombre");
        
        var row13 = sheet.createRow(13);
        row13.createCell(1).setCellValue("XXXXXX Desconocida");
        row13.createCell(7).setCellValue("V");
        
        return mockMultipartFile(wb, "desconocida.xlsx");
    }

    private MultipartFile createExcelConCodigosVariados() throws IOException {
        Workbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet();
        
        var row10 = sheet.createRow(10);
        row10.createCell(7).setCellValue("ENERO");
        var row11 = sheet.createRow(11);
        row11.createCell(7).setCellValue(1);
        row11.createCell(8).setCellValue(2);
        row11.createCell(9).setCellValue(3);
        var row12 = sheet.createRow(12);
        row12.createCell(1).setCellValue("Nombre");
        
        var row13 = sheet.createRow(13);
        row13.createCell(1).setCellValue("Alberto Rodriguez González");
        row13.createCell(7).setCellValue("V");     // Vacaciones
        row13.createCell(8).setCellValue("LD");    // Libre disposición
        row13.createCell(9).setCellValue("AP");    // Asuntos propios
        
        return mockMultipartFile(wb, "codigosVariados.xlsx");
    }

    private MultipartFile createExcelWithWeekendGap() throws IOException {
        // Simula: V, V (viernes-sábado), [DOM], V, V (lunes-martes)
        // Total gap = 3 días (sab-dom es 2, pero los labs son 1 en medio)
        // Según MAX_GAP_DAYS=3, se agrupa todo
        Workbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet();
        
        var row10 = sheet.createRow(10);
        row10.createCell(7).setCellValue("ENERO");
        var row11 = sheet.createRow(11);
        row11.createCell(7).setCellValue(10);  // viernes 10-ene
        row11.createCell(8).setCellValue(13);  // lunes 13-ene
        var row12 = sheet.createRow(12);
        row12.createCell(1).setCellValue("Nombre");
        
        var row13 = sheet.createRow(13);
        row13.createCell(1).setCellValue("Alberto Rodriguez González");
        row13.createCell(7).setCellValue("V");
        row13.createCell(8).setCellValue("V");
        
        return mockMultipartFile(wb, "weekendGap.xlsx");
    }

    private MultipartFile createNonExcelFile() throws IOException {
        byte[] csvData = "Nombre,Enero1,Enero2\nAlberto,V,V\n".getBytes();
        return mockMultipartFileBytes(csvData, "datos.csv");
    }

    private MultipartFile mockMultipartFile(Workbook wb, String filename) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        return mockMultipartFileBytes(baos.toByteArray(), filename);
    }

    private MultipartFile mockMultipartFileBytes(byte[] content, String filename) {
        return new org.springframework.mock.web.MockMultipartFile(
                "file",
                filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content
        );
    }
}
