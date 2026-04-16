package com.pulse.desktop.ui;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FrontGamesController implements RouteAwarePage {
    @FXML
    private TextField qField;
    @FXML
    private ComboBox<LookupItem> categoryCombo;
    @FXML
    private ComboBox<String> publisherCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private CheckBox activeOnlyCheck;

    @FXML
    private Label resultCountLabel;
    @FXML
    private VBox trendingBox;
    @FXML
    private VBox gamesBox;

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        loadLookups();
        refresh();
    }

    @FXML
    public void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList("name", "popular", "latest", "publisher", "category"));
        sortCombo.getSelectionModel().select("name");
    }

    @FXML
    private void applyFilters() {
        refresh();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        activeOnlyCheck.setSelected(false);
        sortCombo.getSelectionModel().select("name");
        if (!categoryCombo.getItems().isEmpty()) {
            categoryCombo.getSelectionModel().select(0);
        }
        if (!publisherCombo.getItems().isEmpty()) {
            publisherCombo.getSelectionModel().select(0);
        }
        refresh();
    }

    @FXML
    private void goTournaments() {
        Navigator.goTo("front_tournaments");
    }

    private void loadLookups() {
        Integer selectedCategory = categoryCombo.getValue() == null ? null : categoryCombo.getValue().getId();
        String selectedPublisher = publisherCombo.getValue();

        List<LookupItem> categories = new ArrayList<>();
        categories.add(new LookupItem(0, "Toutes categories"));

        List<String> publishers = new ArrayList<>();
        publishers.add("");

        try (Connection connection = Jdbc.open()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT category_id, name FROM categories ORDER BY name ASC");
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    categories.add(new LookupItem(rs.getInt("category_id"), rs.getString("name")));
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT DISTINCT publisher
                    FROM games
                    WHERE publisher IS NOT NULL
                      AND publisher <> ''
                      AND status <> 'ARCHIVED'
                    ORDER BY publisher ASC
                    """);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    publishers.add(rs.getString("publisher"));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Jeux", "Impossible de charger les filtres.\n" + ex.getMessage());
        }

        categoryCombo.setItems(FXCollections.observableArrayList(categories));
        if (selectedCategory != null) {
            for (LookupItem item : categories) {
                if (item.getId() == selectedCategory) {
                    categoryCombo.getSelectionModel().select(item);
                    break;
                }
            }
        }
        if (categoryCombo.getValue() == null) {
            categoryCombo.getSelectionModel().select(0);
        }

        publisherCombo.setItems(FXCollections.observableArrayList(publishers));
        if (selectedPublisher != null && publishers.contains(selectedPublisher)) {
            publisherCombo.getSelectionModel().select(selectedPublisher);
        }
        if (publisherCombo.getValue() == null) {
            publisherCombo.getSelectionModel().select(0);
        }
    }

    private void refresh() {
        List<GameRow> rows = loadCatalogRows();
        resultCountLabel.setText(rows.size() + " jeu(x)");
        renderCatalog(rows);
        renderTrending(loadTrendingRows());
    }

    private List<GameRow> loadCatalogRows() {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    g.game_id,
                    g.name,
                    g.slug,
                    g.description,
                    g.publisher,
                    g.status,
                    g.popularity_score,
                    g.views_count,
                    g.favorites_count,
                    c.category_id,
                    c.name AS category_name,
                    COALESCE(i.file_url, g.cover_name, '') AS cover_url
                FROM games g
                INNER JOIN categories c ON c.category_id = g.category_id
                LEFT JOIN images i ON i.image_id = g.cover_image_id
                WHERE g.status <> 'ARCHIVED'
                """);

        List<Object> params = new ArrayList<>();

        String q = safe(qField.getText());
        if (!q.isBlank()) {
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            sql.append("""
                     AND (
                        LOWER(g.name) LIKE ?
                        OR LOWER(COALESCE(g.description, '')) LIKE ?
                        OR LOWER(COALESCE(g.publisher, '')) LIKE ?
                        OR LOWER(COALESCE(c.name, '')) LIKE ?
                     )
                    """);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        LookupItem category = categoryCombo.getValue();
        if (category != null && category.getId() > 0) {
            sql.append(" AND g.category_id = ? ");
            params.add(category.getId());
        }

        String publisher = safe(publisherCombo.getValue());
        if (!publisher.isBlank()) {
            sql.append(" AND LOWER(COALESCE(g.publisher, '')) = ? ");
            params.add(publisher.toLowerCase(Locale.ROOT));
        }

        if (activeOnlyCheck.isSelected()) {
            sql.append("""
                     AND EXISTS (
                        SELECT 1
                        FROM tournaments t
                        WHERE t.game_id = g.game_id
                          AND t.status IN ('OPEN', 'ONGOING')
                     )
                    """);
        }

        String sort = safe(sortCombo.getValue());
        switch (sort) {
            case "latest" -> sql.append(" ORDER BY g.created_at DESC, g.name ASC ");
            case "publisher" -> sql.append(" ORDER BY g.publisher ASC, g.name ASC ");
            case "category" -> sql.append(" ORDER BY c.name ASC, g.name ASC ");
            case "popular" -> sql.append(" ORDER BY g.popularity_score DESC, g.favorites_count DESC, g.views_count DESC, g.name ASC ");
            default -> sql.append(" ORDER BY g.name ASC ");
        }
        sql.append(" LIMIT 240 ");

        List<GameRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new GameRow(
                            rs.getInt("game_id"),
                            rs.getString("name"),
                            rs.getString("slug"),
                            rs.getString("description"),
                            rs.getString("publisher"),
                            rs.getString("status"),
                            rs.getInt("popularity_score"),
                            rs.getInt("views_count"),
                            rs.getInt("favorites_count"),
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            rs.getString("cover_url")
                    ));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Jeux", "Chargement du catalogue impossible.\n" + ex.getMessage());
        }

        return rows;
    }

    private List<GameRow> loadTrendingRows() {
        String sql = """
                SELECT
                    g.game_id,
                    g.name,
                    g.slug,
                    g.description,
                    g.publisher,
                    g.status,
                    g.popularity_score,
                    g.views_count,
                    g.favorites_count,
                    c.category_id,
                    c.name AS category_name,
                    COALESCE(i.file_url, g.cover_name, '') AS cover_url
                FROM games g
                INNER JOIN categories c ON c.category_id = g.category_id
                LEFT JOIN images i ON i.image_id = g.cover_image_id
                WHERE g.status <> 'ARCHIVED'
                ORDER BY g.popularity_score DESC, g.favorites_count DESC, g.views_count DESC, g.name ASC
                LIMIT 6
                """;

        List<GameRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new GameRow(
                        rs.getInt("game_id"),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getString("description"),
                        rs.getString("publisher"),
                        rs.getString("status"),
                        rs.getInt("popularity_score"),
                        rs.getInt("views_count"),
                        rs.getInt("favorites_count"),
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getString("cover_url")
                ));
            }
        } catch (SQLException ex) {
            AlertUtils.error("Jeux", "Chargement des tendances impossible.\n" + ex.getMessage());
        }
        return rows;
    }

    private void renderCatalog(List<GameRow> rows) {
        gamesBox.getChildren().clear();
        if (rows.isEmpty()) {
            gamesBox.getChildren().add(CompetitionUi.emptyState("Aucun jeu ne correspond aux filtres."));
            return;
        }

        Map<Integer, Integer> totalByGame = loadTournamentCountByGame(rows, false);
        Map<Integer, Integer> activeByGame = loadTournamentCountByGame(rows, true);

        for (GameRow row : rows) {
            int totalTournaments = totalByGame.getOrDefault(row.gameId(), 0);
            int activeTournaments = activeByGame.getOrDefault(row.gameId(), 0);
            gamesBox.getChildren().add(buildGameCard(row, totalTournaments, activeTournaments, false));
        }
    }

    private void renderTrending(List<GameRow> rows) {
        trendingBox.getChildren().clear();
        if (rows.isEmpty()) {
            trendingBox.getChildren().add(CompetitionUi.emptyState("Aucun jeu tendance pour le moment."));
            return;
        }

        Map<Integer, Integer> activeByGame = loadTournamentCountByGame(rows, true);
        rows.sort(Comparator.comparingInt(GameRow::popularityScore).reversed());

        for (GameRow row : rows) {
            int activeTournaments = activeByGame.getOrDefault(row.gameId(), 0);
            trendingBox.getChildren().add(buildGameCard(row, activeTournaments, activeTournaments, true));
        }
    }

    private HBox buildGameCard(GameRow row, int totalTournaments, int activeTournaments, boolean trending) {
        ImageView cover = new ImageView();
        cover.setFitWidth(104);
        cover.setFitHeight(66);
        cover.setPreserveRatio(false);
        cover.getStyleClass().add("post-image-preview");

        String fallback = "https://picsum.photos/seed/pulse_game_" + row.gameId() + "/640/360";
        String external = ImageResolver.toExternalForm(row.coverUrl());
        if (external == null || external.isBlank()) {
            external = fallback;
        }
        Image image = new Image(external, true);
        if (image.isError()) {
            image = new Image(fallback, true);
        }
        cover.setImage(image);

        String line1 = CompetitionUi.emptySafe(row.name()) + " | " + CompetitionUi.emptySafe(row.categoryName());
        String line2 = "Publisher: " + CompetitionUi.emptySafe(row.publisher())
                + " | Tournois: " + totalTournaments
                + " | Actifs: " + activeTournaments
                + " | Vues: " + row.viewsCount()
                + " | Favoris: " + row.favoritesCount()
                + " | Score: " + row.popularityScore()
                + (trending ? " | Trending" : "");

        Label title = new Label(line1);
        title.getStyleClass().add("card__title");
        title.setWrapText(true);

        Label meta = new Label(line2);
        meta.getStyleClass().add("list-item-meta");
        meta.setWrapText(true);

        Button detail = new Button("Detail");
        detail.getStyleClass().add("btn-ghost");
        detail.setOnAction(event -> {
            RouteContext.putInt(RouteContext.KEY_GAME_ID, row.gameId());
            Navigator.goTo("front_game_detail");
        });

        Button tournaments = new Button("Tournois");
        tournaments.getStyleClass().add("btn-primary");
        tournaments.setOnAction(event -> {
            RouteContext.putInt(RouteContext.KEY_GAME_ID, row.gameId());
            Navigator.goTo("front_tournaments");
        });

        HBox actions = new HBox(8, detail, tournaments);

        VBox textBox = new VBox(6, title, meta, actions);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox rowBox = new HBox(12, cover, textBox);
        rowBox.getStyleClass().add("list-item");
        return rowBox;
    }

    private Map<Integer, Integer> loadTournamentCountByGame(List<GameRow> games, boolean activeOnly) {
        Map<Integer, Integer> map = new HashMap<>();
        List<Integer> ids = new ArrayList<>();
        for (GameRow row : games) {
            ids.add(row.gameId());
        }
        if (ids.isEmpty()) {
            return map;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        StringBuilder sql = new StringBuilder("SELECT game_id, COUNT(*) AS total FROM tournaments WHERE game_id IN (")
                .append(placeholders)
                .append(")");
        if (activeOnly) {
            sql.append(" AND status IN ('OPEN', 'ONGOING')");
        }
        sql.append(" GROUP BY game_id ");

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (Integer id : ids) {
                statement.setInt(index++, id);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("game_id"), rs.getInt("total"));
                }
            }
        } catch (SQLException ignored) {
            // graceful fallback with 0 counts
        }

        return map;
    }

    private static void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        int i = 1;
        for (Object value : params) {
            if (value instanceof Integer intValue) {
                statement.setInt(i++, intValue);
            } else if (value instanceof String text) {
                statement.setString(i++, text);
            } else {
                statement.setObject(i++, value);
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record GameRow(
            int gameId,
            String name,
            String slug,
            String description,
            String publisher,
            String status,
            int popularityScore,
            int viewsCount,
            int favoritesCount,
            int categoryId,
            String categoryName,
            String coverUrl
    ) {
    }
}
