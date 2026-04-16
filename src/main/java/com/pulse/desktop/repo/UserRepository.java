package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.UserModel;
import com.pulse.desktop.util.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    public List<UserModel> findAll() throws SQLException {
        String sql = """
                SELECT user_id, username, email, display_name, role, is_active, created_at
                FROM users
                ORDER BY user_id DESC
                """;

        List<UserModel> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                UserModel user = new UserModel();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setDisplayName(rs.getString("display_name"));
                user.setRole(rs.getString("role"));
                user.setActive(rs.getBoolean("is_active"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                rows.add(user);
            }
        }
        return rows;
    }

    public void insert(UserModel user, String plainPassword) throws SQLException {
        String sql = """
                INSERT INTO users (
                    username, email, password_hash, role, display_name,
                    email_verified, is_active, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, 1, ?, NOW(), NOW())
                """;

        String hash = PasswordHasher.hash(plainPassword);
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, hash);
            statement.setString(4, user.getRole());
            statement.setString(5, user.getDisplayName());
            statement.setBoolean(6, user.isActive());
            statement.executeUpdate();
        }
    }

    public void update(UserModel user) throws SQLException {
        String sql = """
                UPDATE users
                SET username = ?, email = ?, display_name = ?, role = ?, is_active = ?, updated_at = NOW()
                WHERE user_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getDisplayName());
            statement.setString(4, user.getRole());
            statement.setBoolean(5, user.isActive());
            statement.setInt(6, user.getUserId());
            statement.executeUpdate();
        }
    }

    public void deleteById(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }
}
