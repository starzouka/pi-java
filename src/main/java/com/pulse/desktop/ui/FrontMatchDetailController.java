package com.pulse.desktop.ui;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.CompetitionRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FrontMatchDetailController implements RouteAwarePage {
    @FXML
    private Label matchTitleLabel;
    @FXML
    private Label matchSummaryLabel;
    @FXML
    private VBox teamsListBox;
    @FXML
    private Label statusBadgeLabel;
    @FXML
    private Label hourValueLabel;
    @FXML
    private Label gameValueLabel;
    @FXML
    private Label categoryValueLabel;
    @FXML
    private Label submittedByValueLabel;
    @FXML
    private Label matchIdValueLabel;
    @FXML
    private Label winnersValueLabel;
    @FXML
    private Button openTournamentButton;

    private final CompetitionRepository repository = new CompetitionRepository();
    private CompetitionRepository.MatchRow currentMatch;

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        load();
    }

    @FXML
    private void goBackMatches() {
        Navigator.goTo("front_matches");
    }

    @FXML
    private void openTournamentDetail() {
        if (currentMatch == null || currentMatch.tournamentId() <= 0) {
            return;
        }
        RouteContext.putInt(RouteContext.KEY_TOURNAMENT_ID, currentMatch.tournamentId());
        Navigator.goTo("front_tournament_detail");
    }

    private void load() {
        Integer selectedMatchId = RouteContext.getInt(RouteContext.KEY_MATCH_ID);
        try {
            currentMatch = repository.loadMatchDetail(selectedMatchId);
            if (currentMatch == null) {
                matchTitleLabel.setText("Match introuvable");
                matchSummaryLabel.setText("Aucun match disponible.");
                teamsListBox.getChildren().setAll(CompetitionUi.emptyState("Aucune equipe renseignee pour ce match."));
                fillInfosFallback();
                openTournamentButton.setDisable(true);
                return;
            }

            matchTitleLabel.setText(currentMatch.teamsLabel());
            String summary = "Tournoi: " + CompetitionUi.emptySafe(currentMatch.tournamentTitle())
                    + " | Round: " + CompetitionUi.emptySafe(currentMatch.roundName())
                    + (currentMatch.bestOf() == null ? "" : " | BO" + currentMatch.bestOf());
            matchSummaryLabel.setText(summary);

            renderTeams();
            renderInfos();
        } catch (SQLException ex) {
            AlertUtils.error("Detail match", "Impossible de charger le detail du match.\n" + ex.getMessage());
        }
    }

    private void renderTeams() {
        teamsListBox.getChildren().clear();
        if (currentMatch == null || currentMatch.teams().isEmpty()) {
            teamsListBox.getChildren().add(CompetitionUi.emptyState("Aucune equipe renseignee pour ce match."));
            winnersValueLabel.setText("-");
            return;
        }

        List<String> winners = new ArrayList<>();
        for (CompetitionRepository.MatchTeamRow team : currentMatch.teams()) {
            String teamName = CompetitionUi.emptySafe(team.teamName());
            String score = team.score() == null ? "-" : Integer.toString(team.score());
            String right = "Score: " + score;

            if (Boolean.TRUE.equals(team.winner())) {
                right += " | WINNER";
                winners.add(teamName);
            }
            teamsListBox.getChildren().add(CompetitionUi.listRow(teamName, right));
        }
        winnersValueLabel.setText(winners.isEmpty() ? "-" : String.join(", ", winners));
    }

    private void renderInfos() throws SQLException {
        if (currentMatch == null) {
            fillInfosFallback();
            return;
        }

        String status = CompetitionUi.emptySafe(currentMatch.status());
        statusBadgeLabel.setText(status);
        applyStatusBadge(status);

        hourValueLabel.setText(CompetitionUi.fmtDateTime(currentMatch.scheduledAt()));
        gameValueLabel.setText(CompetitionUi.emptySafe(currentMatch.gameName()));
        categoryValueLabel.setText(resolveCategoryLabel(currentMatch.matchId()));
        submittedByValueLabel.setText(CompetitionUi.emptySafe(currentMatch.resultSubmittedByName()));
        matchIdValueLabel.setText("#" + currentMatch.matchId());
        openTournamentButton.setDisable(currentMatch.tournamentId() <= 0);
    }

    private void fillInfosFallback() {
        statusBadgeLabel.setText("-");
        applyStatusBadge("");
        hourValueLabel.setText("-");
        gameValueLabel.setText("-");
        categoryValueLabel.setText("-");
        submittedByValueLabel.setText("-");
        matchIdValueLabel.setText("-");
        winnersValueLabel.setText("-");
    }

    private void applyStatusBadge(String status) {
        statusBadgeLabel.getStyleClass().removeAll("badge--success", "badge--warning", "badge--danger", "badge--info");
        String normalized = status == null ? "" : status.trim().toUpperCase();
        String variant = switch (normalized) {
            case "FINISHED" -> "badge--success";
            case "ONGOING" -> "badge--warning";
            case "CANCELLED" -> "badge--danger";
            default -> "badge--info";
        };
        if (!statusBadgeLabel.getStyleClass().contains("badge")) {
            statusBadgeLabel.getStyleClass().add("badge");
        }
        statusBadgeLabel.getStyleClass().add(variant);
    }

    private String resolveCategoryLabel(int matchId) throws SQLException {
        if (matchId <= 0) {
            return "-";
        }

        String sql = """
                SELECT COALESCE(c.name, '-') AS category_name
                FROM matches m
                JOIN tournaments t ON t.tournament_id = m.tournament_id
                JOIN games g ON g.game_id = t.game_id
                LEFT JOIN categories c ON c.category_id = g.category_id
                WHERE m.match_id = ?
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, matchId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return CompetitionUi.emptySafe(rs.getString("category_name"));
                }
            }
        }
        return "-";
    }
}
