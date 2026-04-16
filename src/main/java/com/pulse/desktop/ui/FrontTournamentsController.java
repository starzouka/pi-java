package com.pulse.desktop.ui;

import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.CompetitionRepository;
import com.pulse.desktop.service.ExportService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FrontTournamentsController implements RouteAwarePage {
    private static final LookupItem ANY = new LookupItem(0, "Tous");

    @FXML
    private TextField qField;
    @FXML
    private ComboBox<LookupItem> gameCombo;
    @FXML
    private ComboBox<LookupItem> categoryCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private ComboBox<String> formatCombo;
    @FXML
    private ComboBox<String> registrationModeCombo;
    @FXML
    private DatePicker dateFromPicker;
    @FXML
    private DatePicker dateToPicker;
    @FXML
    private TextField prizeMinField;
    @FXML
    private TextField prizeMaxField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private FlowPane cardsPane;

    private final CompetitionRepository repository = new CompetitionRepository();
    private final ExportService exportService = new ExportService();
    private List<CompetitionRepository.TournamentCatalogRow> currentRows = List.of();

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("", "DRAFT", "OPEN", "ONGOING", "FINISHED", "CANCELLED"));
        statusCombo.getSelectionModel().select("");

        formatCombo.setItems(FXCollections.observableArrayList("", "BO1", "BO3", "BO5"));
        formatCombo.getSelectionModel().select("");

        registrationModeCombo.setItems(FXCollections.observableArrayList("", "OPEN", "APPROVAL"));
        registrationModeCombo.getSelectionModel().select("");

        sortCombo.setItems(FXCollections.observableArrayList("latest", "prize", "progress", "oldest"));
        sortCombo.getSelectionModel().select("latest");
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
        gameCombo.getSelectionModel().select(0);
        categoryCombo.getSelectionModel().select(0);
        statusCombo.getSelectionModel().select("");
        formatCombo.getSelectionModel().select("");
        registrationModeCombo.getSelectionModel().select("");
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        prizeMinField.clear();
        prizeMaxField.clear();
        sortCombo.getSelectionModel().select("latest");
        refresh();
    }

    @FXML
    private void exportPdf() {
        export("tournaments_front", true);
    }

    @FXML
    private void exportExcel() {
        export("tournaments_front", false);
    }

    private void loadLookups() {
        try {
            List<LookupItem> games = new ArrayList<>();
            games.add(ANY);
            games.addAll(repository.listGames());
            gameCombo.setItems(FXCollections.observableArrayList(games));
            if (gameCombo.getSelectionModel().isEmpty()) {
                gameCombo.getSelectionModel().select(0);
            }

            List<LookupItem> categories = new ArrayList<>();
            categories.add(ANY);
            categories.addAll(repository.listCategories());
            categoryCombo.setItems(FXCollections.observableArrayList(categories));
            if (categoryCombo.getSelectionModel().isEmpty()) {
                categoryCombo.getSelectionModel().select(0);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Tournois", "Impossible de charger les filtres.\n" + ex.getMessage());
        }
    }

    private void refresh() {
        try {
            CompetitionRepository.TournamentSearchFilter filter = new CompetitionRepository.TournamentSearchFilter(
                    qField.getText(),
                    selectedId(gameCombo),
                    selectedId(categoryCombo),
                    statusCombo.getValue(),
                    formatCombo.getValue(),
                    registrationModeCombo.getValue(),
                    dateFromPicker.getValue(),
                    dateToPicker.getValue(),
                    parseDecimal(prizeMinField.getText()),
                    parseDecimal(prizeMaxField.getText()),
                    sortCombo.getValue()
            );

            currentRows = repository.searchTournamentCatalog(filter, 200);
            resultCountLabel.setText(currentRows.size() + " tournoi(s) trouves");
            renderCards();
        } catch (SQLException ex) {
            AlertUtils.error("Tournois", "Erreur lors du chargement des tournois.\n" + ex.getMessage());
        }
    }

    private void renderCards() {
        cardsPane.getChildren().clear();
        if (currentRows.isEmpty()) {
            cardsPane.getChildren().add(CompetitionUi.emptyState("Aucun tournoi ne correspond aux filtres selectionnes."));
            return;
        }

        for (CompetitionRepository.TournamentCatalogRow row : currentRows) {
            VBox card = new VBox(8);
            card.getStyleClass().add("card");
            card.setPrefWidth(360);

            VBox media = new VBox();
            media.setPrefHeight(110);
            media.getStyleClass().add("card__media");
            String bg = ImageResolver.toBackgroundStyle(row.photoPath());
            if (!bg.isBlank()) {
                media.setStyle(bg);
            }

            HBox chips = new HBox(6,
                    makeChip(row.status(), "chip chip--status"),
                    makeChip(row.format(), "chip chip--format"),
                    makeChip(row.gameName(), "chip")
            );
            chips.getStyleClass().add("card__chips");
            media.getChildren().add(chips);

            VBox body = new VBox(6);
            body.getStyleClass().add("card__body");

            Label title = new Label(CompetitionUi.emptySafe(row.title()));
            title.getStyleClass().add("card__title");

            Label desc = new Label("Dates: " + CompetitionUi.fmtDate(row.startDate()) + " - " + CompetitionUi.fmtDate(row.endDate()));
            desc.getStyleClass().add("card__desc");
            desc.setWrapText(true);

            Label stats1 = new Label("Prize pool: " + row.prizePool() + " DT");
            stats1.getStyleClass().add("muted");
            Label stats2 = new Label("Equipes: " + row.acceptedCount() + "/" + row.maxTeams() + " | Matchs: " + row.matchesFinished() + "/" + row.matchesTotal());
            stats2.getStyleClass().add("muted");

            Button detailButton = new Button("Voir detail");
            detailButton.getStyleClass().add("btn-ghost");
            detailButton.setOnAction(event -> {
                RouteContext.putInt(RouteContext.KEY_TOURNAMENT_ID, row.tournamentId());
                Navigator.goTo("front_tournament_detail");
            });

            body.getChildren().addAll(title, desc, stats1, stats2, detailButton);
            card.getChildren().addAll(media, body);
            cardsPane.getChildren().add(card);
        }
    }

    private void export(String baseName, boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "Titre", "Jeu", "Categorie", "Status", "Format", "Inscription", "Start date", "End date", "Prize pool");
        List<List<String>> rows = new ArrayList<>();
        for (CompetitionRepository.TournamentCatalogRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.tournamentId()),
                    CompetitionUi.emptySafe(row.title()),
                    CompetitionUi.emptySafe(row.gameName()),
                    CompetitionUi.emptySafe(row.categoryName()),
                    CompetitionUi.emptySafe(row.status()),
                    CompetitionUi.emptySafe(row.format()),
                    CompetitionUi.emptySafe(row.registrationMode()),
                    CompetitionUi.fmtDate(row.startDate()),
                    CompetitionUi.fmtDate(row.endDate()),
                    row.prizePool() == null ? "0" : row.prizePool().toPlainString()
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Tournois Front", headers, rows)
                    : exportService.exportExcel(baseName, headers, rows);
            exportService.openFile(file);
            AlertUtils.info("Export", "Fichier genere: " + file.toAbsolutePath());
        } catch (IOException ex) {
            AlertUtils.error("Export", "Impossible de generer l'export.\n" + ex.getMessage());
        }
    }

    private static Label makeChip(String text, String styleClass) {
        Label label = new Label(CompetitionUi.emptySafe(text));
        for (String cls : styleClass.split(" ")) {
            if (!cls.isBlank()) {
                label.getStyleClass().add(cls);
            }
        }
        return label;
    }

    private static Integer selectedId(ComboBox<LookupItem> combo) {
        LookupItem item = combo.getValue();
        if (item == null || item.getId() <= 0) {
            return null;
        }
        return item.getId();
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
