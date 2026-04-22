package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FrontOrganizerRequestsController implements RouteAwarePage {
    private static final LookupItem ANY = new LookupItem(0, "Tous");
    private static final List<String> STATUS_OPTIONS = List.of("", "PENDING", "ACCEPTED", "REFUSED");
    private static final List<String> SORT_OPTIONS = List.of("latest", "oldest", "title", "prize", "status");

    @FXML
    private TextField qField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private ComboBox<LookupItem> gameCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label countLabel;
    @FXML
    private VBox requestsListBox;

    private final CompetitionRepository repository = new CompetitionRepository();
    private final ExportService exportService = new ExportService();
    private List<CompetitionRepository.RequestRow> currentRows = List.of();

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList(STATUS_OPTIONS));
        statusCombo.getSelectionModel().select("");

        sortCombo.setItems(FXCollections.observableArrayList(SORT_OPTIONS));
        sortCombo.getSelectionModel().select("latest");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser user = requireOrganizer();
        if (user == null) {
            return;
        }
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
        statusCombo.getSelectionModel().select("");
        sortCombo.getSelectionModel().select("latest");
        if (gameCombo.getItems().isEmpty()) {
            loadLookups();
        }
        gameCombo.getSelectionModel().select(0);
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

    @FXML
    private void openCreateRequest() {
        Navigator.goTo("front_organizer_request_create");
    }

    @FXML
    private void openOrganizerRequests() {
        Navigator.goTo("front_organizer_requests");
    }

    @FXML
    private void openOrganizerMatches() {
        Navigator.goTo("front_organizer_matches");
    }

    @FXML
    private void openOrganizerRegistrations() {
        Navigator.goTo("front_organizer_registrations");
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
        } catch (SQLException ex) {
            AlertUtils.error("Demandes organisateur", "Impossible de charger les jeux.\n" + ex.getMessage());
        }
    }

    private void refresh() {
        SessionUser user = requireOrganizer();
        if (user == null) {
            return;
        }
        try {
            CompetitionRepository.RequestSearchFilter filter = new CompetitionRepository.RequestSearchFilter(
                    qField.getText(),
                    statusCombo.getValue(),
                    selectedId(gameCombo),
                    sortCombo.getValue()
            );
            currentRows = repository.searchOrganizerRequests(user.getUserId(), filter, 600);
            countLabel.setText(currentRows.size() + " demande(s)");
            renderRows();
        } catch (SQLException ex) {
            AlertUtils.error("Demandes organisateur", "Erreur lors du chargement des demandes.\n" + ex.getMessage());
        }
    }

    private void renderRows() {
        requestsListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            requestsListBox.getChildren().add(CompetitionUi.emptyState("Aucune demande pour le moment."));
            return;
        }

        for (CompetitionRepository.RequestRow row : currentRows) {
            String line1 = "#" + row.requestId() + " | " + CompetitionUi.emptySafe(row.title());
            String line2 = CompetitionUi.emptySafe(row.gameName())
                    + " | " + CompetitionUi.fmtDate(row.startDate()) + " - " + CompetitionUi.fmtDate(row.endDate())
                    + " | " + CompetitionUi.emptySafe(row.status())
                    + " | Cree le " + CompetitionUi.fmtDateTime(row.createdAt());

            Button detailButton = new Button("Voir");
            detailButton.getStyleClass().add("btn-ghost");
            detailButton.setOnAction(event -> {
                RouteContext.putInt(RouteContext.KEY_REQUEST_ID, row.requestId());
                Navigator.goTo("front_organizer_request_detail");
            });

            requestsListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, detailButton));
        }
    }

    private void export(boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "Titre", "Jeu", "Status", "Start date", "End date", "Prize pool", "Created at");
        List<List<String>> rows = new ArrayList<>();
        for (CompetitionRepository.RequestRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.requestId()),
                    CompetitionUi.emptySafe(row.title()),
                    CompetitionUi.emptySafe(row.gameName()),
                    CompetitionUi.emptySafe(row.status()),
                    CompetitionUi.fmtDate(row.startDate()),
                    CompetitionUi.fmtDate(row.endDate()),
                    row.prizePool() == null ? "0" : row.prizePool().toPlainString(),
                    CompetitionUi.fmtDateTime(row.createdAt())
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Demandes organisateur", headers, rows)
                    : exportService.exportExcel("front_organizer_requests", headers, rows);
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

    private static SessionUser requireOrganizer() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            AlertUtils.warning("Connexion requise", "Connectez-vous pour acceder aux demandes organisateur.");
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
