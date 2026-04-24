package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.model.AuthLoginResult;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AuthRepository;
import com.pulse.desktop.service.MailService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ClipboardUtils;
import com.pulse.desktop.util.Validators;
import com.pulse.desktop.service.SymfonyVerifyEmailSigner;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class LoginController implements RouteAwarePage {
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox rememberMeCheck;
    @FXML
    private Label feedbackLabel;

    private final AuthRepository authRepository = new AuthRepository();
    private final MailService mailService = new MailService();
    private final SymfonyVerifyEmailSigner verifyEmailSigner = new SymfonyVerifyEmailSigner();

    @FXML
    private void submitLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        if (Validators.isBlank(email) || Validators.isBlank(password)) {
            feedbackLabel.setText("Email et mot de passe obligatoires.");
            return;
        }

        try {
            AuthLoginResult result = authRepository.login(email, password);
            switch (result.status()) {
                case SUCCESS -> {
                    SessionUser user = result.user();
                    SessionContext.login(user);
                    feedbackLabel.setText("Connexion reussie.");
                    Navigator.authChanged();
                    if ("ADMIN".equals(SessionContext.currentRole())) {
                        Navigator.goTo("admin_categories");
                    } else {
                        Navigator.goTo("front_dashboard");
                    }
                }
                case TWO_FACTOR_REQUIRED -> {
                    SessionContext.beginTwoFactor(result.user());
                    feedbackLabel.setText("Code 2FA requis pour terminer la connexion.");
                    Navigator.goTo("front_two_factor_challenge");
                }
                case EMAIL_NOT_VERIFIED -> feedbackLabel.setText("Votre email n'est pas verifie. Cliquez sur \"Renvoyer verification\".");
                case ACCOUNT_INACTIVE -> feedbackLabel.setText("Compte inactif.");
                default -> feedbackLabel.setText("Identifiants invalides.");
            }
        } catch (SQLException ex) {
            AlertUtils.error("Connexion", "Erreur DB pendant le login.\n" + ex.getMessage());
        }
    }

    @FXML
    private void resendVerificationEmail() {
        String email = emailField.getText();
        String password = passwordField.getText();
        if (Validators.isBlank(email) || !email.contains("@")) {
            feedbackLabel.setText("Entrez votre email pour renvoyer la verification.");
            return;
        }
        if (Validators.isBlank(password)) {
            feedbackLabel.setText("Entrez votre mot de passe pour renvoyer la verification.");
            return;
        }

        try {
            AuthLoginResult result = authRepository.login(email, password);
            if (result.status() == AuthLoginResult.Status.EMAIL_NOT_VERIFIED && result.user() != null) {
                SessionUser user = result.user();
                String signedUrl = verifyEmailSigner.buildSignedVerificationUrl(user.getUserId(), user.getEmail());

                if (mailService.isConfigured()) {
                    mailService.sendEmailVerification(user.getEmail(), signedUrl, AppConfig.verifyEmailLifetimeSeconds());
                    feedbackLabel.setText("Email de verification renvoye. Verifiez votre boite mail (spam inclus).");
                } else {
                    ClipboardUtils.copyToClipboard(signedUrl);
                    feedbackLabel.setText("MAILER_DSN non configure. Lien de verification copie dans le presse-papiers.");
                    AlertUtils.info("Verification email",
                            "MAILER_DSN est non configure.\n"
                                    + "Lien de verification copie dans le presse-papiers:\n"
                                    + signedUrl);
                }
                return;
            }

            if (result.status() == AuthLoginResult.Status.SUCCESS) {
                feedbackLabel.setText("Votre compte est deja verifie. Cliquez sur \"Se connecter\".");
                return;
            }
            if (result.status() == AuthLoginResult.Status.TWO_FACTOR_REQUIRED) {
                feedbackLabel.setText("Votre compte est deja verifie. Cliquez sur \"Se connecter\" pour la 2FA.");
                return;
            }
            if (result.status() == AuthLoginResult.Status.ACCOUNT_INACTIVE) {
                feedbackLabel.setText("Compte inactif.");
                return;
            }

            feedbackLabel.setText("Identifiants invalides.");
        } catch (SQLException ex) {
            AlertUtils.error("Verification email", "Erreur DB.\n" + ex.getMessage());
        } catch (Exception ex) {
            AlertUtils.error("Verification email", "Impossible de renvoyer l'email.\n" + ex.getMessage());
        }
    }

    @FXML
    private void openRegister() {
        Navigator.goTo("front_register");
    }

    @FXML
    private void openForgotPassword() {
        Navigator.goTo("front_forgot_password");
    }

    @FXML
    private void backHome() {
        Navigator.goTo("front_home");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        feedbackLabel.setText("Connectez-vous pour acceder aux actions sensibles.");
        rememberMeCheck.setSelected(false);
        if (SessionContext.isAuthenticated()) {
            Navigator.goTo("front_profile");
        }
    }
}
