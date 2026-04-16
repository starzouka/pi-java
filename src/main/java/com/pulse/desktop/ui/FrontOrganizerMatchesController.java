package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.CompetitionRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FrontOrganizerMatchesController implements RouteAwarePage {
    @FXML
    private ComboBox<LookupItem> tournamentCombo;
    @FXML
    private Label matchesCountLabel;
    @FXML
    private VBox matchesListBox;

    @FXML
    private Label formTitleLabel;
    @FXML
    private TextField roundField;
    @FXML
    private DatePicker scheduledDatePicker;
    @FXML
    private TextField scheduledTimeField;
    @FXML
    private ComboBox<String> bestOfCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private VBox participantsBox;
    @FXML
    private Label formFeedbackLabel;
    @FXML
    private Button saveMatchButton;
    @FXML
    private Button cancelEditButton;

    private final CompetitionRepository repository = new CompetitionRepository();
    private final List<ParticipantEditor> participantEditors = new ArrayList<>();
    private Integer editingMatchId;
    private Integer currentOrganizerId;

    @FXML
    public void initialize() {
        bestOfCombo.setItems(FXCollections.observableArrayList("", "1", "3", "5"));
        bestOfCombo.getSelectionModel().select("");

        statusCombo.setItems(FXCollections.observableArrayList("SCHEDULED", "ONGOING", "FINISHED", "CANCELLED"));
        statusCombo.getSelectionModel().select("SCHEDULED");

        scheduledTimeField.setPromptText("HH:mm");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser user = requireOrganizer();
        if (user == null) {
            return;
        }
        currentOrganizerId = user.getUserId();
        loadOrganizerTournaments();
        resetForm();
        refreshMatches();
    }

    @FXML
    private void onTournamentChanged() {
        reloadParticipantEditors(Set.of(), Map.of(), Set.of());
        refreshMatches();
    }

    @FXML
    private void saveMatch() {
        SessionUser user = requireOrganizer();
        if (user == null) {
            return;
        }
        LookupItem tournamentItem = tournamentCombo.getValue();
        if (tournamentItem == null || tournamentItem.getId() <= 0) {
            formFeedbackLabel.setText("Selectionnez un tournoi.");
            return;
        }

        List<CompetitionRepository.MatchParticipantInput> participants = collectParticipants();
        if (participants.size() < 2) {
            formFeedbackLabel.setText("Un match doit avoir au moins deux equipes selectionnees.");
            return;
        }

        LocalDateTime scheduledAt = parseScheduledAt();
        if (scheduledAt == LocalDateTime.MIN) {
            formFeedbackLabel.setText("Heure invalide. Utilisez HH:mm.");
            return;
        }

        Integer bestOf = parseInteger(bestOfCombo.getValue());
        String status = statusCombo.getValue();

        CompetitionRepository.MatchMutationInput input = new CompetitionRepository.MatchMutationInput(
                editingMatchId,
                tournamentItem.getId(),
                roundField.getText(),
                scheduledAt == null ? null : scheduledAt,
                bestOf,
                status,
                participants
        );

        try {
            CompetitionRepository.OperationResult result = repository.upsertMatch(input, user.getUserId(), true);
            formFeedbackLabel.setText(result.message());
            if (!result.ok()) {
                return;
            }
            resetForm();
            refreshMatches();
        } catch (SQLException ex) {
            AlertUtils.error("Gestion matchs", "Impossible d'enregistrer le match.\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancelEdit() {
        resetForm();
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

    private void loadOrganizerTournaments() {
        if (currentOrganizerId == null || currentOrganizerId <= 0) {
            return;
        }
        try {
            List<LookupItem> rows = repository.listOrganizerTournaments(currentOrganizerId);
            tournamentCombo.setItems(FXCollections.observableArrayList(rows));
            if (!rows.isEmpty()) {
                tournamentCombo.getSelectionModel().select(0);
            }
            reloadParticipantEditors(Set.of(), Map.of(), Set.of());
        } catch (SQLException ex) {
            AlertUtils.error("Gestion matchs", "Impossible de charger les tournois.\n" + ex.getMessage());
        }
    }

    private void refreshMatches() {
        if (currentOrganizerId == null || currentOrganizerId <= 0) {
            return;
        }
        LookupItem selectedTournament = tournamentCombo.getValue();
        Integer tournamentId = selectedTournament == null ? null : selectedTournament.getId();
        try {
            List<CompetitionRepository.MatchRow> rows = repository.listOrganizerMatches(currentOrganizerId, tournamentId);
            matchesCountLabel.setText(rows.size() + " match(s)");
            renderMatchRows(rows);
        } catch (SQLException ex) {
            AlertUtils.error("Gestion matchs", "Erreur lors du chargement des matchs.\n" + ex.getMessage());
        }
    }

    private void renderMatchRows(List<CompetitionRepository.MatchRow> rows) {
        matchesListBox.getChildren().clear();
        if (rows.isEmpty()) {
            matchesListBox.getChildren().add(CompetitionUi.emptyState("Aucun match pour ce tournoi."));
            return;
        }

        for (CompetitionRepository.MatchRow row : rows) {
            String line1 = "#" + row.matchId() + " | " + CompetitionUi.emptySafe(row.roundName()) + " | " + row.teamsLabel();
            String line2 = CompetitionUi.emptySafe(row.tournamentTitle())
                    + " | " + CompetitionUi.fmtDateTime(row.scheduledAt())
                    + " | " + CompetitionUi.emptySafe(row.status());

            Button editButton = new Button("Editer");
            editButton.getStyleClass().add("btn-ghost");
            editButton.setOnAction(event -> startEdit(row.matchId()));

            Button deleteButton = new Button("Delete");
            deleteButton.getStyleClass().add("btn-ghost");
            deleteButton.setOnAction(event -> deleteMatch(row.matchId()));

            matchesListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, editButton, deleteButton));
        }
    }

    private void startEdit(int matchId) {
        try {
            CompetitionRepository.MatchRow detail = repository.loadMatchDetail(matchId);
            if (detail == null) {
                AlertUtils.warning("Gestion matchs", "Match introuvable.");
                return;
            }

            editingMatchId = detail.matchId();
            selectTournament(detail.tournamentId());
            roundField.setText(detail.roundName() == null ? "" : detail.roundName());
            if (detail.scheduledAt() == null) {
                scheduledDatePicker.setValue(null);
                scheduledTimeField.clear();
            } else {
                scheduledDatePicker.setValue(detail.scheduledAt().toLocalDate());
                scheduledTimeField.setText(detail.scheduledAt().toLocalTime().toString());
            }
            bestOfCombo.getSelectionModel().select(detail.bestOf() == null ? "" : Integer.toString(detail.bestOf()));
            statusCombo.getSelectionModel().select(detail.status() == null ? "SCHEDULED" : detail.status());

            Set<Integer> selectedTeamIds = new HashSet<>();
            Set<Integer> winnerIds = new HashSet<>();
            Map<Integer, Integer> scoresByTeamId = new HashMap<>();
            for (CompetitionRepository.MatchTeamRow team : detail.teams()) {
                selectedTeamIds.add(team.teamId());
                if (team.score() != null) {
                    scoresByTeamId.put(team.teamId(), team.score());
                }
                if (Boolean.TRUE.equals(team.winner())) {
                    winnerIds.add(team.teamId());
                }
            }

            reloadParticipantEditors(selectedTeamIds, scoresByTeamId, winnerIds);
            formTitleLabel.setText("MODIFIER MATCH #" + matchId);
            saveMatchButton.setText("Mettre a jour");
            cancelEditButton.setVisible(true);
            cancelEditButton.setManaged(true);
        } catch (SQLException ex) {
            AlertUtils.error("Gestion matchs", "Impossible de charger le match a editer.\n" + ex.getMessage());
        }
    }

    private void deleteMatch(int matchId) {
        SessionUser user = requireOrganizer();
        if (user == null) {
            return;
        }
        try {
            CompetitionRepository.OperationResult result = repository.deleteMatch(matchId, user.getUserId(), true);
            if (!result.ok()) {
                AlertUtils.warning("Gestion matchs", result.message());
                return;
            }
            if (editingMatchId != null && editingMatchId == matchId) {
                resetForm();
            }
            refreshMatches();
        } catch (SQLException ex) {
            AlertUtils.error("Gestion matchs", "Impossible de supprimer le match.\n" + ex.getMessage());
        }
    }

    private void selectTournament(int tournamentId) {
        if (tournamentId <= 0) {
            return;
        }
        for (LookupItem item : tournamentCombo.getItems()) {
            if (item.getId() == tournamentId) {
                tournamentCombo.getSelectionModel().select(item);
                return;
            }
        }
    }

    private void resetForm() {
        editingMatchId = null;
        formTitleLabel.setText("CREER MATCH");
        roundField.clear();
        scheduledDatePicker.setValue(null);
        scheduledTimeField.clear();
        bestOfCombo.getSelectionModel().select("");
        statusCombo.getSelectionModel().select("SCHEDULED");
        saveMatchButton.setText("Creer match");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        formFeedbackLabel.setText("");
        reloadParticipantEditors(Set.of(), Map.of(), Set.of());
    }

    private void reloadParticipantEditors(
            Set<Integer> selectedTeamIds,
            Map<Integer, Integer> scoresByTeamId,
            Set<Integer> winnerTeamIds
    ) {
        participantsBox.getChildren().clear();
        participantEditors.clear();

        LookupItem tournament = tournamentCombo.getValue();
        if (tournament == null || tournament.getId() <= 0) {
            participantsBox.getChildren().add(CompetitionUi.emptyState("Selectionnez un tournoi."));
            return;
        }

        try {
            List<LookupItem> acceptedTeams = loadAcceptedTeams(tournament.getId());
            if (acceptedTeams.isEmpty()) {
                participantsBox.getChildren().add(CompetitionUi.emptyState("Aucune equipe acceptee pour ce tournoi."));
                return;
            }

            for (LookupItem team : acceptedTeams) {
                CheckBox includeBox = new CheckBox(team.getLabel());
                includeBox.getStyleClass().add("muted");
                includeBox.setSelected(selectedTeamIds.contains(team.getId()));

                TextField scoreField = new TextField();
                scoreField.getStyleClass().add("input");
                scoreField.setPromptText("Score");
                scoreField.setPrefWidth(90);
                Integer score = scoresByTeamId.get(team.getId());
                if (score != null) {
                    scoreField.setText(Integer.toString(score));
                }

                CheckBox winnerBox = new CheckBox("Winner");
                winnerBox.getStyleClass().add("muted");
                winnerBox.setSelected(winnerTeamIds.contains(team.getId()));

                Runnable refreshState = () -> {
                    boolean enabled = includeBox.isSelected();
                    scoreField.setDisable(!enabled);
                    winnerBox.setDisable(!enabled);
                    if (!enabled) {
                        winnerBox.setSelected(false);
                        scoreField.clear();
                    }
                };
                includeBox.selectedProperty().addListener((obs, old, current) -> refreshState.run());
                refreshState.run();

                HBox row = new HBox(10, includeBox, scoreField, winnerBox);
                row.getStyleClass().add("list-item");
                row.setPrefWidth(Region.USE_COMPUTED_SIZE);
                participantsBox.getChildren().add(row);

                participantEditors.add(new ParticipantEditor(team.getId(), includeBox, scoreField, winnerBox));
            }
        } catch (SQLException ex) {
            participantsBox.getChildren().add(CompetitionUi.emptyState("Erreur chargement participants: " + ex.getMessage()));
        }
    }

    private List<CompetitionRepository.MatchParticipantInput> collectParticipants() {
        List<CompetitionRepository.MatchParticipantInput> participants = new ArrayList<>();
        for (ParticipantEditor editor : participantEditors) {
            if (!editor.includeBox().isSelected()) {
                continue;
            }
            Integer score = parseInteger(editor.scoreField().getText());
            participants.add(new CompetitionRepository.MatchParticipantInput(
                    editor.teamId(),
                    score,
                    editor.winnerBox().isSelected()
            ));
        }
        return participants;
    }

    private LocalDateTime parseScheduledAt() {
        LocalDate date = scheduledDatePicker.getValue();
        if (date == null) {
            return null;
        }
        String timeRaw = scheduledTimeField.getText();
        if (timeRaw == null || timeRaw.isBlank()) {
            return LocalDateTime.of(date, LocalTime.of(0, 0));
        }
        try {
            LocalTime time = LocalTime.parse(timeRaw.trim());
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.MIN;
        }
    }

    private List<LookupItem> loadAcceptedTeams(int tournamentId) throws SQLException {
        String sql = """
                SELECT t.team_id, t.name
                FROM tournament_teams tt
                JOIN teams t ON t.team_id = tt.team_id
                WHERE tt.tournament_id = ? AND tt.status = 'ACCEPTED'
                ORDER BY t.name ASC
                """;
        List<LookupItem> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tournamentId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new LookupItem(rs.getInt("team_id"), rs.getString("name")));
                }
            }
        }
        return rows;
    }

    private static Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static SessionUser requireOrganizer() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            AlertUtils.warning("Connexion requise", "Connectez-vous pour gerer les matchs.");
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

    private record ParticipantEditor(
            int teamId,
            CheckBox includeBox,
            TextField scoreField,
            CheckBox winnerBox
    ) {
    }
}
