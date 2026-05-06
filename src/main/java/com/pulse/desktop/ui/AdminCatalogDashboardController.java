package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminCatalogDashboardController implements RouteAwarePage {
    @FXML
    private Label totalGamesLabel;
    @FXML
    private Label publicationRateLabel;
    @FXML
    private Label reviewDelayLabel;
    @FXML
    private Label pipelineLabel;
    @FXML
    private Label monthLabel;

    @FXML
    private TextField categorySearchField;

    @FXML
    private VBox categoriesBox;
    @FXML
    private VBox trendingBox;

    private List<CategoryStat> topCategoryRows = List.of();

    @FXML
    public void initialize() {
        if (categorySearchField != null) {
            categorySearchField.textProperty().addListener((obs, oldValue, newValue) -> renderTopCategories());
        }
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        refresh();
    }

    @FXML
    private void refresh() {
        loadKpis();
        loadTopCategories();
        loadTrendingGames();
    }

    @FXML
    private void goGames() {
        Navigator.goTo("admin_games");
    }

    @FXML
    private void goCategories() {
        Navigator.goTo("admin_categories");
    }

    private void loadKpis() {
        int total = 0;
        int published = 0;
        int pending = 0;
        int draft = 0;
        int archived = 0;
        double reviewDelay = 0.0;

        try (Connection connection = Jdbc.open()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT status, COUNT(*) AS total FROM games GROUP BY status");
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    int count = rs.getInt("total");
                    total += count;
                    if ("PUBLISHED".equalsIgnoreCase(status)) {
                        published += count;
                    } else if ("PENDING".equalsIgnoreCase(status)) {
                        pending += count;
                    } else if ("DRAFT".equalsIgnoreCase(status)) {
                        draft += count;
                    } else if ("ARCHIVED".equalsIgnoreCase(status)) {
                        archived += count;
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COALESCE(AVG(TIMESTAMPDIFF(HOUR, g.created_at, COALESCE(g.reviewed_at, NOW()))), 0) AS avg_delay
                    FROM games g
                    WHERE g.status IN ('PENDING', 'PUBLISHED', 'ARCHIVED')
                    """);
                 ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    reviewDelay = rs.getDouble("avg_delay");
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("KPI catalogue", "Chargement KPI impossible.\n" + ex.getMessage());
        }

        double publicationRate = total <= 0 ? 0.0 : (published * 100.0 / total);

        totalGamesLabel.setText(Integer.toString(total));
        publicationRateLabel.setText(String.format("%.2f%%", publicationRate));
        reviewDelayLabel.setText(String.format("%.1fh", reviewDelay));
        pipelineLabel.setText(pending + " pending | Draft: " + draft + " | Archived: " + archived);
    }

    private void loadTopCategories() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        monthLabel.setText(monthStart.getMonthValue() + "/" + monthStart.getYear());

        List<CategoryStat> rows = new ArrayList<>();
        String from = monthStart + " 00:00:00";

        try (Connection connection = Jdbc.open()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT c.name AS category_name, COUNT(gf.user_id) AS total
                    FROM game_favorites gf
                    INNER JOIN games g ON g.game_id = gf.game_id
                    INNER JOIN categories c ON c.category_id = g.category_id
                    WHERE gf.created_at >= ?
                    GROUP BY c.category_id, c.name
                    ORDER BY total DESC, c.name ASC
                    LIMIT 6
                    """)) {
                statement.setString(1, from);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new CategoryStat(rs.getString("category_name"), rs.getInt("total")));
                    }
                }
            }

            if (rows.isEmpty()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT c.name AS category_name, COUNT(g.game_id) AS total
                        FROM games g
                        INNER JOIN categories c ON c.category_id = g.category_id
                        WHERE g.created_at >= ?
                        GROUP BY c.category_id, c.name
                        ORDER BY total DESC, c.name ASC
                        LIMIT 6
                        """)) {
                    statement.setString(1, from);
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            rows.add(new CategoryStat(rs.getString("category_name"), rs.getInt("total")));
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("KPI catalogue", "Chargement top categories impossible.\n" + ex.getMessage());
        }

        topCategoryRows = rows;
        renderTopCategories();
    }

    private void renderTopCategories() {
        categoriesBox.getChildren().clear();

        String q = categorySearchField == null ? "" : safe(categorySearchField.getText());
        List<CategoryStat> rows = topCategoryRows;
        if (!q.isBlank()) {
            String needle = q.toLowerCase(Locale.ROOT);
            rows = rows.stream()
                    .filter(r -> safe(r.categoryName()).toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }

        if (rows.isEmpty()) {
            categoriesBox.getChildren().add(CompetitionUi.emptyState("Aucune categorie ne correspond a la recherche."));
            return;
        }

        int rank = 1;
        for (CategoryStat row : rows) {
            String left = "#" + rank + " | " + CompetitionUi.emptySafe(row.categoryName());
            String right = Integer.toString(row.total());
            categoriesBox.getChildren().add(CompetitionUi.listRow(left, right));
            rank++;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void loadTrendingGames() {
        trendingBox.getChildren().clear();

        String sql = """
                SELECT
                    g.game_id,
                    g.name,
                    g.slug,
                    g.status,
                    g.views_count,
                    g.favorites_count,
                    g.popularity_score,
                    COALESCE(i.file_url, g.cover_name, '') AS cover_url
                FROM games g
                LEFT JOIN images i ON i.image_id = g.cover_image_id
                WHERE g.status <> 'ARCHIVED'
                ORDER BY g.popularity_score DESC, g.favorites_count DESC, g.views_count DESC, g.name ASC
                LIMIT 8
                """;

        List<TrendingRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new TrendingRow(
                        rs.getInt("game_id"),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getString("status"),
                        rs.getInt("views_count"),
                        rs.getInt("favorites_count"),
                        rs.getInt("popularity_score"),
                        rs.getString("cover_url")
                ));
            }
        } catch (SQLException ex) {
            AlertUtils.error("KPI catalogue", "Chargement trending impossible.\n" + ex.getMessage());
        }

        if (rows.isEmpty()) {
            trendingBox.getChildren().add(CompetitionUi.emptyState("Aucun jeu tendance."));
            return;
        }

        for (TrendingRow row : rows) {
            trendingBox.getChildren().add(buildTrendingRow(row));
        }
    }

    private HBox buildTrendingRow(TrendingRow row) {
        ImageView cover = new ImageView();
        cover.setFitWidth(64);
        cover.setFitHeight(64);
        cover.setPreserveRatio(false);
        cover.getStyleClass().add("post-image-preview");

        String fallback = "https://picsum.photos/seed/pulse_catalog_" + row.gameId() + "/280/280";
        String path = ImageResolver.toExternalForm(row.coverUrl());
        if (path == null || path.isBlank()) {
            path = fallback;
        }
        Image image = new Image(path, true);
        if (image.isError()) {
            image = new Image(fallback, true);
        }
        cover.setImage(image);

        String line1 = CompetitionUi.emptySafe(row.name()) + " | " + CompetitionUi.emptySafe(row.slug());
        String line2 = "Status: " + CompetitionUi.emptySafe(row.status())
                + " | Vues: " + row.viewsCount()
                + " | Favoris: " + row.favoritesCount()
                + " | Score: " + row.popularityScore();

        Label title = new Label(line1);
        title.getStyleClass().add("card__title");
        title.setWrapText(true);

        Label meta = new Label(line2);
        meta.getStyleClass().add("list-item-meta");
        meta.setWrapText(true);

        Button open = new Button("Voir");
        open.getStyleClass().add("btn-ghost");
        open.setOnAction(event -> Navigator.goTo("admin_games"));

        VBox text = new VBox(6, title, meta, open);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox rowBox = new HBox(12, cover, text);
        rowBox.getStyleClass().add("list-item");
        return rowBox;
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

    private record CategoryStat(String categoryName, int total) {
    }

    private record TrendingRow(
            int gameId,
            String name,
            String slug,
            String status,
            int viewsCount,
            int favoritesCount,
            int popularityScore,
            String coverUrl
    ) {
    }
}
