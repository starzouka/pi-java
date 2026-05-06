package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.ProductModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    public List<ProductModel> findAll() throws SQLException {
        String sql = """
                SELECT
                    p.product_id,
                    p.team_id,
                    p.name,
                    p.price,
                    p.stock_qty,
                    p.is_active,
                    t.name AS team_name
                FROM products p
                JOIN teams t ON t.team_id = p.team_id
                ORDER BY p.product_id DESC
                """;

        List<ProductModel> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                ProductModel product = new ProductModel();
                product.setProductId(rs.getInt("product_id"));
                product.setTeamId(rs.getInt("team_id"));
                product.setTeamName(rs.getString("team_name"));
                product.setName(rs.getString("name"));
                product.setPrice(rs.getBigDecimal("price"));
                product.setStockQty(rs.getInt("stock_qty"));
                product.setActive(rs.getBoolean("is_active"));
                rows.add(product);
            }
        }
        return rows;
    }

    public void insert(ProductModel product) throws SQLException {
        String sql = """
                INSERT INTO products (
                    team_id, name, description, price, stock_qty, sku, is_active, created_at, updated_at
                )
                VALUES (?, ?, NULL, ?, ?, NULL, ?, NOW(), NOW())
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, product.getTeamId());
            statement.setString(2, product.getName());
            statement.setBigDecimal(3, product.getPrice());
            statement.setInt(4, product.getStockQty());
            statement.setBoolean(5, product.isActive());
            statement.executeUpdate();
        }
    }

    public void update(ProductModel product) throws SQLException {
        String sql = """
                UPDATE products
                SET team_id = ?, name = ?, price = ?, stock_qty = ?, is_active = ?, updated_at = NOW()
                WHERE product_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, product.getTeamId());
            statement.setString(2, product.getName());
            statement.setBigDecimal(3, product.getPrice());
            statement.setInt(4, product.getStockQty());
            statement.setBoolean(5, product.isActive());
            statement.setInt(6, product.getProductId());
            statement.executeUpdate();
        }
    }

    public void deleteById(int productId) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            statement.executeUpdate();
        }
    }
}
