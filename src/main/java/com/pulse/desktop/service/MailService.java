package com.pulse.desktop.service;

import com.pulse.desktop.config.AppConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class MailService {
    private static final String APP_NAME = "PULSE";

    private final MailerDsn mailerDsn;
    private final String fromAddress;

    public MailService() {
        this.mailerDsn = MailerDsn.parse(AppConfig.mailerDsn());
        this.fromAddress = AppConfig.mailerFromAddress();
    }

    public boolean isConfigured() {
        return mailerDsn.enabled();
    }

    public boolean isValidRecipientEmail(String email) {
        try {
            parseRecipients(email);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
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

    public void sendTournamentRequestReceived(String toEmail, String organizerName, String requestTitle, String gameName) throws Exception {
        String safeOrganizer = safeLabel(organizerName, "Organisateur");
        String safeRequestTitle = safeLabel(requestTitle, "-");
        String safeGameName = safeLabel(gameName, "-");
        String html = """
                <h2>Nouvelle demande de tournoi</h2>
                <p>Une nouvelle demande est en attente de traitement.</p>
                <p>Organisateur: <b>%s</b></p>
                <p>Titre: <b>%s</b></p>
                <p>Jeu: <b>%s</b></p>
                """.formatted(
                escapeHtml(safeOrganizer),
                escapeHtml(safeRequestTitle),
                escapeHtml(safeGameName)
        );
        sendHtml(toEmail, "Nouvelle demande tournoi", html);
    }

    public void sendTeamInviteReceived(String toEmail, String invitedName, String teamName, String captainName, String message) throws Exception {
        String safeInvitedName = safeLabel(invitedName, "joueur");
        String safeTeamName = safeLabel(teamName, "notre equipe");
        String safeCaptainName = safeLabel(captainName, "capitaine");
        String safeMessage = safeLabel(message, "Vous avez recu une invitation d'equipe sur PULSE.");
        String html = """
                <h2>Nouvelle invitation d'equipe</h2>
                <p>Bonjour %s,</p>
                <p>Vous avez recu une invitation pour rejoindre <b>%s</b>.</p>
                <p>Envoyee par: <b>%s</b></p>
                <p>Message:</p>
                <p><i>%s</i></p>
                """.formatted(
                escapeHtml(safeInvitedName),
                escapeHtml(safeTeamName),
                escapeHtml(safeCaptainName),
                escapeHtml(safeMessage)
        );
        sendHtml(toEmail, "Invitation equipe PULSE", html);
    }

    private void sendHtml(String toEmail, String subject, String html) throws Exception {
        if (!mailerDsn.enabled()) {
            throw new IllegalStateException("MAILER_DSN non configure.");
        }

        InternetAddress[] recipients = parseRecipients(toEmail);

        Session session = Session.getInstance(buildProperties(mailerDsn), buildAuthenticator(mailerDsn));
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress, APP_NAME, StandardCharsets.UTF_8.name()));
        message.setRecipients(Message.RecipientType.TO, recipients);
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setContent(html, "text/html; charset=UTF-8");
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

    private static String safeLabel(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static InternetAddress[] parseRecipients(String toEmail) {
        String raw = toEmail == null ? "" : toEmail.trim();
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Adresse email destinataire manquante.");
        }

        try {
            InternetAddress[] recipients = InternetAddress.parse(raw, true);
            if (recipients.length == 0) {
                throw new IllegalArgumentException("Adresse email destinataire invalide.");
            }
            for (InternetAddress recipient : recipients) {
                recipient.validate();
            }
            return recipients;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Adresse email destinataire invalide.", ex);
        }
    }
}
