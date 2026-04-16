-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : jeu. 16 avr. 2026 à 01:06
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `pulsedb`
--

-- --------------------------------------------------------

--
-- Structure de la table `carts`
--

CREATE TABLE `carts` (
  `cart_id` int(10) UNSIGNED NOT NULL,
  `status` varchar(7) NOT NULL DEFAULT 'OPEN',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `locked_at` datetime DEFAULT NULL,
  `user_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `carts`
--

INSERT INTO `carts` (`cart_id`, `status`, `created_at`, `updated_at`, `locked_at`, `user_id`) VALUES
(1, 'OPEN', '2026-01-26 10:00:00', '2026-02-13 12:53:03', NULL, 1),
(2, 'OPEN', '2026-01-27 10:00:00', '2026-02-12 12:41:12', NULL, 2),
(3, 'OPEN', '2026-01-28 10:00:00', '2026-02-12 20:08:56', NULL, 3),
(4, 'OPEN', '2026-01-29 10:00:00', '2026-01-29 10:30:00', NULL, 4),
(5, 'OPEN', '2026-01-30 10:00:00', '2026-01-30 10:30:00', NULL, 5),
(6, 'OPEN', '2026-01-31 10:00:00', '2026-01-31 10:30:00', NULL, 6),
(7, 'LOCKED', '2026-02-01 10:00:00', '2026-02-01 10:30:00', '2026-02-01 14:00:00', 7),
(8, 'LOCKED', '2026-02-02 10:00:00', '2026-02-02 10:30:00', '2026-02-02 14:00:00', 8),
(9, 'ORDERED', '2026-02-03 10:00:00', '2026-02-03 10:30:00', '2026-02-03 14:00:00', 9),
(10, 'ORDERED', '2026-02-04 10:00:00', '2026-02-04 10:30:00', '2026-02-04 14:00:00', 10),
(11, 'OPEN', '2026-03-06 01:30:59', '2026-03-06 01:30:59', NULL, 15);

-- --------------------------------------------------------

--
-- Structure de la table `cart_items`
--

CREATE TABLE `cart_items` (
  `quantity` int(10) UNSIGNED NOT NULL DEFAULT 1,
  `unit_price_at_add` decimal(10,2) NOT NULL,
  `added_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `cart_id` int(10) UNSIGNED NOT NULL,
  `product_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `cart_items`
--

INSERT INTO `cart_items` (`quantity`, `unit_price_at_add`, `added_at`, `updated_at`, `cart_id`, `product_id`) VALUES
(14, 1000.00, '2026-02-13 12:51:35', '2026-02-13 12:53:03', 1, 12),
(1, 55.00, '2026-02-11 21:00:00', '2026-02-12 12:41:12', 2, 2),
(1, 50.00, '2026-02-12 14:27:04', '2026-02-12 14:27:04', 3, 1),
(1, 55.00, '2026-02-12 20:05:32', '2026-02-12 20:05:32', 3, 2),
(1, 60.00, '2026-02-11 22:00:00', '2026-02-11 23:00:00', 3, 3),
(2, 1000.00, '2026-02-12 20:05:26', '2026-02-12 20:08:56', 3, 11),
(2, 65.00, '2026-02-11 23:00:00', '2026-02-12 00:00:00', 4, 4),
(3, 70.00, '2026-02-12 00:00:00', '2026-02-12 01:00:00', 5, 5),
(1, 75.00, '2026-02-12 01:00:00', '2026-02-12 02:00:00', 6, 6),
(2, 80.00, '2026-02-12 02:00:00', '2026-02-12 03:00:00', 7, 7),
(3, 85.00, '2026-02-12 03:00:00', '2026-02-12 04:00:00', 8, 8),
(1, 90.00, '2026-02-12 04:00:00', '2026-02-12 05:00:00', 9, 9),
(2, 95.00, '2026-02-12 05:00:00', '2026-02-12 06:00:00', 10, 10),
(1, 1000.00, '2026-03-06 01:30:59', '2026-03-06 01:30:59', 11, 11);

-- --------------------------------------------------------

--
-- Structure de la table `categories`
--

CREATE TABLE `categories` (
  `category_id` int(10) UNSIGNED NOT NULL,
  `name` varchar(80) NOT NULL,
  `description` longtext DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `slug` varchar(191) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `categories`
--

INSERT INTO `categories` (`category_id`, `name`, `description`, `created_at`, `slug`) VALUES
(1, 'FPS', 'Categorie FPS', '2026-01-04 10:00:00', 'fps'),
(2, 'MOBA', 'Categorie MOBA', '2026-01-05 10:00:00', 'moba'),
(3, 'Battle Royale', 'Categorie Battle Royale', '2026-01-06 10:00:00', 'battle-royale'),
(4, 'Sports', 'Categorie Sports', '2026-01-07 10:00:00', 'sports'),
(5, 'Racing', 'Categorie Racing', '2026-01-08 10:00:00', 'racing'),
(6, 'Fighting', 'Categorie Fighting', '2026-01-09 10:00:00', 'fighting'),
(7, 'RTS', 'Categorie RTS', '2026-01-10 10:00:00', 'rts'),
(8, 'Card Game', 'Categorie Card Game', '2026-01-11 10:00:00', 'card-game'),
(9, 'MMORPG', 'Categorie MMORPG', '2026-01-12 10:00:00', 'mmorpg'),
(10, 'Simulation', 'Categorie Simulation', '2026-01-13 10:00:00', 'simulation'),
(11, 'DRISSS', 'test', '2026-02-13 12:38:46', 'drisss');

-- --------------------------------------------------------

--
-- Structure de la table `comments`
--

CREATE TABLE `comments` (
  `comment_id` int(10) UNSIGNED NOT NULL,
  `content_text` longtext NOT NULL,
  `is_deleted` tinyint(4) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `post_id` int(10) UNSIGNED DEFAULT NULL,
  `author_user_id` int(10) UNSIGNED NOT NULL,
  `parent_comment_id` int(10) UNSIGNED DEFAULT NULL,
  `product_id` int(10) UNSIGNED DEFAULT NULL,
  `rating` smallint(5) UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `comments`
--

INSERT INTO `comments` (`comment_id`, `content_text`, `is_deleted`, `created_at`, `updated_at`, `post_id`, `author_user_id`, `parent_comment_id`, `product_id`, `rating`) VALUES
(3, 'Commentaire 3', 0, '2026-02-10 21:00:00', '2026-02-10 21:10:00', 2, 4, NULL, NULL, NULL),
(4, 'Commentaire 4', 0, '2026-02-10 22:00:00', '2026-02-10 22:10:00', 2, 5, 3, NULL, NULL),
(5, 'Commentaire 5', 0, '2026-02-10 23:00:00', '2026-02-10 23:10:00', 3, 6, NULL, NULL, NULL),
(6, 'Commentaire 6', 0, '2026-02-11 00:00:00', '2026-02-11 00:10:00', 4, 7, NULL, NULL, NULL),
(7, 'Commentaire 7', 0, '2026-02-11 01:00:00', '2026-02-11 01:10:00', 5, 8, NULL, NULL, NULL),
(8, 'Commentaire 8', 0, '2026-02-11 02:00:00', '2026-02-11 02:10:00', 6, 9, NULL, NULL, NULL),
(9, 'Commentaire 9', 0, '2026-02-11 03:00:00', '2026-02-11 03:10:00', 7, 10, NULL, NULL, NULL),
(10, 'Commentaire 10', 0, '2026-02-11 04:00:00', '2026-02-11 04:10:00', 8, 1, NULL, NULL, NULL),
(11, 'ilyes', 0, '2026-02-12 04:17:16', '2026-02-12 04:17:16', 12, 2, NULL, NULL, NULL),
(12, 'lassssssssss', 0, '2026-02-12 04:17:40', '2026-02-12 04:17:40', 12, 2, NULL, NULL, NULL),
(13, 'mahlek ye tofla', 0, '2026-02-12 12:43:47', '2026-02-12 12:43:47', 16, 2, NULL, NULL, NULL),
(17, 'TEST', 0, '2026-02-27 09:33:51', '2026-02-27 09:33:51', 25, 15, NULL, NULL, NULL),
(18, 'test', 0, '2026-03-06 01:36:31', '2026-03-06 01:36:31', NULL, 15, NULL, 11, 5);

-- --------------------------------------------------------

--
-- Structure de la table `doctrine_migration_versions`
--

CREATE TABLE `doctrine_migration_versions` (
  `version` varchar(191) NOT NULL,
  `executed_at` datetime DEFAULT NULL,
  `execution_time` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `doctrine_migration_versions`
--

INSERT INTO `doctrine_migration_versions` (`version`, `executed_at`, `execution_time`) VALUES
('DoctrineMigrations\\Version20260211112802', '2026-02-11 12:28:17', 6105),
('DoctrineMigrations\\Version20260211222000', '2026-02-11 23:30:51', 85),
('DoctrineMigrations\\Version20260211230000', '2026-02-11 23:54:11', 13),
('DoctrineMigrations\\Version20260226103000', '2026-02-26 23:42:32', 474),
('DoctrineMigrations\\Version20260226114500', '2026-02-26 23:43:50', 110),
('DoctrineMigrations\\Version20260227130000', '2026-02-27 01:56:20', 21),
('DoctrineMigrations\\Version20260227143000', '2026-02-27 07:03:21', 158),
('DoctrineMigrations\\Version20260227160000', '2026-02-27 08:18:26', 659),
('DoctrineMigrations\\Version20260306120000', '2026-03-06 01:33:44', 413);

-- --------------------------------------------------------

--
-- Structure de la table `friendships`
--

CREATE TABLE `friendships` (
  `created_at` datetime NOT NULL,
  `user1_id` int(10) UNSIGNED NOT NULL,
  `user2_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `friendships`
--

INSERT INTO `friendships` (`created_at`, `user1_id`, `user2_id`) VALUES
('2026-01-28 10:00:00', 1, 2),
('2026-01-29 10:00:00', 1, 3),
('2026-01-30 10:00:00', 1, 4),
('2026-01-31 10:00:00', 2, 3),
('2026-02-01 10:00:00', 2, 4),
('2026-02-12 18:03:26', 2, 10),
('2026-02-02 10:00:00', 3, 4),
('2026-02-03 10:00:00', 5, 6),
('2026-02-04 10:00:00', 5, 7),
('2026-02-05 10:00:00', 6, 7),
('2026-02-06 10:00:00', 8, 9);

-- --------------------------------------------------------

--
-- Structure de la table `friend_requests`
--

CREATE TABLE `friend_requests` (
  `request_id` int(10) UNSIGNED NOT NULL,
  `status` varchar(9) NOT NULL DEFAULT 'PENDING',
  `request_message` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `responded_at` datetime DEFAULT NULL,
  `from_user_id` int(10) UNSIGNED NOT NULL,
  `to_user_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `friend_requests`
--

INSERT INTO `friend_requests` (`request_id`, `status`, `request_message`, `created_at`, `responded_at`, `from_user_id`, `to_user_id`) VALUES
(1, 'ACCEPTED', 'Demande 1', '2026-02-03 10:00:00', '2026-02-12 04:45:51', 1, 2),
(2, 'ACCEPTED', 'Demande 2', '2026-02-04 10:00:00', '2026-02-04 16:00:00', 2, 3),
(3, 'REFUSED', 'Demande 3', '2026-02-05 10:00:00', '2026-02-05 16:00:00', 3, 4),
(4, 'CANCELLED', 'Demande 4', '2026-02-06 10:00:00', '2026-02-06 16:00:00', 4, 5),
(5, 'PENDING', 'Demande 5', '2026-02-07 10:00:00', NULL, 5, 6),
(6, 'ACCEPTED', 'Demande 6', '2026-02-08 10:00:00', '2026-02-08 16:00:00', 6, 7),
(7, 'PENDING', 'Demande 7', '2026-02-09 10:00:00', NULL, 7, 8),
(8, 'ACCEPTED', 'Demande 8', '2026-02-10 10:00:00', '2026-02-10 16:00:00', 8, 9),
(9, 'REFUSED', 'Demande 9', '2026-02-11 10:00:00', '2026-02-11 16:00:00', 9, 10),
(10, 'CANCELLED', 'Demande 10', '2026-02-12 10:00:00', '2026-02-12 18:03:38', 10, 1),
(11, 'ACCEPTED', NULL, '2026-02-12 11:54:17', '2026-02-12 18:03:26', 2, 10),
(12, 'PENDING', NULL, '2026-02-12 11:54:40', NULL, 2, 9),
(15, 'PENDING', NULL, '2026-02-12 23:55:13', NULL, 5, 1),
(17, 'PENDING', NULL, '2026-02-27 05:56:07', NULL, 15, 5),
(18, 'PENDING', NULL, '2026-02-27 09:40:56', NULL, 15, 16);

-- --------------------------------------------------------

--
-- Structure de la table `games`
--

CREATE TABLE `games` (
  `game_id` int(10) UNSIGNED NOT NULL,
  `name` varchar(120) NOT NULL,
  `description` longtext DEFAULT NULL,
  `publisher` varchar(120) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `category_id` int(10) UNSIGNED NOT NULL,
  `cover_image_id` int(10) UNSIGNED DEFAULT NULL,
  `slug` varchar(191) NOT NULL,
  `status` varchar(10) NOT NULL DEFAULT 'DRAFT',
  `popularity_score` int(10) UNSIGNED NOT NULL DEFAULT 0,
  `views_count` int(10) UNSIGNED NOT NULL DEFAULT 0,
  `favorites_count` int(10) UNSIGNED NOT NULL DEFAULT 0,
  `cover_name` varchar(255) DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `games`
--

INSERT INTO `games` (`game_id`, `name`, `description`, `publisher`, `created_at`, `category_id`, `cover_image_id`, `slug`, `status`, `popularity_score`, `views_count`, `favorites_count`, `cover_name`, `reviewed_at`) VALUES
(1, 'Valorant', 'Valorant jeu test', 'Publisher 01', '2026-01-24 10:00:00', 1, 1, 'valorant-1', 'DRAFT', 19, 7, 0, 'seed_01.jpg', NULL),
(2, 'League of Legends', 'League of Legends jeu test', 'Publisher 02', '2026-01-25 10:00:00', 2, 2, 'league-of-legends-2', 'DRAFT', 20, 1, 1, 'seed_02.jpg', NULL),
(3, 'Fortnite', 'Fortnite jeu test', 'Publisher 03', '2026-01-26 10:00:00', 3, 3, 'fortnite-3', 'DRAFT', 15, 0, 0, 'seed_03.jpg', NULL),
(4, 'EA FC 26', 'EA FC 26 jeu test', 'Publisher 04', '2026-01-27 10:00:00', 4, 4, 'ea-fc-26-4', 'DRAFT', 19, 1, 1, 'seed_04.jpg', NULL),
(5, 'Gran Turismo 7', 'Gran Turismo 7 jeu test', 'Publisher 05', '2026-01-28 10:00:00', 5, 5, 'gran-turismo-7-5', 'DRAFT', 0, 0, 0, 'seed_05.jpg', NULL),
(6, 'Street Fighter 6', 'Street Fighter 6 jeu test', 'Publisher 06', '2026-01-29 10:00:00', 6, 6, 'street-fighter-6-6', 'DRAFT', 0, 0, 0, 'seed_06.jpg', NULL),
(7, 'StarCraft II', 'StarCraft II jeu test', 'Publisher 07', '2026-01-30 10:00:00', 7, 7, 'starcraft-ii-7', 'DRAFT', 25, 1, 0, 'seed_07.jpg', NULL),
(8, 'Hearthstone', 'Hearthstone jeu test', 'Publisher 08', '2026-01-31 10:00:00', 8, 8, 'hearthstone-8', 'DRAFT', 5, 0, 0, 'seed_08.jpg', NULL),
(9, 'World of Warcraft', 'World of Warcraft jeu test', 'Publisher 09', '2026-02-01 10:00:00', 9, 9, 'world-of-warcraft-9', 'DRAFT', 5, 0, 0, 'seed_09.jpg', NULL),
(10, 'Football Manager 2026', 'Football Manager 2026 jeu test', 'Publisher 10', '2026-02-02 10:00:00', 10, 10, 'football-manager-2026-10', 'DRAFT', 15, 0, 0, 'seed_10.jpg', NULL),
(11, 'FRIVVV', 'AHSE JEUUUU', 'MEE', '2026-02-13 12:38:00', 3, 39, 'frivvv-11', 'DRAFT', 0, 0, 0, 'game_cover_9ad6d4e167e5f2775564.jpg', NULL),
(13, 'meriem', 'meriem est un titre Selectionner axe sur la competition, la progression et les tournois communautaires. Le catalogue vise une experience stable pour les equipes et organisateurs.', 'Independant', '2026-02-27 08:23:52', 2, 56, 'pending-game-325205', 'PENDING', 0, 0, 0, 'WhatsApp Image 2026-01-30 at 10.28.53 AM.jpeg', '2026-02-27 08:23:52'),
(14, 'idri', 'idri cible une experience FPS competitive et accessible. Titre edite par Independant, ideal pour des tournois frequents.', 'Independant', '2026-02-27 08:26:37', 1, NULL, 'pending-game-912560', 'PENDING', 0, 0, 0, NULL, '2026-02-27 08:26:37'),
(15, 'hr', NULL, 'MEE', '2026-02-27 10:10:00', 8, 62, 'pending-game-218336', 'DRAFT', 0, 0, 0, 'WhatsApp Image 2026-01-30 at 10.28.53 AM.jpeg', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `game_favorites`
--

CREATE TABLE `game_favorites` (
  `created_at` datetime NOT NULL,
  `user_id` int(10) UNSIGNED NOT NULL,
  `game_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `game_favorites`
--

INSERT INTO `game_favorites` (`created_at`, `user_id`, `game_id`) VALUES
('2026-02-27 08:29:33', 1, 2),
('2026-02-27 10:12:16', 3, 4);

-- --------------------------------------------------------

--
-- Structure de la table `images`
--

CREATE TABLE `images` (
  `image_id` int(10) UNSIGNED NOT NULL,
  `file_url` varchar(500) NOT NULL,
  `mime_type` varchar(60) NOT NULL,
  `size_bytes` bigint(20) UNSIGNED NOT NULL,
  `width` int(10) UNSIGNED DEFAULT NULL,
  `height` int(10) UNSIGNED DEFAULT NULL,
  `alt_text` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `uploaded_by_user_id` int(10) UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `images`
--

INSERT INTO `images` (`image_id`, `file_url`, `mime_type`, `size_bytes`, `width`, `height`, `alt_text`, `created_at`, `uploaded_by_user_id`) VALUES
(1, 'uploads/seeds/seed_01.jpg', 'image/jpeg', 133024, 1200, 800, 'Seed 01', '2026-02-03 10:00:00', 1),
(2, 'uploads/seeds/seed_02.jpg', 'image/jpeg', 171553, 1200, 800, 'Seed 02', '2026-02-04 10:00:00', 2),
(3, 'uploads/seeds/seed_03.jpg', 'image/jpeg', 60391, 1200, 800, 'Seed 03', '2026-02-05 10:00:00', 3),
(4, 'uploads/seeds/seed_04.jpg', 'image/jpeg', 87155, 1200, 800, 'Seed 04', '2026-02-06 10:00:00', 4),
(5, 'uploads/seeds/seed_05.jpg', 'image/jpeg', 125916, 1200, 800, 'Seed 05', '2026-02-07 10:00:00', 5),
(6, 'uploads/seeds/seed_06.jpg', 'image/jpeg', 107030, 1200, 800, 'Seed 06', '2026-02-08 10:00:00', 6),
(7, 'uploads/seeds/seed_07.jpg', 'image/jpeg', 140013, 1200, 800, 'Seed 07', '2026-02-09 10:00:00', 7),
(8, 'uploads/seeds/seed_08.jpg', 'image/jpeg', 89216, 1200, 800, 'Seed 08', '2026-02-10 10:00:00', 8),
(9, 'uploads/seeds/seed_09.jpg', 'image/jpeg', 63628, 1200, 800, 'Seed 09', '2026-02-11 10:00:00', 9),
(10, 'uploads/seeds/seed_10.jpg', 'image/jpeg', 36425, 1200, 800, 'Seed 10', '2026-02-12 10:00:00', 10),
(11, 'uploads/profiles/profile_9243ae6482b042fe.jpg', 'image/jpeg', 28339, 391, 514, 'Photo profil de lassss', '2026-02-12 04:49:31', 2),
(12, 'uploads/posts/post_1adf56f29bfb1a20d0c7.png', 'image/png', 2244485, 1024, 1536, 'Image du post de lassss', '2026-02-12 12:38:24', 2),
(13, 'uploads/posts/post_7834c424ca40cf4a0438.jpg', 'image/jpeg', 76631, 1280, 720, 'Image du post de lassss', '2026-02-12 12:43:09', 2),
(14, 'uploads/posts/post_bf4c96b0cf2e6dc34e3c.jpg', 'image/jpeg', 14344, 406, 419, 'Image du post de lassss', '2026-02-12 12:46:57', 2),
(15, 'uploads/posts/post_8a1dcbdfede35fc12c54.jpg', 'image/jpeg', 76631, 1280, 720, 'Image du post de lassss', '2026-02-12 12:46:57', 2),
(16, 'uploads/posts/post_e7f4cf1fd334115715a2.jpg', 'image/jpeg', 28339, 391, 514, 'Image du post de lassss', '2026-02-12 12:46:57', 2),
(17, 'uploads/posts/post_4943d5113d1706f5d7bd.jpg', 'image/jpeg', 12185, 416, 468, 'Image du post de lassss', '2026-02-12 12:46:57', 2),
(18, 'uploads/teams/team_logo_7a45900be6ae2bde3151.png', 'image/png', 606964, 802, 649, 'Logo equipe team ilyes', '2026-02-12 14:21:48', 3),
(19, 'uploads/teams/team_logo_bae8f45f98a2be314c73.webp', 'image/webp', 259584, 1024, 1024, 'Logo equipe Team Pulse 01', '2026-02-12 14:22:21', 3),
(20, 'uploads/products/product_image_6dc38281c3ae28bd3738.png', 'image/png', 795052, 800, 800, 'Produit casque', '2026-02-12 14:26:37', 3),
(21, 'uploads/products/product_image_3fd0fe6f9e9c77e8ea51.png', 'image/png', 68738, 762, 507, 'Produit casque', '2026-02-12 14:28:00', 3),
(22, 'uploads/products/product_image_a46502d12b9984164294.png', 'image/png', 99681, 973, 682, 'Produit casque', '2026-02-12 14:28:00', 3),
(23, 'uploads/products/product_image_bd6686c2598d4e8a1dfe.png', 'image/png', 44934, 733, 486, 'Produit casque', '2026-02-12 14:28:00', 3),
(24, 'uploads/products/product_image_7ea8c9f4a29955ef5bb9.png', 'image/png', 54570, 738, 484, 'Produit casque', '2026-02-12 14:28:00', 3),
(25, 'uploads/products/product_image_560a2035b47bb4e92f92.png', 'image/png', 51220, 738, 484, 'Produit casque', '2026-02-12 14:28:00', 3),
(26, 'uploads/products/product_image_d5a3e69bbaf813391883.png', 'image/png', 185733, 1192, 861, 'Produit casque', '2026-02-12 14:28:00', 3),
(27, 'uploads/profiles/profile_1132ee5cbbc8381b.jpg', 'image/jpeg', 693494, 1536, 2048, 'Photo profil de User 01', '2026-02-12 18:20:28', 1),
(28, 'uploads/posts/post_05b9f3edded137956532.jpg', 'image/jpeg', 28339, 391, 514, 'Image du post de User 01', '2026-02-12 18:21:06', 1),
(29, 'uploads/posts/post_16399e80ca918460a0ab.jpg', 'image/jpeg', 14344, 406, 419, 'Image du post de 3OSFOUR', '2026-02-12 18:43:14', NULL),
(30, 'uploads/posts/post_0f3cf9d8d0391ca7b8ab.jpg', 'image/jpeg', 76631, 1280, 720, 'Image du post de 3OSFOUR', '2026-02-12 18:43:14', NULL),
(31, 'uploads/posts/post_bd095277f8f67e65a835.jpg', 'image/jpeg', 28339, 391, 514, 'Image du post de 3OSFOUR', '2026-02-12 18:43:14', NULL),
(32, 'uploads/posts/post_25f1cebbc167d61238ba.jpg', 'image/jpeg', 12185, 416, 468, 'Image du post de 3OSFOUR', '2026-02-12 18:43:14', NULL),
(33, 'uploads/profiles/profile_980dbf41c257fb97.jpg', 'image/jpeg', 156327, 1170, 1560, 'Photo profil de 3OSFOUR', '2026-02-12 18:44:23', NULL),
(34, 'C:\\Users\\ilyes\\Downloads\\339499497_1183150892342409_1448347523191946492_n.jpg', 'image/jpeg', 0, NULL, NULL, NULL, '2026-02-12 22:01:05', 1),
(35, 'uploads/posts/post_77422119c59923e8ab4e.jpg', 'image/jpeg', 646807, 2000, 1125, 'Image du post de User 05', '2026-02-12 23:55:02', 5),
(36, 'uploads/profiles/profile_266c81ac7da43fe8.png', 'image/png', 2244485, 1024, 1536, 'Photo profil de 3OSFOUR', '2026-02-13 11:20:08', NULL),
(37, 'uploads/teams/team_logo_322c42602ee73ce1f259.png', 'image/png', 1330193, 1535, 861, 'Logo equipe ace', '2026-02-13 12:24:14', 6),
(38, 'uploads/admin/teams/team_logo_2fb85cdae964a30a67e4.jpg', 'image/jpeg', 12185, 416, 468, 'Logo equipe trahhh', '2026-02-13 12:32:42', 1),
(39, 'uploads/admin/games/game_cover_9ad6d4e167e5f2775564.jpg', 'image/jpeg', 646807, 2000, 1125, 'Cover jeu FRIVVV', '2026-02-13 12:38:00', 1),
(40, 'uploads/admin/games/game_cover_04a0ee4939bc4363e11d.jpg', 'image/jpeg', 87730, 736, 414, 'Cover jeu TEST', '2026-02-13 12:39:34', 1),
(41, 'uploads/products/product_image_c23da03c00c5a4bd1418.png', 'image/png', 288869, 541, 461, 'Produit Maryoul', '2026-02-13 12:47:48', 6),
(42, 'uploads/products/product_image_25d344ae992e38c40402.png', 'image/png', 795052, 800, 800, 'Produit casque', '2026-02-13 12:48:34', 6),
(43, 'uploads/products/product_image_19a6f752cd2f62569fa4.png', 'image/png', 606964, 802, 649, 'Produit casqueeeee', '2026-02-13 12:49:49', 6),
(44, 'uploads/products/product_image_9060b6bc56afd7467b54.png', 'image/png', 2784362, 1024, 1536, 'Produit casqueeeee', '2026-02-13 12:49:49', 6),
(45, 'uploads/products/product_image_e7cb6ccc52d468457b80.png', 'image/png', 3213724, 1024, 1536, 'Produit casqueeeee', '2026-02-13 12:49:49', 6),
(46, 'uploads/posts/post_3dae4a799d67166477f6.webp', 'image/webp', 780092, 1792, 1024, 'Image du post de starzoukaaaa', '2026-02-13 13:06:19', NULL),
(47, 'uploads/posts/post_13fcfadf78396a7410b3.jpg', 'image/jpeg', 52053, 612, 408, 'Image du post de starzoukaaaa', '2026-02-13 13:06:19', NULL),
(48, 'uploads/posts/post_486bf5bcd9b37494a80a.jpg', 'image/jpeg', 99495, 564, 644, 'Image du post de starzoukaaaa', '2026-02-13 13:06:19', NULL),
(49, 'uploads/profiles/profile_1357e706623f8f35.png', 'image/png', 1886815, 1024, 1024, 'Photo profil de 5outifat', '2026-02-26 22:32:38', NULL),
(50, 'uploads/posts/post_ba714d2419b5b9c85575.jpg', 'image/jpeg', 14344, 406, 419, 'Image du post de 5outifat', '2026-02-26 22:36:05', NULL),
(51, 'uploads/posts/post_e529fb078e2e9e14470b.jpg', 'image/jpeg', 76631, 1280, 720, 'Image du post de 5outifat', '2026-02-26 22:36:05', NULL),
(52, 'uploads/posts/post_54d0b12089e70566744e.jpg', 'image/jpeg', 28339, 391, 514, 'Image du post de 5outifat', '2026-02-26 22:36:05', NULL),
(53, 'uploads/posts/post_e9a89aa7e74397e33a68.jpg', 'image/jpeg', 12185, 416, 468, 'Image du post de 5outifat', '2026-02-26 22:36:05', NULL),
(54, 'uploads/posts/post_790d08a50362ab217a83.png', 'image/png', 2784362, 1024, 1536, 'Image du post de 5outifat', '2026-02-26 22:36:50', NULL),
(55, 'uploads/profiles/profile_4c8a0fd6d51efc5c.png', 'image/png', 2917929, 1024, 1536, 'Photo profil de 3asfouraaa', '2026-02-27 04:42:43', 15),
(56, 'uploads/admin/games/game_cover_f79925682d821df4f9ee.jpg', 'image/jpeg', 76631, 1280, 720, 'Cover jeu meriem', '2026-02-27 08:23:52', 1),
(57, 'uploads/posts/post_0064b1be741ae723f16a.png', 'image/png', 3305234, 1024, 1536, 'Image du post de 3asfouraaa', '2026-02-27 09:33:19', 15),
(58, 'uploads/posts/post_bd152c74378a2fedca4a.png', 'image/png', 2917929, 1024, 1536, 'Image du post de 3asfouraaa', '2026-02-27 09:33:19', 15),
(59, 'uploads/posts/post_894f4e50ae1250956390.png', 'image/png', 3213251, 1024, 1536, 'Image du post de 3asfouraaa', '2026-02-27 09:33:19', 15),
(60, 'uploads/posts/post_a12322d07d91ce52c248.png', 'image/png', 3706101, 1024, 1536, 'Image du post de 3asfouraaa', '2026-02-27 09:33:19', 15),
(61, 'uploads/teams/team_logo_2dadf7c5de566bad6f39.jpg', 'image/jpeg', 14344, 406, 419, 'Logo equipe team ilyes', '2026-02-27 10:01:51', 3),
(62, 'uploads/admin/games/game_cover_9a8f2f9376dd828e0fbe.jpg', 'image/jpeg', 76631, 1280, 720, 'Cover jeu hr', '2026-02-27 10:10:00', 1),
(63, 'uploads/profiles/profile_257ef9dc5dfb470481b537814dc287b9.png', 'image/png', 2879254, 1024, 1536, 'Photo profil de lasssslousa', '2026-04-15 23:50:56', 21);

-- --------------------------------------------------------

--
-- Structure de la table `matches`
--

CREATE TABLE `matches` (
  `match_id` int(10) UNSIGNED NOT NULL,
  `scheduled_at` datetime DEFAULT NULL,
  `round_name` varchar(80) DEFAULT NULL,
  `best_of` smallint(5) UNSIGNED DEFAULT NULL,
  `status` varchar(9) NOT NULL DEFAULT 'SCHEDULED',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `tournament_id` int(10) UNSIGNED NOT NULL,
  `result_submitted_by_user_id` int(10) UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `matches`
--

INSERT INTO `matches` (`match_id`, `scheduled_at`, `round_name`, `best_of`, `status`, `created_at`, `updated_at`, `tournament_id`, `result_submitted_by_user_id`) VALUES
(1, '2026-02-13 10:00:00', 'Round 01', 1, 'FINISHED', '2026-02-11 17:00:00', '2026-02-11 18:00:00', 1, 2),
(2, '2026-02-14 10:00:00', 'Round 02', 3, 'ONGOING', '2026-02-11 18:00:00', '2026-02-11 19:00:00', 2, NULL),
(3, '2026-02-15 10:00:00', 'Round 03', 5, 'SCHEDULED', '2026-02-11 19:00:00', '2026-02-11 20:00:00', 3, NULL),
(4, '2026-02-16 10:00:00', 'Round 04', 1, 'ONGOING', '2026-02-11 20:00:00', '2026-02-12 17:24:44', 4, NULL),
(7, '2026-02-19 10:00:00', 'Round 07', 1, 'SCHEDULED', '2026-02-11 23:00:00', '2026-02-12 00:00:00', 7, NULL),
(8, '2026-02-20 10:00:00', 'Round 08', 3, 'ONGOING', '2026-02-12 00:00:00', '2026-02-12 01:00:00', 8, NULL),
(9, '2026-02-21 10:00:00', 'Round 09', 5, 'FINISHED', '2026-02-12 01:00:00', '2026-02-12 02:00:00', 9, 9),
(10, '2026-02-22 10:00:00', 'Round 10', 1, 'SCHEDULED', '2026-02-12 02:00:00', '2026-02-12 03:00:00', 10, NULL),
(11, NULL, NULL, NULL, 'SCHEDULED', '2026-02-12 02:07:10', '2026-02-12 02:07:26', 4, NULL),
(12, '2026-02-05 17:25:00', 'Round 13', NULL, 'SCHEDULED', '2026-02-12 17:25:52', '2026-02-12 17:25:52', 4, NULL),
(13, '2026-02-12 17:39:00', 'azer', 5, 'CANCELLED', '2026-02-12 17:39:09', '2026-02-12 17:39:42', 1, NULL),
(14, '2026-02-20 17:48:00', 'Round 04', 5, 'FINISHED', '2026-02-12 17:49:01', '2026-02-12 17:49:01', 4, 1),
(16, NULL, 'jhkjk', 1, 'FINISHED', '2026-02-13 12:04:11', '2026-02-13 12:04:11', 10, 1),
(17, '2026-02-26 20:56:28', 'Generated Round 3', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 1, NULL),
(18, '2026-02-26 21:56:28', 'Generated Round 4', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 1, NULL),
(19, '2026-02-26 22:56:28', 'Generated Round 5', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 1, NULL),
(20, '2026-02-27 01:56:28', 'Generated Round 2', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 2, NULL),
(21, '2026-02-27 02:56:28', 'Generated Round 3', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 2, NULL),
(22, '2026-02-27 03:56:28', 'Generated Round 4', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 2, NULL),
(23, '2026-02-27 04:56:28', 'Generated Round 5', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 2, NULL),
(24, '2026-02-27 07:56:28', 'Generated Round 2', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 3, NULL),
(25, '2026-02-27 08:56:28', 'Generated Round 3', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 3, NULL),
(26, '2026-02-27 09:56:28', 'Generated Round 4', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 3, NULL),
(27, '2026-02-27 10:56:28', 'Generated Round 5', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 3, NULL),
(28, '2026-02-27 16:56:28', 'Generated Round 5', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 4, NULL),
(29, '2026-02-26 13:56:28', 'Generated Round 2', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 7, NULL),
(30, '2026-02-26 14:56:28', 'Generated Round 3', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 7, NULL),
(31, '2026-02-26 15:56:28', 'Generated Round 4', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 7, NULL),
(32, '2026-02-26 16:56:28', 'Generated Round 5', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 7, NULL),
(33, '2026-02-26 19:56:28', 'Generated Round 2', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 8, NULL),
(34, '2026-02-26 20:56:28', 'Generated Round 3', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 8, NULL),
(35, '2026-02-26 21:56:28', 'Generated Round 4', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 8, NULL),
(36, '2026-02-26 22:56:28', 'Generated Round 5', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 8, NULL),
(37, '2026-02-27 01:56:28', 'Generated Round 2', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 9, NULL),
(38, '2026-02-27 02:56:28', 'Generated Round 3', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 9, NULL),
(39, '2026-02-27 03:56:28', 'Generated Round 4', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 9, NULL),
(40, '2026-02-27 04:56:28', 'Generated Round 5', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 9, NULL),
(41, '2026-02-27 08:56:28', 'Generated Round 3', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 10, NULL),
(42, '2026-02-27 09:56:28', 'Generated Round 4', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 10, NULL),
(43, '2026-02-27 10:56:28', 'Generated Round 5', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 10, NULL),
(44, '2026-02-27 12:56:28', 'Generated Round 1', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 11, NULL),
(45, '2026-02-27 13:56:28', 'Generated Round 2', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 11, NULL),
(46, '2026-02-27 14:56:28', 'Generated Round 3', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 11, NULL),
(47, '2026-02-27 15:56:28', 'Generated Round 4', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 11, NULL),
(48, '2026-02-27 16:56:28', 'Generated Round 5', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 11, NULL),
(49, '2026-02-27 18:56:28', 'Generated Round 1', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 12, NULL),
(50, '2026-02-27 19:56:28', 'Generated Round 2', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 12, NULL),
(51, '2026-02-27 20:56:28', 'Generated Round 3', 1, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 12, NULL),
(52, '2026-02-27 21:56:28', 'Generated Round 4', 3, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 12, NULL),
(53, '2026-02-27 22:56:28', 'Generated Round 5', 5, 'FINISHED', '2026-02-26 11:56:28', '2026-02-26 11:56:28', 12, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `match_teams`
--

CREATE TABLE `match_teams` (
  `score` int(10) UNSIGNED DEFAULT NULL,
  `is_winner` tinyint(4) DEFAULT NULL,
  `match_id` int(10) UNSIGNED NOT NULL,
  `team_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `match_teams`
--

INSERT INTO `match_teams` (`score`, `is_winner`, `match_id`, `team_id`) VALUES
(2, 1, 1, 1),
(1, 0, 1, 2),
(NULL, NULL, 2, 1),
(NULL, NULL, 2, 2),
(NULL, NULL, 3, 3),
(NULL, NULL, 3, 4),
(2, 1, 4, 5),
(0, 0, 4, 6),
(NULL, 0, 4, 7),
(NULL, 0, 4, 8),
(4, 1, 11, 7),
(1, 0, 11, 8),
(NULL, 0, 12, 7),
(NULL, 0, 12, 8),
(2, 1, 13, 1),
(1, 0, 13, 2),
(NULL, 0, 14, 4),
(NULL, 0, 14, 5),
(NULL, 0, 14, 7),
(NULL, 0, 16, 10),
(NULL, 0, 16, 11),
(2, 1, 17, 1),
(0, 0, 17, 2),
(3, 1, 18, 1),
(2, 0, 18, 2),
(1, 1, 19, 1),
(0, 0, 19, 2),
(0, 0, 20, 2),
(2, 1, 20, 4),
(2, 0, 21, 3),
(3, 1, 21, 4),
(0, 0, 22, 2),
(1, 1, 22, 3),
(1, 0, 23, 2),
(2, 1, 23, 4),
(3, 1, 24, 3),
(2, 0, 24, 4),
(1, 1, 25, 3),
(0, 0, 25, 6),
(1, 0, 26, 4),
(2, 1, 26, 5),
(3, 1, 27, 5),
(2, 0, 27, 6),
(0, 0, 28, 7),
(1, 1, 28, 8),
(1, 1, 29, 7),
(0, 0, 29, 8),
(2, 1, 30, 7),
(0, 0, 30, 8),
(3, 1, 31, 7),
(2, 0, 31, 8),
(1, 1, 32, 7),
(0, 0, 32, 8),
(0, 0, 33, 8),
(2, 1, 33, 9),
(2, 0, 34, 8),
(3, 1, 34, 9),
(0, 0, 35, 8),
(1, 1, 35, 9),
(1, 0, 36, 8),
(2, 1, 36, 9),
(3, 1, 37, 9),
(2, 0, 37, 10),
(1, 1, 38, 9),
(0, 0, 38, 10),
(2, 1, 39, 9),
(1, 0, 39, 10),
(3, 1, 40, 9),
(2, 0, 40, 10),
(1, 0, 41, 10),
(2, 1, 41, 11),
(2, 0, 42, 10),
(3, 1, 42, 11),
(0, 0, 43, 10),
(1, 1, 43, 11),
(1, 1, 44, 1),
(0, 0, 44, 11),
(2, 1, 45, 1),
(1, 0, 45, 11),
(3, 1, 46, 1),
(2, 0, 46, 11),
(1, 1, 47, 1),
(0, 0, 47, 11),
(2, 1, 48, 1),
(0, 0, 48, 11),
(1, 0, 49, 2),
(2, 1, 49, 11),
(2, 0, 50, 2),
(3, 1, 50, 10),
(1, 1, 51, 10),
(0, 0, 51, 11),
(0, 0, 52, 2),
(2, 1, 52, 11),
(2, 0, 53, 2),
(3, 1, 53, 10);

-- --------------------------------------------------------

--
-- Structure de la table `messages`
--

CREATE TABLE `messages` (
  `message_id` int(10) UNSIGNED NOT NULL,
  `body_text` longtext NOT NULL,
  `created_at` datetime NOT NULL,
  `is_read` tinyint(4) NOT NULL DEFAULT 0,
  `read_at` datetime DEFAULT NULL,
  `is_deleted_by_sender` tinyint(4) NOT NULL DEFAULT 0,
  `is_deleted_by_receiver` tinyint(4) NOT NULL DEFAULT 0,
  `sender_user_id` int(10) UNSIGNED NOT NULL,
  `receiver_user_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `messages`
--

INSERT INTO `messages` (`message_id`, `body_text`, `created_at`, `is_read`, `read_at`, `is_deleted_by_sender`, `is_deleted_by_receiver`, `sender_user_id`, `receiver_user_id`) VALUES
(1, 'Message 1', '2026-02-12 01:00:00', 1, '2026-02-12 01:05:00', 0, 0, 1, 2),
(2, 'Message 2', '2026-02-12 02:00:00', 0, NULL, 0, 0, 2, 3),
(3, 'Message 3', '2026-02-12 03:00:00', 1, '2026-02-12 03:05:00', 0, 0, 3, 4),
(4, 'Message 4', '2026-02-12 04:00:00', 0, NULL, 0, 0, 4, 5),
(5, 'Message 5', '2026-02-12 05:00:00', 1, '2026-02-12 05:05:00', 0, 0, 5, 6),
(6, 'Message 6', '2026-02-12 06:00:00', 0, NULL, 0, 0, 6, 7),
(7, 'Message 7', '2026-02-12 07:00:00', 1, '2026-02-12 07:05:00', 0, 0, 7, 8),
(8, 'Message 8', '2026-02-12 08:00:00', 0, NULL, 0, 0, 8, 9),
(9, 'Message 9', '2026-02-12 09:00:00', 1, '2026-02-12 09:05:00', 0, 0, 9, 10),
(10, 'Message 10', '2026-02-12 10:00:00', 0, NULL, 0, 0, 10, 1),
(11, 'ilyes', '2026-02-12 04:51:42', 1, '2026-02-12 16:00:52', 0, 0, 2, 1),
(12, 'ayy jeyyy', '2026-02-12 12:51:18', 0, NULL, 0, 0, 2, 3),
(13, 'ti jewe', '2026-02-12 12:57:48', 0, NULL, 0, 0, 2, 3);

-- --------------------------------------------------------

--
-- Structure de la table `messenger_messages`
--

CREATE TABLE `messenger_messages` (
  `id` bigint(20) NOT NULL,
  `body` longtext NOT NULL,
  `headers` longtext NOT NULL,
  `queue_name` varchar(190) NOT NULL,
  `created_at` datetime NOT NULL,
  `available_at` datetime NOT NULL,
  `delivered_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `notifications`
--

CREATE TABLE `notifications` (
  `notification_id` int(10) UNSIGNED NOT NULL,
  `type` varchar(25) NOT NULL,
  `ref_table` varchar(64) DEFAULT NULL,
  `ref_id` bigint(20) UNSIGNED DEFAULT NULL,
  `content` varchar(255) NOT NULL,
  `is_read` tinyint(4) NOT NULL DEFAULT 0,
  `read_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `user_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `notifications`
--

INSERT INTO `notifications` (`notification_id`, `type`, `ref_table`, `ref_id`, `content`, `is_read`, `read_at`, `created_at`, `user_id`) VALUES
(1, 'FRIEND_REQUEST', 'tournaments', 1, 'Notification 1', 1, '2026-02-11 23:03:00', '2026-02-11 23:00:00', 1),
(2, 'TEAM_INVITE', 'tournaments', 2, 'Notification 2', 1, '2026-02-12 04:50:27', '2026-02-12 00:00:00', 2),
(3, 'TEAM_JOIN_RESPONSE', 'tournaments', 3, 'Notification 3', 1, '2026-02-12 01:03:00', '2026-02-12 01:00:00', 3),
(4, 'NEW_MESSAGE', 'tournaments', 4, 'Notification 4', 0, NULL, '2026-02-12 02:00:00', 4),
(5, 'TOURNAMENT_REQUEST_STATUS', 'tournaments', 5, 'Notification 5', 1, '2026-02-12 03:03:00', '2026-02-12 03:00:00', 5),
(6, 'ORDER_STATUS', 'tournaments', 6, 'Notification 6', 0, NULL, '2026-02-12 04:00:00', 6),
(7, 'FRIEND_REQUEST', 'tournaments', 7, 'Notification 7', 1, '2026-02-12 05:03:00', '2026-02-12 05:00:00', 7),
(8, 'TEAM_INVITE', 'tournaments', 8, 'Notification 8', 0, NULL, '2026-02-12 06:00:00', 8),
(9, 'TEAM_JOIN_RESPONSE', 'tournaments', 9, 'Notification 9', 1, '2026-02-12 07:03:00', '2026-02-12 07:00:00', 9),
(10, 'NEW_MESSAGE', 'tournaments', 10, 'Notification 10', 1, '2026-02-12 18:08:13', '2026-02-12 08:00:00', 10);

-- --------------------------------------------------------

--
-- Structure de la table `orders`
--

CREATE TABLE `orders` (
  `order_id` int(10) UNSIGNED NOT NULL,
  `order_number` varchar(30) NOT NULL,
  `status` varchar(9) NOT NULL DEFAULT 'PENDING',
  `payment_method` varchar(5) DEFAULT NULL,
  `payment_status` varchar(8) NOT NULL DEFAULT 'UNPAID',
  `total_amount` decimal(10,2) NOT NULL,
  `shipping_address` varchar(255) DEFAULT NULL,
  `phone_for_delivery` varchar(30) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `paid_at` datetime DEFAULT NULL,
  `shipped_at` datetime DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL,
  `cart_id` int(10) UNSIGNED NOT NULL,
  `user_id` int(10) UNSIGNED NOT NULL,
  `stripe_checkout_session_id` varchar(255) DEFAULT NULL,
  `stripe_payment_intent_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `orders`
--

INSERT INTO `orders` (`order_id`, `order_number`, `status`, `payment_method`, `payment_status`, `total_amount`, `shipping_address`, `phone_for_delivery`, `created_at`, `paid_at`, `shipped_at`, `delivered_at`, `cart_id`, `user_id`, `stripe_checkout_session_id`, `stripe_payment_intent_id`) VALUES
(2, 'ORD-2026-000002', 'PAID', 'CARD', 'PAID', 165.00, 'Adresse 2', '+21621110002', '2026-02-08 10:00:00', '2026-02-08 12:00:00', NULL, NULL, 2, 2, NULL, NULL),
(3, 'ORD-2026-000003', 'CANCELLED', 'CASH', 'UNPAID', 60.00, 'Adresse 3', '+21621110003', '2026-02-09 10:00:00', NULL, NULL, NULL, 3, 3, NULL, NULL),
(4, 'ORD-2026-000004', 'SHIPPED', 'CARD', 'PAID', 130.00, 'Adresse 4', '+21621110004', '2026-02-09 10:00:00', '2026-02-09 12:00:00', '2026-02-10 10:00:00', NULL, 4, 4, NULL, NULL),
(5, 'ORD-2026-000005', 'DELIVERED', 'CASH', 'PAID', 210.00, 'Adresse 5', '+21621110005', '2026-02-09 10:00:00', '2026-02-09 12:00:00', '2026-02-10 10:00:00', '2026-02-11 10:00:00', 5, 5, NULL, NULL),
(6, 'ORD-2026-000006', 'PAID', 'CARD', 'PAID', 75.00, 'Adresse 6', '+21621110006', '2026-02-10 10:00:00', '2026-02-10 12:00:00', NULL, NULL, 6, 6, NULL, NULL),
(7, 'ORD-2026-000007', 'SHIPPED', 'CASH', 'PAID', 160.00, 'Adresse 7', '+21621110007', '2026-02-10 10:00:00', '2026-02-10 12:00:00', '2026-02-11 10:00:00', NULL, 7, 7, NULL, NULL),
(8, 'ORD-2026-000008', 'DELIVERED', 'CARD', 'PAID', 255.00, 'Adresse 8', '+21621110008', '2026-02-10 10:00:00', '2026-02-10 12:00:00', '2026-02-11 10:00:00', '2026-02-12 10:00:00', 8, 8, NULL, NULL),
(9, 'ORD-2026-000009', 'PENDING', 'CASH', 'UNPAID', 90.00, 'Adresse 9', '+21621110009', '2026-02-11 10:00:00', NULL, NULL, NULL, 9, 9, NULL, NULL),
(10, 'ORD-2026-000010', 'PAID', 'CARD', 'PAID', 190.00, 'Adresse 10', '+21621110010', '2026-02-11 10:00:00', '2026-02-11 12:00:00', NULL, NULL, 10, 10, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `personne`
--

CREATE TABLE `personne` (
  `id` int(11) NOT NULL,
  `nom` varchar(20) NOT NULL,
  `prenom` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `personne`
--

INSERT INTO `personne` (`id`, `nom`, `prenom`) VALUES
(1, 'Martin', 'Alicia');

-- --------------------------------------------------------

--
-- Structure de la table `posts`
--

CREATE TABLE `posts` (
  `post_id` int(10) UNSIGNED NOT NULL,
  `content_text` longtext DEFAULT NULL,
  `visibility` varchar(9) NOT NULL DEFAULT 'PUBLIC',
  `is_deleted` tinyint(4) NOT NULL DEFAULT 0,
  `deleted_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `author_user_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `posts`
--

INSERT INTO `posts` (`post_id`, `content_text`, `visibility`, `is_deleted`, `deleted_at`, `created_at`, `updated_at`, `author_user_id`) VALUES
(2, 'Post demo 2', 'FRIENDS', 0, NULL, '2026-02-11 06:00:00', '2026-02-11 06:15:00', 2),
(3, 'Post demo 3', 'TEAM_ONLY', 0, NULL, '2026-02-11 07:00:00', '2026-02-11 07:15:00', 3),
(4, 'Post demo 4', 'PUBLIC', 0, NULL, '2026-02-11 08:00:00', '2026-02-11 08:15:00', 4),
(5, 'Post demo 5', 'FRIENDS', 0, NULL, '2026-02-11 09:00:00', '2026-02-11 09:15:00', 5),
(6, 'Post demo 6', 'TEAM_ONLY', 0, NULL, '2026-02-11 10:00:00', '2026-02-11 10:15:00', 6),
(7, 'Post demo 7', 'PUBLIC', 0, NULL, '2026-02-11 11:00:00', '2026-02-11 11:15:00', 7),
(8, 'Post demo 8', 'FRIENDS', 0, NULL, '2026-02-11 12:00:00', '2026-02-11 12:15:00', 8),
(9, 'Post demo 9', 'TEAM_ONLY', 0, NULL, '2026-02-11 13:00:00', '2026-02-11 13:15:00', 9),
(10, 'Post demo 10', 'PUBLIC', 0, NULL, '2026-02-11 14:00:00', '2026-02-11 14:15:00', 10),
(11, 'lassssss', 'PUBLIC', 0, NULL, '2026-02-12 04:13:18', '2026-02-12 04:13:18', 2),
(12, 'mahmoud', 'PUBLIC', 0, NULL, '2026-02-12 04:14:02', '2026-02-12 04:14:02', 2),
(13, 'first', 'PUBLIC', 0, NULL, '2026-02-12 04:49:51', '2026-02-12 04:49:51', 2),
(14, 'yyyyyyyyyyyy', 'PUBLIC', 0, NULL, '2026-02-12 04:52:22', '2026-02-12 04:52:22', 2),
(15, '', 'PUBLIC', 0, NULL, '2026-02-12 12:38:24', '2026-02-12 12:38:24', 2),
(16, 'GG', 'PUBLIC', 1, '2026-02-12 13:03:27', '2026-02-12 12:43:09', '2026-02-12 13:03:27', 2),
(17, 'THE TEAM', 'PUBLIC', 0, NULL, '2026-02-12 12:46:57', '2026-02-12 12:46:57', 2),
(18, 'first admi post', 'PUBLIC', 0, NULL, '2026-02-12 18:21:06', '2026-02-12 18:21:06', 1),
(21, 'game', 'PUBLIC', 0, NULL, '2026-02-12 23:55:02', '2026-02-12 23:55:02', 5),
(25, 'TEST', 'PUBLIC', 1, '2026-02-27 09:34:25', '2026-02-27 09:33:19', '2026-02-27 09:34:25', 15),
(27, 'azerty', 'PUBLIC', 0, NULL, '2026-04-15 23:47:53', '2026-04-15 23:47:53', 21),
(28, 'aqwzsxedc', 'PUBLIC', 0, NULL, '2026-04-15 23:48:17', '2026-04-15 23:48:17', 21);

-- --------------------------------------------------------

--
-- Structure de la table `post_images`
--

CREATE TABLE `post_images` (
  `position` int(10) UNSIGNED NOT NULL DEFAULT 1,
  `post_id` int(10) UNSIGNED NOT NULL,
  `image_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `post_images`
--

INSERT INTO `post_images` (`position`, `post_id`, `image_id`) VALUES
(1, 2, 2),
(1, 3, 3),
(1, 4, 4),
(1, 5, 5),
(1, 6, 6),
(1, 7, 7),
(1, 8, 8),
(1, 9, 9),
(1, 10, 10),
(1, 16, 13),
(1, 17, 14),
(2, 17, 15),
(3, 17, 16),
(4, 17, 17),
(1, 18, 28),
(1, 21, 35),
(1, 25, 57),
(2, 25, 58),
(3, 25, 59),
(4, 25, 60);

-- --------------------------------------------------------

--
-- Structure de la table `post_likes`
--

CREATE TABLE `post_likes` (
  `created_at` datetime NOT NULL,
  `post_id` int(10) UNSIGNED NOT NULL,
  `user_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `post_likes`
--

INSERT INTO `post_likes` (`created_at`, `post_id`, `user_id`) VALUES
('2026-02-11 16:00:00', 2, 5),
('2026-02-11 17:00:00', 3, 6),
('2026-02-11 18:00:00', 4, 7),
('2026-02-11 19:00:00', 5, 8),
('2026-02-11 20:00:00', 6, 9),
('2026-02-11 21:00:00', 7, 10),
('2026-02-11 22:00:00', 8, 1),
('2026-02-11 23:00:00', 9, 2),
('2026-02-12 04:53:35', 13, 2),
('2026-02-12 04:52:46', 14, 2),
('2026-02-12 13:03:48', 15, 2),
('2026-02-12 12:43:55', 16, 2),
('2026-02-12 16:04:10', 17, 1),
('2026-02-12 12:47:08', 17, 2),
('2026-02-12 18:21:15', 18, 1),
('2026-02-27 09:33:32', 25, 15);

-- --------------------------------------------------------

--
-- Structure de la table `products`
--

CREATE TABLE `products` (
  `product_id` int(10) UNSIGNED NOT NULL,
  `name` varchar(150) NOT NULL,
  `description` longtext DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `stock_qty` int(10) UNSIGNED NOT NULL DEFAULT 0,
  `sku` varchar(64) DEFAULT NULL,
  `is_active` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `team_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `products`
--

INSERT INTO `products` (`product_id`, `name`, `description`, `price`, `stock_qty`, `sku`, `is_active`, `created_at`, `updated_at`, `team_id`) VALUES
(1, 'Produit 1', 'Produit officiel 1', 50.00, 21, 'SKU-0001', 1, '2026-01-26 10:00:00', '2026-01-26 11:00:00', 1),
(2, 'Produit 2', 'Produit officiel 2', 55.00, 22, 'SKU-0002', 1, '2026-01-27 10:00:00', '2026-01-27 11:00:00', 2),
(3, 'Produit 3', 'Produit officiel 3', 60.00, 23, 'SKU-0003', 1, '2026-01-28 10:00:00', '2026-01-28 11:00:00', 3),
(4, 'Produit 4', 'Produit officiel 4', 65.00, 24, 'SKU-0004', 1, '2026-01-29 10:00:00', '2026-01-29 11:00:00', 4),
(5, 'Produit 5', 'Produit officiel 5', 70.00, 25, 'SKU-0005', 1, '2026-01-30 10:00:00', '2026-01-30 11:00:00', 5),
(6, 'Produit 6', 'Produit officiel 6', 75.00, 26, 'SKU-0006', 1, '2026-01-31 10:00:00', '2026-01-31 11:00:00', 6),
(7, 'Produit 7', 'Produit officiel 7', 80.00, 27, 'SKU-0007', 1, '2026-02-01 10:00:00', '2026-02-01 11:00:00', 7),
(8, 'Produit 8', 'Produit officiel 8', 85.00, 28, 'SKU-0008', 1, '2026-02-02 10:00:00', '2026-02-02 11:00:00', 8),
(9, 'Produit 9', 'Produit officiel 9', 90.00, 29, 'SKU-0009', 1, '2026-02-03 10:00:00', '2026-02-03 11:00:00', 9),
(10, 'Produit 10', 'Produit officiel 10', 95.00, 30, 'SKU-0010', 1, '2026-02-04 10:00:00', '2026-02-04 11:00:00', 10),
(11, 'casque', 'ichri z 3iik mghamdha', 1000.00, 100, 'TEST', 1, '2026-02-12 14:26:37', '2026-02-12 14:28:00', 1),
(12, 'Maryoul', 'test', 1000.00, 14, NULL, 1, '2026-02-13 12:47:48', '2026-02-13 12:47:48', 13);

-- --------------------------------------------------------

--
-- Structure de la table `product_images`
--

CREATE TABLE `product_images` (
  `position` int(10) UNSIGNED NOT NULL DEFAULT 1,
  `product_id` int(10) UNSIGNED NOT NULL,
  `image_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `product_images`
--

INSERT INTO `product_images` (`position`, `product_id`, `image_id`) VALUES
(1, 1, 1),
(1, 2, 2),
(1, 3, 3),
(1, 4, 4),
(1, 5, 5),
(1, 6, 6),
(1, 7, 7),
(1, 8, 8),
(1, 9, 9),
(1, 10, 10),
(1, 11, 20),
(2, 11, 21),
(3, 11, 22),
(4, 11, 23),
(5, 11, 24),
(6, 11, 25),
(7, 11, 26),
(1, 12, 41);

-- --------------------------------------------------------

--
-- Structure de la table `reports`
--

CREATE TABLE `reports` (
  `report_id` int(10) UNSIGNED NOT NULL,
  `target_type` varchar(7) NOT NULL,
  `target_id` bigint(20) UNSIGNED NOT NULL,
  `reason` longtext NOT NULL,
  `status` varchar(9) NOT NULL DEFAULT 'OPEN',
  `created_at` datetime NOT NULL,
  `handled_at` datetime DEFAULT NULL,
  `admin_note` longtext DEFAULT NULL,
  `reporter_user_id` int(10) UNSIGNED NOT NULL,
  `handled_by_admin_id` int(10) UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `reports`
--

INSERT INTO `reports` (`report_id`, `target_type`, `target_id`, `reason`, `status`, `created_at`, `handled_at`, `admin_note`, `reporter_user_id`, `handled_by_admin_id`) VALUES
(1, 'POST', 1, 'Signalement 1', 'OPEN', '2026-02-03 10:00:00', NULL, NULL, 1, NULL),
(2, 'COMMENT', 2, 'Signalement 2', 'IN_REVIEW', '2026-02-04 10:00:00', '2026-02-12 06:00:00', 'Traite', 2, 1),
(3, 'USER', 3, 'Signalement 3', 'CLOSED', '2026-02-05 10:00:00', '2026-02-12 07:00:00', 'Traite', 3, 1),
(4, 'TEAM', 4, 'Signalement 4', 'OPEN', '2026-02-06 10:00:00', NULL, NULL, 4, NULL),
(5, 'POST', 5, 'Signalement 5', 'IN_REVIEW', '2026-02-07 10:00:00', '2026-02-12 09:00:00', 'Traite', 5, 1),
(6, 'COMMENT', 6, 'Signalement 6', 'CLOSED', '2026-02-08 10:00:00', '2026-02-12 10:00:00', 'Traite', 6, 1),
(7, 'USER', 7, 'Signalement 7', 'OPEN', '2026-02-09 10:00:00', NULL, NULL, 7, NULL),
(8, 'TEAM', 8, 'Signalement 8', 'IN_REVIEW', '2026-02-10 10:00:00', '2026-02-12 08:00:00', 'Traite', 8, 1),
(9, 'POST', 9, 'Signalement 9', 'CLOSED', '2026-02-11 10:00:00', '2026-02-12 07:00:00', 'Traite', 9, 1),
(10, 'COMMENT', 10, 'Signalement 10', 'OPEN', '2026-02-12 10:00:00', NULL, NULL, 10, NULL),
(11, 'POST', 20, 'zahhh', 'OPEN', '2026-02-12 23:54:32', NULL, NULL, 5, NULL),
(14, 'POST', 25, 'mot innaproprié', 'OPEN', '2026-02-27 09:34:18', NULL, NULL, 15, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `teams`
--

CREATE TABLE `teams` (
  `team_id` int(10) UNSIGNED NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` longtext DEFAULT NULL,
  `region` varchar(80) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `logo_image_id` int(10) UNSIGNED DEFAULT NULL,
  `captain_user_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `teams`
--

INSERT INTO `teams` (`team_id`, `name`, `description`, `region`, `created_at`, `updated_at`, `logo_image_id`, `captain_user_id`) VALUES
(1, 'Team Pulse 01', 'Equipe 1', 'Region-01', '2026-01-19 10:00:00', '2026-02-12 14:22:20', 19, 3),
(2, 'Team Pulse 02', 'Equipe 2', 'Region-02', '2026-01-20 10:00:00', '2026-01-20 12:00:00', 2, 6),
(3, 'Team Pulse 03', 'Equipe 3', 'Region-03', '2026-01-21 10:00:00', '2026-01-21 12:00:00', 3, 2),
(4, 'Team Pulse 04', 'Equipe 4', 'Region-04', '2026-01-22 10:00:00', '2026-01-22 12:00:00', 4, 5),
(5, 'Team Pulse 05', 'Equipe 5', 'Region-05', '2026-01-23 10:00:00', '2026-01-23 12:00:00', 5, 9),
(6, 'Team Pulse 06', 'Equipe 6', 'Region-06', '2026-01-24 10:00:00', '2026-01-24 12:00:00', 6, 4),
(7, 'Team Pulse 07', 'Equipe 7', 'Region-07', '2026-01-25 10:00:00', '2026-01-25 12:00:00', 7, 7),
(8, 'Team Pulse 08', 'Equipe 8', 'Region-08', '2026-01-26 10:00:00', '2026-01-26 12:00:00', 8, 8),
(9, 'Team Pulse 09', 'Equipe 9', 'Region-09', '2026-01-27 10:00:00', '2026-01-27 12:00:00', 9, 10),
(10, 'Team Pulse 10', 'Equipe 10', 'Region-10', '2026-01-28 10:00:00', '2026-01-28 12:00:00', 10, 1),
(11, 'team ilyes', 'team ilyes est une equipe e-sport orientee performance, discipline et progression collective. Ancree en USA, elle construit un roster actif et engage. Structure pilotee par User 03, avec focus sur scrims, communication et regularite.', 'United States', '2026-02-12 14:21:48', '2026-02-27 10:01:51', 61, 3),
(13, 'aceee', 'aceee est une equipe e-sport orientee performance, discipline et progression collective. Ancree en USA, elle construit un roster actif et engage. Structure pilotee par User 06, avec focus sur scrims, communication et regularite.', 'United States', '2026-02-13 12:24:14', '2026-02-27 07:44:37', 37, 6);

-- --------------------------------------------------------

--
-- Structure de la table `team_invites`
--

CREATE TABLE `team_invites` (
  `invite_id` int(10) UNSIGNED NOT NULL,
  `status` varchar(9) NOT NULL DEFAULT 'PENDING',
  `message` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `responded_at` datetime DEFAULT NULL,
  `team_id` int(10) UNSIGNED NOT NULL,
  `invited_user_id` int(10) UNSIGNED NOT NULL,
  `invited_by_user_id` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `team_invites`
--

INSERT INTO `team_invites` (`invite_id`, `status`, `message`, `created_at`, `responded_at`, `team_id`, `invited_user_id`, `invited_by_user_id`) VALUES
(1, 'ACCEPTED', 'Invite 1', '2026-01-30 10:00:00', '2026-02-12 23:59:27', 1, 5, 3),
(2, 'ACCEPTED', 'Invite 2', '2026-01-31 10:00:00', '2026-01-31 18:00:00', 2, 7, 6),
(3, 'REFUSED', 'Invite 3', '2026-02-01 10:00:00', '2026-02-01 18:00:00', 3, 7, 2),
(4, 'CANCELLED', 'Invite 4', '2026-02-02 10:00:00', '2026-02-02 18:00:00', 4, 8, 5),
(5, 'ACCEPTED', 'Invite 5', '2026-02-03 10:00:00', '2026-02-12 18:06:32', 5, 10, 9),
(6, 'ACCEPTED', 'Invite 6', '2026-02-04 10:00:00', '2026-02-04 18:00:00', 6, 10, 4),
(7, 'REFUSED', 'Invite 7', '2026-02-05 10:00:00', '2026-02-05 18:00:00', 7, 1, 7),
(8, 'CANCELLED', 'Invite 8', '2026-02-06 10:00:00', '2026-02-06 18:00:00', 8, 2, 8),
(9, 'PENDING', 'Invite 9', '2026-02-07 10:00:00', NULL, 9, 3, 10),
(10, 'ACCEPTED', 'Invite 10', '2026-02-08 10:00:00', '2026-02-08 18:00:00', 10, 4, 1),
(11, 'PENDING', 'ti ija', '2026-02-12 14:24:33', NULL, 1, 2, 3),
(15, 'PENDING', 'Salut User 01, ici User 06, capitaine de aceee. Ton profil ADMIN nous interesse pour renforcer le roster. Notre base est en USA, avec un planning stable. Si tu es partant, on peut discuter des prochains scrims.', '2026-02-27 07:32:04', NULL, 13, 1, 6),
(16, 'PENDING', 'ti ija', '2026-02-27 07:32:23', NULL, 13, 5, 6),
(17, 'PENDING', 'noob', '2026-02-27 07:55:28', NULL, 13, 8, 6),
(18, 'PENDING', 'NOOB idiot NOOB', '2026-02-27 10:06:30', NULL, 1, 1, 3);

-- --------------------------------------------------------

--
-- Structure de la table `team_join_requests`
--

CREATE TABLE `team_join_requests` (
  `request_id` int(10) UNSIGNED NOT NULL,
  `status` varchar(9) NOT NULL DEFAULT 'PENDING',
  `note` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `responded_at` datetime DEFAULT NULL,
  `team_id` int(10) UNSIGNED NOT NULL,
  `user_id` int(10) UNSIGNED NOT NULL,
  `responded_by_captain_id` int(10) UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `team_join_requests`
--

INSERT INTO `team_join_requests` (`request_id`, `status`, `note`, `created_at`, `responded_at`, `team_id`, `user_id`, `responded_by_captain_id`) VALUES
(1, 'ACCEPTED', 'Join 1', '2026-01-30 10:00:00', '2026-02-12 14:24:03', 1, 4, 3),
(2, 'ACCEPTED', 'Join 2', '2026-01-31 10:00:00', '2026-01-31 15:00:00', 2, 5, 6),
(3, 'REFUSED', 'Join 3', '2026-02-01 10:00:00', '2026-02-01 15:00:00', 3, 6, 2),
(4, 'CANCELLED', 'Join 4', '2026-02-02 10:00:00', '2026-02-02 15:00:00', 4, 7, 5),
(5, 'PENDING', 'Join 5', '2026-02-03 10:00:00', NULL, 5, 8, NULL),
(6, 'ACCEPTED', 'Join 6', '2026-02-04 10:00:00', '2026-02-04 15:00:00', 6, 9, 4),
(7, 'REFUSED', 'Join 7', '2026-02-05 10:00:00', '2026-02-05 15:00:00', 7, 10, 7),
(8, 'CANCELLED', 'Join 8', '2026-02-06 10:00:00', '2026-02-06 15:00:00', 8, 1, 8),
(9, 'PENDING', 'Join 9', '2026-02-07 10:00:00', NULL, 9, 2, NULL),
(10, 'ACCEPTED', 'Join 10', '2026-02-08 10:00:00', '2026-02-08 15:00:00', 10, 3, 1),
(12, 'ACCEPTED', NULL, '2026-02-13 12:29:18', '2026-02-13 12:29:33', 13, 8, 6);

-- --------------------------------------------------------

--
-- Structure de la table `team_members`
--

CREATE TABLE `team_members` (
  `joined_at` datetime NOT NULL,
  `is_active` tinyint(4) NOT NULL DEFAULT 1,
  `left_at` datetime DEFAULT NULL,
  `team_id` int(10) UNSIGNED NOT NULL,
  `user_id` int(10) UNSIGNED NOT NULL,
  `roster_role` varchar(20) NOT NULL DEFAULT 'STARTER'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `team_members`
--

INSERT INTO `team_members` (`joined_at`, `is_active`, `left_at`, `team_id`, `user_id`, `roster_role`) VALUES
('2026-02-01 10:00:00', 1, NULL, 1, 3, 'CAPTAIN'),
('2026-02-12 14:24:03', 1, NULL, 1, 4, 'STARTER'),
('2026-02-12 23:59:27', 1, NULL, 1, 5, 'STARTER'),
('2026-02-02 10:00:00', 1, NULL, 2, 6, 'CAPTAIN'),
('2026-02-03 10:00:00', 1, NULL, 3, 2, 'CAPTAIN'),
('2026-02-04 10:00:00', 1, NULL, 4, 5, 'CAPTAIN'),
('2026-02-05 10:00:00', 1, NULL, 5, 9, 'CAPTAIN'),
('2026-02-12 18:06:32', 1, NULL, 5, 10, 'STARTER'),
('2026-02-06 10:00:00', 1, NULL, 6, 4, 'CAPTAIN'),
('2026-02-07 10:00:00', 1, NULL, 7, 7, 'CAPTAIN'),
('2026-02-08 10:00:00', 1, NULL, 8, 8, 'CAPTAIN'),
('2026-02-09 10:00:00', 0, '2026-02-12 18:06:12', 9, 10, 'CAPTAIN'),
('2026-02-10 10:00:00', 1, NULL, 10, 1, 'CAPTAIN'),
('2026-02-12 14:21:48', 1, NULL, 11, 3, 'CAPTAIN'),
('2026-02-13 12:24:14', 1, NULL, 13, 6, 'CAPTAIN'),
('2026-02-13 12:29:33', 0, '2026-02-27 07:18:57', 13, 8, 'CO_CAPTAIN');

-- --------------------------------------------------------

--
-- Structure de la table `tournaments`
--

CREATE TABLE `tournaments` (
  `tournament_id` int(10) UNSIGNED NOT NULL,
  `title` varchar(180) NOT NULL,
  `description` longtext DEFAULT NULL,
  `rules` longtext DEFAULT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `registration_deadline` date DEFAULT NULL,
  `max_teams` int(10) UNSIGNED NOT NULL,
  `format` varchar(3) NOT NULL DEFAULT 'BO1',
  `prize_pool` decimal(12,2) NOT NULL DEFAULT 0.00,
  `prize_description` varchar(255) DEFAULT NULL,
  `status` varchar(9) NOT NULL DEFAULT 'DRAFT',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `organizer_user_id` int(10) UNSIGNED NOT NULL,
  `game_id` int(10) UNSIGNED NOT NULL,
  `registration_mode` varchar(8) NOT NULL DEFAULT 'OPEN',
  `photo_path` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `tournaments`
--

INSERT INTO `tournaments` (`tournament_id`, `title`, `description`, `rules`, `start_date`, `end_date`, `registration_deadline`, `max_teams`, `format`, `prize_pool`, `prize_description`, `status`, `created_at`, `updated_at`, `organizer_user_id`, `game_id`, `registration_mode`, `photo_path`) VALUES
(1, 'Tournament 1', 'Description tournoi 1', 'Rules tournoi 1', '2026-02-18', '2026-02-21', '2026-02-16', 9, 'BO1', 1200.00, 'Prize tournoi 1', 'DRAFT', '2026-02-01 10:00:00', '2026-02-01 13:00:00', 2, 1, 'OPEN', 'uploads/seeds/seed_01.jpg'),
(2, 'Tournament 2', 'Description tournoi 2', 'Rules tournoi 2', '2026-02-19', '2026-02-22', '2026-02-17', 10, 'BO3', 1400.00, 'Prize tournoi 2', 'OPEN', '2026-02-02 10:00:00', '2026-02-02 13:00:00', 5, 2, 'APPROVAL', 'uploads/seeds/seed_02.jpg'),
(3, 'Tournament 3', 'Description tournoi 3', 'Rules tournoi 3', '2026-02-20', '2026-02-23', '2026-02-18', 11, 'BO5', 1600.00, 'Prize tournoi 3', 'ONGOING', '2026-02-03 10:00:00', '2026-02-03 13:00:00', 9, 3, 'OPEN', 'uploads/seeds/seed_03.jpg'),
(4, 'Tournament 4', 'Description tournoi 4', 'Rules tournoi 4', '2026-02-21', '2026-02-24', '2026-02-19', 12, 'BO1', 1800.00, 'Prize tournoi 4', 'FINISHED', '2026-02-04 10:00:00', '2026-02-04 13:00:00', 2, 4, 'APPROVAL', 'uploads/seeds/seed_04.jpg'),
(7, 'Tournament 7', 'Description tournoi 7', 'Rules tournoi 7', '2026-02-24', '2026-02-27', '2026-02-22', 15, 'BO1', 2400.00, 'Prize tournoi 7', 'ONGOING', '2026-02-07 10:00:00', '2026-02-07 13:00:00', 2, 7, 'OPEN', 'uploads/seeds/seed_07.jpg'),
(8, 'Tournament 8', 'Description tournoi 8', 'Rules tournoi 8', '2026-02-25', '2026-02-28', '2026-02-23', 16, 'BO3', 2600.00, 'Prize tournoi 8', 'FINISHED', '2026-02-08 10:00:00', '2026-02-08 13:00:00', 5, 8, 'APPROVAL', 'uploads/seeds/seed_08.jpg'),
(9, 'Tournament 9', 'Description tournoi 9', 'Rules tournoi 9', '2026-02-26', '2026-03-01', '2026-02-24', 15, 'BO3', 2800.00, 'Prize tournoi 9', 'DRAFT', '2026-02-09 10:00:00', '2026-02-13 12:02:54', 9, 9, 'OPEN', 'uploads/tournaments/tournament_c8902ad8df969622.png'),
(10, 'Tournament 10', 'Description tournoi 10', 'Rules tournoi 10', '2026-02-27', '2026-03-02', '2026-02-25', 18, 'BO1', 3000.00, 'Prize tournoi 10', 'OPEN', '2026-02-10 10:00:00', '2026-02-12 15:36:22', 2, 10, 'APPROVAL', 'uploads/seeds/seed_10.jpg'),
(11, 'hhhhhhhh', 'fghjkl', 'frghjkl', '2026-02-14', '2026-02-27', '2026-02-10', 33, 'BO5', 234567.00, 'fghjk', 'DRAFT', '2026-02-12 15:38:26', '2026-02-12 15:38:26', 4, 4, 'APPROVAL', 'uploads/tournaments/tournament_f043b3663bfca6c2.jpg'),
(12, 'Tournament Request 7', 'Description request 7', 'Rules request 7', '2026-02-26', '2026-02-28', '2026-02-23', 15, 'BO1', 1550.00, 'Prize request 7', 'OPEN', '2026-02-12 15:41:56', '2026-02-12 15:41:56', 2, 7, 'OPEN', 'uploads/seeds/seed_07.jpg'),
(17, 'azerty', 'FGHKL/', 'FGHJKL', '2026-02-04', '2026-02-19', '2026-02-01', 12, 'BO1', 12345.00, 'FDHJL', 'OPEN', '2026-02-27 09:48:46', '2026-02-27 09:48:46', 16, 1, 'APPROVAL', 'uploads/tournaments/tournament_98805cb1abef65ec.png');

-- --------------------------------------------------------

--
-- Structure de la table `tournament_requests`
--

CREATE TABLE `tournament_requests` (
  `request_id` int(10) UNSIGNED NOT NULL,
  `title` varchar(180) NOT NULL,
  `description` longtext DEFAULT NULL,
  `rules` longtext DEFAULT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `registration_deadline` date DEFAULT NULL,
  `max_teams` int(10) UNSIGNED NOT NULL,
  `format` varchar(3) NOT NULL DEFAULT 'BO1',
  `registration_mode` varchar(8) NOT NULL DEFAULT 'OPEN',
  `prize_pool` decimal(12,2) NOT NULL DEFAULT 0.00,
  `prize_description` varchar(255) DEFAULT NULL,
  `status` varchar(8) NOT NULL DEFAULT 'PENDING',
  `admin_response_note` longtext DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `organizer_user_id` int(10) UNSIGNED NOT NULL,
  `game_id` int(10) UNSIGNED NOT NULL,
  `reviewed_by_admin_id` int(10) UNSIGNED DEFAULT NULL,
  `photo_path` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `tournament_requests`
--

INSERT INTO `tournament_requests` (`request_id`, `title`, `description`, `rules`, `start_date`, `end_date`, `registration_deadline`, `max_teams`, `format`, `registration_mode`, `prize_pool`, `prize_description`, `status`, `admin_response_note`, `created_at`, `reviewed_at`, `organizer_user_id`, `game_id`, `reviewed_by_admin_id`, `photo_path`) VALUES
(1, 'Tournament Request 1', 'Description request 1', 'Rules request 1', '2026-02-14', '2026-02-16', '2026-02-11', 9, 'BO1', 'OPEN', 650.00, 'Prize request 1', 'PENDING', NULL, '2026-01-24 10:00:00', NULL, 2, 1, NULL, 'uploads/seeds/seed_01.jpg'),
(2, 'Tournament Request 2', 'Description request 2', 'Rules request 2', '2026-02-16', '2026-02-18', '2026-02-13', 10, 'BO3', 'APPROVAL', 800.00, 'Prize request 2', 'ACCEPTED', 'Reponse admin', '2026-01-25 10:00:00', '2026-02-04 10:00:00', 5, 2, 1, 'uploads/seeds/seed_02.jpg'),
(3, 'Tournament Request 3', 'Description request 3', 'Rules request 3', '2026-02-18', '2026-02-20', '2026-02-15', 11, 'BO5', 'OPEN', 950.00, 'Prize request 3', 'REFUSED', 'Reponse admin', '2026-01-26 10:00:00', '2026-02-05 10:00:00', 9, 3, 1, 'uploads/seeds/seed_03.jpg'),
(4, 'Tournament Request 4', 'Description request 4', 'Rules request 4', '2026-02-20', '2026-02-22', '2026-02-17', 12, 'BO1', 'APPROVAL', 1100.00, 'Prize request 4', 'PENDING', NULL, '2026-01-27 10:00:00', NULL, 2, 4, NULL, 'uploads/seeds/seed_04.jpg'),
(5, 'Tournament Request 5', 'Description request 5', 'Rules request 5', '2026-02-22', '2026-02-24', '2026-02-19', 13, 'BO3', 'OPEN', 1250.00, 'Prize request 5', 'ACCEPTED', 'Reponse admin', '2026-01-28 10:00:00', '2026-02-07 10:00:00', 5, 5, 1, 'uploads/seeds/seed_05.jpg'),
(6, 'Tournament Request 6', 'Description request 6', 'Rules request 6', '2026-02-24', '2026-02-26', '2026-02-21', 14, 'BO5', 'APPROVAL', 1400.00, 'Prize request 6', 'REFUSED', 'Reponse admin', '2026-01-29 10:00:00', '2026-02-08 10:00:00', 9, 6, 1, 'uploads/seeds/seed_06.jpg'),
(7, 'Tournament Request 7', 'Description request 7', 'Rules request 7', '2026-02-26', '2026-02-28', '2026-02-23', 15, 'BO1', 'OPEN', 1550.00, 'Prize request 7', 'ACCEPTED', NULL, '2026-01-30 10:00:00', '2026-02-12 15:41:56', 2, 7, 1, 'uploads/seeds/seed_07.jpg'),
(8, 'Tournament Request 8', 'Description request 8', 'Rules request 8', '2026-02-28', '2026-03-02', '2026-02-25', 16, 'BO3', 'APPROVAL', 1700.00, 'Prize request 8', 'ACCEPTED', 'Reponse admin', '2026-01-31 10:00:00', '2026-02-10 10:00:00', 5, 8, 1, 'uploads/seeds/seed_08.jpg'),
(9, 'Tournament Request 9', 'Description request 9', 'Rules request 9', '2026-03-02', '2026-03-04', '2026-02-27', 17, 'BO5', 'OPEN', 1850.00, 'Prize request 9', 'REFUSED', 'Reponse admin', '2026-02-01 10:00:00', '2026-02-11 10:00:00', 9, 9, 1, 'uploads/seeds/seed_09.jpg'),
(10, 'Tournament Request 10', 'Description request 10', 'Rules request 10', '2026-03-04', '2026-03-06', '2026-03-01', 18, 'BO1', 'APPROVAL', 2000.00, 'Prize request 10', 'PENDING', NULL, '2026-02-02 10:00:00', NULL, 2, 10, NULL, 'uploads/seeds/seed_10.jpg'),
(17, 'zerty', 'cfvhkl', 'vghklm', '2026-02-05', '2026-02-27', '2026-02-02', 12, 'BO1', 'OPEN', 0.00, 'fvgjk;l', 'PENDING', NULL, '2026-02-26 19:30:50', NULL, 5, 1, NULL, 'uploads/tournaments/tournament_744ee9f6931e09db.jpg'),
(18, 'FAA-2026', 'TEST', 'TEST', '2026-02-20', '2026-02-28', '2026-02-18', 32, 'BO1', 'OPEN', 4000.00, 'TEST', 'PENDING', NULL, '2026-02-27 05:37:42', NULL, 1, 3, NULL, 'uploads/tournaments/tournament_d137683d6d5101f6.png'),
(19, 'ZSDGHJK', 'FGHJK./', 'DFGHJK', '2026-02-05', '2026-02-21', '2026-02-02', 12, 'BO5', 'OPEN', 34567.00, 'DFGHJKL', 'REFUSED', NULL, '2026-02-27 09:46:26', '2026-02-27 09:49:45', 1, 1, 1, 'uploads/tournaments/tournament_abebd58d97512cbf.png'),
(20, 'azerty', 'FGHKL/', 'FGHJKL', '2026-02-04', '2026-02-19', '2026-02-01', 12, 'BO1', 'APPROVAL', 12345.00, 'FDHJL', 'ACCEPTED', NULL, '2026-02-27 09:48:10', '2026-02-27 09:48:46', 16, 1, 1, 'uploads/tournaments/tournament_98805cb1abef65ec.png');

-- --------------------------------------------------------

--
-- Structure de la table `tournament_teams`
--

CREATE TABLE `tournament_teams` (
  `status` varchar(9) NOT NULL DEFAULT 'PENDING',
  `seed` int(10) UNSIGNED DEFAULT NULL,
  `registered_at` datetime NOT NULL,
  `decided_at` datetime DEFAULT NULL,
  `checked_in` tinyint(4) NOT NULL DEFAULT 0,
  `checkin_at` datetime DEFAULT NULL,
  `tournament_id` int(10) UNSIGNED NOT NULL,
  `team_id` int(10) UNSIGNED NOT NULL,
  `decided_by_user_id` int(10) UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `tournament_teams`
--

INSERT INTO `tournament_teams` (`status`, `seed`, `registered_at`, `decided_at`, `checked_in`, `checkin_at`, `tournament_id`, `team_id`, `decided_by_user_id`) VALUES
('ACCEPTED', 1, '2026-02-07 10:00:00', '2026-02-12 17:38:30', 0, NULL, 1, 1, 1),
('ACCEPTED', 2, '2026-02-07 10:00:00', '2026-02-12 17:38:30', 1, '2026-02-07 22:00:00', 1, 2, 1),
('ACCEPTED', 1, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 2, 2, 1),
('ACCEPTED', 2, '2026-02-08 10:00:00', '2026-02-12 17:38:30', 0, NULL, 2, 3, 1),
('ACCEPTED', 2, '2026-02-08 10:00:00', '2026-02-08 16:00:00', 1, '2026-02-08 22:00:00', 2, 4, 5),
('PENDING', NULL, '2026-02-12 20:10:16', NULL, 0, NULL, 2, 11, NULL),
('ACCEPTED', 1, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 3, 3, 1),
('ACCEPTED', 2, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 3, 4, 1),
('ACCEPTED', 1, '2026-02-09 10:00:00', '2026-02-09 16:00:00', 0, NULL, 3, 5, 9),
('ACCEPTED', 2, '2026-02-09 10:00:00', '2026-02-09 16:00:00', 1, '2026-02-09 22:00:00', 3, 6, 9),
('ACCEPTED', 1, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 4, 4, 1),
('ACCEPTED', 2, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 4, 5, 1),
('ACCEPTED', 1, '2026-02-10 10:00:00', '2026-02-12 02:01:32', 0, NULL, 4, 7, 2),
('ACCEPTED', 2, '2026-02-10 10:00:00', '2026-02-12 02:01:35', 1, '2026-02-10 22:00:00', 4, 8, 2),
('ACCEPTED', 1, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 7, 7, 1),
('ACCEPTED', 2, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 7, 8, 1),
('ACCEPTED', 1, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 8, 8, 1),
('ACCEPTED', 2, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 8, 9, 1),
('ACCEPTED', 1, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 9, 9, 1),
('ACCEPTED', 2, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 9, 10, 1),
('PENDING', NULL, '2026-02-12 14:29:25', NULL, 0, NULL, 10, 1, NULL),
('PENDING', NULL, '2026-02-12 05:21:19', NULL, 0, NULL, 10, 3, NULL),
('ACCEPTED', 1, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 10, 10, 1),
('ACCEPTED', 2, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 10, 11, 1),
('ACCEPTED', 1, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 11, 1, 1),
('ACCEPTED', 2, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 11, 11, 1),
('ACCEPTED', 1, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 12, 2, 1),
('ACCEPTED', NULL, '2026-02-12 22:56:26', NULL, 0, NULL, 12, 10, NULL),
('ACCEPTED', 2, '2026-02-12 17:38:30', '2026-02-12 17:38:30', 0, NULL, 12, 11, 1);

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
  `user_id` int(10) UNSIGNED NOT NULL,
  `username` varchar(50) NOT NULL,
  `email` varchar(190) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` varchar(9) NOT NULL DEFAULT 'PLAYER',
  `display_name` varchar(80) NOT NULL,
  `bio` longtext DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `country` varchar(80) DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `gender` varchar(7) DEFAULT 'UNKNOWN',
  `email_verified` tinyint(4) NOT NULL DEFAULT 0,
  `is_active` tinyint(4) NOT NULL DEFAULT 1,
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `profile_image_id` int(10) UNSIGNED DEFAULT NULL,
  `reset_password_token_hash` varchar(64) DEFAULT NULL,
  `reset_password_expires_at` datetime DEFAULT NULL,
  `two_factor_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `two_factor_secret` varchar(64) DEFAULT NULL,
  `two_factor_enabled_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `users`
--

INSERT INTO `users` (`user_id`, `username`, `email`, `password_hash`, `role`, `display_name`, `bio`, `phone`, `country`, `birth_date`, `gender`, `email_verified`, `is_active`, `last_login_at`, `created_at`, `updated_at`, `profile_image_id`, `reset_password_token_hash`, `reset_password_expires_at`, `two_factor_enabled`, `two_factor_secret`, `two_factor_enabled_at`) VALUES
(1, 'user_01', 'user01@pulse.test', '$2y$13$Z8KH6F1Yc7zEaeNm0e3Fg.M6lwozPar2t56J0LRV.YixCgtlgZ7mG', 'ADMIN', 'User 01', 'Profil test 01', '+21650000001', 'Tunisia', '2001-03-15', 'MALE', 1, 1, '2026-02-27 09:42:04', '2026-01-14 10:00:00', '2026-02-27 09:42:04', 27, NULL, NULL, 1, 'QQFF3RQUHQFTKESTVRTYNCBZJUE7Z3UF', '2026-02-27 03:45:10'),
(2, 'user_02', 'user02@pulse.test', '$2y$13$hkee6gG8rGpo3Vkjx6wCZO3U7W7X1VZHXfGs7DqHNQkvuI67dw3U.', 'ORGANIZER', 'lassss', 'Profil test 02', '+21650000002', 'Tunisia', '2000-12-05', 'MALE', 1, 1, '2026-02-12 17:31:15', '2026-01-15 10:00:00', '2026-02-12 17:31:15', 11, NULL, NULL, 0, NULL, NULL),
(3, 'user_03', 'user03@pulse.test', '$2y$13$6JETWg75nD.wwaiovImnu.8tjtIV/tpGdicEroHgUJLlA4svxt2bS', 'CAPTAIN', 'User 03', 'Profil test 03', '+21650000003', 'Tunisia', '2000-08-27', 'OTHER', 1, 1, '2026-02-27 10:00:25', '2026-01-16 10:00:00', '2026-02-27 10:00:25', 3, NULL, NULL, 0, NULL, NULL),
(4, 'user_04', 'user04@pulse.test', '$2y$13$Xuod0vKmkviNEupq10lWM.2WG/2TDpizMn38AVimEOnm7rKRN3mRS', 'PLAYER', 'User 04', 'Profil test 04', '+21650000004', 'Tunisia', '2000-05-19', 'UNKNOWN', 1, 1, '2026-02-13 11:31:54', '2026-01-17 10:00:00', '2026-02-13 11:31:54', 4, NULL, NULL, 0, NULL, NULL),
(5, 'user_05', 'user05@pulse.test', '$2y$13$jK8qph2MwRskKOi6nxlZ4O3BBXrAxrq1q2lRdzcndtvV21N.ntnT6', 'ORGANIZER', 'User 05', 'Profil test 05', '+21650000005', 'Tunisia', '2000-02-09', 'MALE', 1, 1, '2026-02-26 19:29:29', '2026-01-18 10:00:00', '2026-02-26 19:29:29', 5, NULL, NULL, 0, NULL, NULL),
(6, 'user_06', 'user06@pulse.test', '$2y$13$fSySJPcy7emdITgIwf7rXOgEBdKOJXZ1YpUwVFdUqtxcvm7mmJwyK', 'CAPTAIN', 'User 06', 'Profil test 06', '+21650000006', 'Tunisia', '1999-11-01', 'FEMALE', 1, 1, '2026-02-27 07:01:12', '2026-01-19 10:00:00', '2026-02-27 07:01:12', 6, NULL, NULL, 0, NULL, NULL),
(7, 'user_07', 'user07@pulse.test', '$2y$10$hXnVS9o9snQnrV7BTs9xrepKhFJ4IdNx2r.Mh71A8RlRTzVJvQvVS', 'PLAYER', 'User 07', 'Profil test 07', '+21650000007', 'Tunisia', '1999-07-24', 'OTHER', 1, 1, '2026-02-12 03:00:00', '2026-01-20 10:00:00', '2026-01-20 12:00:00', 7, NULL, NULL, 0, NULL, NULL),
(8, 'user_08', 'user08@pulse.test', '$2y$13$lVGucH1a1f1xrI/Pa1CTg.BvtgivoE7pFY/zWRjDnb/d5As85pT3u', 'PLAYER', 'User 08', 'Profil test 08', '+21650000008', 'Tunisia', '1999-04-15', 'UNKNOWN', 1, 1, '2026-02-13 12:28:22', '2026-01-21 10:00:00', '2026-02-13 12:28:22', 8, NULL, NULL, 0, NULL, NULL),
(9, 'user_09', 'user09@pulse.test', '$2y$10$hXnVS9o9snQnrV7BTs9xrepKhFJ4IdNx2r.Mh71A8RlRTzVJvQvVS', 'ORGANIZER', 'User 09', 'Profil test 09', '+21650000009', 'Tunisia', '1999-01-05', 'MALE', 0, 1, '2026-02-12 01:00:00', '2026-01-22 10:00:00', '2026-01-22 12:00:00', 9, NULL, NULL, 0, NULL, NULL),
(10, 'user_10', 'user10@pulse.test', '$2y$13$kgT4laHP67b/b/8sF/iiY.vuqSlK.6N81S7erSIqMY5C8fTZ1S2nu', 'PLAYER', 'User 10', 'Profil test 10', '+21650000010', 'Tunisia', '1998-09-27', 'FEMALE', 1, 1, '2026-02-12 18:00:21', '2026-01-23 10:00:00', '2026-02-12 18:00:21', 10, NULL, NULL, 0, NULL, NULL),
(15, '3asfouraaa', 'miladimiladi2310@gmail.com', '$2y$13$KJ/d.2wq7CvjflGNglAUAuqzhDEH2hNf/8BgjeLlm65n0bj1V9ScW', 'ORGANIZER', '3asfouraaa', NULL, '99059409', 'Tunisie', '2003-10-23', 'MALE', 1, 1, '2026-03-06 09:02:56', '2026-02-27 02:06:02', '2026-03-06 09:02:56', 55, NULL, NULL, 0, NULL, NULL),
(16, '5outifa', 'starzouka@gmail.com', '$2y$13$Ui74fYXuplvw7PPuVSPV1.eftF/KhXsRGSfotkzPcuOIBCuq5e1FS', 'ORGANIZER', '5outifa', NULL, '99059409', 'Tunisie', '2003-10-23', 'MALE', 1, 1, '2026-02-27 09:47:22', '2026-02-27 09:28:25', '2026-02-27 09:47:22', NULL, NULL, NULL, 0, NULL, NULL),
(17, 'iliyes', 'azerty@azertyu.yu', '$2a$12$isGDIBfF7wqyPlSmqpEjYeHG21UspTMZ67WmLvETRES6PCnwuuL3O', 'PLAYER', 'azertyuiop', NULL, NULL, NULL, NULL, 'UNKNOWN', 1, 1, NULL, '2026-04-15 18:24:52', '2026-04-15 18:24:52', NULL, NULL, NULL, 0, NULL, NULL),
(21, 'lassss', 'ilyesmiladi3@gmail.com', '$2y$13$MCEYUWqG8okufwFiIZR5c.9VBbXmmxtWTOwX8QtVq0wsF3qhF3Esa', 'ORGANIZER', 'lasssslousa', NULL, '12345678', 'usa', '2026-04-08', 'MALE', 1, 1, '2026-04-15 23:57:08', '2026-04-15 23:15:07', '2026-04-15 23:57:28', 63, NULL, NULL, 0, NULL, NULL);

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `carts`
--
ALTER TABLE `carts`
  ADD PRIMARY KEY (`cart_id`),
  ADD UNIQUE KEY `UNIQ_4E004AACA76ED395` (`user_id`);

--
-- Index pour la table `cart_items`
--
ALTER TABLE `cart_items`
  ADD PRIMARY KEY (`cart_id`,`product_id`),
  ADD KEY `IDX_BEF484451AD5CDBF` (`cart_id`),
  ADD KEY `IDX_BEF484454584665A` (`product_id`);

--
-- Index pour la table `categories`
--
ALTER TABLE `categories`
  ADD PRIMARY KEY (`category_id`),
  ADD UNIQUE KEY `UNIQ_3AF346687989D9B62F6E5E0A` (`slug`);

--
-- Index pour la table `comments`
--
ALTER TABLE `comments`
  ADD PRIMARY KEY (`comment_id`),
  ADD UNIQUE KEY `UNIQ_COMMENTS_PRODUCT_AUTHOR` (`product_id`,`author_user_id`),
  ADD KEY `IDX_5F9E962A4B89032C` (`post_id`),
  ADD KEY `IDX_5F9E962AE2544CD6` (`author_user_id`),
  ADD KEY `IDX_5F9E962ABF2AF943` (`parent_comment_id`),
  ADD KEY `IDX_COMMENTS_PRODUCT_ID` (`product_id`);

--
-- Index pour la table `doctrine_migration_versions`
--
ALTER TABLE `doctrine_migration_versions`
  ADD PRIMARY KEY (`version`);

--
-- Index pour la table `friendships`
--
ALTER TABLE `friendships`
  ADD PRIMARY KEY (`user1_id`,`user2_id`),
  ADD KEY `IDX_E0A8B7CA31EE6AF` (`user1_id`),
  ADD KEY `IDX_E0A8B7CA9A17B715` (`user2_id`);

--
-- Index pour la table `friend_requests`
--
ALTER TABLE `friend_requests`
  ADD PRIMARY KEY (`request_id`),
  ADD KEY `IDX_EC63B01B2130303A` (`from_user_id`),
  ADD KEY `IDX_EC63B01B29F6EE60` (`to_user_id`);

--
-- Index pour la table `games`
--
ALTER TABLE `games`
  ADD PRIMARY KEY (`game_id`),
  ADD UNIQUE KEY `UNIQ_FF232B31989D9B62F6E5E0A` (`slug`),
  ADD KEY `IDX_FF232B3112469DE2` (`category_id`),
  ADD KEY `IDX_FF232B31E5A0E336` (`cover_image_id`),
  ADD KEY `IDX_FF232B31FB5A3183` (`status`),
  ADD KEY `IDX_FF232B31DFADE446` (`popularity_score`);

--
-- Index pour la table `game_favorites`
--
ALTER TABLE `game_favorites`
  ADD PRIMARY KEY (`user_id`,`game_id`),
  ADD KEY `IDX_E4F1BA04A76ED395` (`user_id`),
  ADD KEY `IDX_E4F1BA04E48FD905` (`game_id`);

--
-- Index pour la table `images`
--
ALTER TABLE `images`
  ADD PRIMARY KEY (`image_id`),
  ADD KEY `IDX_E01FBE6A861E61EA` (`uploaded_by_user_id`);

--
-- Index pour la table `matches`
--
ALTER TABLE `matches`
  ADD PRIMARY KEY (`match_id`),
  ADD KEY `IDX_62615BA33D1A3E7` (`tournament_id`),
  ADD KEY `IDX_62615BAB956681A` (`result_submitted_by_user_id`);

--
-- Index pour la table `match_teams`
--
ALTER TABLE `match_teams`
  ADD PRIMARY KEY (`match_id`,`team_id`),
  ADD KEY `IDX_28A85DF92ABEACD6` (`match_id`),
  ADD KEY `IDX_28A85DF9296CD8AE` (`team_id`);

--
-- Index pour la table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`message_id`),
  ADD KEY `IDX_DB021E962A98155E` (`sender_user_id`),
  ADD KEY `IDX_DB021E96DA57E237` (`receiver_user_id`);

--
-- Index pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_75EA56E0FB7336F0E3BD61CE16BA31DBBF396750` (`queue_name`,`available_at`,`delivered_at`,`id`);

--
-- Index pour la table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`notification_id`),
  ADD KEY `IDX_6000B0D3A76ED395` (`user_id`);

--
-- Index pour la table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`order_id`),
  ADD UNIQUE KEY `UNIQ_E52FFDEE1AD5CDBF` (`cart_id`),
  ADD KEY `IDX_E52FFDEEA76ED395` (`user_id`),
  ADD KEY `IDX_ORDERS_STRIPE_CHECKOUT_SESSION` (`stripe_checkout_session_id`);

--
-- Index pour la table `personne`
--
ALTER TABLE `personne`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `posts`
--
ALTER TABLE `posts`
  ADD PRIMARY KEY (`post_id`),
  ADD KEY `IDX_885DBAFAE2544CD6` (`author_user_id`);

--
-- Index pour la table `post_images`
--
ALTER TABLE `post_images`
  ADD PRIMARY KEY (`post_id`,`image_id`),
  ADD KEY `IDX_D03D5A0F4B89032C` (`post_id`),
  ADD KEY `IDX_D03D5A0F3DA5256D` (`image_id`);

--
-- Index pour la table `post_likes`
--
ALTER TABLE `post_likes`
  ADD PRIMARY KEY (`post_id`,`user_id`),
  ADD KEY `IDX_DED1C2924B89032C` (`post_id`),
  ADD KEY `IDX_DED1C292A76ED395` (`user_id`);

--
-- Index pour la table `products`
--
ALTER TABLE `products`
  ADD PRIMARY KEY (`product_id`),
  ADD KEY `IDX_B3BA5A5A296CD8AE` (`team_id`);

--
-- Index pour la table `product_images`
--
ALTER TABLE `product_images`
  ADD PRIMARY KEY (`product_id`,`image_id`),
  ADD KEY `IDX_8263FFCE4584665A` (`product_id`),
  ADD KEY `IDX_8263FFCE3DA5256D` (`image_id`);

--
-- Index pour la table `reports`
--
ALTER TABLE `reports`
  ADD PRIMARY KEY (`report_id`),
  ADD KEY `IDX_F11FA745DF3D6D95` (`reporter_user_id`),
  ADD KEY `IDX_F11FA7454E1B747C` (`handled_by_admin_id`);

--
-- Index pour la table `teams`
--
ALTER TABLE `teams`
  ADD PRIMARY KEY (`team_id`),
  ADD KEY `IDX_96C222586D947EBB` (`logo_image_id`),
  ADD KEY `IDX_96C2225896F755D8` (`captain_user_id`);

--
-- Index pour la table `team_invites`
--
ALTER TABLE `team_invites`
  ADD PRIMARY KEY (`invite_id`),
  ADD KEY `IDX_FC071B5B296CD8AE` (`team_id`),
  ADD KEY `IDX_FC071B5BC58DAD6E` (`invited_user_id`),
  ADD KEY `IDX_FC071B5BEDB25FDD` (`invited_by_user_id`);

--
-- Index pour la table `team_join_requests`
--
ALTER TABLE `team_join_requests`
  ADD PRIMARY KEY (`request_id`),
  ADD KEY `IDX_438737F3296CD8AE` (`team_id`),
  ADD KEY `IDX_438737F3A76ED395` (`user_id`),
  ADD KEY `IDX_438737F3CC39CF7C` (`responded_by_captain_id`);

--
-- Index pour la table `team_members`
--
ALTER TABLE `team_members`
  ADD PRIMARY KEY (`team_id`,`user_id`),
  ADD KEY `IDX_BAD9A3C8296CD8AE` (`team_id`),
  ADD KEY `IDX_BAD9A3C8A76ED395` (`user_id`);

--
-- Index pour la table `tournaments`
--
ALTER TABLE `tournaments`
  ADD PRIMARY KEY (`tournament_id`),
  ADD KEY `IDX_E4BCFAC3EE5F645C` (`organizer_user_id`),
  ADD KEY `IDX_E4BCFAC3E48FD905` (`game_id`);

--
-- Index pour la table `tournament_requests`
--
ALTER TABLE `tournament_requests`
  ADD PRIMARY KEY (`request_id`),
  ADD KEY `IDX_9B3B30B4EE5F645C` (`organizer_user_id`),
  ADD KEY `IDX_9B3B30B4E48FD905` (`game_id`),
  ADD KEY `IDX_9B3B30B472001902` (`reviewed_by_admin_id`);

--
-- Index pour la table `tournament_teams`
--
ALTER TABLE `tournament_teams`
  ADD PRIMARY KEY (`tournament_id`,`team_id`),
  ADD KEY `IDX_5794B24133D1A3E7` (`tournament_id`),
  ADD KEY `IDX_5794B241296CD8AE` (`team_id`),
  ADD KEY `IDX_5794B241515F5BC8` (`decided_by_user_id`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD KEY `IDX_1483A5E9C4CF44DC` (`profile_image_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `carts`
--
ALTER TABLE `carts`
  MODIFY `cart_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT pour la table `categories`
--
ALTER TABLE `categories`
  MODIFY `category_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT pour la table `comments`
--
ALTER TABLE `comments`
  MODIFY `comment_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT pour la table `friend_requests`
--
ALTER TABLE `friend_requests`
  MODIFY `request_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT pour la table `games`
--
ALTER TABLE `games`
  MODIFY `game_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT pour la table `images`
--
ALTER TABLE `images`
  MODIFY `image_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=64;

--
-- AUTO_INCREMENT pour la table `matches`
--
ALTER TABLE `matches`
  MODIFY `match_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=69;

--
-- AUTO_INCREMENT pour la table `messages`
--
ALTER TABLE `messages`
  MODIFY `message_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `notification_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT pour la table `orders`
--
ALTER TABLE `orders`
  MODIFY `order_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT pour la table `personne`
--
ALTER TABLE `personne`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `posts`
--
ALTER TABLE `posts`
  MODIFY `post_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=29;

--
-- AUTO_INCREMENT pour la table `products`
--
ALTER TABLE `products`
  MODIFY `product_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT pour la table `reports`
--
ALTER TABLE `reports`
  MODIFY `report_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT pour la table `teams`
--
ALTER TABLE `teams`
  MODIFY `team_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT pour la table `team_invites`
--
ALTER TABLE `team_invites`
  MODIFY `invite_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT pour la table `team_join_requests`
--
ALTER TABLE `team_join_requests`
  MODIFY `request_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT pour la table `tournaments`
--
ALTER TABLE `tournaments`
  MODIFY `tournament_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT pour la table `tournament_requests`
--
ALTER TABLE `tournament_requests`
  MODIFY `request_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=22;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `carts`
--
ALTER TABLE `carts`
  ADD CONSTRAINT `FK_4E004AACA76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `cart_items`
--
ALTER TABLE `cart_items`
  ADD CONSTRAINT `FK_BEF484451AD5CDBF` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`cart_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_BEF484454584665A` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`);

--
-- Contraintes pour la table `comments`
--
ALTER TABLE `comments`
  ADD CONSTRAINT `FK_5F9E962A4B89032C` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_5F9E962ABF2AF943` FOREIGN KEY (`parent_comment_id`) REFERENCES `comments` (`comment_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `FK_5F9E962AE2544CD6` FOREIGN KEY (`author_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_COMMENTS_PRODUCT_ID` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `friendships`
--
ALTER TABLE `friendships`
  ADD CONSTRAINT `FK_E0A8B7CA31EE6AF` FOREIGN KEY (`user1_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_E0A8B7CA9A17B715` FOREIGN KEY (`user2_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `friend_requests`
--
ALTER TABLE `friend_requests`
  ADD CONSTRAINT `FK_EC63B01B2130303A` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_EC63B01B29F6EE60` FOREIGN KEY (`to_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `games`
--
ALTER TABLE `games`
  ADD CONSTRAINT `FK_FF232B3112469DE2` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  ADD CONSTRAINT `FK_FF232B31E5A0E336` FOREIGN KEY (`cover_image_id`) REFERENCES `images` (`image_id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `game_favorites`
--
ALTER TABLE `game_favorites`
  ADD CONSTRAINT `FK_E4F1BA04A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_E4F1BA04E48FD905` FOREIGN KEY (`game_id`) REFERENCES `games` (`game_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `images`
--
ALTER TABLE `images`
  ADD CONSTRAINT `FK_E01FBE6A861E61EA` FOREIGN KEY (`uploaded_by_user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `matches`
--
ALTER TABLE `matches`
  ADD CONSTRAINT `FK_62615BA33D1A3E7` FOREIGN KEY (`tournament_id`) REFERENCES `tournaments` (`tournament_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_62615BAB956681A` FOREIGN KEY (`result_submitted_by_user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `match_teams`
--
ALTER TABLE `match_teams`
  ADD CONSTRAINT `FK_28A85DF9296CD8AE` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_28A85DF92ABEACD6` FOREIGN KEY (`match_id`) REFERENCES `matches` (`match_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `FK_DB021E962A98155E` FOREIGN KEY (`sender_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_DB021E96DA57E237` FOREIGN KEY (`receiver_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `FK_6000B0D3A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `orders`
--
ALTER TABLE `orders`
  ADD CONSTRAINT `FK_E52FFDEE1AD5CDBF` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`cart_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_E52FFDEEA76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `posts`
--
ALTER TABLE `posts`
  ADD CONSTRAINT `FK_885DBAFAE2544CD6` FOREIGN KEY (`author_user_id`) REFERENCES `users` (`user_id`);

--
-- Contraintes pour la table `post_images`
--
ALTER TABLE `post_images`
  ADD CONSTRAINT `FK_D03D5A0F3DA5256D` FOREIGN KEY (`image_id`) REFERENCES `images` (`image_id`),
  ADD CONSTRAINT `FK_D03D5A0F4B89032C` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `post_likes`
--
ALTER TABLE `post_likes`
  ADD CONSTRAINT `FK_DED1C2924B89032C` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_DED1C292A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `products`
--
ALTER TABLE `products`
  ADD CONSTRAINT `FK_B3BA5A5A296CD8AE` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `product_images`
--
ALTER TABLE `product_images`
  ADD CONSTRAINT `FK_8263FFCE3DA5256D` FOREIGN KEY (`image_id`) REFERENCES `images` (`image_id`),
  ADD CONSTRAINT `FK_8263FFCE4584665A` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `reports`
--
ALTER TABLE `reports`
  ADD CONSTRAINT `FK_F11FA7454E1B747C` FOREIGN KEY (`handled_by_admin_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `FK_F11FA745DF3D6D95` FOREIGN KEY (`reporter_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `teams`
--
ALTER TABLE `teams`
  ADD CONSTRAINT `FK_96C222586D947EBB` FOREIGN KEY (`logo_image_id`) REFERENCES `images` (`image_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `FK_96C2225896F755D8` FOREIGN KEY (`captain_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `team_invites`
--
ALTER TABLE `team_invites`
  ADD CONSTRAINT `FK_FC071B5B296CD8AE` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_FC071B5BC58DAD6E` FOREIGN KEY (`invited_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_FC071B5BEDB25FDD` FOREIGN KEY (`invited_by_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `team_join_requests`
--
ALTER TABLE `team_join_requests`
  ADD CONSTRAINT `FK_438737F3296CD8AE` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_438737F3A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_438737F3CC39CF7C` FOREIGN KEY (`responded_by_captain_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `team_members`
--
ALTER TABLE `team_members`
  ADD CONSTRAINT `FK_BAD9A3C8296CD8AE` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_BAD9A3C8A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `tournaments`
--
ALTER TABLE `tournaments`
  ADD CONSTRAINT `FK_E4BCFAC3E48FD905` FOREIGN KEY (`game_id`) REFERENCES `games` (`game_id`),
  ADD CONSTRAINT `FK_E4BCFAC3EE5F645C` FOREIGN KEY (`organizer_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `tournament_requests`
--
ALTER TABLE `tournament_requests`
  ADD CONSTRAINT `FK_9B3B30B472001902` FOREIGN KEY (`reviewed_by_admin_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `FK_9B3B30B4E48FD905` FOREIGN KEY (`game_id`) REFERENCES `games` (`game_id`),
  ADD CONSTRAINT `FK_9B3B30B4EE5F645C` FOREIGN KEY (`organizer_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `tournament_teams`
--
ALTER TABLE `tournament_teams`
  ADD CONSTRAINT `FK_5794B241296CD8AE` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_5794B24133D1A3E7` FOREIGN KEY (`tournament_id`) REFERENCES `tournaments` (`tournament_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_5794B241515F5BC8` FOREIGN KEY (`decided_by_user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `users`
--
ALTER TABLE `users`
  ADD CONSTRAINT `FK_1483A5E9C4CF44DC` FOREIGN KEY (`profile_image_id`) REFERENCES `images` (`image_id`) ON DELETE SET NULL;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
