package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.repo.TeamModuleRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FrontCaptainProductsController implements RouteAwarePage {
    @FXML
    private ComboBox<LookupItem> teamSelectorCombo;
    @FXML
    private TextField qField;
    @FXML
    private CheckBox inactiveCheck;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label feedbackLabel;
    @FXML
    private VBox productsBox;

    @FXML
    private Label formTitleLabel;
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
    private TextField imageUrlField;
    @FXML
    private ComboBox<String> activeCombo;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelEditButton;

    private final TeamModuleRepository teamRepository = new TeamModuleRepository();
    private List<TeamModuleRepository.CaptainTeamRow> captainTeams = List.of();
    private TeamModuleRepository.CaptainTeamRow activeTeam;
    private Integer editingProductId;

    @FXML
    public void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList("latest", "oldest", "name", "price_high", "price_low", "stock_high"));
        sortCombo.getSelectionModel().select("latest");

        activeCombo.setItems(FXCollections.observableArrayList("1", "0"));
        activeCombo.getSelectionModel().select("1");

        resetForm();
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        SessionUser user = requireCaptain();
        if (user == null) {
            return;
        }
        loadTeams(user.getUserId());
    }

    @FXML
    private void onTeamSelectionChanged() {
        LookupItem item = teamSelectorCombo.getValue();
        if (item == null || item.getId() <= 0) {
            activeTeam = null;
            productsBox.getChildren().setAll(CompetitionUi.emptyState("Selectionnez une equipe."));
            resultCountLabel.setText("0 produit(s)");
            return;
        }
        activeTeam = findTeam(item.getId());
        RouteContext.putInt(RouteContext.KEY_TEAM_ID, item.getId());
        refreshProducts();
    }

    @FXML
    private void applyFilters() {
        refreshProducts();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        inactiveCheck.setSelected(false);
        sortCombo.getSelectionModel().select("latest");
        refreshProducts();
    }

    @FXML
    private void saveProduct() {
        SessionUser user = requireCaptain();
        if (user == null || activeTeam == null) {
            return;
        }

        String name = safe(nameField.getText());
        if (name.isBlank()) {
            feedbackLabel.setText("Le nom du produit est obligatoire.");
            return;
        }
        BigDecimal price = parseBigDecimal(priceField.getText());
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            feedbackLabel.setText("Prix invalide.");
            return;
        }
        Integer stock = parseInt(stockField.getText());
        if (stock == null || stock < 0) {
            feedbackLabel.setText("Stock invalide.");
            return;
        }
        boolean active = "1".equals(activeCombo.getValue());

        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Integer productId = editingProductId;
                if (productId == null) {
                    String insertSql = """
                            INSERT INTO products (
                                team_id, name, description, price, stock_qty, sku, is_active, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setInt(1, activeTeam.teamId());
                        statement.setString(2, name);
                        statement.setString(3, nullIfBlank(descriptionArea.getText()));
                        statement.setBigDecimal(4, price);
                        statement.setInt(5, stock);
                        statement.setString(6, nullIfBlank(skuField.getText()));
                        statement.setBoolean(7, active);
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (keys.next()) {
                                productId = keys.getInt(1);
                            }
                        }
                    }
                    feedbackLabel.setText("Produit cree.");
                } else {
                    String updateSql = """
                            UPDATE products
                            SET team_id = ?, name = ?, description = ?, price = ?, stock_qty = ?, sku = ?, is_active = ?, updated_at = NOW()
                            WHERE product_id = ? AND team_id = ?
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                        statement.setInt(1, activeTeam.teamId());
                        statement.setString(2, name);
                        statement.setString(3, nullIfBlank(descriptionArea.getText()));
                        statement.setBigDecimal(4, price);
                        statement.setInt(5, stock);
                        statement.setString(6, nullIfBlank(skuField.getText()));
                        statement.setBoolean(7, active);
                        statement.setInt(8, productId);
                        statement.setInt(9, activeTeam.teamId());
                        statement.executeUpdate();
                    }
                    feedbackLabel.setText("Produit mis a jour.");
                }

                if (productId != null) {
                    syncProductImage(connection, productId, imageUrlField.getText(), user.getUserId(), name);
                }
                connection.commit();
                resetForm();
                refreshProducts();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Produits", "Enregistrement impossible.\n" + ex.getMessage());
        }
    }

    @FXML
    private void cancelEdit() {
        resetForm();
    }

    @FXML
    private void goTeamManage() {
        goCaptainPage("front_captain_team_manage");
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

    private void loadTeams(int captainUserId) {
        try {
            captainTeams = teamRepository.listCaptainTeams(captainUserId, 200);
            List<LookupItem> options = new ArrayList<>();
            for (TeamModuleRepository.CaptainTeamRow row : captainTeams) {
                options.add(new LookupItem(row.teamId(), row.name()));
            }
            teamSelectorCombo.setItems(FXCollections.observableArrayList(options));

            Integer fromContext = RouteContext.getInt(RouteContext.KEY_TEAM_ID);
            if (fromContext != null) {
                activeTeam = findTeam(fromContext);
            }
            if (activeTeam == null && !captainTeams.isEmpty()) {
                activeTeam = captainTeams.get(0);
            }
            if (activeTeam != null) {
                RouteContext.putInt(RouteContext.KEY_TEAM_ID, activeTeam.teamId());
                selectTeam(activeTeam.teamId());
            }
            refreshProducts();
        } catch (SQLException ex) {
            AlertUtils.error("Produits", "Impossible de charger les equipes.\n" + ex.getMessage());
        }
    }

    private void refreshProducts() {
        productsBox.getChildren().clear();
        if (activeTeam == null) {
            resultCountLabel.setText("0 produit(s)");
            productsBox.getChildren().add(CompetitionUi.emptyState("Aucune equipe active."));
            return;
        }

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_id,
                    p.name,
                    p.description,
                    p.price,
                    p.stock_qty,
                    p.sku,
                    p.is_active,
                    p.updated_at,
                    i.file_url AS image_url
                FROM products p
                LEFT JOIN product_images pi ON pi.product_id = p.product_id
                    AND pi.image_id = (
                        SELECT pi2.image_id
                        FROM product_images pi2
                        WHERE pi2.product_id = p.product_id
                        ORDER BY pi2.position ASC, pi2.image_id ASC
                        LIMIT 1
                    )
                LEFT JOIN images i ON i.image_id = pi.image_id
                WHERE p.team_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(activeTeam.teamId());

        String q = safe(qField.getText());
        if (!q.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(p.name) LIKE ?
                        OR LOWER(COALESCE(p.description, '')) LIKE ?
                        OR LOWER(COALESCE(p.sku, '')) LIKE ?
                     )
                    """);
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (!inactiveCheck.isSelected()) {
            sql.append(" AND p.is_active = 1 ");
        }

        String sort = sortCombo.getValue() == null ? "latest" : sortCombo.getValue();
        switch (sort) {
            case "oldest" -> sql.append(" ORDER BY p.created_at ASC, p.product_id ASC ");
            case "name" -> sql.append(" ORDER BY p.name ASC, p.product_id DESC ");
            case "price_high" -> sql.append(" ORDER BY p.price DESC, p.product_id DESC ");
            case "price_low" -> sql.append(" ORDER BY p.price ASC, p.product_id DESC ");
            case "stock_high" -> sql.append(" ORDER BY p.stock_qty DESC, p.product_id DESC ");
            default -> sql.append(" ORDER BY p.created_at DESC, p.product_id DESC ");
        }
        sql.append(" LIMIT 400 ");

        List<ProductRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ProductRow(
                            rs.getInt("product_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBigDecimal("price"),
                            rs.getInt("stock_qty"),
                            rs.getString("sku"),
                            rs.getBoolean("is_active"),
                            toLocalDateTime(rs.getTimestamp("updated_at")),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Produits", "Chargement impossible.\n" + ex.getMessage());
            return;
        }

        resultCountLabel.setText(rows.size() + " produit(s)");
        if (rows.isEmpty()) {
            productsBox.getChildren().add(CompetitionUi.emptyState("Aucun produit pour cette equipe."));
            return;
        }

        for (ProductRow row : rows) {
            Button edit = new Button("Modifier");
            edit.getStyleClass().add("btn-ghost");
            edit.setOnAction(e -> startEdit(row));

            Button detail = new Button("Voir detail");
            detail.getStyleClass().add("btn-ghost");
            detail.setOnAction(e -> {
                RouteContext.putInt(RouteContext.KEY_PRODUCT_ID, row.productId());
                Navigator.goTo("front_product_detail");
            });

            Button delete = new Button("Supprimer");
            delete.getStyleClass().add("btn-ghost");
            delete.setOnAction(e -> deleteProduct(row.productId()));

            String line1 = "#" + row.productId() + " | " + row.name() + " | " + row.price() + " DT | Stock: " + row.stockQty();
            String line2 = "SKU: " + safe(row.sku()) + " | Actif: " + (row.active() ? "Oui" : "Non") + " | Maj: " + CompetitionUi.fmtDateTime(row.updatedAt());
            productsBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, detail, edit, delete));
        }
    }

    private void startEdit(ProductRow row) {
        editingProductId = row.productId();
        formTitleLabel.setText("MODIFIER PRODUIT #" + row.productId());
        saveButton.setText("Mettre a jour");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
        nameField.setText(row.name());
        descriptionArea.setText(safe(row.description()));
        priceField.setText(row.price() == null ? "" : row.price().toPlainString());
        stockField.setText(Integer.toString(row.stockQty()));
        skuField.setText(safe(row.sku()));
        imageUrlField.setText(safe(row.imageUrl()));
        activeCombo.getSelectionModel().select(row.active() ? "1" : "0");
    }

    private void deleteProduct(int productId) {
        if (activeTeam == null) {
            return;
        }
        String sql = "DELETE FROM products WHERE product_id = ? AND team_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            statement.setInt(2, activeTeam.teamId());
            int deleted = statement.executeUpdate();
            if (deleted <= 0) {
                feedbackLabel.setText("Produit introuvable.");
                return;
            }
            feedbackLabel.setText("Produit supprime.");
            refreshProducts();
        } catch (SQLException ex) {
            AlertUtils.error("Produits", "Suppression impossible.\n" + ex.getMessage());
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
            String insertImageSql = """
                    INSERT INTO images (file_url, mime_type, size_bytes, width, height, alt_text, created_at, uploaded_by_user_id)
                    VALUES (?, ?, 0, NULL, NULL, ?, NOW(), ?)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(insertImageSql, Statement.RETURN_GENERATED_KEYS)) {
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
        priceField.clear();
        stockField.setText("0");
        skuField.clear();
        imageUrlField.clear();
        activeCombo.getSelectionModel().select("1");
    }

    private void selectTeam(int teamId) {
        for (LookupItem item : teamSelectorCombo.getItems()) {
            if (item.getId() == teamId) {
                teamSelectorCombo.getSelectionModel().select(item);
                return;
            }
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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullIfBlank(String value) {
        String v = safe(value);
        return v.isBlank() ? null : v;
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

    private static void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        int index = 1;
        for (Object value : params) {
            if (value instanceof Integer intValue) {
                statement.setInt(index++, intValue);
            } else if (value instanceof String stringValue) {
                statement.setString(index++, stringValue);
            } else {
                statement.setObject(index++, value);
            }
        }
    }

    private static LocalDateTime toLocalDateTime(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
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
            String name,
            String description,
            BigDecimal price,
            int stockQty,
            String sku,
            boolean active,
            LocalDateTime updatedAt,
            String imageUrl
    ) {
    }
}
