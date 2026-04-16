package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.CompetitionRepository;
import com.pulse.desktop.service.BrowserService;
import com.pulse.desktop.service.ExportService;
import com.pulse.desktop.service.MailService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminTournamentRequestsController implements RouteAwarePage {
    private static final LookupItem ANY = new LookupItem(0, "ALL");
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
    private TextArea adminNoteArea;
    @FXML
    private Label resultCountLabel;
    @FXML
    private VBox requestsListBox;

    private final CompetitionRepository repository = new CompetitionRepository();
    private final ExportService exportService = new ExportService();
    private final MailService mailService = new MailService();
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
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        loadGames();
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

    private void loadGames() {
        try {
            List<LookupItem> games = new ArrayList<>();
            games.add(ANY);
            games.addAll(repository.listGames());
            gameCombo.setItems(FXCollections.observableArrayList(games));
            if (gameCombo.getSelectionModel().isEmpty()) {
                gameCombo.getSelectionModel().select(0);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin demandes", "Impossible de charger les jeux.\n" + ex.getMessage());
        }
    }

    private void refresh() {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        try {
            CompetitionRepository.RequestSearchFilter filter = new CompetitionRepository.RequestSearchFilter(
                    qField.getText(),
                    statusCombo.getValue(),
                    selectedId(gameCombo),
                    sortCombo.getValue()
            );
            currentRows = repository.searchAdminRequests(filter, 700);
            resultCountLabel.setText(currentRows.size() + " resultat(s)");
            renderRows(admin.getUserId());
        } catch (SQLException ex) {
            AlertUtils.error("Admin demandes", "Erreur lors du chargement des demandes.\n" + ex.getMessage());
        }
    }

    private void renderRows(int adminUserId) {
        requestsListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            requestsListBox.getChildren().add(CompetitionUi.emptyState("Aucune demande trouvee."));
            return;
        }

        for (CompetitionRepository.RequestRow row : currentRows) {
            String line1 = "#" + row.requestId() + " | " + CompetitionUi.emptySafe(row.title())
                    + " | Org: " + CompetitionUi.emptySafe(row.organizerName());
            String line2 = CompetitionUi.emptySafe(row.gameName())
                    + " | " + CompetitionUi.fmtDate(row.startDate()) + " -> " + CompetitionUi.fmtDate(row.endDate())
                    + " | Prize: " + (row.prizePool() == null ? "0" : row.prizePool().toPlainString()) + " DT"
                    + " | " + CompetitionUi.emptySafe(row.status());

            Button detailButton = new Button("Voir detail");
            detailButton.getStyleClass().add("btn-ghost");
            detailButton.setOnAction(event -> {
                String url = AppConfig.webBaseUrl() + "/admin/tournament-requests/" + row.requestId();
                BrowserService.openUrl(url);
            });

            if ("PENDING".equalsIgnoreCase(row.status())) {
                Button acceptButton = new Button("Accepter");
                acceptButton.getStyleClass().add("btn-primary");
                acceptButton.setOnAction(event -> review(adminUserId, row.requestId(), "ACCEPTED"));

                Button refuseButton = new Button("Refuser");
                refuseButton.getStyleClass().add("btn-ghost");
                refuseButton.setOnAction(event -> review(adminUserId, row.requestId(), "REFUSED"));

                requestsListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, detailButton, acceptButton, refuseButton));
            } else {
                requestsListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, detailButton));
            }
        }
    }

    private void review(int adminUserId, int requestId, String decision) {
        String note = adminNoteArea.getText();
        try {
            CompetitionRepository.ReviewResult result = repository.reviewTournamentRequest(requestId, decision, note, adminUserId);
            if (!result.ok()) {
                AlertUtils.warning("Review demande", result.message());
                return;
            }
            sendReviewEmailIfPossible(result);
            AlertUtils.info("Review demande", result.message());
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Review demande", "Erreur SQL.\n" + ex.getMessage());
        }
    }

    private void sendReviewEmailIfPossible(CompetitionRepository.ReviewResult result) {
        if (result == null || result.organizerEmail() == null || result.organizerEmail().isBlank()) {
            return;
        }
        if (!mailService.isConfigured()) {
            return;
        }
        try {
            mailService.sendTournamentRequestDecision(result.organizerEmail(), result.requestTitle(), result.decision());
        } catch (Exception ex) {
            AlertUtils.warning("Email", "Decision enregistree, mais email non envoye.\n" + ex.getMessage());
        }
    }

    private void export(boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "Titre", "Organisateur", "Jeu", "Start date", "End date", "Status", "Prize pool", "Created at");
        List<List<String>> rows = new ArrayList<>();
        for (CompetitionRepository.RequestRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.requestId()),
                    CompetitionUi.emptySafe(row.title()),
                    CompetitionUi.emptySafe(row.organizerName()),
                    CompetitionUi.emptySafe(row.gameName()),
                    CompetitionUi.fmtDate(row.startDate()),
                    CompetitionUi.fmtDate(row.endDate()),
                    CompetitionUi.emptySafe(row.status()),
                    row.prizePool() == null ? "0" : row.prizePool().toPlainString(),
                    CompetitionUi.fmtDateTime(row.createdAt())
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Admin demandes tournois", headers, rows)
                    : exportService.exportExcel("admin_tournament_requests", headers, rows);
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
}
