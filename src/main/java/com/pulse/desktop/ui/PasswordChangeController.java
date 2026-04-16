package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AuthRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.Validators;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.sql.SQLException;

public class PasswordChangeController implements RouteAwarePage {
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label sessionStatusLabel;
    @FXML
    private Label emailVerifiedLabel;
    @FXML
    private Label feedbackLabel;

    private final AuthRepository authRepository = new AuthRepository();

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        feedbackLabel.setText("Securisez votre session active.");

        sessionStatusLabel.setText("ACTIVE");
        emailVerifiedLabel.setText(user.isEmailVerified() ? "Oui" : "Non");
    }

    @FXML
    private void submitPasswordChange() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (Validators.isBlank(currentPassword)) {
            feedbackLabel.setText("Le mot de passe actuel est obligatoire.");
            return;
        }
        if (Validators.isBlank(newPassword)) {
            feedbackLabel.setText("Le nouveau mot de passe est obligatoire.");
            return;
        }
        if (newPassword.length() < 8) {
            feedbackLabel.setText("Le mot de passe doit contenir au moins 8 caracteres.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            feedbackLabel.setText("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            AuthRepository.PasswordChangeStatus status = authRepository.changePassword(
                    user.getUserId(),
                    currentPassword,
                    newPassword
            );

            switch (status) {
                case SUCCESS -> {
                    feedbackLabel.setText("Mot de passe mis a jour avec succes.");
                    Navigator.goTo("front_profile");
                }
                case INVALID_CURRENT_PASSWORD -> feedbackLabel.setText("Mot de passe actuel invalide.");
                case SAME_PASSWORD_AS_OLD -> feedbackLabel.setText("Le nouveau mot de passe doit etre different de l'ancien.");
                default -> feedbackLabel.setText("Utilisateur introuvable.");
            }
        } catch (SQLException ex) {
            AlertUtils.error("Changer mot de passe", "Erreur DB.\n" + ex.getMessage());
        }
    }

    @FXML
    private void goProfile() {
        Navigator.goTo("front_profile");
    }
}
