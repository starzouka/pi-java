package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.CompetitionRepository;
import com.pulse.desktop.service.MailService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class FrontOrganizerRequestCreateController implements RouteAwarePage {
    @FXML
    private Label organizerValueLabel;
    @FXML
    private ComboBox<LookupItem> gameCombo;
    @FXML
    private TextField titleField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private DatePicker registrationDeadlinePicker;
    @FXML
    private TextField maxTeamsField;
    @FXML
    private ComboBox<String> formatCombo;
    @FXML
    private ComboBox<String> registrationModeCombo;
    @FXML
    private TextField prizePoolField;
    @FXML
    private TextArea prizeDescriptionArea;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextArea rulesArea;
    @FXML
    private TextField photoPathField;
    @FXML
    private Label feedbackLabel;

    private final CompetitionRepository repository = new CompetitionRepository();
    private final MailService mailService = new MailService();
    private Path selectedPhotoFile;

    @FXML
    public void initialize() {
        formatCombo.setItems(FXCollections.observableArrayList("", "BO1", "BO3", "BO5"));
        formatCombo.getSelectionModel().select("BO1");

        registrationModeCombo.setItems(FXCollections.observableArrayList("", "OPEN", "APPROVAL"));
        registrationModeCombo.getSelectionModel().select("OPEN");

        maxTeamsField.setText("16");
        prizePoolField.setText("0");
        photoPathField.setEditable(false);
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser user = requireOrganizer();
        if (user == null) {
            return;
        }
        organizerValueLabel.setText("#" + user.getUserId() + " - " + user.getDisplayName() + " (" + user.getUsername() + ")");
        loadGames();
    }

    @FXML
    private void choosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selectionner une photo de tournoi");
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
    private void submitForm() {
        SessionUser user = requireOrganizer();
        if (user == null) {
            return;
        }

        LookupItem selectedGame = gameCombo.getValue();
        if (selectedGame == null || selectedGame.getId() <= 0) {
            feedbackLabel.setText("Selectionnez un jeu.");
            return;
        }
        if (titleField.getText() == null || titleField.getText().trim().length() < 3) {
            feedbackLabel.setText("Le titre doit contenir au moins 3 caracteres.");
            return;
        }
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate == null || endDate == null) {
            feedbackLabel.setText("Les dates de debut et de fin sont obligatoires.");
            return;
        }

        Integer maxTeams = parseInteger(maxTeamsField.getText());
        if (maxTeams == null || maxTeams < 2) {
            feedbackLabel.setText("Le nombre max d'equipes doit etre >= 2.");
            return;
        }

        BigDecimal prizePool = parseDecimal(prizePoolField.getText());
        if (prizePool == null) {
            prizePool = BigDecimal.ZERO;
        }

        String format = normalizeOrDefault(formatCombo.getValue(), "BO1");
        String registrationMode = normalizeOrDefault(registrationModeCombo.getValue(), "OPEN");

        try {
            String storedPhotoPath = storeSelectedPhoto();
            CompetitionRepository.RequestMutationInput input = new CompetitionRepository.RequestMutationInput(
                    user.getUserId(),
                    selectedGame.getId(),
                    titleField.getText(),
                    descriptionArea.getText(),
                    rulesArea.getText(),
                    startDate,
                    endDate,
                    registrationDeadlinePicker.getValue(),
                    maxTeams,
                    format,
                    registrationMode,
                    prizePool,
                    prizeDescriptionArea.getText(),
                    storedPhotoPath
            );

            CompetitionRepository.OperationResult result = repository.createOrganizerRequest(input);
            feedbackLabel.setText(result.message());
            if (!result.ok()) {
                return;
            }

            sendRequestReceivedEmailToAdmins(user, selectedGame, titleField.getText());
            clearForm();
            Navigator.goTo("front_organizer_requests");
        } catch (SQLException ex) {
            AlertUtils.error("Demande tournoi", "Erreur SQL.\n" + ex.getMessage());
        } catch (IOException ex) {
            AlertUtils.error("Photo tournoi", "Impossible de sauvegarder la photo.\n" + ex.getMessage());
        }
    }

    private void sendRequestReceivedEmailToAdmins(SessionUser organizer, LookupItem game, String requestTitle) {
        if (organizer == null || !mailService.isConfigured()) {
            return;
        }

        List<CompetitionRepository.AdminEmailRecipient> recipients;
        try {
            recipients = repository.listActiveAdminEmailRecipients();
        } catch (SQLException ex) {
            AlertUtils.warning("Email", "Demande enregistree, mais impossible de charger les emails admins.\n" + ex.getMessage());
            return;
        }
        if (recipients.isEmpty()) {
            return;
        }

        String organizerName = organizer.getDisplayName();
        String gameLabel = game == null ? null : game.getLabel();
        int failed = 0;

        for (CompetitionRepository.AdminEmailRecipient recipient : recipients) {
            if (recipient == null || !mailService.isValidRecipientEmail(recipient.email())) {
                failed++;
                continue;
            }
            try {
                mailService.sendTournamentRequestReceived(
                        recipient.email(),
                        organizerName,
                        requestTitle,
                        gameLabel
                );
            } catch (Exception ex) {
                failed++;
            }
        }

        if (failed > 0) {
            AlertUtils.warning("Email", "Demande enregistree, mais certains emails admins n'ont pas ete envoyes.");
        }
    }

    @FXML
    private void cancelToRequests() {
        Navigator.goTo("front_organizer_requests");
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

    private void loadGames() {
        try {
            List<LookupItem> games = new ArrayList<>();
            games.add(new LookupItem(0, "Selectionner un jeu"));
            games.addAll(repository.listGames());
            gameCombo.setItems(FXCollections.observableArrayList(games));
            if (gameCombo.getSelectionModel().isEmpty()) {
                gameCombo.getSelectionModel().select(0);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Demande tournoi", "Impossible de charger les jeux.\n" + ex.getMessage());
        }
    }

    private String storeSelectedPhoto() throws IOException {
        if (selectedPhotoFile == null || !Files.exists(selectedPhotoFile) || !Files.isRegularFile(selectedPhotoFile)) {
            String currentText = photoPathField.getText() == null ? "" : photoPathField.getText().trim();
            return currentText.isBlank() ? null : currentText;
        }

        Path uploadsDir = AppConfig.webRootPath().resolve("public").resolve("uploads").resolve("tournaments");
        Files.createDirectories(uploadsDir);

        String original = selectedPhotoFile.getFileName().toString();
        String ext = ".bin";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            String candidate = original.substring(dot).toLowerCase(Locale.ROOT);
            if (candidate.matches("\\.[a-z0-9]{1,10}")) {
                ext = candidate;
            }
        }

        String filename = "tournament_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ext;
        Path target = uploadsDir.resolve(filename);
        Files.copy(selectedPhotoFile, target, StandardCopyOption.REPLACE_EXISTING);

        String relative = "uploads/tournaments/" + filename;
        photoPathField.setText(relative);
        selectedPhotoFile = null;
        return relative;
    }

    private void clearForm() {
        titleField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        registrationDeadlinePicker.setValue(null);
        maxTeamsField.setText("16");
        formatCombo.getSelectionModel().select("BO1");
        registrationModeCombo.getSelectionModel().select("OPEN");
        prizePoolField.setText("0");
        prizeDescriptionArea.clear();
        descriptionArea.clear();
        rulesArea.clear();
        photoPathField.clear();
        selectedPhotoFile = null;
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

    private static String normalizeOrDefault(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static SessionUser requireOrganizer() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            AlertUtils.warning("Connexion requise", "Connectez-vous pour envoyer une demande de tournoi.");
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
