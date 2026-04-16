package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.CompetitionRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class FrontOrganizerRegistrationsController implements RouteAwarePage {
    @FXML
    private ComboBox<LookupItem> tournamentCombo;
    @FXML
    private Label countLabel;
    @FXML
    private VBox registrationsListBox;

    private final CompetitionRepository repository = new CompetitionRepository();

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser user = requireOrganizer();
        if (user == null) {
            return;
        }
        loadOrganizerTournaments(user.getUserId());
        refresh();
    }

    @FXML
    private void onTournamentChanged() {
        refresh();
    }

    @FXML
    private void openOrganizerRequests() {
        Navigator.goTo("front_organizer_requests");
    }

    @FXML
    private void openOrganizerRequestCreate() {
        Navigator.goTo("front_organizer_request_create");
    }

    @FXML
    private void openOrganizerMatches() {
        Navigator.goTo("front_organizer_matches");
    }

    @FXML
    private void openOrganizerRegistrations() {
        Navigator.goTo("front_organizer_registrations");
    }

    private void loadOrganizerTournaments(int userId) {
        try {
            List<LookupItem> tournaments = repository.listOrganizerTournaments(userId);
            tournamentCombo.setItems(FXCollections.observableArrayList(tournaments));
            if (!tournaments.isEmpty()) {
                tournamentCombo.getSelectionModel().select(0);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Inscriptions", "Impossible de charger les tournois organisateur.\n" + ex.getMessage());
        }
    }

    private void refresh() {
        SessionUser user = requireOrganizer();
        if (user == null) {
            return;
        }

        LookupItem selectedTournament = tournamentCombo.getValue();
        Integer selectedTournamentId = selectedTournament == null ? null : selectedTournament.getId();

        try {
            List<CompetitionRepository.RegistrationRow> rows = repository.listOrganizerRegistrations(user.getUserId(), selectedTournamentId);
            countLabel.setText(rows.size() + " inscription(s)");
            renderRows(user.getUserId(), selectedTournamentId, rows);
        } catch (SQLException ex) {
            AlertUtils.error("Inscriptions", "Erreur lors du chargement des inscriptions.\n" + ex.getMessage());
        }
    }

    private void renderRows(int organizerUserId, Integer tournamentId, List<CompetitionRepository.RegistrationRow> rows) {
        registrationsListBox.getChildren().clear();
        if (rows.isEmpty()) {
            if (tournamentId == null || tournamentId <= 0) {
                registrationsListBox.getChildren().add(CompetitionUi.emptyState("Aucun tournoi disponible."));
            } else {
                registrationsListBox.getChildren().add(CompetitionUi.emptyState("Aucune inscription pour ce tournoi."));
            }
            return;
        }

        for (CompetitionRepository.RegistrationRow row : rows) {
            String line1 = row.teamName();
            String line2 = "Statut: " + CompetitionUi.emptySafe(row.status())
                    + " | Inscrite le " + CompetitionUi.fmtDateTime(row.registeredAt())
                    + " | Seed: " + (row.seed() == null ? "-" : row.seed())
                    + " | Check-in: " + (row.checkedIn() ? "Oui" : "Non");

            Button acceptButton = new Button("Accepter");
            acceptButton.getStyleClass().add("btn-primary");
            acceptButton.setDisable("ACCEPTED".equalsIgnoreCase(row.status()));
            acceptButton.setOnAction(event -> updateStatus(organizerUserId, row, "ACCEPTED"));

            Button refuseButton = new Button("Refuser");
            refuseButton.getStyleClass().add("btn-ghost");
            refuseButton.setDisable("REFUSED".equalsIgnoreCase(row.status()));
            refuseButton.setOnAction(event -> updateStatus(organizerUserId, row, "REFUSED"));

            registrationsListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, acceptButton, refuseButton));
        }
    }

    private void updateStatus(int organizerUserId, CompetitionRepository.RegistrationRow row, String status) {
        try {
            CompetitionRepository.OperationResult result = repository.updateOrganizerRegistrationStatus(
                    organizerUserId,
                    row.tournamentId(),
                    row.teamId(),
                    status
            );
            if (!result.ok()) {
                AlertUtils.warning("Inscriptions", result.message());
                return;
            }
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Inscriptions", "Impossible de mettre a jour le statut.\n" + ex.getMessage());
        }
    }

    private static SessionUser requireOrganizer() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            AlertUtils.warning("Connexion requise", "Connectez-vous pour gerer les inscriptions.");
            Navigator.goTo("front_login");
            return null;
        }
        String role = SessionContext.currentRole();
        if (!"ORGANIZER".equals(role) && !"ADMIN".equals(role)) {
            AlertUtils.warning("Acces refuse", "Cette page est reservee a l'organisateur.");
            Navigator.goTo("front_home");
            return null;
        }
        return user;
    }
}
