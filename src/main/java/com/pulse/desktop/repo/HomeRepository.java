package com.pulse.desktop.repo;

import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.HomeHeroStats;
import com.pulse.desktop.model.SimpleCard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HomeRepository {

    public HomeHeroStats heroStats() throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM matches) AS matches_count,
                    (SELECT COUNT(*) FROM tournaments) AS tournaments_count,
                    (SELECT COUNT(*) FROM users) AS players_count
                """;

        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return new HomeHeroStats(
                        rs.getLong("matches_count"),
                        rs.getLong("tournaments_count"),
                        rs.getLong("players_count")
                );
            }
        }

        return new HomeHeroStats(0, 0, 0);
    }

    public List<SimpleCard> weekTournaments(int limit) throws SQLException {
        String sql = """
                SELECT t.tournament_id,
                       t.title,
                       g.name AS game_name,
                       DATE_FORMAT(t.start_date, '%Y-%m-%d') AS start_date,
                       t.status,
                       t.format,
                       t.photo_path AS image_path
                FROM tournaments t
                JOIN games g ON g.game_id = t.game_id
                ORDER BY t.start_date ASC
                LIMIT ?
                """;

        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    cards.add(new SimpleCard(
                            rs.getString("title"),
                            "Jeu: " + rs.getString("game_name") + " | Debut: " + rs.getString("start_date"),
                            rs.getString("status"),
                            rs.getString("format"),
                            "front_tournament_detail",
                            rs.getString("image_path"),
                            "tournament"
                    ));
                }
            }
        }
        return cards;
    }

    public List<SimpleCard> bestProducts(int limit) throws SQLException {
        String sql = """
                SELECT p.product_id,
                       p.name,
                       p.price,
                       p.stock_qty,
                       t.name AS team_name,
                       i.file_url AS image_path
                FROM products p
                JOIN teams t ON t.team_id = p.team_id
                LEFT JOIN (
                    SELECT product_id, MIN(position) AS min_position
                    FROM product_images
                    GROUP BY product_id
                ) pim ON pim.product_id = p.product_id
                LEFT JOIN product_images pi ON pi.product_id = p.product_id AND pi.position = pim.min_position
                LEFT JOIN images i ON i.image_id = pi.image_id
                WHERE p.is_active = 1
                ORDER BY p.updated_at DESC
                LIMIT ?
                """;

        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    cards.add(new SimpleCard(
                            rs.getString("name"),
                            "Equipe: " + rs.getString("team_name"),
                            rs.getBigDecimal("price") + " DT",
                            "Stock: " + rs.getInt("stock_qty"),
                            "front_product_detail",
                            rs.getString("image_path"),
                            "product"
                    ));
                }
            }
        }
        return cards;
    }

    public List<SimpleCard> popularGames(int limit) throws SQLException {
        String sql = """
                SELECT g.game_id,
                       g.name,
                       c.name AS category_name,
                       g.status,
                       g.popularity_score,
                       i.file_url AS image_path
                FROM games g
                JOIN categories c ON c.category_id = g.category_id
                LEFT JOIN images i ON i.image_id = g.cover_image_id
                ORDER BY g.popularity_score DESC, g.views_count DESC
                LIMIT ?
                """;

        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int gameId = rs.getInt("game_id");
                    cards.add(new SimpleCard(
                            rs.getString("name"),
                            "Categorie: " + rs.getString("category_name"),
                            rs.getString("status"),
                            "Popularite: " + rs.getInt("popularity_score"),
                            "front_game_detail::" + gameId,
                            rs.getString("image_path"),
                            "game"
                    ));
                }
            }
        }
        return cards;
    }

    public List<SimpleCard> topTeams(int limit) throws SQLException {
        String sql = """
                SELECT t.team_id,
                       t.name,
                       COALESCE(t.region, 'Region -') AS region,
                       COALESCE(u.display_name, u.username) AS captain_name,
                       i.file_url AS image_path,
                       (
                           SELECT COUNT(*) FROM team_members tm
                           WHERE tm.team_id = t.team_id AND tm.is_active = 1
                       ) AS members_count
                FROM teams t
                JOIN users u ON u.user_id = t.captain_user_id
                LEFT JOIN images i ON i.image_id = t.logo_image_id
                ORDER BY members_count DESC, t.updated_at DESC
                LIMIT ?
                """;

        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    cards.add(new SimpleCard(
                            rs.getString("name"),
                            "Capitaine: " + rs.getString("captain_name"),
                            rs.getString("region"),
                            "Membres: " + rs.getInt("members_count"),
                            "front_team_detail",
                            rs.getString("image_path"),
                            "team"
                    ));
                }
            }
        }
        return cards;
    }

    public List<SimpleCard> latestMatches(int limit) throws SQLException {
        String sql = """
                SELECT m.match_id,
                       COALESCE(m.round_name, 'Round') AS round_name,
                       COALESCE(DATE_FORMAT(m.scheduled_at, '%Y-%m-%d %H:%i'), 'A planifier') AS planned,
                       m.status,
                       t.title AS tournament_title,
                       t.photo_path AS image_path
                FROM matches m
                JOIN tournaments t ON t.tournament_id = m.tournament_id
                ORDER BY m.updated_at DESC
                LIMIT ?
                """;

        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    cards.add(new SimpleCard(
                            rs.getString("round_name"),
                            rs.getString("tournament_title"),
                            rs.getString("status"),
                            rs.getString("planned"),
                            "front_match_detail",
                            rs.getString("image_path"),
                            "tournament"
                    ));
                }
            }
        }
        return cards;
    }

    public List<SimpleCard> globalSearch(String term, int limitEachType) throws SQLException {
        List<SimpleCard> cards = new ArrayList<>();
        if (term == null || term.isBlank()) {
            return cards;
        }

        String like = "%" + term.trim() + "%";
        cards.addAll(searchUsers(like, limitEachType));
        cards.addAll(searchTeams(like, limitEachType));
        cards.addAll(searchTournaments(like, limitEachType));
        cards.addAll(searchGames(like, limitEachType));
        cards.addAll(searchProducts(like, limitEachType));
        return cards;
    }

    public List<SimpleCard> listForRoute(String routeId, int limit) throws SQLException {
        if (routeId.startsWith("front_tournament") || routeId.startsWith("front_organizer_tournament") || routeId.startsWith("front_captain_tournament")) {
            return weekTournaments(limit);
        }
        if (routeId.startsWith("front_game")) {
            return popularGames(limit);
        }
        if (routeId.startsWith("front_team") || routeId.startsWith("front_player") || routeId.contains("my_teams")) {
            return topTeams(limit);
        }
        if (routeId.startsWith("front_shop") || routeId.startsWith("front_product") || routeId.startsWith("front_cart")
                || routeId.startsWith("front_order") || routeId.startsWith("front_checkout")) {
            return bestProducts(limit);
        }
        if (routeId.startsWith("front_match") || routeId.startsWith("front_organizer_match")) {
            return latestMatches(limit);
        }
        if (routeId.startsWith("front_feed") || routeId.startsWith("front_message") || routeId.startsWith("front_friend")
                || routeId.startsWith("front_notification")) {
            return latestSocialCards(limit);
        }
        return latestSocialCards(limit);
    }

    private List<SimpleCard> latestSocialCards(int limit) throws SQLException {
        String sql = """
                SELECT p.post_id,
                       COALESCE(u.display_name, u.username) AS author,
                       LEFT(COALESCE(p.content_text, 'Publication sans texte'), 120) AS excerpt,
                       DATE_FORMAT(p.created_at, '%Y-%m-%d %H:%i') AS created_at,
                       i.file_url AS image_path
                FROM posts p
                JOIN users u ON u.user_id = p.author_user_id
                LEFT JOIN (
                    SELECT post_id, MIN(image_id) AS first_image_id
                    FROM post_images
                    GROUP BY post_id
                ) pii ON pii.post_id = p.post_id
                LEFT JOIN images i ON i.image_id = pii.first_image_id
                WHERE p.is_deleted = 0
                ORDER BY p.created_at DESC
                LIMIT ?
                """;

        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    cards.add(new SimpleCard(
                            "Post de " + rs.getString("author"),
                            rs.getString("excerpt"),
                            "Feed",
                            rs.getString("created_at"),
                            "front_feed",
                            rs.getString("image_path"),
                            "social"
                    ));
                }
            }
        }
        return cards;
    }

    private List<SimpleCard> searchUsers(String like, int limit) throws SQLException {
        String sql = """
                SELECT COALESCE(u.display_name, u.username) AS display_name,
                       u.username,
                       u.role,
                       i.file_url AS image_path
                FROM users u
                LEFT JOIN images i ON i.image_id = u.profile_image_id
                WHERE u.display_name LIKE ? OR u.username LIKE ? OR u.email LIKE ?
                ORDER BY u.updated_at DESC
                LIMIT ?
                """;
        return readUserSearch(sql, like, like, like, limit, "Joueur", "front_player_profile");
    }

    private List<SimpleCard> searchTeams(String like, int limit) throws SQLException {
        String sql = """
                SELECT t.name,
                       COALESCE(t.region, 'Region -') AS region,
                       i.file_url AS image_path
                FROM teams t
                LEFT JOIN images i ON i.image_id = t.logo_image_id
                WHERE t.name LIKE ? OR t.region LIKE ?
                ORDER BY t.updated_at DESC
                LIMIT ?
                """;

        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setString(2, like);
            statement.setInt(3, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    cards.add(new SimpleCard(
                            rs.getString("name"),
                            rs.getString("region"),
                            "Equipe",
                            "Recherche",
                            "front_team_detail",
                            rs.getString("image_path"),
                            "team"
                    ));
                }
            }
        }
        return cards;
    }

    private List<SimpleCard> searchTournaments(String like, int limit) throws SQLException {
        String sql = """
                SELECT title, status, format, photo_path AS image_path
                FROM tournaments
                WHERE title LIKE ?
                ORDER BY start_date DESC
                LIMIT ?
                """;

        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    cards.add(new SimpleCard(
                            rs.getString("title"),
                            "Tournoi",
                            rs.getString("status"),
                            rs.getString("format"),
                            "front_tournament_detail",
                            rs.getString("image_path"),
                            "tournament"
                    ));
                }
            }
        }
        return cards;
    }

    private List<SimpleCard> searchGames(String like, int limit) throws SQLException {
        String sql = """
                SELECT g.game_id, g.name, g.status, i.file_url AS image_path
                FROM games g
                LEFT JOIN images i ON i.image_id = g.cover_image_id
                WHERE g.name LIKE ? OR g.slug LIKE ?
                ORDER BY g.popularity_score DESC
                LIMIT ?
                """;

        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setString(2, like);
            statement.setInt(3, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int gameId = rs.getInt("game_id");
                    cards.add(new SimpleCard(
                            rs.getString("name"),
                            "Jeu",
                            rs.getString("status"),
                            "Recherche",
                            "front_game_detail::" + gameId,
                            rs.getString("image_path"),
                            "game"
                    ));
                }
            }
        }
        return cards;
    }

    private List<SimpleCard> searchProducts(String like, int limit) throws SQLException {
        String sql = """
                SELECT p.name,
                       p.price,
                       p.stock_qty,
                       i.file_url AS image_path
                FROM products p
                LEFT JOIN (
                    SELECT product_id, MIN(position) AS min_position
                    FROM product_images
                    GROUP BY product_id
                ) pim ON pim.product_id = p.product_id
                LEFT JOIN product_images pi ON pi.product_id = p.product_id AND pi.position = pim.min_position
                LEFT JOIN images i ON i.image_id = pi.image_id
                WHERE p.name LIKE ?
                ORDER BY p.updated_at DESC
                LIMIT ?
                """;

        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    cards.add(new SimpleCard(
                            rs.getString("name"),
                            "Produit",
                            rs.getBigDecimal("price") + " DT",
                            "Stock: " + rs.getInt("stock_qty"),
                            "front_product_detail",
                            rs.getString("image_path"),
                            "product"
                    ));
                }
            }
        }
        return cards;
    }

    private List<SimpleCard> readUserSearch(String sql, String p1, String p2, String p3, int limit, String kind, String targetRoute) throws SQLException {
        List<SimpleCard> cards = new ArrayList<>();
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, p1);
            statement.setString(2, p2);
            statement.setString(3, p3);
            statement.setInt(4, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    cards.add(new SimpleCard(
                            rs.getString("display_name"),
                            "@" + rs.getString("username"),
                            kind,
                            rs.getString("role"),
                            targetRoute,
                            rs.getString("image_path"),
                            "member"
                    ));
                }
            }
        }
        return cards;
    }
}
