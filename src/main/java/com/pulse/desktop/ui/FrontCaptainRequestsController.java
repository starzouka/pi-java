package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.TeamModuleRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FrontCaptainRequestsController implements RouteAwarePage {
    @FXML
    private ComboBox<LookupItem> teamSelectorCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private Label activeTeamLabel;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label feedbackLabel;
    @FXML
    private VBox requestsBox;

    private final TeamModuleRepository repository = new TeamModuleRepository();

    private List<TeamModuleRepository.CaptainTeamRow> captainTeams = List.of();
    private TeamModuleRepository.CaptainTeamRow activeTeam;

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("", "PENDING", "ACCEPTED", "REFUSED", "CANCELLED"));
        statusCombo.getSelectionModel().select("");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser user = requireCaptain();
        if (user == null) {
            return;
        }
        reload(user.getUserId());
    }

    @FXML
    private void onTeamSelectionChanged() {
        LookupItem item = teamSelectorCombo.getValue();
        if (item == null || item.getId() <= 0) {
            return;
        }
        RouteContext.putInt(RouteContext.KEY_TEAM_ID, item.getId());
        activeTeam = findTeam(item.getId());
        refreshRows();
    }

    @FXML
    private void applyFilters() {
        refreshRows();
    }

    @FXML
    private void goTeamManage() {
        if (activeTeam != null) {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
        }
        Navigator.goTo("front_captain_team_manage");
    }

    @FXML
    private void goMembers() {
        if (activeTeam != null) {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
        }
        Navigator.goTo("front_captain_members");
    }

    @FXML
    private void goInvite() {
        if (activeTeam != null) {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
        }
        Navigator.goTo("front_captain_invite");
    }

    @FXML
    private void goProducts() {
        if (activeTeam != null) {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
        }
        Navigator.goTo("front_captain_products");
    }

    @FXML
    private void goOrders() {
        if (activeTeam != null) {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
        }
        Navigator.goTo("front_captain_orders");
    }

    @FXML
    private void goTournaments() {
        if (activeTeam != null) {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
        }
        Navigator.goTo("front_captain_tournaments");
    }

    private void reload(int userId) {
        try {
            captainTeams = repository.listCaptainTeams(userId, 200);
            Integer contextTeamId = RouteContext.getInt(RouteContext.KEY_TEAM_ID);
            if (contextTeamId != null) {
                activeTeam = findTeam(contextTeamId);
            }
            if (activeTeam == null && !captainTeams.isEmpty()) {
                activeTeam = captainTeams.get(0);
            }
            if (activeTeam != null) {
                RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
            }
            renderSelector();
            refreshRows();
        } catch (SQLException ex) {
            AlertUtils.error("Demandes equipe", "Chargement impossible.\n" + ex.getMessage());
        }
    }

    private void renderSelector() {
        List<LookupItem> items = new ArrayList<>();
        for (TeamModuleRepository.CaptainTeamRow row : captainTeams) {
            items.add(new LookupItem(row.teamId(), row.name()));
        }
        teamSelectorCombo.setItems(FXCollections.observableArrayList(items));
        teamSelectorCombo.setDisable(items.isEmpty());
        if (activeTeam == null) {
            teamSelectorCombo.getSelectionModel().clearSelection();
            return;
        }
        for (LookupItem item : items) {
            if (item.getId() == activeTeam.teamId()) {
                teamSelectorCombo.getSelectionModel().select(item);
                return;
            }
        }
    }

    private void refreshRows() {
        requestsBox.getChildren().clear();
        if (activeTeam == null) {
            activeTeamLabel.setText("Aucune equipe active");
            resultCountLabel.setText("0 demande(s)");
            requestsBox.getChildren().add(CompetitionUi.emptyState("Creez ou selectionnez une equipe."));
            return;
        }

        activeTeamLabel.setText(activeTeam.name());
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            List<TeamModuleRepository.JoinRequestRow> rows = repository.listJoinRequests(
                    user.getUserId(),
                    activeTeam.teamId(),
                    statusCombo.getValue(),
                    200
            );

            resultCountLabel.setText(rows.size() + " demande(s)");
            if (rows.isEmpty()) {
                requestsBox.getChildren().add(CompetitionUi.emptyState("Aucune demande disponible."));
                return;
            }

            for (TeamModuleRepository.JoinRequestRow row : rows) {
                requestsBox.getChildren().add(buildRow(row));
            }
        } catch (SQLException ex) {
            AlertUtils.error("Demandes equipe", "Erreur de chargement.\n" + ex.getMessage());
        }
    }

    private VBox buildRow(TeamModuleRepository.JoinRequestRow row) {
        String line1 = CompetitionUi.emptySafe(row.displayName())
                + "  @" + CompetitionUi.emptySafe(row.username())
                + "  |  " + CompetitionUi.fmtDateTime(row.createdAt());
        String line2 = "Note: " + CompetitionUi.emptySafe(row.note())
                + "  |  Statut: " + CompetitionUi.emptySafe(row.status());

        Label top = new Label(line1);
        top.setWrapText(true);
        Label meta = new Label(line2);
        meta.getStyleClass().add("list-item-meta");
        meta.setWrapText(true);

        HBox actions = new HBox(8);
        if ("PENDING".equalsIgnoreCase(row.status())) {
            Button accept = new Button("Accepter");
            accept.getStyleClass().add("btn-primary");
            accept.setOnAction(event -> respond(row.requestId(), "ACCEPTED"));

            Button refuse = new Button("Refuser");
            refuse.getStyleClass().add("btn-ghost");
            refuse.setOnAction(event -> respond(row.requestId(), "REFUSED"));
            actions.getChildren().addAll(accept, refuse);
        }

        VBox box = new VBox(8, top, meta, actions);
        box.getStyleClass().add("list-item");
        return box;
    }

    private void respond(int requestId, String decision) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null || activeTeam == null) {
            return;
        }
        try {
            TeamModuleRepository.OperationResult result = repository.respondJoinRequest(
                    user.getUserId(),
                    activeTeam.teamId(),
                    requestId,
                    decision
            );
            feedbackLabel.setText(result.message());
            if (result.ok()) {
                refreshRows();
            }
        } catch (SQLException ex) {
            AlertUtils.error("Demandes equipe", "Action impossible.\n" + ex.getMessage());
        }
    }

    private TeamModuleRepository.CaptainTeamRow findTeam(int teamId) {
        for (TeamModuleRepository.CaptainTeamRow row : captainTeams) {
            if (row.teamId() == teamId) {
                return row;
            }
        }
        return null;
    }

    private static SessionUser requireCaptain() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return null;
        }
        String role = SessionContext.currentRole();
        if (!"CAPTAIN".equals(role) && !"ADMIN".equals(role)) {
            AlertUtils.warning("Acces refuse", "Cette page est reservee a l'espace capitaine.");
            Navigator.goTo("front_home");
            return null;
        }
        return user;
    }
}
