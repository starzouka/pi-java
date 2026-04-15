package com.pulse.desktop.ui;

import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AuthRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.Validators;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class ResetPasswordController implements RouteAwarePage {
    @FXML
    private TextField tokenField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label feedbackLabel;

    private final AuthRepository authRepository = new AuthRepository();

    @FXML
    private void applyReset() {
        String token = tokenField.getText();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (Validators.isBlank(token)) {
            feedbackLabel.setText("Token requis.");
            return;
        }
        if (Validators.isBlank(password) || password.length() < 8) {
            feedbackLabel.setText("Mot de passe minimum 8 caracteres.");
            return;
        }
        if (!password.equals(confirm)) {
            feedbackLabel.setText("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            boolean updated = authRepository.resetPasswordByToken(token.trim(), password);
            if (!updated) {
                feedbackLabel.setText("Lien invalide ou expire.");
                return;
            }
            feedbackLabel.setText("Mot de passe mis a jour. Connectez-vous.");
            Navigator.goTo("front_login");
        } catch (SQLException ex) {
            AlertUtils.error("Reset mot de passe", "Erreur DB.\n" + ex.getMessage());
        }
    }

    @FXML
    private void checkToken() {
        String token = tokenField.getText();
        if (Validators.isBlank(token)) {
            feedbackLabel.setText("Entrez le token a verifier.");
            return;
        }
        try {
            boolean valid = authRepository.hasValidResetPasswordToken(token.trim());
            feedbackLabel.setText(valid ? "Token valide." : "Token invalide ou expire.");
        } catch (SQLException ex) {
            AlertUtils.error("Reset mot de passe", "Erreur DB.\n" + ex.getMessage());
        }
    }

    @FXML
    private void openForgot() {
        Navigator.goTo("front_forgot_password");
    }

    @FXML
    private void openLogin() {
        Navigator.goTo("front_login");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        feedbackLabel.setText("Choisissez un nouveau mot de passe.");
    }
}
