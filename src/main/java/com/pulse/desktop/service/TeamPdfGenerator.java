package com.pulse.desktop.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.pulse.desktop.config.AppConfig;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TeamPdfGenerator {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter LABEL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color PAGE_BG = new Color(245, 249, 255);
    private static final Color HEADER_BG = new Color(27, 40, 56);
    private static final Color HEADER_TEXT = new Color(255, 255, 255);
    private static final Color ACCENT = new Color(102, 192, 244);
    private static final Color CARD_BG = new Color(252, 254, 255);
    private static final Color BORDER = new Color(199, 224, 245);
    private static final Color TEXT_MAIN = new Color(34, 43, 53);
    private static final Color TEXT_MUTED = new Color(91, 108, 125);

    public record MemberReportRow(
            String player,
            String rosterRole,
            String accountRole,
            LocalDateTime joinedAt
    ) {
    }

    public record RequestReportRow(
            String requester,
            String status,
            LocalDateTime createdAt,
            String message
    ) {
    }

    public record InviteReportRow(
            String invited,
            String status,
            LocalDateTime createdAt,
            String message
    ) {
    }

    public Path exportTeamManagementReport(
            String teamName,
            String teamRegion,
            String captainName,
            int memberCount,
            LocalDateTime createdAt,
            List<MemberReportRow> members,
            List<RequestReportRow> pendingRequests,
            List<InviteReportRow> pendingInvites
    ) throws IOException {
        Path output = buildOutputPath("rapport_gestion_equipe_" + safe(teamName, "equipe"), ".pdf");
        Files.createDirectories(output.getParent());

        try (OutputStream stream = Files.newOutputStream(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            Document document = new Document(PageSize.A4.rotate(), 30f, 30f, 30f, 28f);
            try {
                PdfWriter writer = PdfWriter.getInstance(document, stream);
                writer.setPageEvent(new SoftBlueBackgroundEvent());
                document.open();

                addHeader(document);
                addIntroduction(document);
                addTeamInfoCard(document, teamName, teamRegion, captainName, memberCount, createdAt);
                addMembersSection(document, members);
                addRequestsSection(document, pendingRequests);
                addInvitesSection(document, pendingInvites);
                addFooter(document);
            } catch (DocumentException ex) {
                throw new IOException("Impossible de generer le rapport PDF de gestion d'equipe.", ex);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        }

        return output;
    }

    private static void addHeader(Document document) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100f);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorderColor(ACCENT);
        cell.setBorderWidth(1f);
        cell.setPadding(16f);

        Paragraph app = new Paragraph("PULSE", new Font(Font.HELVETICA, 11f, Font.BOLD, ACCENT));
        app.setSpacingAfter(5f);
        Paragraph title = new Paragraph("Rapport de Gestion d'Equipe", new Font(Font.HELVETICA, 20f, Font.BOLD, HEADER_TEXT));
        title.setSpacingAfter(5f);
        Paragraph subtitle = new Paragraph("Synthese operationnelle equipe, membres et demandes", new Font(Font.HELVETICA, 10.5f, Font.NORMAL, new Color(199, 213, 224)));

        cell.addElement(app);
        cell.addElement(title);
        cell.addElement(subtitle);
        banner.addCell(cell);
        document.add(banner);
        document.add(spacer(10f));
    }

    private static void addIntroduction(Document document) throws DocumentException {
        Paragraph intro = new Paragraph(
                "Ce document resume les informations de gestion d'equipe: identite, composition du roster, demandes en attente et invitations actives.",
                new Font(Font.HELVETICA, 10f, Font.NORMAL, TEXT_MUTED)
        );
        intro.setLeading(14f);
        document.add(intro);
        document.add(spacer(10f));
    }

    private static void addTeamInfoCard(
            Document document,
            String teamName,
            String teamRegion,
            String captainName,
            int memberCount,
            LocalDateTime createdAt
    ) throws DocumentException {
        document.add(sectionTitle("Informations de l'Equipe"));

        PdfPTable table = new PdfPTable(new float[]{1.3f, 2.2f, 1.3f, 2.2f});
        table.setWidthPercentage(100f);

        addInfoPair(table, "Nom", safe(teamName, "-"), "Region", safe(teamRegion, "-"));
        addInfoPair(table, "Capitaine", safe(captainName, "-"), "Membres", Integer.toString(Math.max(0, memberCount)));
        addInfoPair(table, "Date creation", fmt(createdAt), "Statut", "Actif");

        document.add(table);
        document.add(spacer(12f));
    }

    private static void addMembersSection(Document document, List<MemberReportRow> rows) throws DocumentException {
        document.add(sectionTitle("Membres Actifs"));
        PdfPTable table = new PdfPTable(new float[]{2.3f, 1.2f, 1.4f, 1.4f});
        table.setWidthPercentage(100f);
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(false);

        addHeaderCell(table, "Joueur");
        addHeaderCell(table, "Role Roster");
        addHeaderCell(table, "Role Compte");
        addHeaderCell(table, "Date d'adhesion");

        if (rows == null || rows.isEmpty()) {
            addEmptyRow(table, "Aucun membre actif.");
        } else {
            int index = 0;
            for (MemberReportRow row : rows) {
                Color bg = alternate(index++);
                addBodyCell(table, safe(row == null ? null : row.player(), "-"), bg, Element.ALIGN_LEFT);
                addBodyCell(table, safe(row == null ? null : row.rosterRole(), "-"), bg, Element.ALIGN_CENTER);
                addBodyCell(table, safe(row == null ? null : row.accountRole(), "-"), bg, Element.ALIGN_CENTER);
                addBodyCell(table, fmt(row == null ? null : row.joinedAt()), bg, Element.ALIGN_CENTER);
            }
        }

        document.add(table);
        document.add(spacer(12f));
    }

    private static void addRequestsSection(Document document, List<RequestReportRow> rows) throws DocumentException {
        document.add(sectionTitle("Demandes en Attente"));
        PdfPTable table = new PdfPTable(new float[]{2.0f, 1.1f, 1.2f, 2.9f});
        table.setWidthPercentage(100f);
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(false);

        addHeaderCell(table, "Demandeur");
        addHeaderCell(table, "Statut");
        addHeaderCell(table, "Date");
        addHeaderCell(table, "Message");

        if (rows == null || rows.isEmpty()) {
            addEmptyRow(table, "Aucune demande en attente.");
        } else {
            int index = 0;
            for (RequestReportRow row : rows) {
                Color bg = alternate(index++);
                addBodyCell(table, safe(row == null ? null : row.requester(), "-"), bg, Element.ALIGN_LEFT);
                addStatusCell(table, row == null ? null : row.status());
                addBodyCell(table, fmt(row == null ? null : row.createdAt()), bg, Element.ALIGN_CENTER);
                addBodyCell(table, safe(row == null ? null : row.message(), "-"), bg, Element.ALIGN_LEFT);
            }
        }

        document.add(table);
        document.add(spacer(12f));
    }

    private static void addInvitesSection(Document document, List<InviteReportRow> rows) throws DocumentException {
        document.add(sectionTitle("Invitations en Attente"));
        PdfPTable table = new PdfPTable(new float[]{2.0f, 1.1f, 1.2f, 2.9f});
        table.setWidthPercentage(100f);
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(false);

        addHeaderCell(table, "Invite");
        addHeaderCell(table, "Statut");
        addHeaderCell(table, "Date");
        addHeaderCell(table, "Message");

        if (rows == null || rows.isEmpty()) {
            addEmptyRow(table, "Aucune invitation en attente.");
        } else {
            int index = 0;
            for (InviteReportRow row : rows) {
                Color bg = alternate(index++);
                addBodyCell(table, safe(row == null ? null : row.invited(), "-"), bg, Element.ALIGN_LEFT);
                addStatusCell(table, row == null ? null : row.status());
                addBodyCell(table, fmt(row == null ? null : row.createdAt()), bg, Element.ALIGN_CENTER);
                addBodyCell(table, safe(row == null ? null : row.message(), "-"), bg, Element.ALIGN_LEFT);
            }
        }

        document.add(table);
    }

    private static void addFooter(Document document) throws DocumentException {
        document.add(spacer(14f));
        Paragraph footer = new Paragraph(
                "Genere par PULSE - " + LocalDateTime.now().format(LABEL_TS),
                new Font(Font.HELVETICA, 9f, Font.ITALIC, TEXT_MUTED)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private static void addInfoPair(PdfPTable table, String leftLabel, String leftValue, String rightLabel, String rightValue) {
        table.addCell(infoLabelCell(leftLabel));
        table.addCell(infoValueCell(leftValue));
        table.addCell(infoLabelCell(rightLabel));
        table.addCell(infoValueCell(rightValue));
    }

    private static PdfPCell infoLabelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text, "-"), new Font(Font.HELVETICA, 9.5f, Font.BOLD, TEXT_MUTED)));
        cell.setPadding(9f);
        cell.setBackgroundColor(CARD_BG);
        cell.setBorderColor(BORDER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell infoValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text, "-"), new Font(Font.HELVETICA, 10.5f, Font.NORMAL, TEXT_MAIN)));
        cell.setPadding(9f);
        cell.setBackgroundColor(Color.WHITE);
        cell.setBorderColor(BORDER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text, "-"), new Font(Font.HELVETICA, 10f, Font.BOLD, HEADER_TEXT)));
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorderColor(BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        table.addCell(cell);
    }

    private static void addBodyCell(PdfPTable table, String text, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text, "-"), new Font(Font.HELVETICA, 9.5f, Font.NORMAL, TEXT_MAIN)));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        table.addCell(cell);
    }

    private static void addStatusCell(PdfPTable table, String statusRaw) {
        String normalized = safe(statusRaw, "PENDING").toUpperCase(Locale.ROOT);
        Color fill;
        Color textColor;
        switch (normalized) {
            case "ACCEPTED" -> {
                fill = new Color(209, 247, 224);
                textColor = new Color(22, 102, 63);
            }
            case "REFUSED", "CANCELLED" -> {
                fill = new Color(255, 224, 224);
                textColor = new Color(140, 31, 31);
            }
            default -> {
                fill = new Color(255, 241, 210);
                textColor = new Color(120, 83, 22);
            }
        }

        PdfPCell cell = new PdfPCell(new Phrase(normalized, new Font(Font.HELVETICA, 9f, Font.BOLD, textColor)));
        cell.setBackgroundColor(fill);
        cell.setBorderColor(BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(7f);
        table.addCell(cell);
    }

    private static void addEmptyRow(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text, "-"), new Font(Font.HELVETICA, 9.5f, Font.ITALIC, TEXT_MUTED)));
        cell.setColspan(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new Color(250, 252, 255));
        cell.setBorderColor(BORDER);
        cell.setPadding(10f);
        table.addCell(cell);
    }

    private static Paragraph sectionTitle(String title) {
        Paragraph paragraph = new Paragraph(safe(title, "-"), new Font(Font.HELVETICA, 12.5f, Font.BOLD, HEADER_BG));
        paragraph.setSpacingAfter(7f);
        return paragraph;
    }

    private static Color alternate(int index) {
        return index % 2 == 0 ? Color.WHITE : new Color(248, 252, 255);
    }

    private static Paragraph spacer(float points) {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(points);
        return paragraph;
    }

    private static String fmt(LocalDateTime value) {
        return value == null ? "-" : value.format(LABEL_TS);
    }

    private static String safe(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private Path buildOutputPath(String baseName, String extension) {
        String safeBase = sanitizeFilename(baseName);
        String ts = LocalDateTime.now().format(FILE_TS);
        Path root = AppConfig.webRootPath().resolve("var").resolve("desktop-exports");
        return root.resolve(safeBase + "_" + ts + extension);
    }

    private static String sanitizeFilename(String raw) {
        String normalized = raw == null ? "" : raw.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            return "rapport_equipe";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                builder.append(c);
            } else if (c == ' ' || c == '_' || c == '-') {
                builder.append('_');
            }
        }

        String out = builder.toString().replaceAll("_+", "_");
        return out.isBlank() ? "rapport_equipe" : out;
    }

    private static final class SoftBlueBackgroundEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();
            Rectangle page = document.getPageSize();
            canvas.saveState();
            canvas.setColorFill(PAGE_BG);
            canvas.rectangle(page.getLeft(), page.getBottom(), page.getWidth(), page.getHeight());
            canvas.fill();
            canvas.restoreState();
        }
    }
}
