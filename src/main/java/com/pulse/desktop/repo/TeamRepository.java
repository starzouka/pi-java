package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.TeamModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TeamRepository {

    public List<TeamModel> findAll() throws SQLException {
        String sql = """
                SELECT
                    t.team_id,
                    t.name,
                    t.region,
                    t.captain_user_id,
                    t.created_at,
                    u.display_name AS captain_name
                FROM teams t
                JOIN users u ON u.user_id = t.captain_user_id
                ORDER BY t.team_id DESC
                """;

        List<TeamModel> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                TeamModel team = new TeamModel();
                team.setTeamId(rs.getInt("team_id"));
                team.setName(rs.getString("name"));
                team.setRegion(rs.getString("region"));
                team.setCaptainUserId(rs.getInt("captain_user_id"));
                team.setCaptainName(rs.getString("captain_name"));
                team.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                rows.add(team);
            }
        }
        return rows;
    }

    public void insert(TeamModel team) throws SQLException {
        String sql = """
                INSERT INTO teams (
                    name, description, region, logo_image_id, captain_user_id, created_at, updated_at
                )
                VALUES (?, NULL, ?, NULL, ?, NOW(), NOW())
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, team.getName());
            statement.setString(2, team.getRegion());
            statement.setInt(3, team.getCaptainUserId());
            statement.executeUpdate();
        }
    }

    public void update(TeamModel team) throws SQLException {
        String sql = """
                UPDATE teams
                SET name = ?, region = ?, captain_user_id = ?, updated_at = NOW()
                WHERE team_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, team.getName());
            statement.setString(2, team.getRegion());
            statement.setInt(3, team.getCaptainUserId());
            statement.setInt(4, team.getTeamId());
            statement.executeUpdate();
        }
    }

    public void deleteById(int teamId) throws SQLException {
        String sql = "DELETE FROM teams WHERE team_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            statement.executeUpdate();
        }
    }
}
