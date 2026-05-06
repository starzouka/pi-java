package com.pulse.desktop.service;

/**
 * Builds HTML email templates for team management with Steam-inspired styling.
 * Uses dark modern design with Steam-like colors and card layouts.
 */
public class TeamEmailTemplateBuilder {
    private static final String APP_NAME = "PULSE";
    
    // Steam-inspired color palette
    private static final String BG_DARK = "#0b141f";
    private static final String BG_CARD = "#1b2838";
    private static final String TEXT_PRIMARY = "#c7d5e0";
    private static final String TEXT_TITLE = "#ffffff";
    private static final String ACCENT_COLOR = "#66c0f4";
    private static final String ACCENT_DARK = "#1a9fff";
    private static final String BORDER_COLOR = "rgba(102, 192, 244, 0.25)";

    /**
     * Builds an HTML email template for a team join request.
     * Sent to the captain when someone requests to join their team.
     * 
     * @param requestingUsername The username of the person requesting to join
     * @param requestingName The display name of the person requesting
     * @param teamName The name of the team
     * @param captainName The captain's display name
     * @param requestMessage Optional personal message from the requester
     * @param actionUrl Optional URL for captain to review the request
     * @return HTML-formatted email body
     */
    public static String buildJoinRequestEmail(
            String requestingUsername,
            String requestingName,
            String teamName,
            String captainName,
            String requestMessage,
            String actionUrl) {
        
        String safeRequestingName = escapeHtml(nvl(requestingName, requestingUsername));
        String safeTeamName = escapeHtml(nvl(teamName, "l'équipe"));
        String safeCaptainName = escapeHtml(nvl(captainName, "Capitaine"));
        String safeMessage = escapeHtml(nvl(requestMessage, ""));
        
        String actionButton = actionUrl != null && !actionUrl.isBlank()
                ? String.format("""
                    <tr>
                        <td style="text-align: center; padding: 24px 0;">
                            <a href="%s" style="
                                display: inline-block;
                                padding: 12px 32px;
                                background: linear-gradient(135deg, %s 0%, %s 100%);
                                color: %s;
                                text-decoration: none;
                                border-radius: 4px;
                                font-weight: bold;
                                font-size: 14px;
                                border: 1px solid %s;
                            ">Voir la demande</a>
                        </td>
                    </tr>
                    """, escapeHtml(actionUrl), ACCENT_COLOR, ACCENT_DARK, TEXT_TITLE, ACCENT_COLOR)
                : "";
        
        return buildEmailTemplate(
                "Nouvelle demande pour " + safeTeamName,
                String.format("""
                    <tr>
                        <td style="padding: 0 24px 20px 24px;">
                            <p style="color: %s; margin: 0 0 20px 0; font-size: 14px;">
                                Bonjour <strong>%s</strong>,
                            </p>
                            <p style="color: %s; margin: 0 0 16px 0; font-size: 14px; line-height: 1.6;">
                                <strong>%s</strong> (@%s) souhaite rejoindre <strong>%s</strong>.
                            </p>
                            %s
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 20px 24px; background-color: %s; border-top: 1px solid %s; border-bottom: 1px solid %s;">
                            <p style="color: %s; margin: 0; font-size: 13px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px;">
                                Message personnel
                            </p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 16px 24px; background-color: rgba(0, 0, 0, 0.2);">
                            <p style="color: %s; margin: 0; font-size: 13px; font-style: italic; line-height: 1.5;">
                                %s
                            </p>
                        </td>
                    </tr>
                    %s
                    """,
                    TEXT_PRIMARY,
                    safeCaptainName,
                    TEXT_PRIMARY,
                    safeRequestingName,
                    escapeHtml(requestingUsername),
                    safeTeamName,
                    safeMessage.isBlank() ? "<p style=\"color: " + TEXT_PRIMARY + "; margin: 0 0 16px 0; font-size: 14px; font-style: italic;\">Aucun message fourni.</p>" : "",
                    BG_CARD,
                    BORDER_COLOR,
                    BORDER_COLOR,
                    ACCENT_COLOR,
                    TEXT_PRIMARY,
                    safeMessage.isBlank() ? "<i>Aucun message fourni.</i>" : safeMessage,
                    actionButton
        ));
    }

    /**
     * Builds an HTML email template for a team invitation.
     * Sent to a player when the captain invites them to join the team.
     * 
     * @param invitedUsername The username of the invited player
     * @param invitedName The display name of the invited player
     * @param teamName The name of the team
     * @param captainName The captain's display name
     * @param invitationMessage The message from the captain
     * @param actionUrl Optional URL for the player to accept/review the invitation
     * @return HTML-formatted email body
     */
    public static String buildTeamInvitationEmail(
            String invitedUsername,
            String invitedName,
            String teamName,
            String captainName,
            String invitationMessage,
            String actionUrl) {
        
        String safeInvitedName = escapeHtml(nvl(invitedName, invitedUsername));
        String safeTeamName = escapeHtml(nvl(teamName, "l'équipe"));
        String safeCaptainName = escapeHtml(nvl(captainName, "Capitaine"));
        String safeMessage = escapeHtml(nvl(invitationMessage, "Vous avez reçu une invitation pour rejoindre notre équipe sur PULSE."));
        
        String actionButton = actionUrl != null && !actionUrl.isBlank()
                ? String.format("""
                    <tr>
                        <td style="text-align: center; padding: 24px 0;">
                            <a href="%s" style="
                                display: inline-block;
                                padding: 12px 32px;
                                background: linear-gradient(135deg, %s 0%, %s 100%);
                                color: %s;
                                text-decoration: none;
                                border-radius: 4px;
                                font-weight: bold;
                                font-size: 14px;
                                border: 1px solid %s;
                            ">Voir l'invitation</a>
                        </td>
                    </tr>
                    """, escapeHtml(actionUrl), ACCENT_COLOR, ACCENT_DARK, TEXT_TITLE, ACCENT_COLOR)
                : "";
        
        return buildEmailTemplate(
                "Invitation à rejoindre " + safeTeamName,
                String.format("""
                    <tr>
                        <td style="padding: 0 24px 20px 24px;">
                            <p style="color: %s; margin: 0 0 20px 0; font-size: 14px;">
                                Bonjour <strong>%s</strong>,
                            </p>
                            <p style="color: %s; margin: 0 0 16px 0; font-size: 14px; line-height: 1.6;">
                                <strong>%s</strong> t'invite à rejoindre <strong>%s</strong> sur PULSE.
                            </p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 20px 24px; background-color: %s; border-top: 1px solid %s; border-bottom: 1px solid %s;">
                            <p style="color: %s; margin: 0; font-size: 13px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px;">
                                Message du capitaine
                            </p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 16px 24px; background-color: rgba(0, 0, 0, 0.2);">
                            <p style="color: %s; margin: 0; font-size: 13px; line-height: 1.5;">
                                %s
                            </p>
                        </td>
                    </tr>
                    %s
                    <tr>
                        <td style="padding: 20px 24px; background-color: rgba(0, 0, 0, 0.3); border-top: 1px solid %s;">
                            <p style="color: %s; margin: 0; font-size: 12px; line-height: 1.5;">
                                Si tu n'as pas demandé cette invitation ou si tu n'es pas intéressé, tu peux ignorer cet email.
                            </p>
                        </td>
                    </tr>
                    """,
                    TEXT_PRIMARY,
                    safeInvitedName,
                    TEXT_PRIMARY,
                    safeCaptainName,
                    safeTeamName,
                    BG_CARD,
                    BORDER_COLOR,
                    BORDER_COLOR,
                    ACCENT_COLOR,
                    TEXT_PRIMARY,
                    safeMessage,
                    actionButton,
                    BORDER_COLOR,
                    TEXT_PRIMARY
        ));
    }

    /**
     * Builds the complete email HTML structure with header, body, and footer.
     * 
     * @param subject Email subject line
     * @param bodyContent HTML content for the email body
     * @return Complete HTML document string
     */
    private static String buildEmailTemplate(String subject, String bodyContent) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                            background-color: %s;
                            color: %s;
                            margin: 0;
                            padding: 0;
                        }
                        a {
                            color: %s;
                            text-decoration: none;
                        }
                        a:hover {
                            text-decoration: underline;
                        }
                        .email-container {
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .email-card {
                            background-color: %s;
                            border: 1px solid %s;
                            border-radius: 4px;
                            overflow: hidden;
                        }
                        .email-header {
                            background-color: %s;
                            padding: 20px 24px;
                            border-bottom: 2px solid %s;
                        }
                        .email-header h1 {
                            margin: 0;
                            color: %s;
                            font-size: 16px;
                            font-weight: bold;
                            letter-spacing: 1px;
                            text-transform: uppercase;
                        }
                        .email-header p {
                            margin: 8px 0 0 0;
                            color: %s;
                            font-size: 12px;
                        }
                        .email-footer {
                            background-color: rgba(0, 0, 0, 0.2);
                            padding: 20px 24px;
                            border-top: 1px solid %s;
                            text-align: center;
                        }
                        .email-footer p {
                            margin: 0;
                            color: %s;
                            font-size: 11px;
                            line-height: 1.6;
                        }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: %s; border-collapse: collapse;">
                            <tr>
                                <td>
                                    <div class="email-card">
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse: collapse;">
                                            <tr>
                                                <td class="email-header">
                                                    <h1>%s</h1>
                                                    <p>Gestion d'équipe • %s</p>
                                                </td>
                                            </tr>
                                        </table>
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse: collapse;">
                                            %s
                                        </table>
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse: collapse;">
                                            <tr>
                                                <td class="email-footer">
                                                    <p>
                                                        Cet email a été envoyé par %s.<br>
                                                        Si tu reçois des emails non sollicités, tu peux les ignorer en toute sécurité.
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                    </div>
                                </td>
                            </tr>
                        </table>
                    </div>
                </body>
                </html>
                """,
                escapeHtml(subject),
                BG_DARK,
                TEXT_PRIMARY,
                ACCENT_COLOR,
                BG_CARD,
                BORDER_COLOR,
                BG_DARK,
                ACCENT_COLOR,
                TEXT_TITLE,
                TEXT_PRIMARY,
                BORDER_COLOR,
                TEXT_PRIMARY,
                BG_DARK,
                escapeHtml(subject),
                APP_NAME,
                bodyContent,
                APP_NAME
        );
    }

    /**
     * Escapes HTML special characters to prevent injection attacks.
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Returns the value if non-null and non-blank, otherwise returns the default.
     */
    private static String nvl(String value, String defaultValue) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return defaultValue;
    }
}
