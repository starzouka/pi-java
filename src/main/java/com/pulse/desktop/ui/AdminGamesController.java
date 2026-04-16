package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.ExportService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminGamesController implements RouteAwarePage {
    private static final LookupItem ANY = new LookupItem(0, "ALL");

    @FXML
    private Label formTitleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<LookupItem> categoryCombo;
    @FXML
    private TextField publisherField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextField coverNameField;
    @FXML
    private TextField coverUrlField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label aiMetaLabel;
    @FXML
    private Label formFeedbackLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelEditButton;

    @FXML
    private TextField qField;
    @FXML
    private ComboBox<LookupItem> categoryFilterCombo;
    @FXML
    private TextField publisherFilterField;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private ComboBox<String> directionCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private VBox gamesListBox;

    private final ExportService exportService = new ExportService();
    private Integer editingGameId;
    private List<GameRow> currentRows = List.of();

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("DRAFT", "PENDING", "PUBLISHED", "ARCHIVED"));
        statusCombo.getSelectionModel().select("DRAFT");

        statusFilterCombo.setItems(FXCollections.observableArrayList("", "DRAFT", "PENDING", "PUBLISHED", "ARCHIVED"));
        statusFilterCombo.getSelectionModel().select(0);

        sortCombo.setItems(FXCollections.observableArrayList("created_at", "id", "name", "slug", "status", "category", "publisher", "views", "favorites", "score"));
        sortCombo.getSelectionModel().select("created_at");

        directionCombo.setItems(FXCollections.observableArrayList("desc", "asc"));
        directionCombo.getSelectionModel().select("desc");

        resetForm();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        loadCategories();
        resetForm();
        refresh();
    }

    @FXML
    private void saveGame() {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }

        String name = safe(nameField.getText());
        if (name.isBlank()) {
            formFeedbackLabel.setText("Le nom du jeu est obligatoire.");
            return;
        }

        LookupItem category = categoryCombo.getValue();
        if (category == null || category.getId() <= 0) {
            formFeedbackLabel.setText("Categorie invalide.");
            return;
        }

        String status = safe(statusCombo.getValue()).toUpperCase(Locale.ROOT);
        if (!List.of("DRAFT", "PENDING", "PUBLISHED", "ARCHIVED").contains(status)) {
            status = "DRAFT";
        }

        String publisher = safe(publisherField.getText());
        String description = safe(descriptionArea.getText());
        String coverName = safe(coverNameField.getText());
        String coverUrl = safe(coverUrlField.getText());

        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Integer coverImageId = resolveImageId(connection, coverUrl, admin.getUserId(), name);
                LocalDateTime reviewedAt = "DRAFT".equals(status) ? null : LocalDateTime.now();

                if (editingGameId == null) {
                    String tempSlug = "pending-game-" + System.nanoTime();
                    int gameId;
                    String insertSql = """
                            INSERT INTO games (
                                category_id, name, slug, description, publisher, status,
                                popularity_score, views_count, favorites_count,
                                cover_name, cover_image_id, created_at, reviewed_at
                            ) VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0, ?, ?, NOW(), ?)
                            """;

                    try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setInt(1, category.getId());
                        statement.setString(2, name);
                        statement.setString(3, tempSlug);
                        statement.setString(4, nullIfBlank(description));
                        statement.setString(5, nullIfBlank(publisher));
                        statement.setString(6, status);
                        statement.setString(7, nullIfBlank(coverName));
                        if (coverImageId == null) {
                            statement.setObject(8, null);
                        } else {
                            statement.setInt(8, coverImageId);
                        }
                        if (reviewedAt == null) {
                            statement.setObject(9, null);
                        } else {
                            statement.setTimestamp(9, Timestamp.valueOf(reviewedAt));
                        }
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("Creation jeu impossible.");
                            }
                            gameId = keys.getInt(1);
                        }
                    }

                    String finalSlug = buildSlug(name, gameId);
                    try (PreparedStatement statement = connection.prepareStatement("UPDATE games SET slug = ? WHERE game_id = ?")) {
                        statement.setString(1, finalSlug);
                        statement.setInt(2, gameId);
                        statement.executeUpdate();
                    }

                    AlertUtils.info("Admin jeux", "Jeu cree.");
                } else {
                    String updateSql = """
                            UPDATE games
                            SET category_id = ?, name = ?, slug = ?, description = ?, publisher = ?, status = ?,
                                cover_name = ?, cover_image_id = ?,
                                reviewed_at = CASE
                                    WHEN reviewed_at IS NULL AND ? IS NOT NULL THEN ?
                                    ELSE reviewed_at
                                END
                            WHERE game_id = ?
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                        statement.setInt(1, category.getId());
                        statement.setString(2, name);
                        statement.setString(3, buildSlug(name, editingGameId));
                        statement.setString(4, nullIfBlank(description));
                        statement.setString(5, nullIfBlank(publisher));
                        statement.setString(6, status);
                        statement.setString(7, nullIfBlank(coverName));
                        if (coverImageId == null) {
                            statement.setObject(8, null);
                        } else {
                            statement.setInt(8, coverImageId);
                        }
                        if (reviewedAt == null) {
                            statement.setObject(9, null);
                            statement.setObject(10, null);
                        } else {
                            statement.setTimestamp(9, Timestamp.valueOf(reviewedAt));
                            statement.setTimestamp(10, Timestamp.valueOf(reviewedAt));
                        }
                        statement.setInt(11, editingGameId);
                        statement.executeUpdate();
                    }
                    AlertUtils.info("Admin jeux", "Jeu mis a jour.");
                }

                connection.commit();
                resetForm();
                refresh();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin jeux", "Enregistrement impossible (nom/slug deja utilise ou liaison invalide).\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancelEdit() {
        resetForm();
    }

    @FXML
    private void aiSuggest() {
        String currentName = safe(nameField.getText());
        String currentPublisher = safe(publisherField.getText());
        LookupItem selectedCategory = categoryCombo.getValue();
        String categoryName = selectedCategory == null ? "" : safe(selectedCategory.getLabel());

        String suggestion = buildSuggestion(currentName, currentPublisher, categoryName);
        if (suggestion.isBlank()) {
            suggestion = "Jeu e-sport competitif avec progression saisonniere, matchmaking equilibre et suivi des performances.";
        }
        descriptionArea.setText(suggestion);
        if (aiMetaLabel != null) {
            aiMetaLabel.setText("IA Suggestion: fallback local");
        }
    }

    @FXML
    private void aiAutofill() {
        String currentName = safe(nameField.getText());
        LookupItem selectedCategory = categoryCombo.getValue();
        String categoryName = selectedCategory == null ? "" : safe(selectedCategory.getLabel());

        if (currentName.isBlank()) {
            String prefix = categoryName.isBlank() ? "Pulse" : categoryName;
            currentName = prefix + " Arena";
            nameField.setText(currentName);
        }

        if (safe(publisherField.getText()).isBlank()) {
            publisherField.setText("PULSE Studio");
        }

        if (safe(coverNameField.getText()).isBlank()) {
            coverNameField.setText("cover-" + buildSlugBase(currentName));
        }

        if (statusCombo.getValue() == null || safe(statusCombo.getValue()).isBlank()) {
            statusCombo.getSelectionModel().select("DRAFT");
        }

        if (safe(descriptionArea.getText()).isBlank()) {
            descriptionArea.setText(buildSuggestion(currentName, safe(publisherField.getText()), categoryName));
        }

        if (aiMetaLabel != null) {
            aiMetaLabel.setText("IA AutoFill: fallback local");
        }
    }

    @FXML
    private void applyFilters() {
        refresh();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        publisherFilterField.clear();
        if (!categoryFilterCombo.getItems().isEmpty()) {
            categoryFilterCombo.getSelectionModel().select(0);
        }
        statusFilterCombo.getSelectionModel().select(0);
        sortCombo.getSelectionModel().select("created_at");
        directionCombo.getSelectionModel().select("desc");
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
    private void openCatalogDashboard() {
        Navigator.goTo("admin_catalog_dashboard");
    }

    private void loadCategories() {
        List<LookupItem> categories = new ArrayList<>();
        categories.add(ANY);

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement("SELECT category_id, name FROM categories ORDER BY name ASC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                categories.add(new LookupItem(rs.getInt("category_id"), rs.getString("name")));
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin jeux", "Impossible de charger les categories.\n" + ex.getMessage());
        }

        categoryFilterCombo.setItems(FXCollections.observableArrayList(categories));
        categoryFilterCombo.getSelectionModel().select(0);

        List<LookupItem> formCategories = new ArrayList<>(categories);
        formCategories.remove(0);
        categoryCombo.setItems(FXCollections.observableArrayList(formCategories));
        if (!formCategories.isEmpty()) {
            categoryCombo.getSelectionModel().select(0);
        }
    }

    private void refresh() {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    g.game_id,
                    g.name,
                    g.slug,
                    g.status,
                    g.publisher,
                    g.description,
                    g.views_count,
                    g.favorites_count,
                    g.popularity_score,
                    g.created_at,
                    g.reviewed_at,
                    g.cover_name,
                    g.cover_image_id,
                    c.category_id,
                    c.name AS category_name,
                    i.file_url AS cover_url
                FROM games g
                LEFT JOIN categories c ON c.category_id = g.category_id
                LEFT JOIN images i ON i.image_id = g.cover_image_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        String q = safe(qField.getText());
        if (!q.isBlank()) {
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            sql.append("""
                     AND (
                        LOWER(COALESCE(g.name, '')) LIKE ?
                        OR LOWER(COALESCE(g.slug, '')) LIKE ?
                        OR LOWER(COALESCE(g.description, '')) LIKE ?
                        OR LOWER(COALESCE(g.publisher, '')) LIKE ?
                        OR LOWER(COALESCE(c.name, '')) LIKE ?
                     )
                    """);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        LookupItem category = categoryFilterCombo.getValue();
        if (category != null && category.getId() > 0) {
            sql.append(" AND g.category_id = ? ");
            params.add(category.getId());
        }

        String publisher = safe(publisherFilterField.getText());
        if (!publisher.isBlank()) {
            sql.append(" AND LOWER(COALESCE(g.publisher, '')) LIKE ? ");
            params.add("%" + publisher.toLowerCase(Locale.ROOT) + "%");
        }

        String status = safe(statusFilterCombo.getValue()).toUpperCase(Locale.ROOT);
        if (!status.isBlank()) {
            sql.append(" AND g.status = ? ");
            params.add(status);
        }

        String direction = "asc".equalsIgnoreCase(safe(directionCombo.getValue())) ? "ASC" : "DESC";
        String sort = safe(sortCombo.getValue());
        switch (sort) {
            case "id" -> sql.append(" ORDER BY g.game_id ").append(direction);
            case "name" -> sql.append(" ORDER BY g.name ").append(direction).append(", g.game_id DESC");
            case "slug" -> sql.append(" ORDER BY g.slug ").append(direction).append(", g.game_id DESC");
            case "status" -> sql.append(" ORDER BY g.status ").append(direction).append(", g.game_id DESC");
            case "category" -> sql.append(" ORDER BY c.name ").append(direction).append(", g.name ASC");
            case "publisher" -> sql.append(" ORDER BY g.publisher ").append(direction).append(", g.name ASC");
            case "views" -> sql.append(" ORDER BY g.views_count ").append(direction).append(", g.game_id DESC");
            case "favorites" -> sql.append(" ORDER BY g.favorites_count ").append(direction).append(", g.game_id DESC");
            case "score" -> sql.append(" ORDER BY g.popularity_score ").append(direction).append(", g.game_id DESC");
            default -> sql.append(" ORDER BY g.created_at ").append(direction).append(", g.game_id DESC");
        }
        sql.append(" LIMIT 800 ");

        List<GameRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new GameRow(
                            rs.getInt("game_id"),
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            rs.getString("name"),
                            rs.getString("slug"),
                            rs.getString("status"),
                            rs.getString("publisher"),
                            rs.getString("description"),
                            rs.getInt("views_count"),
                            rs.getInt("favorites_count"),
                            rs.getInt("popularity_score"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("reviewed_at")),
                            rs.getString("cover_name"),
                            rs.getObject("cover_image_id") == null ? null : rs.getInt("cover_image_id"),
                            rs.getString("cover_url")
                    ));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin jeux", "Chargement impossible.\n" + ex.getMessage());
            return;
        }

        currentRows = rows;
        resultCountLabel.setText(rows.size() + " resultat(s)");
        renderRows();
    }

    private void renderRows() {
        gamesListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            gamesListBox.getChildren().add(CompetitionUi.emptyState("Aucun jeu trouve."));
            return;
        }

        for (GameRow row : currentRows) {
            Button update = new Button("Update");
            update.getStyleClass().add("btn-ghost");
            update.setOnAction(event -> startEdit(row));

            Button delete = new Button("Delete");
            delete.getStyleClass().add("btn-ghost");
            delete.setOnAction(event -> deleteGame(row.gameId()));

            gamesListBox.getChildren().add(buildGameRow(row, update, delete));
        }
    }

    private HBox buildGameRow(GameRow row, Button update, Button delete) {
        ImageView cover = new ImageView();
        cover.setFitWidth(72);
        cover.setFitHeight(72);
        cover.setPreserveRatio(false);
        cover.getStyleClass().add("post-image-preview");

        String fallback = "https://picsum.photos/seed/pulse_admin_game_" + row.gameId() + "/300/300";
        String path = ImageResolver.toExternalForm(row.coverUrl());
        if (path == null || path.isBlank()) {
            path = ImageResolver.toExternalForm(row.coverName());
        }
        if (path == null || path.isBlank()) {
            path = fallback;
        }

        Image image = new Image(path, true);
        if (image.isError()) {
            image = new Image(fallback, true);
        }
        cover.setImage(image);

        String line1 = "#" + row.gameId() + " | " + CompetitionUi.emptySafe(row.name()) + " | " + CompetitionUi.emptySafe(row.categoryName());
        String line2 = "Slug: " + CompetitionUi.emptySafe(row.slug())
                + " | Status: " + CompetitionUi.emptySafe(row.status())
                + " | Publisher: " + CompetitionUi.emptySafe(row.publisher())
                + " | Vues: " + row.viewsCount()
                + " | Favoris: " + row.favoritesCount()
                + " | Score: " + row.popularityScore()
                + " | Cree le: " + CompetitionUi.fmtDateTime(row.createdAt());

        Label title = new Label(line1);
        title.getStyleClass().add("card__title");
        title.setWrapText(true);

        Label meta = new Label(line2);
        meta.getStyleClass().add("list-item-meta");
        meta.setWrapText(true);

        HBox actions = new HBox(8, update, delete);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox text = new VBox(6, title, meta, actions);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox rowBox = new HBox(12, cover, text, spacer);
        rowBox.getStyleClass().add("list-item");
        return rowBox;
    }

    private void startEdit(GameRow row) {
        editingGameId = row.gameId();
        formTitleLabel.setText("MODIFIER JEU #" + row.gameId());
        saveButton.setText("Mettre a jour");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);

        nameField.setText(safe(row.name()));
        selectById(categoryCombo, row.categoryId());
        publisherField.setText(safe(row.publisher()));
        statusCombo.getSelectionModel().select(CompetitionUi.emptySafe(row.status()));
        coverNameField.setText(safe(row.coverName()));
        coverUrlField.setText(safe(row.coverUrl()));
        descriptionArea.setText(safe(row.description()));
        formFeedbackLabel.setText("Mode edition actif.");
        if (aiMetaLabel != null) {
            aiMetaLabel.setText("IA: non utilisee");
        }
    }

    private void deleteGame(int gameId) {
        if (!confirmDelete()) {
            return;
        }

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM games WHERE game_id = ?")) {
            statement.setInt(1, gameId);
            int affected = statement.executeUpdate();
            if (affected <= 0) {
                AlertUtils.warning("Admin jeux", "Jeu introuvable.");
                return;
            }
            if (editingGameId != null && editingGameId == gameId) {
                resetForm();
            }
            AlertUtils.info("Admin jeux", "Jeu supprime.");
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Admin jeux", "Suppression impossible (jeu lie a des tournois/demandes).\n" + ex.getMessage());
        }
    }

    private void export(boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "Nom", "Slug", "Categorie", "Publisher", "Statut", "Vues", "Favoris", "Score", "Cover", "Cree le");
        List<List<String>> rows = new ArrayList<>();
        for (GameRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.gameId()),
                    CompetitionUi.emptySafe(row.name()),
                    CompetitionUi.emptySafe(row.slug()),
                    CompetitionUi.emptySafe(row.categoryName()),
                    CompetitionUi.emptySafe(row.publisher()),
                    CompetitionUi.emptySafe(row.status()),
                    Integer.toString(row.viewsCount()),
                    Integer.toString(row.favoritesCount()),
                    Integer.toString(row.popularityScore()),
                    safe(row.coverName()).isBlank() ? CompetitionUi.emptySafe(row.coverUrl()) : row.coverName(),
                    CompetitionUi.fmtDateTime(row.createdAt())
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Jeux", headers, rows)
                    : exportService.exportExcel("admin_games", headers, rows);
            exportService.openFile(file);
            AlertUtils.info("Export", "Fichier genere: " + file.toAbsolutePath());
        } catch (IOException ex) {
            AlertUtils.error("Export", "Impossible de generer l'export.\n" + ex.getMessage());
        }
    }

    private Integer resolveImageId(Connection connection, String coverUrl, int uploadedByUserId, String altText) throws SQLException {
        String url = safe(coverUrl);
        if (url.isBlank()) {
            return null;
        }

        try (PreparedStatement statement = connection.prepareStatement("SELECT image_id FROM images WHERE file_url = ? LIMIT 1")) {
            statement.setString(1, url);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("image_id");
                }
            }
        }

        String sql = """
                INSERT INTO images (file_url, mime_type, size_bytes, width, height, alt_text, created_at, uploaded_by_user_id)
                VALUES (?, ?, 0, NULL, NULL, ?, NOW(), ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, url);
            statement.setString(2, guessMime(url));
            statement.setString(3, altText);
            statement.setInt(4, uploadedByUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return null;
    }

    private void resetForm() {
        editingGameId = null;
        formTitleLabel.setText("NOUVEAU JEU");
        saveButton.setText("Creer jeu");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);

        nameField.clear();
        publisherField.clear();
        statusCombo.getSelectionModel().select("DRAFT");
        coverNameField.clear();
        coverUrlField.clear();
        descriptionArea.clear();
        formFeedbackLabel.setText("");
        if (aiMetaLabel != null) {
            aiMetaLabel.setText("IA: non utilisee");
        }

        if (categoryCombo.getValue() == null && !categoryCombo.getItems().isEmpty()) {
            categoryCombo.getSelectionModel().select(0);
        }
    }

    private boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression jeu");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer ce jeu ?");
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

    private static String buildSlug(String name, int id) {
        String base = safe(name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (base.isBlank()) {
            base = "game";
        }
        return base + "-" + id;
    }

    private static String buildSlugBase(String text) {
        String base = safe(text).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return base.isBlank() ? "game" : base;
    }

    private static String buildSuggestion(String name, String publisher, String categoryName) {
        String game = name.isBlank() ? "ce jeu" : name;
        String studio = publisher.isBlank() ? "PULSE Studio" : publisher;
        String category = categoryName.isBlank() ? "e-sport" : categoryName.toLowerCase(Locale.ROOT);

        return game + " est un " + category
                + " competitif developpe par " + studio
                + ", avec modes classes, progression saisonniere, statistiques avancees et matchmaking equilibre.";
    }

    private static void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        int index = 1;
        for (Object value : params) {
            if (value instanceof Integer intValue) {
                statement.setInt(index++, intValue);
            } else if (value instanceof String text) {
                statement.setString(index++, text);
            } else {
                statement.setObject(index++, value);
            }
        }
    }

    private static void selectById(ComboBox<LookupItem> combo, int id) {
        for (LookupItem item : combo.getItems()) {
            if (item.getId() == id) {
                combo.getSelectionModel().select(item);
                return;
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullIfBlank(String value) {
        String v = safe(value);
        return v.isBlank() ? null : v;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String guessMime(String url) {
        String lower = safe(url).toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "image/jpeg";
    }

    private record GameRow(
            int gameId,
            int categoryId,
            String categoryName,
            String name,
            String slug,
            String status,
            String publisher,
            String description,
            int viewsCount,
            int favoritesCount,
            int popularityScore,
            LocalDateTime createdAt,
            LocalDateTime reviewedAt,
            String coverName,
            Integer coverImageId,
            String coverUrl
    ) {
    }
}
