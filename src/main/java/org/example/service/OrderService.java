package org.example.service;

import org.example.connection.MyConnection;
import org.example.model.Order;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderService {
    private Connection connection;

    public OrderService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    /**
     * Créer une nouvelle commande
     */
    public int add(Order order) throws SQLException {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String query = "INSERT INTO orders (order_number, cart_id, user_id, status, payment_status, total_amount, shipping_address, phone_for_delivery, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, orderNumber);
            stmt.setInt(2, order.getCartId());
            stmt.setInt(3, order.getUserId());
            stmt.setString(4, order.getStatus() != null ? order.getStatus() : "PENDING");
            stmt.setString(5, order.getPaymentStatus() != null ? order.getPaymentStatus() : "UNPAID");
            stmt.setDouble(6, order.getTotalAmount());
            stmt.setString(7, order.getShippingAddress());
            stmt.setString(8, order.getPhoneForDelivery());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Récupérer toutes les commandes
     */
    public List<Order> getAll() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String query = "SELECT * FROM orders ORDER BY created_at DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                orders.add(mapRowToOrder(rs));
            }
        }
        return orders;
    }

    /**
     * Récupérer une commande par ID
     */
    public Order getById(int orderId) throws SQLException {
        String query = "SELECT * FROM orders WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToOrder(rs);
                }
            }
        }
        return null;
    }

    /**
     * Récupérer une commande par numéro
     */
    public Order getByOrderNumber(String orderNumber) throws SQLException {
        String query = "SELECT * FROM orders WHERE order_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, orderNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToOrder(rs);
                }
            }
        }
        return null;
    }

    /**
     * Récupérer les commandes d'un utilisateur
     */
    public List<Order> getByUserId(int userId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String query = "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRowToOrder(rs));
                }
            }
        }
        return orders;
    }

    /**
     * Récupérer les commandes d'un panier
     */
    public Order getByCartId(int cartId) throws SQLException {
        String query = "SELECT * FROM orders WHERE cart_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, cartId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToOrder(rs);
                }
            }
        }
        return null;
    }

    /**
     * Mettre à jour une commande
     */
    public boolean update(Order order) throws SQLException {
        String query = "UPDATE orders SET status = ?, payment_status = ?, payment_method = ?, shipping_address = ?, phone_for_delivery = ? WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, order.getStatus());
            stmt.setString(2, order.getPaymentStatus());
            stmt.setString(3, order.getPaymentMethod());
            stmt.setString(4, order.getShippingAddress());
            stmt.setString(5, order.getPhoneForDelivery());
            stmt.setInt(6, order.getOrderId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Mettre à jour le statut d'une commande
     */
    public boolean updateStatus(int orderId, String status) throws SQLException {
        String query = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Mettre à jour le statut de paiement
     */
    public boolean updatePaymentStatus(int orderId, String paymentStatus) throws SQLException {
        String query = "UPDATE orders SET payment_status = ?, paid_at = NOW() WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, paymentStatus);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Supprimer une commande
     */
    public boolean delete(int orderId) throws SQLException {
        String query = "DELETE FROM orders WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, orderId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Compter les commandes par statut
     */
    public int countByStatus(String status) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM orders WHERE status = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    /**
     * Obtenir les statistiques des commandes par statut
     */
    public List<Object[]> getStatsByStatus() throws SQLException {
        List<Object[]> stats = new ArrayList<>();
        String query = "SELECT status, COUNT(*) as count, SUM(total_amount) as total FROM orders GROUP BY status";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                stats.add(new Object[]{
                        rs.getString("status"),
                        rs.getInt("count"),
                        rs.getDouble("total")
                });
            }
        }
        return stats;
    }

    /**
     * Obtenir les ventes totales
     */
    public double getTotalRevenue() throws SQLException {
        String query = "SELECT SUM(total_amount) as total FROM orders WHERE payment_status = 'PAID'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                double total = rs.getDouble("total");
                return rs.wasNull() ? 0.0 : total;
            }
        }
        return 0.0;
    }

    /**
     * Mapper une ligne ResultSet vers un objet Order
     */
    private Order mapRowToOrder(ResultSet rs) throws SQLException {
        Order order = new Order(
                rs.getString("order_number"),
                rs.getInt("cart_id"),
                rs.getInt("user_id"),
                rs.getDouble("total_amount")
        );
        order.setOrderId(rs.getInt("order_id"));
        order.setStatus(rs.getString("status"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setPaymentStatus(rs.getString("payment_status"));
        order.setShippingAddress(rs.getString("shipping_address"));
        order.setPhoneForDelivery(rs.getString("phone_for_delivery"));
        order.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        order.setPaidAt(rs.getObject("paid_at", LocalDateTime.class));
        order.setShippedAt(rs.getObject("shipped_at", LocalDateTime.class));
        order.setDeliveredAt(rs.getObject("delivered_at", LocalDateTime.class));
        return order;
    }
}

