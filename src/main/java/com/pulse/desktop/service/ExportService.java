package com.pulse.desktop.service;

import com.lowagie.text.BaseColor;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pulse.desktop.config.AppConfig;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ExportService {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public Path exportExcel(String baseName, List<String> headers, List<List<String>> rows) throws IOException {
        Path output = buildOutputPath(baseName, ".xlsx");
        Files.createDirectories(output.getParent());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream stream = Files.newOutputStream(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            var sheet = workbook.createSheet("Export");

            CellStyle headerStyle = workbook.createCellStyle();
            var headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setUnderline(FontUnderline.SINGLE.getByteValue());
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);

            int rowIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(valueAt(headers, col));
                cell.setCellStyle(headerStyle);
            }

            for (List<String> rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int col = 0; col < headers.size(); col++) {
                    Cell cell = row.createCell(col);
                    cell.setCellValue(valueAt(rowData, col));
                }
            }

            for (int col = 0; col < headers.size(); col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(stream);
        }

        return output;
    }

    public Path exportPdf(String title, List<String> headers, List<List<String>> rows) throws IOException {
        return exportPdfStyled(title, headers, rows, false);
    }

    /**
     * Exports data to PDF with optional Steam-inspired styling.
     * @param title The title of the document
     * @param headers Column headers
     * @param rows Data rows
     * @param useSteamStyle If true, applies Steam-inspired styling with dark theme and card layout
     * @return Path to the generated PDF file
     */
    public Path exportPdfStyled(String title, List<String> headers, List<List<String>> rows, boolean useSteamStyle) throws IOException {
        Path output = buildOutputPath(title, ".pdf");
        Files.createDirectories(output.getParent());

        try (OutputStream stream = Files.newOutputStream(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            Document document = new Document(com.lowagie.text.PageSize.A4.rotate());
            try {
                PdfWriter.getInstance(document, stream);
                document.open();

                if (useSteamStyle) {
                    addSteamStyledContent(document, title, headers, rows);
                } else {
                    addBasicContent(document, title, headers, rows);
                }

            } catch (DocumentException ex) {
                throw new IOException("Impossible de generer le PDF.", ex);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        }

        return output;
    }

    private void addBasicContent(Document document, String title, List<String> headers, List<List<String>> rows) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font metaFont = new Font(Font.HELVETICA, 9, Font.ITALIC);
        document.add(new Paragraph(title, titleFont));
        document.add(new Paragraph("Genere le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), metaFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(Math.max(1, headers.size()));
        table.setWidthPercentage(100);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header == null ? "" : header));
            cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
            table.addCell(cell);
        }

        for (List<String> row : rows) {
            for (int i = 0; i < headers.size(); i++) {
                table.addCell(new Phrase(valueAt(row, i)));
            }
        }

        document.add(table);
    }

    private void addSteamStyledContent(Document document, String title, List<String> headers, List<List<String>> rows) throws DocumentException {
        // Steam-inspired colors
        BaseColor headerBg = new BaseColor(11, 20, 31);     // #0b141f
        BaseColor accentColor = new BaseColor(102, 192, 244);  // #66c0f4
        BaseColor textColor = new BaseColor(27, 40, 56);    // #1b2838
        BaseColor altRowBg = new BaseColor(245, 248, 250);  // Light blue tint

        // Title
        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, textColor);
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(8);
        document.add(titlePara);

        // Meta information
        Font metaFont = new Font(Font.HELVETICA, 9, Font.ITALIC, new BaseColor(140, 140, 140));
        Paragraph metaPara = new Paragraph(
                "Généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                metaFont
        );
        metaPara.setAlignment(Element.ALIGN_CENTER);
        metaPara.setSpacingAfter(16);
        document.add(metaPara);

        // Table with Steam styling
        PdfPTable table = new PdfPTable(Math.max(1, headers.size()));
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        // Header cells
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, com.lowagie.text.BaseColor.WHITE);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header == null ? "" : header, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
            cell.setPadding(8);
            cell.setBorderColor(accentColor);
            table.addCell(cell);
        }

        // Data rows with alternating background
        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL, textColor);
        boolean alternate = false;
        for (List<String> row : rows) {
            for (int i = 0; i < headers.size(); i++) {
                PdfPCell cell = new PdfPCell(new Phrase(valueAt(row, i), dataFont));
                cell.setBackgroundColor(alternate ? altRowBg : com.lowagie.text.BaseColor.WHITE);
                cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                cell.setPadding(8);
                table.addCell(cell);
            }
            alternate = !alternate;
        }

        document.add(table);
    }

    public void openFile(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().open(file.toFile());
        } catch (IOException ignored) {
            // Ignore silently when shell cannot open the file.
        }
    }

    private Path buildOutputPath(String baseName, String extension) {
        String safeBase = sanitizeFilename(baseName == null ? "export" : baseName);
        String ts = LocalDateTime.now().format(FILE_TS);
        Path root = AppConfig.webRootPath().resolve("var").resolve("desktop-exports");
        return root.resolve(safeBase + "_" + ts + extension);
    }

    private static String valueAt(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return "";
        }
        String value = values.get(index);
        return value == null ? "" : value;
    }

    private static String sanitizeFilename(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            return "export";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                builder.append(c);
            } else if (c == ' ' || c == '-' || c == '_') {
                builder.append('_');
            }
        }

        String out = builder.toString().replaceAll("_+", "_");
        if (out.isBlank()) {
            return "export";
        }
        return out;
    }
}
