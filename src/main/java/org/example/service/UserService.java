package org.example.service;

import org.example.connection.MyConnection;
import org.example.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private Connection connection;

    public UserService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    /**
     * Ajouter un nouvel utilisateur
     */
    public boolean add(User user) throws SQLException {
        String query = "INSERT INTO users (username, email, password_hash, role, display_name, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getRole() != null ? user.getRole() : "PLAYER");
            stmt.setString(5, user.getDisplayName());
            stmt.setBoolean(6, user.isActive());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Récupérer un utilisateur par ID
     */
    public User getById(int userId) throws SQLException {
        String query = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Récupérer un utilisateur par nom d'utilisateur
     */
    public User getByUsername(String username) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Récupérer un utilisateur par email
     */
    public User getByEmail(String email) throws SQLException {
        String query = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Récupérer tous les utilisateurs
     */
    public List<User> getAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    /**
     * Vérifier les identifiants (login)
     */
    public User authenticate(String username, String passwordHash) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ? AND password_hash = ? AND is_active = 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Mettre à jour un utilisateur
     */
    public boolean update(User user) throws SQLException {
        String query = "UPDATE users SET username = ?, email = ?, role = ?, display_name = ?, is_active = ?, updated_at = NOW() WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getDisplayName());
            stmt.setBoolean(5, user.isActive());
            stmt.setInt(6, user.getUserId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Supprimer un utilisateur
     */
    public boolean delete(int userId) throws SQLException {
        String query = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Mapper une ligne ResultSet vers un objet User
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("display_name")
        );
        user.setUserId(rs.getInt("user_id"));
        user.setRole(rs.getString("role"));
        user.setPhone(rs.getString("phone"));
        user.setCountry(rs.getString("country"));
        user.setEmailVerified(rs.getBoolean("email_verified"));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        user.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return user;
    }
}

