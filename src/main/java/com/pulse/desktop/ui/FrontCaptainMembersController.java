package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.TeamModuleRepository;
import com.pulse.desktop.service.ExportService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FrontCaptainMembersController implements RouteAwarePage {
    @FXML
    private ComboBox<LookupItem> teamSelectorCombo;
    @FXML
    private Label activeTeamLabel;
    @FXML
    private Label activeCountLabel;
    @FXML
    private Label inactiveCountLabel;
    @FXML
    private Label rosterCaptainLabel;
    @FXML
    private Label rosterCoCaptainLabel;
    @FXML
    private Label rosterStarterLabel;
    @FXML
    private Label rosterSubstituteLabel;
    @FXML
    private Label feedbackLabel;
    @FXML
    private VBox activeMembersBox;
    @FXML
    private VBox inactiveMembersBox;

    private final TeamModuleRepository repository = new TeamModuleRepository();
    private final ExportService exportService = new ExportService();

    private List<TeamModuleRepository.CaptainTeamRow> captainTeams = List.of();
    private TeamModuleRepository.CaptainTeamRow activeTeam;
    private List<TeamModuleRepository.MemberRow> activeMembers = List.of();

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
        refreshContent();
    }

    @FXML
    private void exportRosterPdf() {
        if (activeTeam == null || activeMembers.isEmpty()) {
            AlertUtils.info("Roster", "Aucun membre actif a exporter.");
            return;
        }

        List<String> headers = List.of("Joueur", "Role roster", "Role compte", "Date adhesion");
        List<List<String>> rows = new ArrayList<>();
        for (TeamModuleRepository.MemberRow row : activeMembers) {
            rows.add(List.of(
                    CompetitionUi.emptySafe(row.displayName()) + " (@" + CompetitionUi.emptySafe(row.username()) + ")",
                    CompetitionUi.emptySafe(row.rosterRole()),
                    CompetitionUi.emptySafe(row.accountRole()),
                    CompetitionUi.fmtDateTime(row.joinedAt())
            ));
        }

        try {
            var file = exportService.exportPdf("Roster " + activeTeam.name(), headers, rows);
            exportService.openFile(file);
            feedbackLabel.setText("Fiche roster exportee: " + file.getFileName());
        } catch (IOException ex) {
            AlertUtils.error("Roster", "Export PDF impossible.\n" + ex.getMessage());
        }
    }

    @FXML
    private void goTeamManage() {
        if (activeTeam != null) {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
        }
        Navigator.goTo("front_captain_team_manage");
    }

    @FXML
    private void goRequests() {
        if (activeTeam != null) {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
        }
        Navigator.goTo("front_captain_requests");
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

    private void reload(int captainUserId) {
        try {
            captainTeams = repository.listCaptainTeams(captainUserId, 200);
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
            refreshContent();
        } catch (SQLException ex) {
            AlertUtils.error("Membres", "Chargement impossible.\n" + ex.getMessage());
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

    private void refreshContent() {
        activeMembersBox.getChildren().clear();
        inactiveMembersBox.getChildren().clear();

        if (activeTeam == null) {
            activeTeamLabel.setText("Aucune equipe active");
            activeMembersBox.getChildren().add(CompetitionUi.emptyState("Creez ou selectionnez une equipe."));
            inactiveMembersBox.getChildren().add(CompetitionUi.emptyState("Aucun historique."));
            activeCountLabel.setText("0");
            inactiveCountLabel.setText("0");
            rosterCaptainLabel.setText("0");
            rosterCoCaptainLabel.setText("0");
            rosterStarterLabel.setText("0");
            rosterSubstituteLabel.setText("0");
            return;
        }

        activeTeamLabel.setText(activeTeam.name());
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            activeMembers = repository.listTeamMembers(user.getUserId(), activeTeam.teamId(), true);
            List<TeamModuleRepository.MemberRow> inactiveMembers = repository.listTeamMembers(user.getUserId(), activeTeam.teamId(), false);
            TeamModuleRepository.RosterDistribution distribution = repository.loadRosterDistribution(user.getUserId(), activeTeam.teamId());

            activeCountLabel.setText(Integer.toString(activeMembers.size()));
            inactiveCountLabel.setText(Integer.toString(inactiveMembers.size()));
            rosterCaptainLabel.setText(Integer.toString(distribution.captain()));
            rosterCoCaptainLabel.setText(Integer.toString(distribution.coCaptain()));
            rosterStarterLabel.setText(Integer.toString(distribution.starter()));
            rosterSubstituteLabel.setText(Integer.toString(distribution.substitute()));

            renderActiveMembers(activeMembers);
            renderInactiveMembers(inactiveMembers);
        } catch (SQLException ex) {
            AlertUtils.error("Membres", "Erreur de chargement des membres.\n" + ex.getMessage());
        }
    }

    private void renderActiveMembers(List<TeamModuleRepository.MemberRow> rows) {
        activeMembersBox.getChildren().clear();
        if (rows.isEmpty()) {
            activeMembersBox.getChildren().add(CompetitionUi.emptyState("Aucun membre actif."));
            return;
        }

        for (TeamModuleRepository.MemberRow row : rows) {
            boolean isCaptain = activeTeam != null && row.userId() == activeTeam.captainUserId();

            Label identity = new Label(
                    CompetitionUi.emptySafe(row.displayName())
                            + "  @" + CompetitionUi.emptySafe(row.username())
                            + "  |  role compte: " + CompetitionUi.emptySafe(row.accountRole())
                            + "  |  join: " + CompetitionUi.fmtDateTime(row.joinedAt())
            );
            identity.setWrapText(true);

            ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList("CO_CAPTAIN", "STARTER", "SUBSTITUTE"));
            roleCombo.setPrefWidth(160);
            if (isCaptain) {
                roleCombo.setItems(FXCollections.observableArrayList("CAPTAIN"));
                roleCombo.getSelectionModel().select("CAPTAIN");
                roleCombo.setDisable(true);
            } else {
                String role = row.rosterRole();
                if ("CAPTAIN".equals(role)) {
                    role = "STARTER";
                }
                roleCombo.getSelectionModel().select(role == null || role.isBlank() ? "STARTER" : role);
            }

            Button updateRole = new Button("Maj role");
            updateRole.getStyleClass().add("btn-ghost");
            updateRole.setDisable(isCaptain);
            updateRole.setOnAction(event -> updateRole(row.userId(), roleCombo.getValue()));

            Button remove = new Button("Retirer");
            remove.getStyleClass().add("btn-ghost");
            remove.setDisable(isCaptain);
            remove.setOnAction(event -> removeMember(row.userId()));

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            HBox actions = new HBox(8, roleCombo, updateRole, spacer, remove);

            VBox box = new VBox(8, identity, actions);
            box.getStyleClass().add("list-item");
            activeMembersBox.getChildren().add(box);
        }
    }

    private void renderInactiveMembers(List<TeamModuleRepository.MemberRow> rows) {
        inactiveMembersBox.getChildren().clear();
        if (rows.isEmpty()) {
            inactiveMembersBox.getChildren().add(CompetitionUi.emptyState("Aucun historique de sortie."));
            return;
        }

        for (TeamModuleRepository.MemberRow row : rows) {
            boolean isCaptain = activeTeam != null && row.userId() == activeTeam.captainUserId();

            String line = CompetitionUi.emptySafe(row.displayName())
                    + "  @" + CompetitionUi.emptySafe(row.username())
                    + "  |  role: " + CompetitionUi.emptySafe(row.rosterRole())
                    + "  |  sortie: " + CompetitionUi.fmtDateTime(row.leftAt());

            Button reactivate = new Button("Reactiver");
            reactivate.getStyleClass().add("btn-ghost");
            reactivate.setOnAction(event -> reactivateMember(row.userId()));

            Button removeHistory = new Button("Retirer");
            removeHistory.getStyleClass().add("btn-ghost");
            removeHistory.setDisable(isCaptain);
            removeHistory.setOnAction(event -> removeInactive(row.userId()));

            inactiveMembersBox.getChildren().add(CompetitionUi.listRowWithActions(line, "", reactivate, removeHistory));
        }
    }

    private void updateRole(int userId, String role) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null || activeTeam == null) {
            return;
        }
        try {
            TeamModuleRepository.OperationResult result = repository.updateRosterRole(user.getUserId(), activeTeam.teamId(), userId, role);
            feedbackLabel.setText(result.message());
            if (result.ok()) {
                refreshContent();
            }
        } catch (SQLException ex) {
            AlertUtils.error("Membres", "Mise a jour role impossible.\n" + ex.getMessage());
        }
    }

    private void removeMember(int userId) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null || activeTeam == null) {
            return;
        }
        try {
            TeamModuleRepository.OperationResult result = repository.deactivateMember(user.getUserId(), activeTeam.teamId(), userId);
            feedbackLabel.setText(result.message());
            if (result.ok()) {
                refreshContent();
            }
        } catch (SQLException ex) {
            AlertUtils.error("Membres", "Suppression impossible.\n" + ex.getMessage());
        }
    }

    private void reactivateMember(int userId) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null || activeTeam == null) {
            return;
        }
        try {
            TeamModuleRepository.OperationResult result = repository.reactivateMember(user.getUserId(), activeTeam.teamId(), userId);
            feedbackLabel.setText(result.message());
            if (result.ok()) {
                refreshContent();
            }
        } catch (SQLException ex) {
            AlertUtils.error("Membres", "Reactivation impossible.\n" + ex.getMessage());
        }
    }

    private void removeInactive(int userId) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null || activeTeam == null) {
            return;
        }
        try {
            TeamModuleRepository.OperationResult result = repository.removeInactiveMember(user.getUserId(), activeTeam.teamId(), userId);
            feedbackLabel.setText(result.message());
            if (result.ok()) {
                refreshContent();
            }
        } catch (SQLException ex) {
            AlertUtils.error("Membres", "Suppression historique impossible.\n" + ex.getMessage());
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
