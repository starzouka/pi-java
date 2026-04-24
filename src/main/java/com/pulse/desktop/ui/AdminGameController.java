package com.pulse.desktop.ui;

import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.model.CategoryModel;
import com.pulse.desktop.model.GameModel;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.CategoryRepository;
import com.pulse.desktop.repo.GameRepository;
import com.pulse.desktop.service.ActivityLogService;
import com.pulse.desktop.service.AiChatService;
import com.pulse.desktop.service.GameCoverStorageService;
import com.pulse.desktop.service.LocalQrWebServer;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import com.pulse.desktop.util.QrCodeDialog;
import com.pulse.desktop.util.UrlUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class AdminGameController implements RouteAwarePage {
    private static final String DEFAULT_IMAGE = "assets/template_fo/img/ll.png";

    private static final CategoryModel ALL_CATEGORIES = new CategoryModel(null, "Toutes", null, null, null);
    private static final String SORT_POPULARITY = "Popularite";
    private static final String SORT_NAME = "Nom A-Z";
    private static final String SORT_NEWEST = "Plus recents";

    @FXML private FlowPane gamesCardsPane;

    // CRUD form
    @FXML private TextField nameField;
    @FXML private ComboBox<CategoryModel> categoryBox;
    @FXML private TextField publisherField;
    @FXML private TextField slugField;
    @FXML private TextField statusField;
    @FXML private TextArea descriptionField;
    @FXML private Region coverPreview;
    @FXML private Label coverPathLabel;

    // Filters
    @FXML private TextField qField;
    @FXML private ComboBox<CategoryModel> categoryFilterBox;
    @FXML private TextField publisherFilterField;
    @FXML private ComboBox<String> statusBox;
    @FXML private ComboBox<String> sortBox;
    @FXML private Button filterButton;
    @FXML private Button resetButton;

    // IA assistant (API)
    @FXML private Label aiStatusLabel;
    @FXML private TextArea aiChatArea;
    @FXML private TextField aiInputField;

    private final GameRepository gameRepo = new GameRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final AiChatService aiChatService = new AiChatService();
    private final GameCoverStorageService coverStorageService = new GameCoverStorageService();
    private final ActivityLogService activityLogService = new ActivityLogService();

    private final ObservableList<CategoryModel> categories = FXCollections.observableArrayList();
    private final ObservableList<CategoryModel> filterCategories = FXCollections.observableArrayList();
    private final ObservableList<String> statuses = FXCollections.observableArrayList();
    private final ObservableList<String> sorts = FXCollections.observableArrayList();

    private final Map<Integer, VBox> cardById = new HashMap<>();

    private List<GameModel> allGames = List.of();
    private GameModel selectedGame;
    private String formCoverName;

    @FXML
    public void initialize() {
        categoryBox.setItems(categories);
        if (categoryFilterBox != null) {
            categoryFilterBox.setItems(filterCategories);
        }
        if (statusBox != null) {
            statusBox.setItems(statuses);
        }
        if (sortBox != null) {
            sortBox.setItems(sorts);
        }

        categoryBox.setConverter(categoryConverter());
        if (categoryFilterBox != null) {
            categoryFilterBox.setConverter(categoryConverter());
        }

        sorts.setAll(SORT_POPULARITY, SORT_NEWEST, SORT_NAME);
        if (sortBox != null) {
            sortBox.getSelectionModel().select(SORT_POPULARITY);
        }

        if (filterButton != null) {
            filterButton.setOnAction(event -> applyFilters());
        }
        if (resetButton != null) {
            resetButton.setOnAction(event -> resetFilters());
        }

        refreshAiStatus();
        refreshCoverUi();
        loadData();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
    }

    @FXML
    private void sendAiMessage() {
        if (aiInputField == null || aiChatArea == null) {
            return;
        }
        String message = safeText(aiInputField.getText());
        if (message.isBlank()) {
            return;
        }
        aiInputField.clear();

        if (!aiChatService.isConfigured()) {
            appendChat("System", "API IA non configuree. Ajoutez PULSE_AI_ENDPOINT (ou ai.endpoint) puis relancez l'app.");
            refreshAiStatus();
            return;
        }

        appendChat("Vous", message);
        Map<String, String> context = buildAiContext();

        Thread worker = new Thread(() -> {
            try {
                String reply = aiChatService.sendMessage(message, context);
                Platform.runLater(() -> appendChat("IA", reply));
            } catch (Exception ex) {
                String details = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "(no message)" : ex.getMessage());
                Platform.runLater(() -> appendChat("System", "Erreur IA: " + details));
            }
        }, "ai-chat-admin-games");
        worker.setDaemon(true);
        worker.start();
    }

    private Map<String, String> buildAiContext() {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("page", "admin_games");
        ctx.put("db", AppConfig.dbName());
        ctx.put("web_base_url", AppConfig.webBaseUrl());

        ctx.put("selected_game_id", selectedGame == null || selectedGame.getGameId() == null ? "" : selectedGame.getGameId().toString());
        ctx.put("selected_game_name", selectedGame == null ? "" : safeText(selectedGame.getName()));
        ctx.put("selected_game_slug", selectedGame == null ? "" : safeText(selectedGame.getSlug()));

        ctx.put("form_name", safeText(nameField == null ? "" : nameField.getText()));
        ctx.put("form_publisher", safeText(publisherField == null ? "" : publisherField.getText()));
        ctx.put("form_slug", safeText(slugField == null ? "" : slugField.getText()));
        ctx.put("form_status", safeText(statusField == null ? "" : statusField.getText()));
        ctx.put("form_description", safeText(descriptionField == null ? "" : descriptionField.getText()));
        ctx.put("form_cover", safeText(formCoverName));

        CategoryModel selectedCat = categoryBox == null ? null : categoryBox.getSelectionModel().getSelectedItem();
        ctx.put("form_category", selectedCat == null ? "" : safeText(selectedCat.getName()));
        return ctx;
    }

    private void appendChat(String who, String message) {
        if (aiChatArea == null) {
            return;
        }
        String line = (who == null ? "" : who) + ": " + (message == null ? "" : message).trim();
        if (aiChatArea.getText() == null || aiChatArea.getText().isBlank()) {
            aiChatArea.setText(line);
        } else {
            aiChatArea.appendText("\n\n" + line);
        }
    }

    private void refreshAiStatus() {
        if (aiStatusLabel == null) {
            return;
        }
        aiStatusLabel.setText(aiChatService.isConfigured() ? "API: configuree" : "API: non configuree");
    }

    @FXML
    private void loadData() {
        try {
            categories.setAll(categoryRepo.findAll());
            allGames = gameRepo.findAll();
            rebuildFilterCategories();
            rebuildStatusOptions();
            applyFilters();
        } catch (Exception e) {
            AlertUtils.error("Erreur DB", "Chargement echoue: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        String name = safeText(nameField.getText());
        if (name.isBlank()) {
            AlertUtils.warning("Attention", "Le nom du jeu est obligatoire.");
            return;
        }

        CategoryModel selectedCat = categoryBox.getSelectionModel().getSelectedItem();
        if (selectedCat == null || selectedCat.getCategoryId() == null) {
            AlertUtils.warning("Attention", "Veuillez choisir une categorie.");
            return;
        }

        try {
            GameModel game = new GameModel();
            game.setName(name);
            game.setPublisher(safeNullIfBlank(publisherField.getText()));
            game.setSlug(resolveSlug(slugField.getText(), name));
            game.setStatus(resolveStatus(statusField.getText()));
            game.setDescription(safeNullIfBlank(descriptionField == null ? "" : descriptionField.getText()));
            game.setCategoryId(selectedCat.getCategoryId());
            game.setCoverName(safeNullIfBlank(formCoverName));

            gameRepo.insert(game);
            activityLogService.log("GAME", "CREATE", game.getGameId());
            AlertUtils.info("Succes", "Jeu ajoute.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Ajout echoue: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedGame == null || selectedGame.getGameId() == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner un jeu.");
            return;
        }

        String name = safeText(nameField.getText());
        if (name.isBlank()) {
            AlertUtils.warning("Attention", "Le nom du jeu est obligatoire.");
            return;
        }

        CategoryModel selectedCat = categoryBox.getSelectionModel().getSelectedItem();
        if (selectedCat == null || selectedCat.getCategoryId() == null) {
            AlertUtils.warning("Attention", "Veuillez choisir une categorie.");
            return;
        }

        try {
            selectedGame.setName(name);
            selectedGame.setPublisher(safeNullIfBlank(publisherField.getText()));
            selectedGame.setSlug(resolveSlug(slugField.getText(), name));
            selectedGame.setStatus(resolveStatus(statusField.getText()));
            selectedGame.setDescription(safeNullIfBlank(descriptionField == null ? "" : descriptionField.getText()));
            selectedGame.setCategoryId(selectedCat.getCategoryId());
            selectedGame.setCoverName(safeNullIfBlank(formCoverName));

            gameRepo.update(selectedGame);
            activityLogService.log("GAME", "UPDATE", selectedGame.getGameId());
            AlertUtils.info("Succes", "Jeu modifie.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Modification echouee: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedGame == null || selectedGame.getGameId() == null) {
            AlertUtils.warning("Attention", "Veuillez selectionner un jeu.");
            return;
        }

        boolean ok = AlertUtils.confirm("Confirmer", "Supprimer le jeu \"" + safeText(selectedGame.getName()) + "\" ?");
        if (!ok) {
            return;
        }

        try {
            gameRepo.deleteById(selectedGame.getGameId());
            activityLogService.log("GAME", "DELETE", selectedGame.getGameId());
            AlertUtils.info("Succes", "Jeu supprime.");
            clearFields();
            loadData();
        } catch (Exception e) {
            AlertUtils.error("Erreur", "Suppression echouee: " + e.getMessage());
        }
    }

    @FXML
    private void clearFields() {
        clearSelected();
        nameField.clear();
        publisherField.clear();
        slugField.clear();
        statusField.clear();
        if (descriptionField != null) {
            descriptionField.clear();
        }
        categoryBox.getSelectionModel().clearSelection();
        formCoverName = null;
        refreshCoverUi();
    }

    @FXML
    private void applyFilters() {
        String q = normalize(qField == null ? "" : qField.getText());
        String publisher = normalize(publisherFilterField == null ? "" : publisherFilterField.getText());
        CategoryModel selectedCategory = categoryFilterBox == null
            ? ALL_CATEGORIES
            : categoryFilterBox.getSelectionModel().getSelectedItem();
        String status = statusBox == null ? "Tous" : statusBox.getSelectionModel().getSelectedItem();
        String sort = sortBox == null ? SORT_POPULARITY : sortBox.getSelectionModel().getSelectedItem();

        List<GameModel> filtered = allGames.stream()
            .filter(game -> matchesQuery(game, q))
            .filter(game -> matchesPublisher(game, publisher))
            .filter(game -> matchesCategory(game, selectedCategory))
            .filter(game -> matchesStatus(game, status))
            .collect(Collectors.toList());

        sortGames(filtered, sort);
        renderCards(filtered);
    }

    @FXML
    private void resetFilters() {
        if (qField != null) {
            qField.clear();
        }
        if (publisherFilterField != null) {
            publisherFilterField.clear();
        }
        if (categoryFilterBox != null) {
            categoryFilterBox.getSelectionModel().select(ALL_CATEGORIES);
        }
        if (statusBox != null) {
            statusBox.getSelectionModel().select("Tous");
        }
        if (sortBox != null) {
            sortBox.getSelectionModel().select(SORT_POPULARITY);
        }
        applyFilters();
    }

    private void renderCards(List<GameModel> games) {
        gamesCardsPane.getChildren().clear();
        cardById.clear();

        if (games.isEmpty()) {
            Label empty = new Label("Aucun jeu ne correspond aux filtres.");
            empty.getStyleClass().add("muted");
            gamesCardsPane.getChildren().add(empty);
            return;
        }

        for (GameModel game : games) {
            VBox card = buildGameCard(game);
            if (game.getGameId() != null) {
                cardById.put(game.getGameId(), card);
            }
            gamesCardsPane.getChildren().add(card);
        }

        applySelectionStyle();
    }

    private VBox buildGameCard(GameModel game) {
        VBox cardBox = new VBox();
        cardBox.getStyleClass().addAll("card", "card--game");
        cardBox.setPrefWidth(305);
        cardBox.setMinWidth(280);

        Region media = new Region();
        media.getStyleClass().add("card__media");
        media.setPrefHeight(162);

        String imagePath = firstNonBlank(game.getCoverName(), DEFAULT_IMAGE);
        media.setStyle(ImageResolver.toBackgroundStyle(imagePath));

        HBox chips = new HBox(6);
        chips.getStyleClass().add("card__chips");

        Label categoryChip = new Label(safe(game.getCategoryName()));
        categoryChip.getStyleClass().addAll("chip", "chip--category");
        Label statusChip = new Label(safe(game.getStatus()));
        statusChip.getStyleClass().addAll("chip", "chip--status");
        chips.getChildren().addAll(categoryChip, statusChip);

        StackPane mediaWrapper = new StackPane(media, chips);
        StackPane.setAlignment(chips, Pos.TOP_LEFT);

        VBox body = new VBox(8);
        body.getStyleClass().add("card__body");

        Label title = new Label(safe(game.getName()));
        title.getStyleClass().add("card__title");
        title.setWrapText(true);

        String subtitleText = "Editeur: " + safe(game.getPublisher()) + " | Slug: " + safe(game.getSlug());
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("card__desc");
        subtitle.setWrapText(true);

        Button editButton = new Button("Editer");
        editButton.getStyleClass().addAll("btn", "btn--ghost", "btn--compact");
        editButton.setOnAction(event -> selectGame(game));

        Button qrButton = new Button("QR Code");
        qrButton.getStyleClass().addAll("btn", "btn--ghost", "btn--compact");
        qrButton.setOnAction(event -> showGameQr(game));

        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().addAll("btn", "btn--soft", "btn--compact");
        deleteButton.setOnAction(event -> {
            selectGame(game);
            handleDelete();
        });

        HBox actions = new HBox(8, editButton, qrButton, deleteButton);
        actions.getStyleClass().add("card__actions");

        body.getChildren().addAll(title, subtitle, actions);
        cardBox.getChildren().addAll(mediaWrapper, body);

        cardBox.setOnMouseClicked(event -> selectGame(game));
        return cardBox;
    }

    private void showGameQr(GameModel game) {
        if (game == null) {
            return;
        }
        String slugOrId = safeText(game.getSlug());
        if (slugOrId.isBlank()) {
            slugOrId = game.getGameId() == null ? "" : Integer.toString(game.getGameId());
        }
        if (slugOrId.isBlank()) {
            AlertUtils.warning("QR Code", "Jeu invalide (id/slug manquant).");
            return;
        }

        String local = LocalQrWebServer.deviceBaseUrl();
        String baseUrl = local != null ? local : UrlUtils.toDeviceAccessibleBaseUrl(AppConfig.qrBaseUrl());
        String url = baseUrl + "/games/" + slugOrId;
        String header = safe(game.getName()) + " (" + slugOrId + ")";
        QrCodeDialog.show("QR Code - Jeu", header, url);
    }

    private void selectGame(GameModel game) {
        selectedGame = game;
        nameField.setText(game.getName());
        publisherField.setText(game.getPublisher());
        slugField.setText(game.getSlug());
        statusField.setText(game.getStatus());
        if (descriptionField != null) {
            descriptionField.setText(game.getDescription());
        }
        formCoverName = safeNullIfBlank(game.getCoverName());
        refreshCoverUi();

        if (game.getCategoryId() != null) {
            categories.stream()
                .filter(c -> c.getCategoryId() != null && c.getCategoryId().equals(game.getCategoryId()))
                .findFirst()
                .ifPresent(c -> categoryBox.getSelectionModel().select(c));
        } else {
            categoryBox.getSelectionModel().clearSelection();
        }

        applySelectionStyle();
    }

    @FXML
    private void chooseCover() {
        Window window = gamesCardsPane == null || gamesCardsPane.getScene() == null
                ? null
                : gamesCardsPane.getScene().getWindow();
        if (window == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer une cover (image)");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );
        java.io.File file = chooser.showOpenDialog(window);
        if (file == null) {
            return;
        }

        try {
            formCoverName = coverStorageService.storeCover(file.toPath());
            refreshCoverUi();
            AlertUtils.info("Cover", "Image importee.");
        } catch (Exception ex) {
            AlertUtils.error("Cover", "Import echoue.\n" + ex.getMessage());
        }
    }

    @FXML
    private void removeCover() {
        formCoverName = null;
        refreshCoverUi();
    }

    @FXML
    private void autoFillWithAi() {
        String name = safeText(nameField == null ? "" : nameField.getText());
        if (name.isBlank()) {
            AlertUtils.warning("IA", "Entrez d'abord le nom du jeu.");
            return;
        }

        boolean needsSlug = slugField != null && safeText(slugField.getText()).isBlank();
        boolean needsStatus = statusField != null && safeText(statusField.getText()).isBlank();
        boolean needsPublisher = publisherField != null && safeText(publisherField.getText()).isBlank();
        boolean needsDesc = descriptionField != null && safeText(descriptionField.getText()).isBlank();
        if (!needsSlug && !needsStatus && !needsPublisher && !needsDesc) {
            AlertUtils.info("IA", "Aucun champ vide a remplir.");
            return;
        }

        if (!aiChatService.isConfigured()) {
            appendChat("System", "API IA non configuree. Ajoutez PULSE_AI_ENDPOINT (ou ai.endpoint) puis relancez l'app.");
            refreshAiStatus();
            return;
        }

        String prompt = """
                Auto-remplis uniquement les champs vides d'un formulaire de jeu video.
                Reponds exactement en 4 lignes (pas de texte en plus):
                Slug: <slug-en-minuscules-avec-tirets>
                Statut: <DRAFT ou PUBLISHED>
                Editeur: <nom court>
                Description: <1 phrase courte>
                """.trim();

        appendChat("Vous", "IA: Auto-remplir les champs vides");
        Map<String, String> context = buildAiContext();

        Thread worker = new Thread(() -> {
            try {
                String reply = aiChatService.sendMessage(prompt, context);
                Platform.runLater(() -> {
                    appendChat("IA", reply);
                    applyAutoFillReply(reply);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                String details = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "(no message)" : ex.getMessage());
                Platform.runLater(() -> appendChat("System", "Erreur IA: " + details));
            }
        }, "ai-autofill-admin-games");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void openStats() {
        Navigator.goTo("admin_stats");
    }

    private void applyAutoFillReply(String reply) {
        if (reply == null || reply.isBlank()) {
            return;
        }

        String slug = firstNonBlank(extractLineValue(reply, "slug"), extractLineValue(reply, "lien"));
        String status = firstNonBlank(extractLineValue(reply, "statut"), extractLineValue(reply, "status"));
        String publisher = extractLineValue(reply, "editeur");
        if (publisher.isBlank()) {
            publisher = extractLineValue(reply, "éditeur");
        }
        if (publisher.isBlank()) {
            publisher = extractLineValue(reply, "publisher");
        }
        String desc = extractLineValue(reply, "description");

        if (slugField != null && safeText(slugField.getText()).isBlank()) {
            String candidate = slugifyValue(firstNonBlank(slug, safeText(nameField == null ? "" : nameField.getText())));
            slugField.setText(makeUniqueSlug(candidate));
        }
        if (statusField != null && safeText(statusField.getText()).isBlank()) {
            statusField.setText(normalizeAiStatus(status));
        }
        if (publisherField != null && safeText(publisherField.getText()).isBlank() && publisher != null && !publisher.isBlank()) {
            publisherField.setText(publisher.trim());
        }
        if (descriptionField != null && safeText(descriptionField.getText()).isBlank() && desc != null && !desc.isBlank()) {
            descriptionField.setText(desc.trim());
        }
    }

    private String makeUniqueSlug(String slug) {
        String candidate = safeText(slug);
        if (candidate.isBlank()) {
            return "";
        }
        try {
            if (gameRepo.findBySlug(candidate) == null) {
                return candidate;
            }
        } catch (Exception ignored) {
            return candidate;
        }
        return candidate + "-" + (System.currentTimeMillis() % 1000);
    }

    private static String normalizeAiStatus(String raw) {
        String v = safeText(raw).toUpperCase(Locale.ROOT);
        if (v.contains("PUBLISHED")) {
            return "PUBLISHED";
        }
        return "DRAFT";
    }

    private static String extractLineValue(String text, String keyLower) {
        if (text == null || text.isBlank() || keyLower == null || keyLower.isBlank()) {
            return "";
        }
        String needle = keyLower.trim().toLowerCase(Locale.ROOT);
        for (String line : text.split("\\R")) {
            String t = line == null ? "" : line.trim();
            String lower = t.toLowerCase(Locale.ROOT);
            if (lower.startsWith(needle + ":")) {
                int idx = t.indexOf(':');
                return idx < 0 ? "" : t.substring(idx + 1).trim();
            }
        }
        return "";
    }

    private static String slugifyValue(String value) {
        if (value == null) {
            return "";
        }
        String out = value.trim().toLowerCase(Locale.ROOT);
        out = out.replaceAll("[^a-z0-9]+", "-");
        out = out.replaceAll("-{2,}", "-");
        out = out.replaceAll("^-+", "");
        out = out.replaceAll("-+$", "");
        return out;
    }

    private void refreshCoverUi() {
        if (coverPathLabel != null) {
            coverPathLabel.setText(formCoverName == null || formCoverName.isBlank() ? "-" : formCoverName);
        }
        if (coverPreview != null) {
            String imagePath = firstNonBlank(formCoverName, DEFAULT_IMAGE);
            coverPreview.setStyle(ImageResolver.toBackgroundStyle(imagePath));
        }
    }

    private void clearSelected() {
        selectedGame = null;
        applySelectionStyle();
    }

    private void applySelectionStyle() {
        for (VBox card : cardById.values()) {
            card.getStyleClass().remove("is-selected");
        }
        if (selectedGame == null || selectedGame.getGameId() == null) {
            return;
        }
        VBox card = cardById.get(selectedGame.getGameId());
        if (card != null) {
            card.getStyleClass().add("is-selected");
        }
    }

    private void rebuildFilterCategories() {
        filterCategories.setAll(ALL_CATEGORIES);
        filterCategories.addAll(categories);
        if (categoryFilterBox != null && categoryFilterBox.getSelectionModel().isEmpty()) {
            categoryFilterBox.getSelectionModel().select(ALL_CATEGORIES);
        }
    }

    private void rebuildStatusOptions() {
        List<String> distinct = allGames.stream()
            .map(GameModel::getStatus)
            .filter(s -> s != null && !s.isBlank())
            .map(String::trim)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        statuses.setAll("Tous");
        statuses.addAll(distinct);
        if (statusBox != null && statusBox.getSelectionModel().isEmpty()) {
            statusBox.getSelectionModel().select("Tous");
        }
    }

    private static void sortGames(List<GameModel> games, String sort) {
        if (sort == null || sort.isBlank() || SORT_POPULARITY.equals(sort)) {
            games.sort(Comparator.comparingInt(GameModel::getPopularityScore).reversed()
                .thenComparing(g -> safeText(g.getName())));
            return;
        }
        if (SORT_NAME.equals(sort)) {
            games.sort(Comparator.comparing(g -> safeText(g.getName()).toLowerCase(Locale.ROOT)));
            return;
        }
        if (SORT_NEWEST.equals(sort)) {
            games.sort(Comparator
                .comparing(GameModel::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(g -> safeText(g.getName())));
        }
    }

    private static boolean matchesQuery(GameModel game, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String name = normalize(game.getName());
        String slug = normalize(game.getSlug());
        String desc = normalize(game.getDescription());
        return name.contains(q) || slug.contains(q) || desc.contains(q);
    }

    private static boolean matchesPublisher(GameModel game, String publisher) {
        if (publisher == null || publisher.isBlank()) {
            return true;
        }
        return normalize(game.getPublisher()).contains(publisher);
    }

    private static boolean matchesCategory(GameModel game, CategoryModel category) {
        if (category == null || category.getCategoryId() == null) {
            return true;
        }
        return game.getCategoryId() != null && game.getCategoryId().equals(category.getCategoryId());
    }

    private static boolean matchesStatus(GameModel game, String status) {
        if (status == null || status.isBlank() || "Tous".equals(status)) {
            return true;
        }
        return status.equalsIgnoreCase(safeText(game.getStatus()));
    }

    private static String resolveStatus(String rawStatus) {
        String status = safeText(rawStatus);
        return status.isBlank() ? "DRAFT" : status;
    }

    private static String resolveSlug(String rawSlug, String rawName) {
        String slug = safeText(rawSlug);
        if (!slug.isBlank()) {
            return slug;
        }
        String name = safeText(rawName);
        if (name.isBlank()) {
            return "";
        }
        return name.toLowerCase().replaceAll("\\s+", "-");
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeNullIfBlank(String value) {
        String t = safeText(value);
        return t.isBlank() ? null : t;
    }

    private static String firstNonBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return fallback;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static StringConverter<CategoryModel> categoryConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(CategoryModel object) {
                return object == null ? "" : object.getName();
            }

            @Override
            public CategoryModel fromString(String string) {
                return null;
            }
        };
    }
}

