package com.pulse.desktop.ui;

import com.pulse.desktop.model.RouteDefinition;
import com.pulse.desktop.model.SimpleCard;
import com.pulse.desktop.repo.HomeRepository;
import com.pulse.desktop.service.Navigator;
import com.pulse.desktop.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoutePageController implements RouteAwarePage {
    @FXML
    private Label routeNameLabel;
    @FXML
    private Label routeIdLabel;
    @FXML
    private Label routeDescriptionLabel;
    @FXML
    private Label sectionHeadingLabel;
    @FXML
    private Label sectionSubheadingLabel;

    @FXML
    private FlowPane cardsPane;
    @FXML
    private VBox pageRoot;

    private final HomeRepository homeRepository = new HomeRepository();
    private RouteDefinition routeDefinition;

    @FXML
    private void refreshCards() {
        if (routeDefinition == null || cardsPane == null) {
            return;
        }
        try {
            List<SimpleCard> cards = homeRepository.listForRoute(routeDefinition.id(), 12);
            renderCards(cards);
        } catch (SQLException ex) {
            AlertUtils.error("Page", "Erreur de chargement du contenu.\n" + ex.getMessage());
        }
    }

    @FXML
    private void goHome() {
        Navigator.goTo("front_home");
    }

    @Override
    public void setRoute(RouteDefinition routeDefinition) {
        this.routeDefinition = routeDefinition;

        if (routeNameLabel != null) {
            routeNameLabel.setText(routeDefinition.title());
        }
        if (routeIdLabel != null) {
            routeIdLabel.setText("Route Symfony: " + routeDefinition.id());
        }
        if (routeDescriptionLabel != null) {
            routeDescriptionLabel.setText(templateHint(routeDefinition.id()));
        }
        if (sectionHeadingLabel != null) {
            sectionHeadingLabel.setText(sectionHeading(routeDefinition.id()));
        }
        if (sectionSubheadingLabel != null) {
            sectionSubheadingLabel.setText(sectionSubheading(routeDefinition.id()));
        }

        wireDynamicButtons();
        refreshCards();
    }

    private String sectionHeading(String routeId) {
        if (routeId.contains("tournament") || routeId.contains("match")) {
            return "RESULTATS COMPETITION";
        }
        if (routeId.contains("shop") || routeId.contains("product") || routeId.contains("order") || routeId.contains("cart")) {
            return "RESULTATS BOUTIQUE";
        }
        if (routeId.contains("team") || routeId.contains("player")) {
            return "RESULTATS EQUIPE/JOUEUR";
        }
        if (routeId.contains("feed") || routeId.contains("message") || routeId.contains("notification") || routeId.contains("friend")) {
            return "RESULTATS SOCIAUX";
        }
        return "RESULTATS";
    }

    private String sectionSubheading(String routeId) {
        if (routeId.contains("tournament") || routeId.contains("match")) {
            return "Structure inspiree des pages tournoi/match Symfony (filtres + cartes).";
        }
        if (routeId.contains("shop") || routeId.contains("product") || routeId.contains("order") || routeId.contains("cart")) {
            return "Structure inspiree des pages boutique Symfony (catalogue, actions, detail).";
        }
        if (routeId.contains("team") || routeId.contains("player")) {
            return "Structure inspiree des pages equipe/joueur Symfony (profils, listes, details).";
        }
        if (routeId.contains("feed") || routeId.contains("message") || routeId.contains("notification") || routeId.contains("friend")) {
            return "Structure inspiree des pages sociales Symfony (fil, widgets, messagerie).";
        }
        return "Donnees chargees depuis la base pulsedb.";
    }

    private String templateHint(String routeId) {
        if (routeId.startsWith("front_tournament") || routeId.startsWith("front_organizer_tournament") || routeId.startsWith("front_captain_tournament")) {
            return "Structure catalogue front (hero mini, filtres, cartes tournoi, pagination).";
        }
        if (routeId.startsWith("front_shop") || routeId.startsWith("front_product") || routeId.startsWith("front_order") || routeId.startsWith("front_cart")) {
            return "Structure boutique front (header page, filtres, cartes produits, actions panier).";
        }
        if (routeId.startsWith("front_match") || routeId.startsWith("front_organizer_match")) {
            return "Structure matchs front (hero mini, filtres, cartes match, details).";
        }
        if (routeId.startsWith("front_team") || routeId.startsWith("front_player")) {
            return "Structure equipes/joueurs front (layout social, cartes media, actions de profil).";
        }
        if (routeId.startsWith("front_feed") || routeId.startsWith("front_message") || routeId.startsWith("front_notification")) {
            return "Structure sociale front (side nav joueur, fil, widgets et notifications).";
        }
        return "Structure front harmonisee avec le template Symfony.";
    }

    private void renderCards(List<SimpleCard> cards) {
        cardsPane.getChildren().clear();
        if (cards.isEmpty()) {
            Label empty = new Label("Aucune donnee pour cette page.");
            empty.getStyleClass().add("muted");
            cardsPane.getChildren().add(empty);
            return;
        }

        for (SimpleCard card : cards) {
            cardsPane.getChildren().add(CardFactory.create(card));
        }
    }

    private void wireDynamicButtons() {
        if (pageRoot == null) {
            return;
        }
        for (Button button : collectButtons(pageRoot)) {
            String id = button.getId();
            if (id == null || id.isBlank()) {
                continue;
            }

            if (id.startsWith("nav__")) {
                String route = parseRouteFromId(id, "nav__");
                if (route != null && !route.isBlank()) {
                    button.setOnAction(event -> Navigator.goTo(route));
                }
                continue;
            }

            if (id.startsWith("submit__")) {
                String route = parseRouteFromId(id, "submit__");
                button.setOnAction(event -> handleSubmitRoute(route));
            }
        }
    }

    private void handleSubmitRoute(String route) {
        if (route == null || route.isBlank() || "self".equals(route)) {
            refreshCards();
            return;
        }
        Navigator.goTo(route);
    }

    private static String parseRouteFromId(String id, String prefix) {
        String payload = id.substring(prefix.length());
        int sep = payload.indexOf("__");
        if (sep > 0) {
            return payload.substring(0, sep);
        }
        return payload;
    }

    private static List<Button> collectButtons(Parent root) {
        List<Button> buttons = new ArrayList<>();
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button) {
                buttons.add(button);
            }
            if (child instanceof Parent nested) {
                buttons.addAll(collectButtons(nested));
            }
        }
        return buttons;
    }
}
