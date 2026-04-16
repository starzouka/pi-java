package org.example.service;

import org.example.connection.MyConnection;
import org.example.model.Team;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TeamService {
    private Connection connection;

    public TeamService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    /**
     * Récupérer toutes les équipes
     */
    public List<Team> getAll() throws SQLException {
        List<Team> teams = new ArrayList<>();
        String query = "SELECT * FROM teams ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                teams.add(mapRowToTeam(rs));
            }
        }
        return teams;
    }

    /**
     * Récupérer une équipe par ID
     */
    public Team getById(int teamId) throws SQLException {
        String query = "SELECT * FROM teams WHERE team_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTeam(rs);
                }
            }
        }
        return null;
    }

    /**
     * Vérifier si une équipe existe par ID
     */
    public boolean exists(int teamId) throws SQLException {
        String query = "SELECT 1 FROM teams WHERE team_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Mapper une ligne ResultSet vers un objet Team
     */
    private Team mapRowToTeam(ResultSet rs) throws SQLException {
        Team team = new Team();
        team.setTeamId(rs.getInt("team_id"));
        team.setName(rs.getString("name"));
        team.setDescription(rs.getString("description"));
        team.setRegion(rs.getString("region"));
        team.setCaptainUserId(rs.getInt("captain_user_id"));
        team.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        team.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));

        Object logoImageId = rs.getObject("logo_image_id");
        if (logoImageId != null) {
            team.setLogoImageId((Integer) logoImageId);
        }

        return team;
    }
}

