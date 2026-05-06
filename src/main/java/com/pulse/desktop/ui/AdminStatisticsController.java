package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminStatisticsController implements RouteAwarePage {
    private static final DateTimeFormatter DAY_SQL = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    private ComboBox<Integer> daysCombo;

    @FXML
    private Label periodLabel;
    @FXML
    private Label totalProductsLabel;
    @FXML
    private Label activeProductsLabel;
    @FXML
    private Label totalOrdersLabel;
    @FXML
    private Label paidOrdersLabel;
    @FXML
    private Label paidRevenueLabel;
    @FXML
    private Label reviewsCountLabel;
    @FXML
    private Label averageRatingLabel;

    @FXML
    private VBox salesSeriesBox;
    @FXML
    private VBox topSellingBox;
    @FXML
    private VBox topRatedBox;

    @FXML
    public void initialize() {
        daysCombo.setItems(FXCollections.observableArrayList(7, 30, 90, 180));
        daysCombo.getSelectionModel().select(Integer.valueOf(30));
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
        Integer selected = daysCombo.getValue();
        int days = selected == null ? 30 : Math.max(1, Math.min(365, selected));
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(days - 1L);
        String fromSql = fromDate + " 00:00:00";

        loadKpis(fromSql, fromDate, today);
        loadSalesSeries(fromSql, fromDate, today);
        loadTopSelling(fromSql);
        loadTopRated();
    }

    private void loadKpis(String fromSql, LocalDate fromDate, LocalDate today) {
        int totalProducts = 0;
        int activeProducts = 0;
        int totalOrders = 0;
        int paidOrders = 0;
        BigDecimal paidRevenue = BigDecimal.ZERO;
        int reviewsCount = 0;
        BigDecimal averageRating = BigDecimal.ZERO;

        try (Connection connection = Jdbc.open()) {
            totalProducts = queryInt(connection, "SELECT COUNT(*) FROM products");
            activeProducts = queryInt(connection, "SELECT COUNT(*) FROM products WHERE is_active = 1");

            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM orders WHERE created_at >= ?")) {
                statement.setString(1, fromSql);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        totalOrders = rs.getInt(1);
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM orders WHERE created_at >= ? AND payment_status = 'PAID'")) {
                statement.setString(1, fromSql);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        paidOrders = rs.getInt(1);
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE created_at >= ? AND payment_status = 'PAID'")) {
                statement.setString(1, fromSql);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        paidRevenue = rs.getBigDecimal(1);
                        if (paidRevenue == null) {
                            paidRevenue = BigDecimal.ZERO;
                        }
                    }
                }
            }

            reviewsCount = queryInt(connection,
                    "SELECT COUNT(*) FROM comments WHERE product_id IS NOT NULL AND is_deleted = 0 AND rating IS NOT NULL");

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COALESCE(AVG(rating), 0) FROM comments WHERE product_id IS NOT NULL AND is_deleted = 0 AND rating IS NOT NULL");
                 ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    averageRating = rs.getBigDecimal(1);
                    if (averageRating == null) {
                        averageRating = BigDecimal.ZERO;
                    }
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Statistiques", "Impossible de charger les KPI.\n" + ex.getMessage());
        }

        periodLabel.setText(fromDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " - " + today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        totalProductsLabel.setText(Integer.toString(totalProducts));
        activeProductsLabel.setText(Integer.toString(activeProducts));
        totalOrdersLabel.setText(Integer.toString(totalOrders));
        paidOrdersLabel.setText(Integer.toString(paidOrders));
        paidRevenueLabel.setText(paidRevenue.toPlainString() + " DT");
        reviewsCountLabel.setText(Integer.toString(reviewsCount));
        averageRatingLabel.setText(averageRating.toPlainString() + "/5");
    }

    private void loadSalesSeries(String fromSql, LocalDate fromDate, LocalDate today) {
        salesSeriesBox.getChildren().clear();

        Map<String, DaySeries> byDay = new HashMap<>();
        String sql = """
                SELECT DATE(o.created_at) AS day_label,
                       COUNT(*) AS orders_count,
                       COALESCE(SUM(o.total_amount), 0) AS revenue_amount
                FROM orders o
                WHERE o.created_at >= ?
                GROUP BY DATE(o.created_at)
                ORDER BY day_label ASC
                """;

        int maxOrders = 1;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fromSql);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String day = rs.getString("day_label");
                    int orders = rs.getInt("orders_count");
                    BigDecimal revenue = rs.getBigDecimal("revenue_amount");
                    if (revenue == null) {
                        revenue = BigDecimal.ZERO;
                    }
                    byDay.put(day, new DaySeries(orders, revenue));
                    maxOrders = Math.max(maxOrders, orders);
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Statistiques", "Impossible de charger l'evolution journaliere.\n" + ex.getMessage());
        }

        LocalDate cursor = fromDate;
        while (!cursor.isAfter(today)) {
            String key = cursor.format(DAY_SQL);
            DaySeries daySeries = byDay.getOrDefault(key, new DaySeries(0, BigDecimal.ZERO));

            Label text = new Label(cursor.format(DateTimeFormatter.ofPattern("dd/MM"))
                    + " | Orders: " + daySeries.orders()
                    + " | Revenue: " + daySeries.revenue().toPlainString() + " DT");
            text.getStyleClass().add("list-item-meta");

            ProgressBar bar = new ProgressBar(daySeries.orders() / (double) maxOrders);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("admin-users-progress");

            VBox row = new VBox(4, text, bar);
            row.getStyleClass().add("list-item");
            salesSeriesBox.getChildren().add(row);
            cursor = cursor.plusDays(1);
        }
    }

    private void loadTopSelling(String fromSql) {
        topSellingBox.getChildren().clear();

        String sql = """
                SELECT
                    p.product_id,
                    p.name,
                    SUM(ci.quantity) AS quantity_sold,
                    SUM(ci.quantity * ci.unit_price_at_add) AS gross_revenue
                FROM orders o
                INNER JOIN cart_items ci ON ci.cart_id = o.cart_id
                INNER JOIN products p ON p.product_id = ci.product_id
                WHERE o.created_at >= ?
                GROUP BY p.product_id, p.name
                ORDER BY quantity_sold DESC, gross_revenue DESC, p.name ASC
                LIMIT 6
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fromSql);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String left = "#" + rs.getInt("product_id") + " | " + CompetitionUi.emptySafe(rs.getString("name"));
                    String right = "Qte: " + rs.getInt("quantity_sold")
                            + " | CA: " + rs.getBigDecimal("gross_revenue").toPlainString() + " DT";
                    topSellingBox.getChildren().add(CompetitionUi.listRow(left, right));
                }
            }
        } catch (SQLException ex) {
            AlertUtils.error("Statistiques", "Impossible de charger le top ventes.\n" + ex.getMessage());
        }

        if (topSellingBox.getChildren().isEmpty()) {
            topSellingBox.getChildren().add(CompetitionUi.emptyState("Aucune vente sur cette periode."));
        }
    }

    private void loadTopRated() {
        topRatedBox.getChildren().clear();

        String sql = """
                SELECT
                    p.product_id,
                    p.name,
                    AVG(c.rating) AS average_rating,
                    COUNT(c.comment_id) AS ratings_count
                FROM products p
                INNER JOIN comments c ON c.product_id = p.product_id
                WHERE c.is_deleted = 0
                  AND c.rating IS NOT NULL
                GROUP BY p.product_id, p.name
                ORDER BY average_rating DESC, ratings_count DESC, p.name ASC
                LIMIT 6
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String left = "#" + rs.getInt("product_id") + " | " + CompetitionUi.emptySafe(rs.getString("name"));
                String right = "Note: " + rs.getBigDecimal("average_rating").toPlainString() + "/5"
                        + " | Avis: " + rs.getInt("ratings_count");
                topRatedBox.getChildren().add(CompetitionUi.listRow(left, right));
            }
        } catch (SQLException ex) {
            AlertUtils.error("Statistiques", "Impossible de charger le top notes.\n" + ex.getMessage());
        }

        if (topRatedBox.getChildren().isEmpty()) {
            topRatedBox.getChildren().add(CompetitionUi.emptyState("Aucun avis produit disponible."));
        }
    }

    private static int queryInt(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
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

    private record DaySeries(int orders, BigDecimal revenue) {
    }
}
