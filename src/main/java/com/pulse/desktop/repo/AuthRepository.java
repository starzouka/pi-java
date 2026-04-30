package com.pulse.desktop.repo;

import com.pulse.desktop.auth.SessionUser;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.AuthLoginResult;
import com.pulse.desktop.model.RegisterFormData;
import com.pulse.desktop.model.RegisteredUser;
import com.pulse.desktop.util.PasswordHasher;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;

public class AuthRepository {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public enum PasswordChangeStatus {
        SUCCESS,
        USER_NOT_FOUND,
        INVALID_CURRENT_PASSWORD,
        SAME_PASSWORD_AS_OLD
    }

    public AuthLoginResult login(String email, String plainPassword) throws SQLException {
        String sql = """
                SELECT user_id, username, email, password_hash, role, display_name,
                       is_active, email_verified, two_factor_enabled, two_factor_secret
                FROM users
                WHERE email = ?
                LIMIT 1
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return AuthLoginResult.failure(AuthLoginResult.Status.INVALID_CREDENTIALS);
                }

                boolean active = rs.getBoolean("is_active");
                String hash = rs.getString("password_hash");
                if (!active) {
                    return AuthLoginResult.failure(AuthLoginResult.Status.ACCOUNT_INACTIVE);
                }
                if (!PasswordHasher.verify(plainPassword, hash)) {
                    return AuthLoginResult.failure(AuthLoginResult.Status.INVALID_CREDENTIALS);
                }

                boolean emailVerified = rs.getBoolean("email_verified");
                if (!emailVerified) {
                    return AuthLoginResult.failure(AuthLoginResult.Status.EMAIL_NOT_VERIFIED);
                }

                boolean twoFactorEnabled = rs.getBoolean("two_factor_enabled");
                String twoFactorSecret = rs.getString("two_factor_secret");
                int userId = rs.getInt("user_id");
                SessionUser user = new SessionUser(
                        userId,
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("display_name"),
                        emailVerified,
                        twoFactorEnabled
                );

                if (twoFactorEnabled && twoFactorSecret != null && !twoFactorSecret.isBlank()) {
                    return new AuthLoginResult(AuthLoginResult.Status.TWO_FACTOR_REQUIRED, user);
                }

                touchLastLogin(userId);
                return AuthLoginResult.success(user);
            }
        }
    }

    public AuthLoginResult loginOrRegisterGoogleUser(String email, String displayName, String googleSubject, boolean emailVerified)
            throws SQLException {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            return AuthLoginResult.failure(AuthLoginResult.Status.INVALID_CREDENTIALS);
        }

        try (Connection connection = Jdbc.open()) {
            AuthUserRow row = loadAuthUserByEmail(connection, normalizedEmail);
            if (row == null) {
                createGoogleUser(connection, normalizedEmail, displayName, googleSubject, emailVerified);
                row = loadAuthUserByEmail(connection, normalizedEmail);
            }

            if (row == null) {
                return AuthLoginResult.failure(AuthLoginResult.Status.INVALID_CREDENTIALS);
            }

            if (!row.active()) {
                return AuthLoginResult.failure(AuthLoginResult.Status.ACCOUNT_INACTIVE);
            }

            if (emailVerified && !row.emailVerified()) {
                markEmailVerified(connection, row.userId());
                row = new AuthUserRow(
                        row.userId(),
                        row.username(),
                        row.email(),
                        row.role(),
                        row.displayName(),
                        row.active(),
                        true,
                        row.twoFactorEnabled(),
                        row.twoFactorSecret()
                );
            }

            SessionUser user = toSessionUser(row);
            if (row.twoFactorEnabled() && row.twoFactorSecret() != null && !row.twoFactorSecret().isBlank()) {
                return new AuthLoginResult(AuthLoginResult.Status.TWO_FACTOR_REQUIRED, user);
            }

            touchLastLogin(connection, row.userId());
            return AuthLoginResult.success(user);
        }
    }

    public AuthLoginResult loginOrRegisterSteamUser(String steamId, String steamDisplayName) throws SQLException {
        String normalizedSteamId = normalizeSteamId(steamId);
        if (normalizedSteamId.isBlank()) {
            return AuthLoginResult.failure(AuthLoginResult.Status.INVALID_CREDENTIALS);
        }
        String steamEmail = "steam_" + normalizedSteamId + "@steam.pulse.local";

        try (Connection connection = Jdbc.open()) {
            AuthUserRow row = loadAuthUserByEmail(connection, steamEmail);
            if (row == null) {
                createSteamUser(connection, normalizedSteamId, steamDisplayName);
                row = loadAuthUserByEmail(connection, steamEmail);
            }

            if (row == null) {
                return AuthLoginResult.failure(AuthLoginResult.Status.INVALID_CREDENTIALS);
            }

            if (!row.active()) {
                return AuthLoginResult.failure(AuthLoginResult.Status.ACCOUNT_INACTIVE);
            }

            if (!row.emailVerified()) {
                markEmailVerified(connection, row.userId());
                row = new AuthUserRow(
                        row.userId(),
                        row.username(),
                        row.email(),
                        row.role(),
                        row.displayName(),
                        row.active(),
                        true,
                        row.twoFactorEnabled(),
                        row.twoFactorSecret()
                );
            }

            SessionUser user = toSessionUser(row);
            if (row.twoFactorEnabled() && row.twoFactorSecret() != null && !row.twoFactorSecret().isBlank()) {
                return new AuthLoginResult(AuthLoginResult.Status.TWO_FACTOR_REQUIRED, user);
            }

            touchLastLogin(connection, row.userId());
            return AuthLoginResult.success(user);
        }
    }

    public SessionUser findSessionUserById(int userId) throws SQLException {
        String sql = """
                SELECT user_id, username, email, role, display_name, email_verified, two_factor_enabled
                FROM users
                WHERE user_id = ?
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new SessionUser(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("display_name"),
                        rs.getBoolean("email_verified"),
                        rs.getBoolean("two_factor_enabled")
                );
            }
        }
    }

    public void markLoginSuccess(int userId) throws SQLException {
        touchLastLogin(userId);
    }

    public boolean emailExists(String email) throws SQLException {
        return countByColumn("email", email.trim().toLowerCase()) > 0;
    }

    public boolean usernameExists(String username) throws SQLException {
        return countByColumn("username", username.trim()) > 0;
    }

    public RegisteredUser register(RegisterFormData data) throws SQLException {
        if (data == null) {
            throw new SQLException("Formulaire invalide.");
        }
        LocalDate birthDate = data.getBirthDate();
        if (birthDate != null) {
            LocalDate today = LocalDate.now();
            if (birthDate.isAfter(today)) {
                throw new SQLException("Date de naissance invalide.");
            }
            if (birthDate.isAfter(today.minusYears(13))) {
                throw new SQLException("Age minimum requis: 13 ans.");
            }
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
                    NULL, ?, ?, ?, ?,
                    0, 1, NULL,
                    NULL, NOW(), NOW(),
                    NULL, NULL,
                    0, NULL, NULL
                )
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, data.getUsername().trim());
            statement.setString(2, data.getEmail().trim().toLowerCase());
            statement.setString(3, PasswordHasher.hash(data.getPassword()));
            statement.setString(4, normalizeRole(data.getRole()));
            statement.setString(5, data.getDisplayName().trim());
            statement.setString(6, safeText(data.getPhone()));
            statement.setString(7, safeText(data.getCountry()));
            if (birthDate == null) {
                statement.setDate(8, null);
            } else {
                statement.setDate(8, Date.valueOf(birthDate));
            }
            statement.setString(9, normalizeGender(data.getGender()));
            statement.executeUpdate();

            int userId = -1;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    userId = keys.getInt(1);
                }
            }
            if (userId <= 0) {
                throw new SQLException("Impossible de recuperer user_id apres creation du compte.");
            }
            return new RegisteredUser(userId, data.getEmail().trim().toLowerCase(), data.getDisplayName().trim());
        }
    }

    public boolean markEmailAsVerified(int userId) throws SQLException {
        String sql = """
                UPDATE users
                SET email_verified = 1, updated_at = NOW()
                WHERE user_id = ?
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public String createResetPasswordToken(String email, int lifetimeSeconds) throws SQLException {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isBlank()) {
            return null;
        }

        String sql = """
                UPDATE users
                SET reset_password_token_hash = ?, reset_password_expires_at = FROM_UNIXTIME(?), updated_at = NOW()
                WHERE email = ?
                """;

        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);
        long expiresAt = Instant.now().getEpochSecond() + Math.max(60, lifetimeSeconds);

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            statement.setLong(2, expiresAt);
            statement.setString(3, normalizedEmail);
            int updated = statement.executeUpdate();
            return updated > 0 ? rawToken : null;
        }
    }

    public boolean hasValidResetPasswordToken(String rawToken) throws SQLException {
        String sql = """
                SELECT user_id
                FROM users
                WHERE reset_password_token_hash = ?
                  AND reset_password_expires_at IS NOT NULL
                  AND reset_password_expires_at > NOW()
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sha256(rawToken));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean resetPasswordByToken(String rawToken, String plainPassword) throws SQLException {
        String sql = """
                UPDATE users
                SET password_hash = ?,
                    reset_password_token_hash = NULL,
                    reset_password_expires_at = NULL,
                    updated_at = NOW()
                WHERE reset_password_token_hash = ?
                  AND reset_password_expires_at IS NOT NULL
                  AND reset_password_expires_at > NOW()
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PasswordHasher.hash(plainPassword));
            statement.setString(2, sha256(rawToken));
            return statement.executeUpdate() > 0;
        }
    }

    public PasswordChangeStatus changePassword(int userId, String currentPassword, String newPassword) throws SQLException {
        String selectSql = """
                SELECT password_hash
                FROM users
                WHERE user_id = ?
                LIMIT 1
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement select = connection.prepareStatement(selectSql)) {
            select.setInt(1, userId);
            String hash;
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    return PasswordChangeStatus.USER_NOT_FOUND;
                }
                hash = rs.getString("password_hash");
            }

            if (!PasswordHasher.verify(currentPassword, hash)) {
                return PasswordChangeStatus.INVALID_CURRENT_PASSWORD;
            }
            if (PasswordHasher.verify(newPassword, hash)) {
                return PasswordChangeStatus.SAME_PASSWORD_AS_OLD;
            }

            String updateSql = """
                    UPDATE users
                    SET password_hash = ?, updated_at = NOW()
                    WHERE user_id = ?
                    """;
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, PasswordHasher.hash(newPassword));
                update.setInt(2, userId);
                int updated = update.executeUpdate();
                return updated > 0 ? PasswordChangeStatus.SUCCESS : PasswordChangeStatus.USER_NOT_FOUND;
            }
        }
    }

    public String getTwoFactorSecret(int userId) throws SQLException {
        String sql = """
                SELECT two_factor_secret
                FROM users
                WHERE user_id = ?
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString("two_factor_secret");
            }
        }
    }

    public void enableTwoFactor(int userId, String secret) throws SQLException {
        String sql = """
                UPDATE users
                SET two_factor_enabled = 1,
                    two_factor_secret = ?,
                    two_factor_enabled_at = NOW(),
                    updated_at = NOW()
                WHERE user_id = ?
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, secret);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public void disableTwoFactor(int userId) throws SQLException {
        String sql = """
                UPDATE users
                SET two_factor_enabled = 0,
                    two_factor_secret = NULL,
                    two_factor_enabled_at = NULL,
                    updated_at = NOW()
                WHERE user_id = ?
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    public RegisteredUser findRegisteredUserByEmail(String email) throws SQLException {
        String sql = """
                SELECT user_id, email, COALESCE(display_name, username) AS display_name
                FROM users
                WHERE email = ?
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email == null ? "" : email.trim().toLowerCase());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new RegisteredUser(rs.getInt("user_id"), rs.getString("email"), rs.getString("display_name"));
            }
        }
    }

    public RegisteredUser findRegisteredUserById(int userId) throws SQLException {
        String sql = """
                SELECT user_id, email, COALESCE(display_name, username) AS display_name
                FROM users
                WHERE user_id = ?
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new RegisteredUser(rs.getInt("user_id"), rs.getString("email"), rs.getString("display_name"));
            }
        }
    }

    private AuthUserRow loadAuthUserByEmail(Connection connection, String email) throws SQLException {
        String sql = """
                SELECT user_id, username, email, role, display_name,
                       is_active, email_verified, two_factor_enabled, two_factor_secret
                FROM users
                WHERE email = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new AuthUserRow(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("display_name"),
                        rs.getBoolean("is_active"),
                        rs.getBoolean("email_verified"),
                        rs.getBoolean("two_factor_enabled"),
                        rs.getString("two_factor_secret")
                );
            }
        }
    }

    private SessionUser toSessionUser(AuthUserRow row) {
        return new SessionUser(
                row.userId(),
                row.username(),
                row.email(),
                row.role(),
                row.displayName(),
                row.emailVerified(),
                row.twoFactorEnabled()
        );
    }

    private void createGoogleUser(
            Connection connection,
            String normalizedEmail,
            String displayName,
            String googleSubject,
            boolean emailVerified
    ) throws SQLException {
        String sql = """
                INSERT INTO users (
                    username, email, password_hash, role, display_name,
                    bio, phone, country, birth_date, gender,
                    email_verified, is_active, last_login_at,
                    profile_image_id, created_at, updated_at,
                    reset_password_token_hash, reset_password_expires_at,
                    two_factor_enabled, two_factor_secret, two_factor_enabled_at
                ) VALUES (
                    ?, ?, ?, 'PLAYER', ?,
                    NULL, NULL, NULL, NULL, 'UNKNOWN',
                    ?, 1, NOW(),
                    NULL, NOW(), NOW(),
                    NULL, NULL,
                    0, NULL, NULL
                )
                """;

        String username = generateGoogleUsername(connection, normalizedEmail);
        String resolvedDisplayName = normalizeGoogleDisplayName(displayName, normalizedEmail, username);
        String hashedRandomPassword = PasswordHasher.hash(generateSecureToken() + (googleSubject == null ? "" : googleSubject));

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, normalizedEmail);
            statement.setString(3, hashedRandomPassword);
            statement.setString(4, resolvedDisplayName);
            statement.setBoolean(5, emailVerified);
            statement.executeUpdate();
        } catch (SQLException ex) {
            if (!isDuplicateKey(ex)) {
                throw ex;
            }
        }
    }

    private void createSteamUser(
            Connection connection,
            String normalizedSteamId,
            String steamDisplayName
    ) throws SQLException {
        String email = "steam_" + normalizedSteamId + "@steam.pulse.local";
        String username = generateSteamUsername(connection, steamDisplayName, normalizedSteamId);
        String displayName = normalizeSteamDisplayName(steamDisplayName, normalizedSteamId);

        String sql = """
                INSERT INTO users (
                    username, email, password_hash, role, display_name,
                    bio, phone, country, birth_date, gender,
                    email_verified, is_active, last_login_at,
                    profile_image_id, created_at, updated_at,
                    reset_password_token_hash, reset_password_expires_at,
                    two_factor_enabled, two_factor_secret, two_factor_enabled_at
                ) VALUES (
                    ?, ?, ?, 'PLAYER', ?,
                    NULL, NULL, NULL, NULL, 'UNKNOWN',
                    1, 1, NOW(),
                    NULL, NOW(), NOW(),
                    NULL, NULL,
                    0, NULL, NULL
                )
                """;

        String hashedRandomPassword = PasswordHasher.hash(generateSecureToken() + normalizedSteamId);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, hashedRandomPassword);
            statement.setString(4, displayName);
            statement.executeUpdate();
        } catch (SQLException ex) {
            if (!isDuplicateKey(ex)) {
                throw ex;
            }
        }
    }

    private static boolean isDuplicateKey(SQLException ex) {
        if (ex == null) {
            return false;
        }
        if (ex.getErrorCode() == 1062) {
            return true;
        }
        String sqlState = ex.getSQLState();
        return sqlState != null && sqlState.startsWith("23");
    }

    private void markEmailVerified(Connection connection, int userId) throws SQLException {
        String sql = """
                UPDATE users
                SET email_verified = 1, updated_at = NOW()
                WHERE user_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private String generateGoogleUsername(Connection connection, String email) throws SQLException {
        String localPart = email;
        int at = email.indexOf('@');
        if (at > 0) {
            localPart = email.substring(0, at);
        }

        String base = sanitizeUsername(localPart);
        if (base.isBlank()) {
            base = "player";
        }
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }

        String candidate = base;
        int suffix = 1;
        while (usernameExists(connection, candidate)) {
            String tail = "_" + suffix;
            int maxBaseLength = Math.max(3, 24 - tail.length());
            String currentBase = base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base;
            candidate = currentBase + tail;
            suffix++;
            if (suffix > 5000) {
                candidate = "player_" + generateSecureToken().substring(0, 10).toLowerCase(Locale.ROOT);
                if (!usernameExists(connection, candidate)) {
                    break;
                }
            }
        }
        return candidate;
    }

    private String generateSteamUsername(Connection connection, String steamDisplayName, String steamId) throws SQLException {
        String baseSource = steamDisplayName == null ? "" : steamDisplayName.trim();
        String base = sanitizeUsername(baseSource);
        if (base.isBlank()) {
            String tail = steamId == null ? "player" : steamId;
            if (tail.length() > 8) {
                tail = tail.substring(tail.length() - 8);
            }
            base = "steam_" + tail;
        }
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }

        String candidate = base;
        int suffix = 1;
        while (usernameExists(connection, candidate)) {
            String tail = "_" + suffix;
            int maxBaseLength = Math.max(3, 24 - tail.length());
            String currentBase = base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base;
            candidate = currentBase + tail;
            suffix++;
            if (suffix > 5000) {
                candidate = "steam_" + generateSecureToken().substring(0, 10).toLowerCase(Locale.ROOT);
                if (!usernameExists(connection, candidate)) {
                    break;
                }
            }
        }
        return candidate;
    }

    private boolean usernameExists(Connection connection, String username) throws SQLException {
        String sql = """
                SELECT user_id
                FROM users
                WHERE username = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSteamId(String steamId) {
        if (steamId == null) {
            return "";
        }
        String digits = steamId.trim().replaceAll("[^0-9]", "");
        if (digits.length() < 5) {
            return "";
        }
        return digits;
    }

    private static String sanitizeUsername(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (char c : value.toLowerCase(Locale.ROOT).toCharArray()) {
            boolean alphaNumeric = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (alphaNumeric || c == '_' || c == '-') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String normalizeGoogleDisplayName(String displayName, String email, String username) {
        if (displayName != null) {
            String trimmed = displayName.trim();
            if (!trimmed.isBlank()) {
                return trimmed;
            }
        }
        int at = email == null ? -1 : email.indexOf('@');
        if (at > 0) {
            return email.substring(0, at);
        }
        return username;
    }

    private static String normalizeSteamDisplayName(String displayName, String steamId) {
        if (displayName != null) {
            String trimmed = displayName.trim();
            if (!trimmed.isBlank()) {
                return trimmed;
            }
        }
        String tail = steamId == null ? "" : steamId.trim();
        if (tail.length() > 4) {
            tail = tail.substring(tail.length() - 4);
        }
        return tail.isBlank() ? "Steam Player" : "Steam Player " + tail;
    }

    private int countByColumn(String column, String value) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE " + column + " = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private void touchLastLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET last_login_at = NOW(), updated_at = NOW() WHERE user_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private void touchLastLogin(Connection connection, int userId) throws SQLException {
        String sql = "UPDATE users SET last_login_at = NOW(), updated_at = NOW() WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private static String safeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "PLAYER";
        }
        String normalized = role.trim().toUpperCase();
        return switch (normalized) {
            case "PLAYER", "CAPTAIN", "ORGANIZER", "ADMIN" -> normalized;
            default -> "PLAYER";
        };
    }

    private static String normalizeGender(String gender) {
        if (gender == null) {
            return "UNKNOWN";
        }
        String normalized = gender.trim().toUpperCase();
        return switch (normalized) {
            case "MALE", "FEMALE", "OTHER", "UNKNOWN" -> normalized;
            default -> "UNKNOWN";
        };
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de calculer SHA-256.", ex);
        }
    }

    private record AuthUserRow(
            int userId,
            String username,
            String email,
            String role,
            String displayName,
            boolean active,
            boolean emailVerified,
            boolean twoFactorEnabled,
            String twoFactorSecret
    ) {
    }
}
