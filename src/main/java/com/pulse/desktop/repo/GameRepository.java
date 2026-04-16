package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.GameModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class GameRepository {

    public List<GameModel> findAll() throws SQLException {
        String sql = """
                SELECT
                    g.game_id,
                    g.category_id,
                    g.name,
                    g.slug,
                    g.status,
                    g.popularity_score,
                    g.views_count,
                    g.favorites_count,
                    g.description,
                    g.publisher,
                    g.created_at,
                    g.cover_name,
                    c.name AS category_name
                FROM games g
                JOIN categories c ON c.category_id = g.category_id
                ORDER BY g.game_id DESC
                """;

        List<GameModel> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                GameModel game = new GameModel();
                game.setGameId(rs.getInt("game_id"));
                game.setCategoryId(rs.getInt("category_id"));
                game.setCategoryName(rs.getString("category_name"));
                game.setName(rs.getString("name"));
                game.setSlug(rs.getString("slug"));
                game.setStatus(rs.getString("status"));
                game.setPopularityScore(rs.getInt("popularity_score"));
                game.setViewsCount(rs.getInt("views_count"));
                game.setFavoritesCount(rs.getInt("favorites_count"));
                game.setDescription(rs.getString("description"));
                game.setPublisher(rs.getString("publisher"));
                game.setCoverName(rs.getString("cover_name"));
                
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    game.setCreatedAt(ts.toLocalDateTime());
                }
                rows.add(game);
            }
        }
        return rows;
    }

    public GameModel findById(int gameId) throws SQLException {
        String sql = """
                SELECT
                    g.game_id,
                    g.category_id,
                    g.name,
                    g.slug,
                    g.status,
                    g.popularity_score,
                    g.views_count,
                    g.favorites_count,
                    g.description,
                    g.publisher,
                    g.created_at,
                    g.cover_name,
                    c.name AS category_name
                FROM games g
                JOIN categories c ON c.category_id = g.category_id
                WHERE g.game_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, gameId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                GameModel game = new GameModel();
                game.setGameId(rs.getInt("game_id"));
                game.setCategoryId(rs.getInt("category_id"));
                game.setCategoryName(rs.getString("category_name"));
                game.setName(rs.getString("name"));
                game.setSlug(rs.getString("slug"));
                game.setStatus(rs.getString("status"));
                game.setPopularityScore(rs.getInt("popularity_score"));
                game.setViewsCount(rs.getInt("views_count"));
                game.setFavoritesCount(rs.getInt("favorites_count"));
                game.setDescription(rs.getString("description"));
                game.setPublisher(rs.getString("publisher"));
                game.setCoverName(rs.getString("cover_name"));

                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    game.setCreatedAt(ts.toLocalDateTime());
                }
                return game;
            }
        }
    }

    public void insert(GameModel game) throws SQLException {
        String sql = """
                INSERT INTO games (
                    category_id, name, slug, description, publisher,
                    status, popularity_score, views_count, favorites_count, cover_name, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?, NOW()))
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, game.getCategoryId(), Types.INTEGER);
            statement.setString(2, game.getName());
            statement.setString(3, game.getSlug());
            statement.setString(4, game.getDescription());
            statement.setString(5, game.getPublisher());
            statement.setString(6, game.getStatus());
            statement.setInt(7, game.getPopularityScore());
            statement.setInt(8, game.getViewsCount());
            statement.setInt(9, game.getFavoritesCount());
            statement.setString(10, game.getCoverName());
            if (game.getCreatedAt() == null) {
                statement.setNull(11, java.sql.Types.TIMESTAMP);
            } else {
                statement.setTimestamp(11, java.sql.Timestamp.valueOf(game.getCreatedAt()));
            }
            statement.executeUpdate();
        }
    }

    public void update(GameModel game) throws SQLException {
        String sql = """
                UPDATE games
                SET category_id = ?, name = ?, slug = ?, description = ?, publisher = ?, status = ?,
                    popularity_score = ?, views_count = ?, favorites_count = ?, cover_name = ?
                WHERE game_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, game.getCategoryId(), Types.INTEGER);
            statement.setString(2, game.getName());
            statement.setString(3, game.getSlug());
            statement.setString(4, game.getDescription());
            statement.setString(5, game.getPublisher());
            statement.setString(6, game.getStatus());
            statement.setInt(7, game.getPopularityScore());
            statement.setInt(8, game.getViewsCount());
            statement.setInt(9, game.getFavoritesCount());
            statement.setString(10, game.getCoverName());
            statement.setInt(11, game.getGameId());
            statement.executeUpdate();
        }
    }

    public void deleteById(int gameId) throws SQLException {
        String sql = "DELETE FROM games WHERE game_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, gameId);
            statement.executeUpdate();
        }
    }

    public int countByCategoryId(int categoryId) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM games WHERE category_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, categoryId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return rs.getInt("cnt");
            }
        }
    }

    public int reassignCategory(int fromCategoryId, int toCategoryId) throws SQLException {
        String sql = "UPDATE games SET category_id = ? WHERE category_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, toCategoryId);
            statement.setInt(2, fromCategoryId);
            return statement.executeUpdate();
        }
    }
}
