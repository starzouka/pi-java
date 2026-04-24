package com.pulse.desktop.ui;

import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AuthRepository;
import com.pulse.desktop.service.MailService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ClipboardUtils;
import com.pulse.desktop.util.Validators;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ForgotPasswordController implements RouteAwarePage {
    private static final DateTimeFormatter MAIL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private TextField emailField;
    @FXML
    private Label feedbackLabel;

    private final AuthRepository authRepository = new AuthRepository();
    private final MailService mailService = new MailService();

    @FXML
    private void sendResetLink() {
        String email = emailField.getText();
        if (Validators.isBlank(email) || !email.contains("@")) {
            feedbackLabel.setText("Entrez un email valide.");
            return;
        }

        try {
            int lifetime = AppConfig.resetPasswordLifetimeSeconds();
            String token = authRepository.createResetPasswordToken(email, lifetime);
            if (token == null) {
                feedbackLabel.setText("Cet email n'existe pas.");
                return;
            }

            RouteContext.setPendingResetPasswordToken(token);

            String resetUrl = trimTrailingSlash(AppConfig.webBaseUrl())
                    + "/pages/reset-password?token="
                    + URLEncoder.encode(token, StandardCharsets.UTF_8);

            if (!mailService.isConfigured()) {
                ClipboardUtils.copyToClipboard(token);
                feedbackLabel.setText("MAILER_DSN non configure. Token copie dans le presse-papiers.");
                AlertUtils.info("Mot de passe oublie",
                        "MAILER_DSN est non configure.\n"
                                + "Token copie dans le presse-papiers:\n"
                                + token
                                + "\n\nVous pouvez coller ce token dans la page \"Reset direct (token)\".\n"
                                + "Lien web equivalent (si Symfony tourne):\n"
                                + resetUrl);
                Navigator.goTo("front_reset_password");
                return;
            }

            String expiresAt = LocalDateTime.now().plusSeconds(lifetime).format(MAIL_DATE);
            mailService.sendResetPassword(email.trim().toLowerCase(), resetUrl, expiresAt);
            feedbackLabel.setText("Un email de reinitialisation vous a ete envoye.");
        } catch (SQLException ex) {
            AlertUtils.error("Mot de passe oublie", "Erreur DB.\n" + ex.getMessage());
        } catch (Exception ex) {
            AlertUtils.error("Mot de passe oublie", "Impossible d'envoyer l'email.\n" + ex.getMessage());
        }
    }

    @FXML
    private void openLogin() {
        Navigator.goTo("front_login");
    }

    @FXML
    private void openResetDirect() {
        Navigator.goTo("front_reset_password");
    }

    @FXML
    private void backHome() {
        Navigator.goTo("front_home");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        feedbackLabel.setText("Recevoir un lien de reinitialisation.");
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:8000";
        }
        String out = value.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}
