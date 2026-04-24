package com.pulse.desktop.ui;

import com.pulse.desktop.auth.SessionContext;
import com.pulse.desktop.config.AppConfig;
import com.pulse.desktop.db.Jdbc;
import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.service.RouteContext;
import com.pulse.desktop.util.AlertUtils;
import com.pulse.desktop.util.ImageResolver;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppController {
    @FXML
    private Label accountAvatarLabel;
    @FXML
    private Label accountNameLabel;
    @FXML
    private Label accountRoleLabel;
    @FXML
    private Label dbInfoLabel;
    @FXML
    private Label pageTitleLabel;

    @FXML
    private ImageView brandLogoView;

    @FXML
    private Button navHomeButton;
    @FXML
    private Button navTournamentsButton;
    @FXML
    private Button navGamesButton;
    @FXML
    private Button navMatchesButton;
    @FXML
    private Button navShopButton;
    @FXML
    private Button navTeamsButton;

    @FXML
    private Button authPrimaryButton;
    @FXML
    private Button authSecondaryButton;

    @FXML
    private Pane sidebarBackdrop;
    @FXML
    private VBox accountSidebar;

    @FXML
    private VBox sectionMainLinks;
    @FXML
    private VBox sectionCompetitionLinks;
    @FXML
    private VBox sectionShopLinks;
    @FXML
    private VBox sectionPlayerLinks;
    @FXML
    private VBox sectionCaptainLinks;
    @FXML
    private VBox sectionOrganizerLinks;
    @FXML
    private VBox sectionAdminLinks;
    @FXML
    private VBox sectionGuestLinks;
    @FXML
    private VBox sectionSupportLinks;

    @FXML
    private VBox sectionPlayerContainer;
    @FXML
    private VBox sectionCaptainContainer;
    @FXML
    private VBox sectionOrganizerContainer;
    @FXML
    private VBox sectionAdminContainer;
    @FXML
    private VBox sectionGuestContainer;

    @FXML
    private StackPane pageHost;

    private final Map<String, RouteDefinition> routes = new LinkedHashMap<>();
    private final Map<String, Button> sidebarButtons = new LinkedHashMap<>();
    private String activeRouteId = "front_home";
    private boolean sidebarOpen;

    @FXML
    public void initialize() {
        registerRoutes();
        buildSidebar();

        dbInfoLabel.setText("%s@%s:%d/%s".formatted(
                AppConfig.dbUser(),
                AppConfig.dbHost(),
                AppConfig.dbPort(),
                AppConfig.dbName()
        ));

        loadBrandImage();
        closeSidebarInstant();

        Navigator.init(this::openRoute, this::refreshSessionDependentUi);
        refreshSessionDependentUi();
        openRoute("front_home");
    }

    @FXML
    private void openTopHome() {
        openRoute("front_home");
    }

    @FXML
    private void openTopTournaments() {
        openRoute("front_tournaments");
    }

    @FXML
    private void openTopGames() {
        openRoute("front_games");
    }

    @FXML
    private void openTopMatches() {
        openRoute("front_matches");
    }

    @FXML
    private void openTopShop() {
        openRoute("front_shop");
    }

    @FXML
    private void openTopTeams() {
        openRoute("front_teams");
    }

    @FXML
    private void onAuthPrimary() {
        if (!SessionContext.isAuthenticated()) {
            openRoute("front_register");
            return;
        }
        openRoute("front_profile");
    }

    @FXML
    private void onAuthSecondary() {
        if (!SessionContext.isAuthenticated()) {
            openRoute("front_login");
            return;
        }
        SessionContext.logout();
        Navigator.authChanged();
        openRoute("front_home");
    }

    @FXML
    private void toggleSidebar() {
        if (sidebarOpen) {
            closeSidebar();
        } else {
            openSidebar();
        }
    }

    @FXML
    private void closeSidebar() {
        setSidebarOpen(false, true);
    }

    private void openSidebar() {
        setSidebarOpen(true, true);
    }

    private void closeSidebarInstant() {
        setSidebarOpen(false, false);
    }

    private void setSidebarOpen(boolean open, boolean animate) {
        sidebarOpen = open;
        sidebarBackdrop.setVisible(open);
        sidebarBackdrop.setManaged(open);
        double target = open ? 0 : 430;

        if (!animate) {
            accountSidebar.setTranslateX(target);
            return;
        }

        TranslateTransition transition = new TranslateTransition(Duration.millis(220), accountSidebar);
        transition.setToX(target);
        transition.play();
    }

    private void openRoute(String routeId) {
        RouteDefinition route = routes.getOrDefault(routeId, routes.get("front_home"));
        String fxmlPath;
        switch (route.pageType()) {
            case "home":
                fxmlPath = "/fxml/pages/home-view.fxml";
                break;
            case "login":
                fxmlPath = "/fxml/pages/login-view.fxml";
                break;
            case "register":
                fxmlPath = "/fxml/pages/register-view.fxml";
                break;
            case "dashboard":
                fxmlPath = "/fxml/pages/dashboard-view.fxml";
                break;
            case "forgot-password":
                fxmlPath = "/fxml/pages/routes/front_forgot_password-view.fxml";
                break;
            case "reset-password":
                fxmlPath = "/fxml/pages/routes/front_reset_password-view.fxml";
                break;
            case "password-change":
                fxmlPath = "/fxml/pages/routes/front_password_change-view.fxml";
                break;
            case "profile":
                fxmlPath = "/fxml/pages/routes/front_profile-view.fxml";
                break;
            case "profile-edit":
                fxmlPath = "/fxml/pages/routes/front_profile_edit-view.fxml";
                break;
            case "two-factor":
                fxmlPath = "/fxml/pages/routes/front_two_factor_challenge-view.fxml";
                break;
            case "action":
                handleActionRoute(route.id());
                return;
            default:
                fxmlPath = "/fxml/pages/routes/" + route.id() + "-view.fxml";
                break;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            Object controller = loader.getController();
            if (controller instanceof RouteAwarePage awarePage) {
                awarePage.setRoute(route);
            }
            pageHost.getChildren().setAll(content);
            pageTitleLabel.setText(route.title());
            activeRouteId = route.id();
            refreshTopNavActive();
            refreshSidebarActive();
            closeSidebar();
        } catch (IOException ex) {
            AlertUtils.error("Navigation", "Impossible d'ouvrir la page " + routeId + "\n" + ex.getMessage());
        }
    }

    private void handleActionRoute(String routeId) {
        switch (routeId) {
            case "front_notifications_mark_all_read":
                markAllNotificationsAsRead();
                Navigator.goTo("front_notifications");
                break;
            case "front_cart_add", "front_cart_item_remove", "front_cart_item_update":
                Navigator.goTo("front_cart");
                break;
            case "front_payment_checkout", "front_payment_checkout_session":
                Navigator.goTo("front_checkout");
                break;
            case "front_profile_2fa_setup", "front_profile_2fa_enable", "front_profile_2fa_disable", "front_profile_2fa_state":
                Navigator.goTo("front_profile_edit");
                break;
            case "front_profile_add_friend":
                Navigator.goTo("front_friends");
                break;
            case "front_team_detail_join":
                Navigator.goTo("front_team_detail");
                break;
            case "front_tournament_participate", "front_tournaments_export":
                Navigator.goTo("front_tournaments");
                break;
            case "front_game_detail_favorite_toggle":
                Navigator.goTo("front_game_detail");
                break;
            case "front_feed_chunk":
                Navigator.goTo("front_feed");
                break;
            case "front_captain_team_create":
                RouteContext.putString(RouteContext.KEY_TEAM_MODE, "create");
                Navigator.goTo("front_captain_team_manage");
                break;
            case "front_organizer_requests_export":
                Navigator.goTo("front_organizer_requests");
                break;
            default:
                String fallback = guessParentRoute(routeId);
                if (fallback != null) {
                    Navigator.goTo(fallback);
                } else {
                    AlertUtils.info("Action", "Action " + routeId + " executee (mode desktop de demonstration).");
                }
                break;
        }
    }

    private void markAllNotificationsAsRead() {
        if (!SessionContext.isAuthenticated()) {
            AlertUtils.warning("Notifications", "Connectez-vous pour marquer les notifications comme lues.");
            return;
        }

        String sql = """
                UPDATE notifications
                SET is_read = 1, read_at = NOW()
                WHERE user_id = ? AND is_read = 0
                """;
        try (Connection connection = Jdbc.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, SessionContext.getCurrentUser().getUserId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            AlertUtils.error("Notifications", "Impossible de marquer les notifications.\n" + ex.getMessage());
        }
    }

    private static String guessParentRoute(String actionRouteId) {
        if (actionRouteId == null || actionRouteId.isBlank()) {
            return null;
        }
        String id = actionRouteId;
        if (id.endsWith("_create")) {
            return id.substring(0, id.length() - "_create".length());
        }
        if (id.endsWith("_edit")) {
            return id.substring(0, id.length() - "_edit".length());
        }
        if (id.endsWith("_update")) {
            return id.substring(0, id.length() - "_update".length());
        }
        if (id.endsWith("_remove")) {
            return id.substring(0, id.length() - "_remove".length());
        }
        if (id.endsWith("_cancel")) {
            return id.substring(0, id.length() - "_cancel".length());
        }
        if (id.endsWith("_respond")) {
            return id.substring(0, id.length() - "_respond".length());
        }
        if (id.endsWith("_checkin")) {
            return id.substring(0, id.length() - "_checkin".length());
        }
        if (id.endsWith("_register")) {
            return id.substring(0, id.length() - "_register".length());
        }
        if (id.endsWith("_send")) {
            return id.substring(0, id.length() - "_send".length());
        }
        return null;
    }

    private void refreshSessionDependentUi() {
        String role = SessionContext.currentRole();
        String roleLabel = switch (role) {
            case "PLAYER" -> "Joueur";
            case "CAPTAIN" -> "Capitaine";
            case "ORGANIZER" -> "Organisateur";
            case "ADMIN" -> "Admin";
            default -> "Invite";
        };

        String displayName = SessionContext.currentDisplayName();
        accountNameLabel.setText(displayName);
        accountRoleLabel.setText(roleLabel);
        accountAvatarLabel.setText(initialOf(displayName));

        updateRolePill(role);

        if (SessionContext.isAuthenticated()) {
            authPrimaryButton.setText("Mon compte");
            authSecondaryButton.setText("Logout");
        } else {
            authPrimaryButton.setText("S'inscrire");
            authSecondaryButton.setText("SIGN IN");
        }

        boolean playerVisible = List.of("PLAYER", "CAPTAIN", "ORGANIZER", "ADMIN").contains(role);
        boolean captainVisible = List.of("CAPTAIN", "ADMIN").contains(role);
        boolean organizerVisible = List.of("ORGANIZER", "ADMIN").contains(role);
        boolean adminVisible = "ADMIN".equals(role);
        boolean guestVisible = "GUEST".equals(role);

        sectionPlayerContainer.setManaged(playerVisible);
        sectionPlayerContainer.setVisible(playerVisible);
        sectionCaptainContainer.setManaged(captainVisible);
        sectionCaptainContainer.setVisible(captainVisible);
        sectionOrganizerContainer.setManaged(organizerVisible);
        sectionOrganizerContainer.setVisible(organizerVisible);
        sectionAdminContainer.setManaged(adminVisible);
        sectionAdminContainer.setVisible(adminVisible);
        sectionGuestContainer.setManaged(guestVisible);
        sectionGuestContainer.setVisible(guestVisible);
    }

    private void refreshTopNavActive() {
        navHomeButton.getStyleClass().remove("topbar__item--active");
        navTournamentsButton.getStyleClass().remove("topbar__item--active");
        navGamesButton.getStyleClass().remove("topbar__item--active");
        navMatchesButton.getStyleClass().remove("topbar__item--active");
        navShopButton.getStyleClass().remove("topbar__item--active");
        navTeamsButton.getStyleClass().remove("topbar__item--active");

        if (isHomeRoute(activeRouteId)) {
            navHomeButton.getStyleClass().add("topbar__item--active");
        } else if (isTournamentsRoute(activeRouteId)) {
            navTournamentsButton.getStyleClass().add("topbar__item--active");
        } else if (isGamesRoute(activeRouteId)) {
            navGamesButton.getStyleClass().add("topbar__item--active");
        } else if (isMatchesRoute(activeRouteId)) {
            navMatchesButton.getStyleClass().add("topbar__item--active");
        } else if (isShopRoute(activeRouteId)) {
            navShopButton.getStyleClass().add("topbar__item--active");
        } else if (isTeamsRoute(activeRouteId)) {
            navTeamsButton.getStyleClass().add("topbar__item--active");
        }
    }

    private void refreshSidebarActive() {
        for (Map.Entry<String, Button> entry : sidebarButtons.entrySet()) {
            entry.getValue().getStyleClass().remove("accountSidebar__link--active");
        }
        Button activeButton = sidebarButtons.get(activeRouteId);
        if (activeButton != null) {
            activeButton.getStyleClass().add("accountSidebar__link--active");
        }
    }

    private void buildSidebar() {
        addLinks(sectionMainLinks, List.of(
                "front_home", "front_dashboard", "front_profile", "front_messages", "front_notifications"
        ));
        addLinks(sectionCompetitionLinks, List.of(
                "front_tournaments", "front_matches", "front_games", "front_teams"
        ));
        addLinks(sectionShopLinks, List.of(
                "front_shop", "front_cart", "front_orders"
        ));
        addLinks(sectionPlayerLinks, List.of(
                "front_feed", "front_players", "front_my_teams", "front_my_requests"
        ));
        addLinks(sectionCaptainLinks, List.of(
                "front_captain_team_manage", "front_captain_members", "front_captain_tournaments", "front_captain_products"
        ));
        addLinks(sectionOrganizerLinks, List.of(
                "front_organizer_requests", "front_organizer_tournaments", "front_organizer_matches", "front_organizer_registrations"
        ));
        addLinks(sectionAdminLinks, List.of(
                "front_dashboard",
                "admin_tournament_requests",
                "admin_tournaments",
                "admin_matches",
                "admin_teams",
                "admin_users",
                "admin_products",
                "admin_orders",
                "admin_carts",
                "admin_games",
                "admin_categories",
                "admin_catalog_dashboard",
                "admin_statistics"
        ));
        addLinks(sectionGuestLinks, List.of(
                "front_login", "front_register", "front_forgot_password"
        ));
        addLinks(sectionSupportLinks, List.of(
                "front_about", "front_contact", "front_faq"
        ));
    }

    private void addLinks(VBox section, List<String> routeIds) {
        section.getChildren().clear();
        for (String routeId : routeIds) {
            RouteDefinition route = routes.get(routeId);
            if (route == null) {
                continue;
            }
            Button button = new Button(route.title());
            button.getStyleClass().add("accountSidebar__link");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> openRoute(route.id()));
            section.getChildren().add(button);
            sidebarButtons.put(route.id(), button);
        }
    }

    private void loadBrandImage() {
        String logoExternal = ImageResolver.toExternalForm("assets/template_fo/img/logo1.jpeg");
        if (logoExternal == null) {
            return;
        }
        Image image = new Image(logoExternal, true);
        if (!image.isError()) {
            brandLogoView.setImage(image);
        }
    }

    private void updateRolePill(String role) {
        accountRoleLabel.getStyleClass().removeAll(
                "role--player", "role--captain", "role--organizer", "role--admin", "role--guest"
        );

        String styleClass = switch (role) {
            case "PLAYER" -> "role--player";
            case "CAPTAIN" -> "role--captain";
            case "ORGANIZER" -> "role--organizer";
            case "ADMIN" -> "role--admin";
            default -> "role--guest";
        };
        accountRoleLabel.getStyleClass().add(styleClass);
    }

    private static String initialOf(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "I";
        }
        return String.valueOf(Character.toUpperCase(displayName.trim().charAt(0)));
    }

    private void registerRoutes() {
        addRoute("front_home", "Accueil", "home");
        addRoute("front_dashboard", "Dashboard", "dashboard");
        addRoute("front_profile", "Mon profil", "profile");
        addRoute("front_profile_edit", "Edition profil", "profile-edit");
        addRoute("front_messages", "Messagerie", "generic");
        addRoute("front_conversation", "Conversation", "generic");
        addRoute("front_notifications", "Notifications", "generic");
        addRoute("front_tournaments", "Tournois", "generic");
        addRoute("front_tournament_detail", "Detail tournoi", "generic");
        addRoute("front_matches", "Matchs", "generic");
        addRoute("front_match_detail", "Detail match", "generic");
        addRoute("front_games", "Jeux", "generic");
        addRoute("front_game_detail", "Detail jeu", "generic");
        addRoute("front_teams", "Equipes", "generic");
        addRoute("front_teams_explore", "Explorer equipes", "generic");
        addRoute("front_team_detail", "Detail equipe", "generic");
        addRoute("front_shop", "Catalogue", "generic");
        addRoute("front_product_detail", "Detail produit", "generic");
        addRoute("front_cart", "Panier", "generic");
        addRoute("front_checkout", "Checkout", "generic");
        addRoute("front_orders", "Mes commandes", "generic");
        addRoute("front_order_detail", "Detail commande", "generic");
        addRoute("front_feed", "Fil d'actualite", "generic");
        addRoute("front_feed_public", "Fil public", "generic");
        addRoute("front_players", "Recherche joueurs", "generic");
        addRoute("front_player_profile", "Profil joueur", "generic");
        addRoute("front_friends", "Amis", "generic");
        addRoute("front_my_teams", "Mes equipes", "generic");
        addRoute("front_my_requests", "Mes demandes", "generic");
        addRoute("front_captain_team_manage", "Equipe (creer/gerer)", "generic");
        addRoute("front_captain_team_create", "Creer equipe", "action");
        addRoute("front_captain_members", "Membres", "generic");
        addRoute("front_captain_invite", "Invitations", "generic");
        addRoute("front_captain_requests", "Demandes equipe", "generic");
        addRoute("front_captain_tournaments", "Tournois equipe", "generic");
        addRoute("front_captain_team_tournaments", "Tournois de l'equipe", "generic");
        addRoute("front_captain_products", "Produits equipe", "generic");
        addRoute("front_captain_product_create", "Creer produit equipe", "generic");
        addRoute("front_captain_product_edit", "Editer produit equipe", "generic");
        addRoute("front_captain_orders", "Commandes equipe", "generic");
        addRoute("front_organizer_requests", "Demandes tournoi", "generic");
        addRoute("front_organizer_request_create", "Creer demande tournoi", "generic");
        addRoute("front_organizer_request_detail", "Detail demande tournoi", "generic");
        addRoute("front_organizer_tournaments", "Mes tournois", "generic");
        addRoute("front_organizer_tournament_detail", "Detail tournoi organise", "generic");
        addRoute("front_organizer_tournament_edit", "Editer tournoi organise", "generic");
        addRoute("front_organizer_matches", "Gestion matchs", "generic");
        addRoute("front_organizer_match_create", "Creer match", "generic");
        addRoute("front_organizer_match_edit", "Editer match", "generic");
        addRoute("front_organizer_registrations", "Inscriptions equipes", "generic");
        addRoute("front_about", "A propos", "generic");
        addRoute("front_contact", "Contact", "generic");
        addRoute("front_faq", "FAQ", "generic");
        addRoute("front_login", "Connexion", "login");
        addRoute("front_register", "Inscription", "register");
        addRoute("front_forgot_password", "Mot de passe oublie", "forgot-password");
        addRoute("front_reset_password", "Reset mot de passe", "reset-password");
        addRoute("front_password_change", "Changer mot de passe", "password-change");
        addRoute("front_two_factor_challenge", "Verification 2FA", "two-factor");
        addRoute("front_search", "Recherche", "generic");
        addRoute("front_post_create", "Creer post", "generic");
        addRoute("front_post_detail", "Detail post", "generic");
        addRoute("admin_users", "Admin utilisateurs", "generic");
        addRoute("admin_user_detail", "Detail utilisateur", "generic");
        addRoute("admin_user_edit", "Modifier utilisateur", "generic");
        addRoute("admin_user_create", "Creer utilisateur", "generic");
        addRoute("admin_tournament_requests", "Admin demandes tournois", "generic");
        addRoute("admin_tournaments", "Admin tournois", "generic");
        addRoute("admin_matches", "Admin matchs", "generic");
        addRoute("admin_teams", "Admin equipes", "generic");
        addRoute("admin_products", "Admin produits", "generic");
        addRoute("admin_orders", "Admin commandes", "generic");
        addRoute("admin_carts", "Admin paniers", "generic");
        addRoute("admin_games", "Admin jeux", "generic");
        addRoute("admin_categories", "Admin categories", "generic");
        addRoute("admin_catalog_dashboard", "Admin KPI catalogue", "generic");
        addRoute("admin_statistics", "Admin statistiques", "generic");

        addRoute("front_captain_invite_generate_message", "Generer message invitation", "action");
        addRoute("front_captain_invite_moderation_preview", "Apercu moderation invitation", "action");
        addRoute("front_captain_invite_send", "Envoyer invitation", "action");
        addRoute("front_captain_members_reactivate", "Reactiver membre", "action");
        addRoute("front_captain_members_remove", "Supprimer membre", "action");
        addRoute("front_captain_members_remove_inactive", "Supprimer inactifs", "action");
        addRoute("front_captain_members_roster_pdf", "Roster PDF", "action");
        addRoute("front_captain_members_roster_role", "Role roster", "action");
        addRoute("front_captain_product_delete", "Supprimer produit equipe", "action");
        addRoute("front_captain_product_image_remove", "Supprimer image produit", "action");
        addRoute("front_captain_requests_respond", "Repondre demande equipe", "action");
        addRoute("front_captain_team_manage_create", "Creer equipe capitaine", "action");
        addRoute("front_captain_team_manage_generate_branding", "Generer branding equipe", "action");
        addRoute("front_captain_team_manage_normalize_region", "Normaliser region equipe", "action");
        addRoute("front_captain_team_manage_update", "Mettre a jour equipe", "action");
        addRoute("front_captain_tournaments_cancel", "Annuler inscription tournoi", "action");
        addRoute("front_captain_tournaments_checkin", "Check-in tournoi", "action");
        addRoute("front_captain_tournaments_register", "Inscrire equipe tournoi", "action");
        addRoute("front_cart_add", "Ajouter au panier", "action");
        addRoute("front_cart_item_remove", "Supprimer article panier", "action");
        addRoute("front_cart_item_update", "Mettre a jour article panier", "action");
        addRoute("front_feed_chunk", "Feed chunk", "action");
        addRoute("front_friends_request_accept", "Accepter demande ami", "action");
        addRoute("front_friends_request_cancel", "Annuler demande ami", "action");
        addRoute("front_friends_request_refuse", "Refuser demande ami", "action");
        addRoute("front_game_detail_favorite_toggle", "Favori jeu", "action");
        addRoute("front_my_requests_cancel", "Annuler ma demande", "action");
        addRoute("front_my_teams_invite_respond", "Repondre invitation equipe", "action");
        addRoute("front_my_teams_leave", "Quitter equipe", "action");
        addRoute("front_notifications_mark_all_read", "Marquer notifications lues", "action");
        addRoute("front_organizer_registration_status", "Statut inscription equipe", "action");
        addRoute("front_organizer_requests_export", "Exporter demandes organisateur", "action");
        addRoute("front_payment_checkout", "Paiement checkout", "action");
        addRoute("front_payment_checkout_session", "Session checkout paiement", "action");
        addRoute("front_profile_2fa_disable", "Desactiver 2FA", "action");
        addRoute("front_profile_2fa_enable", "Activer 2FA", "action");
        addRoute("front_profile_2fa_setup", "Configurer 2FA", "action");
        addRoute("front_profile_2fa_state", "Etat 2FA", "action");
        addRoute("front_profile_add_friend", "Ajouter ami depuis profil", "action");
        addRoute("front_team_detail_join", "Rejoindre equipe", "action");
        addRoute("front_tournament_participate", "Participer tournoi", "action");
        addRoute("front_tournaments_export", "Exporter tournois", "action");
    }

    private void addRoute(String id, String title, String pageType) {
        routes.put(id, new RouteDefinition(id, title, pageType));
    }

    private static boolean isHomeRoute(String route) {
        return List.of(
                "front_home", "front_about", "front_contact", "front_faq",
                "front_search", "front_login", "front_register", "front_forgot_password",
                "front_reset_password", "front_password_change", "front_dashboard",
                "front_feed", "front_feed_public", "front_messages", "front_conversation",
                "front_notifications", "front_profile", "front_profile_edit",
                "front_two_factor_challenge"
        ).contains(route);
    }

    private static boolean isTournamentsRoute(String route) {
        return route.startsWith("front_tournament")
                || route.startsWith("front_organizer_tournament")
                || route.startsWith("front_organizer_request")
                || route.startsWith("admin_tournament")
                || List.of("front_organizer_registrations", "front_captain_tournaments", "front_captain_team_tournaments", "front_captain_requests")
                .contains(route);
    }

    private static boolean isGamesRoute(String route) {
        return route.startsWith("front_game")
                || "admin_games".equals(route)
                || "admin_categories".equals(route)
                || "admin_catalog_dashboard".equals(route);
    }

    private static boolean isMatchesRoute(String route) {
        return route.startsWith("front_match")
                || route.startsWith("front_organizer_match")
                || route.startsWith("admin_match");
    }

    private static boolean isShopRoute(String route) {
        return List.of("front_shop", "front_cart", "front_checkout", "front_orders", "front_order_detail", "front_product_detail",
                "front_captain_products", "front_captain_orders",
                "admin_products", "admin_orders", "admin_carts", "admin_statistics").contains(route)
                || route.startsWith("front_captain_product");
    }

    private static boolean isTeamsRoute(String route) {
        return route.startsWith("front_team")
                || route.startsWith("front_player")
                || route.startsWith("admin_user")
                || "admin_teams".equals(route)
                || "admin_users".equals(route)
                || List.of("front_players", "front_friends", "front_my_teams", "front_my_requests",
                "front_captain_members", "front_captain_invite", "front_captain_team_create", "front_captain_team_manage")
                .contains(route);
    }
}
