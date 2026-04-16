package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.config.AppConfig;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AdminTournamentsController implements RouteAwarePage {
    private static final LookupItem ANY = new LookupItem(0, "ALL");
    private static final List<String> STATUS_OPTIONS = List.of("", "DRAFT", "OPEN", "ONGOING", "FINISHED", "CANCELLED");
    private static final List<String> SORT_OPTIONS = List.of("latest", "oldest", "title", "prize", "status");

    @FXML
    private Label formTitleLabel;
    @FXML
    private ScrollPane pageScroll;
    @FXML
    private ComboBox<LookupItem> organizerCombo;
    @FXML
    private ComboBox<LookupItem> formGameCombo;
    @FXML
    private TextField titleField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private DatePicker deadlineDatePicker;
    @FXML
    private TextField maxTeamsField;
    @FXML
    private ComboBox<String> formatCombo;
    @FXML
    private ComboBox<String> registrationModeCombo;
    @FXML
    private TextField prizePoolField;
    @FXML
    private ComboBox<String> formStatusCombo;
    @FXML
    private TextArea prizeDescriptionArea;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextArea rulesArea;
    @FXML
    private TextField photoPathField;
    @FXML
    private Label formFeedbackLabel;
    @FXML
    private Button saveTournamentButton;
    @FXML
    private Button cancelEditButton;

    @FXML
    private TextField qField;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private ComboBox<LookupItem> gameFilterCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private javafx.scene.layout.VBox tournamentsListBox;

    private final CompetitionRepository repository = new CompetitionRepository();
    private final ExportService exportService = new ExportService();
    private Integer editingTournamentId;
    private Path selectedPhotoFile;
    private List<CompetitionRepository.TournamentCatalogRow> currentRows = List.of();

    @FXML
    public void initialize() {
        formatCombo.setItems(FXCollections.observableArrayList("BO1", "BO3", "BO5"));
        formatCombo.getSelectionModel().select("BO1");

        registrationModeCombo.setItems(FXCollections.observableArrayList("OPEN", "APPROVAL"));
        registrationModeCombo.getSelectionModel().select("OPEN");

        formStatusCombo.setItems(FXCollections.observableArrayList("DRAFT", "OPEN", "ONGOING", "FINISHED", "CANCELLED"));
        formStatusCombo.getSelectionModel().select("OPEN");

        statusFilterCombo.setItems(FXCollections.observableArrayList(STATUS_OPTIONS));
        statusFilterCombo.getSelectionModel().select("");

        sortCombo.setItems(FXCollections.observableArrayList(SORT_OPTIONS));
        sortCombo.getSelectionModel().select("latest");

        maxTeamsField.setText("16");
        prizePoolField.setText("0");
        photoPathField.setEditable(false);
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        loadLookups();
        resetForm();
        refresh();
    }

    @FXML
    private void choosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Photo tournoi");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        Window owner = photoPathField.getScene() == null ? null : photoPathField.getScene().getWindow();
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        selectedPhotoFile = file.toPath();
        photoPathField.setText(file.getAbsolutePath());
    }

    @FXML
    private void saveTournament() {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }

        LookupItem organizer = organizerCombo.getValue();
        LookupItem game = formGameCombo.getValue();
        if (organizer == null || organizer.getId() <= 0 || game == null || game.getId() <= 0) {
            formFeedbackLabel.setText("Organisateur et jeu sont obligatoires.");
            return;
        }
        if (titleField.getText() == null || titleField.getText().trim().length() < 3) {
            formFeedbackLabel.setText("Le titre doit contenir au moins 3 caracteres.");
            return;
        }
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            formFeedbackLabel.setText("Dates de debut et fin obligatoires.");
            return;
        }
        Integer maxTeams = parseInteger(maxTeamsField.getText());
        if (maxTeams == null || maxTeams < 2) {
            formFeedbackLabel.setText("Le nombre max d'equipes doit etre >= 2.");
            return;
        }

        BigDecimal prizePool = parseDecimal(prizePoolField.getText());
        if (prizePool == null) {
            prizePool = BigDecimal.ZERO;
        }

        try {
            String photoPath = resolveStoredPhotoPath();
            CompetitionRepository.TournamentMutationInput input = new CompetitionRepository.TournamentMutationInput(
                    editingTournamentId,
                    organizer.getId(),
                    game.getId(),
                    titleField.getText(),
                    descriptionArea.getText(),
                    rulesArea.getText(),
                    startDatePicker.getValue(),
                    endDatePicker.getValue(),
                    deadlineDatePicker.getValue(),
                    maxTeams,
                    formatCombo.getValue(),
                    registrationModeCombo.getValue(),
                    prizePool,
                    prizeDescriptionArea.getText(),
                    formStatusCombo.getValue(),
                    photoPath
            );

            CompetitionRepository.OperationResult result = repository.upsertTournament(input);
            formFeedbackLabel.setText(result.message());
            if (!result.ok()) {
                return;
            }
            resetForm();
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Admin tournois", "Erreur SQL.\n" + ex.getMessage());
        } catch (IOException ex) {
            AlertUtils.error("Photo tournoi", "Impossible de sauvegarder la photo.\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancelEdit() {
        resetForm();
    }

    @FXML
    private void applyFilters() {
        refresh();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        statusFilterCombo.getSelectionModel().select("");
        sortCombo.getSelectionModel().select("latest");
        gameFilterCombo.getSelectionModel().select(0);
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
            List<LookupItem> games = new ArrayList<>();
            games.add(ANY);
            games.addAll(repository.listGames());
            gameFilterCombo.setItems(FXCollections.observableArrayList(games));
            gameFilterCombo.getSelectionModel().select(0);

            List<LookupItem> formGames = new ArrayList<>(games);
            formGames.remove(0);
            formGameCombo.setItems(FXCollections.observableArrayList(formGames));
            if (!formGames.isEmpty()) {
                formGameCombo.getSelectionModel().select(0);
            }

            List<LookupItem> organizers = loadOrganizerUsers();
            organizerCombo.setItems(FXCollections.observableArrayList(organizers));
            if (!organizers.isEmpty()) {
                organizerCombo.getSelectionModel().select(0);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin tournois", "Impossible de charger les listes.\n" + ex.getMessage());
        }
    }

    private void refresh() {
        try {
            String sort = sortCombo.getValue();
            CompetitionRepository.TournamentSearchFilter filter = new CompetitionRepository.TournamentSearchFilter(
                    qField.getText(),
                    selectedId(gameFilterCombo),
                    null,
                    statusFilterCombo.getValue(),
                    "",
                    "",
                    null,
                    null,
                    null,
                    null,
                    mapSortForRepository(sort)
            );
            List<CompetitionRepository.TournamentCatalogRow> rows = repository.searchAdminTournaments(filter, 1000);
            currentRows = applyClientSort(rows, sort);
            resultCountLabel.setText(currentRows.size() + " resultat(s)");
            renderRows();
        } catch (SQLException ex) {
            AlertUtils.error("Admin tournois", "Erreur chargement des tournois.\n" + ex.getMessage());
        }
    }

    private void renderRows() {
        tournamentsListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            tournamentsListBox.getChildren().add(CompetitionUi.emptyState("Aucun tournoi trouve."));
            return;
        }

        for (CompetitionRepository.TournamentCatalogRow row : currentRows) {
            final int tournamentId = row.tournamentId();
            String line1 = "#" + row.tournamentId() + " | " + CompetitionUi.emptySafe(row.title())
                    + " | " + CompetitionUi.emptySafe(row.gameName())
                    + " | Org: " + CompetitionUi.emptySafe(row.organizerName());
            String line2 = "Status: " + CompetitionUi.emptySafe(row.status())
                    + " | " + CompetitionUi.fmtDate(row.startDate()) + " -> " + CompetitionUi.fmtDate(row.endDate())
                    + " | Equipes: " + row.acceptedCount() + "/" + row.maxTeams()
                    + " (total " + row.registeredCount() + ")"
                    + " | Matchs: " + row.matchesTotal()
                    + " | Prize: " + (row.prizePool() == null ? "0" : row.prizePool().toPlainString()) + " DT";

            Button detailButton = new Button("Voir detail");
            detailButton.getStyleClass().add("btn-ghost");
            detailButton.setOnAction(event -> {
                RouteContext.putInt(RouteContext.KEY_TOURNAMENT_ID, tournamentId);
                Navigator.goTo("front_tournament_detail");
            });

            Button updateButton = new Button("Update");
            updateButton.getStyleClass().add("btn-ghost");
            updateButton.setOnAction(event -> loadToForm(tournamentId));

            Button deleteButton = new Button("Delete");
            deleteButton.getStyleClass().add("btn-ghost");
            deleteButton.setOnAction(event -> deleteTournament(tournamentId));

            tournamentsListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, detailButton, updateButton, deleteButton));
        }
    }

    private void loadToForm(int tournamentId) {
        try {
            CompetitionRepository.TournamentCatalogRow row = repository.loadTournamentDetail(tournamentId);
            if (row == null) {
                AlertUtils.warning("Admin tournois", "Tournoi introuvable.");
                return;
            }
            editingTournamentId = row.tournamentId();
            formTitleLabel.setText("MODIFIER TOURNOI #" + tournamentId);
            saveTournamentButton.setText("Mettre a jour");
            cancelEditButton.setVisible(true);
            cancelEditButton.setManaged(true);

            selectById(organizerCombo, row.organizerUserId());
            selectById(formGameCombo, row.gameId());
            titleField.setText(row.title());
            startDatePicker.setValue(row.startDate());
            endDatePicker.setValue(row.endDate());
            deadlineDatePicker.setValue(row.registrationDeadline());
            maxTeamsField.setText(Integer.toString(row.maxTeams()));
            selectOrFallback(formatCombo, row.format(), "BO1");
            selectOrFallback(registrationModeCombo, row.registrationMode(), "OPEN");
            prizePoolField.setText(row.prizePool() == null ? "0" : row.prizePool().toPlainString());
            selectOrFallback(formStatusCombo, row.status(), "OPEN");
            prizeDescriptionArea.setText(row.prizeDescription() == null ? "" : row.prizeDescription());
            descriptionArea.setText(row.description() == null ? "" : row.description());
            rulesArea.setText(row.rules() == null ? "" : row.rules());
            photoPathField.setText(row.photoPath() == null ? "" : row.photoPath());
            selectedPhotoFile = null;
            formFeedbackLabel.setText("Mode modification actif pour le tournoi #" + tournamentId + ".");
            if (pageScroll != null) {
                pageScroll.setVvalue(0.0);
            }
            titleField.requestFocus();
        } catch (SQLException ex) {
            AlertUtils.error("Admin tournois", "Impossible de charger le tournoi.\n" + ex.getMessage());
        } catch (Exception ex) {
            AlertUtils.error("Admin tournois", "Erreur inattendue lors de l'ouverture du formulaire de modification.\n" + ex.getMessage());
        }
    }

    private void deleteTournament(int tournamentId) {
        try {
            CompetitionRepository.OperationResult result = repository.deleteTournament(tournamentId);
            if (!result.ok()) {
                AlertUtils.warning("Admin tournois", result.message());
                return;
            }
            if (editingTournamentId != null && editingTournamentId == tournamentId) {
                resetForm();
            }
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Admin tournois", "Suppression impossible.\n" + ex.getMessage());
        }
    }

    private void resetForm() {
        editingTournamentId = null;
        selectedPhotoFile = null;
        formTitleLabel.setText("CREER TOURNOI");
        saveTournamentButton.setText("Creer tournoi");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);

        titleField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        deadlineDatePicker.setValue(null);
        maxTeamsField.setText("16");
        formatCombo.getSelectionModel().select("BO1");
        registrationModeCombo.getSelectionModel().select("OPEN");
        prizePoolField.setText("0");
        formStatusCombo.getSelectionModel().select("OPEN");
        prizeDescriptionArea.clear();
        descriptionArea.clear();
        rulesArea.clear();
        photoPathField.clear();
        formFeedbackLabel.setText("");
    }

    private void export(boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "Titre", "Jeu", "Organisateur", "Status", "Start date", "End date", "Equipes", "Matchs", "Prize pool");
        List<List<String>> rows = new ArrayList<>();
        for (CompetitionRepository.TournamentCatalogRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.tournamentId()),
                    CompetitionUi.emptySafe(row.title()),
                    CompetitionUi.emptySafe(row.gameName()),
                    CompetitionUi.emptySafe(row.organizerName()),
                    CompetitionUi.emptySafe(row.status()),
                    CompetitionUi.fmtDate(row.startDate()),
                    CompetitionUi.fmtDate(row.endDate()),
                    row.acceptedCount() + "/" + row.maxTeams(),
                    Integer.toString(row.matchesTotal()),
                    row.prizePool() == null ? "0" : row.prizePool().toPlainString()
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Admin tournois", headers, rows)
                    : exportService.exportExcel("admin_tournaments", headers, rows);
            exportService.openFile(file);
            AlertUtils.info("Export", "Fichier genere: " + file.toAbsolutePath());
        } catch (IOException ex) {
            AlertUtils.error("Export", "Impossible de generer l'export.\n" + ex.getMessage());
        }
    }

    private String resolveStoredPhotoPath() throws IOException {
        if (selectedPhotoFile == null || !Files.exists(selectedPhotoFile)) {
            String current = photoPathField.getText() == null ? "" : photoPathField.getText().trim();
            return current.isBlank() ? null : current;
        }

        Path targetDir = AppConfig.webRootPath().resolve("public").resolve("uploads").resolve("tournaments");
        Files.createDirectories(targetDir);

        String original = selectedPhotoFile.getFileName().toString();
        String ext = ".bin";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            String parsed = original.substring(dot).toLowerCase(Locale.ROOT);
            if (parsed.matches("\\.[a-z0-9]{1,10}")) {
                ext = parsed;
            }
        }
        String filename = "tournament_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ext;
        Path target = targetDir.resolve(filename);
        Files.copy(selectedPhotoFile, target, StandardCopyOption.REPLACE_EXISTING);
        String relative = "uploads/tournaments/" + filename;
        photoPathField.setText(relative);
        selectedPhotoFile = null;
        return relative;
    }

    private static String mapSortForRepository(String sort) {
        if ("prize".equalsIgnoreCase(sort) || "oldest".equalsIgnoreCase(sort)) {
            return sort.toLowerCase(Locale.ROOT);
        }
        return "latest";
    }

    private static List<CompetitionRepository.TournamentCatalogRow> applyClientSort(
            List<CompetitionRepository.TournamentCatalogRow> rows,
            String sort
    ) {
        List<CompetitionRepository.TournamentCatalogRow> sorted = new ArrayList<>(rows);
        if ("title".equalsIgnoreCase(sort)) {
            sorted.sort(Comparator.comparing(row -> CompetitionUi.emptySafe(row.title()).toLowerCase(Locale.ROOT)));
        } else if ("status".equalsIgnoreCase(sort)) {
            sorted.sort(Comparator.comparing(row -> CompetitionUi.emptySafe(row.status()).toLowerCase(Locale.ROOT)));
        }
        return sorted;
    }

    private List<LookupItem> loadOrganizerUsers() throws SQLException {
        String sql = """
                SELECT
                    u.user_id,
                    CONCAT('#', u.user_id, ' - ', COALESCE(NULLIF(u.display_name, ''), u.username), ' (', u.role, ')') AS label
                FROM users u
                WHERE u.role IN ('ORGANIZER', 'ADMIN')
                ORDER BY COALESCE(NULLIF(u.display_name, ''), u.username) ASC
                """;

        List<LookupItem> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new LookupItem(rs.getInt("user_id"), rs.getString("label")));
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

    private static Integer selectedId(ComboBox<LookupItem> combo) {
        LookupItem item = combo.getValue();
        if (item == null || item.getId() <= 0) {
            return null;
        }
        return item.getId();
    }

    private static void selectById(ComboBox<LookupItem> combo, int id) {
        for (LookupItem item : combo.getItems()) {
            if (item.getId() == id) {
                combo.getSelectionModel().select(item);
                return;
            }
        }
    }

    private static void selectOrFallback(ComboBox<String> combo, String value, String fallback) {
        if (value != null && combo.getItems().contains(value)) {
            combo.getSelectionModel().select(value);
            return;
        }
        combo.getSelectionModel().select(fallback);
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
