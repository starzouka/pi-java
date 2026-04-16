package org.example.service;

import org.example.connection.MyConnection;
import org.example.model.Cart;
import org.example.model.CartItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CartService {
    private Connection connection;

    public CartService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    /**
     * Créer ou récupérer le panier d'un utilisateur
     */
    public Cart getCartByUserId(int userId) throws SQLException {
        // D'abord chercher un panier OPEN
        String queryOpen = "SELECT * FROM carts WHERE user_id = ? AND status = 'OPEN' ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(queryOpen)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Cart cart = mapRowToCart(rs);
                    cart.setItems(getCartItems(cart.getCartId()));
                    return cart;
                }
            }
        }

        // Si pas de panier OPEN, chercher un panier LOCKED ou CLOSED et le réouvrir
        String queryOther = "SELECT * FROM carts WHERE user_id = ? AND (status = 'LOCKED' OR status = 'CLOSED') ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(queryOther)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Cart cart = mapRowToCart(rs);
                    // Réouvrir le panier
                    updateCartStatus(cart.getCartId(), "OPEN");
                    cart.setStatus("OPEN");
                    cart.setItems(getCartItems(cart.getCartId()));
                    return cart;
                }
            }
        }

        // Créer un nouveau panier s'il n'existe pas
        return createCart(userId);
    }

    /**
     * Récupérer un panier par ID
     */
    public Cart getCartById(int cartId) throws SQLException {
        String query = "SELECT * FROM carts WHERE cart_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, cartId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Cart cart = mapRowToCart(rs);
                    cart.setItems(getCartItems(cart.getCartId()));
                    return cart;
                }
            }
        }
        return null;
    }

    /**
     * Créer un nouveau panier
     */
    public Cart createCart(int userId) throws SQLException {
        // Créer un nouveau panier seulement s'il n'existe pas d'OPEN
        String checkQuery = "SELECT * FROM carts WHERE user_id = ? AND status = 'OPEN'";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setInt(1, userId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Un panier OPEN existe déjà
                    return mapRowToCart(rs);
                }
            }
        }

        // Créer un nouveau panier
        String query = "INSERT INTO carts (user_id, status, created_at, updated_at) VALUES (?, 'OPEN', NOW(), NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int cartId = rs.getInt(1);
                    Cart newCart = new Cart(cartId, userId, "OPEN");
                    newCart.setItems(new ArrayList<>());
                    return newCart;
                }
            }
        }
        return null;
    }

    /**
     * Ajouter un article au panier (avec décrémentation du stock)
     */
    public boolean addToCart(int cartId, int productId, int quantity, double unitPrice) throws SQLException {
        try {
            // Démarrer une transaction
            connection.setAutoCommit(false);

            // Vérifier d'abord que le panier est en status OPEN
            String statusCheckQuery = "SELECT status FROM carts WHERE cart_id = ?";
            String cartStatus = null;
            try (PreparedStatement statusStmt = connection.prepareStatement(statusCheckQuery)) {
                statusStmt.setInt(1, cartId);
                try (ResultSet rs = statusStmt.executeQuery()) {
                    if (rs.next()) {
                        cartStatus = rs.getString("status");
                    }
                }
            }

            // Si le panier n'est pas OPEN, le réouvrir
            if (cartStatus != null && !"OPEN".equals(cartStatus)) {
                String updateStatusQuery = "UPDATE carts SET status = 'OPEN' WHERE cart_id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateStatusQuery)) {
                    updateStmt.setInt(1, cartId);
                    updateStmt.executeUpdate();
                }
            }

            // Vérifier si l'article existe déjà dans le panier
            String checkQuery = "SELECT quantity FROM cart_items WHERE cart_id = ? AND product_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setInt(1, cartId);
                checkStmt.setInt(2, productId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // Article existe déjà, augmenter la quantité
                        int existingQuantity = rs.getInt("quantity");
                        String updateQuery = "UPDATE cart_items SET quantity = ?, updated_at = NOW() WHERE cart_id = ? AND product_id = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setInt(1, existingQuantity + quantity);
                            updateStmt.setInt(2, cartId);
                            updateStmt.setInt(3, productId);
                            updateStmt.executeUpdate();

                            // Décrémenter le stock du produit
                            decrementProductStock(productId, quantity);

                            connection.commit();
                            return true;
                        }
                    }
                }
            }

            // Article n'existe pas, l'ajouter
            String insertQuery = "INSERT INTO cart_items (cart_id, product_id, quantity, unit_price_at_add, added_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())";
            try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
                stmt.setInt(1, cartId);
                stmt.setInt(2, productId);
                stmt.setInt(3, quantity);
                stmt.setDouble(4, unitPrice);
                stmt.executeUpdate();

                // Décrémenter le stock du produit
                decrementProductStock(productId, quantity);

                connection.commit();
                return true;
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                // Ignore rollback errors
            }
            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    /**
     * Décrémenter le stock d'un produit
     */
    private void decrementProductStock(int productId, int quantity) throws SQLException {
        String updateStockQuery = "UPDATE products SET stock_qty = stock_qty - ?, updated_at = NOW() WHERE product_id = ? AND stock_qty >= ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateStockQuery)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.executeUpdate();
        }
    }

    /**
     * Réincrémenter le stock d'un produit
     */
    private void incrementProductStock(int productId, int quantity) throws SQLException {
        String updateStockQuery = "UPDATE products SET stock_qty = stock_qty + ?, updated_at = NOW() WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateStockQuery)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        }
    }

    /**
     * Récupérer tous les articles du panier
     */
    public List<CartItem> getCartItems(int cartId) throws SQLException {
        List<CartItem> items = new ArrayList<>();
        String query = "SELECT ci.*, p.name AS product_name FROM cart_items ci " +
                "LEFT JOIN products p ON ci.product_id = p.product_id WHERE ci.cart_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, cartId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CartItem item = new CartItem(
                            rs.getInt("cart_id"),
                            rs.getInt("product_id"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price_at_add"),
                            rs.getString("product_name")
                    );
                    item.setAddedAt(rs.getObject("added_at", LocalDateTime.class));
                    item.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
                    items.add(item);
                }
            }
        }
        return items;
    }

    /**
     * Supprimer un article du panier (avec réincrémentation du stock)
     */
    public boolean removeFromCart(int cartId, int productId) throws SQLException {
        // D'abord, récupérer la quantité pour réincrémenter le stock
        String getQuantityQuery = "SELECT quantity FROM cart_items WHERE cart_id = ? AND product_id = ?";
        int quantityToRemove = 0;
        try (PreparedStatement getStmt = connection.prepareStatement(getQuantityQuery)) {
            getStmt.setInt(1, cartId);
            getStmt.setInt(2, productId);
            try (ResultSet rs = getStmt.executeQuery()) {
                if (rs.next()) {
                    quantityToRemove = rs.getInt("quantity");
                }
            }
        }

        // Supprimer l'article du panier
        String deleteQuery = "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?";
        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery)) {
            deleteStmt.setInt(1, cartId);
            deleteStmt.setInt(2, productId);
            boolean success = deleteStmt.executeUpdate() > 0;
            if (success && quantityToRemove > 0) {
                // Réincrémenter le stock du produit
                incrementProductStock(productId, quantityToRemove);
            }
            return success;
        }
    }

    /**
     * Mettre à jour la quantité d'un article (avec gestion du stock)
     */
    public boolean updateCartItemQuantity(int cartId, int productId, int newQuantity) throws SQLException {
        if (newQuantity <= 0) {
            return removeFromCart(cartId, productId);
        }

        // Récupérer l'ancienne quantité
        String getQuantityQuery = "SELECT quantity FROM cart_items WHERE cart_id = ? AND product_id = ?";
        int oldQuantity = 0;
        try (PreparedStatement getStmt = connection.prepareStatement(getQuantityQuery)) {
            getStmt.setInt(1, cartId);
            getStmt.setInt(2, productId);
            try (ResultSet rs = getStmt.executeQuery()) {
                if (rs.next()) {
                    oldQuantity = rs.getInt("quantity");
                }
            }
        }

        // Mettre à jour la quantité
        String updateQuery = "UPDATE cart_items SET quantity = ?, updated_at = NOW() WHERE cart_id = ? AND product_id = ?";
        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
            updateStmt.setInt(1, newQuantity);
            updateStmt.setInt(2, cartId);
            updateStmt.setInt(3, productId);
            boolean success = updateStmt.executeUpdate() > 0;
            if (success) {
                // Ajuster le stock en fonction de la différence
                int quantityDifference = oldQuantity - newQuantity;
                if (quantityDifference > 0) {
                    // Augmentation de quantité = décrémentation de stock supplémentaire
                    decrementProductStock(productId, quantityDifference);
                } else if (quantityDifference < 0) {
                    // Diminution de quantité = réincrémentation de stock
                    incrementProductStock(productId, -quantityDifference);
                }
            }
            return success;
        }
    }

    /**
     * Vider le panier
     */
    public boolean clearCart(int cartId) throws SQLException {
        String query = "DELETE FROM cart_items WHERE cart_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, cartId);
            return stmt.executeUpdate() >= 0;
        }
    }

    /**
     * Obtenir le total du panier
     */
    public double getCartTotal(int cartId) throws SQLException {
        String query = "SELECT SUM(quantity * unit_price_at_add) as total FROM cart_items WHERE cart_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, cartId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double total = rs.getDouble("total");
                    return rs.wasNull() ? 0.0 : total;
                }
            }
        }
        return 0.0;
    }

    /**
     * Obtenir le nombre d'articles dans le panier
     */
    public int getCartItemCount(int cartId) throws SQLException {
        String query = "SELECT SUM(quantity) as count FROM cart_items WHERE cart_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, cartId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    return rs.wasNull() ? 0 : count;
                }
            }
        }
        return 0;
    }

    /**
     * Changer le statut du panier
     */
    public boolean updateCartStatus(int cartId, String status) throws SQLException {
        String query = "UPDATE carts SET status = ?, updated_at = NOW() WHERE cart_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, status);
            stmt.setInt(2, cartId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Verrouiller le panier
     */
    public boolean lockCart(int cartId) throws SQLException {
        String query = "UPDATE carts SET status = 'LOCKED', locked_at = NOW(), updated_at = NOW() WHERE cart_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, cartId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Finaliser le panier après une commande réussie
     * - Marque le panier actuel comme COMPLETED
     * - Simplifié: pas de création automatique d'un nouveau panier
     * - Le prochain appel à getCartByUserId() créera un nouveau panier OPEN si nécessaire
     */
    public boolean finalizeCartAfterOrder(int completedCartId, int userId) throws SQLException {
        try {
            connection.setAutoCommit(false);

            // Marquer le panier comme COMPLETED
            String completeQuery = "UPDATE carts SET status = 'COMPLETED', updated_at = NOW() WHERE cart_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(completeQuery)) {
                stmt.setInt(1, completedCartId);
                stmt.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                // Ignore rollback errors
            }
            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    /**
     * Mapper une ligne ResultSet vers un objet Cart
     */
    private Cart mapRowToCart(ResultSet rs) throws SQLException {
        Cart cart = new Cart(
                rs.getInt("cart_id"),
                rs.getInt("user_id"),
                rs.getString("status")
        );
        cart.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        cart.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        cart.setLockedAt(rs.getObject("locked_at", LocalDateTime.class));
        return cart;
    }
}

