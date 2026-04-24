package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminCategoriesController implements RouteAwarePage {
    @FXML
    private Label formTitleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private Label slugPreviewLabel;
    @FXML
    private Label formFeedbackLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelEditButton;

    @FXML
    private TextField qField;
    @FXML
    private Label resultCountLabel;
    @FXML
    private VBox categoriesListBox;

    private Integer editingCategoryId;

    @FXML
    public void initialize() {
        resetForm();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        resetForm();
        refresh();
    }

    @FXML
    private void openCatalogDashboard() {
        Navigator.goTo("admin_catalog_dashboard");
    }

    @FXML
    private void saveCategory() {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }

        String name = safe(nameField.getText());
        if (name.isBlank()) {
            formFeedbackLabel.setText("Le nom de la categorie est obligatoire.");
            return;
        }

        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                boolean hasSlug = columnExists(connection, "categories", "slug");
                boolean hasCreatedAt = columnExists(connection, "categories", "created_at");
                boolean hasUpdatedAt = columnExists(connection, "categories", "updated_at");

                if (editingCategoryId == null) {
                    int categoryId = insertCategory(connection, name, hasSlug, hasCreatedAt, hasUpdatedAt);
                    if (hasSlug) {
                        updateSlug(connection, categoryId, buildSlug(name, categoryId), hasUpdatedAt);
                    }
                    AlertUtils.info("Admin categories", "Categorie creee.");
                } else {
                    updateCategory(connection, editingCategoryId, name, hasSlug, hasUpdatedAt);
                    AlertUtils.info("Admin categories", "Categorie mise a jour.");
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
            AlertUtils.error("Admin categories", "Enregistrement categorie impossible.\n" + ex.getMessage());
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
        refresh();
    }

    private void refresh() {
        List<CategoryRow> rows = loadRows();
        resultCountLabel.setText(rows.size() + " categorie(s)");
        renderRows(rows);
    }

    private List<CategoryRow> loadRows() {
        String q = safe(qField.getText());
        List<CategoryRow> rows = new ArrayList<>();

        try (Connection connection = Jdbc.open()) {
            boolean hasSlug = columnExists(connection, "categories", "slug");

            StringBuilder sql = new StringBuilder("""
                    SELECT
                        c.category_id,
                        c.name,
                    """);
            if (hasSlug) {
                sql.append(" c.slug, ");
            } else {
                sql.append(" NULL AS slug, ");
            }
            sql.append("""
                        (SELECT COUNT(*) FROM games g WHERE g.category_id = c.category_id) AS games_count
                    FROM categories c
                    WHERE 1=1
                    """);

            List<Object> params = new ArrayList<>();
            if (!q.isBlank()) {
                sql.append(" AND LOWER(c.name) LIKE ? ");
                params.add("%" + q.toLowerCase(Locale.ROOT) + "%");
            }
            sql.append(" ORDER BY games_count DESC, c.name ASC, c.category_id DESC ");
            sql.append(" LIMIT 500 ");

            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                bind(statement, params);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new CategoryRow(
                                rs.getInt("category_id"),
                                rs.getString("name"),
                                rs.getString("slug"),
                                rs.getInt("games_count")
                        ));
                    }
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin categories", "Chargement categories impossible.\n" + ex.getMessage());
        }

        return rows;
    }

    private void renderRows(List<CategoryRow> rows) {
        categoriesListBox.getChildren().clear();
        if (rows.isEmpty()) {
            categoriesListBox.getChildren().add(CompetitionUi.emptyState("Aucune categorie."));
            return;
        }

        for (CategoryRow row : rows) {
            Button edit = new Button("Editer");
            edit.getStyleClass().add("btn-ghost");
            edit.setOnAction(event -> startEdit(row));

            Button delete = new Button("Supprimer");
            delete.getStyleClass().addAll("btn-ghost", "btn-danger");
            delete.setOnAction(event -> deleteCategory(row));

            String left = "#" + row.categoryId() + " | " + CompetitionUi.emptySafe(row.name());
            String right = "Jeux: " + row.gamesCount()
                    + (row.slug() == null || row.slug().isBlank() ? "" : " | slug: " + row.slug());

            categoriesListBox.getChildren().add(CompetitionUi.listRowWithActions(left, right, edit, delete));
        }
    }

    private void startEdit(CategoryRow row) {
        editingCategoryId = row.categoryId();
        formTitleLabel.setText("EDITER CATEGORIE #" + row.categoryId());
        saveButton.setText("Mettre a jour");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);

        nameField.setText(row.name());
        slugPreviewLabel.setText(row.slug() == null ? "-" : row.slug());
        formFeedbackLabel.setText("");
    }

    private void deleteCategory(CategoryRow row) {
        if (!confirmDelete(row)) {
            return;
        }
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM categories WHERE category_id = ?")) {
            statement.setInt(1, row.categoryId());
            statement.executeUpdate();
            AlertUtils.info("Admin categories", "Categorie supprimee.");
            if (editingCategoryId != null && editingCategoryId == row.categoryId()) {
                resetForm();
            }
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Admin categories", "Suppression impossible (categorie utilisee par des jeux ?).\n" + ex.getMessage());
        }
    }

    private void resetForm() {
        editingCategoryId = null;
        formTitleLabel.setText("NOUVELLE CATEGORIE");
        saveButton.setText("Creer categorie");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        nameField.clear();
        slugPreviewLabel.setText("-");
        formFeedbackLabel.setText("");
    }

    private boolean confirmDelete(CategoryRow row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression categorie");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer la categorie #" + row.categoryId() + " (" + safe(row.name()) + ") ?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static int insertCategory(Connection connection, String name, boolean hasSlug, boolean hasCreatedAt, boolean hasUpdatedAt) throws SQLException {
        String tempSlug = "temp-" + System.currentTimeMillis();

        StringBuilder sql = new StringBuilder("INSERT INTO categories (name");
        if (hasSlug) {
            sql.append(", slug");
        }
        if (hasCreatedAt) {
            sql.append(", created_at");
        }
        if (hasUpdatedAt) {
            sql.append(", updated_at");
        }
        sql.append(") VALUES (?"); // name
        if (hasSlug) {
            sql.append(", ?"); // slug
        }
        if (hasCreatedAt) {
            sql.append(", NOW()");
        }
        if (hasUpdatedAt) {
            sql.append(", NOW()");
        }
        sql.append(")");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            statement.setString(index++, name);
            if (hasSlug) {
                statement.setString(index, tempSlug);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creation categorie impossible.");
                }
                return keys.getInt(1);
            }
        }
    }

    private static void updateCategory(Connection connection, int categoryId, String name, boolean hasSlug, boolean hasUpdatedAt) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE categories SET name = ?");
        if (hasSlug) {
            sql.append(", slug = ?");
        }
        if (hasUpdatedAt) {
            sql.append(", updated_at = NOW()");
        }
        sql.append(" WHERE category_id = ? ");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, name);
            if (hasSlug) {
                statement.setString(index++, buildSlug(name, categoryId));
            }
            statement.setInt(index, categoryId);
            statement.executeUpdate();
        }
    }

    private static void updateSlug(Connection connection, int categoryId, String slug, boolean hasUpdatedAt) throws SQLException {
        String sql = "UPDATE categories SET slug = ?" + (hasUpdatedAt ? ", updated_at = NOW()" : "") + " WHERE category_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            statement.setInt(2, categoryId);
            statement.executeUpdate();
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = connection.getCatalog();
        try (ResultSet rs = meta.getColumns(catalog, null, tableName, columnName)) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = meta.getColumns(catalog, null, tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT))) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = meta.getColumns(catalog, null, tableName.toLowerCase(Locale.ROOT), columnName.toLowerCase(Locale.ROOT))) {
            return rs.next();
        }
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
            base = "category";
        }
        return base + "-" + id;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record CategoryRow(int categoryId, String name, String slug, int gamesCount) {
    }
}
