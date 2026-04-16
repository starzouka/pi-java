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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FrontCaptainInviteController implements RouteAwarePage {
    @FXML
    private ComboBox<LookupItem> teamSelectorCombo;
    @FXML
    private TextField searchField;
    @FXML
    private Label activeTeamLabel;
    @FXML
    private Label searchCountLabel;
    @FXML
    private Label feedbackLabel;
    @FXML
    private VBox candidatesBox;
    @FXML
    private VBox historyBox;

    private final TeamModuleRepository repository = new TeamModuleRepository();
    private List<TeamModuleRepository.CaptainTeamRow> captainTeams = List.of();
    private TeamModuleRepository.CaptainTeamRow activeTeam;

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
        refreshAll();
    }

    @FXML
    private void searchCandidates() {
        refreshCandidates();
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
    private void goRequests() {
        if (activeTeam != null) {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
        }
        Navigator.goTo("front_captain_requests");
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
            refreshAll();
        } catch (SQLException ex) {
            AlertUtils.error("Invitations", "Chargement impossible.\n" + ex.getMessage());
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

    private void refreshAll() {
        if (activeTeam == null) {
            activeTeamLabel.setText("Aucune equipe active");
            searchCountLabel.setText("0 resultat(s)");
            candidatesBox.getChildren().setAll(CompetitionUi.emptyState("Creez ou selectionnez une equipe."));
            historyBox.getChildren().setAll(CompetitionUi.emptyState("Aucune invitation."));
            return;
        }

        activeTeamLabel.setText(activeTeam.name());
        refreshCandidates();
        refreshHistory();
    }

    private void refreshCandidates() {
        candidatesBox.getChildren().clear();
        if (activeTeam == null) {
            return;
        }
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            List<TeamModuleRepository.InviteCandidateRow> rows = repository.searchInviteCandidates(
                    user.getUserId(),
                    activeTeam.teamId(),
                    searchField.getText(),
                    80
            );

            searchCountLabel.setText(rows.size() + " resultat(s)");
            if (rows.isEmpty()) {
                String empty = (searchField.getText() == null || searchField.getText().isBlank())
                        ? "Lancez une recherche pour inviter un joueur."
                        : "Aucun joueur disponible pour cette recherche.";
                candidatesBox.getChildren().add(CompetitionUi.emptyState(empty));
                return;
            }

            for (TeamModuleRepository.InviteCandidateRow row : rows) {
                candidatesBox.getChildren().add(buildCandidateRow(row));
            }
        } catch (SQLException ex) {
            AlertUtils.error("Invitations", "Recherche impossible.\n" + ex.getMessage());
        }
    }

    private VBox buildCandidateRow(TeamModuleRepository.InviteCandidateRow row) {
        String line = CompetitionUi.emptySafe(row.displayName())
                + "  @" + CompetitionUi.emptySafe(row.username())
                + "  |  " + CompetitionUi.emptySafe(row.role())
                + "  |  " + CompetitionUi.emptySafe(row.country());

        Label identity = new Label(line);
        identity.setWrapText(true);

        TextField messageField = new TextField();
        messageField.getStyleClass().add("input");
        messageField.setPromptText("Message optionnel (auto IA si vide)");
        messageField.setPrefWidth(520);

        Button generateButton = new Button("Generer message IA");
        generateButton.getStyleClass().add("btn-ghost");
        generateButton.setOnAction(event -> {
            String message = repository.buildInviteSuggestion(activeTeam, row);
            messageField.setText(message);
            feedbackLabel.setText("Suggestion IA locale generee.");
        });

        Button inviteButton = new Button("Inviter");
        inviteButton.getStyleClass().add("btn-primary");
        inviteButton.setOnAction(event -> sendInvite(row.userId(), messageField.getText()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox actions = new HBox(8, messageField, generateButton, spacer, inviteButton);

        VBox box = new VBox(8, identity, actions);
        box.getStyleClass().add("list-item");
        return box;
    }

    private void refreshHistory() {
        historyBox.getChildren().clear();
        if (activeTeam == null) {
            return;
        }
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            List<TeamModuleRepository.InviteRow> rows = repository.listLatestInvites(user.getUserId(), activeTeam.teamId(), 200);
            if (rows.isEmpty()) {
                historyBox.getChildren().add(CompetitionUi.emptyState("Aucune invitation pour le moment."));
                return;
            }

            for (TeamModuleRepository.InviteRow row : rows) {
                String left = CompetitionUi.emptySafe(row.displayName())
                        + "  @" + CompetitionUi.emptySafe(row.username())
                        + "  |  " + CompetitionUi.fmtDateTime(row.createdAt());
                String right = CompetitionUi.emptySafe(row.status())
                        + "  |  " + CompetitionUi.emptySafe(row.message());
                historyBox.getChildren().add(CompetitionUi.listRow(left, right));
            }
        } catch (SQLException ex) {
            AlertUtils.error("Invitations", "Impossible de charger l'historique.\n" + ex.getMessage());
        }
    }

    private void sendInvite(int invitedUserId, String message) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null || activeTeam == null) {
            return;
        }
        try {
            TeamModuleRepository.OperationResult result = repository.sendInvite(
                    user.getUserId(),
                    activeTeam.teamId(),
                    invitedUserId,
                    message
            );
            feedbackLabel.setText(result.message());
            if (result.ok()) {
                refreshAll();
            }
        } catch (SQLException ex) {
            AlertUtils.error("Invitations", "Envoi impossible.\n" + ex.getMessage());
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
