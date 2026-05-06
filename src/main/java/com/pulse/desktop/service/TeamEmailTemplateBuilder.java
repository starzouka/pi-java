package com.pulse.desktop.service;

public class TeamEmailTemplateBuilder {
    private static final String APP_NAME = "PULSE";
    private static final String OUTER_BG = "#0b141f";
    private static final String CARD_BG = "#1b2838";
    private static final String TEXT_MAIN = "#c7d5e0";
    private static final String TEXT_TITLE = "#ffffff";
    private static final String ACCENT = "#66c0f4";
    private static final String ACCENT_BORDER = "rgba(102, 192, 244, 0.25)";
    private static final String MUTED = "#8f98a0";

    public String buildJoinRequestEmail(
            String requestingUsername,
            String requestingName,
            String teamName,
            String captainName,
            String requestMessage,
            String actionUrl
    ) {
        String subject = "Nouvelle demande pour rejoindre votre equipe";
        String subtitle = "Un joueur souhaite rejoindre votre roster.";
        String actionLabel = "Ouvrir les demandes";

        String details = detailRow("Equipe", strong(teamName))
                + detailRow("Capitaine", safeOrDash(captainName))
                + detailRow("Joueur", safeOrDash(requestingName) + " (@" + safeOrDash(requestingUsername) + ")");

        return buildLayout(
                subject,
                subtitle,
                details,
                "Message du joueur",
                safeMultilineOrFallback(requestMessage, "Aucun message personnalise."),
                actionLabel,
                actionUrl,
                "Vous recevez cet email car vous etes capitaine d'equipe sur PULSE."
        );
    }

    public String buildTeamInvitationEmail(
            String invitedUsername,
            String invitedName,
            String teamName,
            String captainName,
            String message,
            String actionUrl
    ) {
        String subject = "Invitation a rejoindre une equipe";
        String subtitle = "Un capitaine vous propose de rejoindre son equipe.";
        String actionLabel = "Voir mon espace equipe";

        String details = detailRow("Equipe", strong(teamName))
                + detailRow("Capitaine", safeOrDash(captainName))
                + detailRow("Invite", safeOrDash(invitedName) + " (@" + safeOrDash(invitedUsername) + ")");

        return buildLayout(
                subject,
                subtitle,
                details,
                "Message du capitaine",
                safeMultilineOrFallback(message, "Vous etes invite a rejoindre cette equipe."),
                actionLabel,
                actionUrl,
                "Si cette invitation ne vous concerne pas, vous pouvez ignorer cet email."
        );
    }

    public String buildJoinRequestEmailText(
            String requestingUsername,
            String requestingName,
            String teamName,
            String captainName,
            String requestMessage,
            String actionUrl
    ) {
        return """
                PULSE - Nouvelle demande de rejoindre votre equipe

                Equipe: %s
                Capitaine: %s
                Joueur: %s (@%s)

                Message:
                %s

                %s

                Vous recevez cet email car vous etes capitaine d'equipe sur PULSE.
                """
                .formatted(
                        plain(teamName, "-"),
                        plain(captainName, "-"),
                        plain(requestingName, "-"),
                        plain(requestingUsername, "-"),
                        plain(requestMessage, "Aucun message personnalise."),
                        actionText(actionUrl)
                );
    }

    public String buildTeamInvitationEmailText(
            String invitedUsername,
            String invitedName,
            String teamName,
            String captainName,
            String message,
            String actionUrl
    ) {
        return """
                PULSE - Invitation a rejoindre une equipe

                Equipe: %s
                Capitaine: %s
                Invite: %s (@%s)

                Message:
                %s

                %s

                Si cette invitation ne vous concerne pas, ignorez cet email.
                """
                .formatted(
                        plain(teamName, "-"),
                        plain(captainName, "-"),
                        plain(invitedName, "-"),
                        plain(invitedUsername, "-"),
                        plain(message, "Vous etes invite a rejoindre cette equipe."),
                        actionText(actionUrl)
                );
    }

    private String buildLayout(
            String title,
            String subtitle,
            String detailsRowsHtml,
            String messageTitle,
            String messageBodyHtml,
            String actionLabel,
            String actionUrl,
            String securityLine
    ) {
        String safeTitle = escapeHtml(title);
        String safeSubtitle = escapeHtml(subtitle);
        String safeMessageTitle = escapeHtml(messageTitle);
        String safeSecurityLine = escapeHtml(securityLine);

        String actionBlock = "";
        if (actionUrl != null && !actionUrl.isBlank()) {
            actionBlock = """
                    <tr>
                      <td style="padding: 0 28px 18px 28px;">
                        <a href="%s"
                           style="display:inline-block; text-decoration:none; font-weight:700; font-size:14px; color:#0b141f; background:linear-gradient(135deg, #66c0f4 0%%, #1a9fff 100%%); padding:11px 20px; border-radius:8px; border:1px solid rgba(102, 192, 244, 0.45);">
                           %s
                        </a>
                      </td>
                    </tr>
                    """.formatted(escapeHtml(actionUrl), escapeHtml(actionLabel));
        }

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <body style="margin:0; padding:0; background-color:%s;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:%s; padding:20px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="640" cellpadding="0" cellspacing="0" border="0"
                               style="width:640px; max-width:640px; background-color:%s; border:1px solid %s; border-radius:12px; overflow:hidden;">
                          <tr>
                            <td style="padding:18px 28px; background-color:#171a21; border-bottom:1px solid %s;">
                              <div style="font-family:Segoe UI, Arial, sans-serif; font-size:11px; letter-spacing:1.5px; color:%s; font-weight:700;">%s</div>
                              <div style="font-family:Segoe UI, Arial, sans-serif; font-size:22px; line-height:1.2; color:%s; font-weight:700; margin-top:8px;">%s</div>
                              <div style="font-family:Segoe UI, Arial, sans-serif; font-size:14px; line-height:1.5; color:%s; margin-top:8px;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 28px 12px 28px;">
                              <div style="font-family:Segoe UI, Arial, sans-serif; font-size:12px; color:%s; font-weight:700; text-transform:uppercase; letter-spacing:1px;">Details</div>
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"
                                     style="margin-top:8px; border:1px solid %s; border-radius:10px; background-color:rgba(255,255,255,0.02);">
                                %s
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 28px 12px 28px;">
                              <div style="font-family:Segoe UI, Arial, sans-serif; font-size:12px; color:%s; font-weight:700; text-transform:uppercase; letter-spacing:1px;">%s</div>
                              <div style="margin-top:8px; font-family:Segoe UI, Arial, sans-serif; font-size:14px; line-height:1.55; color:%s; border:1px solid %s; border-radius:10px; background-color:rgba(255,255,255,0.02); padding:12px;">
                                %s
                              </div>
                            </td>
                          </tr>
                          %s
                          <tr>
                            <td style="padding:0 28px 16px 28px; font-family:Segoe UI, Arial, sans-serif; font-size:12px; line-height:1.5; color:%s;">
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:12px 28px 18px 28px; border-top:1px solid %s; font-family:Segoe UI, Arial, sans-serif; font-size:11px; color:%s;">
                              Genere automatiquement par %s. Merci de ne pas repondre a cet email.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """
                .formatted(
                        OUTER_BG,
                        OUTER_BG,
                        CARD_BG,
                        ACCENT_BORDER,
                        ACCENT_BORDER,
                        ACCENT,
                        APP_NAME,
                        TEXT_TITLE,
                        safeTitle,
                        TEXT_MAIN,
                        safeSubtitle,
                        ACCENT,
                        ACCENT_BORDER,
                        detailsRowsHtml,
                        ACCENT,
                        safeMessageTitle,
                        TEXT_MAIN,
                        ACCENT_BORDER,
                        messageBodyHtml,
                        actionBlock,
                        TEXT_MAIN,
                        safeSecurityLine,
                        ACCENT_BORDER,
                        MUTED,
                        APP_NAME
                );
    }

    private static String detailRow(String label, String valueHtml) {
        return """
                <tr>
                  <td style="padding:10px 12px; border-bottom:1px solid rgba(102, 192, 244, 0.14); font-family:Segoe UI, Arial, sans-serif; font-size:12px; color:%s; width:34%%;">%s</td>
                  <td style="padding:10px 12px; border-bottom:1px solid rgba(102, 192, 244, 0.14); font-family:Segoe UI, Arial, sans-serif; font-size:13px; color:%s; width:66%%;">%s</td>
                </tr>
                """.formatted(MUTED, escapeHtml(label), TEXT_MAIN, valueHtml);
    }

    private static String strong(String raw) {
        return "<span style=\"color:#ffffff; font-weight:700;\">" + safeOrDash(raw) + "</span>";
    }

    private static String safeOrDash(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        return escapeHtml(normalized.isBlank() ? "-" : normalized);
    }

    private static String safeMultilineOrFallback(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            value = fallback;
        }
        return escapeHtml(value).replace("\n", "<br/>");
    }

    private static String plain(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        return value.isBlank() ? fallback : value;
    }

    private static String actionText(String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank()) {
            return "Aucune action en ligne requise.";
        }
        return "Action: " + actionUrl.trim();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
