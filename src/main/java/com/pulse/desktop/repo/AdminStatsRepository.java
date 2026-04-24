package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.AdminStatsSnapshot;
import com.pulse.desktop.model.DailyActionCounts;
import com.pulse.desktop.model.DailyCount;
import com.pulse.desktop.model.NamedCount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminStatsRepository {
    private final ActivityLogRepository activityRepo = new ActivityLogRepository();

    public AdminStatsSnapshot loadSnapshot() throws SQLException {
        activityRepo.ensureSchema();

        long totalGames = scalarLong("SELECT COUNT(*) FROM games");
        long totalCategories = scalarLong("SELECT COUNT(*) FROM categories");
        long totalViews = scalarLong("""
                SELECT COUNT(*)
                FROM pulse_activity_log
                WHERE action = 'VIEW'
                  AND entity_type = 'GAME'
                """);
        long viewsToday = scalarLong("""
                SELECT COUNT(*)
                FROM pulse_activity_log
                WHERE action = 'VIEW'
                  AND entity_type = 'GAME'
                  AND DATE(created_at) = CURDATE()
                """);

        List<NamedCount> perCategory = loadGamesPerCategory();
        List<NamedCount> topViewed = loadTopViewedGames();
        List<DailyActionCounts> activity = loadActivityLastDays(14);

        return new AdminStatsSnapshot(
                totalGames,
                totalCategories,
                totalViews,
                viewsToday,
                perCategory,
                topViewed,
                activity
        );
    }

    private long scalarLong(String sql) throws SQLException {
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        }
    }

    private List<NamedCount> loadTopViewedGames() throws SQLException {
        String sql = """
                SELECT g.name AS name, COUNT(l.id) AS cnt
                FROM games g
                LEFT JOIN pulse_activity_log l
                  ON l.entity_type = 'GAME'
                 AND l.action = 'VIEW'
                 AND l.entity_id = g.game_id
                GROUP BY g.game_id, g.name
                ORDER BY cnt DESC, g.game_id DESC
                LIMIT 8
                """;
        List<NamedCount> out = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                out.add(new NamedCount(rs.getString("name"), rs.getLong("cnt")));
            }
        }
        return out;
    }

    public List<DailyCount> loadGameViewsLastDays(int gameId, int days) throws SQLException {
        activityRepo.ensureSchema();
        int effectiveDays = Math.max(1, days);
        String sql = """
                SELECT DATE(created_at) AS day, COUNT(*) AS cnt
                FROM pulse_activity_log
                WHERE action = 'VIEW'
                  AND entity_type = 'GAME'
                  AND entity_id = ?
                  AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
                GROUP BY DATE(created_at)
                ORDER BY day ASC
                """;

        Map<LocalDate, Long> aggregated = new HashMap<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, gameId);
            statement.setInt(2, effectiveDays - 1);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date d = rs.getDate("day");
                    if (d == null) {
                        continue;
                    }
                    aggregated.put(d.toLocalDate(), rs.getLong("cnt"));
                }
            }
        }

        List<DailyCount> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(effectiveDays - 1L);
        for (int i = 0; i < effectiveDays; i++) {
            LocalDate day = start.plusDays(i);
            out.add(new DailyCount(day, aggregated.getOrDefault(day, 0L)));
        }
        return out;
    }

    private List<NamedCount> loadGamesPerCategory() throws SQLException {
        String sql = """
                SELECT c.name AS name, COUNT(g.game_id) AS cnt
                FROM categories c
                LEFT JOIN games g ON g.category_id = c.category_id
                GROUP BY c.category_id, c.name
                ORDER BY cnt DESC, c.name ASC
                """;
        List<NamedCount> out = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                out.add(new NamedCount(rs.getString("name"), rs.getLong("cnt")));
            }
        }
        return out;
    }

    private List<DailyActionCounts> loadActivityLastDays(int days) throws SQLException {
        int effectiveDays = Math.max(1, days);
        String sql = """
                SELECT DATE(created_at) AS day, action, COUNT(*) AS cnt
                FROM pulse_activity_log
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
                  AND entity_type IN ('GAME', 'CATEGORY')
                  AND action IN ('VIEW', 'CREATE', 'UPDATE', 'DELETE')
                GROUP BY DATE(created_at), action
                ORDER BY day ASC
                """;

        Map<LocalDate, Map<String, Long>> aggregated = new HashMap<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, effectiveDays - 1);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date d = rs.getDate("day");
                    if (d == null) {
                        continue;
                    }
                    LocalDate day = d.toLocalDate();
                    String action = rs.getString("action");
                    long cnt = rs.getLong("cnt");
                    aggregated.computeIfAbsent(day, ignored -> new HashMap<>())
                            .put(action == null ? "" : action.trim().toUpperCase(), cnt);
                }
            }
        }

        List<DailyActionCounts> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(effectiveDays - 1L);
        for (int i = 0; i < effectiveDays; i++) {
            LocalDate day = start.plusDays(i);
            Map<String, Long> map = aggregated.getOrDefault(day, Map.of());
            out.add(new DailyActionCounts(
                    day,
                    map.getOrDefault("VIEW", 0L),
                    map.getOrDefault("CREATE", 0L),
                    map.getOrDefault("UPDATE", 0L),
                    map.getOrDefault("DELETE", 0L)
            ));
        }
        return out;
    }
}
