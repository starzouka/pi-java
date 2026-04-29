package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FrontShopController implements RouteAwarePage {
    @FXML
    private TextField qField;
    @FXML
    private ComboBox<LookupItem> teamCombo;
    @FXML
    private TextField minField;
    @FXML
    private TextField maxField;
    @FXML
    private CheckBox stockCheck;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label cartCountLabel;
    @FXML
    private VBox productsBox;

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        loadTeamOptions();
        refreshCartCount();
        refreshProducts();
    }

    @FXML
    public void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList("latest", "oldest", "name", "price_high", "price_low", "stock_high"));
        sortCombo.getSelectionModel().select("latest");
    }

    @FXML
    private void applyFilters() {
        refreshProducts();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        minField.clear();
        maxField.clear();
        stockCheck.setSelected(false);
        sortCombo.getSelectionModel().select("latest");
        if (!teamCombo.getItems().isEmpty()) {
            teamCombo.getSelectionModel().select(0);
        }
        refreshProducts();
    }

    @FXML
    private void openCart() {
        Navigator.goTo("front_cart");
    }

    @FXML
    private void openOrders() {
        Navigator.goTo("front_orders");
    }

    @FXML
    private void openChatbot() {
        Navigator.goTo("front_shop_chatbot");
    }

    private void loadTeamOptions() {
        List<LookupItem> teams = new ArrayList<>();
        teams.add(new LookupItem(0, "Toutes les equipes"));
        String sql = "SELECT team_id, name FROM teams ORDER BY name ASC";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                teams.add(new LookupItem(rs.getInt("team_id"), rs.getString("name")));
            }
            teamCombo.setItems(FXCollections.observableArrayList(teams));
            if (teamCombo.getValue() == null) {
                teamCombo.getSelectionModel().select(0);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Shop", "Impossible de charger les equipes.\n" + ex.getMessage());
        }
    }

    private void refreshProducts() {
        productsBox.getChildren().clear();

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_id,
                    p.team_id,
                    t.name AS team_name,
                    p.name,
                    p.description,
                    p.price,
                    p.stock_qty,
                    p.is_active,
                    i.file_url AS image_url
                FROM products p
                INNER JOIN teams t ON t.team_id = p.team_id
                LEFT JOIN product_images pi ON pi.product_id = p.product_id
                    AND pi.image_id = (
                        SELECT pi2.image_id
                        FROM product_images pi2
                        WHERE pi2.product_id = p.product_id
                        ORDER BY pi2.position ASC, pi2.image_id ASC
                        LIMIT 1
                    )
                LEFT JOIN images i ON i.image_id = pi.image_id
                WHERE p.is_active = 1
                """);
        List<Object> params = new ArrayList<>();

        String q = safe(qField.getText());
        if (!q.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(p.name) LIKE ?
                        OR LOWER(COALESCE(p.description, '')) LIKE ?
                        OR LOWER(COALESCE(t.name, '')) LIKE ?
                     )
                    """);
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        LookupItem team = teamCombo.getValue();
        if (team != null && team.getId() > 0) {
            sql.append(" AND p.team_id = ? ");
            params.add(team.getId());
        }

        BigDecimal min = parseBigDecimal(minField.getText());
        BigDecimal max = parseBigDecimal(maxField.getText());
        if (min != null) {
            sql.append(" AND p.price >= ? ");
            params.add(min);
        }
        if (max != null) {
            sql.append(" AND p.price <= ? ");
            params.add(max);
        }
        if (stockCheck.isSelected()) {
            sql.append(" AND p.stock_qty > 0 ");
        }

        String sort = sortCombo.getValue() == null ? "latest" : sortCombo.getValue();
        switch (sort) {
            case "oldest" -> sql.append(" ORDER BY p.created_at ASC, p.product_id ASC ");
            case "name" -> sql.append(" ORDER BY p.name ASC, p.created_at DESC ");
            case "price_high" -> sql.append(" ORDER BY p.price DESC, p.created_at DESC ");
            case "price_low" -> sql.append(" ORDER BY p.price ASC, p.created_at DESC ");
            case "stock_high" -> sql.append(" ORDER BY p.stock_qty DESC, p.created_at DESC ");
            default -> sql.append(" ORDER BY p.created_at DESC, p.product_id DESC ");
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
                            rs.getBoolean("is_active"),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Shop", "Chargement impossible.\n" + ex.getMessage());
            return;
        }

        resultCountLabel.setText(rows.size() + " produit(s)");
        if (rows.isEmpty()) {
            productsBox.getChildren().add(CompetitionUi.emptyState("Aucun produit ne correspond aux filtres."));
            return;
        }

        for (ProductRow row : rows) {
            Button detail = new Button("Detail");
            detail.getStyleClass().add("btn-ghost");
            detail.setOnAction(e -> {
                RouteContext.putInt(RouteContext.KEY_PRODUCT_ID, row.productId());
                Navigator.goTo("front_product_detail");
            });

            Button teamButton = new Button("Equipe");
            teamButton.getStyleClass().add("btn-ghost");
            teamButton.setOnAction(e -> {
                RouteContext.putInt(RouteContext.KEY_TEAM_ID, row.teamId());
                Navigator.goTo("front_team_detail");
            });

            Button addCart = new Button(row.stockQty() <= 0 ? "Rupture" : "Ajouter au panier");
            addCart.getStyleClass().add("btn-primary");
            addCart.setDisable(row.stockQty() <= 0);
            addCart.setOnAction(e -> addToCart(row.productId(), 1));

            productsBox.getChildren().add(buildProductRow(row, detail, teamButton, addCart));
        }
    }

    private HBox buildProductRow(ProductRow row, Button detail, Button teamButton, Button addCart) {
        ImageView preview = new ImageView();
        preview.setFitWidth(112);
        preview.setFitHeight(72);
        preview.setPreserveRatio(false);
        preview.getStyleClass().add("post-image-preview");

        String fallback = "https://picsum.photos/seed/pulse_shop_" + row.productId() + "/640/360";
        String imagePath = ImageResolver.toExternalForm(row.imageUrl());
        if (imagePath == null || imagePath.isBlank()) {
            imagePath = fallback;
        }
        Image image = new Image(imagePath, true);
        if (image.isError()) {
            image = new Image(fallback, true);
        }
        preview.setImage(image);

        String line1 = row.name() + " | " + row.price() + " DT | Stock: " + row.stockQty();
        String line2 = row.teamName() + " | " + (safe(row.description()).isBlank() ? "Produit equipe PULSE." : safe(row.description()));

        Label title = new Label(line1);
        title.getStyleClass().add("card__title");
        title.setWrapText(true);

        Label meta = new Label(line2);
        meta.getStyleClass().add("list-item-meta");
        meta.setWrapText(true);

        HBox actions = new HBox(8, detail, teamButton, addCart);
        VBox text = new VBox(6, title, meta, actions);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox rowBox = new HBox(12, preview, text);
        rowBox.getStyleClass().add("list-item");
        return rowBox;
    }

    private void addToCart(int productId, int quantity) {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
            return;
        }

        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                CartInfo cart = loadOrCreateCart(connection, user.getUserId());
                if (!"OPEN".equalsIgnoreCase(cart.status())) {
                    connection.rollback();
                    AlertUtils.warning("Panier", "Ce panier est verrouille.");
                    return;
                }

                Integer currentQty = null;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT quantity FROM cart_items WHERE cart_id = ? AND product_id = ?")) {
                    statement.setInt(1, cart.cartId());
                    statement.setInt(2, productId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            currentQty = rs.getInt("quantity");
                        }
                    }
                }

                BigDecimal unitPrice = BigDecimal.ZERO;
                int stock = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT price, stock_qty FROM products WHERE product_id = ? AND is_active = 1")) {
                    statement.setInt(1, productId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            AlertUtils.warning("Panier", "Produit introuvable.");
                            return;
                        }
                        unitPrice = rs.getBigDecimal("price");
                        stock = rs.getInt("stock_qty");
                    }
                }

                int finalQty = Math.min(stock, (currentQty == null ? 0 : currentQty) + Math.max(1, quantity));
                if (finalQty <= 0) {
                    connection.rollback();
                    AlertUtils.warning("Panier", "Produit en rupture de stock.");
                    return;
                }

                if (currentQty == null) {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO cart_items (cart_id, product_id, quantity, unit_price_at_add, added_at, updated_at)
                            VALUES (?, ?, ?, ?, NOW(), NOW())
                            """)) {
                        statement.setInt(1, cart.cartId());
                        statement.setInt(2, productId);
                        statement.setInt(3, finalQty);
                        statement.setBigDecimal(4, unitPrice);
                        statement.executeUpdate();
                    }
                } else {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            UPDATE cart_items
                            SET quantity = ?, updated_at = NOW()
                            WHERE cart_id = ? AND product_id = ?
                            """)) {
                        statement.setInt(1, finalQty);
                        statement.setInt(2, cart.cartId());
                        statement.setInt(3, productId);
                        statement.executeUpdate();
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE carts SET updated_at = NOW() WHERE cart_id = ?")) {
                    statement.setInt(1, cart.cartId());
                    statement.executeUpdate();
                }

                connection.commit();
                refreshCartCount();
                AlertUtils.info("Panier", "Produit ajoute au panier.");
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Panier", "Ajout impossible.\n" + ex.getMessage());
        }
    }

    private void refreshCartCount() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            cartCountLabel.setText("Panier (0)");
            return;
        }
        String sql = """
                SELECT COALESCE(SUM(ci.quantity), 0) AS total_qty
                FROM carts c
                LEFT JOIN cart_items ci ON ci.cart_id = c.cart_id
                WHERE c.user_id = ?
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, user.getUserId());
            try (ResultSet rs = statement.executeQuery()) {
                int qty = rs.next() ? rs.getInt("total_qty") : 0;
                cartCountLabel.setText("Panier (" + qty + ")");
            }
        } catch (SQLException ex) {
            cartCountLabel.setText("Panier (?)");
        }
    }

    private CartInfo loadOrCreateCart(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT cart_id, status FROM carts WHERE user_id = ? LIMIT 1")) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new CartInfo(rs.getInt("cart_id"), rs.getString("status"));
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO carts (user_id, status, created_at, updated_at, locked_at) VALUES (?, 'OPEN', NOW(), NOW(), NULL)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new CartInfo(keys.getInt(1), "OPEN");
                }
            }
        }
        throw new SQLException("Creation panier impossible.");
    }

    private static void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        int i = 1;
        for (Object param : params) {
            if (param instanceof Integer intValue) {
                statement.setInt(i++, intValue);
            } else if (param instanceof BigDecimal decimal) {
                statement.setBigDecimal(i++, decimal);
            } else if (param instanceof String text) {
                statement.setString(i++, text);
            } else {
                statement.setObject(i++, param);
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static BigDecimal parseBigDecimal(String value) {
        try {
            String text = safe(value);
            return text.isBlank() ? null : new BigDecimal(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private record ProductRow(
            int productId,
            int teamId,
            String teamName,
            String name,
            String description,
            BigDecimal price,
            int stockQty,
            boolean active,
            String imageUrl
    ) {
    }

    private record CartInfo(int cartId, String status) {
    }
}
