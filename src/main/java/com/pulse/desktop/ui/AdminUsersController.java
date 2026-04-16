package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AdminUserRepository;
import com.pulse.desktop.service.ExportService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminUsersController implements RouteAwarePage {
    private static final LookupItem ANY = new LookupItem(0, "ALL");

    @FXML
    private ScrollPane pageScroll;
    @FXML
    private TextField qField;
    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private ComboBox<String> activeCombo;
    @FXML
    private ComboBox<String> verifiedCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private ComboBox<String> directionCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label statsTotalLabel;
    @FXML
    private Label statsActiveLabel;
    @FXML
    private Label statsInactiveLabel;
    @FXML
    private Label statsVerifiedLabel;
    @FXML
    private Label statsTwoFactorLabel;
    @FXML
    private Label statsNewUsersLabel;
    @FXML
    private Label statsRecentLoginLabel;
    @FXML
    private VBox roleBarsBox;
    @FXML
    private VBox countryBarsBox;
    @FXML
    private VBox usersListBox;

    private final AdminUserRepository repository = new AdminUserRepository();
    private final ExportService exportService = new ExportService();
    private List<AdminUserRepository.UserRow> currentRows = List.of();

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("", "PLAYER", "CAPTAIN", "ORGANIZER", "ADMIN"));
        roleCombo.getSelectionModel().select(0);

        activeCombo.setItems(FXCollections.observableArrayList("", "1", "0"));
        activeCombo.getSelectionModel().select(0);

        verifiedCombo.setItems(FXCollections.observableArrayList("", "1", "0"));
        verifiedCombo.getSelectionModel().select(0);

        sortCombo.setItems(FXCollections.observableArrayList(
                "created_at", "id", "username", "email", "role", "active", "verified", "country", "last_login_at"
        ));
        sortCombo.getSelectionModel().select("created_at");

        directionCombo.setItems(FXCollections.observableArrayList("desc", "asc"));
        directionCombo.getSelectionModel().select("desc");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        refresh();
    }

    @FXML
    private void applyFilters() {
        refresh();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        roleCombo.getSelectionModel().select(0);
        activeCombo.getSelectionModel().select(0);
        verifiedCombo.getSelectionModel().select(0);
        sortCombo.getSelectionModel().select("created_at");
        directionCombo.getSelectionModel().select("desc");
        refresh();
    }

    @FXML
    private void openCreate() {
        Navigator.goTo("admin_user_create");
    }

    @FXML
    private void exportPdf() {
        export(true);
    }

    @FXML
    private void exportExcel() {
        export(false);
    }

    private void refresh() {
        try {
            AdminUserRepository.UserSearchFilter filter = new AdminUserRepository.UserSearchFilter(
                    qField.getText(),
                    roleCombo.getValue(),
                    activeCombo.getValue(),
                    verifiedCombo.getValue(),
                    sortCombo.getValue(),
                    directionCombo.getValue()
            );

            currentRows = repository.searchForAdmin(filter, 1000);
            AdminUserRepository.UserStats stats = repository.buildStats(currentRows);

            resultCountLabel.setText(currentRows.size() + " resultat(s)");
            renderStats(stats);
            renderRows();
        } catch (SQLException ex) {
            AlertUtils.error("Admin users", "Erreur chargement utilisateurs.\n" + ex.getMessage());
        }
    }

    private void renderStats(AdminUserRepository.UserStats stats) {
        if (stats == null) {
            statsTotalLabel.setText("0");
            statsActiveLabel.setText("0");
            statsInactiveLabel.setText("0");
            statsVerifiedLabel.setText("0");
            statsTwoFactorLabel.setText("0");
            statsNewUsersLabel.setText("0");
            statsRecentLoginLabel.setText("0");
            roleBarsBox.getChildren().setAll(CompetitionUi.emptyState("Aucune donnee."));
            countryBarsBox.getChildren().setAll(CompetitionUi.emptyState("Aucune donnee."));
            return;
        }

        statsTotalLabel.setText(Integer.toString(stats.total()));
        statsActiveLabel.setText(Integer.toString(stats.active()));
        statsInactiveLabel.setText(Integer.toString(stats.inactive()));
        statsVerifiedLabel.setText(Integer.toString(stats.verified()));
        statsTwoFactorLabel.setText(Integer.toString(stats.twoFactor()));
        statsNewUsersLabel.setText(Integer.toString(stats.newUsers30d()));
        statsRecentLoginLabel.setText(Integer.toString(stats.recentLogin7d()));

        roleBarsBox.getChildren().clear();
        for (AdminUserRepository.RoleStat role : stats.roles()) {
            roleBarsBox.getChildren().add(buildBarItem(
                    role.label(),
                    role.count() + " (" + role.percent() + "%)",
                    role.percent() / 100.0
            ));
        }
        if (roleBarsBox.getChildren().isEmpty()) {
            roleBarsBox.getChildren().add(CompetitionUi.emptyState("Aucune donnee role."));
        }

        countryBarsBox.getChildren().clear();
        for (AdminUserRepository.CountryStat country : stats.countries()) {
            countryBarsBox.getChildren().add(buildBarItem(
                    country.name(),
                    country.count() + " (" + country.percent() + "%)",
                    country.percent() / 100.0
            ));
        }
        if (countryBarsBox.getChildren().isEmpty()) {
            countryBarsBox.getChildren().add(CompetitionUi.emptyState("Aucune donnee pays disponible."));
        }
    }

    private VBox buildBarItem(String title, String meta, double progress) {
        Label topLabel = new Label((title == null ? "-" : title) + "  " + (meta == null ? "" : meta));
        topLabel.getStyleClass().add("list-item-meta");

        ProgressBar bar = new ProgressBar(Math.max(0.0, Math.min(1.0, progress)));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("admin-users-progress");

        VBox box = new VBox(4, topLabel, bar);
        box.getStyleClass().add("admin-users-bar-item");
        return box;
    }

    private void renderRows() {
        usersListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            usersListBox.getChildren().add(CompetitionUi.emptyState("Aucun utilisateur trouve."));
            return;
        }

        for (AdminUserRepository.UserRow row : currentRows) {
            final int userId = row.userId();

            String line1 = "#" + userId
                    + " | " + CompetitionUi.emptySafe(row.username())
                    + " | " + CompetitionUi.emptySafe(row.email());

            String line2 = "Role: " + CompetitionUi.emptySafe(row.role())
                    + " | Actif: " + (row.active() ? "Oui" : "Non")
                    + " | Verifie: " + (row.emailVerified() ? "Oui" : "Non")
                    + " | 2FA: " + (row.twoFactorEnabled() ? "Oui" : "Non")
                    + " | Country: " + CompetitionUi.emptySafe(row.country())
                    + " | Created: " + CompetitionUi.fmtDateTime(row.createdAt())
                    + " | Last login: " + CompetitionUi.fmtDateTime(row.lastLoginAt());

            Button detailButton = new Button("Voir");
            detailButton.getStyleClass().add("btn-ghost");
            detailButton.setOnAction(event -> {
                RouteContext.putInt(RouteContext.KEY_USER_ID, userId);
                Navigator.goTo("admin_user_detail");
            });

            Button editButton = new Button("Update");
            editButton.getStyleClass().add("btn-ghost");
            editButton.setOnAction(event -> {
                RouteContext.putInt(RouteContext.KEY_USER_ID, userId);
                Navigator.goTo("admin_user_edit");
            });

            Button deleteButton = new Button("Delete");
            deleteButton.getStyleClass().add("btn-ghost");
            deleteButton.setOnAction(event -> deleteUser(userId));

            usersListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, detailButton, editButton, deleteButton));
        }
    }

    private void deleteUser(int userId) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        if (!confirmDelete()) {
            return;
        }

        try {
            AdminUserRepository.OperationResult result = repository.deleteUser(userId, admin.getUserId());
            if (!result.ok()) {
                AlertUtils.warning("Admin users", result.message());
                return;
            }
            AlertUtils.info("Admin users", result.message());
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Admin users", "Suppression impossible (relations existantes).\n" + ex.getMessage());
        }
    }

    private boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression utilisateur");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer cet utilisateur ?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void export(boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "Username", "Email", "Role", "Actif", "Email verifie", "Pays", "Creation");
        List<List<String>> rows = new ArrayList<>();

        for (AdminUserRepository.UserRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.userId()),
                    CompetitionUi.emptySafe(row.username()),
                    CompetitionUi.emptySafe(row.email()),
                    CompetitionUi.emptySafe(row.role()),
                    row.active() ? "Oui" : "Non",
                    row.emailVerified() ? "Oui" : "Non",
                    CompetitionUi.emptySafe(row.country()),
                    CompetitionUi.fmtDateTime(row.createdAt())
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Utilisateurs", headers, rows)
                    : exportService.exportExcel("admin_users", headers, rows);
            exportService.openFile(file);
            AlertUtils.info("Export", "Fichier genere: " + file.toAbsolutePath());
        } catch (IOException ex) {
            AlertUtils.error("Export", "Impossible de generer l'export.\n" + ex.getMessage());
        }
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
