package org.example.service;

import org.example.connection.MyConnection;
import org.example.model.Product;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ProductService {
    private Connection connection;
    private TeamService teamService;
    private static final Logger LOGGER = Logger.getLogger(ProductService.class.getName());

    public ProductService() {
        this.connection = MyConnection.getInstance().getConnection();
        this.teamService = new TeamService();
    }

    /**
     * Ajouter un nouveau produit avec validation
     */
    public boolean add(Product product) throws SQLException {
        // Validation des données
        if (!isValidProduct(product)) {
            LOGGER.log(Level.WARNING, "Produit invalide: " + product);
            return false;
        }

        // Vérifier que la team existe
        if (!teamService.exists(product.getTeamId())) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout du produit: La team avec l'ID " + product.getTeamId() + " n'existe pas");
            throw new SQLException("La team avec l'ID " + product.getTeamId() + " n'existe pas dans la base de données");
        }

        String query = "INSERT INTO products (team_id, name, description, price, stock_qty, sku, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, product.getTeamId());
            stmt.setString(2, product.getName().trim());
            stmt.setString(3, product.getDescription() != null ? product.getDescription().trim() : null);
            stmt.setDouble(4, product.getPrice());
            stmt.setInt(5, product.getStockQty());
            stmt.setString(6, product.getSku() != null ? product.getSku().trim() : null);
            stmt.setBoolean(7, product.isActive());
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                LOGGER.log(Level.INFO, "Produit créé: " + product.getName());
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout du produit: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Récupérer tous les produits
     */
    public List<Product> getAll() throws SQLException {
        List<Product> products = new ArrayList<>();
        String query = "SELECT * FROM products ORDER BY created_at DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                products.add(mapRowToProduct(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des produits: " + e.getMessage());
            throw e;
        }
        return products;
    }

    /**
     * Récupérer un produit par ID
     */
    public Product getById(int productId) throws SQLException {
        String query = "SELECT * FROM products WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProduct(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération du produit ID " + productId + ": " + e.getMessage());
            throw e;
        }
        return null;
    }

    /**
     * Rechercher des produits par nom ou description
     */
    public List<Product> searchByKeyword(String keyword) throws SQLException {
        List<Product> products = new ArrayList<>();
        String query = "SELECT * FROM products WHERE name LIKE ? OR description LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            String searchTerm = "%" + keyword + "%";
            stmt.setString(1, searchTerm);
            stmt.setString(2, searchTerm);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRowToProduct(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche de produits: " + e.getMessage());
            throw e;
        }
        return products;
    }

    /**
     * Récupérer les produits d'une team
     */
    public List<Product> getByTeamId(int teamId) throws SQLException {
        List<Product> products = new ArrayList<>();
        String query = "SELECT * FROM products WHERE team_id = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRowToProduct(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des produits de la team: " + e.getMessage());
            throw e;
        }
        return products;
    }

    /**
     * Récupérer les produits actifs
     */
    public List<Product> getActive() throws SQLException {
        List<Product> products = new ArrayList<>();
        String query = "SELECT * FROM products WHERE is_active = 1 ORDER BY created_at DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                products.add(mapRowToProduct(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des produits actifs: " + e.getMessage());
            throw e;
        }
        return products;
    }

    /**
     * Mettre à jour un produit avec validation
     */
    public boolean update(Product product) throws SQLException {
        if (product.getProductId() <= 0) {
            LOGGER.log(Level.WARNING, "ID produit invalide: " + product.getProductId());
            return false;
        }

        if (!isValidProduct(product)) {
            LOGGER.log(Level.WARNING, "Produit invalide pour la mise à jour: " + product);
            return false;
        }

        String query = "UPDATE products SET team_id = ?, name = ?, description = ?, price = ?, stock_qty = ?, sku = ?, is_active = ?, updated_at = NOW() WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, product.getTeamId());
            stmt.setString(2, product.getName().trim());
            stmt.setString(3, product.getDescription() != null ? product.getDescription().trim() : null);
            stmt.setDouble(4, product.getPrice());
            stmt.setInt(5, product.getStockQty());
            stmt.setString(6, product.getSku() != null ? product.getSku().trim() : null);
            stmt.setBoolean(7, product.isActive());
            stmt.setInt(8, product.getProductId());
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                LOGGER.log(Level.INFO, "Produit mis à jour: " + product.getName());
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour du produit: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Supprimer un produit par ID
     */
    public boolean delete(int productId) throws SQLException {
        if (productId <= 0) {
            LOGGER.log(Level.WARNING, "ID produit invalide pour suppression: " + productId);
            return false;
        }

        String query = "DELETE FROM products WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, productId);
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                LOGGER.log(Level.INFO, "Produit supprimé, ID: " + productId);
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la suppression du produit: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Vérifier le stock d'un produit
     */
    public int getStock(int productId) throws SQLException {
        String query = "SELECT stock_qty FROM products WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock_qty");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification du stock: " + e.getMessage());
            throw e;
        }
        return 0;
    }

    /**
     * Réduire le stock d'un produit
     */
    public boolean decreaseStock(int productId, int quantity) throws SQLException {
        if (quantity <= 0) {
            LOGGER.log(Level.WARNING, "Quantité invalide pour réduction: " + quantity);
            return false;
        }

        String query = "UPDATE products SET stock_qty = stock_qty - ? WHERE product_id = ? AND stock_qty >= ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la réduction du stock: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Augmenter le stock d'un produit
     */
    public boolean increaseStock(int productId, int quantity) throws SQLException {
        if (quantity <= 0) {
            LOGGER.log(Level.WARNING, "Quantité invalide pour augmentation: " + quantity);
            return false;
        }

        String query = "UPDATE products SET stock_qty = stock_qty + ? WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'augmentation du stock: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Mapper une ligne ResultSet vers un objet Product
     */
    private Product mapRowToProduct(ResultSet rs) throws SQLException {
        Product product = new Product(
                rs.getInt("product_id"),
                rs.getInt("team_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getInt("stock_qty"),
                rs.getString("sku"),
                rs.getBoolean("is_active")
        );
        product.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        product.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return product;
    }

    /**
     * Valider un produit
     */
    private boolean isValidProduct(Product product) {
        if (product == null) return false;
        if (product.getName() == null || product.getName().trim().isEmpty()) return false;
        if (product.getName().length() > 150) return false;
        if (product.getPrice() < 0) return false;
        if (product.getStockQty() < 0) return false;
        if (product.getTeamId() <= 0) return false;
        return true;
    }
}


