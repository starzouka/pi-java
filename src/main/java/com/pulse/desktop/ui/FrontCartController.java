package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class FrontCartController implements RouteAwarePage {
    @FXML
    private TextField qField;
    @FXML
    private ComboBox<LookupItem> teamCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label statusLabel;
    @FXML
    private Label totalQtyLabel;
    @FXML
    private Label subtotalLabel;
    @FXML
    private Label visibleQtyLabel;
    @FXML
    private Label visibleSubtotalLabel;
    @FXML
    private Label feedbackLabel;
    @FXML
    private VBox itemsBox;

    @FXML
    private TextField shippingAddressField;
    @FXML
    private TextField phoneField;
    @FXML
    private ComboBox<String> paymentMethodCombo;

    private SessionUser user;
    private CartInfo currentCart;
    private List<CartRow> currentRows = List.of();

    @FXML
    public void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList("added_asc", "added_desc", "name", "price_high", "price_low", "qty_high"));
        sortCombo.getSelectionModel().select("added_asc");

        paymentMethodCombo.setItems(FXCollections.observableArrayList("CARD", "CASH", "OTHER"));
        paymentMethodCombo.getSelectionModel().select("CARD");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        user = requireUser();
        if (user == null) {
            return;
        }
        refresh();
    }

    @FXML
    private void applyFilters() {
        refresh();
    }

    @FXML
    private void resetFilters() {
        qField.clear();
        sortCombo.getSelectionModel().select("added_asc");
        if (!teamCombo.getItems().isEmpty()) {
            teamCombo.getSelectionModel().select(0);
        }
        refresh();
    }

    @FXML
    private void checkout() {
        if (user == null) {
            return;
        }
        if (currentCart == null) {
            feedbackLabel.setText("Votre panier est vide.");
            return;
        }
        if (!"OPEN".equalsIgnoreCase(currentCart.status())) {
            feedbackLabel.setText("Ce panier est verrouille.");
            return;
        }

        String paymentMethod = paymentMethodCombo.getValue() == null ? "CARD" : paymentMethodCombo.getValue();
        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Integer existingOrder = findOrderByCart(connection, currentCart.cartId());
                if (existingOrder != null) {
                    connection.rollback();
                    RouteContext.putInt(RouteContext.KEY_ORDER_ID, existingOrder);
                    Navigator.goTo("front_order_detail");
                    return;
                }

                BigDecimal subtotal = loadCartSubtotal(connection, currentCart.cartId());
                if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
                    connection.rollback();
                    feedbackLabel.setText("Votre panier est vide.");
                    return;
                }

                String orderNumber = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
                String status = "CASH".equals(paymentMethod) ? "PENDING" : "PAID";
                String paymentStatus = "CASH".equals(paymentMethod) ? "UNPAID" : "PAID";

                int orderId;
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO orders (
                            order_number, cart_id, user_id, status, payment_method, payment_status,
                            total_amount, shipping_address, phone_for_delivery, created_at, paid_at, shipped_at, delivered_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, NULL, NULL)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, orderNumber);
                    statement.setInt(2, currentCart.cartId());
                    statement.setInt(3, user.getUserId());
                    statement.setString(4, status);
                    statement.setString(5, paymentMethod);
                    statement.setString(6, paymentStatus);
                    statement.setBigDecimal(7, subtotal);
                    statement.setString(8, nullIfBlank(shippingAddressField.getText()));
                    statement.setString(9, nullIfBlank(phoneField.getText()));
                    if ("CASH".equals(paymentMethod)) {
                        statement.setObject(10, null);
                    } else {
                        statement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
                    }
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Creation commande impossible.");
                        }
                        orderId = keys.getInt(1);
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE carts
                        SET status = 'ORDERED', locked_at = NOW(), updated_at = NOW()
                        WHERE cart_id = ?
                        """)) {
                    statement.setInt(1, currentCart.cartId());
                    statement.executeUpdate();
                }

                connection.commit();
                feedbackLabel.setText("Commande confirmee.");
                RouteContext.putInt(RouteContext.KEY_ORDER_ID, orderId);
                Navigator.goTo("front_orders");
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException ex) {
            AlertUtils.error("Panier", "Validation commande impossible.\n" + ex.getMessage());
        }
    }

    @FXML
    private void goShop() {
        Navigator.goTo("front_shop");
    }

    @FXML
    private void goOrders() {
        Navigator.goTo("front_orders");
    }

    @FXML
    private void goDashboard() {
        Navigator.goTo("front_dashboard");
    }

    @FXML
    private void goProfile() {
        Navigator.goTo("front_profile");
    }

    @FXML
    private void goPlayers() {
        Navigator.goTo("front_players");
    }

    @FXML
    private void goFriends() {
        Navigator.goTo("front_friends");
    }

    @FXML
    private void goMessages() {
        Navigator.goTo("front_messages");
    }

    @FXML
    private void goFeed() {
        Navigator.goTo("front_feed");
    }

    @FXML
    private void goMyTeams() {
        Navigator.goTo("front_my_teams");
    }

    @FXML
    private void goMyRequests() {
        Navigator.goTo("front_my_requests");
    }

    @FXML
    private void goNotifications() {
        Navigator.goTo("front_notifications");
    }

    private void refresh() {
        itemsBox.getChildren().clear();
        currentCart = loadCartForUser();
        loadTeamOptions();
        if (currentCart == null) {
            statusLabel.setText("OPEN");
            totalQtyLabel.setText("0");
            subtotalLabel.setText("0.00 DT");
            visibleQtyLabel.setText("0");
            visibleSubtotalLabel.setText("0.00 DT");
            itemsBox.getChildren().add(CompetitionUi.emptyState("Votre panier est vide."));
            return;
        }

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_id,
                    p.name AS product_name,
                    p.team_id,
                    t.name AS team_name,
                    p.stock_qty,
                    ci.quantity,
                    ci.unit_price_at_add,
                    (ci.quantity * ci.unit_price_at_add) AS line_total
                FROM cart_items ci
                INNER JOIN products p ON p.product_id = ci.product_id
                LEFT JOIN teams t ON t.team_id = p.team_id
                WHERE ci.cart_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(currentCart.cartId());

        String q = safe(qField.getText());
        if (!q.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(p.name) LIKE ?
                        OR LOWER(COALESCE(p.description, '')) LIKE ?
                        OR LOWER(COALESCE(p.sku, '')) LIKE ?
                        OR LOWER(COALESCE(t.name, '')) LIKE ?
                     )
                    """);
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        LookupItem team = teamCombo.getValue();
        if (team != null && team.getId() > 0) {
            sql.append(" AND p.team_id = ? ");
            params.add(team.getId());
        }

        String sort = sortCombo.getValue() == null ? "added_asc" : sortCombo.getValue();
        switch (sort) {
            case "added_desc" -> sql.append(" ORDER BY ci.added_at DESC, p.name ASC ");
            case "name" -> sql.append(" ORDER BY p.name ASC, ci.added_at DESC ");
            case "price_high" -> sql.append(" ORDER BY ci.unit_price_at_add DESC, ci.added_at DESC ");
            case "price_low" -> sql.append(" ORDER BY ci.unit_price_at_add ASC, ci.added_at DESC ");
            case "qty_high" -> sql.append(" ORDER BY ci.quantity DESC, ci.added_at DESC ");
            default -> sql.append(" ORDER BY ci.added_at ASC, p.name ASC ");
        }

        List<CartRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new CartRow(
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getInt("team_id"),
                            rs.getString("team_name"),
                            rs.getInt("stock_qty"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price_at_add"),
                            rs.getBigDecimal("line_total")
                    ));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Panier", "Chargement impossible.\n" + ex.getMessage());
            return;
        }

        currentRows = rows;
        renderRows();
        renderSummary();
    }

    private void renderRows() {
        itemsBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            itemsBox.getChildren().add(CompetitionUi.emptyState("Aucun article ne correspond aux filtres."));
            return;
        }
        boolean editable = "OPEN".equalsIgnoreCase(currentCart.status());

        for (CartRow row : currentRows) {
            TextField qtyField = new TextField(Integer.toString(row.quantity()));
            qtyField.getStyleClass().add("input");
            qtyField.setPrefWidth(80);
            qtyField.setDisable(!editable);

            Button update = new Button("OK");
            update.getStyleClass().add("btn-ghost");
            update.setDisable(!editable);
            update.setOnAction(e -> updateItem(row.productId(), qtyField.getText()));

            Button remove = new Button(editable ? "Retirer" : "Verrouille");
            remove.getStyleClass().add("btn-ghost");
            remove.setDisable(!editable);
            remove.setOnAction(e -> removeItem(row.productId()));

            Button detail = new Button("Detail");
            detail.getStyleClass().add("btn-ghost");
            detail.setOnAction(e -> {
                RouteContext.putInt(RouteContext.KEY_PRODUCT_ID, row.productId());
                Navigator.goTo("front_product_detail");
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            HBox actions = new HBox(8, qtyField, update, spacer, detail, remove);

            VBox rowBox = new VBox(6,
                    new Label(row.productName() + " | " + row.teamName() + " | " + row.unitPrice() + " DT | Stock: " + row.stockQty()),
                    new Label("Qte: " + row.quantity() + " | Total ligne: " + row.lineTotal() + " DT"),
                    actions
            );
            rowBox.getStyleClass().add("list-item");
            itemsBox.getChildren().add(rowBox);
        }
    }

    private void renderSummary() {
        statusLabel.setText(currentCart.status());
        int visibleQty = 0;
        BigDecimal visibleSubtotal = BigDecimal.ZERO;
        for (CartRow row : currentRows) {
            visibleQty += row.quantity();
            visibleSubtotal = visibleSubtotal.add(row.lineTotal());
        }
        visibleQtyLabel.setText(Integer.toString(visibleQty));
        visibleSubtotalLabel.setText(visibleSubtotal + " DT");

        String sumSql = """
                SELECT
                    COALESCE(SUM(quantity), 0) AS total_qty,
                    COALESCE(SUM(quantity * unit_price_at_add), 0) AS subtotal
                FROM cart_items
                WHERE cart_id = ?
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sumSql)) {
            statement.setInt(1, currentCart.cartId());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    totalQtyLabel.setText(Integer.toString(rs.getInt("total_qty")));
                    subtotalLabel.setText(rs.getBigDecimal("subtotal") + " DT");
                } else {
                    totalQtyLabel.setText("0");
                    subtotalLabel.setText("0.00 DT");
                }
            }
        } catch (SQLException ex) {
            totalQtyLabel.setText("?");
            subtotalLabel.setText("?");
        }
    }

    private void updateItem(int productId, String quantityRaw) {
        if (currentCart == null || !"OPEN".equalsIgnoreCase(currentCart.status())) {
            feedbackLabel.setText("Ce panier est verrouille.");
            return;
        }

        Integer quantity = parseInt(quantityRaw);
        if (quantity == null) {
            feedbackLabel.setText("Quantite invalide.");
            return;
        }
        if (quantity <= 0) {
            removeItem(productId);
            return;
        }

        int safeQty = Math.max(1, quantity);
        CartRow row = findRow(productId);
        if (row != null && row.stockQty() > 0 && safeQty > row.stockQty()) {
            safeQty = row.stockQty();
            feedbackLabel.setText("Quantite limitee au stock disponible.");
        }

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE cart_items
                     SET quantity = ?, updated_at = NOW()
                     WHERE cart_id = ? AND product_id = ?
                     """)) {
            statement.setInt(1, safeQty);
            statement.setInt(2, currentCart.cartId());
            statement.setInt(3, productId);
            statement.executeUpdate();
            feedbackLabel.setText("Quantite mise a jour.");
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Panier", "Mise a jour impossible.\n" + ex.getMessage());
        }
    }

    private void removeItem(int productId) {
        if (currentCart == null || !"OPEN".equalsIgnoreCase(currentCart.status())) {
            feedbackLabel.setText("Ce panier est verrouille.");
            return;
        }

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?")) {
            statement.setInt(1, currentCart.cartId());
            statement.setInt(2, productId);
            statement.executeUpdate();
            feedbackLabel.setText("Produit retire du panier.");
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Panier", "Suppression impossible.\n" + ex.getMessage());
        }
    }

    private CartRow findRow(int productId) {
        for (CartRow row : currentRows) {
            if (row.productId() == productId) {
                return row;
            }
        }
        return null;
    }

    private void loadTeamOptions() {
        Integer previousSelection = teamCombo.getValue() == null ? null : teamCombo.getValue().getId();
        List<LookupItem> options = new ArrayList<>();
        options.add(new LookupItem(0, "Toutes les equipes"));
        if (currentCart != null) {
            String sql = """
                    SELECT DISTINCT t.team_id, t.name
                    FROM cart_items ci
                    INNER JOIN products p ON p.product_id = ci.product_id
                    INNER JOIN teams t ON t.team_id = p.team_id
                    WHERE ci.cart_id = ?
                    ORDER BY t.name ASC
                    """;
            try (Connection connection = Jdbc.open();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, currentCart.cartId());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        options.add(new LookupItem(rs.getInt("team_id"), rs.getString("name")));
                    }
                }
            } catch (SQLException ignored) {
                // keep defaults
            }
        }
        teamCombo.setItems(FXCollections.observableArrayList(options));
        if (previousSelection != null) {
            for (LookupItem option : options) {
                if (option.getId() == previousSelection) {
                    teamCombo.getSelectionModel().select(option);
                    return;
                }
            }
        }
        teamCombo.getSelectionModel().select(0);
    }

    private CartInfo loadCartForUser() {
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT cart_id, status FROM carts WHERE user_id = ? LIMIT 1")) {
            statement.setInt(1, user.getUserId());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new CartInfo(rs.getInt("cart_id"), rs.getString("status"));
            }
        } catch (SQLException ex) {
            AlertUtils.error("Panier", "Impossible de charger le panier.\n" + ex.getMessage());
            return null;
        }
    }

    private Integer findOrderByCart(Connection connection, int cartId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT order_id FROM orders WHERE cart_id = ? LIMIT 1")) {
            statement.setInt(1, cartId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("order_id") : null;
            }
        }
    }

    private BigDecimal loadCartSubtotal(Connection connection, int cartId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(quantity * unit_price_at_add), 0) AS subtotal FROM cart_items WHERE cart_id = ?")) {
            statement.setInt(1, cartId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("subtotal") : BigDecimal.ZERO;
            }
        }
    }

    private static SessionUser requireUser() {
        SessionUser user = SessionContext.getCurrentUser();
        if (user == null) {
            Navigator.goTo("front_login");
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

    private record CartInfo(int cartId, String status) {
    }

    private record CartRow(
            int productId,
            String productName,
            int teamId,
            String teamName,
            int stockQty,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
