package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.CompetitionRepository;
import com.pulse.desktop.service.ExportService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
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

public class AdminMatchesController implements RouteAwarePage {
    private static final LookupItem ANY = new LookupItem(0, "ALL");

    @FXML
    private Label formTitleLabel;
    @FXML
    private ScrollPane pageScroll;
    @FXML
    private ComboBox<LookupItem> formTournamentCombo;
    @FXML
    private TextField roundField;
    @FXML
    private DatePicker scheduledDatePicker;
    @FXML
    private TextField scheduledTimeField;
    @FXML
    private ComboBox<String> bestOfCombo;
    @FXML
    private ComboBox<String> formStatusCombo;
    @FXML
    private VBox participantsBox;
    @FXML
    private Label formFeedbackLabel;
    @FXML
    private Button saveMatchButton;
    @FXML
    private Button cancelEditButton;

    @FXML
    private TextField qField;
    @FXML
    private ComboBox<LookupItem> filterTournamentCombo;
    @FXML
    private ComboBox<String> filterStatusCombo;
    @FXML
    private ComboBox<LookupItem> filterGameCombo;
    @FXML
    private DatePicker filterDateFromPicker;
    @FXML
    private DatePicker filterDateToPicker;
    @FXML
    private TextField filterTeamField;
    @FXML
    private ComboBox<String> filterSortCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private VBox matchesListBox;

    private final CompetitionRepository repository = new CompetitionRepository();
    private final ExportService exportService = new ExportService();
    private final List<ParticipantEditor> participantEditors = new ArrayList<>();
    private List<CompetitionRepository.MatchRow> currentRows = List.of();
    private Integer editingMatchId;

    @FXML
    public void initialize() {
        bestOfCombo.setItems(FXCollections.observableArrayList("", "1", "3", "5"));
        bestOfCombo.getSelectionModel().select("");

        formStatusCombo.setItems(FXCollections.observableArrayList("SCHEDULED", "ONGOING", "FINISHED", "CANCELLED"));
        formStatusCombo.getSelectionModel().select("SCHEDULED");

        filterStatusCombo.setItems(FXCollections.observableArrayList("", "SCHEDULED", "ONGOING", "FINISHED", "CANCELLED"));
        filterStatusCombo.getSelectionModel().select("");

        filterSortCombo.setItems(FXCollections.observableArrayList("latest", "oldest", "status", "tournament"));
        filterSortCombo.getSelectionModel().select("latest");

        scheduledTimeField.setPromptText("HH:mm");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        loadLookups();
        resetForm();
        refreshMatches();
    }

    @FXML
    private void onFormTournamentChanged() {
        reloadParticipantEditors(Set.of(), Map.of(), Set.of());
    }

    @FXML
    private void saveMatch() {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        boolean updating = editingMatchId != null;
        LookupItem tournament = formTournamentCombo.getValue();
        if (tournament == null || tournament.getId() <= 0) {
            formFeedbackLabel.setText("Selectionnez un tournoi.");
            return;
        }

        List<CompetitionRepository.MatchParticipantInput> participants = collectParticipants();
        if (participants.size() < 2) {
            formFeedbackLabel.setText("Selectionnez au moins 2 equipes.");
            return;
        }

        LocalDateTime scheduledAt = parseScheduledAt();
        if (scheduledAt == LocalDateTime.MIN) {
            formFeedbackLabel.setText("Heure invalide (HH:mm).");
            return;
        }

        CompetitionRepository.MatchMutationInput input = new CompetitionRepository.MatchMutationInput(
                editingMatchId,
                tournament.getId(),
                roundField.getText(),
                scheduledAt == null ? null : scheduledAt,
                parseInteger(bestOfCombo.getValue()),
                formStatusCombo.getValue(),
                participants
        );

        try {
            CompetitionRepository.OperationResult result = repository.upsertMatch(input, admin.getUserId(), false);
            formFeedbackLabel.setText(result.message());
            if (!result.ok()) {
                return;
            }
            resetForm();
            refreshMatches();
            AlertUtils.info("Admin matchs", updating ? "Match mis a jour avec succes." : "Match cree avec succes.");
        } catch (SQLException ex) {
            AlertUtils.error("Admin matchs", "Erreur enregistrement match.\n" + ex.getMessage());
        } catch (Exception ex) {
            AlertUtils.error("Admin matchs", "Erreur inattendue lors de l'enregistrement du match.\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancelEdit() {
        resetForm();
    }

    @FXML
    private void applyFilters() {
        refreshMatches();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        filterTournamentCombo.getSelectionModel().select(0);
        filterStatusCombo.getSelectionModel().select("");
        filterGameCombo.getSelectionModel().select(0);
        filterDateFromPicker.setValue(null);
        filterDateToPicker.setValue(null);
        filterTeamField.clear();
        filterSortCombo.getSelectionModel().select("latest");
        refreshMatches();
    }

    @FXML
    private void exportPdf() {
        export(true);
    }

    @FXML
    private void exportExcel() {
        export(false);
    }

    @FXML
    private void startCreate() {
        resetForm();
        formFeedbackLabel.setText("Mode ajout actif.");
        if (pageScroll != null) {
            pageScroll.setVvalue(0.0);
        }
        roundField.requestFocus();
    }

    private void loadLookups() {
        try {
            List<LookupItem> tournaments = new ArrayList<>();
            tournaments.add(ANY);
            tournaments.addAll(repository.listAllTournaments());
            filterTournamentCombo.setItems(FXCollections.observableArrayList(tournaments));
            filterTournamentCombo.getSelectionModel().select(0);

            List<LookupItem> formTournaments = new ArrayList<>(tournaments);
            formTournaments.remove(0);
            formTournamentCombo.setItems(FXCollections.observableArrayList(formTournaments));
            if (!formTournaments.isEmpty()) {
                formTournamentCombo.getSelectionModel().select(0);
            }

            List<LookupItem> games = new ArrayList<>();
            games.add(ANY);
            games.addAll(repository.listGames());
            filterGameCombo.setItems(FXCollections.observableArrayList(games));
            filterGameCombo.getSelectionModel().select(0);

            reloadParticipantEditors(Set.of(), Map.of(), Set.of());
        } catch (SQLException ex) {
            AlertUtils.error("Admin matchs", "Impossible de charger les listes.\n" + ex.getMessage());
        }
    }

    private void refreshMatches() {
        try {
            CompetitionRepository.MatchSearchFilter filter = new CompetitionRepository.MatchSearchFilter(
                    qField.getText(),
                    selectedId(filterTournamentCombo),
                    filterStatusCombo.getValue(),
                    selectedId(filterGameCombo),
                    filterDateFromPicker.getValue(),
                    filterDateToPicker.getValue(),
                    filterTeamField.getText(),
                    filterSortCombo.getValue()
            );
            currentRows = repository.searchAdminMatches(filter, 700);
            resultCountLabel.setText(currentRows.size() + " resultat(s)");
            renderRows();
        } catch (SQLException ex) {
            AlertUtils.error("Admin matchs", "Erreur chargement matchs.\n" + ex.getMessage());
        }
    }

    private void renderRows() {
        matchesListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            matchesListBox.getChildren().add(CompetitionUi.emptyState("Aucun match trouve."));
            return;
        }

        for (CompetitionRepository.MatchRow row : currentRows) {
            final int matchId = row.matchId();
            String line1 = "#" + row.matchId() + " | " + CompetitionUi.emptySafe(row.tournamentTitle())
                    + " | " + CompetitionUi.emptySafe(row.roundName())
                    + (row.bestOf() == null ? "" : " BO" + row.bestOf());
            String line2 = row.teamsLabel()
                    + " | " + CompetitionUi.fmtDateTime(row.scheduledAt())
                    + " | " + CompetitionUi.emptySafe(row.status())
                    + " | " + CompetitionUi.emptySafe(row.gameName());

            Button viewButton = new Button("Voir");
            viewButton.getStyleClass().add("btn-ghost");
            viewButton.setOnAction(event -> {
                RouteContext.putInt(RouteContext.KEY_MATCH_ID, matchId);
                Navigator.goTo("front_match_detail");
            });

            Button editButton = new Button("Modifier");
            editButton.getStyleClass().add("btn-ghost");
            editButton.setOnAction(event -> startEdit(matchId));

            Button deleteButton = new Button("Supprimer");
            deleteButton.getStyleClass().add("btn-ghost");
            deleteButton.setOnAction(event -> deleteMatch(matchId));

            matchesListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, viewButton, editButton, deleteButton));
        }
    }

    private void startEdit(int matchId) {
        try {
            CompetitionRepository.MatchRow detail = repository.loadMatchDetail(matchId);
            if (detail == null) {
                AlertUtils.warning("Admin matchs", "Match introuvable.");
                return;
            }
            editingMatchId = detail.matchId();
            formTitleLabel.setText("MODIFIER MATCH #" + matchId);
            saveMatchButton.setText("Mettre a jour");
            cancelEditButton.setVisible(true);
            cancelEditButton.setManaged(true);

            selectById(formTournamentCombo, detail.tournamentId());
            roundField.setText(detail.roundName() == null ? "" : detail.roundName());
            if (detail.scheduledAt() == null) {
                scheduledDatePicker.setValue(null);
                scheduledTimeField.clear();
            } else {
                scheduledDatePicker.setValue(detail.scheduledAt().toLocalDate());
                scheduledTimeField.setText(detail.scheduledAt().toLocalTime().toString());
            }
            bestOfCombo.getSelectionModel().select(detail.bestOf() == null ? "" : Integer.toString(detail.bestOf()));
            formStatusCombo.getSelectionModel().select(detail.status() == null ? "SCHEDULED" : detail.status());

            Set<Integer> selectedIds = new HashSet<>();
            Set<Integer> winnerIds = new HashSet<>();
            Map<Integer, Integer> scores = new HashMap<>();
            for (CompetitionRepository.MatchTeamRow team : detail.teams()) {
                selectedIds.add(team.teamId());
                if (team.score() != null) {
                    scores.put(team.teamId(), team.score());
                }
                if (Boolean.TRUE.equals(team.winner())) {
                    winnerIds.add(team.teamId());
                }
            }
            reloadParticipantEditors(selectedIds, scores, winnerIds);
            formFeedbackLabel.setText("Mode modification actif pour le match #" + matchId + ".");
            if (pageScroll != null) {
                pageScroll.setVvalue(0.0);
            }
            roundField.requestFocus();
        } catch (SQLException ex) {
            AlertUtils.error("Admin matchs", "Impossible de charger le match.\n" + ex.getMessage());
        } catch (Exception ex) {
            AlertUtils.error("Admin matchs", "Erreur inattendue lors du chargement du match.\n" + ex.getMessage());
        }
    }

    private void deleteMatch(int matchId) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        try {
            CompetitionRepository.OperationResult result = repository.deleteMatch(matchId, admin.getUserId(), false);
            if (!result.ok()) {
                AlertUtils.warning("Admin matchs", result.message());
                return;
            }
            if (editingMatchId != null && editingMatchId == matchId) {
                resetForm();
            }
            refreshMatches();
            AlertUtils.info("Admin matchs", "Match supprime avec succes.");
        } catch (SQLException ex) {
            AlertUtils.error("Admin matchs", "Suppression impossible.\n" + ex.getMessage());
        } catch (Exception ex) {
            AlertUtils.error("Admin matchs", "Erreur inattendue lors de la suppression.\n" + ex.getMessage());
        }
    }

    private void resetForm() {
        editingMatchId = null;
        formTitleLabel.setText("CREER MATCH");
        saveMatchButton.setText("Creer match");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);

        roundField.clear();
        scheduledDatePicker.setValue(null);
        scheduledTimeField.clear();
        bestOfCombo.getSelectionModel().select("");
        formStatusCombo.getSelectionModel().select("SCHEDULED");
        formFeedbackLabel.setText("");

        reloadParticipantEditors(Set.of(), Map.of(), Set.of());
    }

    private void reloadParticipantEditors(Set<Integer> selectedTeamIds, Map<Integer, Integer> scoresByTeamId, Set<Integer> winnerIds) {
        participantsBox.getChildren().clear();
        participantEditors.clear();
        LookupItem tournament = formTournamentCombo.getValue();
        if (tournament == null || tournament.getId() <= 0) {
            participantsBox.getChildren().add(CompetitionUi.emptyState("Selectionnez un tournoi."));
            return;
        }

        try {
            List<LookupItem> teams = loadAcceptedTeams(tournament.getId());
            if (teams.isEmpty()) {
                participantsBox.getChildren().add(CompetitionUi.emptyState("Aucune equipe acceptee pour ce tournoi."));
                return;
            }

            for (LookupItem team : teams) {
                CheckBox include = new CheckBox(team.getLabel());
                include.setSelected(selectedTeamIds.contains(team.getId()));

                TextField scoreField = new TextField();
                scoreField.getStyleClass().add("input");
                scoreField.setPromptText("Score");
                scoreField.setPrefWidth(90);
                Integer score = scoresByTeamId.get(team.getId());
                if (score != null) {
                    scoreField.setText(Integer.toString(score));
                }

                CheckBox winner = new CheckBox("Winner");
                winner.setSelected(winnerIds.contains(team.getId()));

                Runnable refreshState = () -> {
                    boolean enabled = include.isSelected();
                    scoreField.setDisable(!enabled);
                    winner.setDisable(!enabled);
                    if (!enabled) {
                        winner.setSelected(false);
                        scoreField.clear();
                    }
                };
                include.selectedProperty().addListener((obs, oldValue, selected) -> refreshState.run());
                refreshState.run();

                HBox row = new HBox(10, include, scoreField, winner);
                row.getStyleClass().add("list-item");
                row.setPrefWidth(Region.USE_COMPUTED_SIZE);
                participantsBox.getChildren().add(row);

                participantEditors.add(new ParticipantEditor(team.getId(), include, scoreField, winner));
            }
        } catch (SQLException ex) {
            participantsBox.getChildren().add(CompetitionUi.emptyState("Erreur participants: " + ex.getMessage()));
        }
    }

    private List<CompetitionRepository.MatchParticipantInput> collectParticipants() {
        List<CompetitionRepository.MatchParticipantInput> participants = new ArrayList<>();
        for (ParticipantEditor editor : participantEditors) {
            if (!editor.include().isSelected()) {
                continue;
            }
            participants.add(new CompetitionRepository.MatchParticipantInput(
                    editor.teamId(),
                    parseInteger(editor.scoreField().getText()),
                    editor.winner().isSelected()
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
            return LocalDateTime.of(date, LocalTime.parse(timeRaw.trim()));
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

    private void export(boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "Tournoi", "Jeu", "Round", "Participants", "Horaire", "Status");
        List<List<String>> rows = new ArrayList<>();
        for (CompetitionRepository.MatchRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.matchId()),
                    CompetitionUi.emptySafe(row.tournamentTitle()),
                    CompetitionUi.emptySafe(row.gameName()),
                    CompetitionUi.emptySafe(row.roundName()) + (row.bestOf() == null ? "" : " BO" + row.bestOf()),
                    row.teamsLabel(),
                    CompetitionUi.fmtDateTime(row.scheduledAt()),
                    CompetitionUi.emptySafe(row.status())
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Admin matchs", headers, rows)
                    : exportService.exportExcel("admin_matches", headers, rows);
            exportService.openFile(file);
            AlertUtils.info("Export", "Fichier genere: " + file.toAbsolutePath());
        } catch (IOException ex) {
            AlertUtils.error("Export", "Impossible de generer l'export.\n" + ex.getMessage());
        }
    }

    private static Integer selectedId(ComboBox<LookupItem> combo) {
        LookupItem item = combo.getValue();
        if (item == null || item.getId() <= 0) {
            return null;
        }
        return item.getId();
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

    private static void selectById(ComboBox<LookupItem> combo, int id) {
        for (LookupItem item : combo.getItems()) {
            if (item.getId() == id) {
                combo.getSelectionModel().select(item);
                return;
            }
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

    private record ParticipantEditor(int teamId, CheckBox include, TextField scoreField, CheckBox winner) {
    }
}
