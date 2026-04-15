package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.ProfileData;
import com.pulse.desktop.model.ProfileFilters;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AuthRepository;
import com.pulse.desktop.repo.ProfileRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.TwoFactorTotpService;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import com.pulse.desktop.util.Validators;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ProfileEditController implements RouteAwarePage {
    private static final DateTimeFormatter TWO_FACTOR_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private ImageView avatarView;
    @FXML
    private TextField photoPathField;

    @FXML
    private TextField displayNameField;
    @FXML
    private TextArea bioField;
    @FXML
    private TextField countryField;
    @FXML
    private TextField phoneField;
    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private ComboBox<String> genderCombo;
    @FXML
    private CheckBox activeCheck;
    @FXML
    private Label feedbackLabel;

    @FXML
    private Label twoFactorStatusLabel;
    @FXML
    private Label twoFactorMessageLabel;
    @FXML
    private ImageView twoFactorQrView;
    @FXML
    private Label twoFactorSecretLabel;
    @FXML
    private TextField twoFactorCodeField;
    @FXML
    private Button twoFactorSetupButton;
    @FXML
    private Button twoFactorEnableButton;
    @FXML
    private Button twoFactorDisableButton;

    private final ProfileRepository profileRepository = new ProfileRepository();
    private final AuthRepository authRepository = new AuthRepository();
    private final TwoFactorTotpService totpService = new TwoFactorTotpService();

    private File selectedPhotoFile;
    private boolean twoFactorEnabled;

    @FXML
    public void initialize() {
        genderCombo.setItems(FXCollections.observableArrayList("UNKNOWN", "MALE", "FEMALE", "OTHER"));
        genderCombo.getSelectionModel().select("UNKNOWN");
        photoPathField.setEditable(false);
        updateTwoFactorControls();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        selectedPhotoFile = null;
        photoPathField.clear();
        feedbackLabel.setText("Mettez a jour vos informations personnelles.");
        loadProfile();
    }

    @FXML
    private void choosePhoto() {
        Window window = displayNameField.getScene() == null ? null : displayNameField.getScene().getWindow();
        if (window == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );
        File file = chooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        selectedPhotoFile = file;
        photoPathField.setText(file.getAbsolutePath());
    }

    @FXML
    private void saveProfile() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        if (Validators.isBlank(displayNameField.getText())) {
            feedbackLabel.setText("Display name obligatoire.");
            return;
        }
        LocalDate birthDate = birthDatePicker.getValue();
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            feedbackLabel.setText("Date de naissance invalide.");
            return;
        }

        try {
            profileRepository.updateProfile(
                    user.getUserId(),
                    displayNameField.getText(),
                    bioField.getText(),
                    countryField.getText(),
                    phoneField.getText(),
                    birthDate,
                    genderCombo.getValue(),
                    activeCheck.isSelected()
            );

            if (selectedPhotoFile != null) {
                profileRepository.updateProfileImage(
                        user.getUserId(),
                        selectedPhotoFile.toPath(),
                        displayNameField.getText()
                );
            }

            feedbackLabel.setText("Profil mis a jour avec succes.");
            selectedPhotoFile = null;
            photoPathField.clear();
            loadProfile();
        } catch (SQLException ex) {
            AlertUtils.error("Modifier profil", "Erreur DB.\n" + ex.getMessage());
        } catch (Exception ex) {
            AlertUtils.error("Modifier profil", "Impossible de sauvegarder le profil.\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancel() {
        Navigator.goTo("front_profile");
    }

    @FXML
    private void setupTwoFactor() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        String secret = totpService.generateSecret(32);
        SessionContext.setPendingTwoFactorSetupSecret(secret);

        String accountLabel = user.getEmail() == null || user.getEmail().isBlank() ? user.getUsername() : user.getEmail();
        String otpAuth = totpService.buildOtpAuthUri("PULSE", accountLabel, secret);
        String qrUrl = totpService.buildQrCodeUrl(otpAuth, 220);

        twoFactorQrView.setImage(new Image(qrUrl, true));
        twoFactorSecretLabel.setText(secret);
        twoFactorMessageLabel.setText("Configuration 2FA generee. Entrez le code pour activer.");
        updateTwoFactorControls();
    }

    @FXML
    private void enableTwoFactor() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        String pendingSecret = SessionContext.getPendingTwoFactorSetupSecret();
        if (pendingSecret == null || pendingSecret.isBlank()) {
            twoFactorMessageLabel.setText("Aucune configuration en attente. Lancez 'Configurer 2FA'.");
            return;
        }

        String code = twoFactorCodeField.getText();
        if (!totpService.verifyCode(pendingSecret, code, 1)) {
            twoFactorMessageLabel.setText("Code invalide. Verifiez l'heure de votre telephone.");
            return;
        }

        try {
            authRepository.enableTwoFactor(user.getUserId(), pendingSecret);
            SessionContext.clearPendingTwoFactorSetupSecret();
            twoFactorCodeField.clear();
            twoFactorMessageLabel.setText("Authentification a deux facteurs activee.");
            loadProfile();
        } catch (SQLException ex) {
            AlertUtils.error("2FA", "Impossible d'activer la 2FA.\n" + ex.getMessage());
        }
    }

    @FXML
    private void disableTwoFactor() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        String code = twoFactorCodeField.getText();
        if (Validators.isBlank(code)) {
            twoFactorMessageLabel.setText("Saisissez un code actuel pour desactiver la 2FA.");
            return;
        }

        try {
            String secret = authRepository.getTwoFactorSecret(user.getUserId());
            if (secret == null || secret.isBlank() || !totpService.verifyCode(secret, code, 1)) {
                twoFactorMessageLabel.setText("Code invalide. La desactivation est refusee.");
                return;
            }

            authRepository.disableTwoFactor(user.getUserId());
            twoFactorCodeField.clear();
            twoFactorMessageLabel.setText("Authentification a deux facteurs desactivee.");
            SessionContext.clearPendingTwoFactorSetupSecret();
            loadProfile();
        } catch (SQLException ex) {
            AlertUtils.error("2FA", "Impossible de desactiver la 2FA.\n" + ex.getMessage());
        }
    }

    private void loadProfile() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            ProfileData data = profileRepository.loadOwnProfile(user.getUserId(), ProfileFilters.defaults());
            if (data == null) {
                feedbackLabel.setText("Profil introuvable.");
                return;
            }

            ProfileData.Identity identity = data.identity();
            displayNameField.setText(identity.displayName());
            bioField.setText(identity.bio() == null ? "" : identity.bio());
            countryField.setText(identity.country() == null ? "" : identity.country());
            phoneField.setText(identity.phone() == null ? "" : identity.phone());
            birthDatePicker.setValue(identity.birthDate());
            genderCombo.getSelectionModel().select(identity.gender() == null ? "UNKNOWN" : identity.gender());
            activeCheck.setSelected(identity.active());

            String imagePath = identity.profileImagePath();
            if (imagePath == null || imagePath.isBlank()) {
                imagePath = "https://picsum.photos/seed/pulse_profile_edit_" + identity.userId() + "/200/200";
            }
            String external = ImageResolver.toExternalForm(imagePath);
            if (external == null) {
                external = imagePath;
            }
            avatarView.setImage(new Image(external, true));

            twoFactorEnabled = identity.twoFactorEnabled();
            twoFactorStatusLabel.setText(twoFactorEnabled ? "ACTIVE" : "INACTIVE");
            if (twoFactorEnabled && identity.twoFactorEnabledAt() != null) {
                twoFactorMessageLabel.setText("2FA activee le " + identity.twoFactorEnabledAt().format(TWO_FACTOR_DATE));
            } else if (!twoFactorEnabled) {
                twoFactorMessageLabel.setText("2FA inactive.");
            }
            if (SessionContext.getPendingTwoFactorSetupSecret() == null) {
                twoFactorSecretLabel.setText("-");
                twoFactorQrView.setImage(null);
            }
            updateTwoFactorControls();
        } catch (SQLException ex) {
            AlertUtils.error("Modifier profil", "Impossible de charger le profil.\n" + ex.getMessage());
        }
    }

    private void updateTwoFactorControls() {
        boolean hasPendingSetup = SessionContext.getPendingTwoFactorSetupSecret() != null;
        twoFactorSetupButton.setDisable(twoFactorEnabled);
        twoFactorEnableButton.setDisable(twoFactorEnabled || !hasPendingSetup);
        twoFactorDisableButton.setDisable(!twoFactorEnabled);
    }
}
