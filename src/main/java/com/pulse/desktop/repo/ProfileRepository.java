package com.pulse.desktop.repo;

import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.ProfileData;
import com.pulse.desktop.model.ProfileFilters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ProfileRepository {
    private static final int MAX_POST_IMAGES = 8;
    private static final Set<String> ALLOWED_POST_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    public ProfileData loadOwnProfile(int userId, ProfileFilters rawFilters) throws SQLException {
        ProfileFilters filters = sanitizeFilters(rawFilters);
        ProfileData.Identity identity = loadIdentity(userId);
        if (identity == null) {
            return null;
        }

        List<ProfileData.PostItem> posts = loadPosts(userId, filters, userId);
        List<ProfileData.FriendItem> friends = loadFriends(userId, filters);
        List<ProfileData.TeamItem> teams = loadTeams(userId, filters);

        return new ProfileData(
                identity,
                posts,
                friends,
                teams,
                posts.size(),
                friends.size(),
                teams.size()
        );
    }

    public boolean createPost(int userId, String content, String visibility, List<Path> imageFiles) throws SQLException {
        String normalizedVisibility = normalizeVisibility(visibility);
        String text = content == null ? "" : content.trim();
        List<Path> validImageFiles = normalizePostImageFiles(imageFiles);

        if (text.isBlank() && validImageFiles.isEmpty()) {
            return false;
        }

        Path uploadDirectory = AppConfig.webRootPath().resolve("public").resolve("uploads").resolve("posts");
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException ex) {
            throw new SQLException("Impossible de preparer le dossier d'upload des posts.", ex);
        }

        try (Connection connection = Jdbc.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int postId = insertPost(connection, userId, text, normalizedVisibility);
                int position = 1;

                for (Path sourceFile : validImageFiles) {
                    SavedImageFile saved = copyPostImage(sourceFile, uploadDirectory);
                    int imageId = insertImageRow(
                            connection,
                            userId,
                            saved.fileUrl(),
                            saved.mimeType(),
                            saved.sizeBytes(),
                            saved.width(),
                            saved.height(),
                            "Image du post"
                    );
                    insertPostImage(connection, postId, imageId, position);
                    position++;
                }

                connection.commit();
                return true;
            } catch (Exception ex) {
                connection.rollback();
                if (ex instanceof SQLException sqlEx) {
                    throw sqlEx;
                }
                throw new SQLException("Impossible de publier le post.", ex);
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public boolean createPost(int userId, String content, String visibility) throws SQLException {
        return createPost(userId, content, visibility, List.of());
    }

    public boolean togglePostLike(int postId, int viewerUserId) throws SQLException {
        if (!postExists(postId)) {
            return false;
        }

        String findSql = """
                SELECT 1
                FROM post_likes
                WHERE post_id = ? AND user_id = ?
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement find = connection.prepareStatement(findSql)) {
            find.setInt(1, postId);
            find.setInt(2, viewerUserId);

            boolean liked;
            try (ResultSet rs = find.executeQuery()) {
                liked = rs.next();
            }

            if (liked) {
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM post_likes WHERE post_id = ? AND user_id = ?")) {
                    delete.setInt(1, postId);
                    delete.setInt(2, viewerUserId);
                    delete.executeUpdate();
                }
            } else {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO post_likes (post_id, user_id, created_at) VALUES (?, ?, NOW())")) {
                    insert.setInt(1, postId);
                    insert.setInt(2, viewerUserId);
                    insert.executeUpdate();
                }
            }
        }
        return true;
    }

    public boolean addPostComment(int postId, int viewerUserId, String content) throws SQLException {
        String text = content == null ? "" : content.trim();
        if (text.isBlank()) {
            return false;
        }
        if (!postExists(postId)) {
            return false;
        }

        String sql = """
                INSERT INTO comments (
                    content_text, is_deleted, created_at, updated_at,
                    post_id, author_user_id, parent_comment_id
                ) VALUES (?, 0, NOW(), NOW(), ?, ?, NULL)
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, text);
            statement.setInt(2, postId);
            statement.setInt(3, viewerUserId);
            return statement.executeUpdate() > 0;
        }
    }

    public void updateProfile(
            int userId,
            String displayName,
            String bio,
            String country,
            String phone,
            LocalDate birthDate,
            String gender,
            boolean isActive
    ) throws SQLException {
        String sql = """
                UPDATE users
                SET display_name = ?,
                    bio = ?,
                    country = ?,
                    phone = ?,
                    birth_date = ?,
                    gender = ?,
                    is_active = ?,
                    updated_at = NOW()
                WHERE user_id = ?
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, safeDisplayName(displayName));
            statement.setString(2, nullableTrimmed(bio));
            statement.setString(3, nullableTrimmed(country));
            statement.setString(4, nullableTrimmed(phone));
            if (birthDate == null) {
                statement.setDate(5, null);
            } else {
                statement.setDate(5, Date.valueOf(birthDate));
            }
            statement.setString(6, normalizeGender(gender));
            statement.setBoolean(7, isActive);
            statement.setInt(8, userId);
            statement.executeUpdate();
        }
    }

    public void updateProfileImage(int userId, Path sourceFile, String displayName) throws SQLException, IOException {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            return;
        }

        String originalName = sourceFile.getFileName().toString();
        String extension = fileExtension(originalName);
        if (extension.isBlank()) {
            extension = "bin";
        }

        Path uploadDirectory = AppConfig.webRootPath().resolve("public").resolve("uploads").resolve("profiles");
        Files.createDirectories(uploadDirectory);

        String filename = "profile_" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path destination = uploadDirectory.resolve(filename);
        Files.copy(sourceFile, destination, StandardCopyOption.REPLACE_EXISTING);

        String mimeType = Files.probeContentType(destination);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        int width = 0;
        int height = 0;
        try {
            BufferedImage image = ImageIO.read(destination.toFile());
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (Exception ignored) {
            // Optional dimensions.
        }

        String fileUrl = "uploads/profiles/" + filename;
        long size = Files.size(destination);
        int imageId = insertImageRow(userId, fileUrl, mimeType, size, width, height, displayName);

        String updateSql = "UPDATE users SET profile_image_id = ?, updated_at = NOW() WHERE user_id = ?";
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setInt(1, imageId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private ProfileData.Identity loadIdentity(int userId) throws SQLException {
        String sql = """
                SELECT u.user_id,
                       u.username,
                       u.email,
                       COALESCE(NULLIF(u.display_name, ''), u.username) AS display_name,
                       u.role,
                       u.bio,
                       u.phone,
                       u.country,
                       u.birth_date,
                       u.gender,
                       u.is_active,
                       u.two_factor_enabled,
                       u.two_factor_enabled_at,
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

                LocalDate birthDate = null;
                Date birthDateSql = rs.getDate("birth_date");
                if (birthDateSql != null) {
                    birthDate = birthDateSql.toLocalDate();
                }

                LocalDateTime twoFactorEnabledAt = null;
                java.sql.Timestamp enabledAtTs = rs.getTimestamp("two_factor_enabled_at");
                if (enabledAtTs != null) {
                    twoFactorEnabledAt = enabledAtTs.toLocalDateTime();
                }

                return new ProfileData.Identity(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("role"),
                        rs.getString("bio"),
                        rs.getString("phone"),
                        rs.getString("country"),
                        birthDate,
                        rs.getString("gender"),
                        rs.getBoolean("is_active"),
                        rs.getBoolean("two_factor_enabled"),
                        twoFactorEnabledAt,
                        rs.getString("profile_image_path")
                );
            }
        }
    }

    private List<ProfileData.PostItem> loadPosts(int userId, ProfileFilters filters, int viewerUserId) throws SQLException {
        String sort = switch (filters.postsSort()) {
            case "oldest" -> "p.created_at ASC";
            case "liked" -> "likes_count DESC, p.created_at DESC";
            case "commented" -> "comments_count DESC, p.created_at DESC";
            default -> "p.created_at DESC";
        };

        String sql = """
                SELECT p.post_id,
                       p.content_text,
                       p.visibility,
                       p.created_at,
                       COALESCE(likes.likes_count, 0) AS likes_count,
                       COALESCE(comments.comments_count, 0) AS comments_count,
                       EXISTS(
                           SELECT 1
                           FROM post_likes pl_self
                           WHERE pl_self.post_id = p.post_id
                             AND pl_self.user_id = ?
                       ) AS liked_by_viewer
                FROM posts p
                LEFT JOIN (
                    SELECT post_id, COUNT(*) AS likes_count
                    FROM post_likes
                    GROUP BY post_id
                ) likes ON likes.post_id = p.post_id
                LEFT JOIN (
                    SELECT post_id, COUNT(*) AS comments_count
                    FROM comments
                    WHERE is_deleted = 0
                    GROUP BY post_id
                ) comments ON comments.post_id = p.post_id
                WHERE p.author_user_id = ?
                  AND p.is_deleted = 0
                  AND (? = '' OR p.content_text LIKE ?)
                  AND (? = '' OR p.visibility = ?)
                ORDER BY %s
                LIMIT 50
                """.formatted(sort);

        List<ProfileData.PostItem> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, viewerUserId);
            statement.setInt(2, userId);
            statement.setString(3, filters.postsQuery());
            statement.setString(4, "%" + filters.postsQuery() + "%");
            statement.setString(5, filters.postsVisibility());
            statement.setString(6, filters.postsVisibility());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int postId = rs.getInt("post_id");
                    java.sql.Timestamp createdAtTs = rs.getTimestamp("created_at");
                    List<String> imagePaths = loadPostImages(postId);
                    List<ProfileData.PostCommentItem> comments = loadPostComments(postId);

                    rows.add(new ProfileData.PostItem(
                            postId,
                            rs.getString("content_text"),
                            rs.getString("visibility"),
                            createdAtTs == null ? null : createdAtTs.toLocalDateTime(),
                            rs.getInt("likes_count"),
                            rs.getInt("comments_count"),
                            rs.getBoolean("liked_by_viewer"),
                            imagePaths,
                            comments
                    ));
                }
            }
        }
        return rows;
    }

    private List<String> loadPostImages(int postId) throws SQLException {
        String sql = """
                SELECT i.file_url
                FROM post_images pi
                JOIN images i ON i.image_id = pi.image_id
                WHERE pi.post_id = ?
                ORDER BY pi.position ASC, pi.image_id ASC
                LIMIT 8
                """;

        List<String> imagePaths = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, postId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String value = rs.getString("file_url");
                    if (value != null && !value.isBlank()) {
                        imagePaths.add(value);
                    }
                }
            }
        }
        return imagePaths;
    }

    private List<ProfileData.PostCommentItem> loadPostComments(int postId) throws SQLException {
        String sql = """
                SELECT c.comment_id,
                       c.content_text,
                       c.created_at,
                       COALESCE(NULLIF(u.display_name, ''), u.username) AS author_display_name
                FROM comments c
                JOIN users u ON u.user_id = c.author_user_id
                WHERE c.post_id = ?
                  AND c.is_deleted = 0
                  AND c.parent_comment_id IS NULL
                ORDER BY c.created_at DESC
                LIMIT 3
                """;

        List<ProfileData.PostCommentItem> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, postId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp createdAtTs = rs.getTimestamp("created_at");
                    rows.add(new ProfileData.PostCommentItem(
                            rs.getInt("comment_id"),
                            rs.getString("author_display_name"),
                            rs.getString("content_text"),
                            createdAtTs == null ? null : createdAtTs.toLocalDateTime()
                    ));
                }
            }
        }
        Collections.reverse(rows);
        return rows;
    }

    private List<ProfileData.FriendItem> loadFriends(int userId, ProfileFilters filters) throws SQLException {
        String orderBy = switch (filters.friendsSort()) {
            case "oldest" -> "f.created_at ASC";
            case "name" -> "COALESCE(NULLIF(u.display_name, ''), u.username) ASC";
            default -> "f.created_at DESC";
        };

        try {
            return loadFriendsWithColumns(userId, filters, orderBy, "user1_id", "user2_id");
        } catch (SQLException ex) {
            if (!isUnknownColumnError(ex)) {
                throw ex;
            }
            return loadFriendsWithColumns(userId, filters, orderBy, "user_id1", "user_id2");
        }
    }

    private List<ProfileData.FriendItem> loadFriendsWithColumns(
            int userId,
            ProfileFilters filters,
            String orderBy,
            String user1Column,
            String user2Column
    ) throws SQLException {
        String sql = """
                SELECT u.user_id,
                       u.username,
                       COALESCE(NULLIF(u.display_name, ''), u.username) AS display_name,
                       u.role
                FROM friendships f
                JOIN users u
                  ON u.user_id = CASE
                        WHEN f.%s = ? THEN f.%s
                        ELSE f.%s
                     END
                WHERE (f.%s = ? OR f.%s = ?)
                  AND (? = '' OR u.username LIKE ? OR u.display_name LIKE ?)
                ORDER BY %s
                LIMIT 200
                """.formatted(user1Column, user2Column, user1Column, user1Column, user2Column, orderBy);

        List<ProfileData.FriendItem> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            statement.setString(4, filters.friendsQuery());
            statement.setString(5, "%" + filters.friendsQuery() + "%");
            statement.setString(6, "%" + filters.friendsQuery() + "%");
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ProfileData.FriendItem(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("role")
                    ));
                }
            }
        }
        return rows;
    }

    private List<ProfileData.TeamItem> loadTeams(int userId, ProfileFilters filters) throws SQLException {
        String orderBy = switch (filters.teamsSort()) {
            case "oldest" -> "tm.joined_at ASC";
            case "name" -> "t.name ASC";
            case "region" -> "COALESCE(t.region, '') ASC, t.name ASC";
            default -> "tm.joined_at DESC";
        };

        String sql = """
                SELECT t.team_id,
                       t.name,
                       COALESCE(t.region, 'Region -') AS region,
                       tm.roster_role
                FROM team_members tm
                JOIN teams t ON t.team_id = tm.team_id
                WHERE tm.user_id = ?
                  AND tm.is_active = 1
                  AND (? = '' OR t.name LIKE ?)
                  AND (? = '' OR COALESCE(t.region, '') LIKE ?)
                ORDER BY %s
                LIMIT 80
                """.formatted(orderBy);

        List<ProfileData.TeamItem> rows = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, filters.teamsQuery());
            statement.setString(3, "%" + filters.teamsQuery() + "%");
            statement.setString(4, filters.teamsRegion());
            statement.setString(5, "%" + filters.teamsRegion() + "%");

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ProfileData.TeamItem(
                            rs.getInt("team_id"),
                            rs.getString("name"),
                            rs.getString("region"),
                            rs.getString("roster_role")
                    ));
                }
            }
        }
        return rows;
    }

    private int insertPost(Connection connection, int userId, String contentText, String visibility) throws SQLException {
        String sql = """
                INSERT INTO posts (
                    author_user_id, content_text, visibility, is_deleted, deleted_at, created_at, updated_at
                ) VALUES (?, ?, ?, 0, NULL, NOW(), NOW())
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            if (contentText == null || contentText.isBlank()) {
                statement.setObject(2, null);
            } else {
                statement.setString(2, contentText);
            }
            statement.setString(3, visibility);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insertion post echouee: post_id manquant.");
    }

    private void insertPostImage(Connection connection, int postId, int imageId, int position) throws SQLException {
        String sql = """
                INSERT INTO post_images (post_id, image_id, position)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, postId);
            statement.setInt(2, imageId);
            statement.setInt(3, Math.max(1, position));
            statement.executeUpdate();
        }
    }

    private boolean postExists(int postId) throws SQLException {
        String sql = """
                SELECT 1
                FROM posts
                WHERE post_id = ?
                  AND is_deleted = 0
                LIMIT 1
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, postId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<Path> normalizePostImageFiles(List<Path> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return List.of();
        }

        Set<Path> deduped = new LinkedHashSet<>();
        for (Path path : imageFiles) {
            if (path == null) {
                continue;
            }
            Path normalized = path.toAbsolutePath().normalize();
            if (!Files.isRegularFile(normalized)) {
                continue;
            }
            String extension = fileExtension(normalized.getFileName().toString());
            if (!ALLOWED_POST_IMAGE_EXTENSIONS.contains(extension)) {
                continue;
            }
            deduped.add(normalized);
            if (deduped.size() >= MAX_POST_IMAGES) {
                break;
            }
        }
        return new ArrayList<>(deduped);
    }

    private SavedImageFile copyPostImage(Path sourceFile, Path uploadDirectory) throws IOException {
        String extension = fileExtension(sourceFile.getFileName().toString());
        if (extension.isBlank()) {
            extension = "bin";
        }

        String filename = "post_" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path destination = uploadDirectory.resolve(filename);
        Files.copy(sourceFile, destination, StandardCopyOption.REPLACE_EXISTING);

        String mimeType = resolveMimeType(destination, extension);
        long sizeBytes = Files.size(destination);

        int width = 0;
        int height = 0;
        try {
            BufferedImage image = ImageIO.read(destination.toFile());
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (Exception ignored) {
            // Optional dimensions.
        }

        return new SavedImageFile(
                "uploads/posts/" + filename,
                mimeType,
                sizeBytes,
                width,
                height
        );
    }

    private static String resolveMimeType(Path destinationFile, String extension) throws IOException {
        String mimeType = Files.probeContentType(destinationFile);
        if (mimeType != null && !mimeType.isBlank()) {
            return mimeType;
        }

        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    private int insertImageRow(
            int userId,
            String fileUrl,
            String mimeType,
            long sizeBytes,
            int width,
            int height,
            String displayName
    ) throws SQLException {
        try (Connection connection = Jdbc.open()) {
            return insertImageRow(
                    connection,
                    userId,
                    fileUrl,
                    mimeType,
                    sizeBytes,
                    width,
                    height,
                    "Photo profil de " + (displayName == null ? "joueur" : displayName.trim())
            );
        }
    }

    private int insertImageRow(
            Connection connection,
            int userId,
            String fileUrl,
            String mimeType,
            long sizeBytes,
            int width,
            int height,
            String altText
    ) throws SQLException {
        String sql = """
                INSERT INTO images (
                    file_url, mime_type, size_bytes, width, height, alt_text, uploaded_by_user_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fileUrl);
            statement.setString(2, mimeType);
            statement.setLong(3, Math.max(0, sizeBytes));
            if (width > 0) {
                statement.setInt(4, width);
            } else {
                statement.setObject(4, null);
            }
            if (height > 0) {
                statement.setInt(5, height);
            } else {
                statement.setObject(5, null);
            }
            statement.setString(6, altText);
            statement.setInt(7, userId);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insertion image echouee: image_id manquant.");
    }

    private record SavedImageFile(
            String fileUrl,
            String mimeType,
            long sizeBytes,
            int width,
            int height
    ) {
    }

    private static ProfileFilters sanitizeFilters(ProfileFilters raw) {
        ProfileFilters source = raw == null ? ProfileFilters.defaults() : raw;

        String postsVisibility = normalizeVisibility(source.postsVisibility(), true);
        String postsSort = normalizeChoice(source.postsSort(), List.of("latest", "oldest", "liked", "commented"), "latest");
        String friendsSort = normalizeChoice(source.friendsSort(), List.of("recent", "oldest", "name"), "recent");
        String teamsSort = normalizeChoice(source.teamsSort(), List.of("latest", "oldest", "name", "region"), "latest");

        return new ProfileFilters(
                normalizeText(source.postsQuery()),
                postsVisibility,
                postsSort,
                normalizeText(source.friendsQuery()),
                friendsSort,
                normalizeText(source.teamsQuery()),
                normalizeText(source.teamsRegion()),
                teamsSort
        );
    }

    private static String normalizeVisibility(String value) {
        return normalizeVisibility(value, false);
    }

    private static String normalizeVisibility(String value, boolean allowBlank) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if (normalized.isBlank() && allowBlank) {
            return "";
        }
        if (List.of("PUBLIC", "FRIENDS", "TEAM_ONLY").contains(normalized)) {
            return normalized;
        }
        return allowBlank ? "" : "PUBLIC";
    }

    private static String normalizeChoice(String value, List<String> allowed, String fallback) {
        String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
        if (allowed.contains(normalized)) {
            return normalized;
        }
        return fallback;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String nullableTrimmed(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private static String safeDisplayName(String value) {
        String normalized = normalizeText(value);
        if (!normalized.isBlank()) {
            return normalized;
        }
        return "Player";
    }

    private static String normalizeGender(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MALE", "FEMALE", "OTHER", "UNKNOWN" -> normalized;
            default -> "UNKNOWN";
        };
    }

    private static String fileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx >= filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isUnknownColumnError(SQLException ex) {
        String message = ex.getMessage();
        return "42S22".equals(ex.getSQLState())
                || (message != null && message.toLowerCase(Locale.ROOT).contains("unknown column"));
    }
}
