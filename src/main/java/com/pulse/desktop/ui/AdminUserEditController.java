package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AdminUserRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
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

public class AdminUserEditController implements RouteAwarePage {
    private static final LookupItem NONE_IMAGE = new LookupItem(0, "Aucune image");

    @FXML
    private Label titleLabel;
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
    private Integer currentUserId;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("PLAYER", "CAPTAIN", "ORGANIZER", "ADMIN"));
        genderCombo.setItems(FXCollections.observableArrayList("UNKNOWN", "MALE", "FEMALE", "OTHER"));
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }

        Integer selectedUserId = RouteContext.getInt(RouteContext.KEY_USER_ID);
        if (selectedUserId == null || selectedUserId <= 0) {
            AlertUtils.warning("Admin user edit", "Aucun utilisateur selectionne.");
            Navigator.goTo("admin_users");
            return;
        }

        this.currentUserId = selectedUserId;
        loadImages();
        loadUser();
    }

    @FXML
    private void saveUser() {
        SessionUser admin = requireAdmin();
        if (admin == null || currentUserId == null || currentUserId <= 0) {
            return;
        }

        AdminUserRepository.UserMutationInput input = new AdminUserRepository.UserMutationInput(
                currentUserId,
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
            AdminUserRepository.OperationResult result = repository.updateUser(input);
            formFeedbackLabel.setText(result.message());
            if (!result.ok()) {
                return;
            }
            AlertUtils.info("Admin users", result.message());
            RouteContext.putInt(RouteContext.KEY_USER_ID, currentUserId);
            Navigator.goTo("admin_user_detail");
        } catch (SQLException ex) {
            AlertUtils.error("Admin users", "Erreur update utilisateur.\n" + ex.getMessage());
        }
    }

    @FXML
    private void backDetail() {
        if (currentUserId != null && currentUserId > 0) {
            RouteContext.putInt(RouteContext.KEY_USER_ID, currentUserId);
            Navigator.goTo("admin_user_detail");
            return;
        }
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

    private void loadUser() {
        try {
            AdminUserRepository.UserDetail user = repository.loadUserDetail(currentUserId);
            if (user == null) {
                AlertUtils.warning("Admin user edit", "Utilisateur introuvable.");
                Navigator.goTo("admin_users");
                return;
            }

            titleLabel.setText("Modifier utilisateur #" + user.userId());
            usernameField.setText(user.username());
            emailField.setText(user.email());
            passwordField.clear();
            roleCombo.getSelectionModel().select(user.role());
            displayNameField.setText(user.displayName());
            phoneField.setText(user.phone() == null ? "" : user.phone());
            countryField.setText(user.country() == null ? "" : user.country());
            birthDatePicker.setValue(user.birthDate());
            genderCombo.getSelectionModel().select(user.gender() == null ? "UNKNOWN" : user.gender());
            bioArea.setText(user.bio() == null ? "" : user.bio());
            emailVerifiedCheck.setSelected(user.emailVerified());
            activeCheck.setSelected(user.active());
            selectImageById(user.profileImageId());
            formFeedbackLabel.setText("");
        } catch (SQLException ex) {
            AlertUtils.error("Admin user edit", "Impossible de charger utilisateur.\n" + ex.getMessage());
        }
    }

    private Integer selectedImageId() {
        LookupItem selected = profileImageCombo.getValue();
        if (selected == null || selected.getId() <= 0) {
            return null;
        }
        return selected.getId();
    }

    private void selectImageById(Integer imageId) {
        if (imageId == null || imageId <= 0) {
            profileImageCombo.getSelectionModel().select(0);
            return;
        }
        for (LookupItem item : profileImageCombo.getItems()) {
            if (item.getId() == imageId) {
                profileImageCombo.getSelectionModel().select(item);
                return;
            }
        }
        profileImageCombo.getSelectionModel().select(0);
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
