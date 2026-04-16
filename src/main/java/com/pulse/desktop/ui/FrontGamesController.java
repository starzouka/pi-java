package com.pulse.desktop.ui;

import com.pulse.desktop.model.CategoryModel;
import com.pulse.desktop.model.GameModel;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.model.SimpleCard;
import com.pulse.desktop.repo.CategoryRepository;
import com.pulse.desktop.repo.GameRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FrontGamesController implements RouteAwarePage {
    private static final CategoryModel ALL_CATEGORIES = new CategoryModel(null, "Toutes", null, null, null);
    private static final String SORT_POPULARITY = "Popularite";
    private static final String SORT_NAME = "Nom A-Z";
    private static final String SORT_NEWEST = "Plus recents";

    @FXML private Label routeNameLabel;
    @FXML private Label routeIdLabel;
    @FXML private Label routeDescriptionLabel;
    @FXML private Label sectionHeadingLabel;
    @FXML private Label sectionSubheadingLabel;

    @FXML private VBox pageRoot;
    @FXML private FlowPane cardsPane;

    @FXML private TextField qField;
    @FXML private ComboBox<CategoryModel> categoryBox;
    @FXML private TextField publisherField;
    @FXML private ComboBox<String> statusBox;
    @FXML private ComboBox<String> sortBox;

    @FXML private Button filterButton;
    @FXML private Button resetButton;

    private final GameRepository gameRepo = new GameRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();

    private final ObservableList<CategoryModel> categories = FXCollections.observableArrayList();
    private final ObservableList<String> statuses = FXCollections.observableArrayList();
    private final ObservableList<String> sorts = FXCollections.observableArrayList();

    private List<GameModel> allGames = new ArrayList<>();
    private RouteDefinition routeDefinition;

    @FXML
    public void initialize() {
        categoryBox.setItems(categories);
        statusBox.setItems(statuses);
        sortBox.setItems(sorts);

        sorts.setAll(SORT_POPULARITY, SORT_NEWEST, SORT_NAME);
        sortBox.getSelectionModel().select(SORT_POPULARITY);

        filterButton.setOnAction(event -> applyFilters());
        resetButton.setOnAction(event -> resetFilters());

        loadLookups();
        loadGames();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        this.routeDefinition = routeDefinition;
        if (routeNameLabel != null) {
            routeNameLabel.setText("Catalogue des jeux");
        }
        if (routeIdLabel != null) {
            routeIdLabel.setText("Route Symfony: " + routeDefinition.id());
        }
        if (routeDescriptionLabel != null) {
            routeDescriptionLabel.setText("Catalogue charge depuis la base pulsedb (filtres + cartes).");
        }
        if (sectionHeadingLabel != null) {
            sectionHeadingLabel.setText("RESULTATS JEUX");
        }
        if (sectionSubheadingLabel != null) {
            sectionSubheadingLabel.setText("Cliquez sur une carte pour ouvrir le detail du jeu.");
        }
    }

    @FXML
    private void goHome() {
        Navigator.goTo("front_home");
    }

    @FXML
    private void goToTournaments() {
        Navigator.goTo("front_tournaments");
    }

    @FXML
    private void refreshGames() {
        loadLookups();
        loadGames();
    }

    private void loadLookups() {
        try {
            List<CategoryModel> loaded = categoryRepo.findAll();
            categories.setAll(ALL_CATEGORIES);
            categories.addAll(loaded);

            if (categoryBox.getSelectionModel().isEmpty()) {
                categoryBox.getSelectionModel().select(ALL_CATEGORIES);
            }
        } catch (Exception e) {
            AlertUtils.error("Erreur DB", "Chargement categories echoue: " + e.getMessage());
        }
    }

    private void loadGames() {
        try {
            allGames = gameRepo.findAll();
            rebuildStatusOptions();
            applyFilters();
        } catch (Exception e) {
            AlertUtils.error("Erreur DB", "Chargement jeux echoue: " + e.getMessage());
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

        if (statusBox.getSelectionModel().isEmpty()) {
            statusBox.getSelectionModel().select("Tous");
        }
    }

    @FXML
    private void applyFilters() {
        String q = normalize(qField.getText());
        String publisher = normalize(publisherField.getText());
        CategoryModel selectedCategory = categoryBox.getSelectionModel().getSelectedItem();
        String status = statusBox.getSelectionModel().getSelectedItem();
        String sort = sortBox.getSelectionModel().getSelectedItem();

        List<GameModel> filtered = allGames.stream()
                .filter(game -> matchesQuery(game, q))
                .filter(game -> matchesPublisher(game, publisher))
                .filter(game -> matchesCategory(game, selectedCategory))
                .filter(game -> matchesStatus(game, status))
                .collect(Collectors.toList());

        sortGames(filtered, sort);
        renderGames(filtered);
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        publisherField.clear();
        categoryBox.getSelectionModel().select(ALL_CATEGORIES);
        statusBox.getSelectionModel().select("Tous");
        sortBox.getSelectionModel().select(SORT_POPULARITY);
        applyFilters();
    }

    private void renderGames(List<GameModel> games) {
        cardsPane.getChildren().clear();
        if (games.isEmpty()) {
            Label empty = new Label("Aucun jeu ne correspond aux filtres.");
            empty.getStyleClass().add("muted");
            cardsPane.getChildren().add(empty);
            return;
        }

        for (GameModel game : games) {
            String subtitle = "Editeur: " + safe(game.getPublisher())
                    + " | Popularite: " + game.getPopularityScore();
            String badgeLeft = safe(game.getCategoryName());
            String badgeRight = safe(game.getStatus());

            SimpleCard card = new SimpleCard(
                    safe(game.getName()),
                    subtitle,
                    badgeLeft,
                    badgeRight,
                    "front_game_detail",
                    null,
                    "game"
            );

            cardsPane.getChildren().add(CardFactory.create(card, () -> openGame(game)));
        }
    }

    private void openGame(GameModel game) {
        RouteContext.setSelectedGameId(game.getGameId());
        Navigator.goTo("front_game_detail");
    }

    private static void sortGames(List<GameModel> games, String sort) {
        if (sort == null || sort.isBlank() || SORT_POPULARITY.equals(sort)) {
            games.sort(Comparator.comparingInt(GameModel::getPopularityScore).reversed()
                    .thenComparing(g -> safe(g.getName())));
            return;
        }
        if (SORT_NAME.equals(sort)) {
            games.sort(Comparator.comparing(g -> safe(g.getName()).toLowerCase(Locale.ROOT)));
            return;
        }
        if (SORT_NEWEST.equals(sort)) {
            games.sort(Comparator
                    .comparing(GameModel::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed()
                    .thenComparing(g -> safe(g.getName())));
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
        return status.equalsIgnoreCase(safe(game.getStatus()));
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
