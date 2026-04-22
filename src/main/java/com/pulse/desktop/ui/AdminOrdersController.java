package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.ExportService;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminOrdersController implements RouteAwarePage {
    private static final DateTimeFormatter DATE_TIME_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private Label formTitleLabel;
    @FXML
    private TextField orderNumberField;
    @FXML
    private ComboBox<LookupItem> userCombo;
    @FXML
    private ComboBox<LookupItem> cartCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private ComboBox<String> paymentStatusCombo;
    @FXML
    private ComboBox<String> paymentMethodCombo;
    @FXML
    private TextField totalAmountField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextArea shippingAddressArea;
    @FXML
    private TextField paidAtField;
    @FXML
    private TextField shippedAtField;
    @FXML
    private TextField deliveredAtField;
    @FXML
    private Label formFeedbackLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelEditButton;

    @FXML
    private TextField qField;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private ComboBox<String> paymentFilterCombo;
    @FXML
    private TextField userFilterField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private ComboBox<String> directionCombo;
    @FXML
    private Label resultCountLabel;
    @FXML
    private VBox ordersListBox;

    private final ExportService exportService = new ExportService();
    private Integer editingOrderId;
    private List<OrderRow> currentRows = List.of();

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("PENDING", "PAID", "CANCELLED", "SHIPPED", "DELIVERED"));
        statusCombo.getSelectionModel().select("PENDING");

        paymentStatusCombo.setItems(FXCollections.observableArrayList("UNPAID", "PAID", "REFUNDED"));
        paymentStatusCombo.getSelectionModel().select("UNPAID");

        paymentMethodCombo.setItems(FXCollections.observableArrayList("", "CARD", "CASH", "OTHER"));
        paymentMethodCombo.getSelectionModel().select(0);

        statusFilterCombo.setItems(FXCollections.observableArrayList("", "PENDING", "PAID", "CANCELLED", "SHIPPED", "DELIVERED"));
        statusFilterCombo.getSelectionModel().select(0);

        paymentFilterCombo.setItems(FXCollections.observableArrayList("", "UNPAID", "PAID", "REFUNDED"));
        paymentFilterCombo.getSelectionModel().select(0);

        sortCombo.setItems(FXCollections.observableArrayList("created_at", "id", "order_number", "user", "status", "payment_status", "total_amount", "paid_at"));
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
        loadLookups();
        resetForm();
        refresh();
    }

    @FXML
    private void saveOrder() {
        SessionUser admin = requireAdmin();
        if (admin == null) {
            return;
        }

        String orderNumber = safe(orderNumberField.getText());
        if (orderNumber.isBlank()) {
            formFeedbackLabel.setText("Numero commande obligatoire.");
            return;
        }

        LookupItem user = userCombo.getValue();
        LookupItem cart = cartCombo.getValue();
        if (user == null || user.getId() <= 0 || cart == null || cart.getId() <= 0) {
            formFeedbackLabel.setText("Utilisateur et panier obligatoires.");
            return;
        }

        if (!isCartOwnedByUser(cart.getId(), user.getId())) {
            formFeedbackLabel.setText("Le panier selectionne n'appartient pas a cet utilisateur.");
            return;
        }

        BigDecimal total = parseBigDecimal(totalAmountField.getText());
        if (total == null || total.compareTo(BigDecimal.ZERO) < 0) {
            formFeedbackLabel.setText("Montant total invalide.");
            return;
        }

        String status = safe(statusCombo.getValue()).toUpperCase();
        if (!List.of("PENDING", "PAID", "CANCELLED", "SHIPPED", "DELIVERED").contains(status)) {
            status = "PENDING";
        }

        String paymentStatus = safe(paymentStatusCombo.getValue()).toUpperCase();
        if (!List.of("UNPAID", "PAID", "REFUNDED").contains(paymentStatus)) {
            paymentStatus = "UNPAID";
        }

        String paymentMethod = safe(paymentMethodCombo.getValue()).toUpperCase();
        if (!paymentMethod.isBlank() && !List.of("CARD", "CASH", "OTHER").contains(paymentMethod)) {
            paymentMethod = "";
        }

        LocalDateTime paidAt = parseDateTime(paidAtField.getText());
        LocalDateTime shippedAt = parseDateTime(shippedAtField.getText());
        LocalDateTime deliveredAt = parseDateTime(deliveredAtField.getText());

        if (hasInvalidDateInput(paidAtField, paidAt) || hasInvalidDateInput(shippedAtField, shippedAt) || hasInvalidDateInput(deliveredAtField, deliveredAt)) {
            formFeedbackLabel.setText("Dates invalides (yyyy-MM-dd HH:mm).");
            return;
        }

        try (Connection connection = Jdbc.open()) {
            if (editingOrderId == null) {
                String sql = """
                        INSERT INTO orders (
                            order_number, cart_id, user_id, status, payment_method, payment_status,
                            total_amount, shipping_address, phone_for_delivery,
                            created_at, paid_at, shipped_at, delivered_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?)
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bindOrderFields(statement, orderNumber, cart.getId(), user.getId(), status, paymentMethod, paymentStatus,
                            total, shippingAddressArea.getText(), phoneField.getText(), paidAt, shippedAt, deliveredAt, false, null);
                    statement.executeUpdate();
                }
                AlertUtils.info("Admin commandes", "Commande creee.");
            } else {
                String sql = """
                        UPDATE orders
                        SET order_number = ?, cart_id = ?, user_id = ?, status = ?, payment_method = ?, payment_status = ?,
                            total_amount = ?, shipping_address = ?, phone_for_delivery = ?,
                            paid_at = ?, shipped_at = ?, delivered_at = ?
                        WHERE order_id = ?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bindOrderFields(statement, orderNumber, cart.getId(), user.getId(), status, paymentMethod, paymentStatus,
                            total, shippingAddressArea.getText(), phoneField.getText(), paidAt, shippedAt, deliveredAt, true, editingOrderId);
                    statement.executeUpdate();
                }
                AlertUtils.info("Admin commandes", "Commande mise a jour.");
            }
            resetForm();
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Admin commandes", "Enregistrement impossible (numero/correspondance panier deja utilises).\n" + ex.getMessage());
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
        statusFilterCombo.getSelectionModel().select(0);
        paymentFilterCombo.getSelectionModel().select(0);
        userFilterField.clear();
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

    private void loadLookups() {
        List<LookupItem> users = new ArrayList<>();
        List<LookupItem> carts = new ArrayList<>();

        try (Connection connection = Jdbc.open()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT user_id, CONCAT(username, ' (', email, ')') AS label
                    FROM users
                    ORDER BY username ASC
                    LIMIT 900
                    """);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    users.add(new LookupItem(rs.getInt("user_id"), rs.getString("label")));
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT c.cart_id, c.user_id, c.status, u.username
                    FROM carts c
                    LEFT JOIN users u ON u.user_id = c.user_id
                    ORDER BY c.cart_id DESC
                    LIMIT 1500
                    """);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int cartId = rs.getInt("cart_id");
                    String label = "#" + cartId + " - " + safe(rs.getString("username")) + " (" + safe(rs.getString("status")) + ")";
                    carts.add(new LookupItem(cartId, label));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin commandes", "Impossible de charger users/carts.\n" + ex.getMessage());
        }

        userCombo.setItems(FXCollections.observableArrayList(users));
        if (!users.isEmpty()) {
            userCombo.getSelectionModel().select(0);
        }

        cartCombo.setItems(FXCollections.observableArrayList(carts));
        if (!carts.isEmpty()) {
            cartCombo.getSelectionModel().select(0);
        }
    }

    private void refresh() {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    o.order_id,
                    o.order_number,
                    o.user_id,
                    u.username,
                    o.cart_id,
                    o.status,
                    o.payment_status,
                    o.payment_method,
                    o.total_amount,
                    o.shipping_address,
                    o.phone_for_delivery,
                    o.created_at,
                    o.paid_at,
                    o.shipped_at,
                    o.delivered_at
                FROM orders o
                LEFT JOIN users u ON u.user_id = o.user_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        String q = safe(qField.getText());
        if (!q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            sql.append("""
                     AND (
                        LOWER(COALESCE(o.order_number, '')) LIKE ?
                        OR LOWER(COALESCE(o.shipping_address, '')) LIKE ?
                        OR LOWER(COALESCE(o.phone_for_delivery, '')) LIKE ?
                     )
                    """);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        String status = safe(statusFilterCombo.getValue()).toUpperCase();
        if (!status.isBlank()) {
            sql.append(" AND o.status = ? ");
            params.add(status);
        }

        String payment = safe(paymentFilterCombo.getValue()).toUpperCase();
        if (!payment.isBlank()) {
            sql.append(" AND o.payment_status = ? ");
            params.add(payment);
        }

        String userSearch = safe(userFilterField.getText());
        if (!userSearch.isBlank()) {
            String like = "%" + userSearch.toLowerCase() + "%";
            sql.append("""
                     AND (
                        LOWER(COALESCE(u.username, '')) LIKE ?
                        OR LOWER(COALESCE(u.email, '')) LIKE ?
                        OR LOWER(COALESCE(u.display_name, '')) LIKE ?
                     )
                    """);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        String direction = "asc".equalsIgnoreCase(safe(directionCombo.getValue())) ? "ASC" : "DESC";
        String sort = safe(sortCombo.getValue());
        switch (sort) {
            case "id" -> sql.append(" ORDER BY o.order_id ").append(direction);
            case "order_number" -> sql.append(" ORDER BY o.order_number ").append(direction).append(", o.order_id DESC");
            case "user" -> sql.append(" ORDER BY u.username ").append(direction).append(", o.order_id DESC");
            case "status" -> sql.append(" ORDER BY o.status ").append(direction).append(", o.order_id DESC");
            case "payment_status" -> sql.append(" ORDER BY o.payment_status ").append(direction).append(", o.order_id DESC");
            case "total_amount" -> sql.append(" ORDER BY o.total_amount ").append(direction).append(", o.order_id DESC");
            case "paid_at" -> sql.append(" ORDER BY o.paid_at ").append(direction).append(", o.order_id DESC");
            default -> sql.append(" ORDER BY o.created_at ").append(direction).append(", o.order_id DESC");
        }
        sql.append(" LIMIT 500 ");

        List<OrderRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new OrderRow(
                            rs.getInt("order_id"),
                            rs.getString("order_number"),
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getInt("cart_id"),
                            rs.getString("status"),
                            rs.getString("payment_status"),
                            rs.getString("payment_method"),
                            rs.getBigDecimal("total_amount"),
                            rs.getString("shipping_address"),
                            rs.getString("phone_for_delivery"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("paid_at")),
                            toLocalDateTime(rs.getTimestamp("shipped_at")),
                            toLocalDateTime(rs.getTimestamp("delivered_at"))
                    ));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Admin commandes", "Chargement impossible.\n" + ex.getMessage());
            return;
        }

        currentRows = rows;
        resultCountLabel.setText(rows.size() + " resultat(s)");
        renderRows();
    }

    private void renderRows() {
        ordersListBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            ordersListBox.getChildren().add(CompetitionUi.emptyState("Aucune commande trouvee."));
            return;
        }

        for (OrderRow row : currentRows) {
            String line1 = "#" + row.orderId() + " | " + CompetitionUi.emptySafe(row.orderNumber()) + " | " + (row.totalAmount() == null ? "0.00" : row.totalAmount()) + " DT";
            String line2 = "User: " + CompetitionUi.emptySafe(row.username())
                    + " | Cart: #" + row.cartId()
                    + " | Status: " + CompetitionUi.emptySafe(row.status())
                    + " | Payment: " + CompetitionUi.emptySafe(row.paymentStatus())
                    + " | Created: " + CompetitionUi.fmtDateTime(row.createdAt())
                    + " | Paid: " + CompetitionUi.fmtDateTime(row.paidAt());

            Button update = new Button("Update");
            update.getStyleClass().add("btn-ghost");
            update.setOnAction(event -> startEdit(row));

            Button invoice = new Button("Invoice PDF");
            invoice.getStyleClass().add("btn-ghost");
            invoice.setOnAction(event -> exportInvoicePdf(row));

            Button delete = new Button("Delete");
            delete.getStyleClass().add("btn-ghost");
            delete.setOnAction(event -> deleteOrder(row.orderId()));

            ordersListBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, update, invoice, delete));
        }
    }

    private void startEdit(OrderRow row) {
        editingOrderId = row.orderId();
        formTitleLabel.setText("MODIFIER COMMANDE #" + row.orderId());
        saveButton.setText("Mettre a jour");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);

        orderNumberField.setText(safe(row.orderNumber()));
        selectById(userCombo, row.userId());
        selectById(cartCombo, row.cartId());
        statusCombo.getSelectionModel().select(CompetitionUi.emptySafe(row.status()));
        paymentStatusCombo.getSelectionModel().select(CompetitionUi.emptySafe(row.paymentStatus()));
        paymentMethodCombo.getSelectionModel().select(safe(row.paymentMethod()));
        totalAmountField.setText(row.totalAmount() == null ? "0.00" : row.totalAmount().toPlainString());
        shippingAddressArea.setText(safe(row.shippingAddress()));
        phoneField.setText(safe(row.phoneForDelivery()));
        paidAtField.setText(formatDate(row.paidAt()));
        shippedAtField.setText(formatDate(row.shippedAt()));
        deliveredAtField.setText(formatDate(row.deliveredAt()));
        formFeedbackLabel.setText("Mode edition actif.");
    }

    private void deleteOrder(int orderId) {
        if (!confirmDelete()) {
            return;
        }

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM orders WHERE order_id = ?")) {
            statement.setInt(1, orderId);
            int affected = statement.executeUpdate();
            if (affected <= 0) {
                AlertUtils.warning("Admin commandes", "Commande introuvable.");
                return;
            }
            if (editingOrderId != null && editingOrderId == orderId) {
                resetForm();
            }
            AlertUtils.info("Admin commandes", "Commande supprimee.");
            refresh();
        } catch (SQLException ex) {
            AlertUtils.error("Admin commandes", "Suppression impossible.\n" + ex.getMessage());
        }
    }

    private void exportInvoicePdf(OrderRow order) {
        String sql = """
                SELECT
                    p.name AS product_name,
                    ci.quantity,
                    ci.unit_price_at_add,
                    (ci.quantity * ci.unit_price_at_add) AS line_total
                FROM cart_items ci
                INNER JOIN products p ON p.product_id = ci.product_id
                WHERE ci.cart_id = ?
                ORDER BY p.name ASC
                """;

        List<String> headers = List.of("Produit", "Quantite", "Prix unitaire", "Total ligne");
        List<List<String>> rows = new ArrayList<>();

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, order.cartId());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(List.of(
                            CompetitionUi.emptySafe(rs.getString("product_name")),
                            Integer.toString(rs.getInt("quantity")),
                            rs.getBigDecimal("unit_price_at_add") == null ? "0.00" : rs.getBigDecimal("unit_price_at_add").toPlainString(),
                            rs.getBigDecimal("line_total") == null ? "0.00" : rs.getBigDecimal("line_total").toPlainString()
                    ));
                }
            }

            if (rows.isEmpty()) {
                rows.add(List.of("Aucun article", "0", "0.00", "0.00"));
            }

            var file = exportService.exportPdf("Invoice " + CompetitionUi.emptySafe(order.orderNumber()), headers, rows);
            exportService.openFile(file);
            AlertUtils.info("Invoice", "Facture generee: " + file.toAbsolutePath());
        } catch (SQLException | IOException ex) {
            AlertUtils.error("Invoice", "Generation facture impossible.\n" + ex.getMessage());
        }
    }

    private void export(boolean pdf) {
        if (currentRows.isEmpty()) {
            AlertUtils.info("Export", "Aucune donnee a exporter.");
            return;
        }

        List<String> headers = List.of("ID", "Order number", "User", "Cart", "Status", "Payment status", "Payment method", "Total", "Created", "Paid", "Shipped", "Delivered");
        List<List<String>> rows = new ArrayList<>();
        for (OrderRow row : currentRows) {
            rows.add(List.of(
                    Integer.toString(row.orderId()),
                    CompetitionUi.emptySafe(row.orderNumber()),
                    CompetitionUi.emptySafe(row.username()),
                    Integer.toString(row.cartId()),
                    CompetitionUi.emptySafe(row.status()),
                    CompetitionUi.emptySafe(row.paymentStatus()),
                    CompetitionUi.emptySafe(row.paymentMethod()),
                    row.totalAmount() == null ? "0.00" : row.totalAmount().toPlainString(),
                    CompetitionUi.fmtDateTime(row.createdAt()),
                    CompetitionUi.fmtDateTime(row.paidAt()),
                    CompetitionUi.fmtDateTime(row.shippedAt()),
                    CompetitionUi.fmtDateTime(row.deliveredAt())
            ));
        }

        try {
            var file = pdf
                    ? exportService.exportPdf("Commandes", headers, rows)
                    : exportService.exportExcel("admin_orders", headers, rows);
            exportService.openFile(file);
            AlertUtils.info("Export", "Fichier genere: " + file.toAbsolutePath());
        } catch (IOException ex) {
            AlertUtils.error("Export", "Impossible de generer l'export.\n" + ex.getMessage());
        }
    }

    private boolean isCartOwnedByUser(int cartId, int userId) {
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement("SELECT user_id FROM carts WHERE cart_id = ? LIMIT 1")) {
            statement.setInt(1, cartId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return rs.getInt("user_id") == userId;
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    private void bindOrderFields(
            PreparedStatement statement,
            String orderNumber,
            int cartId,
            int userId,
            String status,
            String paymentMethod,
            String paymentStatus,
            BigDecimal total,
            String shippingAddress,
            String phone,
            LocalDateTime paidAt,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt,
            boolean withOrderId,
            Integer orderId
    ) throws SQLException {
        int i = 1;
        statement.setString(i++, orderNumber);
        statement.setInt(i++, cartId);
        statement.setInt(i++, userId);
        statement.setString(i++, status);
        statement.setString(i++, paymentMethod.isBlank() ? null : paymentMethod);
        statement.setString(i++, paymentStatus);
        statement.setBigDecimal(i++, total);
        statement.setString(i++, nullIfBlank(shippingAddress));
        statement.setString(i++, nullIfBlank(phone));
        if (paidAt == null) {
            statement.setTimestamp(i++, null);
        } else {
            statement.setTimestamp(i++, Timestamp.valueOf(paidAt));
        }
        if (shippedAt == null) {
            statement.setTimestamp(i++, null);
        } else {
            statement.setTimestamp(i++, Timestamp.valueOf(shippedAt));
        }
        if (deliveredAt == null) {
            statement.setTimestamp(i++, null);
        } else {
            statement.setTimestamp(i++, Timestamp.valueOf(deliveredAt));
        }

        if (withOrderId && orderId != null) {
            statement.setInt(i, orderId);
        }
    }

    private void resetForm() {
        editingOrderId = null;
        formTitleLabel.setText("NOUVELLE COMMANDE");
        saveButton.setText("Creer commande");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);

        orderNumberField.clear();
        statusCombo.getSelectionModel().select("PENDING");
        paymentStatusCombo.getSelectionModel().select("UNPAID");
        paymentMethodCombo.getSelectionModel().select(0);
        totalAmountField.setText("0.00");
        phoneField.clear();
        shippingAddressArea.clear();
        paidAtField.clear();
        shippedAtField.clear();
        deliveredAtField.clear();
        formFeedbackLabel.setText("");

        if (userCombo.getValue() == null && !userCombo.getItems().isEmpty()) {
            userCombo.getSelectionModel().select(0);
        }
        if (cartCombo.getValue() == null && !cartCombo.getItems().isEmpty()) {
            cartCombo.getSelectionModel().select(0);
        }
    }

    private boolean confirmDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression commande");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer cette commande ?");
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

    private static BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(safe(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LocalDateTime parseDateTime(String raw) {
        String value = safe(raw);
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_INPUT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static boolean hasInvalidDateInput(TextField field, LocalDateTime parsed) {
        return !safe(field.getText()).isBlank() && parsed == null;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String formatDate(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_INPUT);
    }

    private record OrderRow(
            int orderId,
            String orderNumber,
            int userId,
            String username,
            int cartId,
            String status,
            String paymentStatus,
            String paymentMethod,
            BigDecimal totalAmount,
            String shippingAddress,
            String phoneForDelivery,
            LocalDateTime createdAt,
            LocalDateTime paidAt,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt
    ) {
    }
}
