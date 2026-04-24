package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.CategoryModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    public List<CategoryModel> findAll() throws SQLException {
        String sql = """
                SELECT category_id, name, description, created_at, slug
                FROM categories
                ORDER BY created_at DESC, category_id DESC
                """;

        List<CategoryModel> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(mapRow(rs));
            }
        }
        return rows;
    }

    public CategoryModel findById(int categoryId) throws SQLException {
        String sql = """
                SELECT category_id, name, description, created_at, slug
                FROM categories
                WHERE category_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, categoryId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public CategoryModel findBySlug(String slug) throws SQLException {
        String sql = """
                SELECT category_id, name, description, created_at, slug
                FROM categories
                WHERE slug = ?
                LIMIT 1
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug == null ? "" : slug.trim());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapRow(rs);
            }
        }
    }

    public void insert(CategoryModel category) throws SQLException {
        String sql = """
                INSERT INTO categories (name, description, created_at, slug)
                VALUES (?, ?, COALESCE(?, NOW()), ?)
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            
            if (category.getCreatedAt() == null) {
                statement.setNull(3, java.sql.Types.TIMESTAMP);
            } else {
                statement.setTimestamp(3, Timestamp.valueOf(category.getCreatedAt()));
            }
            statement.setString(4, category.getSlug());

            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    category.setCategoryId(keys.getInt(1));
                }
            }
        }
    }

    public void update(CategoryModel category) throws SQLException {
        String sql = """
                UPDATE categories
                SET name = ?, description = ?, slug = ?
                WHERE category_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.setString(3, category.getSlug());
            statement.setInt(4, category.getCategoryId());
            statement.executeUpdate();
        }
    }

    public void deleteById(int categoryId) throws SQLException {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, categoryId);
            statement.executeUpdate();
        }
    }

    private CategoryModel mapRow(ResultSet rs) throws SQLException {
        CategoryModel category = new CategoryModel();
        category.setCategoryId(rs.getInt("category_id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));

        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) {
            category.setCreatedAt(createdAtTs.toLocalDateTime());
        }
        category.setSlug(rs.getString("slug"));

        return category;
    }
}
