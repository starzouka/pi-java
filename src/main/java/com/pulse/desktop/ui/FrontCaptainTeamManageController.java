package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.TeamModuleRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import com.pulse.desktop.util.Validators;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
import java.util.UUID;

public class FrontCaptainTeamManageController implements RouteAwarePage {
    @FXML
    private ComboBox<LookupItem> teamSelectorCombo;
    @FXML
    private Label modeLabel;
    @FXML
    private Label feedbackLabel;

    @FXML
    private ImageView teamLogoView;
    @FXML
    private Label teamNameLabel;
    @FXML
    private Label teamRegionLabel;
    @FXML
    private Label statsMembersLabel;
    @FXML
    private Label statsProductsLabel;
    @FXML
    private Label statsTournamentsLabel;

    @FXML
    private VBox createPane;
    @FXML
    private TextField createNameField;
    @FXML
    private TextField createRegionField;
    @FXML
    private TextArea createDescriptionArea;
    @FXML
    private TextField createLogoPathField;
    @FXML
    private TextField createStyleHintField;

    @FXML
    private VBox editPane;
    @FXML
    private TextField editNameField;
    @FXML
    private TextField editRegionField;
    @FXML
    private TextArea editDescriptionArea;
    @FXML
    private TextField editLogoPathField;
    @FXML
    private TextField editStyleHintField;
    @FXML
    private TextField editSloganField;

    @FXML
    private VBox myTeamsBox;

    private final TeamModuleRepository repository = new TeamModuleRepository();

    private List<TeamModuleRepository.CaptainTeamRow> captainTeams = List.of();
    private TeamModuleRepository.CaptainTeamRow activeTeam;
    private boolean createMode = false;
    private boolean forceCreateMode = false;
    private File createLogoFile;
    private File editLogoFile;

    @FXML
    public void initialize() {
        createLogoPathField.setEditable(false);
        editLogoPathField.setEditable(false);
        editSloganField.setEditable(false);
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser user = requireCaptain();
        if (user == null) {
            return;
        }
        String requestedMode = RouteContext.getString(RouteContext.KEY_TEAM_MODE);
        forceCreateMode = "create".equalsIgnoreCase(requestedMode)
                || (routeDefinition != null && "front_captain_team_create".equals(routeDefinition.id()));
        RouteContext.clear(RouteContext.KEY_TEAM_MODE);
        reload(user.getUserId(), false);
    }

    @FXML
    private void onTeamSelectionChanged() {
        LookupItem item = teamSelectorCombo.getValue();
        if (item == null || item.getId() <= 0) {
            return;
        }
        RouteContext.putInt(RouteContext.KEY_TEAM_ID, item.getId());
        createMode = false;
        activeTeam = findTeam(item.getId());
        renderState();
    }

    @FXML
    private void openCreateMode() {
        createMode = true;
        clearCreateForm();
        renderState();
    }

    @FXML
    private void openEditMode() {
        if (activeTeam == null) {
            AlertUtils.warning("Mon equipe", "Aucune equipe active a modifier.");
            return;
        }
        createMode = false;
        renderState();
    }

    @FXML
    private void chooseCreateLogo() {
        File file = chooseImageFile("Choisir logo equipe");
        if (file == null) {
            return;
        }
        createLogoFile = file;
        createLogoPathField.setText(file.getAbsolutePath());
    }

    @FXML
    private void chooseEditLogo() {
        File file = chooseImageFile("Choisir nouveau logo");
        if (file == null) {
            return;
        }
        editLogoFile = file;
        editLogoPathField.setText(file.getAbsolutePath());
    }

    @FXML
    private void normalizeCreateRegion() {
        createRegionField.setText(normalizeRegion(createRegionField.getText()));
    }

    @FXML
    private void normalizeEditRegion() {
        editRegionField.setText(normalizeRegion(editRegionField.getText()));
    }

    @FXML
    private void locateCreateRegion() {
        createRegionField.setText(defaultRegion());
    }

    @FXML
    private void locateEditRegion() {
        editRegionField.setText(defaultRegion());
    }

    @FXML
    private void generateCreateBranding() {
        createDescriptionArea.setText(buildBrandingBio(createNameField.getText(), createRegionField.getText(), createStyleHintField.getText()));
    }

    @FXML
    private void generateEditBranding() {
        if (activeTeam == null) {
            return;
        }
        editDescriptionArea.setText(buildBrandingBio(editNameField.getText(), editRegionField.getText(), editStyleHintField.getText()));
        editSloganField.setText(buildBrandingSlogan(editNameField.getText(), editStyleHintField.getText()));
    }

    @FXML
    private void createTeam() {
        SessionUser user = requireCaptain();
        if (user == null) {
            return;
        }
        if (Validators.isBlank(createNameField.getText())) {
            feedbackLabel.setText("Le nom de l'equipe est obligatoire.");
            return;
        }

        try {
            Integer logoImageId = saveLogoIfAny(createLogoFile, user.getUserId(), createNameField.getText());
            TeamModuleRepository.OperationResult result = repository.createCaptainTeam(
                    user.getUserId(),
                    new TeamModuleRepository.TeamMutationInput(
                            null,
                            createNameField.getText(),
                            createDescriptionArea.getText(),
                            createRegionField.getText(),
                            user.getUserId(),
                            logoImageId
                    )
            );
            feedbackLabel.setText(result.message());
            if (!result.ok()) {
                return;
            }
            createLogoFile = null;
            clearCreateForm();
            createMode = false;
            if (result.entityId() != null) {
                RouteContext.putInt(RouteContext.KEY_TEAM_ID, result.entityId());
            }
            reload(user.getUserId(), true);
        } catch (SQLException ex) {
            AlertUtils.error("Mon equipe", "Creation impossible.\n" + ex.getMessage());
        } catch (IOException ex) {
            AlertUtils.error("Mon equipe", "Impossible d'enregistrer le logo.\n" + ex.getMessage());
        }
    }

    @FXML
    private void updateTeam() {
        SessionUser user = requireCaptain();
        if (user == null) {
            return;
        }
        if (activeTeam == null) {
            feedbackLabel.setText("Aucune equipe active.");
            return;
        }
        if (Validators.isBlank(editNameField.getText())) {
            feedbackLabel.setText("Le nom de l'equipe est obligatoire.");
            return;
        }

        try {
            Integer logoImageId = saveLogoIfAny(editLogoFile, user.getUserId(), editNameField.getText());
            TeamModuleRepository.OperationResult result = repository.updateCaptainTeam(
                    user.getUserId(),
                    new TeamModuleRepository.TeamMutationInput(
                            activeTeam.teamId(),
                            editNameField.getText(),
                            editDescriptionArea.getText(),
                            editRegionField.getText(),
                            user.getUserId(),
                            logoImageId
                    )
            );
            feedbackLabel.setText(result.message());
            if (!result.ok()) {
                return;
            }
            editLogoFile = null;
            editLogoPathField.clear();
            reload(user.getUserId(), true);
        } catch (SQLException ex) {
            AlertUtils.error("Mon equipe", "Mise a jour impossible.\n" + ex.getMessage());
        } catch (IOException ex) {
            AlertUtils.error("Mon equipe", "Impossible d'enregistrer le logo.\n" + ex.getMessage());
        }
    }

    @FXML
    private void goMembers() {
        goCaptainPage("front_captain_members");
    }

    @FXML
    private void goRequests() {
        goCaptainPage("front_captain_requests");
    }

    @FXML
    private void goInvite() {
        goCaptainPage("front_captain_invite");
    }

    @FXML
    private void goProducts() {
        goCaptainPage("front_captain_products");
    }

    @FXML
    private void goOrders() {
        goCaptainPage("front_captain_orders");
    }

    @FXML
    private void goTournaments() {
        goCaptainPage("front_captain_tournaments");
    }

    private void goCaptainPage(String route) {
        if (activeTeam != null) {
            RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
        }
        Navigator.goTo(route);
    }

    private void reload(int userId, boolean keepMode) {
        try {
            captainTeams = repository.listCaptainTeams(userId, 200);

            Integer selectedFromContext = RouteContext.getInt(RouteContext.KEY_TEAM_ID);
            if (selectedFromContext != null) {
                activeTeam = findTeam(selectedFromContext);
            }
            if (activeTeam == null && !captainTeams.isEmpty()) {
                activeTeam = captainTeams.get(0);
            }
            if (activeTeam != null) {
                RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
            }
            if (forceCreateMode) {
                createMode = true;
                clearCreateForm();
            } else if (!keepMode) {
                createMode = activeTeam == null;
            }

            renderSelector();
            renderState();
            forceCreateMode = false;
        } catch (SQLException ex) {
            AlertUtils.error("Mon equipe", "Chargement impossible.\n" + ex.getMessage());
        }
    }

    private void renderSelector() {
        List<LookupItem> items = new ArrayList<>();
        for (TeamModuleRepository.CaptainTeamRow row : captainTeams) {
            String label = row.name() + (Validators.isBlank(row.region()) ? "" : " - " + row.region());
            items.add(new LookupItem(row.teamId(), label));
        }
        teamSelectorCombo.setItems(FXCollections.observableArrayList(items));
        boolean hasTeams = !items.isEmpty();
        teamSelectorCombo.setDisable(!hasTeams);
        if (!hasTeams) {
            teamSelectorCombo.getSelectionModel().clearSelection();
            return;
        }
        if (activeTeam != null) {
            for (LookupItem item : items) {
                if (item.getId() == activeTeam.teamId()) {
                    teamSelectorCombo.getSelectionModel().select(item);
                    return;
                }
            }
        }
        teamSelectorCombo.getSelectionModel().select(0);
    }

    private void renderState() {
        createPane.setManaged(createMode);
        createPane.setVisible(createMode);
        editPane.setManaged(!createMode);
        editPane.setVisible(!createMode);

        if (createMode) {
            modeLabel.setText("Creation d'une nouvelle equipe");
        } else {
            modeLabel.setText("Modification de l'equipe active");
        }

        if (activeTeam == null) {
            teamNameLabel.setText("Aucune equipe active");
            teamRegionLabel.setText("Creez votre premiere equipe.");
            teamLogoView.setImage(new Image("https://picsum.photos/seed/no_team/240/240", true));
            statsMembersLabel.setText("0");
            statsProductsLabel.setText("0");
            statsTournamentsLabel.setText("0");
        } else {
            teamNameLabel.setText(CompetitionUi.emptySafe(activeTeam.name()));
            teamRegionLabel.setText(CompetitionUi.emptySafe(activeTeam.region()));
            String logo = ImageResolver.toExternalForm(activeTeam.logoPath());
            if (logo == null) {
                logo = "https://picsum.photos/seed/team_logo_" + activeTeam.teamId() + "/320/320";
            }
            teamLogoView.setImage(new Image(logo, true));

            try {
                TeamModuleRepository.TeamStats stats = repository.loadTeamStats(activeTeam.teamId());
                statsMembersLabel.setText(Integer.toString(stats.members()));
                statsProductsLabel.setText(Integer.toString(stats.products()));
                statsTournamentsLabel.setText(Integer.toString(stats.tournaments()));
            } catch (SQLException ex) {
                statsMembersLabel.setText("0");
                statsProductsLabel.setText("0");
                statsTournamentsLabel.setText("0");
            }

            editNameField.setText(activeTeam.name());
            editRegionField.setText(activeTeam.region() == null ? "" : activeTeam.region());
            editDescriptionArea.setText(activeTeam.description() == null ? "" : activeTeam.description());
            if (Validators.isBlank(editSloganField.getText())) {
                editSloganField.setText(buildBrandingSlogan(activeTeam.name(), editStyleHintField.getText()));
            }
        }

        renderMyTeams();
    }

    private void renderMyTeams() {
        myTeamsBox.getChildren().clear();
        if (captainTeams.isEmpty()) {
            myTeamsBox.getChildren().add(CompetitionUi.emptyState("Aucune equipe pour le moment."));
            return;
        }

        for (TeamModuleRepository.CaptainTeamRow row : captainTeams) {
            Label left = new Label(row.name() + " | " + CompetitionUi.emptySafe(row.region()));
            left.setWrapText(true);

            Button manage = new Button("Gerer");
            manage.getStyleClass().add("btn-ghost");
            manage.setOnAction(event -> {
                activeTeam = row;
                RouteContext.putInt(RouteContext.KEY_TEAM_ID, row.teamId());
                createMode = false;
                renderSelector();
                renderState();
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox rowBox = new HBox(10, left, spacer, manage);
            rowBox.getStyleClass().add("list-item");
            myTeamsBox.getChildren().add(rowBox);
        }
    }

    private TeamModuleRepository.CaptainTeamRow findTeam(int teamId) {
        for (TeamModuleRepository.CaptainTeamRow row : captainTeams) {
            if (row.teamId() == teamId) {
                return row;
            }
        }
        return null;
    }

    private void clearCreateForm() {
        createNameField.clear();
        createRegionField.clear();
        createDescriptionArea.clear();
        createLogoPathField.clear();
        createStyleHintField.clear();
        createLogoFile = null;
    }

    private File chooseImageFile(String title) {
        Window window = teamSelectorCombo.getScene() == null ? null : teamSelectorCombo.getScene().getWindow();
        if (window == null) {
            return null;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"));
        return chooser.showOpenDialog(window);
    }

    private Integer saveLogoIfAny(File source, int uploadedByUserId, String teamName) throws IOException, SQLException {
        if (source == null || !source.isFile()) {
            return null;
        }

        String fileName = source.getName().toLowerCase(Locale.ROOT);
        if (!(fileName.endsWith(".png")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".webp")
                || fileName.endsWith(".gif"))) {
            throw new IOException("Format invalide. Formats supportes: png, jpg, jpeg, webp, gif.");
        }

        Path uploadsDir = AppConfig.webRootPath().resolve("public").resolve("uploads").resolve("teams");
        Files.createDirectories(uploadsDir);

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        String generated = "team_" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
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

        String relativePath = "uploads/teams/" + generated;
        return repository.createImageRecord(new TeamModuleRepository.ImageCreateInput(
                uploadedByUserId,
                relativePath,
                mimeType,
                Files.size(target),
                width,
                height,
                "Logo equipe " + (Validators.isBlank(teamName) ? "" : teamName.trim())
        ));
    }

    private static String normalizeRegion(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return "";
        }
        String[] parts = value.split("\\s+");
        StringBuilder normalized = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!normalized.isEmpty()) {
                normalized.append(' ');
            }
            normalized.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                normalized.append(part.substring(1));
            }
        }
        return normalized.toString();
    }

    private static String defaultRegion() {
        String country = Locale.getDefault().getDisplayCountry(Locale.FRENCH);
        if (country == null || country.isBlank()) {
            country = Locale.getDefault().getDisplayCountry(Locale.ENGLISH);
        }
        return country == null ? "" : country;
    }

    private static String buildBrandingBio(String teamNameRaw, String regionRaw, String styleRaw) {
        String teamName = Validators.isBlank(teamNameRaw) ? "Notre equipe" : teamNameRaw.trim();
        String region = Validators.isBlank(regionRaw) ? "notre region" : regionRaw.trim();
        String style = Validators.isBlank(styleRaw) ? "equilibre et discipline" : styleRaw.trim();

        return teamName + " represente " + region
                + " avec une identite axee sur " + style
                + ". Notre roster vise la progression constante, le respect des coequipiers et la performance en competition.";
    }

    private static String buildBrandingSlogan(String teamNameRaw, String styleRaw) {
        String teamName = Validators.isBlank(teamNameRaw) ? "Pulse Squad" : teamNameRaw.trim();
        String style = Validators.isBlank(styleRaw) ? "Precision et Mental" : styleRaw.trim();
        return teamName + " - " + style;
    }

    private static SessionUser requireCaptain() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return null;
        }
        String role = SessionContext.currentRole();
        if (!"CAPTAIN".equals(role) && !"ADMIN".equals(role)) {
            AlertUtils.warning("Acces refuse", "Cette page est reservee a l'espace capitaine.");
            Navigator.goTo("front_home");
            return null;
        }
        return user;
    }
}
