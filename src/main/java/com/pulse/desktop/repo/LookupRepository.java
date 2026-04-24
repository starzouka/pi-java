package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LookupRepository {

    public List<LookupItem> users() throws SQLException {
        String sql = """
                SELECT user_id AS id, CONCAT(display_name, ' @', username) AS label
                FROM users
                ORDER BY display_name ASC
                """;
        return load(sql);
    }

    public List<LookupItem> categories() throws SQLException {
        String sql = """
                SELECT category_id AS id, name AS label
                FROM categories
                ORDER BY name ASC
                """;
        return load(sql);
    }

    public List<LookupItem> games() throws SQLException {
        String sql = """
                SELECT game_id AS id, name AS label
                FROM games
                ORDER BY name ASC
                """;
        return load(sql);
    }

    public List<LookupItem> teams() throws SQLException {
        String sql = """
                SELECT team_id AS id, name AS label
                FROM teams
                ORDER BY name ASC
                """;
        return load(sql);
    }

    private List<LookupItem> load(String sql) throws SQLException {
        List<LookupItem> list = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                list.add(new LookupItem(rs.getInt("id"), rs.getString("label")));
            }
        }
        return list;
    }
}
