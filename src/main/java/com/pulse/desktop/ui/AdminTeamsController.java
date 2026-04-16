package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.TeamModuleRepository;
import com.pulse.desktop.service.ExportService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.Validators;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class AdminTeamsController implements RouteAwarePage {
    @FXML
    private ScrollPane pageScroll;
    @FXML
    private Label formTitleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private TextField regionField;
    @FXML
    private ComboBox<LookupItem> captainCombo;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField logoPathField;
    @FXML
    private Label formFeedbackLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelEditButton;

    @FXML
    private TextField qField;
    @FXML
    private TextField regionFilterField;
    @FXML
    private TextField captainFilterField;
    @FXML
    private ComboBox<String> withProductsCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private ComboBox<String> directionCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private VBox teamsListBox;

    private final TeamModuleRepository repository = new TeamModuleRepository();
    private final ExportService exportService = new ExportService();

    private List<TeamModuleRepository.AdminTeamRow> currentRows = List.of();
    private Integer editingTeamId;
    private Integer editingLogoImageId;
    private File selectedLogoFile;

    @FXML
    public void initialize() {
        logoPathField.setEditable(false);
        withProductsCombo.setItems(FXCollections.observableArrayList("", "1", "0"));
        withProductsCombo.getSelectionModel().select(0);
        sortCombo.setItems(FXCollections.observableArrayList("created_at", "id", "name", "region", "captain", "members", "products"));
        sortCombo.getSelectionModel().select("created_at");
        directionCombo.setItems(FXCollections.observableArrayList("desc", "asc"));
        directionCombo.getSelectionModel().select("desc");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        loadCaptains();
        resetForm();
        refreshRows();
    }

    @FXML
    private void startCreate() {
        resetForm();
        formFeedbackLabel.setText("Mode ajout actif.");
        if (pageScroll != null) {
            pageScroll.setVvalue(0.0);
        }
        nameField.requestFocus();
    }

    @FXML
    private void chooseLogo() {
        Window window = nameField.getScene() == null ? null : nameField.getScene().getWindow();
        if (window == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir logo equipe");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"));
        File file = chooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        selectedLogoFile = file;
        logoPathField.setText(file.getAbsolutePath());
    }

    @FXML
    private void saveTeam() {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }

        LookupItem captain = captainCombo.getValue();
        if (Validators.isBlank(nameField.getText())) {
            formFeedbackLabel.setText("Le nom de l'equipe est obligatoire.");
            return;
        }
        if (captain == null || captain.getId() <= 0) {
            formFeedbackLabel.setText("Capitaine invalide.");
            return;
        }

        try {
            Integer logoImageId = editingLogoImageId;
            if (selectedLogoFile != null) {
                logoImageId = saveLogoAndImage(selectedLogoFile, admin.getUserId(), nameField.getText());
            }

            TeamModuleRepository.OperationResult result = repository.upsertAdminTeam(
                    new TeamModuleRepository.TeamMutationInput(
                            editingTeamId,
                            nameField.getText(),
                            descriptionArea.getText(),
                            regionField.getText(),
                            captain.getId(),
                            logoImageId
                    )
            );
            formFeedbackLabel.setText(result.message());
            if (!result.ok()) {
                return;
            }

            resetForm();
            refreshRows();
        } catch (SQLException ex) {
            AlertUtils.error("Admin equipes", "Enregistrement impossible.\n" + ex.getMessage());
        } catch (IOException ex) {
            AlertUtils.error("Admin equipes", "Impossible d'uploader le logo.\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancelEdit() {
        resetForm();
    }

    @FXML
    private void applyFilters() {
        refreshRows();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        regionFilterField.clear();
        captainFilterField.clear();
        withProductsCombo.getSelectionModel().select(0);
        sortCombo.getSelectionModel().select("created_at");
        directionCombo.getSelectionModel().select("desc");
        refreshRows();
    }

    @FXML
    private void exportPdf() {
        export(true);
    }

    @FXML
    private void exportExcel() {
        export(false);
    }

    private void loadCaptains() {
        try {
            List<LookupItem> captains = repository.listCaptainsForAdmin();
            captainCombo.setItems(FXCollections.observableArrayList(captains));
            if (!captains.isEmpty()) {
                captainCombo.getSelectionModel().select(0);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin equipes", "Impossible de charger les capitaines.\n" + ex.getMessage());
        }
    }

    private void refreshRows() {
        try {
            TeamModuleRepository.AdminTeamFilter filter = new TeamModuleRepository.AdminTeamFilter(
                    qField.getText(),
                    regionFilterField.getText(),
                    captainFilterField.getText(),
                    withProductsCombo.getValue(),
                    sortCombo.getValue(),
                    directionCombo.getValue()
            );
            currentRows = repository.searchAdminTeams(filter, 1000);
            resultCountLabel.setText(currentRows.size() + " resultat(s)");
            renderRows();
        } catch (SQLException ex) {
            AlertUtils.error("Admin equipes", "Erreur de chargement.\n" + ex.getMessage());
        }
    }

    private void renderRows() {
        teamsListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            teamsListBox.getChildren().add(CompetitionUi.emptyState("Aucune equipe trouvee."));
            return;
        }

        for (TeamModuleRepository.AdminTeamRow row : currentRows) {
            String line1 = "#" + row.teamId()
                    + " | " + CompetitionUi.emptySafe(row.name())
                    + " | Region: " + CompetitionUi.emptySafe(row.region());
            String line2 = "Capitaine: " + row.captainLabel()
                    + " | Membres: " + row.membersCount()
                    + " | Produits: " + row.productsCount()
                    + " | Cree le: " + CompetitionUi.fmtDateTime(row.createdAt());

            Button edit = new Button("Update");
            edit.getStyleClass().add("btn-ghost");
            edit.setOnAction(event -> startEdit(row));

            Button delete = new Button("Delete");
            delete.getStyleClass().add("btn-ghost");
            delete.setOnAction(event -> deleteTeam(row.teamId()));

            teamsListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, edit, delete));
        }
    }

    private void startEdit(TeamModuleRepository.AdminTeamRow row) {
        editingTeamId = row.teamId();
        editingLogoImageId = row.logoImageId();
        selectedLogoFile = null;
        formTitleLabel.setText("MODIFIER EQUIPE #" + row.teamId());
        saveButton.setText("Mettre a jour");
        cancelEditButton.setManaged(true);
        cancelEditButton.setVisible(true);

        nameField.setText(row.name());
        regionField.setText(row.region() == null ? "" : row.region());
        descriptionArea.setText(row.description() == null ? "" : row.description());
        logoPathField.clear();
        selectCaptain(row.captainUserId());

        formFeedbackLabel.setText("Mode modification actif.");
        if (pageScroll != null) {
            pageScroll.setVvalue(0.0);
        }
        nameField.requestFocus();
    }

    private void deleteTeam(int teamId) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        if (!confirmDelete()) {
            return;
        }

        try {
            TeamModuleRepository.OperationResult result = repository.deleteAdminTeam(teamId);
            if (!result.ok()) {
                AlertUtils.warning("Admin equipes", result.message());
                return;
            }
            if (editingTeamId != null && editingTeamId == teamId) {
                resetForm();
            }
            refreshRows();
            formFeedbackLabel.setText(result.message());
        } catch (SQLException ex) {
            AlertUtils.error("Admin equipes", "Suppression impossible.\n" + ex.getMessage());
        }
    }

    private void resetForm() {
        editingTeamId = null;
        editingLogoImageId = null;
        selectedLogoFile = null;

        formTitleLabel.setText("NOUVELLE EQUIPE");
        saveButton.setText("Creer equipe");
        cancelEditButton.setManaged(false);
        cancelEditButton.setVisible(false);

        nameField.clear();
        regionField.clear();
        descriptionArea.clear();
        logoPathField.clear();
        if (!captainCombo.getItems().isEmpty()) {
            captainCombo.getSelectionModel().select(0);
        }
        formFeedbackLabel.setText("");
    }

    private void export(boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "Nom", "Region", "Capitaine", "Membres", "Produits", "Cree le");
        List<List<String>> rows = new ArrayList<>();
        for (TeamModuleRepository.AdminTeamRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.teamId()),
                    CompetitionUi.emptySafe(row.name()),
                    CompetitionUi.emptySafe(row.region()),
                    row.captainLabel(),
                    Integer.toString(row.membersCount()),
                    Integer.toString(row.productsCount()),
                    CompetitionUi.fmtDateTime(row.createdAt())
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Equipes", headers, rows)
                    : exportService.exportExcel("admin_teams", headers, rows);
            exportService.openFile(file);
            AlertUtils.info("Export", "Fichier genere: " + file.toAbsolutePath());
        } catch (IOException ex) {
            AlertUtils.error("Export", "Impossible de generer l'export.\n" + ex.getMessage());
        }
    }

    private Integer saveLogoAndImage(File source, int uploadedByUserId, String teamName) throws IOException, SQLException {
        if (source == null || !source.isFile()) {
            return null;
        }

        String fileName = source.getName().toLowerCase(Locale.ROOT);
        if (!(fileName.endsWith(".png")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".webp")
                || fileName.endsWith(".gif"))) {
            throw new IOException("Format invalide.");
        }

        Path uploadsDir = AppConfig.webRootPath().resolve("public").resolve("uploads").resolve("teams");
        Files.createDirectories(uploadsDir);

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        String generated = "team_admin_" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = uploadsDir.resolve(generated);
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

        String mimeType = Files.probeContentType(target);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = switch (extension) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                case "gif" -> "image/gif";
                default -> "application/octet-stream";
            };
        }

        Integer width = null;
        Integer height = null;
        try {
            BufferedImage image = ImageIO.read(target.toFile());
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (Exception ignored) {
            // Optional dimensions.
        }

        return repository.createImageRecord(new TeamModuleRepository.ImageCreateInput(
                uploadedByUserId,
                "uploads/teams/" + generated,
                mimeType,
                Files.size(target),
                width,
                height,
                "Logo equipe " + (Validators.isBlank(teamName) ? "" : teamName.trim())
        ));
    }

    private void selectCaptain(int userId) {
        for (LookupItem item : captainCombo.getItems()) {
            if (item.getId() == userId) {
                captainCombo.getSelectionModel().select(item);
                return;
            }
        }
    }

    private static boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression equipe");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer cette equipe ?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
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
