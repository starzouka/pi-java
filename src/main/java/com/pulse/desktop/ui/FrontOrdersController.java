package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FrontOrdersController implements RouteAwarePage {
    @FXML
    private TextField qField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private ComboBox<String> sortCombo;

    @FXML
    private Label resultCountLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label shippingLabel;
    @FXML
    private Label pendingLabel;
    @FXML
    private VBox ordersBox;

    private SessionUser user;
    private List<OrderRow> currentRows = List.of();

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("", "PENDING", "PAID", "CANCELLED", "SHIPPED", "DELIVERED"));
        statusCombo.getSelectionModel().select(0);

        sortCombo.setItems(FXCollections.observableArrayList("latest", "oldest", "amount_high", "amount_low", "status"));
        sortCombo.getSelectionModel().select("latest");
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
        statusCombo.getSelectionModel().select(0);
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        sortCombo.getSelectionModel().select("latest");
        refresh();
    }

    @FXML
    private void goCart() {
        Navigator.goTo("front_cart");
    }

    @FXML
    private void goShop() {
        Navigator.goTo("front_shop");
    }

    @FXML
    private void goNotifications() {
        Navigator.goTo("front_notifications");
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

    private void refresh() {
        refreshSummary();

        StringBuilder sql = new StringBuilder("""
                SELECT
                    o.order_id,
                    o.order_number,
                    o.status,
                    o.payment_status,
                    o.total_amount,
                    o.created_at
                FROM orders o
                WHERE o.user_id = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(user.getUserId());

        String status = safe(statusCombo.getValue()).toUpperCase(Locale.ROOT);
        if (!status.isBlank()) {
            sql.append(" AND o.status = ? ");
            params.add(status);
        }

        LocalDate from = fromDatePicker.getValue();
        if (from != null) {
            sql.append(" AND o.created_at >= ? ");
            params.add(Timestamp.valueOf(LocalDateTime.of(from, LocalTime.MIN)));
        }

        LocalDate to = toDatePicker.getValue();
        if (to != null) {
            sql.append(" AND o.created_at <= ? ");
            params.add(Timestamp.valueOf(LocalDateTime.of(to, LocalTime.MAX)));
        }

        String q = safe(qField.getText());
        if (!q.isBlank()) {
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            sql.append("""
                     AND (
                        LOWER(o.order_number) LIKE ?
                        OR LOWER(COALESCE(o.shipping_address, '')) LIKE ?
                        OR LOWER(COALESCE(o.payment_status, '')) LIKE ?
                        OR LOWER(COALESCE(o.phone_for_delivery, '')) LIKE ?
                     )
                    """);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        String sort = safe(sortCombo.getValue());
        switch (sort) {
            case "oldest" -> sql.append(" ORDER BY o.created_at ASC, o.order_id ASC ");
            case "amount_high" -> sql.append(" ORDER BY o.total_amount DESC, o.created_at DESC ");
            case "amount_low" -> sql.append(" ORDER BY o.total_amount ASC, o.created_at DESC ");
            case "status" -> sql.append(" ORDER BY o.status ASC, o.created_at DESC ");
            default -> sql.append(" ORDER BY o.created_at DESC, o.order_id DESC ");
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
                            rs.getString("status"),
                            rs.getString("payment_status"),
                            rs.getBigDecimal("total_amount"),
                            toLocalDateTime(rs.getTimestamp("created_at"))
                    ));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Mes commandes", "Chargement impossible.\n" + ex.getMessage());
            return;
        }

        currentRows = rows;
        resultCountLabel.setText(rows.size() + " resultat(s)");
        renderRows();
    }

    private void refreshSummary() {
        String sql = """
                SELECT
                    COUNT(*) AS total_count,
                    SUM(CASE WHEN status = 'SHIPPED' THEN 1 ELSE 0 END) AS shipping_count,
                    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending_count
                FROM orders
                WHERE user_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, user.getUserId());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    totalLabel.setText(Integer.toString(rs.getInt("total_count")));
                    shippingLabel.setText(Integer.toString(rs.getInt("shipping_count")));
                    pendingLabel.setText(Integer.toString(rs.getInt("pending_count")));
                } else {
                    totalLabel.setText("0");
                    shippingLabel.setText("0");
                    pendingLabel.setText("0");
                }
            }
        } catch (SQLException ex) {
            totalLabel.setText("?");
            shippingLabel.setText("?");
            pendingLabel.setText("?");
        }
    }

    private void renderRows() {
        ordersBox.getChildren().clear();
        if (currentRows.isEmpty()) {
            ordersBox.getChildren().add(CompetitionUi.emptyState("Aucune commande pour ces filtres."));
            return;
        }

        for (OrderRow row : currentRows) {
            String line1 = CompetitionUi.emptySafe(row.orderNumber())
                    + " | " + (row.totalAmount() == null ? "0.00" : row.totalAmount()) + " DT";
            String line2 = CompetitionUi.fmtDateTime(row.createdAt())
                    + " | Paiement: " + CompetitionUi.emptySafe(row.paymentStatus())
                    + " | Statut: " + CompetitionUi.emptySafe(row.status());

            Button detail = new Button("Detail");
            detail.getStyleClass().add("btn-ghost");
            detail.setOnAction(event -> {
                RouteContext.putInt(RouteContext.KEY_ORDER_ID, row.orderId());
                Navigator.goTo("front_order_detail");
            });

            ordersBox.getChildren().add(CompetitionUi.listRowWithActions(line1, line2, detail));
        }
    }

    private static SessionUser requireUser() {
        SessionUser current = SessionContext.getCurrentUser();
        if (current == null) {
            Navigator.goTo("front_login");
            return null;
        }
        return current;
    }

    private static void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        int i = 1;
        for (Object value : params) {
            if (value instanceof Integer intValue) {
                statement.setInt(i++, intValue);
            } else if (value instanceof String text) {
                statement.setString(i++, text);
            } else if (value instanceof Timestamp timestamp) {
                statement.setTimestamp(i++, timestamp);
            } else {
                statement.setObject(i++, value);
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private record OrderRow(
            int orderId,
            String orderNumber,
            String status,
            String paymentStatus,
            BigDecimal totalAmount,
            LocalDateTime createdAt
    ) {
    }
}
