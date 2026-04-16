package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AuthRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.TwoFactorTotpService;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.Validators;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class TwoFactorChallengeController implements RouteAwarePage {
    @FXML
    private TextField codeField;
    @FXML
    private Label feedbackLabel;

    private final AuthRepository authRepository = new AuthRepository();
    private final TwoFactorTotpService totpService = new TwoFactorTotpService();

    @FXML
    private void verifyCode() {
        SessionUser pendingUser = SessionContext.getPendingTwoFactorUser();
        if (pendingUser == null) {
            feedbackLabel.setText("Aucune connexion 2FA en attente.");
            Navigator.goTo("front_login");
            return;
        }

        String code = codeField.getText();
        if (Validators.isBlank(code)) {
            feedbackLabel.setText("Code a 6 chiffres requis.");
            return;
        }

        try {
            String secret = authRepository.getTwoFactorSecret(pendingUser.getUserId());
            if (secret == null || secret.isBlank()) {
                feedbackLabel.setText("2FA non configuree sur ce compte.");
                SessionContext.clearPendingTwoFactor();
                Navigator.goTo("front_login");
                return;
            }

            if (!totpService.verifyCode(secret, code, 1)) {
                feedbackLabel.setText("Code invalide. Verifiez votre application Authenticator.");
                return;
            }

            authRepository.markLoginSuccess(pendingUser.getUserId());
            SessionContext.login(pendingUser);
            Navigator.authChanged();
            feedbackLabel.setText("Verification 2FA reussie.");
            Navigator.goTo("front_dashboard");
        } catch (SQLException ex) {
            AlertUtils.error("2FA", "Erreur DB pendant la verification 2FA.\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancelAndBackLogin() {
        SessionContext.clearPendingTwoFactor();
        Navigator.goTo("front_login");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        if (!SessionContext.hasPendingTwoFactor()) {
            feedbackLabel.setText("Aucune verification 2FA en attente.");
            Navigator.goTo("front_login");
            return;
        }
        feedbackLabel.setText("Entrez le code genere par votre application Authenticator.");
        codeField.clear();
    }
}
