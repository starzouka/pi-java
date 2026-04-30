package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.PersistentSessionStore;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.AuthLoginResult;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AuthRepository;
import com.pulse.desktop.service.GoogleSignInService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RecaptchaService;
import com.pulse.desktop.service.SteamSignInService;
import com.pulse.desktop.service.TournamentNotificationTaskService;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.Validators;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.sql.SQLException;

public class LoginController implements RouteAwarePage {
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox rememberMeCheck;
    @FXML
    private Button verifyCaptchaButton;
    @FXML
    private Button googleSignInButton;
    @FXML
    private Button steamSignInButton;
    @FXML
    private Label captchaStatusLabel;
    @FXML
    private Label feedbackLabel;

    private final AuthRepository authRepository = new AuthRepository();
    private final GoogleSignInService googleSignInService = new GoogleSignInService();
    private final SteamSignInService steamSignInService = new SteamSignInService();
    private final RecaptchaService recaptchaService = new RecaptchaService();
    private String captchaToken;

    @FXML
    private void submitLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheck != null && rememberMeCheck.isSelected();
        if (Validators.isBlank(email) || Validators.isBlank(password)) {
            feedbackLabel.setText("Email et mot de passe obligatoires.");
            return;
        }
        if (!verifyCaptchaBeforeSubmit()) {
            return;
        }

        try {
            AuthLoginResult result = authRepository.login(email, password);
            handleLoginResult(result, rememberMe, "Identifiants invalides.");
            resetCaptchaState();
        } catch (SQLException ex) {
            AlertUtils.error("Connexion", "Erreur DB pendant le login.\n" + ex.getMessage());
            resetCaptchaState();
        }
    }

    @FXML
    private void verifyCaptcha() {
        if (!recaptchaService.isEnabled()) {
            captchaToken = "disabled";
            captchaStatusLabel.setText("CAPTCHA desactive (configuration).");
            return;
        }

        Window owner = feedbackLabel.getScene() == null ? null : feedbackLabel.getScene().getWindow();
        String token = RecaptchaDialog.requestToken(owner, recaptchaService.siteKey());
        if (token == null || token.isBlank()) {
            captchaToken = null;
            captchaStatusLabel.setText("CAPTCHA non valide.");
            return;
        }
        captchaToken = token;
        captchaStatusLabel.setText("CAPTCHA valide.");
    }

    @FXML
    private void signInWithGoogle() {
        if (!googleSignInService.isFeatureEnabled()) {
            feedbackLabel.setText("Google Sign-In desactive dans la configuration.");
            return;
        }
        if (!googleSignInService.isEnabled()) {
            String reason = googleSignInService.configurationErrorMessage();
            String redirect = googleSignInService.configuredRedirectUri();
            feedbackLabel.setText((reason == null || reason.isBlank() ? "Google Sign-In non configure." : reason)
                    + " Configurez GOOGLE_CLIENT_ID dans C:/Users/MSI/Downloads/PULSE1/PULSE/PULSE/.env.local"
                    + " | Redirect URI attendu: " + redirect);
            return;
        }

        setSocialSignInBusy(true);
        feedbackLabel.setText("Ouverture de Google Sign-In...");

        boolean rememberMe = rememberMeCheck != null && rememberMeCheck.isSelected();
        Thread worker = new Thread(() -> {
            try {
                GoogleSignInService.GoogleIdentity identity = googleSignInService.authenticate();
                AuthLoginResult result = authRepository.loginOrRegisterGoogleUser(
                        identity.email(),
                        identity.displayName(),
                        identity.subject(),
                        identity.emailVerified()
                );
                Platform.runLater(() -> {
                    setSocialSignInBusy(false);
                    handleLoginResult(result, rememberMe, "Connexion Google invalide.");
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    setSocialSignInBusy(false);
                    AlertUtils.error("Google Sign-In", "Erreur DB pendant la connexion Google.\n" + ex.getMessage());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setSocialSignInBusy(false);
                    feedbackLabel.setText("Google Sign-In annule ou indisponible: " + ex.getMessage());
                });
            }
        }, "google-signin-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void signInWithSteam() {
        if (!steamSignInService.isFeatureEnabled()) {
            feedbackLabel.setText("Steam Sign-In desactive dans la configuration.");
            return;
        }
        if (!steamSignInService.isEnabled()) {
            String reason = steamSignInService.configurationErrorMessage();
            String redirect = steamSignInService.configuredRedirectUri();
            feedbackLabel.setText((reason == null || reason.isBlank() ? "Steam Sign-In non configure." : reason)
                    + " | Redirect URI attendu: " + redirect);
            return;
        }

        setSocialSignInBusy(true);
        feedbackLabel.setText("Ouverture de Steam Sign-In...");

        boolean rememberMe = rememberMeCheck != null && rememberMeCheck.isSelected();
        Thread worker = new Thread(() -> {
            try {
                SteamSignInService.SteamIdentity identity = steamSignInService.authenticate();
                AuthLoginResult result = authRepository.loginOrRegisterSteamUser(
                        identity.steamId(),
                        identity.displayName()
                );
                Platform.runLater(() -> {
                    setSocialSignInBusy(false);
                    handleLoginResult(result, rememberMe, "Connexion Steam invalide.");
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> {
                    setSocialSignInBusy(false);
                    AlertUtils.error("Steam Sign-In", "Erreur DB pendant la connexion Steam.\n" + ex.getMessage());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setSocialSignInBusy(false);
                    feedbackLabel.setText("Steam Sign-In annule ou indisponible: " + ex.getMessage());
                });
            }
        }, "steam-signin-worker");
        worker.setDaemon(true);
        worker.start();
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
        rememberMeCheck.setSelected(true);
        resetCaptchaState();
        boolean enabled = recaptchaService.isEnabled();
        if (verifyCaptchaButton != null) {
            verifyCaptchaButton.setDisable(!enabled);
        }
        if (captchaStatusLabel != null) {
            captchaStatusLabel.setText(enabled ? "Validez le CAPTCHA Google avant connexion." : "CAPTCHA desactive.");
        }
        if (googleSignInButton != null) {
            googleSignInButton.setDisable(!googleSignInService.isFeatureEnabled());
        }
        if (steamSignInButton != null) {
            steamSignInButton.setDisable(!steamSignInService.isFeatureEnabled());
        }
        if (SessionContext.isAuthenticated()) {
            Navigator.goTo("front_profile");
        }
    }

    private boolean verifyCaptchaBeforeSubmit() {
        RecaptchaService.VerificationResult verification = recaptchaService.verifyToken(captchaToken);
        if (!verification.ok()) {
            feedbackLabel.setText(verification.message());
            captchaStatusLabel.setText("Validez le CAPTCHA puis reessayez.");
            resetCaptchaState();
            return false;
        }
        captchaStatusLabel.setText("CAPTCHA confirme.");
        resetCaptchaState();
        return true;
    }

    private void handleLoginResult(AuthLoginResult result, boolean rememberMe, String invalidCredentialsMessage) {
        switch (result.status()) {
            case SUCCESS -> {
                SessionUser user = result.user();
                SessionContext.login(user);
                if (rememberMe) {
                    PersistentSessionStore.rememberUser(user.getUserId());
                } else {
                    PersistentSessionStore.clearRememberedUser();
                }
                TournamentNotificationTaskService.syncWithRememberedSession(rememberMe);
                feedbackLabel.setText("Connexion reussie.");
                Navigator.authChanged();
                Navigator.goTo("front_dashboard");
            }
            case TWO_FACTOR_REQUIRED -> {
                SessionContext.beginTwoFactor(result.user());
                SessionContext.setPendingTwoFactorRememberMe(rememberMe);
                feedbackLabel.setText("Code 2FA requis pour terminer la connexion.");
                Navigator.goTo("front_two_factor_challenge");
            }
            case EMAIL_NOT_VERIFIED -> {
                SessionContext.clearPendingTwoFactor();
                feedbackLabel.setText("Votre email n'est pas verifie. Verifiez votre boite mail.");
            }
            case ACCOUNT_INACTIVE -> {
                SessionContext.clearPendingTwoFactor();
                feedbackLabel.setText("Compte inactif.");
            }
            default -> {
                SessionContext.clearPendingTwoFactor();
                feedbackLabel.setText(invalidCredentialsMessage);
            }
        }
    }

    private void setSocialSignInBusy(boolean busy) {
        if (googleSignInButton != null) {
            googleSignInButton.setDisable(busy || !googleSignInService.isFeatureEnabled());
        }
        if (steamSignInButton != null) {
            steamSignInButton.setDisable(busy || !steamSignInService.isFeatureEnabled());
        }
    }

    private void resetCaptchaState() {
        captchaToken = null;
    }
}
