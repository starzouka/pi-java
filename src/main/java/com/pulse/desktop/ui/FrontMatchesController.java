package com.pulse.desktop.ui;

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
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FrontMatchesController implements RouteAwarePage {
    private static final LookupItem ANY = new LookupItem(0, "Tous");

    @FXML
    private TextField qField;
    @FXML
    private ComboBox<LookupItem> tournamentCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private ComboBox<LookupItem> gameCombo;
    @FXML
    private DatePicker dateFromPicker;
    @FXML
    private DatePicker dateToPicker;
    @FXML
    private TextField teamField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private VBox matchesListBox;

    private final CompetitionRepository repository = new CompetitionRepository();
    private final ExportService exportService = new ExportService();
    private List<CompetitionRepository.MatchRow> currentRows = List.of();

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("", "SCHEDULED", "ONGOING", "FINISHED", "CANCELLED"));
        statusCombo.getSelectionModel().select("");
        sortCombo.setItems(FXCollections.observableArrayList("upcoming", "latest"));
        sortCombo.getSelectionModel().select("upcoming");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        loadLookups();
        refresh();
    }

    @FXML
    private void applyFilters() {
        refresh();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        tournamentCombo.getSelectionModel().select(0);
        statusCombo.getSelectionModel().select("");
        gameCombo.getSelectionModel().select(0);
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        teamField.clear();
        sortCombo.getSelectionModel().select("upcoming");
        refresh();
    }

    @FXML
    private void exportPdf() {
        export(true);
    }

    @FXML
    private void exportExcel() {
        export(false);
    }

    private void loadLookups() {
        try {
            List<LookupItem> tournaments = new ArrayList<>();
            tournaments.add(ANY);
            tournaments.addAll(repository.listAllTournaments());
            tournamentCombo.setItems(FXCollections.observableArrayList(tournaments));
            if (tournamentCombo.getSelectionModel().isEmpty()) {
                tournamentCombo.getSelectionModel().select(0);
            }

            List<LookupItem> games = new ArrayList<>();
            games.add(ANY);
            games.addAll(repository.listGames());
            gameCombo.setItems(FXCollections.observableArrayList(games));
            if (gameCombo.getSelectionModel().isEmpty()) {
                gameCombo.getSelectionModel().select(0);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Matchs", "Impossible de charger les filtres.\n" + ex.getMessage());
        }
    }

    private void refresh() {
        try {
            CompetitionRepository.MatchSearchFilter filter = new CompetitionRepository.MatchSearchFilter(
                    qField.getText(),
                    selectedId(tournamentCombo),
                    statusCombo.getValue(),
                    selectedId(gameCombo),
                    dateFromPicker.getValue(),
                    dateToPicker.getValue(),
                    teamField.getText(),
                    sortCombo.getValue()
            );
            currentRows = repository.searchFrontMatches(filter, 250);
            resultCountLabel.setText(currentRows.size() + " match(s) trouve(s)");
            renderRows();
        } catch (SQLException ex) {
            AlertUtils.error("Matchs", "Erreur lors du chargement des matchs.\n" + ex.getMessage());
        }
    }

    private void renderRows() {
        matchesListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            matchesListBox.getChildren().add(CompetitionUi.emptyState("Aucun match ne correspond aux filtres selectionnes."));
            return;
        }

        for (CompetitionRepository.MatchRow row : currentRows) {
            String line1 = row.teamsLabel() + " | " + CompetitionUi.emptySafe(row.tournamentTitle());
            String line2 = CompetitionUi.emptySafe(row.roundName()) + (row.bestOf() == null ? "" : " BO" + row.bestOf())
                    + " | " + CompetitionUi.emptySafe(row.status())
                    + " | " + CompetitionUi.fmtDateTime(row.scheduledAt());

            Button detailButton = new Button("Detail");
            detailButton.getStyleClass().add("btn-ghost");
            detailButton.setOnAction(event -> {
                RouteContext.putInt(RouteContext.KEY_MATCH_ID, row.matchId());
                Navigator.goTo("front_match_detail");
            });

            matchesListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, detailButton));
        }
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
                    ? exportService.exportPdf("Matchs Front", headers, rows)
                    : exportService.exportExcel("matches_front", headers, rows);
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
}
