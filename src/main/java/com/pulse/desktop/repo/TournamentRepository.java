package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.TournamentModel;
import com.pulse.desktop.util.SqlDates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TournamentRepository {

    public List<TournamentModel> findAll() throws SQLException {
        String sql = """
                SELECT
                    t.tournament_id,
                    t.organizer_user_id,
                    t.game_id,
                    t.title,
                    t.start_date,
                    t.end_date,
                    t.max_teams,
                    t.format,
                    t.status,
                    t.prize_pool,
                    u.display_name AS organizer_name,
                    g.name AS game_name
                FROM tournaments t
                JOIN users u ON u.user_id = t.organizer_user_id
                JOIN games g ON g.game_id = t.game_id
                ORDER BY t.tournament_id DESC
                """;

        List<TournamentModel> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                TournamentModel tournament = new TournamentModel();
                tournament.setTournamentId(rs.getInt("tournament_id"));
                tournament.setOrganizerUserId(rs.getInt("organizer_user_id"));
                tournament.setOrganizerName(rs.getString("organizer_name"));
                tournament.setGameId(rs.getInt("game_id"));
                tournament.setGameName(rs.getString("game_name"));
                tournament.setTitle(rs.getString("title"));
                tournament.setStartDate(rs.getDate("start_date").toLocalDate());
                tournament.setEndDate(rs.getDate("end_date").toLocalDate());
                tournament.setMaxTeams(rs.getInt("max_teams"));
                tournament.setFormat(rs.getString("format"));
                tournament.setStatus(rs.getString("status"));
                tournament.setPrizePool(rs.getBigDecimal("prize_pool"));
                rows.add(tournament);
            }
        }
        return rows;
    }

    public void insert(TournamentModel tournament) throws SQLException {
        String sql = """
                INSERT INTO tournaments (
                    organizer_user_id, game_id, title, description, rules,
                    start_date, end_date, registration_deadline, max_teams,
                    format, registration_mode, prize_pool, prize_description,
                    status, photo_path, created_at, updated_at
                )
                VALUES (?, ?, ?, NULL, NULL, ?, ?, NULL, ?, ?, 'OPEN', ?, NULL, ?, NULL, NOW(), NOW())
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tournament.getOrganizerUserId());
            statement.setInt(2, tournament.getGameId());
            statement.setString(3, tournament.getTitle());
            statement.setDate(4, SqlDates.toSqlDate(tournament.getStartDate()));
            statement.setDate(5, SqlDates.toSqlDate(tournament.getEndDate()));
            statement.setInt(6, tournament.getMaxTeams());
            statement.setString(7, tournament.getFormat());
            statement.setBigDecimal(8, tournament.getPrizePool());
            statement.setString(9, tournament.getStatus());
            statement.executeUpdate();
        }
    }

    public void update(TournamentModel tournament) throws SQLException {
        String sql = """
                UPDATE tournaments
                SET organizer_user_id = ?, game_id = ?, title = ?, start_date = ?, end_date = ?,
                    max_teams = ?, format = ?, prize_pool = ?, status = ?, updated_at = NOW()
                WHERE tournament_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tournament.getOrganizerUserId());
            statement.setInt(2, tournament.getGameId());
            statement.setString(3, tournament.getTitle());
            statement.setDate(4, SqlDates.toSqlDate(tournament.getStartDate()));
            statement.setDate(5, SqlDates.toSqlDate(tournament.getEndDate()));
            statement.setInt(6, tournament.getMaxTeams());
            statement.setString(7, tournament.getFormat());
            statement.setBigDecimal(8, tournament.getPrizePool());
            statement.setString(9, tournament.getStatus());
            statement.setInt(10, tournament.getTournamentId());
            statement.executeUpdate();
        }
    }

    public void deleteById(int tournamentId) throws SQLException {
        String sql = "DELETE FROM tournaments WHERE tournament_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tournamentId);
            statement.executeUpdate();
        }
    }
}
