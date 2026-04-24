package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.CompetitionRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FrontTournamentDetailController implements RouteAwarePage {
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Label progressLabel;
    @FXML
    private Label registrationsLabel;
    @FXML
    private Label feedbackLabel;

    @FXML
    private ComboBox<LookupItem> teamCombo;
    @FXML
    private Button participateButton;

    @FXML
    private Button tabOverviewButton;
    @FXML
    private Button tabScoresButton;
    @FXML
    private Button tabMatchesButton;
    @FXML
    private Button tabTeamsButton;

    @FXML
    private VBox overviewPane;
    @FXML
    private VBox scoresPane;
    @FXML
    private VBox matchesPane;
    @FXML
    private VBox teamsPane;

    @FXML
    private VBox overviewListBox;
    @FXML
    private VBox scoresListBox;
    @FXML
    private VBox matchesListBox;
    @FXML
    private VBox teamsListBox;
    @FXML
    private VBox coverPane;

    private final CompetitionRepository repository = new CompetitionRepository();
    private CompetitionRepository.TournamentCatalogRow tournament;

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        loadData();
        switchTab("overview");
    }

    @FXML
    private void openTabOverview() {
        switchTab("overview");
    }

    @FXML
    private void openTabScores() {
        switchTab("scores");
    }

    @FXML
    private void openTabMatches() {
        switchTab("matches");
    }

    @FXML
    private void openTabTeams() {
        switchTab("teams");
    }

    @FXML
    private void participate() {
        SessionUser current = SessionContext.getCurrentUser();
        if (current == null) {
            Navigator.goTo("front_login");
            return;
        }
        if (tournament == null) {
            return;
        }

        LookupItem selectedTeam = teamCombo.getValue();
        if (selectedTeam == null || selectedTeam.getId() <= 0) {
            feedbackLabel.setText("Selectionnez une equipe.");
            return;
        }

        try {
            CompetitionRepository.OperationResult result = repository.registerTeamToTournament(
                    tournament.tournamentId(),
                    selectedTeam.getId(),
                    current.getUserId()
            );
            feedbackLabel.setText(result.message());
            if (!result.ok()) {
                return;
            }
            loadData();
            switchTab("teams");
        } catch (SQLException ex) {
            AlertUtils.error("Tournoi", "Impossible de participer au tournoi.\n" + ex.getMessage());
        }
    }

    private void loadData() {
        Integer selectedTournamentId = RouteContext.getInt(RouteContext.KEY_TOURNAMENT_ID);
        try {
            tournament = repository.loadTournamentDetail(selectedTournamentId);
            if (tournament == null) {
                feedbackLabel.setText("Aucun tournoi disponible.");
                return;
            }

            titleLabel.setText(CompetitionUi.emptySafe(tournament.title()));
            subtitleLabel.setText(
                    "Organisateur: " + CompetitionUi.emptySafe(tournament.organizerName())
                            + " | Jeu: " + CompetitionUi.emptySafe(tournament.gameName())
                            + " | Format: " + CompetitionUi.emptySafe(tournament.format())
            );
            String coverStyle = ImageResolver.toBackgroundStyle(tournament.photoPath());
            if (coverStyle != null && !coverStyle.isBlank()) {
                coverPane.setStyle(coverStyle);
            } else {
                coverPane.setStyle("");
            }
            progressLabel.setText(tournament.matchesFinished() + " match(s) termine(s) / " + tournament.matchesTotal());
            registrationsLabel.setText("Equipes acceptees: " + tournament.acceptedCount() + "/" + tournament.maxTeams()
                    + " | Total inscriptions: " + tournament.registeredCount());

            renderOverview();
            renderScoreboard();
            renderMatches();
            renderTeams();
            loadParticipationChoices();
        } catch (SQLException ex) {
            AlertUtils.error("Tournoi", "Impossible de charger le detail du tournoi.\n" + ex.getMessage());
        }
    }

    private void renderOverview() {
        overviewListBox.getChildren().clear();
        if (tournament == null) {
            overviewListBox.getChildren().add(CompetitionUi.emptyState("Tournoi introuvable."));
            return;
        }

        overviewListBox.getChildren().addAll(
                CompetitionUi.listRow("Description", CompetitionUi.emptySafe(tournament.description())),
                CompetitionUi.listRow("Regles", CompetitionUi.emptySafe(tournament.rules())),
                CompetitionUi.listRow("Recompense", CompetitionUi.emptySafe(tournament.prizeDescription())),
                CompetitionUi.listRow("Dates", CompetitionUi.fmtDate(tournament.startDate()) + " - " + CompetitionUi.fmtDate(tournament.endDate())),
                CompetitionUi.listRow("Deadline", CompetitionUi.fmtDate(tournament.registrationDeadline())),
                CompetitionUi.listRow("Prize pool", (tournament.prizePool() == null ? "0" : tournament.prizePool().toPlainString()) + " DT"),
                CompetitionUi.listRow("Statut", CompetitionUi.emptySafe(tournament.status())),
                CompetitionUi.listRow("Mode inscription", CompetitionUi.emptySafe(tournament.registrationMode()))
        );
    }

    private void renderScoreboard() throws SQLException {
        scoresListBox.getChildren().clear();
        if (tournament == null) {
            return;
        }

        List<CompetitionRepository.ScoreboardRow> rows = repository.listTournamentScoreboard(tournament.tournamentId());
        if (rows.isEmpty()) {
            scoresListBox.getChildren().add(CompetitionUi.emptyState("Aucun score disponible pour le moment."));
            return;
        }

        for (CompetitionRepository.ScoreboardRow row : rows) {
            scoresListBox.getChildren().add(CompetitionUi.listRow(
                    row.teamName(),
                    "MJ " + row.played() + " | V " + row.wins() + " | D " + row.losses() + " | PTS " + row.points()
            ));
        }
    }

    private void renderMatches() throws SQLException {
        matchesListBox.getChildren().clear();
        if (tournament == null) {
            return;
        }

        List<CompetitionRepository.MatchRow> matches = repository.listTournamentMatches(tournament.tournamentId());
        if (matches.isEmpty()) {
            matchesListBox.getChildren().add(CompetitionUi.emptyState("Aucun match pour ce tournoi."));
            return;
        }

        addMatchSection("Matchs termines", matches, "FINISHED");
        addMatchSection("Matchs en cours", matches, "ONGOING");
        addMatchSection("Matchs a venir", matches, "SCHEDULED");
    }

    private void addMatchSection(String title, List<CompetitionRepository.MatchRow> matches, String status) {
        Label section = new Label(title);
        section.getStyleClass().add("panel-title");
        matchesListBox.getChildren().add(section);

        int added = 0;
        for (CompetitionRepository.MatchRow match : matches) {
            if (!status.equalsIgnoreCase(match.status())) {
                continue;
            }
            Button detailButton = new Button("Detail match");
            detailButton.getStyleClass().add("btn-ghost");
            detailButton.setOnAction(event -> {
                RouteContext.putInt(RouteContext.KEY_MATCH_ID, match.matchId());
                Navigator.goTo("front_match_detail");
            });

            matchesListBox.getChildren().add(CompetitionUi.listRowWithActions(
                    CompetitionUi.emptySafe(match.roundName()) + (match.bestOf() == null ? "" : " BO" + match.bestOf()),
                    match.teamsLabel() + " | " + CompetitionUi.fmtDateTime(match.scheduledAt()),
                    detailButton
            ));
            added++;
        }

        if (added == 0) {
            matchesListBox.getChildren().add(CompetitionUi.emptyState("Aucun resultat."));
        }
    }

    private void renderTeams() throws SQLException {
        teamsListBox.getChildren().clear();
        if (tournament == null) {
            return;
        }

        List<CompetitionRepository.ParticipantRow> participants = repository.listTournamentParticipants(tournament.tournamentId());
        if (participants.isEmpty()) {
            teamsListBox.getChildren().add(CompetitionUi.emptyState("Aucune equipe inscrite pour le moment."));
            return;
        }

        for (CompetitionRepository.ParticipantRow participant : participants) {
            teamsListBox.getChildren().add(CompetitionUi.listRow(
                    participant.teamName(),
                    participant.status() + " | " + CompetitionUi.fmtDateTime(participant.registeredAt())
            ));
        }
    }

    private void loadParticipationChoices() throws SQLException {
        SessionUser current = SessionContext.getCurrentUser();
        if (current == null || tournament == null) {
            teamCombo.setItems(FXCollections.observableArrayList());
            participateButton.setDisable(false);
            participateButton.setText("Se connecter pour participer");
            participateButton.setOnAction(event -> Navigator.goTo("front_login"));
            return;
        }

        List<CompetitionRepository.TeamRegistrationOption> teams = repository.listCaptainTeamsForTournament(current.getUserId(), tournament.tournamentId());
        List<LookupItem> options = new ArrayList<>();
        for (CompetitionRepository.TeamRegistrationOption team : teams) {
            String label = team.teamName();
            if (team.status() != null && !team.status().isBlank()) {
                label += " (" + team.status() + ")";
            }
            options.add(new LookupItem(team.teamId(), label));
        }
        teamCombo.setItems(FXCollections.observableArrayList(options));
        if (!options.isEmpty()) {
            teamCombo.getSelectionModel().select(0);
        }

        participateButton.setDisable(options.isEmpty());
        participateButton.setText(options.isEmpty() ? "Aucune equipe capitaine" : "Participer");
        participateButton.setOnAction(event -> participate());
    }

    private void switchTab(String tab) {
        boolean overview = "overview".equals(tab);
        boolean scores = "scores".equals(tab);
        boolean matches = "matches".equals(tab);
        boolean teams = "teams".equals(tab);

        setTabActive(tabOverviewButton, overview);
        setTabActive(tabScoresButton, scores);
        setTabActive(tabMatchesButton, matches);
        setTabActive(tabTeamsButton, teams);

        overviewPane.setManaged(overview);
        overviewPane.setVisible(overview);

        scoresPane.setManaged(scores);
        scoresPane.setVisible(scores);

        matchesPane.setManaged(matches);
        matchesPane.setVisible(matches);

        teamsPane.setManaged(teams);
        teamsPane.setVisible(teams);
    }

    private static void setTabActive(Button button, boolean active) {
        button.getStyleClass().remove("tab--active");
        if (active) {
            button.getStyleClass().add("tab--active");
        }
    }
}
