
package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.LookupItem;
import com.pulse.desktop.util.PasswordHasher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminUserRepository {
    private static final Set<String> ROLES = Set.of("PLAYER", "CAPTAIN", "ORGANIZER", "ADMIN");
    private static final Set<String> GENDERS = Set.of("UNKNOWN", "MALE", "FEMALE", "OTHER");
    private static final Set<String> USER_SORTS = Set.of(
            "id", "username", "email", "role", "active", "verified", "country", "created_at", "last_login_at"
    );
    private static final Set<String> DIRECTIONS = Set.of("asc", "desc");

    public record UserSearchFilter(String q, String role, String active, String verified, String sort, String direction) {
        public static UserSearchFilter defaults() {
            return new UserSearchFilter("", "", "", "", "created_at", "desc");
        }
    }

    public record UserRow(
            int userId,
            String username,
            String email,
            String displayName,
            String role,
            boolean active,
            boolean emailVerified,
            boolean twoFactorEnabled,
            String country,
            LocalDateTime createdAt,
            LocalDateTime lastLoginAt,
            Integer profileImageId,
            String profileImagePath
    ) {
    }

    public record UserDetail(
            int userId,
            String username,
            String email,
            String displayName,
            String role,
            boolean active,
            boolean emailVerified,
            boolean twoFactorEnabled,
            String bio,
            String phone,
            String country,
            LocalDate birthDate,
            String gender,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lastLoginAt,
            Integer profileImageId,
            String profileImagePath
    ) {
    }

    public record RoleStat(String role, String label, int count, int percent) {
    }

    public record CountryStat(String name, int count, int percent) {
    }

    public record UserStats(
            int total,
            int active,
            int inactive,
            int verified,
            int twoFactor,
            int newUsers30d,
            int recentLogin7d,
            List<RoleStat> roles,
            List<CountryStat> countries
    ) {
    }

    public record TeamMembershipRow(int teamId, String teamName, LocalDateTime joinedAt, boolean active) {
    }

    public record UserPostRow(int postId, String content, String visibility, boolean deleted, LocalDateTime createdAt) {
    }

    public record UserOrderRow(int orderId, String orderNumber, String status, BigDecimal totalAmount, LocalDateTime createdAt) {
    }

    public record UserReportRow(int reportId, String targetType, String targetId, String status, LocalDateTime createdAt) {
    }

    public record UserMutationInput(
            Integer userId,
            String username,
            String email,
            String plainPassword,
            String role,
            String displayName,
            String bio,
            String phone,
            String country,
            LocalDate birthDate,
            String gender,
            boolean emailVerified,
            boolean active,
            Integer profileImageId
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

    public List<UserRow> searchForAdmin(UserSearchFilter rawFilter, int limit) throws SQLException {
        UserSearchFilter filter = normalizeUserFilter(rawFilter);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    u.user_id,
                    u.username,
                    u.email,
                    u.display_name,
                    u.role,
                    u.is_active,
                    u.email_verified,
                    u.two_factor_enabled,
                    u.country,
                    u.created_at,
                    u.last_login_at,
                    u.profile_image_id,
                    i.file_url AS profile_image_path
                FROM users u
                LEFT JOIN images i ON i.image_id = u.profile_image_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (!filter.q().isBlank()) {
            sql.append(" AND (LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ? OR LOWER(COALESCE(u.display_name, '')) LIKE ?) ");
            String like = "%" + filter.q().toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (!filter.role().isBlank()) {
            sql.append(" AND u.role = ? ");
            params.add(filter.role());
        }

        Boolean activeFilter = parseBooleanFilter(filter.active());
        if (activeFilter != null) {
            sql.append(" AND u.is_active = ? ");
            params.add(activeFilter);
        }

        Boolean verifiedFilter = parseBooleanFilter(filter.verified());
        if (verifiedFilter != null) {
            sql.append(" AND u.email_verified = ? ");
            params.add(verifiedFilter);
        }

        String sortField = switch (filter.sort()) {
            case "id" -> "u.user_id";
            case "username" -> "u.username";
            case "email" -> "u.email";
            case "role" -> "u.role";
            case "active" -> "u.is_active";
            case "verified" -> "u.email_verified";
            case "country" -> "u.country";
            case "last_login_at" -> "u.last_login_at";
            default -> "u.created_at";
        };
        String direction = "asc".equals(filter.direction()) ? "ASC" : "DESC";

        sql.append(" ORDER BY ").append(sortField).append(' ').append(direction);
        if (!"u.user_id".equals(sortField)) {
            sql.append(", u.user_id DESC");
        }
        sql.append(" LIMIT ? ");
        params.add(Math.max(1, limit));

        List<UserRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new UserRow(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("display_name"),
                            rs.getString("role"),
                            rs.getBoolean("is_active"),
                            rs.getBoolean("email_verified"),
                            rs.getBoolean("two_factor_enabled"),
                            rs.getString("country"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            toLocalDateTime(rs.getTimestamp("last_login_at")),
                            toInteger(rs.getObject("profile_image_id")),
                            rs.getString("profile_image_path")
                    ));
                }
            }
        }

        return rows;
    }

    public UserDetail loadUserDetail(int userId) throws SQLException {
        if (userId <= 0) {
            return null;
        }

        String sql = """
                SELECT
                    u.user_id,
                    u.username,
                    u.email,
                    u.display_name,
                    u.role,
                    u.is_active,
                    u.email_verified,
                    u.two_factor_enabled,
                    u.bio,
                    u.phone,
                    u.country,
                    u.birth_date,
                    u.gender,
                    u.created_at,
                    u.updated_at,
                    u.last_login_at,
                    u.profile_image_id,
                    i.file_url AS profile_image_path
                FROM users u
                LEFT JOIN images i ON i.image_id = u.profile_image_id
                WHERE u.user_id = ?
                LIMIT 1
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new UserDetail(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("role"),
                        rs.getBoolean("is_active"),
                        rs.getBoolean("email_verified"),
                        rs.getBoolean("two_factor_enabled"),
                        rs.getString("bio"),
                        rs.getString("phone"),
                        rs.getString("country"),
                        toLocalDate(rs.getDate("birth_date")),
                        rs.getString("gender"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        toLocalDateTime(rs.getTimestamp("last_login_at")),
                        toInteger(rs.getObject("profile_image_id")),
                        rs.getString("profile_image_path")
                );
            }
        }
    }

    public List<TeamMembershipRow> listTeamMemberships(int userId, String sort, String direction, int limit) throws SQLException {
        if (userId <= 0) {
            return List.of();
        }
        String orderBy = switch (normalizeSort(sort, Set.of("team_id", "joined_at", "is_active"), "joined_at")) {
            case "team_id" -> "t.team_id";
            case "is_active" -> "tm.is_active";
            default -> "tm.joined_at";
        };
        String dir = "asc".equals(normalizeDirection(direction)) ? "ASC" : "DESC";

        String sql = """
                SELECT
                    COALESCE(t.team_id, 0) AS team_id,
                    COALESCE(t.name, '-') AS team_name,
                    tm.joined_at,
                    tm.is_active
                FROM team_members tm
                LEFT JOIN teams t ON t.team_id = tm.team_id
                WHERE tm.user_id = ?
                ORDER BY %s %s, t.team_id DESC
                LIMIT ?
                """.formatted(orderBy, dir);

        List<TeamMembershipRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TeamMembershipRow(
                            rs.getInt("team_id"),
                            rs.getString("team_name"),
                            toLocalDateTime(rs.getTimestamp("joined_at")),
                            rs.getBoolean("is_active")
                    ));
                }
            }
        }
        return rows;
    }

    public List<UserPostRow> listPosts(int userId, String sort, String direction, int limit) throws SQLException {
        if (userId <= 0) {
            return List.of();
        }
        String orderBy = switch (normalizeSort(sort, Set.of("id", "content", "visibility", "deleted", "created_at"), "created_at")) {
            case "id" -> "p.post_id";
            case "content" -> "p.content_text";
            case "visibility" -> "p.visibility";
            case "deleted" -> "p.is_deleted";
            default -> "p.created_at";
        };
        String dir = "asc".equals(normalizeDirection(direction)) ? "ASC" : "DESC";

        String sql = """
                SELECT
                    p.post_id,
                    p.content_text,
                    p.visibility,
                    p.is_deleted,
                    p.created_at
                FROM posts p
                WHERE p.author_user_id = ?
                ORDER BY %s %s, p.post_id DESC
                LIMIT ?
                """.formatted(orderBy, dir);

        List<UserPostRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new UserPostRow(
                            rs.getInt("post_id"),
                            rs.getString("content_text"),
                            rs.getString("visibility"),
                            rs.getBoolean("is_deleted"),
                            toLocalDateTime(rs.getTimestamp("created_at"))
                    ));
                }
            }
        }
        return rows;
    }

    public List<UserOrderRow> listOrders(int userId, String sort, String direction, int limit) throws SQLException {
        if (userId <= 0) {
            return List.of();
        }
        String orderBy = switch (normalizeSort(sort, Set.of("order_number", "status", "total_amount", "created_at"), "created_at")) {
            case "order_number" -> "o.order_number";
            case "status" -> "o.status";
            case "total_amount" -> "o.total_amount";
            default -> "o.created_at";
        };
        String dir = "asc".equals(normalizeDirection(direction)) ? "ASC" : "DESC";

        String sql = """
                SELECT
                    o.order_id,
                    o.order_number,
                    o.status,
                    o.total_amount,
                    o.created_at
                FROM orders o
                WHERE o.user_id = ?
                ORDER BY %s %s, o.order_id DESC
                LIMIT ?
                """.formatted(orderBy, dir);

        List<UserOrderRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new UserOrderRow(
                            rs.getInt("order_id"),
                            rs.getString("order_number"),
                            rs.getString("status"),
                            rs.getBigDecimal("total_amount"),
                            toLocalDateTime(rs.getTimestamp("created_at"))
                    ));
                }
            }
        }
        return rows;
    }

    public List<UserReportRow> listReports(int userId, String sort, String direction, int limit) throws SQLException {
        if (userId <= 0) {
            return List.of();
        }
        String orderBy = switch (normalizeSort(sort, Set.of("id", "target", "target_id", "status", "created_at"), "created_at")) {
            case "id" -> "r.report_id";
            case "target" -> "r.target_type";
            case "target_id" -> "r.target_id";
            case "status" -> "r.status";
            default -> "r.created_at";
        };
        String dir = "asc".equals(normalizeDirection(direction)) ? "ASC" : "DESC";

        String sql = """
                SELECT
                    r.report_id,
                    r.target_type,
                    r.target_id,
                    r.status,
                    r.created_at
                FROM reports r
                WHERE r.reporter_user_id = ?
                ORDER BY %s %s, r.report_id DESC
                LIMIT ?
                """.formatted(orderBy, dir);

        List<UserReportRow> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new UserReportRow(
                            rs.getInt("report_id"),
                            rs.getString("target_type"),
                            String.valueOf(rs.getObject("target_id")),
                            rs.getString("status"),
                            toLocalDateTime(rs.getTimestamp("created_at"))
                    ));
                }
            }
        }
        return rows;
    }

    public List<LookupItem> listProfileImageOptions(int limit) throws SQLException {
        String sql = """
                SELECT image_id AS id, CONCAT('#', image_id, ' - ', file_url) AS label
                FROM images
                ORDER BY image_id DESC
                LIMIT ?
                """;

        List<LookupItem> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new LookupItem(rs.getInt("id"), rs.getString("label")));
                }
            }
        }
        return rows;
    }

    public OperationResult createUser(UserMutationInput rawInput) throws SQLException {
        if (rawInput == null) {
            return OperationResult.error("Formulaire invalide.");
        }

        String username = normalizeText(rawInput.username());
        String email = normalizeText(rawInput.email()).toLowerCase(Locale.ROOT);
        String plainPassword = rawInput.plainPassword() == null ? "" : rawInput.plainPassword().trim();
        String role = normalizeRole(rawInput.role());
        String displayName = normalizeText(rawInput.displayName());
        String bio = nullable(rawInput.bio());
        String phone = nullable(rawInput.phone());
        String country = nullable(rawInput.country());
        String gender = normalizeGender(rawInput.gender());
        Integer profileImageId = positiveOrNull(rawInput.profileImageId());

        OperationResult validation = validateMutationValues(
                username,
                email,
                plainPassword,
                true,
                role,
                displayName,
                phone,
                country
        );
        if (!validation.ok()) {
            return validation;
        }

        if (existsByUsername(username, null)) {
            return OperationResult.error("Ce username est deja utilise.");
        }
        if (existsByEmail(email, null)) {
            return OperationResult.error("Cet email est deja utilise.");
        }

        String sql = """
                INSERT INTO users (
                    username, email, password_hash, role, display_name,
                    bio, phone, country, birth_date, gender,
                    email_verified, is_active, last_login_at,
                    profile_image_id, created_at, updated_at,
                    reset_password_token_hash, reset_password_expires_at,
                    two_factor_enabled, two_factor_secret, two_factor_enabled_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, NULL,
                    ?, NOW(), NOW(),
                    NULL, NULL,
                    0, NULL, NULL
                )
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, PasswordHasher.hash(plainPassword));
            statement.setString(4, role);
            statement.setString(5, displayName);
            statement.setString(6, bio);
            statement.setString(7, phone);
            statement.setString(8, country);
            if (rawInput.birthDate() == null) {
                statement.setObject(9, null);
            } else {
                statement.setDate(9, Date.valueOf(rawInput.birthDate()));
            }
            statement.setString(10, gender);
            statement.setBoolean(11, rawInput.emailVerified());
            statement.setBoolean(12, rawInput.active());
            if (profileImageId == null) {
                statement.setObject(13, null);
            } else {
                statement.setInt(13, profileImageId);
            }
            statement.executeUpdate();
        }

        return OperationResult.ok("Utilisateur cree avec succes.");
    }

    public OperationResult updateUser(UserMutationInput rawInput) throws SQLException {
        if (rawInput == null || rawInput.userId() == null || rawInput.userId() <= 0) {
            return OperationResult.error("Utilisateur invalide.");
        }

        int userId = rawInput.userId();
        String username = normalizeText(rawInput.username());
        String email = normalizeText(rawInput.email()).toLowerCase(Locale.ROOT);
        String plainPassword = rawInput.plainPassword() == null ? "" : rawInput.plainPassword().trim();
        String role = normalizeRole(rawInput.role());
        String displayName = normalizeText(rawInput.displayName());
        String bio = nullable(rawInput.bio());
        String phone = nullable(rawInput.phone());
        String country = nullable(rawInput.country());
        String gender = normalizeGender(rawInput.gender());
        Integer profileImageId = positiveOrNull(rawInput.profileImageId());

        OperationResult validation = validateMutationValues(
                username,
                email,
                plainPassword,
                false,
                role,
                displayName,
                phone,
                country
        );
        if (!validation.ok()) {
            return validation;
        }

        if (loadUserDetail(userId) == null) {
            return OperationResult.error("Utilisateur introuvable.");
        }
        if (existsByUsername(username, userId)) {
            return OperationResult.error("Ce username est deja utilise.");
        }
        if (existsByEmail(email, userId)) {
            return OperationResult.error("Cet email est deja utilise.");
        }

        StringBuilder sql = new StringBuilder("""
                UPDATE users
                SET username = ?,
                    email = ?,
                    role = ?,
                    display_name = ?,
                    bio = ?,
                    phone = ?,
                    country = ?,
                    birth_date = ?,
                    gender = ?,
                    email_verified = ?,
                    is_active = ?,
                    profile_image_id = ?,
                    updated_at = NOW()
                """);

        boolean withPassword = !plainPassword.isBlank();
        if (withPassword) {
            sql.append(", password_hash = ? ");
        }
        sql.append(" WHERE user_id = ? ");

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, username);
            statement.setString(index++, email);
            statement.setString(index++, role);
            statement.setString(index++, displayName);
            statement.setString(index++, bio);
            statement.setString(index++, phone);
            statement.setString(index++, country);
            if (rawInput.birthDate() == null) {
                statement.setObject(index++, null);
            } else {
                statement.setDate(index++, Date.valueOf(rawInput.birthDate()));
            }
            statement.setString(index++, gender);
            statement.setBoolean(index++, rawInput.emailVerified());
            statement.setBoolean(index++, rawInput.active());
            if (profileImageId == null) {
                statement.setObject(index++, null);
            } else {
                statement.setInt(index++, profileImageId);
            }
            if (withPassword) {
                statement.setString(index++, PasswordHasher.hash(plainPassword));
            }
            statement.setInt(index, userId);

            int updated = statement.executeUpdate();
            if (updated <= 0) {
                return OperationResult.error("Utilisateur introuvable.");
            }
        }

        return OperationResult.ok("Utilisateur mis a jour.");
    }

    public OperationResult deleteUser(int userId, Integer connectedUserId) throws SQLException {
        if (userId <= 0) {
            return OperationResult.error("Utilisateur invalide.");
        }
        if (connectedUserId != null && connectedUserId == userId) {
            return OperationResult.error("Suppression de votre propre compte admin interdite.");
        }

        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            int deleted = statement.executeUpdate();
            if (deleted <= 0) {
                return OperationResult.error("Utilisateur introuvable.");
            }
        }

        return OperationResult.ok("Utilisateur supprime avec succes.");
    }

    public UserStats buildStats(List<UserRow> rows) {
        List<UserRow> users = rows == null ? List.of() : rows;
        int total = users.size();
        int active = 0;
        int verified = 0;
        int twoFactor = 0;
        int newUsers30d = 0;
        int recentLogin7d = 0;

        Map<String, Integer> roleCounts = new LinkedHashMap<>();
        roleCounts.put("PLAYER", 0);
        roleCounts.put("CAPTAIN", 0);
        roleCounts.put("ORGANIZER", 0);
        roleCounts.put("ADMIN", 0);

        Map<String, Integer> countryCounts = new LinkedHashMap<>();

        LocalDateTime newSince = LocalDateTime.now().minusDays(30);
        LocalDateTime loginSince = LocalDateTime.now().minusDays(7);

        for (UserRow user : users) {
            if (user.active()) {
                active++;
            }
            if (user.emailVerified()) {
                verified++;
            }
            if (user.twoFactorEnabled()) {
                twoFactor++;
            }
            if (user.createdAt() != null && !user.createdAt().isBefore(newSince)) {
                newUsers30d++;
            }
            if (user.lastLoginAt() != null && !user.lastLoginAt().isBefore(loginSince)) {
                recentLogin7d++;
            }

            String role = normalizeRole(user.role());
            roleCounts.computeIfPresent(role, (ignored, count) -> count + 1);

            String country = normalizeText(user.country());
            if (country.isBlank()) {
                country = "Non renseigne";
            }
            countryCounts.put(country, countryCounts.getOrDefault(country, 0) + 1);
        }

        List<RoleStat> roles = new ArrayList<>();
        roles.add(new RoleStat("PLAYER", "Joueurs", roleCounts.getOrDefault("PLAYER", 0), percent(roleCounts.getOrDefault("PLAYER", 0), total)));
        roles.add(new RoleStat("CAPTAIN", "Capitaines", roleCounts.getOrDefault("CAPTAIN", 0), percent(roleCounts.getOrDefault("CAPTAIN", 0), total)));
        roles.add(new RoleStat("ORGANIZER", "Organisateurs", roleCounts.getOrDefault("ORGANIZER", 0), percent(roleCounts.getOrDefault("ORGANIZER", 0), total)));
        roles.add(new RoleStat("ADMIN", "Admins", roleCounts.getOrDefault("ADMIN", 0), percent(roleCounts.getOrDefault("ADMIN", 0), total)));
        roles.sort((left, right) -> Integer.compare(right.count(), left.count()));

        List<Map.Entry<String, Integer>> countriesRaw = new ArrayList<>(countryCounts.entrySet());
        countriesRaw.sort((left, right) -> Integer.compare(right.getValue(), left.getValue()));

        List<CountryStat> countries = new ArrayList<>();
        for (int i = 0; i < countriesRaw.size() && i < 5; i++) {
            Map.Entry<String, Integer> entry = countriesRaw.get(i);
            countries.add(new CountryStat(entry.getKey(), entry.getValue(), percent(entry.getValue(), total)));
        }

        return new UserStats(
                total,
                active,
                Math.max(0, total - active),
                verified,
                twoFactor,
                newUsers30d,
                recentLogin7d,
                roles,
                countries
        );
    }

    private static OperationResult validateMutationValues(
            String username,
            String email,
            String plainPassword,
            boolean creating,
            String role,
            String displayName,
            String phone,
            String country
    ) {
        if (username.isBlank() || username.length() < 3 || username.length() > 50) {
            return OperationResult.error("Le username doit contenir entre 3 et 50 caracteres.");
        }
        if (email.isBlank() || email.length() > 190 || !isValidEmail(email)) {
            return OperationResult.error("Email invalide.");
        }
        if (creating && plainPassword.length() < 8) {
            return OperationResult.error("Le mot de passe doit contenir au moins 8 caracteres.");
        }
        if (!creating && !plainPassword.isBlank() && plainPassword.length() < 8) {
            return OperationResult.error("Le mot de passe doit contenir au moins 8 caracteres.");
        }
        if (!ROLES.contains(role)) {
            return OperationResult.error("Role invalide.");
        }
        if (displayName.isBlank() || displayName.length() < 2 || displayName.length() > 80) {
            return OperationResult.error("Le display name doit contenir entre 2 et 80 caracteres.");
        }
        if (phone != null && !phone.matches("^[0-9+\\-\\s().]{6,30}$")) {
            return OperationResult.error("Telephone invalide.");
        }
        if (country != null && country.length() > 80) {
            return OperationResult.error("Le pays ne doit pas depasser 80 caracteres.");
        }
        return OperationResult.ok("OK");
    }

    private boolean existsByUsername(String username, Integer excludeUserId) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE username = ?" + (excludeUserId == null ? "" : " AND user_id <> ?") + " LIMIT 1";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            if (excludeUserId != null) {
                statement.setInt(2, excludeUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean existsByEmail(String email, Integer excludeUserId) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE email = ?" + (excludeUserId == null ? "" : " AND user_id <> ?") + " LIMIT 1";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            if (excludeUserId != null) {
                statement.setInt(2, excludeUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static UserSearchFilter normalizeUserFilter(UserSearchFilter raw) {
        UserSearchFilter source = raw == null ? UserSearchFilter.defaults() : raw;
        String roleToken = normalizeText(source.role()).toUpperCase(Locale.ROOT);
        String role = ROLES.contains(roleToken) ? roleToken : "";

        return new UserSearchFilter(
                normalizeText(source.q()),
                role,
                normalizeText(source.active()),
                normalizeText(source.verified()),
                normalizeSort(source.sort(), USER_SORTS, "created_at"),
                normalizeDirection(source.direction())
        );
    }

    private static String normalizeDirection(String value) {
        String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
        if (DIRECTIONS.contains(normalized)) {
            return normalized;
        }
        return "desc";
    }

    private static String normalizeRole(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if (ROLES.contains(normalized)) {
            return normalized;
        }
        return "PLAYER";
    }

    private static String normalizeGender(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if (GENDERS.contains(normalized)) {
            return normalized;
        }
        return "UNKNOWN";
    }

    private static Boolean parseBooleanFilter(String value) {
        String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        if (Set.of("1", "true", "yes", "oui").contains(normalized)) {
            return Boolean.TRUE;
        }
        if (Set.of("0", "false", "no", "non").contains(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static int percent(int count, int total) {
        if (count <= 0 || total <= 0) {
            return 0;
        }
        return (int) Math.round(((double) count / (double) total) * 100.0d);
    }

    private static boolean isValidEmail(String value) {
        return value != null && value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
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

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer intValue) {
            return intValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        if (value instanceof String text) {
            String normalized = text.trim();
            if (normalized.isBlank()) {
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

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        int index = 1;
        for (Object value : params) {
            if (value instanceof Integer intValue) {
                statement.setInt(index++, intValue);
                continue;
            }
            if (value instanceof Boolean boolValue) {
                statement.setBoolean(index++, boolValue);
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
