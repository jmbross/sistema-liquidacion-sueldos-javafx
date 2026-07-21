package com.jmbross.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmbross.payroll.domain.Receipt;
import com.jmbross.payroll.domain.ReceiptLine;
import com.jmbross.payroll.domain.Worker;
import com.jmbross.payroll.service.ReceiptPdfService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReceiptPdfServiceTest {
    @TempDir
    Path directory;

    @Test
    void createsReadableOnePageReceipt() throws Exception {
        Worker worker = new Worker(
                1L, "Empleado", "Demo", "DEMO-WORKER", "empleado@demo.local", null, new BigDecimal("1000.00"), true);
        Receipt receipt = new Receipt(
                1L,
                1,
                LocalDate.of(2026, 7, 1),
                new BigDecimal("1000.00"),
                new BigDecimal("140.00"),
                new BigDecimal("860.00"),
                List.of(new ReceiptLine("Demo deduction", new BigDecimal("14.0000"), new BigDecimal("140.00"))));
        Path output = new ReceiptPdfService().generate(receipt, worker, directory.resolve("receipt.pdf"));

        assertTrue(java.nio.file.Files.size(output) > 500);
        try (var document = Loader.loadPDF(output.toFile())) {
            assertEquals(1, document.getNumberOfPages());
            assertTrue(new PDFTextStripper().getText(document).contains("Empleado Demo"));
        }
    }
}
