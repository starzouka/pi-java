
package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CompetitionRepository {
    private static final Set<String> TOURNAMENT_STATUSES = Set.of("DRAFT", "OPEN", "ONGOING", "FINISHED", "CANCELLED");
    private static final Set<String> TOURNAMENT_FORMATS = Set.of("BO1", "BO3", "BO5");
    private static final Set<String> REGISTRATION_MODES = Set.of("OPEN", "APPROVAL");
    private static final Set<String> MATCH_STATUSES = Set.of("SCHEDULED", "ONGOING", "FINISHED", "CANCELLED");
    private static final Set<String> REQUEST_STATUSES = Set.of("PENDING", "ACCEPTED", "REFUSED", "CANCELLED");

    public record TournamentSearchFilter(
            String q,
            Integer gameId,
            Integer categoryId,
            String status,
            String format,
            String registrationMode,
            LocalDate dateFrom,
            LocalDate dateTo,
            BigDecimal prizeMin,
            BigDecimal prizeMax,
            String sort
    ) {
        public static TournamentSearchFilter defaults() {
            return new TournamentSearchFilter("", null, null, "", "", "", null, null, null, null, "latest");
        }
    }

    public record MatchSearchFilter(
            String q,
            Integer tournamentId,
            String status,
            Integer gameId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String team,
            String sort
    ) {
        public static MatchSearchFilter defaults(String sort) {
            return new MatchSearchFilter("", null, "", null, null, null, "", sort);
        }
    }

    public record RequestSearchFilter(String q, String status, Integer gameId, String sort) {
        public static RequestSearchFilter defaults() {
            return new RequestSearchFilter("", "", null, "latest");
        }
    }

    public record TournamentCatalogRow(
            int tournamentId,
            String title,
            String status,
            String format,
            String registrationMode,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate registrationDeadline,
            int maxTeams,
            BigDecimal prizePool,
            String prizeDescription,
            String description,
            String rules,
            String photoPath,
            int organizerUserId,
            String organizerName,
            int gameId,
            String gameName,
            int categoryId,
            String categoryName,
            int registeredCount,
            int acceptedCount,
            int matchesTotal,
            int matchesFinished
    ) {
    }

    public record TeamRegistrationOption(int teamId, String teamName, String status) {
    }

    public record MatchTeamRow(int teamId, String teamName, Integer score, Boolean winner) {
    }

    public record MatchRow(
            int matchId,
            int tournamentId,
            String tournamentTitle,
            String gameName,
            String roundName,
            Integer bestOf,
            String status,
            LocalDateTime scheduledAt,
            Integer resultSubmittedByUserId,
            String resultSubmittedByName,
            String tournamentPhotoPath,
            List<MatchTeamRow> teams
    ) {
        public String teamsLabel() {
            if (teams == null || teams.isEmpty()) {
                return "Aucune equipe";
            }
            List<String> names = new ArrayList<>();
            for (MatchTeamRow team : teams) {
                if (team == null || team.teamName() == null || team.teamName().isBlank()) {
                    continue;
                }
                names.add(team.teamName());
            }
            if (names.isEmpty()) {
                return "Aucune equipe";
            }
            return String.join(" vs ", names);
        }
    }

    public record ParticipantRow(
            int teamId,
            String teamName,
            String region,
            String logoPath,
            String status,
            Integer seed,
            boolean checkedIn,
            LocalDateTime registeredAt
    ) {
    }

    public record ScoreboardRow(int teamId, String teamName, int played, int wins, int losses, int points) {
    }

    public record RequestRow(
            int requestId,
            int organizerUserId,
            String organizerName,
            String organizerEmail,
            int gameId,
            String gameName,
            String title,
            String description,
            String rules,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate registrationDeadline,
            int maxTeams,
            String format,
            String registrationMode,
            BigDecimal prizePool,
            String prizeDescription,
            String status,
            String photoPath,
            String adminResponseNote,
            LocalDateTime createdAt,
            LocalDateTime reviewedAt,
            Integer reviewedByAdminId,
            String reviewedByAdminName
    ) {
    }

    public record RegistrationRow(
            int tournamentId,
            int teamId,
            String teamName,
            String status,
            Integer seed,
            boolean checkedIn,
            LocalDateTime registeredAt
    ) {
    }

    public record MatchParticipantInput(int teamId, Integer score, boolean winner) {
    }

    public record MatchMutationInput(
            Integer matchId,
            int tournamentId,
            String roundName,
            LocalDateTime scheduledAt,
            Integer bestOf,
            String status,
            List<MatchParticipantInput> participants
    ) {
    }

    public record TournamentMutationInput(
            Integer tournamentId,
            int organizerUserId,
            int gameId,
            String title,
            String description,
            String rules,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate registrationDeadline,
            int maxTeams,
            String format,
            String registrationMode,
            BigDecimal prizePool,
            String prizeDescription,
            String status,
            String photoPath
    ) {
    }

    public record RequestMutationInput(
            int organizerUserId,
            int gameId,
            String title,
            String description,
            String rules,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate registrationDeadline,
            int maxTeams,
            String format,
            String registrationMode,
            BigDecimal prizePool,
            String prizeDescription,
            String photoPath
    ) {
    }

    public record OperationResult(boolean ok, String message) {
        public static OperationResult ok(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult error(String message) {
            return new OperationResult(false, message);
        }
    }

    public record ReviewResult(boolean ok, String message, String organizerEmail, String requestTitle, String decision) {
    }

    public List<LookupItem> listGames() throws SQLException {
        String sql = """
                SELECT g.game_id AS id, CONCAT(g.name, ' - ', COALESCE(c.name, 'Categorie')) AS label
                FROM games g
                LEFT JOIN categories c ON c.category_id = g.category_id
                ORDER BY g.name ASC
                """;
        return loadLookups(sql);
    }

    public List<LookupItem> listCategories() throws SQLException {
        String sql = """
                SELECT category_id AS id, name AS label
                FROM categories
                ORDER BY name ASC
                """;
        return loadLookups(sql);
    }

    public List<LookupItem> listAllTournaments() throws SQLException {
        String sql = """
                SELECT t.tournament_id AS id, CONCAT('#', t.tournament_id, ' - ', t.title) AS label
                FROM tournaments t
                ORDER BY t.start_date DESC, t.created_at DESC
                LIMIT 500
                """;
        return loadLookups(sql);
    }

    public List<LookupItem> listOrganizerTournaments(int organizerUserId) throws SQLException {
        String sql = """
                SELECT t.tournament_id AS id, CONCAT('#', t.tournament_id, ' - ', t.title) AS label
                FROM tournaments t
                WHERE t.organizer_user_id = ?
                ORDER BY t.start_date DESC, t.created_at DESC
                """;
        List<LookupItem> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, organizerUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new LookupItem(rs.getInt("id"), rs.getString("label")));
                }
            }
        }
        return rows;
    }

    public List<TeamRegistrationOption> listCaptainTeamsForTournament(int userId, int tournamentId) throws SQLException {
        String sql = """
                SELECT t.team_id,
                       t.name,
                       tt.status AS participation_status
                FROM teams t
                LEFT JOIN tournament_teams tt
                  ON tt.team_id = t.team_id
                 AND tt.tournament_id = ?
                WHERE t.captain_user_id = ?
                ORDER BY t.name ASC
                """;

        List<TeamRegistrationOption> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tournamentId);
            statement.setInt(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TeamRegistrationOption(
                            rs.getInt("team_id"),
                            rs.getString("name"),
                            rs.getString("participation_status")
                    ));
                }
            }
        }
        return rows;
    }
    public List<TournamentCatalogRow> searchTournamentCatalog(TournamentSearchFilter rawFilter, int limit) throws SQLException {
        TournamentSearchFilter filter = normalizeTournamentFilter(rawFilter);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    t.tournament_id,
                    t.title,
                    t.status,
                    t.format,
                    t.registration_mode,
                    t.start_date,
                    t.end_date,
                    t.registration_deadline,
                    t.max_teams,
                    t.prize_pool,
                    t.prize_description,
                    t.description,
                    t.rules,
                    t.photo_path,
                    t.organizer_user_id,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS organizer_name,
                    g.game_id,
                    g.name AS game_name,
                    c.category_id,
                    c.name AS category_name,
                    COALESCE(reg.registered_count, 0) AS registered_count,
                    COALESCE(acc.accepted_count, 0) AS accepted_count,
                    COALESCE(mt.matches_total, 0) AS matches_total,
                    COALESCE(mf.matches_finished, 0) AS matches_finished
                FROM tournaments t
                JOIN users u ON u.user_id = t.organizer_user_id
                JOIN games g ON g.game_id = t.game_id
                LEFT JOIN categories c ON c.category_id = g.category_id
                LEFT JOIN (
                    SELECT tournament_id, COUNT(*) AS registered_count
                    FROM tournament_teams
                    WHERE status IN ('PENDING', 'ACCEPTED')
                    GROUP BY tournament_id
                ) reg ON reg.tournament_id = t.tournament_id
                LEFT JOIN (
                    SELECT tournament_id, COUNT(*) AS accepted_count
                    FROM tournament_teams
                    WHERE status = 'ACCEPTED'
                    GROUP BY tournament_id
                ) acc ON acc.tournament_id = t.tournament_id
                LEFT JOIN (
                    SELECT tournament_id, COUNT(*) AS matches_total
                    FROM matches
                    GROUP BY tournament_id
                ) mt ON mt.tournament_id = t.tournament_id
                LEFT JOIN (
                    SELECT tournament_id, COUNT(*) AS matches_finished
                    FROM matches
                    WHERE status = 'FINISHED'
                    GROUP BY tournament_id
                ) mf ON mf.tournament_id = t.tournament_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        if (!filter.q().isBlank()) {
            sql.append(" AND (LOWER(t.title) LIKE ? OR LOWER(COALESCE(t.description, '')) LIKE ? OR LOWER(g.name) LIKE ?) ");
            String like = "%" + filter.q().toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (filter.gameId() != null) {
            sql.append(" AND t.game_id = ? ");
            params.add(filter.gameId());
        }
        if (filter.categoryId() != null) {
            sql.append(" AND c.category_id = ? ");
            params.add(filter.categoryId());
        }
        if (!filter.status().isBlank()) {
            sql.append(" AND t.status = ? ");
            params.add(filter.status());
        }
        if (!filter.format().isBlank()) {
            sql.append(" AND t.format = ? ");
            params.add(filter.format());
        }
        if (!filter.registrationMode().isBlank()) {
            sql.append(" AND t.registration_mode = ? ");
            params.add(filter.registrationMode());
        }
        if (filter.dateFrom() != null) {
            sql.append(" AND t.start_date >= ? ");
            params.add(Date.valueOf(filter.dateFrom()));
        }
        if (filter.dateTo() != null) {
            sql.append(" AND t.end_date <= ? ");
            params.add(Date.valueOf(filter.dateTo()));
        }
        if (filter.prizeMin() != null) {
            sql.append(" AND t.prize_pool >= ? ");
            params.add(filter.prizeMin());
        }
        if (filter.prizeMax() != null) {
            sql.append(" AND t.prize_pool <= ? ");
            params.add(filter.prizeMax());
        }

        switch (filter.sort()) {
            case "prize" -> sql.append(" ORDER BY t.prize_pool DESC, t.start_date DESC ");
            case "progress" -> sql.append(" ORDER BY CASE t.status WHEN 'ONGOING' THEN 0 WHEN 'OPEN' THEN 1 WHEN 'FINISHED' THEN 2 ELSE 3 END ASC, t.start_date ASC ");
            case "oldest" -> sql.append(" ORDER BY t.start_date ASC, t.created_at ASC ");
            default -> sql.append(" ORDER BY t.start_date DESC, t.created_at DESC ");
        }

        sql.append(" LIMIT ? ");
        params.add(Math.max(1, limit));

        List<TournamentCatalogRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TournamentCatalogRow(
                            rs.getInt("tournament_id"),
                            rs.getString("title"),
                            rs.getString("status"),
                            rs.getString("format"),
                            rs.getString("registration_mode"),
                            toLocalDate(rs.getDate("start_date")),
                            toLocalDate(rs.getDate("end_date")),
                            toLocalDate(rs.getDate("registration_deadline")),
                            rs.getInt("max_teams"),
                            rs.getBigDecimal("prize_pool"),
                            rs.getString("prize_description"),
                            rs.getString("description"),
                            rs.getString("rules"),
                            rs.getString("photo_path"),
                            rs.getInt("organizer_user_id"),
                            rs.getString("organizer_name"),
                            rs.getInt("game_id"),
                            rs.getString("game_name"),
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            rs.getInt("registered_count"),
                            rs.getInt("accepted_count"),
                            rs.getInt("matches_total"),
                            rs.getInt("matches_finished")
                    ));
                }
            }
        }
        return rows;
    }

    public TournamentCatalogRow loadTournamentDetail(Integer selectedTournamentId) throws SQLException {
        Integer effectiveId = selectedTournamentId;
        if (effectiveId == null || effectiveId <= 0) {
            effectiveId = findLatestTournamentId();
        }
        if (effectiveId == null || effectiveId <= 0) {
            return null;
        }

        String sql = """
                SELECT
                    t.tournament_id,
                    t.title,
                    t.status,
                    t.format,
                    t.registration_mode,
                    t.start_date,
                    t.end_date,
                    t.registration_deadline,
                    t.max_teams,
                    t.prize_pool,
                    t.prize_description,
                    t.description,
                    t.rules,
                    t.photo_path,
                    t.organizer_user_id,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS organizer_name,
                    g.game_id,
                    g.name AS game_name,
                    c.category_id,
                    c.name AS category_name,
                    COALESCE(reg.registered_count, 0) AS registered_count,
                    COALESCE(acc.accepted_count, 0) AS accepted_count,
                    COALESCE(mt.matches_total, 0) AS matches_total,
                    COALESCE(mf.matches_finished, 0) AS matches_finished
                FROM tournaments t
                JOIN users u ON u.user_id = t.organizer_user_id
                JOIN games g ON g.game_id = t.game_id
                LEFT JOIN categories c ON c.category_id = g.category_id
                LEFT JOIN (
                    SELECT tournament_id, COUNT(*) AS registered_count
                    FROM tournament_teams
                    WHERE status IN ('PENDING', 'ACCEPTED')
                    GROUP BY tournament_id
                ) reg ON reg.tournament_id = t.tournament_id
                LEFT JOIN (
                    SELECT tournament_id, COUNT(*) AS accepted_count
                    FROM tournament_teams
                    WHERE status = 'ACCEPTED'
                    GROUP BY tournament_id
                ) acc ON acc.tournament_id = t.tournament_id
                LEFT JOIN (
                    SELECT tournament_id, COUNT(*) AS matches_total
                    FROM matches
                    GROUP BY tournament_id
                ) mt ON mt.tournament_id = t.tournament_id
                LEFT JOIN (
                    SELECT tournament_id, COUNT(*) AS matches_finished
                    FROM matches
                    WHERE status = 'FINISHED'
                    GROUP BY tournament_id
                ) mf ON mf.tournament_id = t.tournament_id
                WHERE t.tournament_id = ?
                LIMIT 1
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, effectiveId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new TournamentCatalogRow(
                        rs.getInt("tournament_id"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getString("format"),
                        rs.getString("registration_mode"),
                        toLocalDate(rs.getDate("start_date")),
                        toLocalDate(rs.getDate("end_date")),
                        toLocalDate(rs.getDate("registration_deadline")),
                        rs.getInt("max_teams"),
                        rs.getBigDecimal("prize_pool"),
                        rs.getString("prize_description"),
                        rs.getString("description"),
                        rs.getString("rules"),
                        rs.getString("photo_path"),
                        rs.getInt("organizer_user_id"),
                        rs.getString("organizer_name"),
                        rs.getInt("game_id"),
                        rs.getString("game_name"),
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getInt("registered_count"),
                        rs.getInt("accepted_count"),
                        rs.getInt("matches_total"),
                        rs.getInt("matches_finished")
                );
            }
        }
    }

    public Integer findLatestTournamentId() throws SQLException {
        String sql = """
                SELECT t.tournament_id
                FROM tournaments t
                ORDER BY t.start_date DESC, t.created_at DESC
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                int id = rs.getInt(1);
                return id > 0 ? id : null;
            }
        }
        return null;
    }
    public List<ParticipantRow> listTournamentParticipants(int tournamentId) throws SQLException {
        String sql = """
                SELECT
                    tt.team_id,
                    t.name,
                    COALESCE(t.region, 'Region -') AS region,
                    i.file_url AS logo_path,
                    tt.status,
                    tt.seed,
                    tt.checked_in,
                    tt.registered_at
                FROM tournament_teams tt
                JOIN teams t ON t.team_id = tt.team_id
                LEFT JOIN images i ON i.image_id = t.logo_image_id
                WHERE tt.tournament_id = ?
                ORDER BY CASE tt.status WHEN 'ACCEPTED' THEN 0 WHEN 'PENDING' THEN 1 ELSE 2 END ASC,
                         tt.registered_at DESC
                """;

        List<ParticipantRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tournamentId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ParticipantRow(
                            rs.getInt("team_id"),
                            rs.getString("name"),
                            rs.getString("region"),
                            rs.getString("logo_path"),
                            rs.getString("status"),
                            toInteger(rs.getObject("seed")),
                            rs.getBoolean("checked_in"),
                            toLocalDateTime(rs.getTimestamp("registered_at"))
                    ));
                }
            }
        }
        return rows;
    }

    public List<ScoreboardRow> listTournamentScoreboard(int tournamentId) throws SQLException {
        String sql = """
                SELECT
                    mt.team_id,
                    t.name AS team_name,
                    SUM(CASE WHEN m.status = 'FINISHED' THEN 1 ELSE 0 END) AS played,
                    SUM(CASE WHEN m.status = 'FINISHED' AND mt.is_winner = 1 THEN 1 ELSE 0 END) AS wins,
                    SUM(CASE WHEN m.status = 'FINISHED' AND mt.is_winner = 0 THEN 1 ELSE 0 END) AS losses,
                    SUM(CASE WHEN m.status = 'FINISHED' AND mt.is_winner = 1 THEN 3 ELSE 0 END) AS points
                FROM match_teams mt
                JOIN matches m ON m.match_id = mt.match_id
                JOIN teams t ON t.team_id = mt.team_id
                WHERE m.tournament_id = ?
                GROUP BY mt.team_id, t.name
                ORDER BY points DESC, wins DESC, losses ASC, t.name ASC
                """;

        List<ScoreboardRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tournamentId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ScoreboardRow(
                            rs.getInt("team_id"),
                            rs.getString("team_name"),
                            rs.getInt("played"),
                            rs.getInt("wins"),
                            rs.getInt("losses"),
                            rs.getInt("points")
                    ));
                }
            }
        }
        return rows;
    }

    public List<MatchRow> listTournamentMatches(int tournamentId) throws SQLException {
        MatchSearchFilter filter = new MatchSearchFilter("", tournamentId, "", null, null, null, "", "oldest");
        return searchMatchesInternal(filter, 500, true);
    }

    public List<MatchRow> searchFrontMatches(MatchSearchFilter rawFilter, int limit) throws SQLException {
        MatchSearchFilter filter = normalizeMatchFilter(rawFilter, "upcoming");
        return searchMatchesInternal(filter, limit, false);
    }

    public List<MatchRow> searchAdminMatches(MatchSearchFilter rawFilter, int limit) throws SQLException {
        MatchSearchFilter filter = normalizeMatchFilter(rawFilter, "latest");
        return searchMatchesInternal(filter, limit, true);
    }

    private List<MatchRow> searchMatchesInternal(MatchSearchFilter filter, int limit, boolean adminMode) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    m.match_id,
                    m.tournament_id,
                    m.round_name,
                    m.best_of,
                    m.status,
                    m.scheduled_at,
                    m.result_submitted_by_user_id,
                    COALESCE(NULLIF(submitter.display_name, ''), submitter.username) AS submitter_name,
                    t.title AS tournament_title,
                    t.photo_path AS tournament_photo,
                    g.name AS game_name
                FROM matches m
                JOIN tournaments t ON t.tournament_id = m.tournament_id
                JOIN games g ON g.game_id = t.game_id
                LEFT JOIN users submitter ON submitter.user_id = m.result_submitted_by_user_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        if (!filter.q().isBlank()) {
            sql.append(" AND (LOWER(COALESCE(m.round_name, '')) LIKE ? OR LOWER(t.title) LIKE ? OR LOWER(g.name) LIKE ?) ");
            String like = "%" + filter.q().toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (filter.tournamentId() != null) {
            sql.append(" AND m.tournament_id = ? ");
            params.add(filter.tournamentId());
        }
        if (!filter.status().isBlank()) {
            sql.append(" AND m.status = ? ");
            params.add(filter.status());
        }
        if (filter.gameId() != null) {
            sql.append(" AND t.game_id = ? ");
            params.add(filter.gameId());
        }
        if (filter.dateFrom() != null) {
            sql.append(" AND m.scheduled_at IS NOT NULL AND m.scheduled_at >= ? ");
            params.add(Timestamp.valueOf(filter.dateFrom().atStartOfDay()));
        }
        if (filter.dateTo() != null) {
            sql.append(" AND m.scheduled_at IS NOT NULL AND m.scheduled_at <= ? ");
            params.add(Timestamp.valueOf(filter.dateTo().atTime(23, 59, 59)));
        }
        if (!filter.team().isBlank()) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM match_teams mtf
                         JOIN teams tf ON tf.team_id = mtf.team_id
                         WHERE mtf.match_id = m.match_id
                           AND LOWER(tf.name) LIKE ?
                     )
                    """);
            params.add("%" + filter.team().toLowerCase(Locale.ROOT) + "%");
        }

        if (adminMode) {
            switch (filter.sort()) {
                case "oldest" -> sql.append(" ORDER BY CASE WHEN m.scheduled_at IS NULL THEN 1 ELSE 0 END ASC, m.scheduled_at ASC, m.match_id ASC ");
                case "status" -> sql.append(" ORDER BY CASE m.status WHEN 'ONGOING' THEN 0 WHEN 'SCHEDULED' THEN 1 WHEN 'FINISHED' THEN 2 ELSE 3 END ASC, m.scheduled_at ASC, m.match_id ASC ");
                case "tournament" -> sql.append(" ORDER BY t.title ASC, m.scheduled_at DESC, m.match_id DESC ");
                default -> sql.append(" ORDER BY CASE WHEN m.scheduled_at IS NULL THEN 1 ELSE 0 END ASC, m.scheduled_at DESC, m.match_id DESC ");
            }
        } else {
            if ("latest".equals(filter.sort())) {
                sql.append(" ORDER BY m.scheduled_at DESC, m.match_id DESC ");
            } else {
                sql.append(" ORDER BY CASE m.status WHEN 'ONGOING' THEN 0 WHEN 'SCHEDULED' THEN 1 WHEN 'FINISHED' THEN 2 ELSE 3 END ASC, m.scheduled_at ASC, m.match_id ASC ");
            }
        }

        sql.append(" LIMIT ? ");
        params.add(Math.max(1, limit));

        List<MatchRow> rows = new ArrayList<>();
        List<Integer> matchIds = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int matchId = rs.getInt("match_id");
                    matchIds.add(matchId);
                    rows.add(new MatchRow(
                            matchId,
                            rs.getInt("tournament_id"),
                            rs.getString("tournament_title"),
                            rs.getString("game_name"),
                            rs.getString("round_name"),
                            toInteger(rs.getObject("best_of")),
                            rs.getString("status"),
                            toLocalDateTime(rs.getTimestamp("scheduled_at")),
                            toInteger(rs.getObject("result_submitted_by_user_id")),
                            rs.getString("submitter_name"),
                            rs.getString("tournament_photo"),
                            List.of()
                    ));
                }
            }
        }

        if (rows.isEmpty()) {
            return rows;
        }

        Map<Integer, List<MatchTeamRow>> teamsByMatchId = loadMatchTeams(matchIds);
        List<MatchRow> enriched = new ArrayList<>();
        for (MatchRow row : rows) {
            enriched.add(new MatchRow(
                    row.matchId(),
                    row.tournamentId(),
                    row.tournamentTitle(),
                    row.gameName(),
                    row.roundName(),
                    row.bestOf(),
                    row.status(),
                    row.scheduledAt(),
                    row.resultSubmittedByUserId(),
                    row.resultSubmittedByName(),
                    row.tournamentPhotoPath(),
                    teamsByMatchId.getOrDefault(row.matchId(), List.of())
            ));
        }
        return enriched;
    }

    public MatchRow loadMatchDetail(Integer matchId) throws SQLException {
        Integer effectiveId = matchId;
        if (effectiveId == null || effectiveId <= 0) {
            effectiveId = findLatestMatchId();
        }
        if (effectiveId == null || effectiveId <= 0) {
            return null;
        }

        String sql = """
                SELECT
                    m.match_id,
                    m.tournament_id,
                    m.round_name,
                    m.best_of,
                    m.status,
                    m.scheduled_at,
                    m.result_submitted_by_user_id,
                    COALESCE(NULLIF(submitter.display_name, ''), submitter.username) AS submitter_name,
                    t.title AS tournament_title,
                    t.photo_path AS tournament_photo,
                    g.name AS game_name
                FROM matches m
                JOIN tournaments t ON t.tournament_id = m.tournament_id
                JOIN games g ON g.game_id = t.game_id
                LEFT JOIN users submitter ON submitter.user_id = m.result_submitted_by_user_id
                WHERE m.match_id = ?
                LIMIT 1
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, effectiveId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                List<MatchTeamRow> teams = loadMatchTeams(List.of(effectiveId)).getOrDefault(effectiveId, List.of());
                return new MatchRow(
                        rs.getInt("match_id"),
                        rs.getInt("tournament_id"),
                        rs.getString("tournament_title"),
                        rs.getString("game_name"),
                        rs.getString("round_name"),
                        toInteger(rs.getObject("best_of")),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("scheduled_at")),
                        toInteger(rs.getObject("result_submitted_by_user_id")),
                        rs.getString("submitter_name"),
                        rs.getString("tournament_photo"),
                        teams
                );
            }
        }
    }

    public Integer findLatestMatchId() throws SQLException {
        String sql = """
                SELECT match_id
                FROM matches
                ORDER BY scheduled_at DESC, match_id DESC
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                int id = rs.getInt(1);
                return id > 0 ? id : null;
            }
        }
        return null;
    }
    public OperationResult registerTeamToTournament(int tournamentId, int teamId, int userId) throws SQLException {
        if (tournamentId <= 0 || teamId <= 0 || userId <= 0) {
            return OperationResult.error("Donnees invalides.");
        }

        String teamOwnerSql = "SELECT captain_user_id FROM teams WHERE team_id = ? LIMIT 1";
        Integer captainId = null;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(teamOwnerSql)) {
            statement.setInt(1, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    captainId = toInteger(rs.getObject("captain_user_id"));
                }
            }
        }

        if (captainId == null || captainId != userId) {
            return OperationResult.error("Equipe invalide. Selectionnez une equipe de votre capitaine.");
        }

        TournamentCatalogRow tournament = loadTournamentDetail(tournamentId);
        if (tournament == null) {
            return OperationResult.error("Tournoi introuvable.");
        }

        if (!"OPEN".equalsIgnoreCase(tournament.status())) {
            return OperationResult.error("Les inscriptions sont fermees pour ce tournoi.");
        }

        if (tournament.maxTeams() > 0 && tournament.acceptedCount() >= tournament.maxTeams()) {
            return OperationResult.error("Le tournoi est deja complet.");
        }

        if (tournament.registrationDeadline() != null && tournament.registrationDeadline().isBefore(LocalDate.now())) {
            return OperationResult.error("La date limite d'inscription est depassee.");
        }

        String findSql = """
                SELECT status
                FROM tournament_teams
                WHERE tournament_id = ? AND team_id = ?
                LIMIT 1
                """;
        String existingStatus = null;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(findSql)) {
            statement.setInt(1, tournamentId);
            statement.setInt(2, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    existingStatus = rs.getString("status");
                }
            }
        }

        if (existingStatus != null) {
            String statusUpper = existingStatus.toUpperCase(Locale.ROOT);
            if ("PENDING".equals(statusUpper) || "ACCEPTED".equals(statusUpper)) {
                return OperationResult.error("Cette equipe est deja inscrite (ou en attente) pour ce tournoi.");
            }
        }

        String targetStatus = "OPEN".equalsIgnoreCase(tournament.registrationMode()) ? "ACCEPTED" : "PENDING";

        try (Connection connection = Jdbc.open()) {
            if (existingStatus == null) {
                String insertSql = """
                        INSERT INTO tournament_teams (
                            tournament_id,
                            team_id,
                            status,
                            seed,
                            registered_at,
                            decided_at,
                            decided_by_user_id,
                            checked_in,
                            checkin_at
                        ) VALUES (?, ?, ?, NULL, NOW(), NULL, NULL, 0, NULL)
                        """;
                try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                    statement.setInt(1, tournamentId);
                    statement.setInt(2, teamId);
                    statement.setString(3, targetStatus);
                    statement.executeUpdate();
                }
            } else {
                String updateSql = """
                        UPDATE tournament_teams
                        SET status = ?,
                            registered_at = NOW(),
                            decided_at = NULL,
                            decided_by_user_id = NULL,
                            checked_in = 0,
                            checkin_at = NULL
                        WHERE tournament_id = ? AND team_id = ?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setString(1, targetStatus);
                    statement.setInt(2, tournamentId);
                    statement.setInt(3, teamId);
                    statement.executeUpdate();
                }
            }
        }

        if ("ACCEPTED".equals(targetStatus)) {
            return OperationResult.ok("Equipe inscrite avec succes au tournoi.");
        }
        return OperationResult.ok("Demande de participation envoyee avec succes.");
    }

    public List<RequestRow> searchOrganizerRequests(int organizerUserId, RequestSearchFilter rawFilter, int limit) throws SQLException {
        RequestSearchFilter filter = normalizeRequestFilter(rawFilter);

        StringBuilder sql = new StringBuilder(baseRequestSelectSql());
        sql.append(" AND tr.organizer_user_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(organizerUserId);
        applyRequestFilter(sql, params, filter);
        applyRequestSort(sql, filter.sort());
        sql.append(" LIMIT ? ");
        params.add(Math.max(1, limit));

        return readRequestRows(sql.toString(), params);
    }

    public List<RequestRow> searchAdminRequests(RequestSearchFilter rawFilter, int limit) throws SQLException {
        RequestSearchFilter filter = normalizeRequestFilter(rawFilter);

        StringBuilder sql = new StringBuilder(baseRequestSelectSql());
        List<Object> params = new ArrayList<>();
        applyRequestFilter(sql, params, filter);
        applyRequestSort(sql, filter.sort());
        sql.append(" LIMIT ? ");
        params.add(Math.max(1, limit));

        return readRequestRows(sql.toString(), params);
    }

    public OperationResult createOrganizerRequest(RequestMutationInput rawInput) throws SQLException {
        if (rawInput == null) {
            return OperationResult.error("Formulaire invalide.");
        }

        String format = normalizeTournamentFormat(rawInput.format());
        String registrationMode = normalizeRegistrationMode(rawInput.registrationMode());
        BigDecimal prizePool = rawInput.prizePool() == null ? BigDecimal.ZERO : rawInput.prizePool().max(BigDecimal.ZERO);

        if (rawInput.organizerUserId() <= 0 || rawInput.gameId() <= 0) {
            return OperationResult.error("Utilisateur ou jeu invalide.");
        }
        if (rawInput.title() == null || rawInput.title().trim().length() < 3) {
            return OperationResult.error("Le titre doit contenir au moins 3 caracteres.");
        }
        if (rawInput.startDate() == null || rawInput.endDate() == null) {
            return OperationResult.error("Les dates de debut et de fin sont obligatoires.");
        }
        if (rawInput.endDate().isBefore(rawInput.startDate())) {
            return OperationResult.error("La date de fin doit etre superieure ou egale a la date de debut.");
        }
        if (rawInput.registrationDeadline() != null && rawInput.registrationDeadline().isAfter(rawInput.startDate())) {
            return OperationResult.error("La date limite d'inscription doit etre inferieure ou egale a la date de debut.");
        }
        if (rawInput.maxTeams() < 2) {
            return OperationResult.error("Le nombre maximum d'equipes doit etre >= 2.");
        }

        String sql = """
                INSERT INTO tournament_requests (
                    organizer_user_id,
                    game_id,
                    title,
                    description,
                    rules,
                    start_date,
                    end_date,
                    registration_deadline,
                    max_teams,
                    format,
                    registration_mode,
                    prize_pool,
                    prize_description,
                    status,
                    photo_path,
                    admin_response_note,
                    created_at,
                    reviewed_by_admin_id,
                    reviewed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, NULL, NOW(), NULL, NULL)
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, rawInput.organizerUserId());
            statement.setInt(2, rawInput.gameId());
            statement.setString(3, rawInput.title().trim());
            statement.setString(4, nullable(rawInput.description()));
            statement.setString(5, nullable(rawInput.rules()));
            statement.setDate(6, Date.valueOf(rawInput.startDate()));
            statement.setDate(7, Date.valueOf(rawInput.endDate()));
            if (rawInput.registrationDeadline() == null) {
                statement.setObject(8, null);
            } else {
                statement.setDate(8, Date.valueOf(rawInput.registrationDeadline()));
            }
            statement.setInt(9, rawInput.maxTeams());
            statement.setString(10, format);
            statement.setString(11, registrationMode);
            statement.setBigDecimal(12, prizePool);
            statement.setString(13, nullable(rawInput.prizeDescription()));
            statement.setString(14, nullable(rawInput.photoPath()));
            statement.executeUpdate();
        }

        return OperationResult.ok("Demande de tournoi enregistree avec succes.");
    }

    public List<RegistrationRow> listOrganizerRegistrations(int organizerUserId, Integer tournamentId) throws SQLException {
        Integer targetTournament = resolveOrganizerTournament(organizerUserId, tournamentId);
        if (targetTournament == null) {
            return List.of();
        }

        String sql = """
                SELECT
                    tt.tournament_id,
                    tt.team_id,
                    t.name AS team_name,
                    tt.status,
                    tt.seed,
                    tt.checked_in,
                    tt.registered_at
                FROM tournament_teams tt
                JOIN teams t ON t.team_id = tt.team_id
                WHERE tt.tournament_id = ?
                ORDER BY tt.registered_at DESC
                """;

        List<RegistrationRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, targetTournament);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new RegistrationRow(
                            rs.getInt("tournament_id"),
                            rs.getInt("team_id"),
                            rs.getString("team_name"),
                            rs.getString("status"),
                            toInteger(rs.getObject("seed")),
                            rs.getBoolean("checked_in"),
                            toLocalDateTime(rs.getTimestamp("registered_at"))
                    ));
                }
            }
        }
        return rows;
    }

    public OperationResult updateOrganizerRegistrationStatus(int organizerUserId, int tournamentId, int teamId, String status) throws SQLException {
        String normalizedStatus = normalizeRequestStatus(status);
        if (!Set.of("PENDING", "ACCEPTED", "REFUSED", "CANCELLED").contains(normalizedStatus)) {
            return OperationResult.error("Statut invalide.");
        }

        Integer targetTournament = resolveOrganizerTournament(organizerUserId, tournamentId);
        if (targetTournament == null || targetTournament != tournamentId) {
            return OperationResult.error("Tournoi introuvable.");
        }

        String updateSql = """
                UPDATE tournament_teams
                SET status = ?, decided_at = NOW(), decided_by_user_id = ?
                WHERE tournament_id = ? AND team_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setString(1, normalizedStatus);
            statement.setInt(2, organizerUserId);
            statement.setInt(3, tournamentId);
            statement.setInt(4, teamId);
            int updated = statement.executeUpdate();
            if (updated <= 0) {
                return OperationResult.error("Inscription introuvable.");
            }
        }

        return OperationResult.ok("Statut d'inscription mis a jour.");
    }

    public List<MatchRow> listOrganizerMatches(int organizerUserId, Integer tournamentId) throws SQLException {
        Integer targetTournament = resolveOrganizerTournament(organizerUserId, tournamentId);
        if (targetTournament == null) {
            return List.of();
        }

        MatchSearchFilter filter = new MatchSearchFilter("", targetTournament, "", null, null, null, "", "oldest");
        return searchMatchesInternal(filter, 500, true);
    }
    public OperationResult upsertMatch(MatchMutationInput input, int actorUserId, boolean restrictToOrganizer) throws SQLException {
        if (input == null) {
            return OperationResult.error("Formulaire invalide.");
        }

        String normalizedStatus = normalizeMatchStatus(input.status());
        if (input.tournamentId() <= 0) {
            return OperationResult.error("Tournoi invalide.");
        }

        if (restrictToOrganizer) {
            Integer organizerTournament = resolveOrganizerTournament(actorUserId, input.tournamentId());
            if (organizerTournament == null || organizerTournament != input.tournamentId()) {
                return OperationResult.error("Vous ne pouvez pas modifier ce tournoi.");
            }
        }

        List<MatchParticipantInput> participants = sanitizeParticipants(input.participants());
        if (participants.size() < 2) {
            return OperationResult.error("Un match doit avoir au moins deux equipes.");
        }

        Set<Integer> acceptedTeamIds = listAcceptedTeamIdsForTournament(input.tournamentId());
        for (MatchParticipantInput participant : participants) {
            if (!acceptedTeamIds.contains(participant.teamId())) {
                return OperationResult.error("Selection d'equipes invalide pour ce tournoi.");
            }
        }

        try (Connection connection = Jdbc.open()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Integer matchId = input.matchId();
                if (matchId != null && matchId > 0) {
                    if (restrictToOrganizer && !isMatchOwnedByOrganizer(connection, matchId, actorUserId)) {
                        connection.rollback();
                        return OperationResult.error("Vous ne pouvez pas modifier ce match.");
                    }
                    updateMatchRow(connection, matchId, input, normalizedStatus, actorUserId);
                } else {
                    matchId = insertMatchRow(connection, input, normalizedStatus, actorUserId);
                }

                syncMatchTeams(connection, matchId, participants);
                connection.commit();
                return OperationResult.ok(input.matchId() == null ? "Match cree." : "Match mis a jour.");
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public OperationResult deleteMatch(int matchId, int actorUserId, boolean restrictToOrganizer) throws SQLException {
        if (matchId <= 0) {
            return OperationResult.error("Match invalide.");
        }

        String deleteMatchTeamsSql = "DELETE FROM match_teams WHERE match_id = ?";
        String deleteMatchSql = "DELETE FROM matches WHERE match_id = ?";
        try (Connection connection = Jdbc.open()) {
            if (restrictToOrganizer && !isMatchOwnedByOrganizer(connection, matchId, actorUserId)) {
                return OperationResult.error("Vous ne pouvez pas supprimer ce match.");
            }

            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement deleteTeams = connection.prepareStatement(deleteMatchTeamsSql)) {
                    deleteTeams.setInt(1, matchId);
                    deleteTeams.executeUpdate();
                }

                int deleted;
                try (PreparedStatement deleteMatch = connection.prepareStatement(deleteMatchSql)) {
                    deleteMatch.setInt(1, matchId);
                    deleted = deleteMatch.executeUpdate();
                }

                if (deleted <= 0) {
                    connection.rollback();
                    return OperationResult.error("Match introuvable.");
                }

                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
        return OperationResult.ok("Match supprime.");
    }

    public ReviewResult reviewTournamentRequest(int requestId, String decision, String adminNote, int adminUserId) throws SQLException {
        String normalizedDecision = normalizeRequestStatus(decision);
        if (!Set.of("ACCEPTED", "REFUSED").contains(normalizedDecision)) {
            return new ReviewResult(false, "Decision invalide.", null, null, normalizedDecision);
        }

        String selectSql = """
                SELECT
                    tr.request_id,
                    tr.organizer_user_id,
                    tr.game_id,
                    tr.title,
                    tr.description,
                    tr.rules,
                    tr.start_date,
                    tr.end_date,
                    tr.registration_deadline,
                    tr.max_teams,
                    tr.format,
                    tr.registration_mode,
                    tr.prize_pool,
                    tr.prize_description,
                    tr.photo_path,
                    tr.status,
                    u.email AS organizer_email
                FROM tournament_requests tr
                JOIN users u ON u.user_id = tr.organizer_user_id
                WHERE tr.request_id = ?
                LIMIT 1
                """;

        try (Connection connection = Jdbc.open()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                RequestRow request;
                try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                    select.setInt(1, requestId);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            return new ReviewResult(false, "Demande introuvable.", null, null, normalizedDecision);
                        }
                        request = new RequestRow(
                                rs.getInt("request_id"),
                                rs.getInt("organizer_user_id"),
                                "",
                                rs.getString("organizer_email"),
                                rs.getInt("game_id"),
                                "",
                                rs.getString("title"),
                                rs.getString("description"),
                                rs.getString("rules"),
                                toLocalDate(rs.getDate("start_date")),
                                toLocalDate(rs.getDate("end_date")),
                                toLocalDate(rs.getDate("registration_deadline")),
                                rs.getInt("max_teams"),
                                rs.getString("format"),
                                rs.getString("registration_mode"),
                                rs.getBigDecimal("prize_pool"),
                                rs.getString("prize_description"),
                                rs.getString("status"),
                                rs.getString("photo_path"),
                                null,
                                null,
                                null,
                                null,
                                null
                        );
                    }
                }

                String updateRequestSql = """
                        UPDATE tournament_requests
                        SET status = ?,
                            admin_response_note = ?,
                            reviewed_at = NOW(),
                            reviewed_by_admin_id = ?
                        WHERE request_id = ?
                        """;
                try (PreparedStatement update = connection.prepareStatement(updateRequestSql)) {
                    update.setString(1, normalizedDecision);
                    update.setString(2, nullable(adminNote));
                    update.setInt(3, adminUserId);
                    update.setInt(4, requestId);
                    update.executeUpdate();
                }

                if ("ACCEPTED".equals(normalizedDecision) && !"ACCEPTED".equalsIgnoreCase(request.status())) {
                    ensureTournamentFromAcceptedRequest(connection, request);
                }

                connection.commit();
                String message = "ACCEPTED".equals(normalizedDecision)
                        ? "Demande acceptee. Le tournoi est visible dans le Back Office et Front Office."
                        : "Demande refusee.";
                return new ReviewResult(true, message, request.organizerEmail(), request.title(), normalizedDecision);
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public List<TournamentCatalogRow> searchAdminTournaments(TournamentSearchFilter rawFilter, int limit) throws SQLException {
        return searchTournamentCatalog(rawFilter, limit);
    }

    public OperationResult upsertTournament(TournamentMutationInput rawInput) throws SQLException {
        if (rawInput == null) {
            return OperationResult.error("Formulaire invalide.");
        }

        String format = normalizeTournamentFormat(rawInput.format());
        String registrationMode = normalizeRegistrationMode(rawInput.registrationMode());
        String status = normalizeTournamentStatus(rawInput.status());
        BigDecimal prizePool = rawInput.prizePool() == null ? BigDecimal.ZERO : rawInput.prizePool().max(BigDecimal.ZERO);

        if (rawInput.organizerUserId() <= 0 || rawInput.gameId() <= 0) {
            return OperationResult.error("Organisateur ou jeu invalide.");
        }
        if (rawInput.title() == null || rawInput.title().trim().length() < 3) {
            return OperationResult.error("Le titre doit contenir au moins 3 caracteres.");
        }
        if (rawInput.startDate() == null || rawInput.endDate() == null) {
            return OperationResult.error("Dates invalides.");
        }
        if (rawInput.endDate().isBefore(rawInput.startDate())) {
            return OperationResult.error("La date de fin doit etre superieure ou egale a la date de debut.");
        }
        if (rawInput.registrationDeadline() != null && rawInput.registrationDeadline().isAfter(rawInput.startDate())) {
            return OperationResult.error("La date limite d'inscription doit etre inferieure ou egale a la date de debut.");
        }
        if (rawInput.maxTeams() < 2) {
            return OperationResult.error("Le nombre maximum d'equipes doit etre >= 2.");
        }

        if (rawInput.tournamentId() == null || rawInput.tournamentId() <= 0) {
            String insertSql = """
                    INSERT INTO tournaments (
                        organizer_user_id,
                        game_id,
                        title,
                        description,
                        rules,
                        start_date,
                        end_date,
                        registration_deadline,
                        max_teams,
                        format,
                        registration_mode,
                        prize_pool,
                        prize_description,
                        status,
                        photo_path,
                        created_at,
                        updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                    """;
            try (Connection connection = Jdbc.open();
                 PreparedStatement statement = connection.prepareStatement(insertSql)) {
                bindTournamentMutation(statement, rawInput, format, registrationMode, status, prizePool, false);
                statement.executeUpdate();
            }
            return OperationResult.ok("Tournoi cree avec succes.");
        }

        String updateSql = """
                UPDATE tournaments
                SET organizer_user_id = ?,
                    game_id = ?,
                    title = ?,
                    description = ?,
                    rules = ?,
                    start_date = ?,
                    end_date = ?,
                    registration_deadline = ?,
                    max_teams = ?,
                    format = ?,
                    registration_mode = ?,
                    prize_pool = ?,
                    prize_description = ?,
                    status = ?,
                    photo_path = ?,
                    updated_at = NOW()
                WHERE tournament_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(updateSql)) {
            bindTournamentMutation(statement, rawInput, format, registrationMode, status, prizePool, true);
            int updated = statement.executeUpdate();
            if (updated <= 0) {
                return OperationResult.error("Tournoi introuvable.");
            }
        }

        return OperationResult.ok("Tournoi mis a jour.");
    }

    public OperationResult deleteTournament(int tournamentId) throws SQLException {
        if (tournamentId <= 0) {
            return OperationResult.error("Tournoi invalide.");
        }

        String deleteMatchTeamsSql = """
                DELETE mt
                FROM match_teams mt
                JOIN matches m ON m.match_id = mt.match_id
                WHERE m.tournament_id = ?
                """;
        String deleteMatchesSql = "DELETE FROM matches WHERE tournament_id = ?";
        String deleteTournamentTeamsSql = "DELETE FROM tournament_teams WHERE tournament_id = ?";
        String deleteTournamentSql = "DELETE FROM tournaments WHERE tournament_id = ?";

        try (Connection connection = Jdbc.open()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement deleteMatchTeams = connection.prepareStatement(deleteMatchTeamsSql)) {
                    deleteMatchTeams.setInt(1, tournamentId);
                    deleteMatchTeams.executeUpdate();
                }

                try (PreparedStatement deleteMatches = connection.prepareStatement(deleteMatchesSql)) {
                    deleteMatches.setInt(1, tournamentId);
                    deleteMatches.executeUpdate();
                }

                try (PreparedStatement deleteTournamentTeams = connection.prepareStatement(deleteTournamentTeamsSql)) {
                    deleteTournamentTeams.setInt(1, tournamentId);
                    deleteTournamentTeams.executeUpdate();
                }

                int deleted;
                try (PreparedStatement deleteTournament = connection.prepareStatement(deleteTournamentSql)) {
                    deleteTournament.setInt(1, tournamentId);
                    deleted = deleteTournament.executeUpdate();
                }

                if (deleted <= 0) {
                    connection.rollback();
                    return OperationResult.error("Tournoi introuvable.");
                }

                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }

        return OperationResult.ok("Le tournoi a ete supprime.");
    }
    private void ensureTournamentFromAcceptedRequest(Connection connection, RequestRow request) throws SQLException {
        String existsSql = """
                SELECT tournament_id
                FROM tournaments
                WHERE organizer_user_id = ?
                  AND title = ?
                  AND start_date = ?
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(existsSql)) {
            statement.setInt(1, request.organizerUserId());
            statement.setString(2, request.title());
            statement.setDate(3, Date.valueOf(request.startDate()));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        String insertSql = """
                INSERT INTO tournaments (
                    organizer_user_id,
                    game_id,
                    title,
                    description,
                    rules,
                    start_date,
                    end_date,
                    registration_deadline,
                    max_teams,
                    format,
                    registration_mode,
                    prize_pool,
                    prize_description,
                    status,
                    photo_path,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, NOW(), NOW())
                """;

        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setInt(1, request.organizerUserId());
            statement.setInt(2, request.gameId());
            statement.setString(3, request.title());
            statement.setString(4, nullable(request.description()));
            statement.setString(5, nullable(request.rules()));
            statement.setDate(6, Date.valueOf(request.startDate()));
            statement.setDate(7, Date.valueOf(request.endDate()));
            if (request.registrationDeadline() == null) {
                statement.setObject(8, null);
            } else {
                statement.setDate(8, Date.valueOf(request.registrationDeadline()));
            }
            statement.setInt(9, Math.max(2, request.maxTeams()));
            statement.setString(10, normalizeTournamentFormat(request.format()));
            statement.setString(11, normalizeRegistrationMode(request.registrationMode()));
            statement.setBigDecimal(12, request.prizePool() == null ? BigDecimal.ZERO : request.prizePool());
            statement.setString(13, nullable(request.prizeDescription()));
            statement.setString(14, nullable(request.photoPath()));
            statement.executeUpdate();
        }
    }

    private void bindTournamentMutation(
            PreparedStatement statement,
            TournamentMutationInput rawInput,
            String format,
            String registrationMode,
            String status,
            BigDecimal prizePool,
            boolean includeId
    ) throws SQLException {
        int index = 1;
        statement.setInt(index++, rawInput.organizerUserId());
        statement.setInt(index++, rawInput.gameId());
        statement.setString(index++, rawInput.title().trim());
        statement.setString(index++, nullable(rawInput.description()));
        statement.setString(index++, nullable(rawInput.rules()));
        statement.setDate(index++, Date.valueOf(rawInput.startDate()));
        statement.setDate(index++, Date.valueOf(rawInput.endDate()));
        if (rawInput.registrationDeadline() == null) {
            statement.setObject(index++, null);
        } else {
            statement.setDate(index++, Date.valueOf(rawInput.registrationDeadline()));
        }
        statement.setInt(index++, rawInput.maxTeams());
        statement.setString(index++, format);
        statement.setString(index++, registrationMode);
        statement.setBigDecimal(index++, prizePool);
        statement.setString(index++, nullable(rawInput.prizeDescription()));
        statement.setString(index++, status);
        statement.setString(index++, nullable(rawInput.photoPath()));
        if (includeId) {
            statement.setInt(index, rawInput.tournamentId());
        }
    }

    private Map<Integer, List<MatchTeamRow>> loadMatchTeams(List<Integer> matchIds) throws SQLException {
        if (matchIds == null || matchIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(matchIds.size(), "?"));
        String sql = """
                SELECT
                    mt.match_id,
                    mt.team_id,
                    t.name AS team_name,
                    mt.score,
                    mt.is_winner
                FROM match_teams mt
                JOIN teams t ON t.team_id = mt.team_id
                WHERE mt.match_id IN (%s)
                ORDER BY mt.match_id ASC, mt.is_winner DESC, t.name ASC
                """.formatted(placeholders);

        Map<Integer, List<MatchTeamRow>> rows = new LinkedHashMap<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Integer matchId : matchIds) {
                statement.setInt(index++, matchId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int matchId = rs.getInt("match_id");
                    rows.computeIfAbsent(matchId, ignored -> new ArrayList<>())
                            .add(new MatchTeamRow(
                                    rs.getInt("team_id"),
                                    rs.getString("team_name"),
                                    toInteger(rs.getObject("score")),
                                    toBoolean(rs.getObject("is_winner"))
                            ));
                }
            }
        }
        return rows;
    }

    private Integer resolveOrganizerTournament(int organizerUserId, Integer requestedTournamentId) throws SQLException {
        if (organizerUserId <= 0) {
            return null;
        }

        if (requestedTournamentId != null && requestedTournamentId > 0) {
            String sql = """
                    SELECT tournament_id
                    FROM tournaments
                    WHERE tournament_id = ? AND organizer_user_id = ?
                    LIMIT 1
                    """;
            try (Connection connection = Jdbc.open();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, requestedTournamentId);
                statement.setInt(2, organizerUserId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("tournament_id");
                    }
                }
            }
        }

        String latestSql = """
                SELECT tournament_id
                FROM tournaments
                WHERE organizer_user_id = ?
                ORDER BY start_date DESC, created_at DESC
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(latestSql)) {
            statement.setInt(1, organizerUserId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("tournament_id");
                }
            }
        }

        return null;
    }

    private Set<Integer> listAcceptedTeamIdsForTournament(int tournamentId) throws SQLException {
        String sql = """
                SELECT team_id
                FROM tournament_teams
                WHERE tournament_id = ? AND status = 'ACCEPTED'
                """;
        Set<Integer> ids = new HashSet<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tournamentId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("team_id"));
                }
            }
        }
        return ids;
    }

    private boolean isMatchOwnedByOrganizer(Connection connection, int matchId, int organizerUserId) throws SQLException {
        String sql = """
                SELECT 1
                FROM matches m
                JOIN tournaments t ON t.tournament_id = m.tournament_id
                WHERE m.match_id = ? AND t.organizer_user_id = ?
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, matchId);
            statement.setInt(2, organizerUserId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int insertMatchRow(Connection connection, MatchMutationInput input, String status, int actorUserId) throws SQLException {
        String sql = """
                INSERT INTO matches (
                    tournament_id,
                    scheduled_at,
                    round_name,
                    best_of,
                    status,
                    created_at,
                    updated_at,
                    result_submitted_by_user_id
                ) VALUES (?, ?, ?, ?, ?, NOW(), NOW(), ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, input.tournamentId());
            if (input.scheduledAt() == null) {
                statement.setObject(2, null);
            } else {
                statement.setTimestamp(2, Timestamp.valueOf(input.scheduledAt()));
            }
            statement.setString(3, nullable(input.roundName()));
            if (input.bestOf() == null) {
                statement.setObject(4, null);
            } else {
                statement.setInt(4, input.bestOf());
            }
            statement.setString(5, status);
            if ("FINISHED".equals(status)) {
                statement.setInt(6, actorUserId);
            } else {
                statement.setObject(6, null);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("Impossible de creer le match.");
    }

    private void updateMatchRow(Connection connection, int matchId, MatchMutationInput input, String status, int actorUserId) throws SQLException {
        String sql = """
                UPDATE matches
                SET tournament_id = ?,
                    scheduled_at = ?,
                    round_name = ?,
                    best_of = ?,
                    status = ?,
                    updated_at = NOW(),
                    result_submitted_by_user_id = ?
                WHERE match_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, input.tournamentId());
            if (input.scheduledAt() == null) {
                statement.setObject(2, null);
            } else {
                statement.setTimestamp(2, Timestamp.valueOf(input.scheduledAt()));
            }
            statement.setString(3, nullable(input.roundName()));
            if (input.bestOf() == null) {
                statement.setObject(4, null);
            } else {
                statement.setInt(4, input.bestOf());
            }
            statement.setString(5, status);
            if ("FINISHED".equals(status)) {
                statement.setInt(6, actorUserId);
            } else {
                statement.setObject(6, null);
            }
            statement.setInt(7, matchId);
            int updated = statement.executeUpdate();
            if (updated <= 0) {
                throw new SQLException("Match introuvable.");
            }
        }
    }

    private void syncMatchTeams(Connection connection, int matchId, List<MatchParticipantInput> participants) throws SQLException {
        String deleteSql = "DELETE FROM match_teams WHERE match_id = ? AND team_id = ?";
        String upsertSql = """
                INSERT INTO match_teams (match_id, team_id, score, is_winner)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    score = VALUES(score),
                    is_winner = VALUES(is_winner)
                """;

        Set<Integer> selectedTeamIds = new HashSet<>();
        for (MatchParticipantInput participant : participants) {
            selectedTeamIds.add(participant.teamId());
        }

        List<Integer> existingTeamIds = new ArrayList<>();
        String existingSql = "SELECT team_id FROM match_teams WHERE match_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(existingSql)) {
            statement.setInt(1, matchId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    existingTeamIds.add(rs.getInt("team_id"));
                }
            }
        }

        try (PreparedStatement delete = connection.prepareStatement(deleteSql)) {
            for (Integer existingId : existingTeamIds) {
                if (selectedTeamIds.contains(existingId)) {
                    continue;
                }
                delete.setInt(1, matchId);
                delete.setInt(2, existingId);
                delete.addBatch();
            }
            delete.executeBatch();
        }

        try (PreparedStatement upsert = connection.prepareStatement(upsertSql)) {
            for (MatchParticipantInput participant : participants) {
                upsert.setInt(1, matchId);
                upsert.setInt(2, participant.teamId());
                if (participant.score() == null) {
                    upsert.setObject(3, null);
                } else {
                    upsert.setInt(3, Math.max(0, participant.score()));
                }
                upsert.setBoolean(4, participant.winner());
                upsert.addBatch();
            }
            upsert.executeBatch();
        }
    }
    private List<MatchParticipantInput> sanitizeParticipants(List<MatchParticipantInput> participants) {
        if (participants == null || participants.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<Integer, MatchParticipantInput> byTeam = new LinkedHashMap<>();
        for (MatchParticipantInput participant : participants) {
            if (participant == null || participant.teamId() <= 0) {
                continue;
            }
            Integer score = participant.score() == null ? null : Math.max(0, participant.score());
            byTeam.put(participant.teamId(), new MatchParticipantInput(participant.teamId(), score, participant.winner()));
        }
        return new ArrayList<>(byTeam.values());
    }

    private static String baseRequestSelectSql() {
        return """
                SELECT
                    tr.request_id,
                    tr.organizer_user_id,
                    COALESCE(NULLIF(organizer.display_name, ''), organizer.username) AS organizer_name,
                    organizer.email AS organizer_email,
                    tr.game_id,
                    g.name AS game_name,
                    tr.title,
                    tr.description,
                    tr.rules,
                    tr.start_date,
                    tr.end_date,
                    tr.registration_deadline,
                    tr.max_teams,
                    tr.format,
                    tr.registration_mode,
                    tr.prize_pool,
                    tr.prize_description,
                    tr.status,
                    tr.photo_path,
                    tr.admin_response_note,
                    tr.created_at,
                    tr.reviewed_at,
                    tr.reviewed_by_admin_id,
                    COALESCE(NULLIF(reviewer.display_name, ''), reviewer.username) AS reviewed_by_admin_name
                FROM tournament_requests tr
                JOIN games g ON g.game_id = tr.game_id
                LEFT JOIN users organizer ON organizer.user_id = tr.organizer_user_id
                LEFT JOIN users reviewer ON reviewer.user_id = tr.reviewed_by_admin_id
                WHERE 1=1
                """;
    }

    private static void applyRequestFilter(StringBuilder sql, List<Object> params, RequestSearchFilter filter) {
        if (!filter.q().isBlank()) {
            sql.append(" AND (LOWER(tr.title) LIKE ? OR LOWER(COALESCE(tr.description, '')) LIKE ? OR LOWER(g.name) LIKE ? OR LOWER(COALESCE(organizer.username, '')) LIKE ?) ");
            String like = "%" + filter.q().toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (!filter.status().isBlank()) {
            sql.append(" AND tr.status = ? ");
            params.add(filter.status());
        }

        if (filter.gameId() != null) {
            sql.append(" AND tr.game_id = ? ");
            params.add(filter.gameId());
        }
    }

    private static void applyRequestSort(StringBuilder sql, String sort) {
        switch (sort) {
            case "oldest" -> sql.append(" ORDER BY tr.created_at ASC, tr.request_id ASC ");
            case "title" -> sql.append(" ORDER BY tr.title ASC, tr.created_at DESC ");
            case "prize" -> sql.append(" ORDER BY tr.prize_pool DESC, tr.created_at DESC ");
            case "status" -> sql.append(" ORDER BY CASE tr.status WHEN 'PENDING' THEN 0 WHEN 'ACCEPTED' THEN 1 ELSE 2 END ASC, tr.created_at DESC ");
            default -> sql.append(" ORDER BY tr.created_at DESC, tr.request_id DESC ");
        }
    }

    private List<RequestRow> readRequestRows(String sql, List<Object> params) throws SQLException {
        List<RequestRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new RequestRow(
                            rs.getInt("request_id"),
                            rs.getInt("organizer_user_id"),
                            rs.getString("organizer_name"),
                            rs.getString("organizer_email"),
                            rs.getInt("game_id"),
                            rs.getString("game_name"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("rules"),
                            toLocalDate(rs.getDate("start_date")),
                            toLocalDate(rs.getDate("end_date")),
                            toLocalDate(rs.getDate("registration_deadline")),
                            rs.getInt("max_teams"),
                            rs.getString("format"),
                            rs.getString("registration_mode"),
                            rs.getBigDecimal("prize_pool"),
                            rs.getString("prize_description"),
                            rs.getString("status"),
                            rs.getString("photo_path"),
                            rs.getString("admin_response_note"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("reviewed_at")),
                            toInteger(rs.getObject("reviewed_by_admin_id")),
                            rs.getString("reviewed_by_admin_name")
                    ));
                }
            }
        }
        return rows;
    }

    private List<LookupItem> loadLookups(String sql) throws SQLException {
        List<LookupItem> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new LookupItem(rs.getInt("id"), rs.getString("label")));
            }
        }
        return rows;
    }

    private static TournamentSearchFilter normalizeTournamentFilter(TournamentSearchFilter raw) {
        TournamentSearchFilter source = raw == null ? TournamentSearchFilter.defaults() : raw;

        String status = normalizeEnum(source.status(), TOURNAMENT_STATUSES);
        String format = normalizeEnum(source.format(), TOURNAMENT_FORMATS);
        String registrationMode = normalizeEnum(source.registrationMode(), REGISTRATION_MODES);

        BigDecimal min = source.prizeMin();
        BigDecimal max = source.prizeMax();
        if (min != null && max != null && min.compareTo(max) > 0) {
            BigDecimal tmp = min;
            min = max;
            max = tmp;
        }

        String sort = normalizeSort(source.sort(), Set.of("latest", "oldest", "prize", "progress"), "latest");

        return new TournamentSearchFilter(
                normalizeText(source.q()),
                positiveOrNull(source.gameId()),
                positiveOrNull(source.categoryId()),
                status,
                format,
                registrationMode,
                source.dateFrom(),
                source.dateTo(),
                min,
                max,
                sort
        );
    }

    private static MatchSearchFilter normalizeMatchFilter(MatchSearchFilter raw, String defaultSort) {
        MatchSearchFilter source = raw == null ? MatchSearchFilter.defaults(defaultSort) : raw;
        String status = normalizeEnum(source.status(), MATCH_STATUSES);
        String sort = normalizeSort(source.sort(), Set.of("upcoming", "latest", "oldest", "status", "tournament"), defaultSort);

        return new MatchSearchFilter(
                normalizeText(source.q()),
                positiveOrNull(source.tournamentId()),
                status,
                positiveOrNull(source.gameId()),
                source.dateFrom(),
                source.dateTo(),
                normalizeText(source.team()),
                sort
        );
    }

    private static RequestSearchFilter normalizeRequestFilter(RequestSearchFilter raw) {
        RequestSearchFilter source = raw == null ? RequestSearchFilter.defaults() : raw;
        String status = normalizeEnum(source.status(), REQUEST_STATUSES);
        String sort = normalizeSort(source.sort(), Set.of("latest", "oldest", "title", "prize", "status"), "latest");
        return new RequestSearchFilter(
                normalizeText(source.q()),
                status,
                positiveOrNull(source.gameId()),
                sort
        );
    }

    private static String normalizeTournamentStatus(String value) {
        return normalizeEnum(value, TOURNAMENT_STATUSES, "DRAFT");
    }

    private static String normalizeTournamentFormat(String value) {
        return normalizeEnum(value, TOURNAMENT_FORMATS, "BO1");
    }

    private static String normalizeRegistrationMode(String value) {
        return normalizeEnum(value, REGISTRATION_MODES, "OPEN");
    }

    private static String normalizeMatchStatus(String value) {
        return normalizeEnum(value, MATCH_STATUSES, "SCHEDULED");
    }

    private static String normalizeRequestStatus(String value) {
        return normalizeEnum(value, REQUEST_STATUSES, "PENDING");
    }

    private static String normalizeEnum(String value, Set<String> allowed) {
        return normalizeEnum(value, allowed, "");
    }

    private static String normalizeEnum(String value, Set<String> allowed, String fallback) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return fallback;
        }
        if (allowed.contains(normalized)) {
            return normalized;
        }
        return fallback;
    }

    private static String normalizeSort(String value, Set<String> allowed, String fallback) {
        String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
        if (allowed.contains(normalized)) {
            return normalized;
        }
        return fallback;
    }

    private static Integer positiveOrNull(Integer value) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String nullable(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer intValue) {
            return intValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            String normalized = text.trim();
            if (normalized.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(normalized);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String text) {
            String normalized = text.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                return null;
            }
            if (Set.of("1", "true", "yes", "oui").contains(normalized)) {
                return Boolean.TRUE;
            }
            if (Set.of("0", "false", "no", "non").contains(normalized)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private static void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        int index = 1;
        for (Object value : params) {
            if (value instanceof Integer intValue) {
                statement.setInt(index++, intValue);
                continue;
            }
            if (value instanceof Long longValue) {
                statement.setLong(index++, longValue);
                continue;
            }
            if (value instanceof String stringValue) {
                statement.setString(index++, stringValue);
                continue;
            }
            if (value instanceof Date dateValue) {
                statement.setDate(index++, dateValue);
                continue;
            }
            if (value instanceof Timestamp timestampValue) {
                statement.setTimestamp(index++, timestampValue);
                continue;
            }
            if (value instanceof BigDecimal decimalValue) {
                statement.setBigDecimal(index++, decimalValue);
                continue;
            }
            if (value == null) {
                statement.setObject(index++, null);
                continue;
            }
            statement.setObject(index++, value);
        }
    }
}
