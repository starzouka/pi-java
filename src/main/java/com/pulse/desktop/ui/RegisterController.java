package com.pulse.desktop.ui;

import com.pulse.desktop.auth.PersistentSessionStore;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.AuthLoginResult;
import com.pulse.desktop.model.RegisterFormData;
import com.pulse.desktop.model.RegisteredUser;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AuthRepository;
import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.service.GoogleSignInService;
import com.pulse.desktop.service.MailService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RecaptchaService;
import com.pulse.desktop.service.SteamSignInService;
import com.pulse.desktop.service.SymfonyVerifyEmailSigner;
import com.pulse.desktop.service.TournamentNotificationTaskService;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.Validators;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.sql.SQLException;
import java.time.LocalDate;

public class RegisterController implements RouteAwarePage {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private TextField displayNameField;
    @FXML
    private TextField countryField;
    @FXML
    private TextField phoneField;
    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private ComboBox<String> genderCombo;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private CheckBox agreeTermsCheck;
    @FXML
    private Button verifyCaptchaButton;
    @FXML
    private Button googleSignUpButton;
    @FXML
    private Button steamSignUpButton;
    @FXML
    private Label captchaStatusLabel;
    @FXML
    private Label feedbackLabel;

    private final AuthRepository authRepository = new AuthRepository();
    private final MailService mailService = new MailService();
    private final SymfonyVerifyEmailSigner verifyEmailSigner = new SymfonyVerifyEmailSigner();
    private final RecaptchaService recaptchaService = new RecaptchaService();
    private final GoogleSignInService googleSignInService = new GoogleSignInService();
    private final SteamSignInService steamSignInService = new SteamSignInService();
    private String captchaToken;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("PLAYER", "CAPTAIN", "ORGANIZER"));
        roleCombo.getSelectionModel().select("PLAYER");
        genderCombo.setItems(FXCollections.observableArrayList("UNKNOWN", "MALE", "FEMALE", "OTHER"));
        genderCombo.getSelectionModel().select("UNKNOWN");
    }

    @FXML
    private void submitRegistration() {
        if (!validateForm()) {
            return;
        }
        if (!verifyCaptchaBeforeSubmit()) {
            return;
        }

        RegisterFormData data = new RegisterFormData();
        data.setUsername(usernameField.getText().trim());
        data.setEmail(emailField.getText().trim().toLowerCase());
        data.setRole(roleCombo.getValue());
        data.setDisplayName(displayNameField.getText().trim());
        data.setCountry(countryField.getText());
        data.setPhone(phoneField.getText());
        data.setBirthDate(birthDatePicker.getValue());
        data.setGender(genderCombo.getValue());
        data.setPassword(passwordField.getText());

        try {
            if (authRepository.usernameExists(data.getUsername())) {
                feedbackLabel.setText("Username deja utilise.");
                return;
            }
            if (authRepository.emailExists(data.getEmail())) {
                feedbackLabel.setText("Email deja utilise.");
                return;
            }
            RegisteredUser created = authRepository.register(data);
            feedbackLabel.setText("Compte cree. Verification email en cours...");

            if (mailService.isConfigured()) {
                String signedUrl = verifyEmailSigner.buildSignedVerificationUrl(created.userId(), created.email());
                mailService.sendEmailVerification(created.email(), signedUrl, AppConfig.verifyEmailLifetimeSeconds());
                feedbackLabel.setText("Inscription reussie. Verifiez votre boite email pour activer le compte.");
            } else {
                feedbackLabel.setText("Compte cree, mais MAILER_DSN est non configure. Verification email non envoyee.");
            }

            clearForm();
            resetCaptchaState();
            Navigator.goTo("front_login");
        } catch (SQLException ex) {
            AlertUtils.error("Inscription", "Impossible de creer le compte.\n" + ex.getMessage());
            resetCaptchaState();
        } catch (Exception ex) {
            AlertUtils.error("Inscription", "Compte cree mais email de verification non envoye.\n" + ex.getMessage());
            resetCaptchaState();
            Navigator.goTo("front_login");
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
    private void signUpWithGoogle() {
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
                    handleSocialAuthResult(result, "Google");
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
        }, "google-signup-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void signUpWithSteam() {
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

        Thread worker = new Thread(() -> {
            try {
                SteamSignInService.SteamIdentity identity = steamSignInService.authenticate();
                AuthLoginResult result = authRepository.loginOrRegisterSteamUser(
                        identity.steamId(),
                        identity.displayName()
                );

                Platform.runLater(() -> {
                    setSocialSignInBusy(false);
                    handleSocialAuthResult(result, "Steam");
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
        }, "steam-signup-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void openLogin() {
        Navigator.goTo("front_login");
    }

    @FXML
    private void backHome() {
        Navigator.goTo("front_home");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        feedbackLabel.setText("Creez votre compte (joueur, capitaine, organisateur).");
        resetCaptchaState();
        boolean enabled = recaptchaService.isEnabled();
        if (verifyCaptchaButton != null) {
            verifyCaptchaButton.setDisable(!enabled);
        }
        if (captchaStatusLabel != null) {
            captchaStatusLabel.setText(enabled ? "Validez le CAPTCHA Google avant inscription." : "CAPTCHA desactive.");
        }
        if (googleSignUpButton != null) {
            googleSignUpButton.setDisable(!googleSignInService.isFeatureEnabled());
        }
        if (steamSignUpButton != null) {
            steamSignUpButton.setDisable(!steamSignInService.isFeatureEnabled());
        }
        if (SessionContext.isAuthenticated()) {
            Navigator.goTo("front_home");
        }
    }

    private boolean validateForm() {
        if (Validators.isBlank(usernameField.getText()) || Validators.isBlank(emailField.getText())
                || Validators.isBlank(displayNameField.getText())
                || Validators.isBlank(passwordField.getText()) || Validators.isBlank(confirmPasswordField.getText())) {
            feedbackLabel.setText("Veuillez remplir tous les champs obligatoires.");
            return false;
        }
        if (!emailField.getText().contains("@")) {
            feedbackLabel.setText("Email invalide.");
            return false;
        }
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            feedbackLabel.setText("La confirmation du mot de passe est invalide.");
            return false;
        }
        if (passwordField.getText().length() < 8) {
            feedbackLabel.setText("Mot de passe trop court (min 8).");
            return false;
        }
        LocalDate birthDate = birthDatePicker.getValue();
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            feedbackLabel.setText("Date de naissance invalide.");
            return false;
        }
        if (birthDate != null && birthDate.isAfter(LocalDate.now().minusYears(13))) {
            feedbackLabel.setText("Age minimum requis: 13 ans.");
            return false;
        }
        if (!agreeTermsCheck.isSelected()) {
            feedbackLabel.setText("Vous devez accepter les conditions.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        usernameField.clear();
        emailField.clear();
        displayNameField.clear();
        countryField.clear();
        phoneField.clear();
        birthDatePicker.setValue(null);
        passwordField.clear();
        confirmPasswordField.clear();
        agreeTermsCheck.setSelected(false);
        roleCombo.getSelectionModel().select("PLAYER");
        genderCombo.getSelectionModel().select("UNKNOWN");
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

    private void resetCaptchaState() {
        captchaToken = null;
    }

    private void setSocialSignInBusy(boolean busy) {
        if (googleSignUpButton != null) {
            googleSignUpButton.setDisable(busy || !googleSignInService.isFeatureEnabled());
        }
        if (steamSignUpButton != null) {
            steamSignUpButton.setDisable(busy || !steamSignInService.isFeatureEnabled());
        }
    }

    private void handleSocialAuthResult(AuthLoginResult result, String providerLabel) {
        String provider = providerLabel == null || providerLabel.isBlank() ? "Social" : providerLabel.trim();
        if (result == null) {
            feedbackLabel.setText("Connexion " + provider + " invalide.");
            return;
        }
        switch (result.status()) {
            case SUCCESS -> {
                SessionUser user = result.user();
                SessionContext.login(user);
                PersistentSessionStore.clearRememberedUser();
                TournamentNotificationTaskService.syncWithRememberedSession(false);
                feedbackLabel.setText("Connexion " + provider + " reussie.");
                Navigator.authChanged();
                Navigator.goTo("front_dashboard");
            }
            case TWO_FACTOR_REQUIRED -> {
                SessionContext.beginTwoFactor(result.user());
                SessionContext.setPendingTwoFactorRememberMe(false);
                feedbackLabel.setText("Code 2FA requis pour terminer la connexion.");
                Navigator.goTo("front_two_factor_challenge");
            }
            case ACCOUNT_INACTIVE -> feedbackLabel.setText("Compte inactif.");
            case EMAIL_NOT_VERIFIED -> feedbackLabel.setText("Email non verifie.");
            default -> feedbackLabel.setText("Connexion " + provider + " invalide.");
        }
    }
}
