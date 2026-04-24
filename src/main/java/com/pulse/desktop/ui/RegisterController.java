package com.pulse.desktop.ui;

import com.pulse.desktop.model.RegisterFormData;
import com.pulse.desktop.model.RegisteredUser;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AuthRepository;
import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.service.MailService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.SymfonyVerifyEmailSigner;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ClipboardUtils;
import com.pulse.desktop.util.Validators;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
    private Label feedbackLabel;

    private final AuthRepository authRepository = new AuthRepository();
    private final MailService mailService = new MailService();
    private final SymfonyVerifyEmailSigner verifyEmailSigner = new SymfonyVerifyEmailSigner();

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

            String signedUrl = null;
            try {
                signedUrl = verifyEmailSigner.buildSignedVerificationUrl(created.userId(), created.email());
            } catch (Exception ex) {
                feedbackLabel.setText("Compte cree, mais APP_SECRET est manquant: verification email impossible.");
                AlertUtils.warning("Verification email",
                        "Le compte a ete cree mais le lien de verification n'a pas pu etre genere.\n"
                                + ex.getMessage()
                                + "\n\nChemin attendu du projet Symfony (PULSE_WEB_ROOT / app.web.root):\n"
                                + AppConfig.webRootPath());
            }

            if (signedUrl != null) {
                if (mailService.isConfigured()) {
                    try {
                        mailService.sendEmailVerification(created.email(), signedUrl, AppConfig.verifyEmailLifetimeSeconds());
                        feedbackLabel.setText("Inscription reussie. Verifiez votre boite email pour activer le compte.");
                    } catch (Exception ex) {
                        ClipboardUtils.copyToClipboard(signedUrl);
                        feedbackLabel.setText("Email non envoye. Lien de verification copie dans le presse-papiers.");
                        AlertUtils.warning("Verification email",
                                "Impossible d'envoyer l'email de verification.\n"
                                        + ex.getMessage()
                                        + "\n\nLien de verification copie dans le presse-papiers:\n"
                                        + signedUrl);
                    }
                } else {
                    ClipboardUtils.copyToClipboard(signedUrl);
                    feedbackLabel.setText("MAILER_DSN non configure. Lien de verification copie dans le presse-papiers.");
                    AlertUtils.info("Verification email",
                            "MAILER_DSN est non configure.\n"
                                    + "Lien de verification copie dans le presse-papiers:\n"
                                    + signedUrl);
                }
            }

            clearForm();
            Navigator.goTo("front_login");
        } catch (SQLException ex) {
            AlertUtils.error("Inscription", "Impossible de creer le compte.\n" + ex.getMessage());
        } catch (Exception ex) {
            AlertUtils.error("Inscription", "Compte cree mais email de verification non envoye.\n" + ex.getMessage());
            Navigator.goTo("front_login");
        }
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
}
