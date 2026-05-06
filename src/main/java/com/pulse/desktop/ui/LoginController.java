package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.AuthLoginResult;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AuthRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.Validators;
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
                    Navigator.goTo("front_dashboard");
                }
                case TWO_FACTOR_REQUIRED -> {
                    SessionContext.beginTwoFactor(result.user());
                    feedbackLabel.setText("Code 2FA requis pour terminer la connexion.");
                    Navigator.goTo("front_two_factor_challenge");
                }
                case EMAIL_NOT_VERIFIED -> feedbackLabel.setText("Votre email n'est pas verifie. Verifiez votre boite mail.");
                case ACCOUNT_INACTIVE -> feedbackLabel.setText("Compte inactif.");
                default -> feedbackLabel.setText("Identifiants invalides.");
            }
        } catch (SQLException ex) {
            AlertUtils.error("Connexion", "Erreur DB pendant le login.\n" + ex.getMessage());
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
