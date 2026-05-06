package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AdminUserRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminUserCreateController implements RouteAwarePage {
    private static final LookupItem NONE_IMAGE = new LookupItem(0, "Aucune image");

    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private TextField displayNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField countryField;
    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private ComboBox<String> genderCombo;
    @FXML
    private ComboBox<LookupItem> profileImageCombo;
    @FXML
    private TextArea bioArea;
    @FXML
    private CheckBox emailVerifiedCheck;
    @FXML
    private CheckBox activeCheck;
    @FXML
    private Label formFeedbackLabel;

    private final AdminUserRepository repository = new AdminUserRepository();

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("PLAYER", "CAPTAIN", "ORGANIZER", "ADMIN"));
        roleCombo.getSelectionModel().select("PLAYER");

        genderCombo.setItems(FXCollections.observableArrayList("UNKNOWN", "MALE", "FEMALE", "OTHER"));
        genderCombo.getSelectionModel().select("UNKNOWN");

        activeCheck.setSelected(true);
        emailVerifiedCheck.setSelected(false);
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        loadImages();
        resetForm();
    }

    @FXML
    private void saveUser() {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }

        AdminUserRepository.UserMutationInput input = new AdminUserRepository.UserMutationInput(
                null,
                usernameField.getText(),
                emailField.getText(),
                passwordField.getText(),
                roleCombo.getValue(),
                displayNameField.getText(),
                bioArea.getText(),
                phoneField.getText(),
                countryField.getText(),
                birthDatePicker.getValue(),
                genderCombo.getValue(),
                emailVerifiedCheck.isSelected(),
                activeCheck.isSelected(),
                selectedImageId()
        );

        try {
            AdminUserRepository.OperationResult result = repository.createUser(input);
            formFeedbackLabel.setText(result.message());
            if (!result.ok()) {
                return;
            }
            AlertUtils.info("Admin users", result.message());
            Navigator.goTo("admin_users");
        } catch (SQLException ex) {
            AlertUtils.error("Admin users", "Erreur creation utilisateur.\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancel() {
        Navigator.goTo("admin_users");
    }

    private void loadImages() {
        try {
            List<LookupItem> options = new ArrayList<>();
            options.add(NONE_IMAGE);
            options.addAll(repository.listProfileImageOptions(1000));
            profileImageCombo.setItems(FXCollections.observableArrayList(options));
            profileImageCombo.getSelectionModel().select(0);
        } catch (SQLException ex) {
            profileImageCombo.setItems(FXCollections.observableArrayList(List.of(NONE_IMAGE)));
            profileImageCombo.getSelectionModel().select(0);
            AlertUtils.warning("Admin users", "Impossible de charger les images profil.\n" + ex.getMessage());
        }
    }

    private void resetForm() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        roleCombo.getSelectionModel().select("PLAYER");
        displayNameField.clear();
        phoneField.clear();
        countryField.clear();
        birthDatePicker.setValue(null);
        genderCombo.getSelectionModel().select("UNKNOWN");
        if (!profileImageCombo.getItems().isEmpty()) {
            profileImageCombo.getSelectionModel().select(0);
        }
        bioArea.clear();
        emailVerifiedCheck.setSelected(false);
        activeCheck.setSelected(true);
        formFeedbackLabel.setText("");
    }

    private Integer selectedImageId() {
        LookupItem selected = profileImageCombo.getValue();
        if (selected == null || selected.getId() <= 0) {
            return null;
        }
        return selected.getId();
    }

    private static SessionUser requireAdmin() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            AlertUtils.warning("Connexion requise", "Connectez-vous en admin.");
            Navigator.goTo("front_login");
            return null;
        }
        if (!"ADMIN".equals(SessionContext.currentRole())) {
            AlertUtils.warning("Acces refuse", "Cette page est reservee a l'administration.");
            Navigator.goTo("front_home");
            return null;
        }
        return user;
    }
}
