package com.pulse.desktop.service;

import com.pulse.desktop.config.AppConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class MailService {
    private static final String APP_NAME = "PULSE";

    private final MailerDsn mailerDsn;
    private final String fromAddress;
    private final TeamEmailTemplateBuilder teamEmailTemplateBuilder;

    public MailService() {
        this.mailerDsn = MailerDsn.parse(AppConfig.mailerDsn());
        this.fromAddress = AppConfig.mailerFromAddress();
        this.teamEmailTemplateBuilder = new TeamEmailTemplateBuilder();
    }

    public boolean isConfigured() {
        return mailerDsn.enabled();
    }

    public void sendEmailVerification(String toEmail, String signedUrl, int lifetimeSeconds) throws Exception {
        String html = """
                <h2>Confirmez votre adresse email</h2>
                <p>Bienvenue sur PULSE.</p>
                <p>Pour activer votre compte, cliquez ici:</p>
                <p><a href="%s">Verifier mon email</a></p>
                <p>Ce lien expire dans %d heure(s).</p>
                """.formatted(escapeHtml(signedUrl), Math.max(1, lifetimeSeconds / 3600));

        sendHtml(toEmail, "Confirmez votre email", html);
    }

    public void sendResetPassword(String toEmail, String resetUrl, String expiresAtLabel) throws Exception {
        String html = """
                <h2>Reinitialisation du mot de passe</h2>
                <p>Vous avez demande la reinitialisation de votre mot de passe.</p>
                <p>Cliquez sur ce lien pour definir un nouveau mot de passe:</p>
                <p><a href="%s">Reinitialiser mon mot de passe</a></p>
                <p>Ce lien expire le %s.</p>
                """.formatted(escapeHtml(resetUrl), escapeHtml(expiresAtLabel));

        sendHtml(toEmail, "Reinitialisation de votre mot de passe", html);
    }

    public void sendTournamentRequestDecision(String toEmail, String requestTitle, String decision) throws Exception {
        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        String message = "ACCEPTED".equals(normalized)
                ? "Votre demande de tournoi a ete acceptee."
                : "Votre demande de tournoi a ete refusee.";
        String html = """
                <h2>Decision demande tournoi</h2>
                <p>%s</p>
                <p>Tournoi: <b>%s</b></p>
                <p>Decision: <b>%s</b></p>
                """.formatted(
                escapeHtml(message),
                escapeHtml(requestTitle == null ? "-" : requestTitle),
                escapeHtml(normalized.isBlank() ? "-" : normalized)
        );
        sendHtml(toEmail, "Decision demande tournoi", html);
    }

    public void sendTeamJoinRequestEmail(
            String toEmail,
            String requestingUsername,
            String requestingName,
            String teamName,
            String captainName,
            String requestMessage,
            String actionUrl
    ) throws Exception {
        String html = teamEmailTemplateBuilder.buildJoinRequestEmail(
                requestingUsername,
                requestingName,
                teamName,
                captainName,
                requestMessage,
                actionUrl
        );
        String text = teamEmailTemplateBuilder.buildJoinRequestEmailText(
                requestingUsername,
                requestingName,
                teamName,
                captainName,
                requestMessage,
                actionUrl
        );
        sendHtml(toEmail, "Nouvelle demande de rejoindre votre equipe", html, text);
    }

    public void sendTeamInvitationEmail(
            String toEmail,
            String invitedUsername,
            String invitedName,
            String teamName,
            String captainName,
            String message,
            String actionUrl
    ) throws Exception {
        String html = teamEmailTemplateBuilder.buildTeamInvitationEmail(
                invitedUsername,
                invitedName,
                teamName,
                captainName,
                message,
                actionUrl
        );
        String text = teamEmailTemplateBuilder.buildTeamInvitationEmailText(
                invitedUsername,
                invitedName,
                teamName,
                captainName,
                message,
                actionUrl
        );
        sendHtml(toEmail, "Invitation a rejoindre " + safeSubjectTeamName(teamName), html, text);
    }

    private void sendHtml(String toEmail, String subject, String html) throws Exception {
        sendHtml(toEmail, subject, html, null);
    }

    private void sendHtml(String toEmail, String subject, String html, String fallbackText) throws Exception {
        if (!mailerDsn.enabled()) {
            throw new IllegalStateException("MAILER_DSN non configure.");
        }

        Session session = Session.getInstance(buildProperties(mailerDsn), buildAuthenticator(mailerDsn));
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress, APP_NAME, StandardCharsets.UTF_8.name()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        message.setSubject(subject, StandardCharsets.UTF_8.name());

        if (fallbackText == null || fallbackText.isBlank()) {
            message.setContent(html, "text/html; charset=UTF-8");
        } else {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(fallbackText, StandardCharsets.UTF_8.name());

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html, "text/html; charset=UTF-8");

            MimeMultipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);
        }

        Transport.send(message);
    }

    private static Properties buildProperties(MailerDsn dsn) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", dsn.host());
        properties.put("mail.smtp.port", Integer.toString(dsn.port()));
        properties.put("mail.smtp.auth", Boolean.toString(!dsn.username().isBlank()));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(dsn.startTls()));
        properties.put("mail.smtp.ssl.enable", Boolean.toString(dsn.ssl()));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        return properties;
    }

    private static Authenticator buildAuthenticator(MailerDsn dsn) {
        if (dsn.username().isBlank()) {
            return null;
        }
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(dsn.username(), dsn.password());
            }
        };
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

    private static String safeSubjectTeamName(String teamName) {
        String value = teamName == null ? "" : teamName.trim();
        return value.isBlank() ? "votre equipe" : value;
    }
}
