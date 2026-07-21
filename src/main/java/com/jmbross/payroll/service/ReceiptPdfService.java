package com.jmbross.payroll.service;

import com.jmbross.payroll.domain.Receipt;
import com.jmbross.payroll.domain.ReceiptLine;
import com.jmbross.payroll.domain.Worker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public final class ReceiptPdfService {
    private static final Locale DISPLAY_LOCALE = Locale.forLanguageTag("es-AR");

    public Path generate(Receipt receipt, Worker worker, Path output) {
        try {
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    float y = 750;
                    y = line(content, bold, 18, 60, y, "Demo payroll receipt");
                    y = line(content, regular, 11, 60, y - 14, "Worker: " + worker.displayName());
                    y = line(content, regular, 11, 60, y, "Document: " + worker.documentId());
                    y = line(content, regular, 11, 60, y, "Period: " + receipt.period());
                    y = line(content, bold, 12, 60, y - 12, "Gross: " + money(receipt.grossAmount()));
                    for (ReceiptLine item : receipt.lines()) {
                        y = line(
                                content,
                                regular,
                                10,
                                75,
                                y,
                                item.description() + " (" + item.percentage() + "%): -" + money(item.amount()));
                    }
                    y = line(content, bold, 12, 60, y - 8, "Total deductions: " + money(receipt.deductionsAmount()));
                    line(content, bold, 14, 60, y - 4, "Net salary: " + money(receipt.netAmount()));
                    line(
                            content,
                            regular,
                            8,
                            60,
                            50,
                            "Fictitious portfolio data — not valid as a legal or payroll document.");
                }
                document.save(output.toFile());
            }
            validate(output);
            return output;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate receipt PDF", exception);
        }
    }

    private static float line(PDPageContentStream content, PDType1Font font, float size, float x, float y, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text.replace('\n', ' '));
        content.endText();
        return y - size - 7;
    }

    private static String money(java.math.BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(DISPLAY_LOCALE).format(amount);
    }

    private static void validate(Path output) throws IOException {
        try (PDDocument parsed = Loader.loadPDF(output.toFile())) {
            if (parsed.getNumberOfPages() != 1) {
                throw new IOException("Generated receipt has an unexpected page count");
            }
        }
    }
}
