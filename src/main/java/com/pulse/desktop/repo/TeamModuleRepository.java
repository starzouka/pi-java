package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TeamModuleRepository {
    private static final Set<String> TEAM_SORTS = Set.of("latest", "oldest", "name", "region", "popular");
    private static final Set<String> ADMIN_TEAM_SORTS = Set.of("id", "name", "region", "captain", "members", "products", "created_at");
    private static final Set<String> DIRECTIONS = Set.of("asc", "desc");
    private static final Set<String> REQUEST_STATUSES = Set.of("PENDING", "ACCEPTED", "REFUSED", "CANCELLED");
    private static final Set<String> INVITE_STATUSES = Set.of("PENDING", "ACCEPTED", "REFUSED", "CANCELLED");
    private static final Set<String> ROSTER_ROLES = Set.of("CAPTAIN", "CO_CAPTAIN", "STARTER", "SUBSTITUTE");

    public record TeamCatalogFilter(
            String q,
            String region,
            String sort,
            boolean withProducts,
            boolean activeTournaments
    ) {
        public static TeamCatalogFilter defaults() {
            return new TeamCatalogFilter("", "", "latest", false, false);
        }
    }

    public record TeamCatalogRow(
            int teamId,
            String name,
            String description,
            String region,
            int captainUserId,
            String captainName,
            String logoPath,
            LocalDateTime createdAt,
            int membersCount,
            int productsCount,
            int activeTournamentsCount
    ) {
    }

    public record CaptainTeamRow(
            int teamId,
            String name,
            String description,
            String region,
            int captainUserId,
            String captainName,
            String logoPath,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record TeamStats(int members, int products, int tournaments) {
    }

    public record MemberRow(
            int userId,
            String username,
            String displayName,
            String accountRole,
            String profileImagePath,
            LocalDateTime joinedAt,
            boolean active,
            String rosterRole,
            LocalDateTime leftAt
    ) {
    }

    public record RosterDistribution(int captain, int coCaptain, int starter, int substitute) {
    }

    public record JoinRequestRow(
            int requestId,
            int userId,
            String username,
            String displayName,
            String profileImagePath,
            String status,
            String note,
            LocalDateTime createdAt,
            LocalDateTime respondedAt
    ) {
    }

    public record InviteCandidateRow(
            int userId,
            String username,
            String displayName,
            String role,
            String country,
            String profileImagePath
    ) {
    }

    public record InviteRow(
            int inviteId,
            int invitedUserId,
            String username,
            String displayName,
            String profileImagePath,
            String status,
            String message,
            LocalDateTime createdAt,
            LocalDateTime respondedAt
    ) {
    }

    public record AdminTeamFilter(
            String q,
            String region,
            String captain,
            String withProducts,
            String sort,
            String direction
    ) {
        public static AdminTeamFilter defaults() {
            return new AdminTeamFilter("", "", "", "", "created_at", "desc");
        }
    }

    public record AdminTeamRow(
            int teamId,
            String name,
            String description,
            String region,
            Integer logoImageId,
            String logoPath,
            int captainUserId,
            String captainUsername,
            String captainDisplayName,
            String captainEmail,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int membersCount,
            int productsCount
    ) {
        public String captainLabel() {
            String base = displayOrUsername(captainDisplayName, captainUsername);
            return base + " (" + nullSafe(captainEmail, "-") + ")";
        }
    }

    public record TeamMutationInput(
            Integer teamId,
            String name,
            String description,
            String region,
            Integer captainUserId,
            Integer logoImageId
    ) {
    }

    public record ImageCreateInput(
            int uploadedByUserId,
            String fileUrl,
            String mimeType,
            long sizeBytes,
            Integer width,
            Integer height,
            String altText
    ) {
    }

    public record OperationResult(boolean ok, String message, Integer entityId) {
        public static OperationResult ok(String message) {
            return new OperationResult(true, message, null);
        }

        public static OperationResult ok(String message, Integer entityId) {
            return new OperationResult(true, message, entityId);
        }

        public static OperationResult error(String message) {
            return new OperationResult(false, message, null);
        }
    }

    public List<String> listRegions() throws SQLException {
        String sql = """
                SELECT DISTINCT region
                FROM teams
                WHERE region IS NOT NULL AND region <> ''
                ORDER BY region ASC
                """;

        List<String> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String value = normalizeText(rs.getString("region"));
                if (!value.isBlank()) {
                    rows.add(value);
                }
            }
        }
        return rows;
    }

    public List<CaptainTeamRow> listCaptainTeams(int captainUserId, int limit) throws SQLException {
        if (captainUserId <= 0) {
            return List.of();
        }

        String sql = """
                SELECT
                    t.team_id,
                    t.name,
                    t.description,
                    t.region,
                    t.captain_user_id,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS captain_name,
                    i.file_url AS logo_path,
                    t.created_at,
                    t.updated_at
                FROM teams t
                INNER JOIN users u ON u.user_id = t.captain_user_id
                LEFT JOIN images i ON i.image_id = t.logo_image_id
                WHERE t.captain_user_id = ?
                ORDER BY t.name ASC
                LIMIT ?
                """;

        List<CaptainTeamRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, captainUserId);
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapCaptainTeamRow(rs));
                }
            }
        }
        return rows;
    }

    public CaptainTeamRow loadCaptainTeam(int captainUserId, int teamId) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0) {
            return null;
        }

        String sql = """
                SELECT
                    t.team_id,
                    t.name,
                    t.description,
                    t.region,
                    t.captain_user_id,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS captain_name,
                    i.file_url AS logo_path,
                    t.created_at,
                    t.updated_at
                FROM teams t
                INNER JOIN users u ON u.user_id = t.captain_user_id
                LEFT JOIN images i ON i.image_id = t.logo_image_id
                WHERE t.team_id = ? AND t.captain_user_id = ?
                LIMIT 1
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            statement.setInt(2, captainUserId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapCaptainTeamRow(rs);
            }
        }
    }

    public CaptainTeamRow resolveCaptainActiveTeam(int captainUserId, Integer requestedTeamId) throws SQLException {
        if (captainUserId <= 0) {
            return null;
        }

        Integer requested = positiveOrNull(requestedTeamId);
        if (requested != null) {
            CaptainTeamRow explicit = loadCaptainTeam(captainUserId, requested);
            if (explicit != null) {
                return explicit;
            }
        }

        List<CaptainTeamRow> teams = listCaptainTeams(captainUserId, 1);
        return teams.isEmpty() ? null : teams.get(0);
    }

    public TeamStats loadTeamStats(int teamId) throws SQLException {
        if (teamId <= 0) {
            return new TeamStats(0, 0, 0);
        }

        String sql = """
                SELECT
                    (
                        SELECT COUNT(*)
                        FROM team_members tm
                        WHERE tm.team_id = ?
                          AND tm.is_active = 1
                          AND tm.left_at IS NULL
                    ) AS members_count,
                    (
                        SELECT COUNT(*)
                        FROM products p
                        WHERE p.team_id = ?
                    ) AS products_count,
                    (
                        SELECT COUNT(*)
                        FROM tournament_teams tt
                        WHERE tt.team_id = ?
                    ) AS tournaments_count
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            statement.setInt(2, teamId);
            statement.setInt(3, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return new TeamStats(0, 0, 0);
                }
                return new TeamStats(
                        rs.getInt("members_count"),
                        rs.getInt("products_count"),
                        rs.getInt("tournaments_count")
                );
            }
        }
    }

    public OperationResult createCaptainTeam(int captainUserId, TeamMutationInput input) throws SQLException {
        if (captainUserId <= 0) {
            return OperationResult.error("Session capitaine invalide.");
        }

        String name = normalizeText(input == null ? null : input.name());
        if (name.isBlank()) {
            return OperationResult.error("Le nom de l'equipe est obligatoire.");
        }

        String region = nullIfBlank(input == null ? null : input.region());
        String description = nullIfBlank(input == null ? null : input.description());
        Integer logoImageId = positiveOrNull(input == null ? null : input.logoImageId());

        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (teamNameExists(connection, name, null)) {
                    connection.rollback();
                    return OperationResult.error("Une equipe avec ce nom existe deja.");
                }

                String sql = """
                        INSERT INTO teams (
                            name, description, region, logo_image_id, captain_user_id, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                        """;

                int teamId;
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, name);
                    statement.setString(2, description);
                    statement.setString(3, region);
                    setNullableInt(statement, 4, logoImageId);
                    statement.setInt(5, captainUserId);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Insertion equipe echouee: team_id manquant.");
                        }
                        teamId = keys.getInt(1);
                    }
                }

                ensureCaptainMembership(connection, teamId, captainUserId);
                connection.commit();
                return OperationResult.ok("L'equipe a ete creee.", teamId);
            } catch (SQLException ex) {
                connection.rollback();
                if (isDuplicateEntry(ex)) {
                    return OperationResult.error("Une equipe avec ce nom existe deja.");
                }
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public OperationResult updateCaptainTeam(int captainUserId, TeamMutationInput input) throws SQLException {
        if (captainUserId <= 0) {
            return OperationResult.error("Session capitaine invalide.");
        }

        Integer teamId = positiveOrNull(input == null ? null : input.teamId());
        if (teamId == null) {
            return OperationResult.error("Equipe introuvable.");
        }

        String name = normalizeText(input == null ? null : input.name());
        if (name.isBlank()) {
            return OperationResult.error("Le nom de l'equipe est obligatoire.");
        }

        String region = nullIfBlank(input == null ? null : input.region());
        String description = nullIfBlank(input == null ? null : input.description());
        Integer newLogoImageId = positiveOrNull(input == null ? null : input.logoImageId());

        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Integer ownerId = fetchCaptainUserId(connection, teamId);
                if (ownerId == null) {
                    connection.rollback();
                    return OperationResult.error("Equipe introuvable.");
                }
                if (ownerId != captainUserId) {
                    connection.rollback();
                    return OperationResult.error("Equipe non autorisee.");
                }

                if (teamNameExists(connection, name, teamId)) {
                    connection.rollback();
                    return OperationResult.error("Une equipe avec ce nom existe deja.");
                }

                if (newLogoImageId == null) {
                    String sql = """
                            UPDATE teams
                            SET name = ?,
                                description = ?,
                                region = ?,
                                updated_at = NOW()
                            WHERE team_id = ? AND captain_user_id = ?
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, name);
                        statement.setString(2, description);
                        statement.setString(3, region);
                        statement.setInt(4, teamId);
                        statement.setInt(5, captainUserId);
                        int updated = statement.executeUpdate();
                        if (updated <= 0) {
                            connection.rollback();
                            return OperationResult.error("Mise a jour impossible.");
                        }
                    }
                } else {
                    String sql = """
                            UPDATE teams
                            SET name = ?,
                                description = ?,
                                region = ?,
                                logo_image_id = ?,
                                updated_at = NOW()
                            WHERE team_id = ? AND captain_user_id = ?
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, name);
                        statement.setString(2, description);
                        statement.setString(3, region);
                        setNullableInt(statement, 4, newLogoImageId);
                        statement.setInt(5, teamId);
                        statement.setInt(6, captainUserId);
                        int updated = statement.executeUpdate();
                        if (updated <= 0) {
                            connection.rollback();
                            return OperationResult.error("Mise a jour impossible.");
                        }
                    }
                }

                ensureCaptainMembership(connection, teamId, captainUserId);
                connection.commit();
                return OperationResult.ok("L'equipe a ete mise a jour.", teamId);
            } catch (SQLException ex) {
                connection.rollback();
                if (isDuplicateEntry(ex)) {
                    return OperationResult.error("Une equipe avec ce nom existe deja.");
                }
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public List<MemberRow> listTeamMembers(int captainUserId, int teamId, boolean activeOnly) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0) {
            return List.of();
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("""
                SELECT
                    tm.user_id,
                    u.username,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS display_name,
                    u.role AS account_role,
                    i.file_url AS profile_image_path,
                    tm.joined_at,
                    tm.is_active,
                    tm.roster_role,
                    tm.left_at
                FROM team_members tm
                INNER JOIN users u ON u.user_id = tm.user_id
                LEFT JOIN images i ON i.image_id = u.profile_image_id
                WHERE tm.team_id = ?
                """);

        if (activeOnly) {
            sql.append(" AND tm.is_active = 1 AND tm.left_at IS NULL ");
            sql.append("""
                    ORDER BY
                        CASE
                            WHEN tm.roster_role = 'CAPTAIN' THEN 0
                            WHEN tm.roster_role = 'CO_CAPTAIN' THEN 1
                            WHEN tm.roster_role = 'STARTER' THEN 2
                            WHEN tm.roster_role = 'SUBSTITUTE' THEN 3
                            ELSE 4
                        END ASC,
                        tm.joined_at ASC
                    """);
        } else {
            sql.append(" AND (tm.is_active = 0 OR tm.left_at IS NOT NULL) ");
            sql.append(" ORDER BY tm.left_at DESC, tm.joined_at DESC ");
        }

        List<MemberRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setInt(1, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new MemberRow(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("account_role"),
                            rs.getString("profile_image_path"),
                            toLocalDateTime(rs.getTimestamp("joined_at")),
                            rs.getBoolean("is_active"),
                            normalizeRosterRole(rs.getString("roster_role")),
                            toLocalDateTime(rs.getTimestamp("left_at"))
                    ));
                }
            }
        }

        return rows;
    }

    public RosterDistribution loadRosterDistribution(int captainUserId, int teamId) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0) {
            return new RosterDistribution(0, 0, 0, 0);
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return new RosterDistribution(0, 0, 0, 0);
        }

        String sql = """
                SELECT tm.roster_role, COUNT(*) AS members_count
                FROM team_members tm
                WHERE tm.team_id = ?
                  AND tm.is_active = 1
                  AND tm.left_at IS NULL
                GROUP BY tm.roster_role
                """;

        int captain = 0;
        int coCaptain = 0;
        int starter = 0;
        int substitute = 0;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String role = normalizeRosterRole(rs.getString("roster_role"));
                    int count = rs.getInt("members_count");
                    switch (role) {
                        case "CAPTAIN" -> captain = count;
                        case "CO_CAPTAIN" -> coCaptain = count;
                        case "STARTER" -> starter = count;
                        case "SUBSTITUTE" -> substitute = count;
                        default -> {
                        }
                    }
                }
            }
        }

        return new RosterDistribution(captain, coCaptain, starter, substitute);
    }

    public OperationResult updateRosterRole(int captainUserId, int teamId, int memberUserId, String requestedRole) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0 || memberUserId <= 0) {
            return OperationResult.error("Parametres invalides.");
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return OperationResult.error("Equipe non autorisee.");
        }

        String role = normalizeRosterRole(requestedRole);
        if (!ROSTER_ROLES.contains(role)) {
            return OperationResult.error("Role roster invalide.");
        }

        try (Connection connection = Jdbc.open()) {
            Integer mainCaptainId = fetchCaptainUserId(connection, teamId);
            if (mainCaptainId == null) {
                return OperationResult.error("Equipe introuvable.");
            }

            MembershipSnapshot membership = loadMembership(connection, teamId, memberUserId);
            if (!membership.exists()) {
                return OperationResult.error("Ce joueur n'est pas membre de cette equipe.");
            }
            if (!membership.active() || membership.leftAt() != null) {
                return OperationResult.error("Le role roster ne peut etre modifie que pour un membre actif.");
            }

            if (memberUserId == mainCaptainId) {
                role = "CAPTAIN";
            } else if ("CAPTAIN".equals(role)) {
                return OperationResult.error("Seul le capitaine principal peut avoir le role CAPTAIN.");
            }

            String updateSql = """
                    UPDATE team_members
                    SET roster_role = ?
                    WHERE team_id = ? AND user_id = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setString(1, role);
                statement.setInt(2, teamId);
                statement.setInt(3, memberUserId);
                statement.executeUpdate();
            }

            return OperationResult.ok("Le role roster a ete mis a jour.");
        }
    }

    public OperationResult deactivateMember(int captainUserId, int teamId, int memberUserId) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0 || memberUserId <= 0) {
            return OperationResult.error("Parametres invalides.");
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return OperationResult.error("Equipe non autorisee.");
        }

        try (Connection connection = Jdbc.open()) {
            Integer mainCaptainId = fetchCaptainUserId(connection, teamId);
            if (mainCaptainId != null && memberUserId == mainCaptainId) {
                return OperationResult.error("Le capitaine ne peut pas se retirer lui-meme depuis cette page.");
            }

            MembershipSnapshot membership = loadMembership(connection, teamId, memberUserId);
            if (!membership.exists()) {
                return OperationResult.error("Ce joueur n'est pas membre de cette equipe.");
            }
            if (!membership.active() || membership.leftAt() != null) {
                return OperationResult.error("Ce membre est deja inactif.");
            }

            String sql = """
                    UPDATE team_members
                    SET is_active = 0,
                        left_at = NOW()
                    WHERE team_id = ? AND user_id = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, teamId);
                statement.setInt(2, memberUserId);
                statement.executeUpdate();
            }

            return OperationResult.ok("Le membre a ete retire de la liste active.");
        }
    }

    public OperationResult reactivateMember(int captainUserId, int teamId, int memberUserId) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0 || memberUserId <= 0) {
            return OperationResult.error("Parametres invalides.");
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return OperationResult.error("Equipe non autorisee.");
        }

        try (Connection connection = Jdbc.open()) {
            Integer mainCaptainId = fetchCaptainUserId(connection, teamId);
            MembershipSnapshot membership = loadMembership(connection, teamId, memberUserId);
            if (!membership.exists()) {
                return OperationResult.error("Ce joueur n'est pas membre de cette equipe.");
            }
            if (membership.active() && membership.leftAt() == null) {
                return OperationResult.error("Ce membre est deja actif.");
            }

            String sql = """
                    UPDATE team_members
                    SET is_active = 1,
                        left_at = NULL,
                        roster_role = CASE WHEN ? THEN 'CAPTAIN' ELSE COALESCE(NULLIF(roster_role, ''), 'SUBSTITUTE') END
                    WHERE team_id = ? AND user_id = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBoolean(1, mainCaptainId != null && memberUserId == mainCaptainId);
                statement.setInt(2, teamId);
                statement.setInt(3, memberUserId);
                statement.executeUpdate();
            }

            return OperationResult.ok("Le membre a ete reactive.");
        }
    }

    public OperationResult removeInactiveMember(int captainUserId, int teamId, int memberUserId) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0 || memberUserId <= 0) {
            return OperationResult.error("Parametres invalides.");
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return OperationResult.error("Equipe non autorisee.");
        }

        try (Connection connection = Jdbc.open()) {
            Integer mainCaptainId = fetchCaptainUserId(connection, teamId);
            if (mainCaptainId != null && memberUserId == mainCaptainId) {
                return OperationResult.error("Le capitaine ne peut pas etre retire de l'historique.");
            }

            MembershipSnapshot membership = loadMembership(connection, teamId, memberUserId);
            if (!membership.exists()) {
                return OperationResult.error("Ce joueur n'est pas membre de cette equipe.");
            }
            if (membership.active() && membership.leftAt() == null) {
                return OperationResult.error("Ce membre est actif. Utilisez Retirer dans la liste active.");
            }

            String sql = "DELETE FROM team_members WHERE team_id = ? AND user_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, teamId);
                statement.setInt(2, memberUserId);
                int deleted = statement.executeUpdate();
                if (deleted <= 0) {
                    return OperationResult.error("Suppression impossible.");
                }
            }

            return OperationResult.ok("L'ancien membre a ete retire de l'historique.");
        }
    }

    public List<JoinRequestRow> listJoinRequests(int captainUserId, int teamId, String statusFilter, int limit) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0) {
            return List.of();
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return List.of();
        }

        String status = normalizeRequestStatus(statusFilter);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    r.request_id,
                    r.user_id,
                    u.username,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS display_name,
                    i.file_url AS profile_image_path,
                    r.status,
                    r.note,
                    r.created_at,
                    r.responded_at
                FROM team_join_requests r
                INNER JOIN users u ON u.user_id = r.user_id
                LEFT JOIN images i ON i.image_id = u.profile_image_id
                WHERE r.team_id = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(teamId);

        if (!status.isBlank()) {
            sql.append(" AND r.status = ? ");
            params.add(status);
        }

        sql.append(" ORDER BY r.created_at DESC, r.request_id DESC LIMIT ? ");
        params.add(Math.max(1, limit));

        List<JoinRequestRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new JoinRequestRow(
                            rs.getInt("request_id"),
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("profile_image_path"),
                            rs.getString("status"),
                            rs.getString("note"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("responded_at"))
                    ));
                }
            }
        }

        return rows;
    }

    public OperationResult respondJoinRequest(int captainUserId, int teamId, int requestId, String decisionRaw) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0 || requestId <= 0) {
            return OperationResult.error("Parametres invalides.");
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return OperationResult.error("Equipe non autorisee.");
        }

        String decision = normalizeDecision(decisionRaw);
        if (decision.isBlank()) {
            return OperationResult.error("Decision invalide.");
        }

        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Integer applicantUserId = null;
                String findSql = """
                        SELECT user_id
                        FROM team_join_requests
                        WHERE request_id = ? AND team_id = ? AND status = 'PENDING'
                        LIMIT 1
                        FOR UPDATE
                        """;
                try (PreparedStatement statement = connection.prepareStatement(findSql)) {
                    statement.setInt(1, requestId);
                    statement.setInt(2, teamId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            applicantUserId = rs.getInt("user_id");
                        }
                    }
                }

                if (applicantUserId == null || applicantUserId <= 0) {
                    connection.rollback();
                    return OperationResult.error("Demande introuvable ou deja traitee.");
                }

                String updateRequestSql = """
                        UPDATE team_join_requests
                        SET status = ?,
                            responded_at = NOW(),
                            responded_by_captain_id = ?
                        WHERE request_id = ?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(updateRequestSql)) {
                    statement.setString(1, decision);
                    statement.setInt(2, captainUserId);
                    statement.setInt(3, requestId);
                    statement.executeUpdate();
                }

                if ("ACCEPTED".equals(decision)) {
                    MembershipSnapshot membership = loadMembership(connection, teamId, applicantUserId);
                    String roleToApply = applicantUserId == captainUserId ? "CAPTAIN" : "SUBSTITUTE";

                    if (membership.exists()) {
                        String existingRole = normalizeRosterRole(membership.rosterRole());
                        String nextRole;
                        if ("CAPTAIN".equals(roleToApply)) {
                            nextRole = "CAPTAIN";
                        } else if (existingRole.isBlank() || !ROSTER_ROLES.contains(existingRole)) {
                            nextRole = "SUBSTITUTE";
                        } else {
                            nextRole = existingRole;
                        }

                        String updateMemberSql = """
                                UPDATE team_members
                                SET is_active = 1,
                                    left_at = NULL,
                                    roster_role = ?
                                WHERE team_id = ? AND user_id = ?
                                """;
                        try (PreparedStatement statement = connection.prepareStatement(updateMemberSql)) {
                            statement.setString(1, nextRole);
                            statement.setInt(2, teamId);
                            statement.setInt(3, applicantUserId);
                            statement.executeUpdate();
                        }
                    } else {
                        String insertMemberSql = """
                                INSERT INTO team_members (
                                    team_id, user_id, joined_at, is_active, roster_role, left_at
                                ) VALUES (?, ?, NOW(), 1, ?, NULL)
                                """;
                        try (PreparedStatement statement = connection.prepareStatement(insertMemberSql)) {
                            statement.setInt(1, teamId);
                            statement.setInt(2, applicantUserId);
                            statement.setString(3, roleToApply);
                            statement.executeUpdate();
                        }
                    }
                }

                connection.commit();
                if ("ACCEPTED".equals(decision)) {
                    return OperationResult.ok("La demande a ete acceptee.");
                }
                return OperationResult.ok("La demande a ete refusee.");
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public List<InviteCandidateRow> searchInviteCandidates(int captainUserId, int teamId, String query, int limit) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0) {
            return List.of();
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return List.of();
        }

        String q = normalizeText(query);
        if (q.isBlank()) {
            return List.of();
        }

        String sql = """
                SELECT
                    u.user_id,
                    u.username,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS display_name,
                    u.role,
                    u.country,
                    i.file_url AS profile_image_path
                FROM users u
                LEFT JOIN images i ON i.image_id = u.profile_image_id
                WHERE u.user_id <> ?
                  AND u.is_active = 1
                  AND (
                      LOWER(u.username) LIKE ?
                      OR LOWER(COALESCE(u.display_name, '')) LIKE ?
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM team_members tm
                      WHERE tm.team_id = ?
                        AND tm.user_id = u.user_id
                        AND tm.is_active = 1
                        AND tm.left_at IS NULL
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM team_invites ti
                      WHERE ti.team_id = ?
                        AND ti.invited_user_id = u.user_id
                        AND ti.status = 'PENDING'
                  )
                ORDER BY COALESCE(NULLIF(u.display_name, ''), u.username) ASC, u.username ASC
                LIMIT ?
                """;

        List<InviteCandidateRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            statement.setInt(1, captainUserId);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setInt(4, teamId);
            statement.setInt(5, teamId);
            statement.setInt(6, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new InviteCandidateRow(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("role"),
                            rs.getString("country"),
                            rs.getString("profile_image_path")
                    ));
                }
            }
        }

        return rows;
    }

    public List<InviteRow> listLatestInvites(int captainUserId, int teamId, int limit) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0) {
            return List.of();
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return List.of();
        }

        String sql = """
                SELECT
                    ti.invite_id,
                    ti.invited_user_id,
                    u.username,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS display_name,
                    i.file_url AS profile_image_path,
                    ti.status,
                    ti.message,
                    ti.created_at,
                    ti.responded_at
                FROM team_invites ti
                INNER JOIN users u ON u.user_id = ti.invited_user_id
                LEFT JOIN images i ON i.image_id = u.profile_image_id
                WHERE ti.team_id = ?
                ORDER BY ti.created_at DESC, ti.invite_id DESC
                LIMIT ?
                """;

        List<InviteRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new InviteRow(
                            rs.getInt("invite_id"),
                            rs.getInt("invited_user_id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("profile_image_path"),
                            rs.getString("status"),
                            rs.getString("message"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("responded_at"))
                    ));
                }
            }
        }

        return rows;
    }

    public OperationResult sendInvite(int captainUserId, int teamId, int invitedUserId, String rawMessage) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0 || invitedUserId <= 0) {
            return OperationResult.error("Parametres invalides.");
        }
        if (!isTeamOwnedByCaptain(captainUserId, teamId)) {
            return OperationResult.error("Equipe non autorisee.");
        }
        if (invitedUserId == captainUserId) {
            return OperationResult.error("Vous ne pouvez pas vous inviter vous-meme.");
        }

        String message = nullIfBlank(rawMessage);
        if (message != null && message.length() > 255) {
            message = message.substring(0, 255);
        }

        try (Connection connection = Jdbc.open()) {
            if (!userExists(connection, invitedUserId)) {
                return OperationResult.error("Utilisateur introuvable.");
            }
            if (isUserActiveMember(connection, teamId, invitedUserId)) {
                return OperationResult.error("Ce joueur est deja membre de cette equipe.");
            }
            if (hasPendingInvite(connection, teamId, invitedUserId)) {
                return OperationResult.error("Une invitation en attente existe deja pour ce joueur.");
            }

            String sql = """
                    INSERT INTO team_invites (
                        status, message, created_at, responded_at, team_id, invited_user_id, invited_by_user_id
                    ) VALUES ('PENDING', ?, NOW(), NULL, ?, ?, ?)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, message);
                statement.setInt(2, teamId);
                statement.setInt(3, invitedUserId);
                statement.setInt(4, captainUserId);
                statement.executeUpdate();
            }

            return OperationResult.ok("Invitation envoyee.");
        }
    }

    public String buildInviteSuggestion(CaptainTeamRow team, InviteCandidateRow candidate) {
        String teamName = team == null ? "notre equipe" : normalizeText(team.name());
        if (teamName.isBlank()) {
            teamName = "notre equipe";
        }

        String candidateName = candidate == null ? "joueur" : normalizeText(candidate.displayName());
        if (candidateName.isBlank()) {
            candidateName = candidate == null ? "joueur" : normalizeText(candidate.username());
        }
        if (candidateName.isBlank()) {
            candidateName = "joueur";
        }

        return "Salut " + candidateName
                + ", nous suivons ton profil et nous aimerions te proposer une place dans "
                + teamName
                + ". Si tu es partant, on serait ravis d'en discuter.";
    }

    public List<LookupItem> listCaptainsForAdmin() throws SQLException {
        String sql = """
                SELECT
                    u.user_id AS id,
                    CONCAT(u.username, ' (', u.email, ')') AS label
                FROM users u
                WHERE u.is_active = 1
                ORDER BY u.username ASC
                LIMIT 1000
                """;

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

    public List<AdminTeamRow> searchAdminTeams(AdminTeamFilter rawFilter, int limit) throws SQLException {
        AdminTeamFilter filter = normalizeAdminFilter(rawFilter);

        StringBuilder sql = new StringBuilder(baseAdminTeamSelectSql());
        List<Object> params = new ArrayList<>();

        if (!filter.q().isBlank()) {
            sql.append("""
                     AND (
                        LOWER(t.name) LIKE ?
                        OR LOWER(COALESCE(t.description, '')) LIKE ?
                        OR LOWER(COALESCE(t.region, '')) LIKE ?
                        OR LOWER(cu.username) LIKE ?
                        OR LOWER(COALESCE(cu.display_name, '')) LIKE ?
                        OR LOWER(COALESCE(cu.email, '')) LIKE ?
                     )
                    """);
            String like = "%" + filter.q().toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (!filter.region().isBlank()) {
            sql.append(" AND LOWER(COALESCE(t.region, '')) LIKE ? ");
            params.add("%" + filter.region().toLowerCase(Locale.ROOT) + "%");
        }

        if (!filter.captain().isBlank()) {
            sql.append("""
                     AND (
                        LOWER(cu.username) LIKE ?
                        OR LOWER(COALESCE(cu.display_name, '')) LIKE ?
                        OR LOWER(COALESCE(cu.email, '')) LIKE ?
                     )
                    """);
            String like = "%" + filter.captain().toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if ("1".equals(filter.withProducts())) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM products p_filter
                         WHERE p_filter.team_id = t.team_id
                     )
                    """);
        } else if ("0".equals(filter.withProducts())) {
            sql.append("""
                     AND NOT EXISTS (
                         SELECT 1
                         FROM products p_filter
                         WHERE p_filter.team_id = t.team_id
                     )
                    """);
        }

        String sortExpr = switch (filter.sort()) {
            case "id" -> "t.team_id";
            case "name" -> "t.name";
            case "region" -> "COALESCE(t.region, '')";
            case "captain" -> "cu.username";
            case "members" -> "members_count";
            case "products" -> "products_count";
            default -> "t.created_at";
        };

        String direction = filter.direction();
        sql.append(" ORDER BY ").append(sortExpr).append(" ").append(direction.toUpperCase(Locale.ROOT));
        if (!"t.team_id".equals(sortExpr)) {
            sql.append(", t.team_id DESC");
        }

        sql.append(" LIMIT ? ");
        params.add(Math.max(1, limit));

        List<AdminTeamRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapAdminTeamRow(rs));
                }
            }
        }

        return rows;
    }

    public AdminTeamRow loadAdminTeamById(int teamId) throws SQLException {
        if (teamId <= 0) {
            return null;
        }

        String sql = baseAdminTeamSelectSql() + " AND t.team_id = ? LIMIT 1";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapAdminTeamRow(rs);
            }
        }
    }

    public OperationResult upsertAdminTeam(TeamMutationInput rawInput) throws SQLException {
        TeamMutationInput input = rawInput == null
                ? new TeamMutationInput(null, "", "", "", null, null)
                : rawInput;

        Integer teamId = positiveOrNull(input.teamId());
        Integer captainUserId = positiveOrNull(input.captainUserId());
        Integer logoImageId = positiveOrNull(input.logoImageId());
        String name = normalizeText(input.name());
        String description = nullIfBlank(input.description());
        String region = nullIfBlank(input.region());

        if (name.isBlank()) {
            return OperationResult.error("Le nom de l'equipe est obligatoire.");
        }
        if (captainUserId == null) {
            return OperationResult.error("Capitaine invalide.");
        }

        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (!userExists(connection, captainUserId)) {
                    connection.rollback();
                    return OperationResult.error("Capitaine invalide.");
                }

                if (teamNameExists(connection, name, teamId)) {
                    connection.rollback();
                    return OperationResult.error("Une equipe avec ce nom existe deja.");
                }

                int effectiveTeamId;
                if (teamId == null) {
                    String insertSql = """
                            INSERT INTO teams (
                                name, description, region, logo_image_id, captain_user_id, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setString(1, name);
                        statement.setString(2, description);
                        statement.setString(3, region);
                        setNullableInt(statement, 4, logoImageId);
                        statement.setInt(5, captainUserId);
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("Insertion equipe echouee: team_id manquant.");
                            }
                            effectiveTeamId = keys.getInt(1);
                        }
                    }
                } else {
                    if (!teamExists(connection, teamId)) {
                        connection.rollback();
                        return OperationResult.error("Equipe introuvable.");
                    }

                    if (logoImageId == null) {
                        String updateSql = """
                                UPDATE teams
                                SET name = ?,
                                    description = ?,
                                    region = ?,
                                    captain_user_id = ?,
                                    updated_at = NOW()
                                WHERE team_id = ?
                                """;
                        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                            statement.setString(1, name);
                            statement.setString(2, description);
                            statement.setString(3, region);
                            statement.setInt(4, captainUserId);
                            statement.setInt(5, teamId);
                            statement.executeUpdate();
                        }
                    } else {
                        String updateSql = """
                                UPDATE teams
                                SET name = ?,
                                    description = ?,
                                    region = ?,
                                    logo_image_id = ?,
                                    captain_user_id = ?,
                                    updated_at = NOW()
                                WHERE team_id = ?
                                """;
                        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                            statement.setString(1, name);
                            statement.setString(2, description);
                            statement.setString(3, region);
                            setNullableInt(statement, 4, logoImageId);
                            statement.setInt(5, captainUserId);
                            statement.setInt(6, teamId);
                            statement.executeUpdate();
                        }
                    }
                    effectiveTeamId = teamId;
                }

                ensureCaptainMembership(connection, effectiveTeamId, captainUserId);
                connection.commit();
                if (teamId == null) {
                    return OperationResult.ok("Equipe creee.", effectiveTeamId);
                }
                return OperationResult.ok("Equipe mise a jour.", effectiveTeamId);
            } catch (SQLException ex) {
                connection.rollback();
                if (isDuplicateEntry(ex)) {
                    return OperationResult.error("Enregistrement impossible (nom deja utilise ou liaison invalide).");
                }
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public OperationResult deleteAdminTeam(int teamId) throws SQLException {
        if (teamId <= 0) {
            return OperationResult.error("Equipe introuvable.");
        }

        String sql = "DELETE FROM teams WHERE team_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            int deleted = statement.executeUpdate();
            if (deleted <= 0) {
                return OperationResult.error("Equipe introuvable.");
            }
            return OperationResult.ok("Equipe supprimee.");
        } catch (SQLException ex) {
            if (isConstraintViolation(ex)) {
                return OperationResult.error("Suppression impossible (liaisons existantes).");
            }
            throw ex;
        }
    }

    public Integer createImageRecord(ImageCreateInput input) throws SQLException {
        if (input == null || normalizeText(input.fileUrl()).isBlank()) {
            return null;
        }

        String sql = """
                INSERT INTO images (
                    file_url,
                    mime_type,
                    size_bytes,
                    width,
                    height,
                    alt_text,
                    uploaded_by_user_id,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, normalizeText(input.fileUrl()));
            statement.setString(2, nullIfBlank(input.mimeType()) == null ? "application/octet-stream" : normalizeText(input.mimeType()));
            statement.setLong(3, Math.max(0L, input.sizeBytes()));
            if (input.width() == null || input.width() <= 0) {
                statement.setObject(4, null);
            } else {
                statement.setInt(4, input.width());
            }
            if (input.height() == null || input.height() <= 0) {
                statement.setObject(5, null);
            } else {
                statement.setInt(5, input.height());
            }
            statement.setString(6, nullIfBlank(input.altText()));
            if (input.uploadedByUserId() > 0) {
                statement.setInt(7, input.uploadedByUserId());
            } else {
                statement.setObject(7, null);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return null;
    }

    public List<TeamCatalogRow> searchTeamsCatalog(TeamCatalogFilter rawFilter, int limit) throws SQLException {
        TeamCatalogFilter filter = normalizeTeamFilter(rawFilter);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    t.team_id,
                    t.name,
                    t.description,
                    t.region,
                    t.captain_user_id,
                    COALESCE(NULLIF(u.display_name, ''), u.username) AS captain_name,
                    i.file_url AS logo_path,
                    t.created_at,
                    (
                        SELECT COUNT(*)
                        FROM team_members tm
                        WHERE tm.team_id = t.team_id
                          AND tm.is_active = 1
                          AND tm.left_at IS NULL
                    ) AS members_count,
                    (
                        SELECT COUNT(*)
                        FROM products p
                        WHERE p.team_id = t.team_id
                          AND p.is_active = 1
                    ) AS products_count,
                    (
                        SELECT COUNT(*)
                        FROM tournament_teams tt
                        INNER JOIN tournaments tr ON tr.tournament_id = tt.tournament_id
                        WHERE tt.team_id = t.team_id
                          AND tt.status IN ('PENDING', 'ACCEPTED')
                          AND tr.status IN ('OPEN', 'ONGOING')
                    ) AS active_tournaments_count
                FROM teams t
                INNER JOIN users u ON u.user_id = t.captain_user_id
                LEFT JOIN images i ON i.image_id = t.logo_image_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (!filter.q().isBlank()) {
            sql.append("""
                     AND (
                        LOWER(t.name) LIKE ?
                        OR LOWER(COALESCE(t.description, '')) LIKE ?
                        OR LOWER(COALESCE(t.region, '')) LIKE ?
                        OR LOWER(COALESCE(u.username, '')) LIKE ?
                        OR LOWER(COALESCE(u.display_name, '')) LIKE ?
                     )
                    """);
            String like = "%" + filter.q().toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (!filter.region().isBlank()) {
            sql.append(" AND LOWER(COALESCE(t.region, '')) = ? ");
            params.add(filter.region().toLowerCase(Locale.ROOT));
        }

        if (filter.withProducts()) {
            sql.append("""
                     AND EXISTS (
                        SELECT 1
                        FROM products p_filter
                        WHERE p_filter.team_id = t.team_id
                          AND p_filter.is_active = 1
                     )
                    """);
        }

        if (filter.activeTournaments()) {
            sql.append("""
                     AND EXISTS (
                        SELECT 1
                        FROM tournament_teams tt_filter
                        INNER JOIN tournaments tr_filter ON tr_filter.tournament_id = tt_filter.tournament_id
                        WHERE tt_filter.team_id = t.team_id
                          AND tt_filter.status IN ('PENDING', 'ACCEPTED')
                          AND tr_filter.status IN ('OPEN', 'ONGOING')
                     )
                    """);
        }

        switch (filter.sort()) {
            case "name" -> sql.append(" ORDER BY t.name ASC, t.created_at DESC ");
            case "region" -> sql.append(" ORDER BY t.region ASC, t.name ASC ");
            case "oldest" -> sql.append(" ORDER BY t.created_at ASC, t.name ASC ");
            case "popular" -> sql.append(" ORDER BY active_tournaments_count DESC, products_count DESC, members_count DESC, t.name ASC ");
            default -> sql.append(" ORDER BY t.created_at DESC, t.name ASC ");
        }

        sql.append(" LIMIT ? ");
        params.add(Math.max(1, limit));

        List<TeamCatalogRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TeamCatalogRow(
                            rs.getInt("team_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("region"),
                            rs.getInt("captain_user_id"),
                            rs.getString("captain_name"),
                            rs.getString("logo_path"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            rs.getInt("members_count"),
                            rs.getInt("products_count"),
                            rs.getInt("active_tournaments_count")
                    ));
                }
            }
        }

        return rows;
    }

    private static String baseAdminTeamSelectSql() {
        return """
                SELECT
                    t.team_id,
                    t.name,
                    t.description,
                    t.region,
                    t.logo_image_id,
                    i.file_url AS logo_path,
                    t.captain_user_id,
                    cu.username AS captain_username,
                    COALESCE(NULLIF(cu.display_name, ''), cu.username) AS captain_display_name,
                    cu.email AS captain_email,
                    t.created_at,
                    t.updated_at,
                    (
                        SELECT COUNT(*)
                        FROM team_members tm
                        WHERE tm.team_id = t.team_id
                          AND tm.is_active = 1
                          AND tm.left_at IS NULL
                    ) AS members_count,
                    (
                        SELECT COUNT(*)
                        FROM products p
                        WHERE p.team_id = t.team_id
                    ) AS products_count
                FROM teams t
                INNER JOIN users cu ON cu.user_id = t.captain_user_id
                LEFT JOIN images i ON i.image_id = t.logo_image_id
                WHERE 1=1
                """;
    }

    private static TeamCatalogFilter normalizeTeamFilter(TeamCatalogFilter raw) {
        TeamCatalogFilter source = raw == null ? TeamCatalogFilter.defaults() : raw;
        return new TeamCatalogFilter(
                normalizeText(source.q()),
                normalizeText(source.region()),
                normalizeSort(source.sort(), TEAM_SORTS, "latest"),
                source.withProducts(),
                source.activeTournaments()
        );
    }

    private static AdminTeamFilter normalizeAdminFilter(AdminTeamFilter raw) {
        AdminTeamFilter source = raw == null ? AdminTeamFilter.defaults() : raw;
        return new AdminTeamFilter(
                normalizeText(source.q()),
                normalizeText(source.region()),
                normalizeText(source.captain()),
                normalizeWithProducts(source.withProducts()),
                normalizeSort(source.sort(), ADMIN_TEAM_SORTS, "created_at"),
                normalizeDirection(source.direction())
        );
    }

    private static String normalizeWithProducts(String value) {
        String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
        if (Set.of("1", "true", "yes", "oui").contains(normalized)) {
            return "1";
        }
        if (Set.of("0", "false", "no", "non").contains(normalized)) {
            return "0";
        }
        return "";
    }

    private static String normalizeRequestStatus(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        return REQUEST_STATUSES.contains(normalized) ? normalized : "";
    }

    private static String normalizeDecision(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if ("ACCEPTED".equals(normalized) || "REFUSED".equals(normalized)) {
            return normalized;
        }
        return "";
    }

    private static String normalizeRosterRole(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if (ROSTER_ROLES.contains(normalized)) {
            return normalized;
        }
        return normalized.isBlank() ? "" : normalized;
    }

    private static String normalizeSort(String value, Set<String> allowed, String fallback) {
        String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
        if (allowed.contains(normalized)) {
            return normalized;
        }
        return fallback;
    }

    private static String normalizeDirection(String value) {
        String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
        if (DIRECTIONS.contains(normalized)) {
            return normalized;
        }
        return "desc";
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String nullIfBlank(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private static Integer positiveOrNull(Integer value) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    private static void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setObject(index, null);
        } else {
            statement.setInt(index, value);
        }
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
            if (value == null) {
                statement.setObject(index++, null);
                continue;
            }
            statement.setObject(index++, value);
        }
    }

    private static boolean isDuplicateEntry(SQLException ex) {
        String sqlState = ex.getSQLState();
        if (sqlState != null && sqlState.startsWith("23")) {
            String message = ex.getMessage();
            return message != null && message.toLowerCase(Locale.ROOT).contains("duplicate");
        }
        String message = ex.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("duplicate");
    }

    private static boolean isConstraintViolation(SQLException ex) {
        String sqlState = ex.getSQLState();
        if (sqlState != null && sqlState.startsWith("23")) {
            return true;
        }
        String message = ex.getMessage();
        return message != null
                && (message.toLowerCase(Locale.ROOT).contains("constraint")
                || message.toLowerCase(Locale.ROOT).contains("foreign key"));
    }

    private boolean isTeamOwnedByCaptain(int captainUserId, int teamId) throws SQLException {
        if (captainUserId <= 0 || teamId <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM teams WHERE team_id = ? AND captain_user_id = ? LIMIT 1";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            statement.setInt(2, captainUserId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean teamExists(Connection connection, int teamId) throws SQLException {
        String sql = "SELECT 1 FROM teams WHERE team_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Integer fetchCaptainUserId(Connection connection, int teamId) throws SQLException {
        String sql = "SELECT captain_user_id FROM teams WHERE team_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("captain_user_id");
                }
            }
        }
        return null;
    }

    private boolean teamNameExists(Connection connection, String name, Integer excludeTeamId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT 1 FROM teams WHERE LOWER(name) = ?");
        if (excludeTeamId != null) {
            sql.append(" AND team_id <> ?");
        }
        sql.append(" LIMIT 1");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, normalizeText(name).toLowerCase(Locale.ROOT));
            if (excludeTeamId != null) {
                statement.setInt(2, excludeTeamId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void ensureCaptainMembership(Connection connection, int teamId, int captainUserId) throws SQLException {
        String sql = """
                INSERT INTO team_members (
                    team_id, user_id, joined_at, is_active, roster_role, left_at
                ) VALUES (?, ?, NOW(), 1, 'CAPTAIN', NULL)
                ON DUPLICATE KEY UPDATE
                    is_active = 1,
                    left_at = NULL,
                    roster_role = 'CAPTAIN'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            statement.setInt(2, captainUserId);
            statement.executeUpdate();
        }
    }

    private MembershipSnapshot loadMembership(Connection connection, int teamId, int userId) throws SQLException {
        String sql = """
                SELECT is_active, left_at, roster_role
                FROM team_members
                WHERE team_id = ? AND user_id = ?
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            statement.setInt(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return new MembershipSnapshot(false, false, null, null);
                }
                return new MembershipSnapshot(
                        true,
                        rs.getBoolean("is_active"),
                        toLocalDateTime(rs.getTimestamp("left_at")),
                        rs.getString("roster_role")
                );
            }
        }
    }

    private boolean userExists(Connection connection, int userId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE user_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isUserActiveMember(Connection connection, int teamId, int userId) throws SQLException {
        String sql = """
                SELECT 1
                FROM team_members
                WHERE team_id = ?
                  AND user_id = ?
                  AND is_active = 1
                  AND left_at IS NULL
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            statement.setInt(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasPendingInvite(Connection connection, int teamId, int userId) throws SQLException {
        String sql = """
                SELECT 1
                FROM team_invites
                WHERE team_id = ?
                  AND invited_user_id = ?
                  AND status = 'PENDING'
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, teamId);
            statement.setInt(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static CaptainTeamRow mapCaptainTeamRow(ResultSet rs) throws SQLException {
        return new CaptainTeamRow(
                rs.getInt("team_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("region"),
                rs.getInt("captain_user_id"),
                rs.getString("captain_name"),
                rs.getString("logo_path"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private static AdminTeamRow mapAdminTeamRow(ResultSet rs) throws SQLException {
        return new AdminTeamRow(
                rs.getInt("team_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("region"),
                toInteger(rs.getObject("logo_image_id")),
                rs.getString("logo_path"),
                rs.getInt("captain_user_id"),
                rs.getString("captain_username"),
                rs.getString("captain_display_name"),
                rs.getString("captain_email"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                rs.getInt("members_count"),
                rs.getInt("products_count")
        );
    }

    private static String displayOrUsername(String displayName, String username) {
        String display = normalizeText(displayName);
        if (!display.isBlank()) {
            return display;
        }
        String login = normalizeText(username);
        return login.isBlank() ? "-" : login;
    }

    private static String nullSafe(String value, String fallback) {
        String normalized = normalizeText(value);
        if (!normalized.isBlank()) {
            return normalized;
        }
        return fallback;
    }

    private record MembershipSnapshot(boolean exists, boolean active, LocalDateTime leftAt, String rosterRole) {
    }
}
