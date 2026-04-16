package com.pulse.desktop.ui;

import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.model.UserModel;
import com.pulse.desktop.repo.UserRepository;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class AdminUserController implements RouteAwarePage {

    @FXML private TableView<UserModel> usersTable;
    @FXML private TableColumn<UserModel, Integer> idCol;
    @FXML private TableColumn<UserModel, String> usernameCol;
    @FXML private TableColumn<UserModel, String> emailCol;
    @FXML private TableColumn<UserModel, String> displayNameCol;
    @FXML private TableColumn<UserModel, String> roleCol;
    @FXML private TableColumn<UserModel, Boolean> activeCol;

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField displayNameField;
    @FXML private ComboBox<String> roleBox;
    @FXML private CheckBox activeCheck;
    @FXML private PasswordField passwordField;

    private final UserRepository userRepo = new UserRepository();
    private final ObservableList<UserModel> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        usersTable.setItems(data);
        roleBox.setItems(FXCollections.observableArrayList(List.of("PLAYER", "CAPTAIN", "ORGANIZER", "ADMIN")));
        roleBox.getSelectionModel().select("PLAYER");
        activeCheck.setSelected(true);

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel == null) {
                return;
            }
            usernameField.setText(newSel.getUsername());
            emailField.setText(newSel.getEmail());
            displayNameField.setText(newSel.getDisplayName());
            roleBox.getSelectionModel().select(newSel.getRole());
            activeCheck.setSelected(newSel.isActive());
            passwordField.clear();
        });

        loadData();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
    }

    @FXML
    private void loadData() {
        try {
            data.setAll(userRepo.findAll());
        } catch (Exception e) {
            AlertUtils.error("Erreur DB", "Chargement echoue: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        try {
            if (usernameField.getText() == null || usernameField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un username.");
                return;
            }
            if (emailField.getText() == null || emailField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un email.");
                return;
            }
            if (displayNameField.getText() == null || displayNameField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un nom d'affichage.");
                return;
            }
            if (passwordField.getText() == null || passwordField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un mot de passe (ajout).");
                return;
            }

            UserModel user = new UserModel();
            user.setUsername(usernameField.getText().trim());
            user.setEmail(emailField.getText().trim());
            user.setDisplayName(displayNameField.getText().trim());
            user.setRole(roleBox.getSelectionModel().getSelectedItem());
            user.setActive(activeCheck.isSelected());

            userRepo.insert(user, passwordField.getText());
            AlertUtils.info("Succes", "Utilisateur ajoute.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Ajout echoue: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        UserModel sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner un utilisateur.");
            return;
        }
        try {
            if (usernameField.getText() == null || usernameField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un username.");
                return;
            }
            if (emailField.getText() == null || emailField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un email.");
                return;
            }
            if (displayNameField.getText() == null || displayNameField.getText().isBlank()) {
                AlertUtils.warning("Validation", "Veuillez saisir un nom d'affichage.");
                return;
            }

            sel.setUsername(usernameField.getText().trim());
            sel.setEmail(emailField.getText().trim());
            sel.setDisplayName(displayNameField.getText().trim());
            sel.setRole(roleBox.getSelectionModel().getSelectedItem());
            sel.setActive(activeCheck.isSelected());

            userRepo.update(sel);
            AlertUtils.info("Succes", "Utilisateur modifie.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Modification echouee: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        UserModel sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner un utilisateur.");
            return;
        }
        try {
            userRepo.deleteById(sel.getUserId());
            AlertUtils.info("Succes", "Utilisateur supprime.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Suppression echouee: " + e.getMessage());
        }
    }

    @FXML
    private void clearFields() {
        usersTable.getSelectionModel().clearSelection();
        usernameField.clear();
        emailField.clear();
        displayNameField.clear();
        roleBox.getSelectionModel().select("PLAYER");
        activeCheck.setSelected(true);
        passwordField.clear();
    }
}

