package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.GameModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                rows.add(game);
            }
        }
        return rows;
    }

    public void insert(GameModel game) throws SQLException {
        String sql = """
                INSERT INTO games (
                    category_id, name, slug, description, publisher,
                    status, popularity_score, views_count, favorites_count, created_at
                )
                VALUES (?, ?, ?, NULL, NULL, ?, ?, ?, ?, NOW())
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, game.getCategoryId());
            statement.setString(2, game.getName());
            statement.setString(3, game.getSlug());
            statement.setString(4, game.getStatus());
            statement.setInt(5, game.getPopularityScore());
            statement.setInt(6, game.getViewsCount());
            statement.setInt(7, game.getFavoritesCount());
            statement.executeUpdate();
        }
    }

    public void update(GameModel game) throws SQLException {
        String sql = """
                UPDATE games
                SET category_id = ?, name = ?, slug = ?, status = ?,
                    popularity_score = ?, views_count = ?, favorites_count = ?
                WHERE game_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, game.getCategoryId());
            statement.setString(2, game.getName());
            statement.setString(3, game.getSlug());
            statement.setString(4, game.getStatus());
            statement.setInt(5, game.getPopularityScore());
            statement.setInt(6, game.getViewsCount());
            statement.setInt(7, game.getFavoritesCount());
            statement.setInt(8, game.getGameId());
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
}
