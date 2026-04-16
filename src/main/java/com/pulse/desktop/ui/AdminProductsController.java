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
import java.math.BigDecimal;
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

public class AdminProductsController implements RouteAwarePage {
    private static final LookupItem ANY = new LookupItem(0, "ALL");

    @FXML
    private Label formTitleLabel;
    @FXML
    private ComboBox<LookupItem> teamCombo;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField priceField;
    @FXML
    private TextField stockField;
    @FXML
    private TextField skuField;
    @FXML
    private ComboBox<String> activeCombo;
    @FXML
    private TextField imageUrlField;
    @FXML
    private Label formFeedbackLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelEditButton;

    @FXML
    private TextField qField;
    @FXML
    private ComboBox<LookupItem> teamFilterCombo;
    @FXML
    private ComboBox<String> activeFilterCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private ComboBox<String> directionCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private VBox productsListBox;

    private final ExportService exportService = new ExportService();
    private Integer editingProductId;
    private List<ProductRow> currentRows = List.of();

    @FXML
    public void initialize() {
        activeCombo.setItems(FXCollections.observableArrayList("1", "0"));
        activeCombo.getSelectionModel().select("1");

        activeFilterCombo.setItems(FXCollections.observableArrayList("", "1", "0"));
        activeFilterCombo.getSelectionModel().select(0);

        sortCombo.setItems(FXCollections.observableArrayList("created_at", "updated_at", "id", "name", "team", "price", "stock_qty", "is_active"));
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
        loadTeams();
        resetForm();
        refresh();
    }

    @FXML
    private void saveProduct() {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }

        LookupItem team = teamCombo.getValue();
        if (team == null || team.getId() <= 0) {
            formFeedbackLabel.setText("Equipe invalide.");
            return;
        }

        String name = safe(nameField.getText());
        if (name.isBlank()) {
            formFeedbackLabel.setText("Le nom du produit est obligatoire.");
            return;
        }

        BigDecimal price = parseBigDecimal(priceField.getText());
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            formFeedbackLabel.setText("Prix invalide.");
            return;
        }

        Integer stockQty = parseInt(stockField.getText());
        if (stockQty == null || stockQty < 0) {
            formFeedbackLabel.setText("Stock invalide.");
            return;
        }

        boolean active = "1".equals(activeCombo.getValue());

        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Integer productId = editingProductId;
                if (productId == null) {
                    String sql = """
                            INSERT INTO products (
                                team_id, name, description, price, stock_qty, sku, is_active, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setInt(1, team.getId());
                        statement.setString(2, name);
                        statement.setString(3, nullIfBlank(descriptionArea.getText()));
                        statement.setBigDecimal(4, price);
                        statement.setInt(5, stockQty);
                        statement.setString(6, nullIfBlank(skuField.getText()));
                        statement.setBoolean(7, active);
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (keys.next()) {
                                productId = keys.getInt(1);
                            }
                        }
                    }
                } else {
                    String sql = """
                            UPDATE products
                            SET team_id = ?, name = ?, description = ?, price = ?, stock_qty = ?, sku = ?, is_active = ?, updated_at = NOW()
                            WHERE product_id = ?
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setInt(1, team.getId());
                        statement.setString(2, name);
                        statement.setString(3, nullIfBlank(descriptionArea.getText()));
                        statement.setBigDecimal(4, price);
                        statement.setInt(5, stockQty);
                        statement.setString(6, nullIfBlank(skuField.getText()));
                        statement.setBoolean(7, active);
                        statement.setInt(8, productId);
                        statement.executeUpdate();
                    }
                }

                if (productId != null) {
                    syncProductImage(connection, productId, imageUrlField.getText(), admin.getUserId(), name);
                }

                connection.commit();
                AlertUtils.info("Admin produits", editingProductId == null ? "Produit cree." : "Produit mis a jour.");
                resetForm();
                refresh();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin produits", "Enregistrement impossible.\n" + ex.getMessage());
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
        if (!teamFilterCombo.getItems().isEmpty()) {
            teamFilterCombo.getSelectionModel().select(0);
        }
        activeFilterCombo.getSelectionModel().select(0);
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

    private void loadTeams() {
        List<LookupItem> teams = new ArrayList<>();
        teams.add(ANY);
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement("SELECT team_id, name FROM teams ORDER BY name ASC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                teams.add(new LookupItem(rs.getInt("team_id"), rs.getString("name")));
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin produits", "Impossible de charger les equipes.\n" + ex.getMessage());
        }

        teamFilterCombo.setItems(FXCollections.observableArrayList(teams));
        teamFilterCombo.getSelectionModel().select(0);

        List<LookupItem> formTeams = new ArrayList<>(teams);
        formTeams.remove(0);
        teamCombo.setItems(FXCollections.observableArrayList(formTeams));
        if (!formTeams.isEmpty()) {
            teamCombo.getSelectionModel().select(0);
        }
    }

    private void refresh() {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_id,
                    p.team_id,
                    t.name AS team_name,
                    p.name,
                    p.description,
                    p.price,
                    p.stock_qty,
                    p.sku,
                    p.is_active,
                    p.created_at,
                    p.updated_at,
                    i.file_url AS image_url
                FROM products p
                LEFT JOIN teams t ON t.team_id = p.team_id
                LEFT JOIN product_images pi ON pi.product_id = p.product_id
                    AND pi.image_id = (
                        SELECT pi2.image_id
                        FROM product_images pi2
                        WHERE pi2.product_id = p.product_id
                        ORDER BY pi2.position ASC, pi2.image_id ASC
                        LIMIT 1
                    )
                LEFT JOIN images i ON i.image_id = pi.image_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        String q = safe(qField.getText());
        if (!q.isBlank()) {
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            sql.append("""
                     AND (
                        LOWER(p.name) LIKE ?
                        OR LOWER(COALESCE(p.description, '')) LIKE ?
                        OR LOWER(COALESCE(p.sku, '')) LIKE ?
                        OR LOWER(COALESCE(t.name, '')) LIKE ?
                     )
                    """);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        LookupItem team = teamFilterCombo.getValue();
        if (team != null && team.getId() > 0) {
            sql.append(" AND p.team_id = ? ");
            params.add(team.getId());
        }

        String active = safe(activeFilterCombo.getValue());
        if ("1".equals(active) || "0".equals(active)) {
            sql.append(" AND p.is_active = ? ");
            params.add("1".equals(active));
        }

        String direction = "asc".equalsIgnoreCase(safe(directionCombo.getValue())) ? "ASC" : "DESC";
        String sort = safe(sortCombo.getValue());
        switch (sort) {
            case "id" -> sql.append(" ORDER BY p.product_id ").append(direction);
            case "name" -> sql.append(" ORDER BY p.name ").append(direction).append(", p.product_id DESC");
            case "team" -> sql.append(" ORDER BY t.name ").append(direction).append(", p.product_id DESC");
            case "price" -> sql.append(" ORDER BY p.price ").append(direction).append(", p.product_id DESC");
            case "stock_qty" -> sql.append(" ORDER BY p.stock_qty ").append(direction).append(", p.product_id DESC");
            case "is_active" -> sql.append(" ORDER BY p.is_active ").append(direction).append(", p.product_id DESC");
            case "updated_at" -> sql.append(" ORDER BY p.updated_at ").append(direction).append(", p.product_id DESC");
            default -> sql.append(" ORDER BY p.created_at ").append(direction).append(", p.product_id DESC");
        }
        sql.append(" LIMIT 500 ");

        List<ProductRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ProductRow(
                            rs.getInt("product_id"),
                            rs.getInt("team_id"),
                            rs.getString("team_name"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBigDecimal("price"),
                            rs.getInt("stock_qty"),
                            rs.getString("sku"),
                            rs.getBoolean("is_active"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("updated_at")),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin produits", "Chargement impossible.\n" + ex.getMessage());
            return;
        }

        currentRows = rows;
        resultCountLabel.setText(rows.size() + " resultat(s)");
        renderRows();
    }

    private void renderRows() {
        productsListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            productsListBox.getChildren().add(CompetitionUi.emptyState("Aucun produit trouve."));
            return;
        }

        for (ProductRow row : currentRows) {
            Button update = new Button("Update");
            update.getStyleClass().add("btn-ghost");
            update.setOnAction(event -> startEdit(row));

            Button delete = new Button("Delete");
            delete.getStyleClass().add("btn-ghost");
            delete.setOnAction(event -> deleteProduct(row.productId()));

            productsListBox.getChildren().add(buildProductRow(row, update, delete));
        }
    }

    private HBox buildProductRow(ProductRow row, Button update, Button delete) {
        ImageView preview = new ImageView();
        preview.setFitWidth(72);
        preview.setFitHeight(72);
        preview.setPreserveRatio(false);
        preview.getStyleClass().add("post-image-preview");

        String fallback = "https://picsum.photos/seed/pulse_admin_product_" + row.productId() + "/300/300";
        String imagePath = ImageResolver.toExternalForm(row.imageUrl());
        if (imagePath == null || imagePath.isBlank()) {
            imagePath = fallback;
        }
        Image image = new Image(imagePath, true);
        if (image.isError()) {
            image = new Image(fallback, true);
        }
        preview.setImage(image);

        String line1 = "#" + row.productId() + " | " + CompetitionUi.emptySafe(row.name()) + " | " + (row.price() == null ? "0" : row.price()) + " DT";
        String line2 = CompetitionUi.emptySafe(row.teamName())
                + " | Stock: " + row.stockQty()
                + " | Actif: " + (row.active() ? "Oui" : "Non")
                + " | SKU: " + CompetitionUi.emptySafe(row.sku())
                + " | Maj: " + CompetitionUi.fmtDateTime(row.updatedAt());

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

        HBox rowBox = new HBox(12, preview, text, spacer);
        rowBox.getStyleClass().add("list-item");
        return rowBox;
    }

    private void startEdit(ProductRow row) {
        editingProductId = row.productId();
        formTitleLabel.setText("MODIFIER PRODUIT #" + row.productId());
        saveButton.setText("Mettre a jour");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);

        selectById(teamCombo, row.teamId());
        nameField.setText(safe(row.name()));
        descriptionArea.setText(safe(row.description()));
        priceField.setText(row.price() == null ? "" : row.price().toPlainString());
        stockField.setText(Integer.toString(row.stockQty()));
        skuField.setText(safe(row.sku()));
        activeCombo.getSelectionModel().select(row.active() ? "1" : "0");
        imageUrlField.setText(safe(row.imageUrl()));
        formFeedbackLabel.setText("Mode edition actif.");
    }

    private void deleteProduct(int productId) {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }
        if (!confirmDelete()) {
            return;
        }

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM products WHERE product_id = ?")) {
            statement.setInt(1, productId);
            int affected = statement.executeUpdate();
            if (affected <= 0) {
                AlertUtils.warning("Admin produits", "Produit introuvable.");
                return;
            }
            if (editingProductId != null && editingProductId == productId) {
                resetForm();
            }
            AlertUtils.info("Admin produits", "Produit supprime.");
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Admin produits", "Suppression impossible (produit lie a des paniers/commandes).\n" + ex.getMessage());
        }
    }

    private void export(boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "Nom", "Equipe", "Prix", "Stock", "Actif", "SKU", "Image", "Cree le", "Maj");
        List<List<String>> rows = new ArrayList<>();
        for (ProductRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.productId()),
                    CompetitionUi.emptySafe(row.name()),
                    CompetitionUi.emptySafe(row.teamName()),
                    row.price() == null ? "0.00" : row.price().toPlainString(),
                    Integer.toString(row.stockQty()),
                    row.active() ? "Oui" : "Non",
                    CompetitionUi.emptySafe(row.sku()),
                    CompetitionUi.emptySafe(row.imageUrl()),
                    CompetitionUi.fmtDateTime(row.createdAt()),
                    CompetitionUi.fmtDateTime(row.updatedAt())
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Produits", headers, rows)
                    : exportService.exportExcel("admin_products", headers, rows);
            exportService.openFile(file);
            AlertUtils.info("Export", "Fichier genere: " + file.toAbsolutePath());
        } catch (IOException ex) {
            AlertUtils.error("Export", "Impossible de generer l'export.\n" + ex.getMessage());
        }
    }

    private void syncProductImage(Connection connection, int productId, String imageUrlRaw, int uploadedByUserId, String altText) throws SQLException {
        String imageUrl = safe(imageUrlRaw);
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM product_images WHERE product_id = ?")) {
            statement.setInt(1, productId);
            statement.executeUpdate();
        }
        if (imageUrl.isBlank()) {
            return;
        }

        Integer imageId = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT image_id FROM images WHERE file_url = ? LIMIT 1")) {
            statement.setString(1, imageUrl);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    imageId = rs.getInt("image_id");
                }
            }
        }

        if (imageId == null) {
            String sql = """
                    INSERT INTO images (file_url, mime_type, size_bytes, width, height, alt_text, created_at, uploaded_by_user_id)
                    VALUES (?, ?, 0, NULL, NULL, ?, NOW(), ?)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, imageUrl);
                statement.setString(2, guessMime(imageUrl));
                statement.setString(3, altText);
                statement.setInt(4, uploadedByUserId);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        imageId = keys.getInt(1);
                    }
                }
            }
        }

        if (imageId == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO product_images (product_id, image_id, position) VALUES (?, ?, 1)")) {
            statement.setInt(1, productId);
            statement.setInt(2, imageId);
            statement.executeUpdate();
        }
    }

    private void resetForm() {
        editingProductId = null;
        formTitleLabel.setText("NOUVEAU PRODUIT");
        saveButton.setText("Creer produit");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);

        nameField.clear();
        descriptionArea.clear();
        priceField.setText("0.00");
        stockField.setText("0");
        skuField.clear();
        imageUrlField.clear();
        activeCombo.getSelectionModel().select("1");
        formFeedbackLabel.setText("");

        if (teamCombo.getValue() == null && !teamCombo.getItems().isEmpty()) {
            teamCombo.getSelectionModel().select(0);
        }
    }

    private boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression produit");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer ce produit ?");
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

    private static void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        int index = 1;
        for (Object value : params) {
            if (value instanceof Integer intValue) {
                statement.setInt(index++, intValue);
            } else if (value instanceof String text) {
                statement.setString(index++, text);
            } else if (value instanceof Boolean bool) {
                statement.setBoolean(index++, bool);
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
        String text = safe(value);
        return text.isBlank() ? null : text;
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.parseInt(safe(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(safe(value));
        } catch (Exception ignored) {
            return null;
        }
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

    private record ProductRow(
            int productId,
            int teamId,
            String teamName,
            String name,
            String description,
            BigDecimal price,
            int stockQty,
            String sku,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String imageUrl
    ) {
    }
}
