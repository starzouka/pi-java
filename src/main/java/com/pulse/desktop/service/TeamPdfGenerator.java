package com.pulse.desktop.service;

import com.lowagie.text.BaseColor;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.repo.TeamModuleRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Generates professional team management PDFs with Steam-inspired styling.
 * Creates comprehensive team reports with roster, member details, and invitation tracking.
 */
public class TeamPdfGenerator {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // Steam-inspired color palette
    private static final BaseColor BG_LIGHT = new BaseColor(245, 248, 250);
    private static final BaseColor BG_SECTION = new BaseColor(240, 245, 250);
    private static final BaseColor HEADER_BG = new BaseColor(11, 20, 31);       // #0b141f
    private static final BaseColor CARD_BG = new BaseColor(27, 40, 56);         // #1b2838
    private static final BaseColor TEXT_DARK = new BaseColor(27, 40, 56);
    private static final BaseColor TEXT_LIGHT = new BaseColor(199, 213, 224);   // #c7d5e0
    private static final BaseColor ACCENT = new BaseColor(102, 192, 244);       // #66c0f4
    private static final BaseColor ACCENT_DARK = new BaseColor(26, 159, 255);   // #1a9fff
    private static final BaseColor WHITE = BaseColor.WHITE;

    private final Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, TEXT_DARK);
    private final Font subtitleFont = new Font(Font.HELVETICA, 11, Font.BOLD, ACCENT);
    private final Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, WHITE);
    private final Font bodyFont = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_DARK);
    private final Font metaFont = new Font(Font.HELVETICA, 8, Font.ITALIC, new BaseColor(140, 140, 140));

    /**
     * Generates a comprehensive team management PDF with roster, members, and request information.
     */
    public Path exportTeamManagementReport(
            String teamName,
            String teamRegion,
            String captainName,
            int memberCount,
            LocalDateTime createdAt,
            List<TeamMemberRow> members,
            List<TeamRequestRow> pendingRequests,
            List<TeamInviteRow> pendingInvites) throws IOException {
        
        Path output = buildOutputPath("Gestion_Equipe_" + sanitizeName(teamName), ".pdf");
        Files.createDirectories(output.getParent());

        try (OutputStream stream = Files.newOutputStream(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            Document document = new Document(PageSize.A4);
            try {
                PdfWriter.getInstance(document, stream);
                document.open();
                
                // Header
                addHeader(document, teamName);
                
                // Team info section
                addTeamInfoSection(document, teamName, teamRegion, captainName, memberCount, createdAt);
                
                // Members section
                if (members != null && !members.isEmpty()) {
                    addMembersSection(document, members);
                }
                
                // Pending requests section
                if (pendingRequests != null && !pendingRequests.isEmpty()) {
                    addPendingRequestsSection(document, pendingRequests);
                }
                
                // Pending invites section
                if (pendingInvites != null && !pendingInvites.isEmpty()) {
                    addPendingInvitesSection(document, pendingInvites);
                }
                
                // Footer
                addFooter(document);
                
            } catch (DocumentException ex) {
                throw new IOException("Impossible de générer le PDF.", ex);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        }

        return output;
    }

    private void addHeader(Document document, String teamName) throws DocumentException {
        Paragraph title = new Paragraph("Rapport de Gestion d'Équipe", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        document.add(title);
        
        Paragraph subtitle = new Paragraph(sanitizeName(teamName).toUpperCase(Locale.ROOT), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(16);
        document.add(subtitle);
        
        Paragraph meta = new Paragraph("Généré le " + LocalDateTime.now().format(DATE_FORMAT), metaFont);
        meta.setAlignment(Element.ALIGN_CENTER);
        meta.setSpacingAfter(20);
        document.add(meta);
    }

    private void addTeamInfoSection(Document document, String teamName, String teamRegion, 
                                     String captainName, int memberCount, LocalDateTime createdAt) throws DocumentException {
        // Section title
        addSectionTitle(document, "Informations de l'Équipe");
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{30, 70});
        table.setSpacingBefore(10);
        table.setSpacingAfter(16);
        
        addTableCell(table, "Nom de l'équipe", teamName, true);
        addTableCell(table, "Région", teamRegion != null && !teamRegion.isBlank() ? teamRegion : "-", false);
        addTableCell(table, "Capitaine", captainName != null && !captainName.isBlank() ? captainName : "-", true);
        addTableCell(table, "Nombre de membres", String.valueOf(memberCount), false);
        
        if (createdAt != null) {
            addTableCell(table, "Date de création", createdAt.format(DATE_FORMAT), true);
        }
        
        document.add(table);
    }

    private void addMembersSection(Document document, List<TeamMemberRow> members) throws DocumentException {
        addSectionTitle(document, "Membres Actifs");
        
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{30, 20, 25, 25});
        table.setSpacingBefore(10);
        table.setSpacingAfter(16);
        
        // Header row
        addHeaderCell(table, "Joueur");
        addHeaderCell(table, "Rôle Roster");
        addHeaderCell(table, "Rôle Compte");
        addHeaderCell(table, "Date d'adhésion");
        
        boolean alternate = false;
        for (TeamMemberRow member : members) {
            BaseColor bgColor = alternate ? BG_SECTION : WHITE;
            
            PdfPCell cell1 = createCell(member.displayName() + "\n@" + member.username(), bodyFont, bgColor);
            cell1.setMinimumHeight(25);
            table.addCell(cell1);
            
            table.addCell(createCell(member.rosterRole(), bodyFont, bgColor));
            table.addCell(createCell(member.accountRole(), bodyFont, bgColor));
            table.addCell(createCell(
                    member.joinedAt() != null ? member.joinedAt().format(DATE_FORMAT) : "-", 
                    bodyFont, 
                    bgColor
            ));
            
            alternate = !alternate;
        }
        
        document.add(table);
    }

    private void addPendingRequestsSection(Document document, List<TeamRequestRow> requests) throws DocumentException {
        addSectionTitle(document, "Demandes en Attente");
        
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{30, 20, 15, 35});
        table.setSpacingBefore(10);
        table.setSpacingAfter(16);
        
        // Header row
        addHeaderCell(table, "Demandeur");
        addHeaderCell(table, "Statut");
        addHeaderCell(table, "Date");
        addHeaderCell(table, "Message");
        
        boolean alternate = false;
        for (TeamRequestRow request : requests) {
            BaseColor bgColor = alternate ? BG_SECTION : WHITE;
            
            PdfPCell cell1 = createCell(request.displayName() + "\n@" + request.username(), bodyFont, bgColor);
            cell1.setMinimumHeight(25);
            table.addCell(cell1);
            
            PdfPCell statusCell = createCell(request.status(), bodyFont, bgColor);
            statusCell.setBackgroundColor(getStatusColor(request.status()));
            table.addCell(statusCell);
            
            table.addCell(createCell(
                    request.createdAt() != null ? request.createdAt().format(DATE_FORMAT) : "-",
                    bodyFont,
                    bgColor
            ));
            
            table.addCell(createCell(
                    request.message() != null && !request.message().isBlank() ? request.message() : "(Aucun message)",
                    new Font(Font.HELVETICA, 8, Font.ITALIC, new BaseColor(100, 100, 100)),
                    bgColor
            ));
            
            alternate = !alternate;
        }
        
        document.add(table);
    }

    private void addPendingInvitesSection(Document document, List<TeamInviteRow> invites) throws DocumentException {
        addSectionTitle(document, "Invitations en Attente");
        
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{30, 20, 15, 35});
        table.setSpacingBefore(10);
        table.setSpacingAfter(16);
        
        // Header row
        addHeaderCell(table, "Invité");
        addHeaderCell(table, "Statut");
        addHeaderCell(table, "Date");
        addHeaderCell(table, "Message");
        
        boolean alternate = false;
        for (TeamInviteRow invite : invites) {
            BaseColor bgColor = alternate ? BG_SECTION : WHITE;
            
            PdfPCell cell1 = createCell(invite.displayName() + "\n@" + invite.username(), bodyFont, bgColor);
            cell1.setMinimumHeight(25);
            table.addCell(cell1);
            
            PdfPCell statusCell = createCell(invite.status(), bodyFont, bgColor);
            statusCell.setBackgroundColor(getStatusColor(invite.status()));
            table.addCell(statusCell);
            
            table.addCell(createCell(
                    invite.createdAt() != null ? invite.createdAt().format(DATE_FORMAT) : "-",
                    bodyFont,
                    bgColor
            ));
            
            table.addCell(createCell(
                    invite.message() != null && !invite.message().isBlank() ? invite.message() : "(Pas de message)",
                    new Font(Font.HELVETICA, 8, Font.ITALIC, new BaseColor(100, 100, 100)),
                    bgColor
            ));
            
            alternate = !alternate;
        }
        
        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
                "Généré par " + "PULSE" + " • " + LocalDateTime.now().format(DATE_FORMAT),
                metaFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph para = new Paragraph(title, subtitleFont);
        para.setSpacingBefore(12);
        para.setSpacingAfter(6);
        document.add(para);
    }

    private void addTableCell(PdfPTable table, String label, String value, boolean alternate) {
        BaseColor bgColor = alternate ? BG_SECTION : WHITE;
        
        PdfPCell labelCell = createCell(label, bodyFont, bgColor);
        labelCell.setBackgroundColor(new BaseColor(240, 240, 240));
        table.addCell(labelCell);
        
        table.addCell(createCell(value, bodyFont, bgColor));
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = createCell(text, headerFont, CARD_BG);
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(8);
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        cell.setVerticalAlignment(PdfPCell.ALIGN_TOP);
        return cell;
    }

    private BaseColor getStatusColor(String status) {
        if (status == null) return WHITE;
        
        return switch (status.toUpperCase()) {
            case "ACCEPTED", "ACTIF" -> new BaseColor(76, 175, 80);  // Green
            case "PENDING", "EN_ATTENTE" -> new BaseColor(255, 193, 7);  // Amber
            case "REFUSED", "REFUSÉ" -> new BaseColor(244, 67, 54);  // Red
            default -> WHITE;
        };
    }

    private Path buildOutputPath(String baseName, String extension) {
        String safeBase = sanitizeName(baseName);
        String ts = LocalDateTime.now().format(FILE_TS);
        Path root = AppConfig.webRootPath().resolve("var").resolve("desktop-exports");
        return root.resolve(safeBase + "_" + ts + extension);
    }

    private static String sanitizeName(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            return "rapport";
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
        return out.isBlank() ? "rapport" : out;
    }

    // Data model records
    public record TeamMemberRow(
            String username,
            String displayName,
            String rosterRole,
            String accountRole,
            LocalDateTime joinedAt
    ) {}

    public record TeamRequestRow(
            String username,
            String displayName,
            String status,
            String message,
            LocalDateTime createdAt
    ) {}

    public record TeamInviteRow(
            String username,
            String displayName,
            String status,
            String message,
            LocalDateTime createdAt
    ) {}
}
