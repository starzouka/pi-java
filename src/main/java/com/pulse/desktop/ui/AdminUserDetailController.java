package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.AdminUserRepository;
import com.pulse.desktop.service.BrowserService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class AdminUserDetailController implements RouteAwarePage {
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;

    @FXML
    private Button tabProfileButton;
    @FXML
    private Button tabTeamsButton;
    @FXML
    private Button tabPostsButton;
    @FXML
    private Button tabOrdersButton;
    @FXML
    private Button tabReportsButton;

    @FXML
    private VBox profilePane;
    @FXML
    private VBox teamsPane;
    @FXML
    private VBox postsPane;
    @FXML
    private VBox ordersPane;
    @FXML
    private VBox reportsPane;

    @FXML
    private VBox profileListBox;
    @FXML
    private ComboBox<String> teamsSortCombo;
    @FXML
    private ComboBox<String> teamsDirectionCombo;
    @FXML
    private VBox teamsListBox;
    @FXML
    private ComboBox<String> postsSortCombo;
    @FXML
    private ComboBox<String> postsDirectionCombo;
    @FXML
    private VBox postsListBox;
    @FXML
    private ComboBox<String> ordersSortCombo;
    @FXML
    private ComboBox<String> ordersDirectionCombo;
    @FXML
    private VBox ordersListBox;
    @FXML
    private ComboBox<String> reportsSortCombo;
    @FXML
    private ComboBox<String> reportsDirectionCombo;
    @FXML
    private VBox reportsListBox;

    private final AdminUserRepository repository = new AdminUserRepository();
    private Integer currentUserId;
    private AdminUserRepository.UserDetail currentUser;

    @FXML
    public void initialize() {
        teamsSortCombo.setItems(FXCollections.observableArrayList("joined_at", "team_id", "is_active"));
        teamsSortCombo.getSelectionModel().select("joined_at");
        teamsDirectionCombo.setItems(FXCollections.observableArrayList("desc", "asc"));
        teamsDirectionCombo.getSelectionModel().select("desc");

        postsSortCombo.setItems(FXCollections.observableArrayList("created_at", "id", "content", "visibility", "deleted"));
        postsSortCombo.getSelectionModel().select("created_at");
        postsDirectionCombo.setItems(FXCollections.observableArrayList("desc", "asc"));
        postsDirectionCombo.getSelectionModel().select("desc");

        ordersSortCombo.setItems(FXCollections.observableArrayList("created_at", "order_number", "status", "total_amount"));
        ordersSortCombo.getSelectionModel().select("created_at");
        ordersDirectionCombo.setItems(FXCollections.observableArrayList("desc", "asc"));
        ordersDirectionCombo.getSelectionModel().select("desc");

        reportsSortCombo.setItems(FXCollections.observableArrayList("created_at", "id", "target", "target_id", "status"));
        reportsSortCombo.getSelectionModel().select("created_at");
        reportsDirectionCombo.setItems(FXCollections.observableArrayList("desc", "asc"));
        reportsDirectionCombo.getSelectionModel().select("desc");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }

        Integer selectedUserId = RouteContext.getInt(RouteContext.KEY_USER_ID);
        if (selectedUserId == null || selectedUserId <= 0) {
            AlertUtils.warning("Admin user detail", "Aucun utilisateur selectionne.");
            Navigator.goTo("admin_users");
            return;
        }

        this.currentUserId = selectedUserId;
        loadAll();
        switchTab("profile");
    }

    @FXML
    private void goBackList() {
        Navigator.goTo("admin_users");
    }

    @FXML
    private void openEdit() {
        if (currentUserId == null || currentUserId <= 0) {
            return;
        }
        RouteContext.putInt(RouteContext.KEY_USER_ID, currentUserId);
        Navigator.goTo("admin_user_edit");
    }

    @FXML
    private void openTabProfile() {
        switchTab("profile");
    }

    @FXML
    private void openTabTeams() {
        switchTab("teams");
    }

    @FXML
    private void openTabPosts() {
        switchTab("posts");
    }

    @FXML
    private void openTabOrders() {
        switchTab("orders");
    }

    @FXML
    private void openTabReports() {
        switchTab("reports");
    }

    @FXML
    private void reloadTeams() {
        loadTeams();
    }

    @FXML
    private void reloadPosts() {
        loadPosts();
    }

    @FXML
    private void reloadOrders() {
        loadOrders();
    }

    @FXML
    private void reloadReports() {
        loadReports();
    }

    private void loadAll() {
        try {
            currentUser = repository.loadUserDetail(currentUserId);
            if (currentUser == null) {
                AlertUtils.warning("Admin user detail", "Utilisateur introuvable.");
                Navigator.goTo("admin_users");
                return;
            }
            titleLabel.setText("Detail utilisateur #" + currentUser.userId());
            subtitleLabel.setText("Profil, equipes, activite, commerce, moderation.");
            renderProfile();
            loadTeams();
            loadPosts();
            loadOrders();
            loadReports();
        } catch (SQLException ex) {
            AlertUtils.error("Admin user detail", "Impossible de charger le detail utilisateur.\n" + ex.getMessage());
        }
    }

    private void renderProfile() {
        profileListBox.getChildren().clear();
        if (currentUser == null) {
            profileListBox.getChildren().add(CompetitionUi.emptyState("Utilisateur introuvable."));
            return;
        }

        profileListBox.getChildren().addAll(
                CompetitionUi.listRow("user_id", String.valueOf(currentUser.userId())),
                CompetitionUi.listRow("username", CompetitionUi.emptySafe(currentUser.username())),
                CompetitionUi.listRow("email", CompetitionUi.emptySafe(currentUser.email())),
                CompetitionUi.listRow("display_name", CompetitionUi.emptySafe(currentUser.displayName())),
                CompetitionUi.listRow("role", CompetitionUi.emptySafe(currentUser.role())),
                CompetitionUi.listRow("country", CompetitionUi.emptySafe(currentUser.country())),
                CompetitionUi.listRow("birth_date", CompetitionUi.fmtDate(currentUser.birthDate())),
                CompetitionUi.listRow("gender", CompetitionUi.emptySafe(currentUser.gender())),
                CompetitionUi.listRow("email_verified", currentUser.emailVerified() ? "Oui" : "Non"),
                CompetitionUi.listRow("is_active", currentUser.active() ? "Oui" : "Non"),
                CompetitionUi.listRow("two_factor", currentUser.twoFactorEnabled() ? "Oui" : "Non"),
                CompetitionUi.listRow("created_at", CompetitionUi.fmtDateTime(currentUser.createdAt())),
                CompetitionUi.listRow("updated_at", CompetitionUi.fmtDateTime(currentUser.updatedAt())),
                CompetitionUi.listRow("last_login_at", CompetitionUi.fmtDateTime(currentUser.lastLoginAt())),
                CompetitionUi.listRow("bio", CompetitionUi.emptySafe(currentUser.bio())),
                CompetitionUi.listRow("phone", CompetitionUi.emptySafe(currentUser.phone())),
                CompetitionUi.listRow("profile_image", CompetitionUi.emptySafe(currentUser.profileImagePath()))
        );
    }

    private void loadTeams() {
        teamsListBox.getChildren().clear();
        if (currentUserId == null || currentUserId <= 0) {
            return;
        }
        try {
            List<AdminUserRepository.TeamMembershipRow> rows = repository.listTeamMemberships(
                    currentUserId,
                    teamsSortCombo.getValue(),
                    teamsDirectionCombo.getValue(),
                    300
            );
            if (rows.isEmpty()) {
                teamsListBox.getChildren().add(CompetitionUi.emptyState("Aucune equipe."));
                return;
            }
            for (AdminUserRepository.TeamMembershipRow row : rows) {
                teamsListBox.getChildren().add(CompetitionUi.listRow(
                        CompetitionUi.emptySafe(row.teamName()),
                        "joined_at: " + CompetitionUi.fmtDateTime(row.joinedAt()) + " | actif: " + (row.active() ? "Oui" : "Non")
                ));
            }
        } catch (SQLException ex) {
            teamsListBox.getChildren().add(CompetitionUi.emptyState("Erreur equipes: " + ex.getMessage()));
        }
    }

    private void loadPosts() {
        postsListBox.getChildren().clear();
        if (currentUserId == null || currentUserId <= 0) {
            return;
        }
        try {
            List<AdminUserRepository.UserPostRow> rows = repository.listPosts(
                    currentUserId,
                    postsSortCombo.getValue(),
                    postsDirectionCombo.getValue(),
                    300
            );
            if (rows.isEmpty()) {
                postsListBox.getChildren().add(CompetitionUi.emptyState("Aucun post."));
                return;
            }
            for (AdminUserRepository.UserPostRow row : rows) {
                String line1 = CompetitionUi.emptySafe(shorten(row.content(), 80));
                String line2 = CompetitionUi.emptySafe(row.visibility())
                        + " | deleted: " + (row.deleted() ? "Oui" : "Non")
                        + " | " + CompetitionUi.fmtDateTime(row.createdAt());

                Button viewButton = new Button("Voir");
                viewButton.getStyleClass().add("btn-ghost");
                viewButton.setOnAction(event -> BrowserService.openUrl(AppConfig.webBaseUrl() + "/admin/post-detail/" + row.postId()));

                postsListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, viewButton));
            }
        } catch (SQLException ex) {
            postsListBox.getChildren().add(CompetitionUi.emptyState("Erreur posts: " + ex.getMessage()));
        }
    }

    private void loadOrders() {
        ordersListBox.getChildren().clear();
        if (currentUserId == null || currentUserId <= 0) {
            return;
        }
        try {
            List<AdminUserRepository.UserOrderRow> rows = repository.listOrders(
                    currentUserId,
                    ordersSortCombo.getValue(),
                    ordersDirectionCombo.getValue(),
                    300
            );
            if (rows.isEmpty()) {
                ordersListBox.getChildren().add(CompetitionUi.emptyState("Aucune commande."));
                return;
            }
            for (AdminUserRepository.UserOrderRow row : rows) {
                String line1 = CompetitionUi.emptySafe(row.orderNumber());
                String line2 = CompetitionUi.emptySafe(row.status())
                        + " | total: " + (row.totalAmount() == null ? "0" : row.totalAmount().toPlainString()) + " DT"
                        + " | " + CompetitionUi.fmtDateTime(row.createdAt());
                ordersListBox.getChildren().add(CompetitionUi.listRow(line1, line2));
            }
        } catch (SQLException ex) {
            ordersListBox.getChildren().add(CompetitionUi.emptyState("Erreur commandes: " + ex.getMessage()));
        }
    }

    private void loadReports() {
        reportsListBox.getChildren().clear();
        if (currentUserId == null || currentUserId <= 0) {
            return;
        }
        try {
            List<AdminUserRepository.UserReportRow> rows = repository.listReports(
                    currentUserId,
                    reportsSortCombo.getValue(),
                    reportsDirectionCombo.getValue(),
                    300
            );
            if (rows.isEmpty()) {
                reportsListBox.getChildren().add(CompetitionUi.emptyState("Aucun signalement."));
                return;
            }
            for (AdminUserRepository.UserReportRow row : rows) {
                String line1 = "#" + row.reportId() + " | " + CompetitionUi.emptySafe(row.targetType()) + " / " + CompetitionUi.emptySafe(row.targetId());
                String line2 = CompetitionUi.emptySafe(row.status()) + " | " + CompetitionUi.fmtDateTime(row.createdAt());

                Button viewButton = new Button("Voir");
                viewButton.getStyleClass().add("btn-ghost");
                viewButton.setOnAction(event -> BrowserService.openUrl(AppConfig.webBaseUrl() + "/admin/report-detail/" + row.reportId()));

                reportsListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, viewButton));
            }
        } catch (SQLException ex) {
            reportsListBox.getChildren().add(CompetitionUi.emptyState("Erreur signalements: " + ex.getMessage()));
        }
    }

    private void switchTab(String tab) {
        boolean profile = "profile".equals(tab);
        boolean teams = "teams".equals(tab);
        boolean posts = "posts".equals(tab);
        boolean orders = "orders".equals(tab);
        boolean reports = "reports".equals(tab);

        setTabActive(tabProfileButton, profile);
        setTabActive(tabTeamsButton, teams);
        setTabActive(tabPostsButton, posts);
        setTabActive(tabOrdersButton, orders);
        setTabActive(tabReportsButton, reports);

        profilePane.setManaged(profile);
        profilePane.setVisible(profile);

        teamsPane.setManaged(teams);
        teamsPane.setVisible(teams);

        postsPane.setManaged(posts);
        postsPane.setVisible(posts);

        ordersPane.setManaged(orders);
        ordersPane.setVisible(orders);

        reportsPane.setManaged(reports);
        reportsPane.setVisible(reports);
    }

    private static void setTabActive(Button button, boolean active) {
        button.getStyleClass().remove("tab--active");
        if (active) {
            button.getStyleClass().add("tab--active");
        }
    }

    private static String shorten(String value, int max) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, max - 3)) + "...";
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
